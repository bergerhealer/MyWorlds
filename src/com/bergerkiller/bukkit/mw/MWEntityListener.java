package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class MWEntityListener extends EntityListener {
    private final MyWorlds plugin;

    public MWEntityListener(final MyWorlds plugin) {
        this.plugin = plugin;
    }
	
    private static HashMap<Entity, Long> portaltimes = new HashMap<Entity, Long>();
    private static ArrayList<TeleportCommand> teleportations = new ArrayList<TeleportCommand>();  
     
    @Override
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    	Entity e = event.getEntity();
        long currtime = System.currentTimeMillis();
        if (!portaltimes.containsKey(e) || currtime - portaltimes.get(e) >= 1000) {
        	Portal portal = Portal.getPortal(event.getLocation(), 10);  	
        	if (portal != null) {
        		if (!(event.getEntity() instanceof Player) || Permission.has((Player) event.getEntity(), "portal.use")) {
        			delayedTeleport(portal, event.getEntity());
        		}
        	}
    	}
        portaltimes.put(e, currtime);
    }
    
    private static class TeleportCommand {
    	public Entity e;
    	public Portal portal;
    	public TeleportCommand(Entity e, Portal portal) {
    		this.e = e;
    		this.portal = portal;
    	}
    }
    
    public void delayedTeleport(Portal portal, Entity e) {
    	teleportations.add(new TeleportCommand(e, portal));
    	this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
    	    public void run() {
    	    	TeleportCommand telec = teleportations.remove(0);
    	    	boolean worked = telec.portal.teleport(telec.e);
    	    	if (telec.e instanceof Player) {
    	    		if (worked) {
        				((Player) telec.e).sendMessage(ChatColor.GREEN + "You teleported to " + ChatColor.WHITE + telec.portal.getDestinationName() + ChatColor.GREEN + ", have a nice stay!");
        			} else {
        				((Player) telec.e).sendMessage(ChatColor.YELLOW + "This portal has no destination!");
        			}
        		}
    	    }
    	}, 1L);
    }
}
