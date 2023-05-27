package net.elytrium.limboauth.socialaddon.bot.embed;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.elytrium.limboauth.socialaddon.bot.DiscordBuilder;

import java.awt.*;


public class GenerateGoogleForm implements DiscordBuilder<MessageEmbed> {
    @Override
    public MessageEmbed build() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Title", null);
        eb.setColor(Color.red);
        eb.setColor(new Color(0xF40C0C));
        eb.setColor(new Color(255, 0, 54));
        eb.setDescription("Text");
        eb.addField("Title of field", "test of field", false);
        eb.addBlankField(false);
        eb.setAuthor("name", null, "https://github.com/zekroTJA/DiscordBot/blob/master/.websrc/zekroBot_Logo_-_round_small.png");
        eb.setFooter("Text", "https://github.com/zekroTJA/DiscordBot/blob/master/.websrc/zekroBot_Logo_-_round_small.png");
        eb.setImage("https://github.com/zekroTJA/DiscordBot/blob/master/.websrc/logo%20-%20title.png");
        eb.setThumbnail("https://github.com/zekroTJA/DiscordBot/blob/master/.websrc/logo%20-%20title.png");

        return eb.build();
    }
}
