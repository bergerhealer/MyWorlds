package com.bergerkiller.bukkit.mw;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class MWEntityListener extends EntityListener {

    @Override
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    	if (MyWorlds.onlyObsidianPortals) {
    		Block b = event.getLocation().getBlock();
    		if (b.getType() == Material.PORTAL) {
    			if (!Portal.isPortal(b)) return;
    		}
    	}
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
