package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportCommandEvent;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;
import org.bukkit.entity.Player;

public class WorldRejoin extends Command {

    public WorldRejoin() {
        super(Permission.COMMAND_REJOIN, "world.rejoin");
    }

    @Override
    public boolean hasPermission() {
        if (this.player == null) {
            return this.allowConsole();
        } else if (Permission.COMMAND_REJOIN_OTHERS.has(player)) {
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

    @Override
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
                WorldManager.teleportToExact(player, WorldManager.getPlayerRejoinPosition(player, world), (entity, to) -> new MyWorldsTeleportCommandEvent(
                        entity,
                        MyWorldsTeleportCommandEvent.CommandType.REJOIN,
                        to
                ));
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}
