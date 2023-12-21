package com.bergerkiller.bukkit.mw.mythicdungeons;

import com.bergerkiller.bukkit.mw.MyWorlds;
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
    MythicDungeonsHelper DISABLED = new MythicDungeonsHelper() {
        @Override
        public World getEditSession(World world) {
            return null;
        }

        @Override
        public List<World> getSameDungeonWorlds(World world) {
            return Collections.emptyList();
        }
    };

    /**
     * Asks Mythic Dungeon whether the World is a dungeon, and if it is, checks if
     * the world specified is an instance. If it is, and it is not the edit session
     * world, then looks up the associated dungeon and returns the edit session world.
     *
     * @param world World
     * @return Edit Session world of the same dungeon, if World is a dungeon instance.
     *         Null if the world isn't a mythic dungeons instance.
     */
    World getEditSession(World world);

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
        final MythicDungeons mythicDungeons = (MythicDungeons) plugin;
        mythicDungeons.getActiveInstances(); // We need this API to exist, so call it to verify

        return new MythicDungeonsHelper() {
            @Override
            public World getEditSession(World world) {
                try {
                    for (Instance instance : mythicDungeons.getActiveInstances()) {
                        if (instance.getInstanceWorld() == world) {
                            Instance editInstance = instance.getDungeon().getEditSession();
                            if (editInstance != null) {
                                World w = editInstance.getInstanceWorld();
                                if (w != null && w != world) {
                                    return w;
                                }
                            }
                            break;
                        }
                    }
                } catch (Throwable t) {
                    myworlds.getLogger().log(Level.SEVERE, "Failed to check whether world is a mythic dungeons world", t);
                }
                return null;
            }

            @Override
            public List<World> getSameDungeonWorlds(World world) {
                try {
                    for (Instance instance : mythicDungeons.getActiveInstances()) {
                        if (instance.getInstanceWorld() != world) {
                            continue;
                        }

                        Dungeon dungeon = instance.getDungeon();
                        List<World> result = new ArrayList<>();

                        // Edit session first
                        addToList(result, dungeon.getEditSession(), world);

                        // Followed by active instances
                        for (Instance otherInstance : dungeon.getInstances()) {
                            addToList(result, otherInstance, world);
                        }

                        // Done.
                        return result;
                    }
                } catch (Throwable t) {
                    myworlds.getLogger().log(Level.SEVERE, "Failed to check whether world is a mythic dungeons world", t);
                }
                return Collections.emptyList();
            }

            private void addToList(List<World> worlds, Instance instance, World except) {
                if (instance != null) {
                    World w = instance.getInstanceWorld();
                    if (w != null && w != except) {
                        worlds.add(w);
                    }
                }
            }
        };
    }
}
