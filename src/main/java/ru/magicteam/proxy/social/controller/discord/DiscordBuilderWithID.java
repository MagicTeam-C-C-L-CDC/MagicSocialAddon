package ru.magicteam.proxy.social.controller.discord;

public interface DiscordBuilderWithID<T> {
    T build(Long id);
}

