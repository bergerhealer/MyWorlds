package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class MWWorldListener extends WorldListener {
	
	private static HashSet<String> initIgnoreWorlds = new HashSet<String>();
	public static void ignoreWorld(String worldname) {
		initIgnoreWorlds.add(worldname);
	}
    
    @Override
    public void onWorldLoad(WorldLoadEvent event) {
    	WorldConfig.get(event.getWorld()).timeControl.setLocking(true);
    }
    
    @Override
    public void onWorldUnload(WorldUnloadEvent event) {
    	if (!event.isCancelled()) {
    		WorldConfig.get(event.getWorld()).timeControl.setLocking(false);
        	WorldManager.clearWorldReference(event.getWorld());
    	}
    }
    
    @Override
    public void onWorldInit(WorldInitEvent event) {
    	if (initIgnoreWorlds.contains(event.getWorld().getName())) {
    		initIgnoreWorlds.remove(event.getWorld().getName());
    		event.getWorld().setKeepSpawnInMemory(false);
    	} else {
    		WorldConfig.get(event.getWorld()).update(event.getWorld());
    	}
    }
    
}
