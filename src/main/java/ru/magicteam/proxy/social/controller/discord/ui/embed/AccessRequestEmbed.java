package ru.magicteam.proxy.social.controller.discord.ui.embed;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.magicteam.proxy.social.Settings;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AccessRequestEmbed {

    public static MessageEmbed build(Long moderatorID, String username, Long userID, String userTag) {
        EmbedBuilder builder = new EmbedBuilder()
                .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED, "<@" + moderatorID.toString() + ">", false)
                .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED_SEARCH_KEY,
                        String.join(", ", username, userID.toString(), userTag), false
                )
                .setFooter(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")))
                .setColor(Color.decode("#FFFFFF"));
        return builder.build();
    }
}

record Field(String name, String value, Boolean inline){

}
