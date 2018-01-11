package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;

/**
 * Suppresses creative mode SET_CREATIVE_SLOT packets from being received by the server for a couple
 * ticks after a player's inventory is changed (as part of a world change). This fixes a glitch possible
 * when plugins other than MyWorlds manage the game mode change.
 * 
 * https://github.com/Multiverse/Multiverse-Inventories/issues/161
 */
public class MWCreativeSlotSuppressor {
    public static final int CREATIVE_SLOT_TIMEOUT = 5;
    private final Map<UUID, Integer> creativeSlotTimeouts = new HashMap<UUID, Integer>();
    private Task createSlotUpdateTask = null;
    private final PacketListener packetListener = new PacketListener() {
        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getType() != PacketType.IN_SET_CREATIVE_SLOT) {
                return; // Mostly useless, we don't register other packets
            }
            if (!hasTimeout(event.getPlayer())) {
                return; // No timeout is set, allow it
            }

            // Not allowed. Suppress the creative slot update packet.
            event.setCancelled(true);

            // Attempt to re-send the correct item to the client
            // This allows for slightly reducing the ghost item problem that can otherwise occur
            int slot = event.getPacket().read(PacketType.IN_SET_CREATIVE_SLOT.slot);
            if (slot >= 0) {
                PlayerInventory inv = event.getPlayer().getInventory();
                int realSlot = -1;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (PlayerUtil.getInventorySlotIndex(i) == slot) {
                        realSlot = i;
                        break;
                    }
                }
                if (realSlot != -1) {
                    CommonPacket fixSlotPacket = PacketType.OUT_WINDOW_SET_SLOT.newInstance();
                    fixSlotPacket.write(PacketType.OUT_WINDOW_SET_SLOT.slot, slot);
                    fixSlotPacket.write(PacketType.OUT_WINDOW_SET_SLOT.windowId, 0);
                    fixSlotPacket.write(PacketType.OUT_WINDOW_SET_SLOT.item, inv.getItem(realSlot));
                    PacketUtil.sendPacket(event.getPlayer(), fixSlotPacket);
                }
            }
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
        }
    };
    private final Listener eventListener = new Listener() {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            clearTimeout(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            clearTimeout(event.getPlayer());
        }
    };

    public void enable(MyWorlds myWorlds) {
        myWorlds.register(packetListener, PacketType.IN_SET_CREATIVE_SLOT);
        myWorlds.register(eventListener);
        createSlotUpdateTask = new Task(myWorlds) {
            @Override
            public void run() {
                tickTimeouts();
            }
        }.start(1, 1);
    }

    public void disable(MyWorlds myWorlds) {
        myWorlds.unregister(packetListener);
        Task.stop(createSlotUpdateTask);
        createSlotUpdateTask = null;
    }

    /**
     * Checks whether the player is on a timeout, not allowing creative slot packets
     * to be handled at this time
     * 
     * @param player
     * @return True if on a timeout
     */
    public boolean hasTimeout(Player player) {
        return creativeSlotTimeouts.containsKey(player.getUniqueId());
    }

    /**
     * Times the player out for 5 ticks
     * 
     * @param player
     */
    public void setTimeout(Player player) {
        creativeSlotTimeouts.put(player.getUniqueId(), CREATIVE_SLOT_TIMEOUT);
    }

    /**
     * Removes any timeouts imposed on a player
     * 
     * @param player
     */
    public void clearTimeout(Player player) {
        creativeSlotTimeouts.remove(player.getUniqueId());
    }

    // refresh timeouts, subtracting the counters by one, removing entirely when 0
    private void tickTimeouts() {
        if (creativeSlotTimeouts.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iter = creativeSlotTimeouts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Integer> e = iter.next();
            int value = e.getValue().intValue() - 1;
            if (value > 0) {
                e.setValue(Integer.valueOf(value));
            } else {
                iter.remove();
            }
        }
    }

}
