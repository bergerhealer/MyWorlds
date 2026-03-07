package com.bergerkiller.bukkit.mw.mythicdungeons;

import com.bergerkiller.bukkit.mw.MyWorlds;
import net.playavalon.mythicdungeons.api.MythicDungeonsService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Uses the mythic dungeons API for some extra integrations in MyWorlds
 */
public interface MythicDungeonsHelper {
    MythicDungeonsHelper DISABLED = new MythicDungeonsHelper() {
        @Override
        public List<World> getSameDungeonWorlds(World world) {
            return Collections.emptyList();
        }

        @Override
        public boolean isDungeonWorld(World world) {
            return false;
        }
    };

    /**
     * Asks Mythic Dungeons whether the World is a dungeon, and if it is, returns all
     * worlds that are instances or "edit instances" of that world. The edit instance
     * is pushed to the front of the list. Input world is excluded from the results.
     *
     * @param world World
     * @return List of related dungeon worlds. If there are no instances except for
     *         the edit instance, or the world isn't a dungeon world, an empty List
     *         is returned.
     */
    List<World> getSameDungeonWorlds(World world);

    /**
     * Asks Mythic Dungeons whether a particular World is a dungeon world instance.
     * If it is, MyWorlds will ensure this world is not loaded on startup at all.
     *
     * @param world World
     * @return True if it is a dungeon world, false if not
     */
    boolean isDungeonWorld(World world);

    static MythicDungeonsHelper init(final MyWorlds myworlds, Plugin plugin) {
        boolean hasAbstractInstanceAPI = false;
        try {
            Class.forName("net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance",
                    false, plugin.getClass().getClassLoader());
            hasAbstractInstanceAPI = true;
        } catch (Throwable t) {}
        if (!hasAbstractInstanceAPI) {
            // For the 1.x.x there is an Instance class that is used
            return new MythicDungeonsHelper_1_x_x(myworlds, plugin);
        }

        // Check to see if the service api is registered and has the "getAllDungeons()" API we need.
        MythicDungeonsService service = null;
        MythicDungeonsHelper_2_0_1.MythicDungeonsAPIHelper apiHelper = null;
        try {
            service = Bukkit.getServicesManager().load(MythicDungeonsService.class);
            if (service != null) {
                apiHelper = MythicDungeonsHelper_2_0_1.createAPIHelper();
                apiHelper.getAllDungeons(service); // Just to check if the method is there and works
            }
        } catch (Throwable t) {
            service = null; // Incompatible
            apiHelper = null;
        }
        if (service == null) {
            // This is used with the 2.0.0 API which lacks getAllDungeons() (?)
            // Maybe we can remove this one tbh.
            return new MythicDungeonsHelper_2_0_0(myworlds, plugin);
        }

        return new MythicDungeonsHelper_2_0_1(myworlds, service, apiHelper);
    }
}
