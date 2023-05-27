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

package net.elytrium.limboauth.socialaddon;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.scheduler.ScheduledTask;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.elytrium.commons.config.Placeholders;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.socialaddon.bot.DiscordCommandListener;
import net.elytrium.limboauth.socialaddon.proxy.social.LimboAuthListener;
import net.elytrium.limboauth.socialaddon.proxy.social.ReloadListener;
import net.elytrium.limboauth.socialaddon.model.*;
import net.elytrium.limboauth.socialaddon.proxy.SocialManager;
import net.elytrium.limboauth.socialaddon.bot.DiscordSocial;
import net.elytrium.limboauth.socialaddon.proxy.utils.GeoIp;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.DaoManager;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.stmt.UpdateBuilder;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.support.ConnectionSource;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.TableUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Plugin(
    id = "limboauth-social-addon",
    name = "LimboAuth Social Addon",
    version = BuildConstants.ADDON_VERSION,
    url = "https://elytrium.net/",
    authors = {
        "Elytrium (https://elytrium.net/)",
    },
    dependencies = {
        @Dependency(id = "limboauth")
    }
)
public class Addon {

  private static final String INFO_BTN = "info";
  private static final String NOTIFY_BTN = "notify";
  private static final String KICK_BTN = "kick";
  private static final String PLUGIN_MINIMUM_VERSION = "1.1.0";

  private static Serializer SERIALIZER;

  private final ProxyServer server;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;
  private final LimboAuth plugin;

  private final Map<String, Integer> codeMap;
  private final Map<String, TempAccount> requestedReverseMap;
  private final Map<Long, CachedRegisteredUser> cachedAccountRegistrations = new ConcurrentHashMap<>();

  private DataManager dataManager;
  private SocialManager socialManager;
  private DiscordCommandListener discordManager;

  private Pattern nicknamePattern;
  private List<List<DiscordSocial.ButtonItem>> keyboard;
  private GeoIp geoIp;
  private ScheduledTask purgeCacheTask;



  static {
    Objects.requireNonNull(org.apache.commons.logging.impl.LogFactoryImpl.class);
    Objects.requireNonNull(org.apache.commons.logging.impl.Log4JLogger.class);
    Objects.requireNonNull(org.apache.commons.logging.impl.Jdk14Logger.class);
    Objects.requireNonNull(org.apache.commons.logging.impl.Jdk13LumberjackLogger.class);
    Objects.requireNonNull(org.apache.commons.logging.impl.SimpleLog.class);
  }

  @Inject
  public Addon(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.metricsFactory = metricsFactory;
    this.dataDirectory = dataDirectory;

    Optional<PluginContainer> container = this.server.getPluginManager().getPlugin("limboauth");
    String version = container.map(PluginContainer::getDescription).flatMap(PluginDescription::getVersion).orElseThrow();

    if (!UpdatesChecker.checkVersion(PLUGIN_MINIMUM_VERSION, version)) {
      throw new IllegalStateException("Incorrect version of LimboAuth plugin, the addon requires version " + PLUGIN_MINIMUM_VERSION + " or newer");
    }

    this.plugin = (LimboAuth) container.flatMap(PluginContainer::getInstance).orElseThrow();
    this.codeMap = new ConcurrentHashMap<>();
    this.requestedReverseMap = new ConcurrentHashMap<>();
  }

