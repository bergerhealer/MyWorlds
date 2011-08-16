package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;

public class MWBlockListener extends BlockListener {
    private final MyWorlds plugin;

    public MWBlockListener(final  MyWorlds plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onBlockCanBuild(BlockCanBuildEvent event) {
    	if (event.getMaterial() == Material.PORTAL) event.setBuildable(true);
    }
    
    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
    	if (event.getBlock().getType() == Material.PORTAL) event.setCancelled(true);
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {    
    	Portal portal = Portal.get(event.getBlock());
    	if (portal != null) {
    		portal.remove();
    		event.getPlayer().sendMessage(ChatColor.RED + "You removed portal " + ChatColor.WHITE + portal.getName() + ChatColor.RED + "!");
    		MyWorlds.notifyConsole(event.getPlayer(), "Removed portal '" + portal.getName() + "'!");
    	}
    }
    
    @Override
    public void onSignChange(SignChangeEvent event) {
    	Portal portal = Portal.get(event.getBlock(), event.getLines());
		if (portal != null) {
			if (Permission.has(event.getPlayer(), "portal.create")) {
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
