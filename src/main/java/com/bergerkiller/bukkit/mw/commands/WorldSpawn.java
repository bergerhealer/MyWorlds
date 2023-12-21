package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportCommandEvent;
import org.bukkit.Location;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSpawn extends Command {
    
    public WorldSpawn() {
        super(Permission.COMMAND_SPAWN, "world.spawn");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    public void execute() {
        this.genWorldname(0);
        if (this.handleWorld()) {
            World world = WorldManager.getWorld(worldname);
            if (world != null) {
                Location loc = WorldManager.getSpawnLocation(world).clone();
                MyWorldsTeleportCommandEvent event = new MyWorldsTeleportCommandEvent(
                        player,
                        MyWorldsTeleportCommandEvent.CommandType.SPAWN,
                        loc);
                if (CommonUtil.callEvent(event).isCancelled()) {
                    return;
                }
                loc = event.getTo();

                EntityUtil.teleport(player, loc);
            } else {
                Localization.WORLD_NOTLOADED.message(sender, worldname);
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}
