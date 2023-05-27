package ru.magicteam.proxy.social.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = History.TABLE_NAME)
public class History {
    public static final String TABLE_NAME = "history";
    public static final String DISCORD_ID_FIELD = "discord_id";
    public static final String MODERATOR_ID_FIELD = "moderator_id";
    public static final String ACTION_FIELD = "action";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String INFO_FIELD = "info";

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = DISCORD_ID_FIELD)
    private Long discordID;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = MODERATOR_ID_FIELD)
    private Long moderator_id;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = ACTION_FIELD)
    private String action;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = TIMESTAMP_FIELD)
    private String timestamp;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = INFO_FIELD)
    private String info;

    public Long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    public Long getModerator_id() {
        return moderator_id;
    }

    public void setModerator_id(Long moderator_id) {
        this.moderator_id = moderator_id;
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

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


}

