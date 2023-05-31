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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;

public class DiscordListener extends ListenerAdapter {

    private final DiscordController discordController;

    public DiscordListener(DiscordController discordController){
        this.discordController = discordController;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        super.onSlashCommandInteraction(e);
        Optional<DType> optionalDType = DType.fromString(e.getName());
        if(optionalDType.isEmpty())
            return;
        discordController.callEvent(e.getUser().getIdLong(), optionalDType.get(), DiscordEvent.EventType.SLASH_COMMAND, e);
        /*
        switch (optionalDType.get()){


            case PING -> {
                long time = System.currentTimeMillis();
                e.reply("Pong!").setEphemeral(true)
                        .flatMap(v -> e.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                        .queue();
            }
            case MODAL -> e.replyModal(ServerJoinRequest.build()).queue();
            case GENERATE_TYPE -> {
                for(OptionMapping o: e.getOptions()){
                    Optional<DType> optionalOption = DType.fromString(o.getName());
                    if(optionalOption.isEmpty())
                        return;
                    switch (optionalOption.get()) {
                        case GENERATE_GOOGLE_FORM -> e.getChannel()
                                .sendMessageEmbeds(new GenerateGoogleForm().build())
                                .setActionRow(Button.success(DType.CREATE_GOOGLE_FORM_BUTTON.value, "Создать ссылку на google форму"))
                                .queue();
                        default -> throw new IllegalStateException("Unexpected value: " + DType.fromString(o.getName()));
                    }
                }
            }


        } */
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Optional<DType> type = DType.fromString(event.getMessage().getContentRaw());
        if(type.isEmpty())
            return;

        discordController.callEvent(event.getAuthor().getIdLong(), type.get(), DiscordEvent.EventType.MESSAGE,  event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        ComponentInfo info = parse(event.getComponentId());
        discordController.callEvent(info.id, info.type, DiscordEvent.EventType.BUTTON, event);
    }

    private ComponentInfo parse(String name) throws IllegalArgumentException{
        String[] data = name.split("_");
        if(data.length > 1){
            Optional<DType> optionalDType = DType.fromString(data[0]);
            if(optionalDType.isEmpty())
                throw new IllegalArgumentException();
            return new ComponentInfo(optionalDType.get(), Long.parseLong(data[1]));
        }
        else throw new IllegalArgumentException();
    }

    record ComponentInfo(DType type, Long id){}
}
