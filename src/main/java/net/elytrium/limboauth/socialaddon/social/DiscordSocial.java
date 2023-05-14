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

package net.elytrium.limboauth.socialaddon.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.elytrium.limboauth.socialaddon.Settings;
import net.elytrium.limboauth.socialaddon.model.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordSocial{

  private JDA jda;
  private List<RoleAction> onPlayerAddedRoleActions;
  private List<RoleAction> onPlayerRemovedRoleActions;

  private final SocialMessageListener onMessageReceived;
  private final SocialButtonListener onButtonClicked;

  public DiscordSocial(SocialMessageListener onMessageReceived, SocialButtonListener onButtonClicked) {
    this.onMessageReceived = onMessageReceived;
    this.onButtonClicked = onButtonClicked;
  }

  public void start() throws SocialInitializationException {
    try {
      JDABuilder jdaBuilder;
      if (Settings.IMP.MAIN.DISCORD.GUILD_MEMBER_CACHE_ENABLED) {
        jdaBuilder = JDABuilder.create(Settings.IMP.MAIN.DISCORD.TOKEN, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS);
      } else {
        jdaBuilder = JDABuilder.create(Settings.IMP.MAIN.DISCORD.TOKEN, GatewayIntent.DIRECT_MESSAGES);
      }

      this.jda = jdaBuilder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
          .setActivity(Settings.IMP.MAIN.DISCORD.ACTIVITY_ENABLED
              ? Activity.of(Settings.IMP.MAIN.DISCORD.ACTIVITY_TYPE, Settings.IMP.MAIN.DISCORD.ACTIVITY_NAME, Settings.IMP.MAIN.DISCORD.ACTIVITY_URL)
              : null)
          .build().awaitReady();
    } catch (LoginException | InterruptedException e) {
      throw new SocialInitializationException(e);
    }

    this.jda.addEventListener(new Listener(this.jda, this::proceedMessage, this::proceedButton));
    this.onPlayerAddedRoleActions = Settings.IMP.MAIN.DISCORD.ON_PLAYER_ADDED.stream()
        .map(e -> e.split(" "))
        .map(RoleAction::new)
        .collect(Collectors.toList());
    this.onPlayerRemovedRoleActions = Settings.IMP.MAIN.DISCORD.ON_PLAYER_REMOVED.stream()
        .map(e -> e.split(" "))
        .map(RoleAction::new)
        .collect(Collectors.toList());
  }

  public boolean isEnabled() {
    return Settings.IMP.MAIN.DISCORD.ENABLED;
  }

  public void stop() {
    if (this.jda != null) {
      this.jda.shutdown();
    }
  }

  public String getDbField() {
    return Player.DISCORD_DB_FIELD;
  }

  public void onPlayerAdded(Long id) {
    this.onPlayerAddedRoleActions.forEach(action -> action.doAction(id));
  }

  public void onPlayerRemoved(Player player) {
    this.onPlayerRemovedRoleActions.forEach(action -> action.doAction(player.getDiscordID()));
  }
  protected void proceedMessage(Long id, String message) {
    this.onMessageReceived.accept(id, message);
  }

  protected void proceedButton(Long id, String message) {
    this.onButtonClicked.accept(id, message);
  }

  public void sendMessage(Long id, String content, List<List<ButtonItem>> buttons, ButtonVisibility visibility) {
    User user = this.jda.retrieveUserById(id).complete();
    if (user == null) {
      return;
    }

    List<ActionRow> actionRowList = buttons.stream().map(row ->
        ActionRow.of(row.stream().map(e -> {
          ButtonStyle style;

          switch (e.getColor()) {
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

          return Button.of(style, e.getId(), e.getValue());
        }).collect(Collectors.toList()))
    ).collect(Collectors.toList());

    user.openPrivateChannel()
        .submit()
        .thenAccept(privateChannel -> privateChannel
            .sendMessage(content)
            .setActionRows(actionRowList)
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

  private static class Listener extends ListenerAdapter {

    private final List<Role> requiredRoles;
    private final SocialMessageListener onMessageReceived;
    private final SocialButtonListener onButtonClicked;

    Listener(JDA jda, SocialMessageListener onMessageReceived, SocialButtonListener onButtonClicked) {
      List<Role> list = new ArrayList<>();
      for (Object requiredRole : Settings.IMP.MAIN.DISCORD.REQUIRED_ROLES) {
        Role roleById;
        if (requiredRole instanceof Long) {
          roleById = jda.getRoleById((Long) requiredRole);
        } else if (requiredRole instanceof String) {
          roleById = jda.getRoleById((String) requiredRole);
        } else {
          throw new IllegalArgumentException("Required-roles entry cannot be of class " + requiredRole.getClass().getName());
        }

        if (roleById != null) {
          list.add(roleById);
        }
      }
      this.requiredRoles = list;
      this.onMessageReceived = onMessageReceived;
      this.onButtonClicked = onButtonClicked;
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
      User user = event.getAuthor();
      if (user.getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
        return;
      }

      for (Role role : this.requiredRoles) {
        Member member = role.getGuild().retrieveMember(user).complete();
        if (member == null || !member.getRoles().contains(role)) {
          user.openPrivateChannel()
              .submit()
              .thenAccept(privateChannel -> privateChannel.sendMessage(Settings.IMP.MAIN.DISCORD.NO_ROLES_MESSAGE).queue());
          return;
        }
      }

      this.onMessageReceived.accept(event.getAuthor().getIdLong(), event.getMessage().getContentRaw());
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
      event.deferEdit().queue();
      this.onButtonClicked.accept(event.getUser().getIdLong(), event.getButton().getId());
    }

  }

  private final class RoleAction {
    private final RoleActionType action;
    private final Role role;

    private RoleAction(String[] serializedAction) {
      this(RoleActionType.valueOf(serializedAction[0].toUpperCase(Locale.ROOT)), serializedAction[1]);
    }

    private RoleAction(RoleActionType action, String roleId) {
      this(action, DiscordSocial.this.jda.getRoleById(roleId));
    }

    private RoleAction(RoleActionType action, Role role) {
      this.action = action;
      this.role = role;
    }

    public void doAction(Long id) {
      this.action.doAction(this.role, id);
    }
  }

  private enum RoleActionType {
    ADDROLE((role, id) -> role.getGuild().addRoleToMember(id, role).queue()),
    REMROLE((role, id) -> role.getGuild().removeRoleFromMember(id, role).queue());

    private final BiConsumer<Role, Long> doAction;

    RoleActionType(BiConsumer<Role, Long> doAction) {
      this.doAction = doAction;
    }

    public void doAction(Role role, long userId) {
      this.doAction.accept(role, userId);
    }
  }
  public enum ButtonVisibility {

    DEFAULT,
    PREFER_INLINE,
    PREFER_KEYBOARD
  }

  public static class ButtonItem {

    private final String id;
    private final String value;
    private final Color color;

    public ButtonItem(String id, String value, Color color) {
      this.id = id;
      this.value = value;
      this.color = color;
    }

    public String getId() {
      return this.id;
    }

    public String getValue() {
      return this.value;
    }

    public Color getColor() {
      return this.color;
    }

    public enum Color {

      GREEN,
      RED,
      PRIMARY,
      SECONDARY,
      LINK
    }

  }
}