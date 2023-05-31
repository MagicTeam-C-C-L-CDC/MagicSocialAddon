package ru.magicteam.proxy.social.controller.discord.register;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.discord.DType;

import java.util.LinkedList;
import java.util.List;

public class DiscordCommands{
    public static final List<CommandData> commands = new LinkedList<>();
    static {
        commands.add(Commands.slash(DType.MODAL.value, "Get invite modal"));
        commands.add(Commands.slash(DType.PING.value, "Just ping"));


        SlashCommandData generate_google_form = Commands.slash(DType.GENERATE_GOOGLE_FORM.value, "Create google form");
        OptionData buildOptions = new OptionData(OptionType.USER, DType.GENERATE_GOOGLE_FORM_USER_OPTION.value,
                Settings.IMP.MAIN.DISCORD.GOOGLE_FORM_USER_COMMAND_DESCRIPTION);
        generate_google_form.addOptions(buildOptions);

        commands.add(generate_google_form);
    }
}
