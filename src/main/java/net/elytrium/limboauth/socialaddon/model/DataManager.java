package net.elytrium.limboauth.socialaddon.model;

import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.Dao;

public record DataManager (Dao<Player, String> players,  Dao<Activity, Long> activity, Dao<Ban, Long> ban, Dao<History, Long> history){
}
