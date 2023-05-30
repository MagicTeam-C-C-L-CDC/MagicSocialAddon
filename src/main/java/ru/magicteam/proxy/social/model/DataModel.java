package ru.magicteam.proxy.social.model;

import com.velocitypowered.api.permission.Tristate;
import net.elytrium.commons.config.Placeholders;
import net.elytrium.limboauth.model.RegisteredPlayer;
import ru.magicteam.proxy.social.Addon;
import ru.magicteam.proxy.social.Settings;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.Dao;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DataModel(
                        Dao<Player, String> players,
                        Dao<Activity, Long> activity,
                        Dao<Ban, Long> ban,
                        Dao<History, Long> history) implements ModelAPI {
/*
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
    }*/
/*
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
*/
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DataModel) obj;
        return
                Objects.equals(this.players, that.players) &&
                Objects.equals(this.activity, that.activity) &&
                Objects.equals(this.ban, that.ban) &&
                Objects.equals(this.history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(players, activity, ban, history);
    }

    @Override
    public String toString() {
        return "DataManager[" +
                "players=" + players + ", " +
                "activity=" + activity + ", " +
                "ban=" + ban + ", " +
                "history=" + history + ']';
    }


    @Override
    public Optional<Player> queryPlayerByID(Long id) {
        Player player = null;

        try {
            player = players.queryForId(id.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.ofNullable(player);
    }

    @Override
    public Optional<Player> queryPlayerByNickName(String id) {
        List<Player> playerList = null;

        try {
            playerList = players.queryForEq(Player.NICKNAME_FIELD, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return playerList.stream().findFirst();
    }

    @Override
    public Collection<Ban> queryBan(Long id) {
        List<Ban> bans = null;

        try {
            bans = ban.queryForEq(Ban.DISCORD_DB_FIELD, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bans;
    }

    @Override
    public void updatePlayer(Player player) {
        try {
            this.players.update(player);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deletePlayer(Player player) {
        try {
            players.delete(player);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
