package com.bergerkiller.bukkit.mw.commands;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;

public class WorldGivePortal extends Command {

    public WorldGivePortal() {
        super(Permission.COMMAND_GIVE_PORTAL, "world.giveportal");
    }

    public void execute() {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }
        Player player = (Player) sender;
        Material type = Material.PORTAL;
        Material visualType = Material.STAINED_GLASS;
        int visualData = 2;
        String visualName = "Nether Portal";
        if (args.length == 1) {
            String typeStr = args[0].toLowerCase(Locale.ENGLISH);
            if (typeStr.contains("gate")) {
                // Only available >= 1.10.2
                Material m = ParseUtil.parseEnum(Material.class, "END_GATEWAY", null);
                if (m != null) {
                    type = m;
                    visualType = Material.COAL_BLOCK;
                    visualData = 0;
                    visualName = "End Gateway";
                }
            } else if (typeStr.contains("end")) {
                type = Material.ENDER_PORTAL;
                visualData = 15;
                visualName = "Ender Portal";
            }
        }
        ItemStack item = ItemUtil.createItem(visualType, visualData, 1);
        ItemUtil.setDisplayName(item, visualName + " (My Worlds)");
        ItemUtil.getMetaTag(item).putValue("myworlds.specialportal", type.getId());
        player.sendMessage(ChatColor.GREEN + "You've been given infinite " + ChatColor.YELLOW + visualName + ChatColor.GREEN + " blocks");
        player.getInventory().addItem(item);
    }
}
