package com.bergerkiller.bukkit.mw.listeners;

import com.bergerkiller.bukkit.common.events.SignEditTextEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Uses the BKCommonLib {@link SignEditTextEvent} to handle sign changes
 */
class MWListener_SignEdit_BKCL implements Listener {
    private final MWListener_Main listener;

    public MWListener_SignEdit_BKCL(MWListener_Main listener) {
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
