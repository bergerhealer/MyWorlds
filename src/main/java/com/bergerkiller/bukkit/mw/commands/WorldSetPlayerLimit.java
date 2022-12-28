package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldSetPlayerLimit extends Command {
    private static final String[] AUTO_COMPLETE = Stream.concat(IntStream.range(0, 30)
            .mapToObj(Integer::toString), Stream.of("disabled")).toArray(String[]::new);

    public WorldSetPlayerLimit() {
        super(Permission.COMMAND_PLAYERLIMIT, "world.playerlimit");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length == 0) {
                // Display
                if (wc.playerLimit <= -1) {
                    message(ChatColor.YELLOW + "World '" + worldname + "' " + ChatColor.RED + "has no player limit set");
                } else {
                    message(ChatColor.YELLOW + "World '" + worldname + "' has a player limit of " + ChatColor.WHITE + wc.playerLimit);
                }
            } else {
                // Set
                int limit;
                if (ParseUtil.isBool(args[0]) && !ParseUtil.parseBool(args[0])) {
                    limit = -1;
                } else {
                    limit = ParseUtil.parseInt(args[0], -1);
                }

                wc.playerLimit = limit;

                if (limit <= -1) {
                    message(ChatColor.YELLOW + "Player limit of World: '" + worldname + "' " + ChatColor.RED + "DISABLED");
                } else {
                    message(ChatColor.YELLOW + "Player limit of World: '" + worldname + "' set to " + ChatColor.WHITE + limit);
                    if (wc.getNumberOfPlayersLimited() > limit) {
                        message(ChatColor.RED + "More players than this are currently on this world. Use /world evacuate to clear them all out.");
                    }
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName(AUTO_COMPLETE);
    }
}
