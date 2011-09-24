package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class MWWorldListener extends WorldListener {
	
    @Override
    public void onChunkLoad(ChunkLoadEvent event) {
		for (String portalname : Portal.getPortals(event.getChunk())) {
    		Portal p = Portal.get(portalname);
    		if (p == null) {
    			MyWorlds.log(Level.WARNING, "Auto-removed portal '" + portalname + "' because it is no longer there!");
    			Portal.remove(portalname);
    		}
		}
    }
    
    @Override
    public void onWorldLoad(WorldLoadEvent event) {
    	TimeControl.setLocking(event.getWorld().getName(), true);
    }
    
    @Override
    public void onWorldUnload(WorldUnloadEvent event) {
    	if (!event.isCancelled()) {
        	TimeControl.setLocking(event.getWorld().getName(), false);
        	WorldManager.clearWorldReference(event.getWorld());
    	}
    }
    
}
