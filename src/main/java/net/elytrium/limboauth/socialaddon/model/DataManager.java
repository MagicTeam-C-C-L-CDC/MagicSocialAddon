package net.elytrium.limboauth.socialaddon.model;

import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ProxyServer;
import net.elytrium.limboauth.socialaddon.Settings;
import net.elytrium.limboauth.socialaddon.proxy.SocialManager;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.Dao;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;

public record DataManager (SocialManager socialManager, ProxyServer proxyServer, Dao<Player, String> players,  Dao<Activity, Long> activity, Dao<Ban, Long> ban, Dao<History, Long> history){

    public void linkSocial(String lowercaseNickname, Long id) throws SQLException {
        Player player = players().queryForId("" + id);
        if (player == null) {
            Settings.IMP.MAIN.AFTER_LINKAGE_COMMANDS.forEach(command ->
                    proxyServer.getCommandManager().executeAsync(p -> Tristate.TRUE, command.replace("{NICKNAME}", lowercaseNickname)));

            players().create(new Player(lowercaseNickname));
        } else if (player.getDiscordID() != null) {
            socialManager.broadcastMessage(id, Settings.IMP.MAIN.STRINGS.LINK_ALREADY);
            return;
        }

        UpdateBuilder<Player, String> updateBuilder = players().updateBuilder();
        updateBuilder.where().eq(Player.NICKNAME_FIELD, lowercaseNickname);
        updateBuilder.updateColumnValue(Player.DISCORD_DB_FIELD, id);
        updateBuilder.update();
    }

}
