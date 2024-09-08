package com.bergerkiller.bukkit.mw.commands.registry;

import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.commands.*;

/**
 * All subcommands of the /world command
 */
public class WorldCommandRegistry extends CommandRegistry {

    public boolean isWorldCommand(String cmdLabel) {
        return cmdLabel.equalsIgnoreCase("world") ||
               cmdLabel.equalsIgnoreCase("myworlds") ||
               cmdLabel.equalsIgnoreCase("worlds") ||
               cmdLabel.equalsIgnoreCase("mw");
    }

    @Override
    protected void registerAll() {
        register(WorldList::new, "list");
        register(WorldInfo::new, "info", "i");
        register(WorldPortals::new, "portals", "portal");
        register(WorldGivePortal::new, "giveportal");
        register(WorldClearInventory::new, "clearinventory", "clearinv");
        register(WorldLoad::new, "load");
        register(WorldUnload::new, "unload");
        register(WorldCreate::new, "create");
        register(WorldSpawn::new, "spawn");
        register(WorldRejoin::new, "rejoin");
        register(WorldEvacuate::new, "evacuate", "evac");
        register(WorldRepair::new, "repair", "rep");
        register(WorldSave::new, "save");
        register(WorldDelete::new, "delete", "del");
        register(WorldCopy::new, "copy");
        register(WorldSetPVP::new, "pvp", "togglepvp", "tpvp");
        register(WorldWeather::new, "weather", "w");
        register(WorldTime::new, "time", "t");
        register(() -> new WorldSpawning(true), "allowspawn", "spawnallow", "allowspawning");
        register(() -> new WorldSpawning(false), "denyspawn", "spawndeny", "denyspawning");
        register(() -> new WorldSetDefaultPortal(PortalType.NETHER), "netherportal", "setnetherportal");
        register(() -> new WorldSetDefaultPortal(PortalType.END), "endportal", "setendportal", "setenderportal", "settheendportal");
        register(WorldSetSpawn::new, "setspawn");
        register(WorldGamemode::new, "gamemode", "setgamemode", "gm", "setgm");
        register(WorldListGenerators::new, "listgenerators", "generators", "gen", "listgen");
        register(WorldSetSpawnLoaded::new, "keepspawnloaded", "togglespawnloaded", "spawnloaded");
        register(WorldDifficulty::new, "difficulty", "difficult", "diff");
        register(() -> new WorldOpping(true), "op");
        register(() -> new WorldOpping(false), "deop");
        register(WorldSetSaving::new, "autosave", "setsave", "setsaving", "saving");
        register(WorldManageConfig::new, "config", "cfg");
        register(WorldReloadWE::new, "reloadwhennoplayers", "reloadnoplayers", "reloadempty", "reloadwe", "reloadwhenempty");
        register(WorldFormSnow::new, "formsnow");
        register(WorldFormIce::new, "formice");
        register(TeleportPortal::new, "teleport", "tp");
        register(WorldInventory::new, "inventory", "inv");
        register(WorldRespawn::new, "respawn", "setrespawn");
        register(WorldBedRespawn::new, "bedrespawn", "respawnbed", "bedspawn");
        register(WorldSetRememberPlayerPos::new, "rememberlastposition", "rememberlastpos", "setrememberlastpos",
                "rememberlastplayerpos", "setrememberlastplayerpos", "setrememberlastposition",
                "rememberlastplayerposition", "setrememberlastplayerposition");
        register(WorldSetHunger::new, "hunger", "sethunger");
        register(EnableCredits::new, "showcredits", "enablecredits");
        register(WorldAdvancements::new, "advancements", "setadvancements");
        register(WorldAlias::new, "alias", "setalias");
        register(WorldLastPosition::new, "lastposition", "lastpos");
        register(WorldSetPlayerLimit::new, "playerlimit");
        register(WorldSetPortalOption::new, "setportaloption");
        register(WorldSetAutoLoad::new, "setautoload");
    }

}
