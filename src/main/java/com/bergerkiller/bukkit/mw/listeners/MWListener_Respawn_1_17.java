package com.bergerkiller.bukkit.mw.listeners;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.playerdata.InventoryPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

/**
 * Used on Minecraft 1.17+ to set the initial spawn world of new players.
 * The old mechanism in the player data controller to set an initial configuration is no longer used.
 */
class MWListener_Respawn_1_17 implements Listener {
    private final MyWorlds plugin;

    public MWListener_Respawn_1_17(MyWorlds plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        // Spigots devs suck and this actually doesn't work
        // if (event.getPlayer().hasPlayedBefore()) {
        //     return;
        // }
        if (plugin.getPlayerDataController().hasPlayedBefore(InventoryPlayer.online(event.getPlayer()))) {
            return;
        }

        event.setSpawnLocation(WorldConfig.getMain().getSpawnLocation());
    }
}
