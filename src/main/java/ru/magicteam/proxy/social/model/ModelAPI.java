package ru.magicteam.proxy.social.model;

import java.util.Collection;
import java.util.Optional;

public interface ModelAPI {
    Optional<Player> queryPlayerByID(Long id);
    Optional<Player> queryPlayerByNickName(String id);
    Collection<Ban> queryBan(Long id);
    void updatePlayer(Player player);
    void deletePlayer(Player player);
}
