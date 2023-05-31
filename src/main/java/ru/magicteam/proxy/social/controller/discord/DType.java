package ru.magicteam.proxy.social.controller.discord;

import ru.magicteam.proxy.social.Settings;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum DType {
    PING("ping"),
    MODAL("modal"),

    GENERATE_GOOGLE_FORM("generate_google_form"),
    CREATE_GOOGLE_FORM_BUTTON("create_google_form_button"),
    BUTTON_PLAYER_JOIN_REQUEST_YES("button_payer_join_request_yes"),
    BUTTON_PLAYER_JOIN_REQUEST_NO("button_player_join_request_no"),
    GENERATE_GOOGLE_FORM_USER_OPTION(Settings.IMP.MAIN.DISCORD.ACTIVITY_NAME),
    BUTTON_PLAYER_INFO("button_player_info"),
    BUTTON_PLAYER_NOTIFY("button_player_notify"),
    BUTTON_PLAYER_KICK("button_player_kick");

    public final String value;

    DType(String value) {
        this.value = value;
    }

    public static Optional<DType> fromString(String value){
       return Arrays.stream(DType.values()).filter(x -> Objects.equals(x.value, value)).findFirst();
    }
}
