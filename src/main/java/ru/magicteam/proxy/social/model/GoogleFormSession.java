package ru.magicteam.proxy.social.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = GoogleFormSession.TABLE_NAME)
public class GoogleFormSession {
    public static final String TABLE_NAME = "google_form_session";
    public static final String DISCORD_ID_FIELD = "discord_id";
    public static final String HASH_FIELD = "hash";

    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(columnName = DISCORD_ID_FIELD)
    private Long discordID;
    @net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField(id = true, columnName = HASH_FIELD)
    private String hash;

    public Long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }


    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public GoogleFormSession(){}
    public GoogleFormSession(Long id, String hash){
        this.hash = hash;
        this.discordID = id;
    }

}

