package ru.magicteam.proxy.social.model;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public interface ModelAPI {
    Optional<Player> queryPlayerByID(Long id);
    Optional<Player> queryPlayerByNickName(String name);
    Collection<Ban> queryBan(Long id);
    void updatePlayer(Player player);
    void deletePlayer(Player player);

    Status createRequest(Long id, String type) throws SQLException;
    Boolean requestExist(Long id, String type) throws SQLException;

    String createGoogleFormSession(Long id) throws NoSuchAlgorithmException, SQLException;
    Boolean existGoogleFormSession(String hash) throws SQLException;

    GoogleFormSession getGoogleFormSesion(String hash) throws SQLException;
    void deleteGoogleFormSesion(Long id) throws SQLException;
    Status createHistory(Long id, Long moderatorId, String action, String status, String info) throws SQLException;

    Collection<History> searchHistory(Long id) throws SQLException;
    Collection<History> searchHistoryByName(String name) throws SQLException;

    Status createPlayer(Long id, String nickname) throws SQLException;
    void updateHistory(Long id, History history) throws SQLException;

    Collection<History> allHistory (Long id, Predicate<History> filter) throws SQLException;

    enum Status {
        ALREADY_EXISTS,
        SUCCESS
    }
}