  @Subscribe(order = PostOrder.NORMAL)
  public void onProxyInitialization(ProxyInitializeEvent event) throws SQLException {
    this.onReload();
    this.metricsFactory.make(this, 14770);
    if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/LimboAuth-SocialAddon/master/VERSION", Settings.IMP.VERSION)) {
      this.logger.error("****************************************");
      this.logger.warn("The new LimboAuth update was found, please update.");
      this.logger.error("https://github.com/Elytrium/LimboAuth-SocialAddon/releases/");
      this.logger.error("****************************************");
    }
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "LEGACY_AMPERSAND can't be null in velocity.")
  private void load() {
    Settings.IMP.reload(new File(this.dataDirectory.toFile().getAbsoluteFile(), "config.yml"), Settings.IMP.PREFIX);

    ComponentSerializer<Component, Component, String> serializer = Settings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      this.logger.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    this.geoIp = Settings.IMP.MAIN.GEOIP.ENABLED ? new GeoIp(this.dataDirectory) : null;

    if (this.socialManager != null) {
      this.socialManager.stop();
    }

    this.socialManager = new SocialManager();
    this.socialManager.start();

    this.keyboard = List.of(
        List.of(
            new DiscordSocial.ButtonItem(INFO_BTN, Settings.IMP.MAIN.STRINGS.INFO_BTN, DiscordSocial.ButtonItem.Color.PRIMARY)
        ),
        List.of(
            new DiscordSocial.ButtonItem(NOTIFY_BTN, Settings.IMP.MAIN.STRINGS.TOGGLE_NOTIFICATION_BTN, DiscordSocial.ButtonItem.Color.SECONDARY)
        ),
        List.of(
            new DiscordSocial.ButtonItem(KICK_BTN, Settings.IMP.MAIN.STRINGS.KICK_BTN, DiscordSocial.ButtonItem.Color.RED)
        )
    );

    this.socialManager.registerKeyboard(this.keyboard);

    this.socialManager.addButtonEvent(INFO_BTN, (id) -> {
      List<Player> playerList = this.dataManager.players().queryForEq(Player.DISCORD_DB_FIELD, id);

      if (playerList.size() == 0) {
        return;
      }

      Player player = playerList.get(0);
      Optional<com.velocitypowered.api.proxy.Player> proxyPlayer = this.server.getPlayer(player.getNickname());
      String server;
      String ip;
      String location;

      if (proxyPlayer.isPresent()) {
        com.velocitypowered.api.proxy.Player player1 = proxyPlayer.get();
        Optional<ServerConnection> connection = player1.getCurrentServer();

        if (connection.isPresent()) {
          server = connection.get().getServerInfo().getName();
        } else {
          server = Settings.IMP.MAIN.STRINGS.STATUS_OFFLINE;
        }

        ip = player1.getRemoteAddress().getAddress().getHostAddress();
        location = Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("");
      } else {
        server = Settings.IMP.MAIN.STRINGS.STATUS_OFFLINE;
        ip = Settings.IMP.MAIN.STRINGS.STATUS_OFFLINE;
        location = "";
      }

      this.socialManager.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.INFO_MSG,
              player.getNickname(),
              server,
              ip,
              location,
              player.isNotifyEnabled() ? Settings.IMP.MAIN.STRINGS.NOTIFY_ENABLED : Settings.IMP.MAIN.STRINGS.NOTIFY_DISABLED),
          this.keyboard
      );
    });

    this.socialManager.addButtonEvent(NOTIFY_BTN, (id) -> {
      List<Player> playerList = this.dataManager.players().queryForEq(Player.DISCORD_DB_FIELD, id);

      if (playerList.size() == 0) {
        return;
      }

      Player player = playerList.get(0);

      if (player.isNotifyEnabled()) {
        player.setNotifyEnabled(false);
        this.socialManager.broadcastMessage(player,
            Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_DISABLE_SUCCESS, player.getNickname()), this.keyboard
        );
      } else {
        player.setNotifyEnabled(true);
        this.socialManager.broadcastMessage(player,
            Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_ENABLE_SUCCESS, player.getNickname()), this.keyboard
        );
      }

      this.dataManager.players().update(player);
    });

    this.socialManager.addButtonEvent(KICK_BTN, (id) -> {
      List<Player> playerList = this.dataManager.players().queryForEq(Player.DISCORD_DB_FIELD, id);

      if (playerList.size() == 0) {
        return;
      }

      Player player = playerList.get(0);
      Optional<com.velocitypowered.api.proxy.Player> proxyPlayer = this.server.getPlayer(player.getNickname());
      this.plugin.removePlayerFromCache(player.getNickname());

      if (proxyPlayer.isPresent()) {
        proxyPlayer.get().disconnect(Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.KICK_GAME_MESSAGE));
        this.socialManager.broadcastMessage(player,
            Placeholders.replace(Settings.IMP.MAIN.STRINGS.KICK_SUCCESS, player.getNickname()), this.keyboard
        );
      } else {
        this.socialManager.broadcastMessage(player,
            Settings.IMP.MAIN.STRINGS.KICK_IS_OFFLINE.replace("{NICKNAME}", player.getNickname()), this.keyboard
        );
      }

      this.dataManager.players().update(player);
    });


    this.discordManager = new DiscordCommandListener(socialManager, dataManager);
  }

  public void onReload() throws SQLException {
    this.load();
    this.server.getEventManager().unregisterListeners(this);

    ConnectionSource source = this.plugin.getConnectionSource();
    TableUtils.createTableIfNotExists(source, Player.class);
    TableUtils.createTableIfNotExists(source, Activity.class);
    TableUtils.createTableIfNotExists(source, Ban.class);
    TableUtils.createTableIfNotExists(source, History.class);
    this.dataManager = new DataManager(
            socialManager,
            server,
            DaoManager.createDao(source, Player.class),
            DaoManager.createDao(source, Activity.class),
            DaoManager.createDao(source, Ban.class),
            DaoManager.createDao(source, History.class)
    );

    //this.plugin.migrateDb(this.dataManager.players());

    this.server.getEventManager().register(this, new LimboAuthListener(this, this.plugin, this.dataManager, this.socialManager,
        this.keyboard, this.geoIp
    ));
    this.server.getEventManager().register(this, new ReloadListener(this));

    if (this.purgeCacheTask != null) {
      this.purgeCacheTask.cancel();
    }

    this.purgeCacheTask = this.server.getScheduler()
        .buildTask(this, () -> this.checkCache(this.cachedAccountRegistrations, Settings.IMP.MAIN.PURGE_REGISTRATION_CACHE_MILLIS))
        .delay(net.elytrium.limboauth.Settings.IMP.MAIN.PURGE_CACHE_MILLIS, TimeUnit.MILLISECONDS)
        .repeat(net.elytrium.limboauth.Settings.IMP.MAIN.PURGE_CACHE_MILLIS, TimeUnit.MILLISECONDS)
        .schedule();

    CommandManager commandManager = this.server.getCommandManager();
  }

  private void checkCache(Map<?, ? extends CachedUser> userMap, long time) {
    userMap.entrySet().stream()
        .filter(userEntry -> userEntry.getValue().getCheckTime() + time <= System.currentTimeMillis())
        .map(Map.Entry::getKey)
        .forEach(userMap::remove);
  }

  public void unregisterPlayer(String nickname) {
    try {
      List<Player> playersList = this.dataManager.players().queryForEq(Player.NICKNAME_FIELD, nickname);
      Player player = null;
      if(playersList.size() > 0)
        player = playersList.get(0);

      if (player != null) {
        this.socialManager.unregisterHook(player);
        this.dataManager.players().delete(player);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public void linkSocial(String lowercaseNickname, Long id) throws SQLException {
    Player player = this.dataManager.players().queryForId("" + id);
    if (player == null) {
      Settings.IMP.MAIN.AFTER_LINKAGE_COMMANDS.forEach(command ->
          this.server.getCommandManager().executeAsync(p -> Tristate.TRUE, command.replace("{NICKNAME}", lowercaseNickname)));

      this.dataManager.players().create(new Player(lowercaseNickname));
    } else if (player.getDiscordID() != null) {
      this.socialManager.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.LINK_ALREADY);
      return;
    }

    UpdateBuilder<Player, String> updateBuilder = this.dataManager.players().updateBuilder();
    updateBuilder.where().eq(Player.NICKNAME_FIELD, lowercaseNickname);
    updateBuilder.updateColumnValue(Player.DISCORD_DB_FIELD, id);
    updateBuilder.update();
  }

  public Integer getCode(String nickname) {
    return this.codeMap.get(nickname);
  }

  public TempAccount getTempAccount(String nickname) {
    return this.requestedReverseMap.get(nickname);
  }

  public void removeCode(String nickname) {
    this.requestedReverseMap.remove(nickname);
    this.codeMap.remove(nickname);
  }

  public SocialManager getSocialManager() {
    return this.socialManager;
  }

  public ProxyServer getServer() {
    return this.server;
  }

  public List<List<DiscordSocial.ButtonItem>> getKeyboard() {
    return this.keyboard;
  }

  public static class TempAccount {

    private final String dbField;
    private final long id;

    public TempAccount(String dbField, long id) {
      this.dbField = dbField;
      this.id = id;
    }

    public String getDbField() {
      return this.dbField;
    }

    public long getId() {
      return this.id;
    }

  }

  private static class CachedUser {

    private final long checkTime = System.currentTimeMillis();

    public long getCheckTime() {
      return this.checkTime;
    }
  }

  private static class CachedRegisteredUser extends CachedUser {

    private int registrationAmount;

    public int getRegistrationAmount() {
      return this.registrationAmount;
    }

    public void incrementRegistrationAmount() {
      this.registrationAmount++;
    }
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }
}
