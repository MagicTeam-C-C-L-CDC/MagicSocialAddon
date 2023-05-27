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

package ru.magicteam.proxy.social.controller.discord;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.event.TaskEvent;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.Controller;
import ru.magicteam.proxy.social.controller.discord.register.DiscordCommands;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonItem;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonVisibility;
import ru.magicteam.proxy.social.controller.proxy.ProxyController;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;
import ru.magicteam.proxy.social.controller.proxy.social.SocialInitializationException;

public class DiscordController extends Controller {

  private JDA jda;

  private List<Role> requiredRoles = new LinkedList<>();

  public static DiscordBuilder<HashMap<DType, Consumer<ButtonInteractionEvent>>> builder = () -> {
    HashMap<DType, Consumer<ButtonInteractionEvent>> map = new HashMap<>();

    map.put(DType.BUILD, e -> {

    });

    return map;
  };

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
      ProxyController.AuthSession authSession = this.sessions.get(player.getNickname());
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

  public DiscordController(ModelAPI api) {
    super(api);
  }

  public void start() throws InterruptedException {
    JDABuilder jdaBuilder = JDABuilder.create(Settings.IMP.MAIN.DISCORD.TOKEN, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS);
    this.jda = jdaBuilder
            .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
            .setActivity(Settings.IMP.MAIN.DISCORD.ACTIVITY_ENABLED
                    ? Activity.of(Settings.IMP.MAIN.DISCORD.ACTIVITY_TYPE, Settings.IMP.MAIN.DISCORD.ACTIVITY_NAME, Settings.IMP.MAIN.DISCORD.ACTIVITY_URL)
                    : null)
            .build()
            .awaitReady();

    for (Object requiredRole : Settings.IMP.MAIN.DISCORD.REQUIRED_ROLES) {
      Role roleById;
      if (requiredRole instanceof Long)
        roleById = jda.getRoleById((Long) requiredRole);
      else if (requiredRole instanceof String)
        roleById = jda.getRoleById((String) requiredRole);
      else
        throw new IllegalArgumentException("Required-roles entry cannot be of class " + requiredRole.getClass().getName());

      if (roleById != null)
        requiredRoles.add(roleById);
    }

    jda.updateCommands().addCommands(DiscordCommands.commandBuilder.build()).queue();
    jda.addEventListener(new DiscordListener(api));
  }

  public void stop() {
    if (this.jda != null) {
      this.jda.shutdown();
    }
  }
/*
  protected void proceedMessage(Long id, String message) {
    this.onMessageReceived.accept(id, message);
  }

  protected void proceedButton(Long id, String message) {
    this.onButtonClicked.accept(id, message);
  }
 */

  public void sendMessage(Long id, String content, List<List<ButtonItem>> buttons, ButtonVisibility visibility) {
    User user = this.jda.retrieveUserById(id).complete();
    if (user == null) {
      return;
    }

    List<ActionRow> actionRowList = buttons.stream().map(row ->
        ActionRow.of(row.stream().map(e -> {
          ButtonStyle style;

          switch (e.color()) {
            case RED:
              style = ButtonStyle.DANGER;
              break;
            case GREEN:
              style = ButtonStyle.SUCCESS;
              break;
            case LINK:
              style = ButtonStyle.LINK;
              break;
            case PRIMARY:
              style = ButtonStyle.PRIMARY;
              break;
            case SECONDARY:
            default:
              style = ButtonStyle.SECONDARY;
              break;
          }

          return Button.of(style, e.id(), e.value());
        }).collect(Collectors.toList()))
    ).collect(Collectors.toList());

    user.openPrivateChannel()
        .submit()
        .thenAccept(privateChannel -> privateChannel
            .sendMessage(content)
            .setComponents(actionRowList)
            .submit()
            .exceptionally(e -> {
              if (Settings.IMP.MAIN.DEBUG) {
                e.printStackTrace(); // printStackTrace is necessary there
              }
              return null;
            }))
        .exceptionally(e -> {
          if (Settings.IMP.MAIN.DEBUG) {
            e.printStackTrace(); // printStackTrace is necessary there
          }
          return null;
        });
  }

  public void sendMessage(Player player, String content, List<List<ButtonItem>> buttons, ButtonVisibility visibility) {
    this.sendMessage(player.getDiscordID(), content, buttons, visibility);
  }

  public boolean canSend(Player player) {
    return player.getDiscordID() != null;
  }


  public void broadcastMessage(Player player, String message, List<List<ButtonItem>> item) {
    this.broadcastMessage(player, message, item, ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message,
                               List<List<ButtonItem>> item, ButtonVisibility visibility) {
    sendMessage(player, message, item, visibility);
  }

  public void broadcastMessage(Long id, String message,
                               List<List<ButtonItem>> item) {
    sendMessage(id, message, item, ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message) {
    sendMessage(player, message, Collections.emptyList(), ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Long id, String message) {
    sendMessage(id, message, Collections.emptyList(), ButtonVisibility.DEFAULT);
  }


}