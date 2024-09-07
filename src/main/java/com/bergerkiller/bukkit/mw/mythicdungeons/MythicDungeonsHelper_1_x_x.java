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
 * Used for MythicDungeons 1.3.0 / 1.4.0
 */
public class MythicDungeonsHelper_1_x_x implements MythicDungeonsHelper {
    private final MyWorlds myworlds;
    private final MythicDungeons mythicDungeons;
    private final FastField<String> instanceNameField;

    public MythicDungeonsHelper_1_x_x(MyWorlds myworlds, Plugin plugin) {
        this.myworlds = myworlds;

        this.mythicDungeons = (MythicDungeons) plugin;
        this.mythicDungeons.getActiveInstances(); // We need this API to exist, so call it to verify

        // We need to read the instance name field to know the name of the world before the world loads in
        // Instance world is null during this and cannot be used
        // If this fails (code changes?) then hopefully/maybe the next-tick task will fix it.
        this.instanceNameField = LogicUtil.tryCreate(() -> {
            FastField<String> f = new FastField<>(Instance.class.getDeclaredField("instName"));
            if (!f.getField().getType().equals(String.class)) {
                throw new IllegalStateException("Instance name field is invalid");
            }
            return f;
        }, err -> {
            myworlds.getLogger().log(Level.SEVERE, "Failed to identify mythic dungeons instance name field. Might be buggy!", err);
            return null;
        });
    }

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
}
