package ru.magicteam.proxy.social.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = Activity.TABLE_NAME)
public class Activity {
    public static final String TABLE_NAME = "action";
    public static final String DISCORD_DB_FIELD = "discord_id";
    public static final String ACTION_FIELD = "action";
    public static final String TIMESTAMP_FIELD = "timestamp";

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = DISCORD_DB_FIELD)
    private Long discordID;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = ACTION_FIELD)
    private String action;

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = TIMESTAMP_FIELD)
    private String timestamp;

    public Long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
