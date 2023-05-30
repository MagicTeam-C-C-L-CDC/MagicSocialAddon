package ru.magicteam.proxy.social.controller.discord;

import net.dv8tion.jda.api.events.Event;

public class DiscordEvent{
    public final Long id;
    public final EventType type;
    public final Event event;

    public DiscordEvent(Long id, EventType type){
        this.id = id;
        this.event = null;
        this.type = type;
    }

    public DiscordEvent(Long id, EventType type,  Event event){
        this.id = id;
        this.event = event;
        this.type = type;
    }

    public enum EventType{
        BUTTON,
        MESSAGE,
        SLASH_COMMAND
    }
}