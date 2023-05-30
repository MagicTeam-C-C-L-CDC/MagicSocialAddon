package ru.magicteam.proxy.social.controller.proxy.utils;

import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.event.TaskEvent;

public final class AuthSession {
    private final TaskEvent event;
    private final LimboPlayer player;

    public AuthSession(TaskEvent event, LimboPlayer player) {
        this.event = event;
        this.player = player;
    }

    public TaskEvent getEvent() {
        return this.event;
    }

    public LimboPlayer getPlayer() {
        return this.player;
    }
}