package com.bergerkiller.bukkit.mw.portal;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.mw.PortalType;

/**
 * Filters portals that should not be handled by MyWorlds
 */
public interface PortalFilter {

    /**
     * Checks whether the Portal Block specified, if entered by a Player, should be
     * ignored and teleportation logic for it should not be executed.
     *
     * @param portalType
     * @return portalBlock
     */
    boolean isPortalFiltered(PortalType portalType, Block portalBlock);

    /**
     * Checks whether this filter makes use of a particular plugin. If so, if that plugin
     * disables, the filter is automatically removed.
     *
     * @param plugin
     * @return True if the plugin is used
     */
    boolean usesPlugin(Plugin plugin);
}
