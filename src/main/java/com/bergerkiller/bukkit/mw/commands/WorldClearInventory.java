package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.inventory.InventoryBaseImpl;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.playerdata.PlayerDataFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class WorldClearInventory extends Command {

    public WorldClearInventory() {
        super(Permission.COMMAND_INVENTORY_CLEAR, "world.clearinventory");
    }

    @Override
    public void execute() {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world clearinventory <player> <world>");
            return;
        }

        String playerInput = args[0];
        String worldName = args[1];

        OfflinePlayer targetPlayer = getOfflinePlayer(playerInput);

        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerInput + "' not found!");
            return;
        }

        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found!");
            return;
        }

        WorldConfig worldConfig = WorldConfig.get(targetWorld);

        try {
            PlayerDataFile playerData = new PlayerDataFile(targetPlayer, worldConfig);

            if (targetPlayer.isOnline()) {
                Player player = targetPlayer.getPlayer();

                if (!player.getWorld().equals(targetWorld)) {
                    playerData.updateIfExists(data -> {
                        saveInventory(data, "Inventory", player.getInventory().getContents());
                        saveInventory(data, "EnderItems", player.getEnderChest().getContents());
                    });
                } else {
                    player.getInventory().clear();
                    player.getEnderChest().clear();
                }
            }

            playerData.updateIfExists(data -> {
                data.remove("Inventory");
                data.remove("EnderItems");
            });

            sender.sendMessage(ChatColor.GREEN + "Cleared inventory data for " + targetPlayer.getName() + " in world " + worldName);
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error clearing inventory data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private OfflinePlayer getOfflinePlayer(String input) {
        // First, try to parse as UUID
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try as a player name
        }

        // Try to get online player
        Player onlinePlayer = Bukkit.getPlayerExact(input);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        // Search offline players
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(input)) {
                return offlinePlayer;
            }
        }

        return null;
    }

    private void saveInventory(CommonTagCompound data, String key, ItemStack[] contents) {
        InventoryBaseImpl inventory = new InventoryBaseImpl(contents);
        CommonTagCompound inventoryTag = new CommonTagCompound();
        inventory.getContents(); // This line is needed to ensure the inventory is properly initialized
        data.put(key, inventoryTag);
    }

    @Override
    public List<String> autocomplete() {
        if (args.length == 1) {
            return processPlayerNameAutocomplete();
        } else if (args.length == 2) {
            return processWorldNameAutocomplete();
        }
        return null;
    }
}