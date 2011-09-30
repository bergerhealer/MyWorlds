package com.bergerkiller.bukkit.mw;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class MWWorldListener extends WorldListener {
	
    @Override
    public void onChunkLoad(ChunkLoadEvent event) {
    	Portal.validate(1, event.getChunk());
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
