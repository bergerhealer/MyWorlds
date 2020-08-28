package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldRejoin extends Command {
    
    public WorldRejoin() {
        super(Permission.COMMAND_REJOIN, "world.rejoin");
    }

    @Override
    public boolean hasPermission() {
        if (this.player == null) {
            return this.allowConsole();
        } else {
            this.genWorldname(0);
            if (this.worldname == null) {
                return true;
            }
            return this.permission.has(this.player, this.worldname);
        }
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
                EntityUtil.teleport(player, WorldManager.getPlayerRejoinPosition(player, world));
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
