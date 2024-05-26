package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.bergerkiller.bukkit.mw.BedRespawnMode;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldBedRespawn extends Command {

    public WorldBedRespawn() {
        super(Permission.COMMAND_SETSPAWN, "world.bedrespawn");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length > 0) {
                wc.setBedRespawnMode(BedRespawnMode.parse(args[0]));
            }

            wc.getBedRespawnMode().showAsMessage(sender, worldname);
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName(Stream.of(BedRespawnMode.values())
                .map(BedRespawnMode::name)
                .map(s -> s.toLowerCase(Locale.ENGLISH))
                .toArray(String[]::new));
    }
}
