/*
 * Copyright (C) 2022 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.magicteam.proxy.social;

import java.util.List;
import net.dv8tion.jda.api.entities.Activity;
import net.elytrium.commons.config.YamlConfig;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboauth.socialaddon.BuildConstants;

public class Settings extends YamlConfig {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = BuildConstants.ADDON_VERSION;

  @Comment({
      "Available serializers:",
      "LEGACY_AMPERSAND - \"&c&lExample &c&9Text\".",
      "LEGACY_SECTION - \"§c§lExample §c§9Text\".",
      "MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)",
      "GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)",
      "GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling."
  })
  public Serializers SERIALIZER = Serializers.LEGACY_AMPERSAND;
  public String PREFIX = "LimboAuth &6>>&f";

  @Create
  public MAIN MAIN;

  public static class MAIN {
    public List<String> FORCE_KEYBOARD_CMDS = List.of("!keyboard");

    public boolean DEFAULT_NOTIFY_ENABLED = true;

    public List<String> AFTER_LINKAGE_COMMANDS = List.of("alert {NICKNAME} has linked a social account");

    @Comment("Addon will print all exceptions if this parameter is set to true.")
    public boolean DEBUG = false;


    @Comment({
        "false - players with social 2FA enabled should enter the password",
        "true - players with social 2FA enabled can login without the password"
    })
    public boolean AUTH_2FA_WITHOUT_PASSWORD = true;

    @Comment("How long in milliseconds the player should wait before registering new account")
    public long PURGE_REGISTRATION_CACHE_MILLIS = 86400;

    @Comment("How many accounts can register the player per time (per purge-registration-cache-millis)")
    public int MAX_REGISTRATION_COUNT_PER_TIME = 5;

    @Create
    public MAIN.DISCORD DISCORD;

    public static class DISCORD {
      public boolean ENABLED = false;
      public String TOKEN = "1234567890";

      @Comment({
          "Available: ",
          "addrole <role id>",
          "remrole <role id>",
          "",
          "Example: ",
          "on-player-added: ",
          " - addrole 12345678",
          "on-player-removed: ",
          " - remrole 12345678"
      })
      public List<String> ON_PLAYER_ADDED = List.of();
      public List<String> ON_PLAYER_REMOVED = List.of();

      public boolean ACTIVITY_ENABLED = true;
      @Comment("Available values: PLAYING, STREAMING, LISTENING, WATCHING, COMPETING")
      public Activity.ActivityType ACTIVITY_TYPE = Activity.ActivityType.PLAYING;
      @Comment("Activity URL. Supported only with activity-type: STREAMING")
      public String ACTIVITY_URL = null;
      public String ACTIVITY_NAME = "LimboAuth Social Addon";

      @Comment({
          "Which role ids a player must have on the Discord server to use the bot",
          "",
          "Example: ",
          "required-roles: ",
          " - 1234567890"
      })
      public List<Object> REQUIRED_ROLES = List.of();
      @Comment({
          "It's better to keep this option enabled if you have set required-roles config option",
          "Requires SERVER MEMBERS INTENT to be enabled in the bot settings on the Discord Developer Portal"
      })
      public boolean GUILD_MEMBER_CACHE_ENABLED = false;
      public String NO_ROLES_MESSAGE = "You don't have permission to use commands";
    }

    @Create
    public MAIN.GEOIP GEOIP;

    @Comment({
        "GeoIP is an offline database providing approximate IP address locations",
        "In the SocialAddon's case, the IP location is displayed in notifications and alerts"
    })
    public static class GEOIP {
      public boolean ENABLED = false;
      @Comment({
          "Available placeholders: {CITY}, {COUNTRY}, {LEAST_SPECIFIC_SUBDIVISION}, {MOST_SPECIFIC_SUBDIVISION}"
      })
      @Placeholders({"{CITY}", "{COUNTRY}", "{LEAST_SPECIFIC_SUBDIVISION}", "{MOST_SPECIFIC_SUBDIVISION}"})
      public String FORMAT = "{CITY}, {COUNTRY}";
      @Comment("ISO 639-1")
      public String LOCALE = "en";
      @Comment({
          "MaxMind license key",
          "Regenerate if triggers an error"
      })
      public String LICENSE_KEY = "P5g0fVdAQIq8yQau";
      @Comment({
          "The interval at which the database will be updated, in milliseconds",
          "Default value: 14 days"
      })
      public long UPDATE_INTERVAL = 1209600000L;
      public String DEFAULT_VALUE = "Unknown";

      @Comment("It is not necessary to change {LICENSE_KEY}")
      @Placeholders({"{LICENSE_KEY}"})
      public String MMDB_CITY_DOWNLOAD = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key={LICENSE_KEY}&suffix=tar.gz";

      @Placeholders({"{LICENSE_KEY}"})
      public String MMDB_COUNTRY_DOWNLOAD = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key={LICENSE_KEY}&suffix=tar.gz";
    }

    @Create
    public MAIN.STRINGS STRINGS;

    public static class STRINGS {
      public String LINK_ALREADY = "Account is already linked";
      public String LINK_SOCIAL_REGISTER_CMD_USAGE = "You didn't specify a nickname. Enter '!account register <nickname>'";
      public String REGISTER_INCORRECT_NICKNAME = "There is no account with this nickname";
      public String REGISTER_TAKEN_NICKNAME = "This nickname is already taken";
      public String REGISTER_PREMIUM_NICKNAME = "This nickname belongs to a premium player";
      public String REGISTER_LIMIT = "You've tried to registered numerous times!";
      @Placeholders({"{PASSWORD}"})
      public String REGISTER_SUCCESS = "✅ Account was successfully registered{NL}Your password: {PASSWORD}{NL}Use '!keyboard' to show keyboard";

      public String FORCE_UNLINK_CMD_USAGE = "{PRFX} Usage: /forcesocialunregister <username>";

      public String NOTIFY_LEAVE = "➖ You've left the server";
      @Placeholders({"{IP}", "{LOCATION}"})
      public String NOTIFY_JOIN = "➕ You've joined the server {NL}🌐 IP: {IP} {LOCATION}{NL}You can block your account if that is not you";

      public String NOTIFY_ASK_KICK_MESSAGE = "{PRFX} You were kicked by the Social";
      @Placeholders({"{IP}", "{LOCATION}"})
      public String NOTIFY_ASK_VALIDATE = "❔ Someone tries to join the server.{NL}🌐 IP: {IP} {LOCATION}{NL}Is it you?";
      public String NOTIFY_ASK_VALIDATE_GAME = "{PRFX} You have 2FA enabled, check your social and validate your login!";
      public String NOTIFY_ASK_YES = "It's me";
      public String NOTIFY_ASK_NO = "It's not me";
      public String NOTIFY_THANKS = "Thanks for verifying your login";
      public String NOTIFY_WARN = "You can always change your password using the 'Restore' button";

      public String BLOCK_KICK_MESSAGE = "{PRFX} Your account was blocked by the Social";

      @Placeholders({"{NICKNAME}"})
      public String NOTIFY_ENABLE_SUCCESS = "Account {NICKNAME} now receives notifications";
      @Placeholders({"{NICKNAME}"})
      public String NOTIFY_DISABLE_SUCCESS = "Account {NICKNAME} doesn't receive notifications anymore";

      public String KICK_IS_OFFLINE = "Cannot kick player - player is offline";
      @Placeholders("{NICKNAME}")
      public String KICK_SUCCESS = "Player {NICKNAME} was successfully kicked";
      public String KICK_GAME_MESSAGE = "{PRFX} You were kicked by the Social";
      public String INFO_BTN = "Info";
      @Placeholders({"{NICKNAME}", "{SERVER}", "{IP}", "{LOCATION}", "{NOTIFY_STATUS}", "{BLOCK_STATUS}", "{TOTP_STATUS}"})
      public String INFO_MSG = "👤 IGN: {NICKNAME}{NL}🌍 Current status: {SERVER}{NL}🌐 IP: {IP} {LOCATION}{NL}⏰ Notifications: {NOTIFY_STATUS}{NL}❌ Blocked: {BLOCK_STATUS}{NL}🔑 2FA: {TOTP_STATUS}";
      public String STATUS_OFFLINE = "OFFLINE";
      public String NOTIFY_ENABLED = "Enabled";
      public String NOTIFY_DISABLED = "Disabled";

      public String KICK_BTN = "Kick";
      public String TOGGLE_NOTIFICATION_BTN = "Toggle notifications";
      public String SOCIAL_EXCEPTION_CAUGHT = "An exception occurred while processing your request";
    }
  }
}
