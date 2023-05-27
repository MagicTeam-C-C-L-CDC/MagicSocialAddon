package net.elytrium.limboauth.socialaddon.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.elytrium.limboauth.socialaddon.bot.commands.DType;
import net.elytrium.limboauth.socialaddon.bot.embed.GenerateGoogleForm;
import net.elytrium.limboauth.socialaddon.bot.modal.ServerJoinRequest;
import net.elytrium.limboauth.socialaddon.model.DataManager;
import net.elytrium.limboauth.socialaddon.proxy.SocialManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;

public class DiscordCommandListener extends ListenerAdapter {
    public final DataManager dataManager;

    public DiscordCommandListener(DataManager dataManager){
        this.dataManager = dataManager;
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
