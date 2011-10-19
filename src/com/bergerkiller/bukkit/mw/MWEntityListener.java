package com.bergerkiller.bukkit.mw;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class MWEntityListener extends EntityListener {

    @Override
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    	Portal.handlePortalEnter(event.getEntity());
    }
    
    @Override
    public void onCreatureSpawn(CreatureSpawnEvent event) {
    	if (!event.isCancelled()) {
        	if (event.getSpawnReason() != SpawnReason.CUSTOM) {
        		if (WorldConfig.get(event.getEntity()).spawnControl.isDenied(event.getEntity())) {
        			event.setCancelled(true);
        		}
        	}
    	}
    }
    
}
