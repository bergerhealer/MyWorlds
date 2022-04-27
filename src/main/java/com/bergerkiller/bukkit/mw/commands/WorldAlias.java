package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.stream.Stream;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldAlias extends Command {

    public WorldAlias() {
        super(Permission.COMMAND_ALIAS, "world.alias");
    }

    @Override
    public void execute() {
        this.genConsumeWorldName(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length == 0) {
                message(ChatColor.YELLOW + "Alias of world '" + worldname + "' is set to " + ChatColor.WHITE + wc.alias);
            } else {
                wc.alias = StringUtil.ampToColor(String.join(" ", args));
                message(ChatColor.YELLOW + "Alias of world '" + worldname + "' set to " + ChatColor.WHITE + wc.alias);
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        String[] names = Stream.of(ChatColor.values())
                .map(e -> "&" + e.getChar())
                .toArray(String[]::new);
        return processBasicAutocompleteOrWorldName(names);
    }
}
