package com.bergerkiller.bukkit.mw.commands;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.PortalItemType;

public class WorldGivePortal extends Command {

    public WorldGivePortal() {
        super(Permission.COMMAND_GIVE_PORTAL, "world.giveportal");
    }

    public void execute() {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }

        String typeStr = "nether";
        if (args.length >= 1) {
            typeStr = args[0].toLowerCase(Locale.ENGLISH);
        }

        PortalItemType itemType = null;
        for (PortalItemType possibleItemType : PortalItemType.values()) {
            if (typeStr.contains(possibleItemType.getName())) {
                itemType = possibleItemType;
                break;
            }
        }
        if (itemType == null) {
            player.sendMessage(ChatColor.RED + "Unknown portal type: " + typeStr);
            return;
        }

        Player player = (Player) sender;
        player.sendMessage(ChatColor.GREEN + "You've been given infinite " + ChatColor.YELLOW + itemType.getVisualName() + ChatColor.GREEN + " blocks");
        player.getInventory().addItem(itemType.createItem());
    }
}
