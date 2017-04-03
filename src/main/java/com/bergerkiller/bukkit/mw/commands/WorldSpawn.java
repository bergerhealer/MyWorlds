package com.bergerkiller.bukkit.mw.commands;

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
                EntityUtil.teleport(player, WorldManager.getSpawnLocation(world));
            } else {
                Localization.WORLD_NOTLOADED.message(sender, worldname);
            }
        }
    }
}
