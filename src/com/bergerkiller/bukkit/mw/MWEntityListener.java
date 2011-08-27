package com.bergerkiller.bukkit.mw;

import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class MWEntityListener extends EntityListener {

    @Override
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
    	Portal.handlePortalEnter(event.getEntity());
    }
    
}
