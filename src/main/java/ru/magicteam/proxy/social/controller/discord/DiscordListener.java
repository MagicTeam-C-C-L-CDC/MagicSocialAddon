package ru.magicteam.proxy.social.controller.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.discord.ui.embed.GenerateGoogleForm;
import ru.magicteam.proxy.social.controller.discord.ui.modal.ServerJoinRequest;
import org.jetbrains.annotations.NotNull;
import ru.magicteam.proxy.social.model.ModelAPI;

import java.util.Collection;
import java.util.LinkedList;

public class DiscordListener extends ListenerAdapter {

    private final ModelAPI api;

    public DiscordListener(ModelAPI api){
        this.api = api;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        super.onSlashCommandInteraction(e);
        switch (DType.fromString(e.getName())){
            case PING -> {
                long time = System.currentTimeMillis();
                e.reply("Pong!").setEphemeral(true)
                        .flatMap(v -> e.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                        .queue();
            }
            case MODAL -> e.replyModal(ServerJoinRequest.build()).queue();
            case GENERATE_TYPE -> {
                for(OptionMapping o: e.getOptions()){
                    switch (DType.fromString(o.getName())) {
                        case GENERATE_GOOGLE_FORM -> e.getChannel()
                                .sendMessageEmbeds(new GenerateGoogleForm().build())
                                .setActionRow(Button.success(DType.CREATE_GOOGLE_FORM_BUTTON.name, "Создать ссылку на google форму"))
                                .queue();
                        default -> throw new IllegalStateException("Unexpected value: " + DType.fromString(o.getName()));
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User user = event.getAuthor();

        this.onMessageReceived.accept(event.getAuthor().getIdLong(), event.getMessage().getContentRaw());

        String buttonId = this.buttonIdMap.get(message);
        if (buttonId != null) {
            this.onButtonClicked(id, buttonId);
        }

        this.messageEvents.forEach(event -> {
            try {
                event.accept(id, message);
            } catch (Exception e) {
                this.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.SOCIAL_EXCEPTION_CAUGHT);
                if (Settings.IMP.MAIN.DEBUG) {
                    e.printStackTrace(); // printStackTrace is necessary there
                }
            }
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        this.onButtonClicked.accept(event.getUser().getIdLong(), event.getButton().getId());
    }

    private void onButtonClicked(Long id, String buttonId) {
        SocialButtonListenerAdapter buttonListenerAdapter = this.buttonEvents.get(buttonId);
        if (buttonListenerAdapter != null) {
            try {
                buttonListenerAdapter.accept(id);
            } catch (Exception e) {
                this.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.SOCIAL_EXCEPTION_CAUGHT);
                if (Settings.IMP.MAIN.DEBUG) {
                    e.printStackTrace(); // printStackTrace is necessary there
                }
            }
        }
    }

    public Collection<SlashCommandData> build() {
        LinkedList<SlashCommandData> commands = new LinkedList<>();
        commands.add(Commands.slash(DType.MODAL.name, "Get invite modal"));
        commands.add(Commands.slash(DType.PING.name, "Just ping"));

        SlashCommandData build = Commands.slash(DType.BUILD.name, "Build some functional elements");
        OptionData buildOptions = new OptionData(OptionType.STRING, DType.GENERATE_TYPE.name, "element name");
        buildOptions.addChoice(DType.GENERATE_GOOGLE_FORM.name, DType.GENERATE_GOOGLE_FORM.name);
        build.addOptions(buildOptions);

        commands.add(build);

        return commands;
    }

}
