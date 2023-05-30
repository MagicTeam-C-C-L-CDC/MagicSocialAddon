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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.Controller;
import ru.magicteam.proxy.social.controller.discord.register.DiscordCommands;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonItem;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonVisibility;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordController extends Controller {

  private JDA jda;

  private List<Role> requiredRoles = new LinkedList<>();

  private final HashMap<DType, List<Consumer<DiscordEvent>>> events = new HashMap<>();

  public DiscordController(ModelAPI api) {
    super(api);
  }

  public void subscribe(DType type, Consumer<DiscordEvent> consumer){
    if(events.containsKey(type))
      events.get(type).add(consumer);
    else events.put(type, List.of(consumer));
  }

  public void callEvent(Long id, DType type, DiscordEvent.EventType eventType, Event event){
    events.get(type).forEach((e) -> new DiscordEvent(id, eventType, event));
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

    jda.updateCommands().addCommands(DiscordCommands.commands).queue();
    jda.addEventListener(new DiscordListener(api, this));
  }

  public void stop() {
    if (this.jda != null) {
      this.jda.shutdown();
    }
  }

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