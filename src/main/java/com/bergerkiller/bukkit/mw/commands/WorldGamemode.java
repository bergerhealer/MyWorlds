package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldGamemode extends Command {

    public WorldGamemode() {
        super(Permission.COMMAND_GAMEMODE, "world.gamemode");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            if (args.length == 0) {
                //display
                GameMode mode = WorldConfig.get(worldname).gameMode;
                String msg = ChatColor.YELLOW + "Current game mode of world '" + worldname + "': ";
                if (mode == null) {
                    message(msg + ChatColor.YELLOW + "Not set");
                } else {
                    message(msg + ChatColor.YELLOW + mode.name().toLowerCase());
                }
            } else {
                // Try to parse the gamemode as integer first
                GameMode mode;
                if (ParseUtil.isNumeric(args[0])) {
                    int value = ParseUtil.parseInt(args[0], null);
                    mode = GameMode.getByValue(value);
                } else {
                    mode = ParseUtil.parseEnum(GameMode.class, args[0], null);
                }

                // Parse the gamemode
                WorldConfig wc = WorldConfig.get(worldname);
                wc.setGameMode(mode);
                if (mode == null) {
                    message(ChatColor.YELLOW + "Game mode of World '" + worldname + "' cleared!");
                } else {
                    message(ChatColor.YELLOW + "Game mode of World '" + worldname + "' set to " + mode.name().toLowerCase() + "!");
                }
            }
        }
    }
    
}
