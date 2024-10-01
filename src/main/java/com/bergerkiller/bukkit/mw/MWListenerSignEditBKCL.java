package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.events.SignEditTextEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Uses the BKCommonLib {@link SignEditTextEvent} to handle sign changes
 */
class MWListenerSignEditBKCL implements Listener {
    private final MWListener listener;

    public MWListenerSignEditBKCL(MWListener listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignEditText(SignEditTextEvent event) {
        if (!listener.handleSignEdit(
                event.getPlayer(),
                event.getBlock(),
                event.getSide(),
                event.getLines()
        )) {
            event.setCancelled(true);
        }
    }
}
