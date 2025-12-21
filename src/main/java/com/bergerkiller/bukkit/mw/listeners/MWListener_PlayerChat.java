package com.bergerkiller.bukkit.mw.listeners;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

class MWListener_PlayerChat implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Handle chat permissions
        if (!Permission.canChat(event.getPlayer())) {
            event.setCancelled(true);
            Localization.WORLD_NOCHATACCESS.message(event.getPlayer());
            return;
        }
        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            if (!Permission.canChat(event.getPlayer(), iterator.next())) {
                iterator.remove();
            }
        }
    }
}
