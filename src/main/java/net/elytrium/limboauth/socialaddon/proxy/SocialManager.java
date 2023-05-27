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

package net.elytrium.limboauth.socialaddon.proxy;

import net.elytrium.commons.config.Placeholders;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.socialaddon.Addon;
import net.elytrium.limboauth.socialaddon.Settings;
import net.elytrium.limboauth.socialaddon.model.Player;
import net.elytrium.limboauth.socialaddon.bot.DiscordSocial;
import net.elytrium.limboauth.socialaddon.proxy.social.SocialButtonListenerAdapter;
import net.elytrium.limboauth.socialaddon.proxy.social.SocialInitializationException;
import net.elytrium.limboauth.socialaddon.proxy.social.SocialMessageListenerAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SocialManager {

  private final DiscordSocial discordSocial;
  private final LinkedList<SocialMessageListenerAdapter> messageEvents = new LinkedList<>();
  private final HashMap<String, SocialButtonListenerAdapter> buttonEvents = new HashMap<>();
  private final HashMap<String, String> buttonIdMap = new HashMap<>();


  public SocialManager() {
    discordSocial = new DiscordSocial(this::onMessageReceived, this::onButtonClicked);
    addMessageEvent((dbField, id, message) -> {

    });
  }

  private void onMessageReceived(Long id, String message) {
    String buttonId = this.buttonIdMap.get(message);
    if (buttonId != null) {
      this.onButtonClicked(id, buttonId);
    }

    this.messageEvents.forEach(event -> {
      try {
        event.accept(id, message);
      } catch (Exception e) {
        this.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.SOCIAL_EXCEPTION_CAUGHT);
        if (Settings.IMP.MAIN.DEBUG) {
          e.printStackTrace(); // printStackTrace is necessary there
        }
      }
    });
  }

  private void onButtonClicked(Long id, String buttonId) {
    SocialButtonListenerAdapter buttonListenerAdapter = this.buttonEvents.get(buttonId);
    if (buttonListenerAdapter != null) {
      try {
        buttonListenerAdapter.accept(id);
      } catch (Exception e) {
        this.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.SOCIAL_EXCEPTION_CAUGHT);
        if (Settings.IMP.MAIN.DEBUG) {
          e.printStackTrace(); // printStackTrace is necessary there
        }
      }
    }
  }

  public void addMessageEvent(SocialMessageListenerAdapter event) {
    this.messageEvents.add(event);
  }

  public void addButtonEvent(String id, SocialButtonListenerAdapter event) {
    this.buttonEvents.put(id, event);
  }

  public void removeButtonEvent(String id) {
    this.buttonEvents.remove(id);
  }

  public void start() {
    try {
      discordSocial.start();
    } catch (SocialInitializationException e) {
      e.printStackTrace(); // printStackTrace is necessary there
    }
  }

  public void stop() {
    discordSocial.stop();
  }

  public void unregisterHook(Player player) {
    discordSocial.onPlayerRemoved(player);
  }

  public void unregisterHook(String dbField, Player player) {
    if(discordSocial.getDbField().equals(dbField))
      discordSocial.onPlayerRemoved(player);
  }

  public void registerHook(Long id) {
    discordSocial.onPlayerAdded(id);
  }

  public void registerKeyboard(List<List<DiscordSocial.ButtonItem>> keyboard) {
    for (List<DiscordSocial.ButtonItem> items : keyboard)
      for (DiscordSocial.ButtonItem item : items)
        this.buttonIdMap.put(item.getValue(), item.getId());
  }

  public void registerButton(DiscordSocial.ButtonItem item) {
    this.buttonIdMap.put(item.getValue(), item.getId());
  }

  public void broadcastMessage(Player player, String message, List<List<DiscordSocial.ButtonItem>> item) {
    this.broadcastMessage(player, message, item, DiscordSocial.ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message,
                               List<List<DiscordSocial.ButtonItem>> item, DiscordSocial.ButtonVisibility visibility) {
    discordSocial.sendMessage(player, message, item, visibility);
  }

  public void broadcastMessage(Long id, String message,
                                  List<List<DiscordSocial.ButtonItem>> item) {
    discordSocial.sendMessage(id, message, item, DiscordSocial.ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message) {
    discordSocial.sendMessage(player, message, Collections.emptyList(), DiscordSocial.ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Long id, String message) {
   discordSocial.sendMessage(id, message, Collections.emptyList(), DiscordSocial.ButtonVisibility.DEFAULT);
  }

  public void createPlayer(SocialMessageListenerAdapter event){
    if (lowercaseMessage.startsWith(socialRegisterCmd)) {
      int desiredLength = socialRegisterCmd.length() + 1;

      if (message.length() <= desiredLength) {
        this.socialManager.broadcastMessage(id_, Settings.IMP.MAIN.STRINGS.LINK_SOCIAL_REGISTER_CMD_USAGE);
        return;
      }

      String[] info = message.substring(desiredLength).split(" ");
      String account = info[0];
      Long discord_id = Long.getLong(info[1]);

      Addon.CachedRegisteredUser cachedRegisteredUser = this.cachedAccountRegistrations.get(discord_id);
      if (cachedRegisteredUser == null) {
        this.cachedAccountRegistrations.put(discord_id, cachedRegisteredUser = new Addon.CachedRegisteredUser());
      }

      if (cachedRegisteredUser.getRegistrationAmount() >= Settings.IMP.MAIN.MAX_REGISTRATION_COUNT_PER_TIME) {
        this.socialManager.broadcastMessage(discord_id, Settings.IMP.MAIN.STRINGS.REGISTER_LIMIT);
        return;
      }

      cachedRegisteredUser.incrementRegistrationAmount();

      if (this.dataManager.players().idExists("" + discord_id)) {
        this.socialManager.broadcastMessage(discord_id, Settings.IMP.MAIN.STRINGS.LINK_ALREADY);
        return;
      }

      if (!this.nicknamePattern.matcher(account).matches()) {
        this.socialManager.broadcastMessage(discord_id, Settings.IMP.MAIN.STRINGS.REGISTER_INCORRECT_NICKNAME);
        return;
      }

      if (this.plugin.getPlayerDao().idExists(account)) {
        this.socialManager.broadcastMessage(discord_id, Settings.IMP.MAIN.STRINGS.REGISTER_TAKEN_NICKNAME);
        return;
      }


      String newPassword = Long.toHexString(Double.doubleToLongBits(Math.random()));

      RegisteredPlayer player = new RegisteredPlayer(account, "", "").setPassword(newPassword);
      this.plugin.getPlayerDao().create(player);

      this.linkSocial(account, discord_id);
      this.socialManager.broadcastMessage(discord_id,
              Placeholders.replace(Settings.IMP.MAIN.STRINGS.REGISTER_SUCCESS, newPassword));
    }
  }
}
