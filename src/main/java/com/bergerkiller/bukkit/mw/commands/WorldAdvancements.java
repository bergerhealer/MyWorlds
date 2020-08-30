package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldAdvancements extends Command {

    public WorldAdvancements() {
        super(Permission.COMMAND_ADVANCEMENTS, "world.advancements");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (ParseUtil.isBool((args.length>0)?args[0]:"")) {
                wc.advancementsEnabled = ParseUtil.parseBool(args[0]);
                {
                    World w = wc.getWorld();
                    if (w != null) {
                        wc.updateAdvancements(w);
                    }
                }
                if (wc.advancementsEnabled) {
                    message(ChatColor.YELLOW + "Players can now get awarded advancements on world '" + worldname + "'!");
                } else {
                    message(ChatColor.YELLOW + "Players will " + ChatColor.RED + "not " + ChatColor.YELLOW +
                            "get awarded new advancements on world '" + worldname + "'!");
                }
            } else {
                if (wc.advancementsEnabled) {
                    message(ChatColor.YELLOW + "Players can get awarded advancements on world '" + worldname + "'!");
                } else {
                    message(ChatColor.YELLOW + "Players do " + ChatColor.RED + "not " + ChatColor.YELLOW +
                            "get advancements awarded on world '" + worldname + "'!");
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("enabled", "disabled");
    }
}
