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

package net.elytrium.limboauth.socialaddon.model;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.elytrium.limboauth.socialaddon.Settings;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = Player.TABLE_NAME)
public class Player {
  public static final String TABLE_NAME = "players";
  public static final String DISCORD_DB_FIELD = "discord_id";
  public static final String NICKNAME_FIELD = "nickname";
  public static final String IP_FIELD = "ip";
  public static final String NOTIFY_ENABLED_FIELD = "notify";

  @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = DISCORD_DB_FIELD)
  private Long discordID;

  @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = NICKNAME_FIELD)
  private String nickname;


  @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = IP_FIELD)
  private String ip;

  @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = NOTIFY_ENABLED_FIELD)
  private Boolean notifyEnabled = Settings.IMP.MAIN.DEFAULT_NOTIFY_ENABLED;

  public Player(String nickname) {
    this.nickname = nickname;
  }

  public Player() {

  }

  public String getNickname() {
    return this.nickname;
  }


  public Long getDiscordID() {
    return this.discordID;
  }

  public void setDiscordID(Long discordID) {
    this.discordID = discordID;
  }

  public boolean isNotifyEnabled() {
    return this.notifyEnabled;
  }

  public void setNotifyEnabled(boolean notifyEnabled) {
    this.notifyEnabled = notifyEnabled;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }
}
