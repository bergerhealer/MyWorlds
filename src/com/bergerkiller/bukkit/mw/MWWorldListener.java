package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldListener;

public class MWWorldListener extends WorldListener {
    private final MyWorlds plugin;

    public MWWorldListener(final  MyWorlds plugin) {
        this.plugin = plugin;
    }
	
    @Override
    public void onChunkLoad(ChunkLoadEvent event) {
    	for (String portal : Portal.getPortals(event.getChunk())) {
    		Portal p = Portal.get(portal);
    		if (p == null) {
    			MyWorlds.log(Level.WARNING, "Auto-removed portal '" + portal + "' because it is no longer there!");
    			Portal.remove(portal);
    		}
    	}
    }
	
}
