package ru.magicteam.proxy.social.controller.discord.ui.keyboard;

import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.discord.DiscordBuilderWithID;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonColor;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonItem;

import java.util.List;

public abstract class DiscordKeyboards {

    private static final String INFO_BTN = "info";
    private static final String NOTIFY_BTN = "notify";
    private static final String KICK_BTN = "kick";
    private static final String ASK_NO_BTN = "ask_no";
    private static final String ASK_YES_BTN = "ask_yes";

    public static final DiscordBuilderWithID<List<List<ButtonItem>>> infoKeyboard = (id) -> List.of(
            List.of(
                    new ButtonItem(String.join("_", INFO_BTN, id.toString()), Settings.IMP.MAIN.STRINGS.INFO_BTN, ButtonColor.PRIMARY)
            ),
            List.of(
                    new ButtonItem(String.join("_", NOTIFY_BTN, id.toString()), Settings.IMP.MAIN.STRINGS.TOGGLE_NOTIFICATION_BTN, ButtonColor.SECONDARY)
            ),
            List.of(
                    new ButtonItem(String.join("_", KICK_BTN, id.toString()), Settings.IMP.MAIN.STRINGS.KICK_BTN, ButtonColor.RED)
            )
    );

    public static final DiscordBuilderWithID<List<List<ButtonItem>>> yes_no_keyboard = (id) -> List.of(
            List.of(
                    new ButtonItem(String.join("_", ASK_NO_BTN, id.toString()), Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_NO, ButtonColor.RED),
                    new ButtonItem(String.join("_", ASK_YES_BTN, id.toString()), Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_YES, ButtonColor.GREEN)
            )
    );
}
