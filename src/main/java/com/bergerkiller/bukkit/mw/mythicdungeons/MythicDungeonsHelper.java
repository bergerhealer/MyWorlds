package com.bergerkiller.bukkit.mw.mythicdungeons;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.mountiplex.reflection.util.FastField;
import net.playavalon.mythicdungeons.MythicDungeons;
import net.playavalon.mythicdungeons.dungeons.Dungeon;
import net.playavalon.mythicdungeons.dungeons.Instance;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Uses the mythic dungeons API for some extra integrations in MyWorlds
 */
public interface MythicDungeonsHelper {
    MythicDungeonsHelper DISABLED = world -> Collections.emptyList();

    /**
     * Asks Mythic Dungeon whether the World is a dungeon, and if it is, returns all
     * worlds that are instances or "edit instances" of that world. The edit instance
     * is pushed to the front of the list. Input world is excluded from the results.
     *
     * @param world World
     * @return List of related dungeon worlds. If there are no instances except for
     *         the edit instance, or the world isn't a dungeon world, an empty List
     *         is returned.
     */
    List<World> getSameDungeonWorlds(World world);

    static MythicDungeonsHelper init(final MyWorlds myworlds, Plugin plugin) {
        boolean hasAbstractInstanceAPI = false;
        try {
            Class.forName("net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance",
                    false, plugin.getClass().getClassLoader());
            hasAbstractInstanceAPI = true;
        } catch (Throwable t) {}

        if (hasAbstractInstanceAPI) {
            // This is used with the 2.x.x API
            return new MythicDungeonsHelper_2_x_x(myworlds, plugin);
        } else {
            // For the 1.x.x there is an Instance class that is used
            return new MythicDungeonsHelper_1_x_x(myworlds, plugin);
        }
    }
}
