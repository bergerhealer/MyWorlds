package com.bergerkiller.bukkit.mw.advancement;

import com.bergerkiller.bukkit.common.Common;
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
     * Called when on some world, advancements are expected to be disabled. When this is called,
     * the manager can decide to instrument the server to enforce this.<br>
     * <br>
     * This makes sure the server isn't being hooked and hacked into unless someone actually
     * wanted advancements disabled on some world. If that API has bugs in it for one reason or another,
     * at least not everybody is going to be affected by it.<br>
     * <br>
     * This method is called directly after {@link #enable()} if on some world advancements are
     * disabled. If this is not the case, and the player later on disables advancements anywhere
     * using a command, then this method is called again.
     */
    void notifyAdvancementsDisabledOnWorld();

    /**
     * Creates a new advancement manager relevant for the current server
     * version
     * 
     * @param plugin
     * @return advancement manager
     */
    static AdvancementManager create(MyWorlds plugin) {
        if (CommonUtil.getClass("org.bukkit.advancement.Advancement", false) == null) {
            return new AdvancementManagerDisabled(); // Advancements are not supported
        } else if (Common.hasCapability("Common:Event:PlayerAdvancementProgressEvent")) {
            return new AdvancementManagerUsingProgressEvent(plugin);
        } else {
            return new AdvancementManagerUsingRewardDisabler(plugin);
        }
    }
}
