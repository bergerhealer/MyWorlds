package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldStartupLoadMode;
import org.bukkit.ChatColor;

import java.util.List;

public class WorldSetAutoLoad extends Command {

    public WorldSetAutoLoad() {
        super(Permission.COMMAND_AUTOLOAD, "world.setautoload");
    }

    public void execute() {
        if (args.length != 0) {
            this.genWorldname(1);
            if (this.handleWorld()) {
                WorldConfig wc = WorldConfig.get(worldname);
                if (ParseUtil.isBool(args[0])) {
                    if (ParseUtil.parseBool(args[0])) {
                        if (wc.getStartupLoadMode() == WorldStartupLoadMode.IGNORE) {
                            wc.setStartupLoadMode(WorldStartupLoadMode.NOT_LOADED.afterWorldLoadedChanged(wc.isLoaded()));
                        }
                    } else {
                        wc.setStartupLoadMode(WorldStartupLoadMode.IGNORE);
                    }
                }
                if (wc.getStartupLoadMode() == WorldStartupLoadMode.IGNORE) {
                    message(ChatColor.YELLOW + "World '" + worldname + "' will not load on startup! Another plugin might load it anyways.");
                } else {
                    message(ChatColor.YELLOW + "World '" + worldname + "' will load on startup if loaded at shutdown!");
                }
            }
        } else {
            showInv();
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("yes", "no");
    }
}
