package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.RespawnPoint;
import com.bergerkiller.bukkit.mw.WorldConfig;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;

public class WorldSetSpawn extends Command {

    public WorldSetSpawn() {
        super(Permission.COMMAND_SETSPAWN, "world.setspawn");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    @Override
    public boolean allowCommandBlocks() {
        return true;
    }

    public void execute() {
        // If three args are specified, consume those as x/y/z
        OptionalArg overrideX = null;
        OptionalArg overrideY = null;
        OptionalArg overrideZ = null;
        OptionalArg overrideYaw = null;
        OptionalArg overridePitch = null;
        if (args.length >= 3) {
            overrideX = consumeOptionalArg();
            overrideY = consumeOptionalArg();
            overrideZ = consumeOptionalArg();

            if (args.length >= 2) {
                overrideYaw = consumeOptionalArg();
                overridePitch = consumeOptionalArg();
            }
        }

        // An extra arg is possible to set the world name to apply it to
        this.genWorldname(0);
        if (this.handleWorld()) {
            WorldConfig config = WorldConfig.get(worldname);
            Location loc;
            if (player != null) {
                loc = player.getLocation();
            } else if (sender instanceof BlockCommandSender) {
                BlockCommandSender bcmd = (BlockCommandSender) sender;
                loc = bcmd.getBlock().getLocation();
                loc.setX(loc.getBlockX() + 0.5);
                loc.setY(loc.getBlockY() + 1.0);
                loc.setZ(loc.getBlockZ() + 0.5);
            } else {
                // Shouldn't be reached (hasPermission())
                sender.sendMessage("You cannot use this");
                return;
            }

            // Overrides
            boolean hasOverrides = false;
            if (overrideX != null) {
                loc.setX(overrideX.apply(loc.getX()));
                hasOverrides = true;
            }
            if (overrideY != null) {
                loc.setY(overrideY.apply(loc.getY()));
                hasOverrides = true;
            }
            if (overrideZ != null) {
                loc.setZ(overrideZ.apply(loc.getZ()));
                hasOverrides = true;
            }
            if (overrideYaw != null) {
                loc.setYaw(overrideYaw.apply(loc.getYaw()));
            }
            if (overridePitch != null) {
                loc.setPitch(overridePitch.apply(loc.getPitch()));
            }

            String posText = hasOverrides
                    ? " set to [" + loc.getBlockX() + " / " + loc.getBlockY() + " / " + loc.getBlockZ() + "]"
                    : "your position";

            if (config.getWorld() == loc.getWorld()) {
                // Set spawn point of this World
                config.setSpawnLocation(loc);
                sender.sendMessage(ChatColor.GREEN + "Spawn location of world '" + worldname + "' set to " + posText + "!");
            } else {
                // Set a respawn point from that world to the player's Location
                config.respawnPoint = new RespawnPoint.RespawnPointLocation(loc);
                sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to " + posText + "!");
                sender.sendMessage(ChatColor.YELLOW + "Note: You can also use /world respawn [options...]");
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }

    private OptionalArg consumeOptionalArg() {
        String text = removeArg(0);
        boolean relative = false;
        if (text.startsWith("~")) {
            relative = true;
            text = text.substring(1);
        }
        Double parsed = ParseUtil.parseDouble(text, null);
        if (parsed != null) {
            return new OptionalArg(parsed, relative);
        } else {
            return null;
        }
    }

    private static class OptionalArg {
        public final double value;
        public final boolean relative;

        public OptionalArg(double value, boolean relative) {
            this.value = value;
            this.relative = relative;
        }

        public double apply(double originalValue) {
            return relative ? (originalValue + value) : value;
        }

        public float apply(float originalValue) {
            return relative ? (float) (originalValue + value) : (float) value;
        }
    }
}
