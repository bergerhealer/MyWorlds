package com.bergerkiller.bukkit.mw.playerdata;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Uniquely identifies an inventory player. Used to pass around player information
 * internally.
 */
public interface InventoryPlayer {

    /**
     * Gets the name of the player account
     *
     * @return Player name
     */
    String getName();

    /**
     * Gets the UUID String of the player data. Could be mangled.
     *
     * @return Player uuid
     */
    String getUniqueId();

    /**
     * Gets whether this InventoryPlayer is a {@link OnlineInventoryPlayer}, and an online Player
     * instance is available.
     *
     * @return True if Online
     */
    boolean isOnline();

    /**
     * Parses the {@link #getUniqueId()} into a valid UUID, if possible.
     * If the UUID string is mangled, returns null.
     *
     * @return Parsed Player uuid
     */
    default UUID parseUniqueId() {
        try {
            return UUID.fromString(getUniqueId());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    default boolean isOpenInvPlayer() {
        return false;
    }

    static InventoryPlayer tryOnline(String name, String uuid) {
        OfflineInventoryPlayer offline = offline(name, uuid);
        UUID parsedUUID = offline.parseUniqueId();
        if (parsedUUID != null) {
            Player player = Bukkit.getPlayer(parsedUUID);
            if (player != null) {
                return online(player);
            }
        }

        return offline;
    }

    static OfflineInventoryPlayer offline(String name, String uuid) {
        return new OfflineInventoryPlayer(name, uuid);
    }

    static OfflineInventoryPlayer offline(OfflinePlayer offlinePlayer) {
        return new OfflineInventoryPlayer(offlinePlayer.getName(), offlinePlayer.getUniqueId().toString());
    }

    static OnlineInventoryPlayer online(Player player) {
        return new OnlineInventoryPlayer(player);
    }

    /**
     * An Offline player
     */
    class OfflineInventoryPlayer implements InventoryPlayer {
        private final String name;
        private final String uuid;

        public OfflineInventoryPlayer(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUniqueId() {
            return uuid;
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @Override
        public String toString() {
            return "OfflineInventoryPlayer{name=" + name + ", uuid=" + uuid + "}";
        }
    }

    /**
     * An online Player
     */
    class OnlineInventoryPlayer implements InventoryPlayer {
        private final Player player;
        private final String uuidStr;

        public OnlineInventoryPlayer(Player player) {
            this.player = player;
            this.uuidStr = player.getUniqueId().toString();
        }

        /**
         * Gets the online Player object
         *
         * @return Online player
         */
        public Player getOnlinePlayer() {
            return player;
        }

        @Override
        public String getName() {
            return player.getName();
        }

        @Override
        public String getUniqueId() {
            return uuidStr;
        }

        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public UUID parseUniqueId() {
            return player.getUniqueId();
        }

        @Override
        public boolean isOpenInvPlayer() {
            return player.getClass().getName().startsWith("com.lishid.openinv.");
        }

        @Override
        public String toString() {
            return "OnlineInventoryPlayer{player=" + player + "}";
        }
    }
}
