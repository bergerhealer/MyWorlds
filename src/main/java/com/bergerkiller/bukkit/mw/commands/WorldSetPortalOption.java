package com.bergerkiller.bukkit.mw.commands;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.portal.PortalSignList;

public class WorldSetPortalOption extends Command {

    public WorldSetPortalOption() {
        super(Permission.COMMAND_SETPORTALOPTION, "world.setportaloption");
    }

    public void execute() {
        if (args.length < 3) {
            message(ChatColor.RED + "Usage: /" + this.commandRootLabel + " setportaloption <portal> <option> <value>");
            return;
        }

        // Find the portal by name. Prefer on the world player is on.
        PortalSignList.PortalEntry e = null;
        {
            if (player != null) {
                e = plugin.getPortalSignList().findPortalOnWorld(args[0], player.getWorld().getName());
            }
            if (e == null) {
                List<PortalSignList.PortalEntry> l = plugin.getPortalSignList().findPortalsRelaxed(args[0]);
                if (!l.isEmpty()) {
                    e = l.get(0);
                }
            }
            if (e == null) {
                message(ChatColor.RED + "Portal not found: " + args[0]);
                return;
            }
        }

        // Option name and value validation
        if (args[1].equals("playersonly") || args[1].equals("teleportmounts")) {
            // Must be bool
            if (!ParseUtil.isBool(args[2])) {
                message(ChatColor.RED + "Invalid value: " + args[2]);
                return;
            }
        } else {
            message(ChatColor.RED + "Unknown option: " + args[1]);
            return;
        }

        // Passed validation, set it
        e.setOption(args[1], args[2]);
        message(ChatColor.GREEN + "Portal " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + " set " +
                ChatColor.YELLOW + args[1] + ChatColor.GREEN + " to " + ChatColor.YELLOW + args[2]);
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return this.processAutocomplete(Stream.of(Portal.getPortals()));
        } else if (args.length <= 2) {
            return processAutocomplete(Stream.of("playersonly", "teleportmounts"));
        } else if (args.length <= 3) {
            if (args[1].equals("playersonly") || args[1].equals("teleportmounts")) {
                return this.processAutocomplete(Stream.of("yes", "no"));
            }
        }
        return Collections.emptyList();
    }
}
