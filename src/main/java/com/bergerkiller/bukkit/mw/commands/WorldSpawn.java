package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportCommandEvent;
import org.bukkit.Location;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;
import org.bukkit.entity.Player;

public class WorldSpawn extends Command {
    
    public WorldSpawn() {
        super(Permission.COMMAND_SPAWN, "world.spawn");
    }

    @Override
    public boolean hasPermission() {
        if (this.player == null) {
            return this.allowConsole();
        } else if (Permission.COMMAND_SPAWN_OTHERS.has(player)) {
            return true;
        } else {
            this.genWorldname(0);
            if (this.worldname == null) {
                return true;
            }
            if (!this.permission.has(this.player, this.worldname)) {
                return false;
            }
            if (args.length > 1) {
                return false; // No permission to teleport other players
            }
            return true;
        }
    }

    @Override
    public boolean allowConsole() {
        return args.length >= 2; // Must specify world name & player name to work from console
    }

    public void execute() {
        this.genWorldname(0);
        if (this.handleWorld()) {
            World world = WorldManager.getWorld(worldname);
            if (world == null) {
                Localization.WORLD_NOTLOADED.message(sender, worldname);
                return;
            }
            Player player = this.player;
            if (args.length >= 2) {
                player = Util.parsePlayerName(sender, args[0]);
                if (player != null) {
                    Localization.COMMAND_REJOIN_OTHER.message(sender, player.getName(), world.getName());
                }
            }
            if (player != null) {
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
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}
