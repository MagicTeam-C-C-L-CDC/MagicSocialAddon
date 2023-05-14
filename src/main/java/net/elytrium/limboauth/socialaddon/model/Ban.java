package net.elytrium.limboauth.socialaddon.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = Ban.TABLE_NAME)
public class Ban {
    public static final String TABLE_NAME = "ban";
    public static final String DISCORD_DB_FIELD = "discord_id";
    public static final String REASON_FIELD = "reason";
    public static final String EXPIRE_FIELD = "expire_timestamp";

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = DISCORD_DB_FIELD)
    private Long discordID;

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = REASON_FIELD)
    private String reason;


    public Long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExpire_timestamp() {
        return expire_timestamp;
    }

    public void setExpire_timestamp(String expire_timestamp) {
        this.expire_timestamp = expire_timestamp;
    }

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = EXPIRE_FIELD)
    private String expire_timestamp;
}
