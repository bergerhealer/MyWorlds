package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;

public class MWBlockListener extends BlockListener {

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
    	if (!event.isCancelled()) {
        	if (event.getBlock().getType() == Material.PORTAL) {
        		if (!(event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)) {
        			event.setCancelled(true);
        		}
        	}
    	}
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {    
    	if (!event.isCancelled()) {
    		if (Permission.canBuild(event.getPlayer())) {
            	Portal portal = Portal.get(event.getBlock(), false);
            	if (portal != null && portal.remove()) {
            		event.getPlayer().sendMessage(ChatColor.RED + "You removed portal " + ChatColor.WHITE + portal.getName() + ChatColor.RED + "!");
            		MyWorlds.notifyConsole(event.getPlayer(), "Removed portal '" + portal.getName() + "'!");
            	}
    		} else {
    			event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to break blocks in this world!");
    			event.setCancelled(true);
    		}
    	}
    }
    
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
    	if (!event.isCancelled() && event.canBuild()) {
    		if (!Permission.canBuild(event.getPlayer())) {
    			event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to place blocks in this world!");
    			event.setBuild(false);
    		}
    	}
    }
    
	public static float getAngleDifference(float angle1, float angle2) {
		float difference = angle1 - angle2;
        while (difference < -180) difference += 360;
        while (difference > 180) difference -= 360;
        return Math.abs(difference);
	}
            
    @Override
    public void onSignChange(SignChangeEvent event) {
    	if (!event.isCancelled()) {
        	Portal portal = Portal.get(event.getBlock(), event.getLines());
    		if (portal != null) {
    			if (Permission.has(event.getPlayer(), "portal.create")) {
    				if (Portal.exists(event.getPlayer().getWorld().getName(), portal.getName())) {
    					if (!MyWorlds.allowPortalNameOverride || !Permission.has(event.getPlayer(), "portal.override")) {
    						event.getPlayer().sendMessage(ChatColor.RED + "This portal name is already used!");
    						event.setCancelled(true);
    						return;
    					}
    				}
    				portal.add();
    				MyWorlds.notifyConsole(event.getPlayer(), "Created a new portal: '" + portal.getName() + "'!");
    				if (portal.hasDestination()) {
    	    			event.getPlayer().sendMessage(ChatColor.GREEN + "You created a new portal to " + ChatColor.WHITE + portal.getDestinationName() + ChatColor.GREEN + "!");
    				} else {
    	    			event.getPlayer().sendMessage(ChatColor.GREEN + "You created a new destination portal!");
    				}
    			} else {
    				event.setCancelled(true);
    			}
    		}
    	}
    }
    
}
