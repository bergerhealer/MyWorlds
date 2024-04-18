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
        final MythicDungeons mythicDungeons = (MythicDungeons) plugin;
        mythicDungeons.getActiveInstances(); // We need this API to exist, so call it to verify

        // We need to read the instance name field to know the name of the world before the world loads in
        // Instance world is null during this and cannot be used
        // If this fails (code changes?) then hopefully/maybe the next-tick task will fix it.
        FastField<String> instanceNameField = LogicUtil.tryCreate(() -> {
            FastField<String> f = new FastField<>(Instance.class.getDeclaredField("instName"));
            if (!f.getField().getType().equals(String.class)) {
                throw new IllegalStateException("Instance name field is invalid");
            }
            return f;
        }, err -> {
            myworlds.getLogger().log(Level.SEVERE, "Failed to identify mythic dungeons instance name field. Might be buggy!", err);
            return null;
        });

        return new MythicDungeonsHelper() {
            @Override
            public List<World> getSameDungeonWorlds(World world) {
                try {
                    for (Instance instance : mythicDungeons.getActiveInstances()) {
                        if (instance.getInstanceWorld() == null) {
                            if (instanceNameField == null) {
                                continue;
                            }
                            String name = instanceNameField.get(instance);
                            if (name == null || !world.getName().equals(name)) {
                                continue;
                            }
                        } else {
                            if (instance.getInstanceWorld() != world) {
                                continue;
                            }
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
