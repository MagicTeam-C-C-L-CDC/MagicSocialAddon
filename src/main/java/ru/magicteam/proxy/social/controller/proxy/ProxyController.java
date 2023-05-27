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

package ru.magicteam.proxy.social.controller.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import net.elytrium.commons.config.Placeholders;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.event.*;
import ru.magicteam.proxy.social.Addon;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.discord.DiscordController;
import ru.magicteam.proxy.social.controller.proxy.handler.PreLoginLimboSessionHandler;
import ru.magicteam.proxy.social.model.Ban;
import ru.magicteam.proxy.social.model.DataModel;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;
import ru.magicteam.proxy.social.controller.proxy.utils.GeoIp;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyController {

  private final Component blockedAccount = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.BLOCK_KICK_MESSAGE);
  private final Component unregistered = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.REGISTER_INCORRECT_NICKNAME);
  private final Component askedKick = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_KICK_MESSAGE);
  private final Component askedValidate = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE_GAME);

  private final LimboAuth plugin;
  private final ModelAPI api;
  private final DiscordController discordController;
  private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

  private final GeoIp geoIp;
  private final boolean auth2faWithoutPassword = Settings.IMP.MAIN.AUTH_2FA_WITHOUT_PASSWORD;

  public ProxyController(LimboAuth plugin, ModelAPI api, DiscordController discordController, GeoIp geoIp) {
    this.plugin = plugin;
    this.api = api;
    this.discordController = discordController;
    this.geoIp = geoIp;
  }

  @Subscribe
  public void onAuth(PreAuthorizationEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = event.getPlayer();
    Player player = this.queryPlayer(proxyPlayer);
    List<Ban> banInfo = new LinkedList<>();

    try {
      banInfo.addAll(dataManager.ban().queryForEq(Ban.DISCORD_DB_FIELD, player.getDiscordID()));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    boolean allow = banInfo.stream().map(b -> {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ss dd:mm:ss");
      try {
        return dateFormat.parse(b.getExpire_timestamp()).getTime() < LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }).reduce((b1, b2) -> b1 && b2).orElse(true);

    if (player != null && allow)
      event.cancel(this.blockedAccount);

    if (this.auth2faWithoutPassword) {
      if (player != null) {
        event.setResult(TaskEvent.Result.WAIT);
        this.plugin.getAuthServer().spawnPlayer(proxyPlayer, new PreLoginLimboSessionHandler(this, event, player));
      }
    }
  }

  @Subscribe
  public void onAuthCompleted(PostAuthorizationEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = event.getPlayer().getProxyPlayer();
    if (!this.auth2faWithoutPassword) {
      Player player = this.queryPlayer(proxyPlayer);

      if (player != null) {
        event.setResult(TaskEvent.Result.WAIT);
        this.authMainHook(player, event.getPlayer(), event);
      }
    }

  }

  public void authMainHook(Player player, LimboPlayer limboPlayer, TaskEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = limboPlayer.getProxyPlayer();
    this.sessions.put(player.getNickname(), new AuthSession(event, limboPlayer));

    String ip = proxyPlayer.getRemoteAddress().getAddress().getHostAddress();
    this.socialManager.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE,
            ip, Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("")),
        this.yesNoButtons, DiscordController.ButtonVisibility.PREFER_INLINE);

    proxyPlayer.sendMessage(this.askedValidate);
  }

  @Subscribe
  public void onRegisterCompleted(PostRegisterEvent event) {

  }

  @Subscribe
  public void onGameProfile(PlayerChooseInitialServerEvent event) {
    Player player = this.queryPlayer(event.getPlayer());
    if (player != null && Settings.IMP.MAIN.ENABLE_NOTIFY && player.isNotifyEnabled()) {
      String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
      this.socialManager.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_JOIN,
          ip,
          Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("")), this.keyboard);
    }
  }

  @Subscribe
  public void onPlayerLeave(DisconnectEvent event) {
    if (event.getPlayer().getCurrentServer().isEmpty()) {
      return;
    }

    Player player = this.queryPlayer(event.getPlayer());
    if (player != null) {
      if (Settings.IMP.MAIN.ENABLE_NOTIFY && player.isNotifyEnabled()) {
        this.socialManager.broadcastMessage(player, Settings.IMP.MAIN.STRINGS.NOTIFY_LEAVE, this.keyboard);
      }

      this.sessions.remove(player.getNickname());
    }
  }

  @Subscribe
  public void onUnregister(AuthUnregisterEvent event) {
    this.addon.unregisterPlayer(event.getNickname());
  }

  private boolean playerExists(com.velocitypowered.api.proxy.Player player) {
    try {
      return this.dataManager.players().idExists(player.getUsername().toLowerCase(Locale.ROOT));
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private Player queryPlayer(com.velocitypowered.api.proxy.Player player) {
    try {
      List<Player> players = this.dataManager.players().queryForEq(Player.NICKNAME_FIELD, player.getUsername());
      if(players.size() > 0)
        return players.get(0);
      else player.disconnect(unregistered);
      return null;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private void addPlayerActivity(Player player, String action){

  }

  private Player queryPlayer(Long id) {
    try {
      List<Player> l = this.dataManager.players().queryForEq(Player.DISCORD_DB_FIELD, id);

      if (l.size() == 0) {
        return null;
      }

      return l.get(0);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class AuthSession {
    private final TaskEvent event;
    private final LimboPlayer player;

    private AuthSession(TaskEvent event, LimboPlayer player) {
      this.event = event;
      this.player = player;
    }

    public TaskEvent getEvent() {
      return this.event;
    }

    public LimboPlayer getPlayer() {
      return this.player;
    }
  }
}
