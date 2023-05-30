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
import com.velocitypowered.api.proxy.ServerConnection;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.elytrium.commons.config.Placeholders;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.event.*;
import net.kyori.adventure.text.Component;
import ru.magicteam.proxy.social.Addon;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.discord.DType;
import ru.magicteam.proxy.social.controller.discord.DiscordController;
import ru.magicteam.proxy.social.controller.discord.DiscordEvent;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonVisibility;
import ru.magicteam.proxy.social.controller.discord.ui.keyboard.DiscordKeyboards;
import ru.magicteam.proxy.social.controller.proxy.handler.PreLoginLimboSessionHandler;
import ru.magicteam.proxy.social.controller.proxy.utils.AuthSession;
import ru.magicteam.proxy.social.controller.proxy.utils.GeoIp;
import ru.magicteam.proxy.social.model.Ban;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyController {

  private final Component blockedAccount = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.BLOCK_KICK_MESSAGE);
  private final Component unregistered = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.REGISTER_INCORRECT_NICKNAME);
  private final Component askedKick = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_KICK_MESSAGE);
  private final Component askedValidate = Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE_GAME);

  private final LimboAuth plugin;
  private final ModelAPI api;
  private final DiscordController discordController;
  private final Map<Long, AuthSession> sessions = new ConcurrentHashMap<>();

  private final GeoIp geoIp;

  public ProxyController(LimboAuth plugin, ModelAPI api, DiscordController discordController, GeoIp geoIp) {
    this.plugin = plugin;
    this.api = api;
    this.discordController = discordController;
    this.geoIp = geoIp;

    discordController.subscribe (DType.BUTTON_PLAYER_JOIN_REQUEST_NO, (e) -> {
      if(e.type != DiscordEvent.EventType.BUTTON)
        return;

      Long id = e.id;
      if (sessions.containsKey(id)) {
        sessions.get(id).getEvent().completeAndCancel(this.askedKick);
        discordController.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.NOTIFY_WARN, DiscordKeyboards.infoKeyboard.build(id));
      } else
        ((ButtonInteractionEvent)e.event).reply(Settings.IMP.MAIN.STRINGS.SOCIAL_EXCEPTION_CAUGHT);
    });

    discordController.subscribe(DType.BUTTON_PLAYER_JOIN_REQUEST_YES, (e) -> {
      if(e.type != DiscordEvent.EventType.BUTTON)
        return;

      Long id = e.id;

      if(!sessions.containsKey(id))
        return;

      AuthSession authSession = this.sessions.get(id);
      LimboPlayer limboPlayer = authSession.getPlayer();
      limboPlayer.disconnect();
      com.velocitypowered.api.proxy.Player proxyPlayer = limboPlayer.getProxyPlayer();
      this.plugin.cacheAuthUser(proxyPlayer);
      try {
        this.plugin.updateLoginData(proxyPlayer);
      } catch (SQLException ex) {
        ex.printStackTrace();
      }

      discordController.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.NOTIFY_THANKS, DiscordKeyboards.infoKeyboard.build(id));
    });

    discordController.subscribe(DType.BUTTON_PLAYER_INFO, e -> {
      if(e.type != DiscordEvent.EventType.BUTTON)
        return;

      Optional<Player> optionalPlayer = api.queryPlayerByID(e.id);

      if (optionalPlayer.isEmpty())
        return;

      Player player = optionalPlayer.get();
      Optional<com.velocitypowered.api.proxy.Player> proxyPlayer = plugin.getServer().getPlayer(player.getNickname());
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

      discordController.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.INFO_MSG,
                      player.getNickname(),
                      server,
                      ip,
                      location,
                      player.isNotifyEnabled()),
              DiscordKeyboards.infoKeyboard.build(e.id)
      );
    });

    discordController.subscribe(DType.BUTTON_PLAYER_NOTIFY, e -> {
      if(e.type != DiscordEvent.EventType.BUTTON)
        return;

      Optional<Player> optionalPlayer = api.queryPlayerByID(e.id);

      if (optionalPlayer.isEmpty())
        return;

      Player player = optionalPlayer.get();

      if (player.isNotifyEnabled()) {
        player.setNotifyEnabled(false);
        discordController.broadcastMessage(player,
                Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_DISABLE_SUCCESS, player.getNickname()),
                DiscordKeyboards.infoKeyboard.build(e.id)
        );
      } else {
        player.setNotifyEnabled(true);
        discordController.broadcastMessage(player,
                Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_ENABLE_SUCCESS, player.getNickname()),
                DiscordKeyboards.infoKeyboard.build(e.id)
        );
      }


      api.updatePlayer(player);
    });

    discordController.subscribe(DType.BUTTON_PLAYER_KICK, e -> {
      if(e.type != DiscordEvent.EventType.BUTTON)
        return;

      Optional<Player> optionalPlayer = api.queryPlayerByID(e.id);

      if (optionalPlayer.isEmpty())
        return;

      Player player = optionalPlayer.get();
      Optional<com.velocitypowered.api.proxy.Player> proxyPlayer = plugin.getServer().getPlayer(player.getNickname());
      this.plugin.removePlayerFromCache(player.getNickname());

      if (proxyPlayer.isPresent()) {
        proxyPlayer.get().disconnect(Addon.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.KICK_GAME_MESSAGE));
        discordController.broadcastMessage(player,
                Placeholders.replace(Settings.IMP.MAIN.STRINGS.KICK_SUCCESS, player.getNickname()),
                DiscordKeyboards.infoKeyboard.build(e.id)
        );
      } else discordController.broadcastMessage(player,
                Settings.IMP.MAIN.STRINGS.KICK_IS_OFFLINE.replace("{NICKNAME}", player.getNickname()),
                DiscordKeyboards.infoKeyboard.build(e.id)
      );

      api.updatePlayer(player);
    });
  }

  @Subscribe
  public void onAuth(PreAuthorizationEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = event.getPlayer();

    Optional<Player> optionalPlayer = api.queryPlayerByNickName(proxyPlayer.getUsername());
    if(optionalPlayer.isEmpty()){
      proxyPlayer.disconnect(unregistered);
      return;
    }

    Player player = optionalPlayer.get();
    List<Ban> banInfo = new LinkedList<>(api.queryBan(player.getDiscordID()));

    /**
     * Нужно поправить расчет бана
     */
    boolean deny = banInfo.stream().map(b -> {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ss dd:mm:ss");
      try {
        return dateFormat.parse(b.getExpire_timestamp()).getTime() < LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }).reduce((b1, b2) -> b1 || b2).orElse(false);

    if (deny)
      event.cancel(this.blockedAccount);

    event.setResult(TaskEvent.Result.WAIT);
    this.plugin.getAuthServer().spawnPlayer(proxyPlayer, new PreLoginLimboSessionHandler(this, event, player));
  }

  @Subscribe
  public void onAuthCompleted(PostAuthorizationEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = event.getPlayer().getProxyPlayer();

    Optional<Player> optionalPlayer = api.queryPlayerByNickName(proxyPlayer.getUsername());
    if(optionalPlayer.isEmpty())
      return;

    Player player = optionalPlayer.get();
    event.setResult(TaskEvent.Result.WAIT);

    this.sessions.put(player.getDiscordID(), new AuthSession(event, event.getPlayer()));

    String ip = proxyPlayer.getRemoteAddress().getAddress().getHostAddress();
    discordController.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE,
                    ip, Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("")),
            DiscordKeyboards.yes_no_keyboard.build(player.getDiscordID()), ButtonVisibility.PREFER_INLINE);

    proxyPlayer.sendMessage(this.askedValidate);
  }

  @Subscribe
  public void onGameProfile(PlayerChooseInitialServerEvent event) {
    Optional<Player> optionalPlayer = api.queryPlayerByNickName(event.getPlayer().getUsername());
    if(optionalPlayer.isEmpty())
      return;

    Player player = optionalPlayer.get();
    if (player.isNotifyEnabled()) {
      String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
      discordController.broadcastMessage(player, Placeholders.replace(Settings.IMP.MAIN.STRINGS.NOTIFY_JOIN,
          ip,
          Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("")),
              DiscordKeyboards.infoKeyboard.build(player.getDiscordID()));
    }
  }

  @Subscribe
  public void onPlayerLeave(DisconnectEvent event) {
    if (event.getPlayer().getCurrentServer().isEmpty())
      return;

    Optional<Player> optionalPlayer = api.queryPlayerByNickName(event.getPlayer().getUsername());
    if(optionalPlayer.isEmpty())
      return;
    Player player = optionalPlayer.get();
    if (player.isNotifyEnabled())
      discordController.broadcastMessage(
              player,
              Settings.IMP.MAIN.STRINGS.NOTIFY_LEAVE,
              DiscordKeyboards.infoKeyboard.build(player.getDiscordID())
      );

    this.sessions.remove(player.getDiscordID());
  }

  public void authMainHook(Player player, LimboPlayer limboPlayer, TaskEvent event) {
    com.velocitypowered.api.proxy.Player proxyPlayer = limboPlayer.getProxyPlayer();
    this.sessions.put(player.getDiscordID(), new AuthSession(event, limboPlayer));

    String ip = proxyPlayer.getRemoteAddress().getAddress().getHostAddress();
    discordController.broadcastMessage(
            player,
            Placeholders.replace(
                    Settings.IMP.MAIN.STRINGS.NOTIFY_ASK_VALIDATE,
                    ip,
                    Optional.ofNullable(this.geoIp).map(nonNullGeo -> nonNullGeo.getLocation(ip)).orElse("")
            ),
            DiscordKeyboards.yes_no_keyboard.build(player.getDiscordID()),
            ButtonVisibility.PREFER_INLINE
    );

    proxyPlayer.sendMessage(this.askedValidate);
  }
}
