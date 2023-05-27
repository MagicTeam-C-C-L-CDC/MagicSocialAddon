package ru.magicteam.proxy.social.controller.discord.ui.modal;


import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public abstract class ServerJoinRequest{

    public static Modal build() {
        TextInput subject = TextInput.create("requestNickname", "Subject", TextInputStyle.SHORT)
                .setPlaceholder("Укажите ваш ник")
                .setMinLength(10)
                .setMaxLength(100) // or setRequiredRange(10, 100)
                .build();

        TextInput body = TextInput.create("request", "Body", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your concerns go here")
                .setMinLength(30)
                .setMaxLength(1000)
                .build();
        Modal modal = Modal.create("joinRequest", "Заявка на вступление")
                .addComponents(ActionRow.of(subject), ActionRow.of(body))
                .build();

        return modal;
    }

}
