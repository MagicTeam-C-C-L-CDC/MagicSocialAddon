package ru.magicteam.proxy.social.controller.discord.register;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import ru.magicteam.proxy.social.controller.discord.DType;

import java.util.LinkedList;
import java.util.List;

public class DiscordCommands{
    public static final List<CommandData> commands = new LinkedList<>();
    static {
        LinkedList<SlashCommandData> commands = new LinkedList<>();
        commands.add(Commands.slash(DType.MODAL.value, "Get invite modal"));
        commands.add(Commands.slash(DType.PING.value, "Just ping"));

        SlashCommandData build = Commands.slash(DType.BUILD.value, "Build some functional elements");
        OptionData buildOptions = new OptionData(OptionType.STRING, DType.GENERATE_TYPE.value, "element name");
        buildOptions.addChoice(DType.GENERATE_GOOGLE_FORM.value, DType.GENERATE_GOOGLE_FORM.value);
        build.addOptions(buildOptions);

        commands.add(build);
    }
}
