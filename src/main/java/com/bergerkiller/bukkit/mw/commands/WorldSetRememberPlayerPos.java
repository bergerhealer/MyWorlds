package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldSetRememberPlayerPos extends Command {

    public WorldSetRememberPlayerPos() {
        super(Permission.COMMAND_SETREMEMBERLASTPOS, "world.rememberlastpos");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length > 0) {
                wc.rememberLastPlayerPosition = ParseUtil.parseBool(args[0]);
            }
            if (wc.rememberLastPlayerPosition) {
                message(ChatColor.GREEN + "Player last position memory on World: '" + worldname + "' is enabled");
                message(ChatColor.GREEN + "Players will (upon teleporting) go to their last known positions");
            } else {
                message(ChatColor.YELLOW + "Player last position memory on World: '" + worldname + "' is disabled");
                message(ChatColor.GREEN + "Players will (upon teleporting) always go to the world spawn");
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("yes", "no");
    }
}
