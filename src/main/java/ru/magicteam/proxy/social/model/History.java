package ru.magicteam.proxy.social.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = History.TABLE_NAME)
public class History {
    public static final String TABLE_NAME = "history";
    public static final String DISCORD_ID_FIELD = "discord_id";
    public static final String MODERATOR_ID_FIELD = "moderator_id";
    public static final String ACTION_FIELD = "action";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String STATUS_FIELD = "status";
    public static final String INFO_FIELD = "info";

    @Override
    public String toString() {
        return "History{" +
                "discordID=" + discordID +
                ", moderatorID=" + moderatorID +
                ", action='" + action + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", status='" + status + '\'' +
                ", info='" + info + '\'' +
                '}';
    }

    public enum Status {
        ACCEPT("accept"),
        AWAITING("awaiting"),
        DENY("deny");

        public final String value;

        Status(String value){
            this.value = value;
        }
    }

    public enum Action {
        ACCESS("access");

        public final String value;

        Action(String value){
            this.value = value;
        }
    }

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = DISCORD_ID_FIELD)
    private Long discordID;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = MODERATOR_ID_FIELD)
    private Long moderatorID;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = ACTION_FIELD)
    private String action;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = TIMESTAMP_FIELD)
    private String timestamp;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = STATUS_FIELD)
    private String status;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = INFO_FIELD, width = 2048)
    private String info;

    public Long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    public Long getModeratorID() {
        return moderatorID;
    }

    public void setModeratorID(Long moderator_id) {
        this.moderatorID = moderator_id;
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


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}

