package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.block.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Uses the Bukkit {@link SignChangeEvent} to handle sign changes
 */
class MWListenerSignEditLegacy implements Listener {
    private final MWListener listener;

    public MWListenerSignEditLegacy(MWListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!listener.handleSignEdit(
                event.getPlayer(),
                event.getBlock(),
                SignSide.sideChanged(event),
                event.getLines()
        )) {
            event.setCancelled(true);
        }
    }
}
