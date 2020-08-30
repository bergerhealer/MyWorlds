package com.bergerkiller.bukkit.mw.advancement;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * Manages the enabling/disabling of advancements
 */
public interface AdvancementManager {

    /**
     * Enables/initializes this advancement manager
     */
    void enable();

    /**
     * Called when a player joins a world, in order to cache
     * the current advancements the player has unlocked.
     * 
     * @param player
     */
    void cacheAdvancements(Player player);

    /**
     * Applies the game rule to a world to enable/disable announcements of advancements
     * 
     * @param world
     * @param enabled
     */
    void applyGameRule(World world, boolean enabled);

    /**
     * Creates a new advancement manager relevant for the current server
     * version
     * 
     * @param plugin
     * @return advancement manager
     */
    static AdvancementManager create(MyWorlds plugin) {
        if (CommonUtil.getClass("org.bukkit.event.player.PlayerAdvancementDoneEvent", false) != null) {
            return new AdvancementManagerImpl(plugin);
        } else {
            return new AdvancementManagerDisabled();
        }
    }
}
