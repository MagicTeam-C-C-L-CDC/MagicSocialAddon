package net.elytrium.limboauth.socialaddon.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.function.Consumer;

public record DiscordCommandInfo(String name, String description, Consumer<SlashCommandInteractionEvent> action) {

}
