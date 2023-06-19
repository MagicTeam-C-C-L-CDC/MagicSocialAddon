package ru.magicteam.proxy.social.controller.discord.register;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
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
                Settings.IMP.MAIN.DISCORD.GOOGLE_FORM_USER_COMMAND_DESCRIPTION, true);
        generate_google_form.addOptions(buildOptions);

        SlashCommandData searchDiscord = Commands.slash(DType.SEARCH.value, "Search something with bot");
        SubcommandGroupData searchSubCommandType = new SubcommandGroupData(DType.SEARCH_ACCESS.value, "По каким данным искать заявку");

        SubcommandData searchByUser = new SubcommandData(DType.SEARCH_DISCORD_USER.value, "Поиск по пользователям дискорд");
        searchByUser.addOptions(new OptionData(OptionType.USER, "user", "Выберите пользователя", true));

        SubcommandData searchById = new SubcommandData(DType.SEARCH_DISCORD_ID.value, "Поиск по id пользователя");
        searchById.addOptions(new OptionData(OptionType.INTEGER, "id", "Введите id", true));

        SubcommandData searchByNickname = new SubcommandData(DType.SEARCH_DISCORD_USERNAME.value, "Поиск по нику пользователя");
        searchByNickname.addOptions(new OptionData(OptionType.STRING, "nickname", "Введите nickname пользоватея", true));

        searchSubCommandType.addSubcommands(
                searchByUser,
                searchById,
                searchByNickname
        );

        searchDiscord.addSubcommandGroups(searchSubCommandType);

        commands.add(generate_google_form);
        commands.add(searchDiscord);
    }
}
