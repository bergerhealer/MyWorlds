package com.bergerkiller.bukkit.mw.playerdata;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Caches the last player positions that were most recently retrieved.
 * Keeps the values around while players are online. Periodically cleans it up again.
 */
public class LastPlayerPositionCache {
    private final Map<String, LastPlayerPositionList> byUUIDStr = new HashMap<>();

    public synchronized LastPlayerPositionList get(InventoryPlayer player) {
        return byUUIDStr.get(player.getUniqueId());
    }

    public synchronized void put(InventoryPlayer player, LastPlayerPositionList positionList) {
        if (positionList == null) {
            byUUIDStr.remove(player.getUniqueId());
        } else {
            byUUIDStr.put(player.getUniqueId(), positionList);
        }
    }

    public synchronized void cleanup() {
        for (Iterator<String> uniqueIdIter = byUUIDStr.keySet().iterator(); uniqueIdIter.hasNext();) {
            String uniqueIdStr = uniqueIdIter.next();
            UUID uniqueId;
            try {
                uniqueId = UUID.fromString(uniqueIdStr);
            } catch (IllegalArgumentException ex) {
                uniqueIdIter.remove();
                continue;
            }
            if (Bukkit.getPlayer(uniqueId) == null) {
                uniqueIdIter.remove();
            }
        }
    }
}
