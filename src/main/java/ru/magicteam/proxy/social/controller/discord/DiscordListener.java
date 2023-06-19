package ru.magicteam.proxy.social.controller.discord;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.magicteam.proxy.social.Settings;

import java.util.Optional;

public class DiscordListener extends ListenerAdapter {

    private final DiscordController discordController;
    private final Logger logger;

    public DiscordListener(DiscordController discordController, Logger logger){
        this.discordController = discordController;
        this.logger = logger;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        super.onSlashCommandInteraction(e);
        logger.info("Got command " + e.getName() + " from " + e.getUser().getName() + ".");
        Optional<DType> optionalDType = DType.fromString(e.getName());
        if(optionalDType.isEmpty())
            logger.error("Got slashCommand interaction event, but not found DType while converting.");
        discordController.callEvent(e.getUser().getIdLong(), optionalDType.get(), DiscordEvent.EventType.SLASH_COMMAND, e);
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

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        ComponentInfo info = parse(event.getModalId());
        discordController.callEvent(info.id, info.type, DiscordEvent.EventType.MODAL, event);
    }

    private ComponentInfo parse(String name) throws IllegalArgumentException{
        String[] data = name.split(Settings.IMP.MAIN.GOOGLE_FORM.SEPARATOR);
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
