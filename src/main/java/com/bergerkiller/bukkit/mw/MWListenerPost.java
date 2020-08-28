package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.collections.EntityMap;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;

/**
 * Handles events to manage plugin-defined permissions and messages.
 * Basically, everything that happens after performing a command
 * or using portals is dealt with here.
 */
public class MWListenerPost implements Listener {
    private final MyWorlds plugin;
    private static EntityMap<Player, PortalInfo> lastEnteredPortal = new EntityMap<Player, PortalInfo>();

    public MWListenerPost(MyWorlds plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the last-entered portal destination display name.
     * Set elsewhere.
     * 
     * @param player
     * @param remove
     * @return last entered portal destination display name, null if none is applicable
     */
    public static String getLastEnteredPortalDestination(Player player, boolean remove) {
        PortalInfo info = remove ? lastEnteredPortal.remove(player) : lastEnteredPortal.get(player);
        if (info == null) {
            return null;
        }
        // Information must be from the same tick, otherwise it is invalid!
        if (info.tickStamp != CommonUtil.getServerTicks()) {
            lastEnteredPortal.remove(player);
            return null;
        }
        // Done
        return info.portalDestinationDisplayName;
    }

    public static void setLastEntered(Player player, String portalDestinationDisplayName) {
        PortalInfo info = new PortalInfo();
        info.portalDestinationDisplayName = portalDestinationDisplayName;
        info.tickStamp = CommonUtil.getServerTicks();
        lastEnteredPortal.put(player, info);
    }

    public static boolean handleTeleportPermission(Player player, Location to) {
        // World can be entered?
        if (!Permission.canEnter(player, to.getWorld())) {
            Localization.WORLD_NOACCESS.message(player);
            return false;
        }
        return true;
    }

    public static void handleTeleportMessage(Player player, Location to) {
        // We are handling this at the very end, so we can remove the portal as we get it
        String lastPortal = getLastEnteredPortalDestination(player, true);
        if (lastPortal != null) {
            // Show the portal name
            Localization.PORTAL_ENTER.message(player, lastPortal);
        } else if (to != null && to.getWorld() != player.getWorld()) {
            // Show world enter message
            Localization.WORLD_ENTER.message(player, to.getWorld().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortalMonitor(PlayerPortalEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getTo().getWorld() != event.getPlayer().getWorld()) {
            WorldConfig.get(event.getPlayer()).onPlayerLeave(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleportPerm(PlayerTeleportEvent event) {
        // Sometimes TO is NULL, this fixes that. After that check the teleport permission is enforced.
        if (event.getTo() == null || event.getTo().getWorld() == null || !handleTeleportPermission(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleportMsg(PlayerTeleportEvent event) {
        handleTeleportMessage(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawnPerm(PlayerRespawnEvent event) {
        if (event.getRespawnLocation() != null && !handleTeleportPermission(event.getPlayer(), event.getRespawnLocation())) {
            event.setRespawnLocation(event.getPlayer().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnMsg(PlayerRespawnEvent event) {
        if (!plugin.getEndRespawnHandler().isDeathRespawn(event)) {
            handleTeleportMessage(event.getPlayer(), event.getRespawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Result.DENY || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (MaterialUtil.ISBUCKET.get(event.getItem())) {
            if (!Permission.canBuild(event.getPlayer())) {
                Localization.WORLD_NOBUILD.message(event.getPlayer());
                event.setUseInteractedBlock(Result.DENY);
                event.setCancelled(true);
            }
        } else if (MaterialUtil.ISINTERACTABLE.get(event.getClickedBlock())) {
            if (!Permission.canUse(event.getPlayer())) {
                Localization.WORLD_NOUSE.message(event.getPlayer());
                event.setUseInteractedBlock(Result.DENY);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!Permission.canBuild(event.getPlayer())) {
            Localization.WORLD_NOBREAK.message(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.canBuild()) {
            if (!Permission.canBuild(event.getPlayer())) {
                Localization.WORLD_NOBUILD.message(event.getPlayer());
                event.setBuild(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!MyWorlds.debugLogGMChanges) {
            return;
        }

        ArrayList<StackTraceElement> elements = new ArrayList<StackTraceElement>(Arrays.asList(Thread.currentThread().getStackTrace()));
        while (!elements.isEmpty() && !elements.get(0).toString().startsWith("org.bukkit.plugin.SimplePluginManager.callEvent")) {
            elements.remove(0);
        }

        System.out.println("Game Mode of " + event.getPlayer().getName() + " changed from " +
                event.getPlayer().getGameMode() + " to " + event.getNewGameMode());
        ArrayList<String> pluginNames = new ArrayList<String>();
        for (Plugin plugin : CommonUtil.findPlugins(elements)) {
            pluginNames.add(plugin.getName());
        }
        if (pluginNames.isEmpty()) {
            System.out.println("This was likely initiated by the server. Stack trace:");
        } else {
            System.out.println("This was likely initiated by " + StringUtil.join(" OR ", pluginNames) + ". Stack trace:");
        }
        for (StackTraceElement element : elements) {
            System.out.println("  at " + element);
        }
    }

    private static class PortalInfo {
        public String portalDestinationDisplayName;
        public int tickStamp;
    }
}
