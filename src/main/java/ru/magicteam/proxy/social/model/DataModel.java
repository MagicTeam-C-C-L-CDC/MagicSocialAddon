package ru.magicteam.proxy.social.model;

import ru.magicteam.proxy.social.Settings;
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.Dao;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record DataModel(
                        Dao<Player, Long> players,
                        Dao<Activity, Long> activity,
                        Dao<Ban, Long> ban,
                        Dao<History, Long> history,
                        Dao<GoogleFormSession, String> googleFormSessions) implements ModelAPI {
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
        try {
            Player player = players.queryForId(id);
            return Optional.ofNullable(player);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Player> queryPlayerByNickName(String id) {
        try {
            List<Player> playerList = players.queryForEq(Player.NICKNAME_FIELD, id);
            return playerList.stream().findFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Ban> queryBan(Long id) {
        try {
            List<Ban> bans = ban.queryForEq(Ban.DISCORD_DB_FIELD, id);
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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

    @Override
    public Status createRequest(Long id, String type) throws SQLException {
        if(history.idExists(id))
            return ModelAPI.Status.ALREADY_EXISTS;
        History request = new History();
        request.setAction(type);
        request.setDiscordID(id);
        request.setTimestamp(LocalDateTime.now().toString());
        request.setStatus(History.Status.AWAITING.value);
        history.create(request);
        return ModelAPI.Status.SUCCESS;
    }

    @Override
    public Boolean requestExist(Long id, String type) throws SQLException {
        return history.queryForEq(History.DISCORD_ID_FIELD, id)
                .stream()
                .filter(e -> e.getAction().equals(type) && e.getStatus().equals(History.Status.AWAITING))
                .toList()
                .size() > 0;
    }

    @Override
    public String createGoogleFormSession(Long id) throws SQLException {
       try {
           MessageDigest digest = MessageDigest.getInstance("SHA-1");
           digest.reset();
           String saltedID = id + Settings.IMP.MAIN.GOOGLE_FORM.SALT;
           digest.update(saltedID.getBytes(StandardCharsets.UTF_8));
           String hash = String.format("%040x", new BigInteger(1, digest.digest()));
           Dao.CreateOrUpdateStatus status = googleFormSessions.createOrUpdate(new GoogleFormSession(id, hash));
           return hash;
       } catch (NoSuchAlgorithmException e) {
           throw new RuntimeException(e);
       }
    }

    @Override
    public Boolean existGoogleFormSession(String hash) throws SQLException {
        return googleFormSessions().queryForEq(GoogleFormSession.HASH_FIELD, hash).size() > 0;
    }

    @Override
    public GoogleFormSession getGoogleFormSesion(String hash) throws SQLException {
        return googleFormSessions().queryForId(hash);
    }

    @Override
    public void deleteGoogleFormSesion(Long id) throws SQLException {
        history.deleteById(id);
    }

    @Override
    public Status createHistory(Long id, Long moderatorId, String action, String status, String info) throws SQLException {
        if(history.idExists(id))
            return ModelAPI.Status.ALREADY_EXISTS;
        History history1 = new History();
        history1.setDiscordID(id);
        history1.setStatus(status);
        history1.setTimestamp(LocalDateTime.now().toString());
        history1.setInfo(info);
        history1.setModeratorID(moderatorId);
        history1.setAction(action);
        history.createOrUpdate(history1);
        return ModelAPI.Status.SUCCESS;
    }

    @Override
    public Collection<History> searchHistory(Long id) throws SQLException {
       return history.queryForEq(History.DISCORD_ID_FIELD, id);
    }

    @Override
    public Collection<History> searchHistoryByName(String name) throws SQLException {
        Optional<Player> optional = players.queryForEq(Player.NICKNAME_FIELD, name).stream().findFirst();
        if(optional.isEmpty())
            throw new SQLException();
        return history.queryForEq(History.DISCORD_ID_FIELD, optional.get().getDiscordID());
    }

    @Override
    public Status createPlayer(Long id, String nickname) throws SQLException {
        if(players.idExists(id))
            return ModelAPI.Status.ALREADY_EXISTS;
        Player player = new Player();
        player.setDiscordID(id);
        player.setNickname(nickname);
        player.setNotifyEnabled(true);
        players.create(player);
        return ModelAPI.Status.SUCCESS;
    }

    @Override
    public void updateHistory(Long id, History history) throws SQLException {
        this.history.update(history);
    }

    @Override
    public Collection<History> allHistory(Long id, Predicate<History> filter) throws SQLException {
        return history.queryForEq(History.DISCORD_ID_FIELD, id).stream().filter(filter).toList();
    }


}
