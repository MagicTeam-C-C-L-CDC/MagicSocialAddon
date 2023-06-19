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

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.event.AuthPluginReloadEvent;
import net.elytrium.limboauth.socialaddon.BuildConstants;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.DaoManager;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.support.ConnectionSource;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.TableUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import ru.magicteam.proxy.social.controller.discord.DiscordController;
import ru.magicteam.proxy.social.controller.google.GoogleFormController;
import ru.magicteam.proxy.social.controller.proxy.ProxyController;
import ru.magicteam.proxy.social.controller.proxy.utils.GeoIp;
import ru.magicteam.proxy.social.model.*;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

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


  private static final String PLUGIN_MINIMUM_VERSION = "1.1.0";

  private static Serializer SERIALIZER;

  private final ProxyServer server;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;
  private final LimboAuth plugin;

  private DataModel dataManager;
  private DiscordController discordController;
  private GoogleFormController googleFormController;

  private GeoIp geoIp;

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

  }

  @Subscribe(order = PostOrder.NORMAL)
  public void onProxyInitialization(ProxyInitializeEvent event) throws SQLException {
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
    } else
      setSerializer(new Serializer(serializer));

    this.geoIp = Settings.IMP.MAIN.GEOIP.ENABLED ? new GeoIp(this.dataDirectory) : null;
  }

  @Subscribe
  public void onAuthReload(AuthPluginReloadEvent event) throws SQLException {
    logger.info("Magic social addon onAuthReload");

    this.load();
    this.server.getEventManager().unregisterListeners(this);

    ConnectionSource source = this.plugin.getConnectionSource();
    TableUtils.createTableIfNotExists(source, Player.class);
    TableUtils.createTableIfNotExists(source, Activity.class);
    TableUtils.createTableIfNotExists(source, Ban.class);
    TableUtils.createTableIfNotExists(source, History.class);
    TableUtils.createTableIfNotExists(source, GoogleFormSession.class);
    this.dataManager = new DataModel(
            DaoManager.createDao(source, Player.class),
            DaoManager.createDao(source, Activity.class),
            DaoManager.createDao(source, Ban.class),
            DaoManager.createDao(source, History.class),
            DaoManager.createDao(source, GoogleFormSession.class)
    );

    //this.plugin.migrateDb(this.dataManager.players());

    this.discordController = new DiscordController(dataManager, logger);
    try {
      this.discordController.start();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    this.server.getEventManager().register(this, new ProxyController(
            this.plugin,
            this.dataManager,
            this.discordController,
            this.geoIp
    ));

    if(this.googleFormController == null) {
      this.googleFormController = new GoogleFormController(dataManager, discordController, logger);
      this.googleFormController.start();
    }
  }
/*
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
    updateBuilder.updateColumnValue(Player.ID_FIELD, id);
    updateBuilder.update();
  }

 */


  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }
}
