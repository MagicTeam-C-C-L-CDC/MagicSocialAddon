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

package net.elytrium.limboauth.socialaddon.proxy.social;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import net.elytrium.commons.config.Placeholders;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.event.*;
import net.elytrium.limboauth.socialaddon.Addon;
import net.elytrium.limboauth.socialaddon.Settings;
import net.elytrium.limboauth.socialaddon.bot.DiscordSocial;
import net.elytrium.limboauth.socialaddon.proxy.SocialManager;
import net.elytrium.limboauth.socialaddon.proxy.handler.PreLoginLimboSessionHandler;
import net.elytrium.limboauth.socialaddon.model.Ban;
import net.elytrium.limboauth.socialaddon.model.DataManager;
import net.elytrium.limboauth.socialaddon.model.Player;
import net.elytrium.limboauth.socialaddon.proxy.utils.GeoIp;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LimboAuthListener {

  private static final String ASK_NO_BTN = "ask_no";
  private static final String ASK_YES_BTN = "ask_yes";

  private final Component blockedAccount = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.BLOCK_KICK_MESSAGE);
  private final Component unregistered = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.REGISTER_INCORRECT_NICKNAME);
  private final Component askedKick = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_KICK_MESSAGE);
  private final Component askedValidate = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE_GAME);

  private final Addon addon;
  private final LimboAuth plugin;
  private final DataManager dataManager;
  private final SocialManager socialManager;

  private final List<List<DiscordSocial.ButtonItem>> yesNoButtons;
  private final List<List<DiscordSocial.ButtonItem>> keyboard;
  private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

  private final GeoIp geoIp;
  private final boolean auth2faWithoutPassword = Settings.IMP.MAIN.AUTH_2FA_WITHOUT_PASSWORD;

  public LimboAuthListener(Addon addon, LimboAuth plugin, DataManager dataManager, SocialManager socialManager,
                           List<List<DiscordSocial.ButtonItem>> keyboard, GeoIp geoIp) {
    this.addon = addon;
    this.plugin = plugin;
    this.dataManager = dataManager;
    this.socialManager = socialManager;
    this.keyboard = keyboard;
    this.geoIp = geoIp;

    this.yesNoButtons = List.of(
            List.of(
                    new DiscordSocial.ButtonItem(ASK_NO_BTN, Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_NO, DiscordSocial.ButtonItem.Color.RED),
                    new DiscordSocial.ButtonItem(ASK_YES_BTN, Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_YES, DiscordSocial.ButtonItem.Color.GREEN)
            )
    );
    this.socialManager.registerKeyboard(this.yesNoButtons);
    this.socialManager.removeButtonEvent(ASK_NO_BTN);
    this.socialManager.removeButtonEvent(ASK_YES_BTN);

    this.socialManager.addButtonEvent(ASK_NO_BTN, (id) -> {
      Player player = this.queryPlayer(id);

      if (player != null && this.sessions.containsKey(player.getNickname())) {
        this.sessions.get(player.getNickname()).getEvent().completeAndCancel(this.askedKick);
        this.socialManager.broadcastMessage(player, Settings.IMP.MAIN.STRINGS.NOTIFY_WARN, this.keyboard);
      }
    });

    this.socialManager.addButtonEvent(ASK_YES_BTN, (id) -> {
      Player player = this.queryPlayer(id);

      if (player != null && this.sessions.containsKey(player.getNickname())) {
        AuthSession authSession = this.sessions.get(player.getNickname());
        if (this.auth2faWithoutPassword) {
          LimboPlayer limboPlayer = authSession.getPlayer();
          limboPlayer.disconnect();
          com.velocitypowered.api.proxy.Player proxyPlayer = limboPlayer.getProxyPlayer();
          this.plugin.cacheAuthUser(proxyPlayer);
          this.plugin.updateLoginData(proxyPlayer);
        } else {
          authSession.getEvent().complete(TaskEvent.Result.NORMAL);
        }

        this.socialManager.broadcastMessage(player, Settings.IMP.MAIN.STRINGS.NOTIFY_THANKS, this.keyboard);
      }
    });
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
        this.yesNoButtons, DiscordSocial.ButtonVisibility.PREFER_INLINE);

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
