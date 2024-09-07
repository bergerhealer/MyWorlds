package com.bergerkiller.bukkit.mw.mythicdungeons;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.mountiplex.reflection.util.ExtendedClassWriter;
import com.bergerkiller.mountiplex.reflection.util.FastField;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;
import net.playavalon.mythicdungeons.MythicDungeons;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Used for MythicDungeons 2.0.0+
 */
public class MythicDungeonsHelper_2_x_x implements MythicDungeonsHelper {
    private final MyWorlds myworlds;
    private final MythicDungeons mythicDungeons;
    private final Class<?> abstractInstanceType;
    private final Class<?> abstractDungeonType;
    private final FastField<String> instanceNameField;
    private final MythicDungeonsAPI api;

    public MythicDungeonsHelper_2_x_x(MyWorlds myworlds, Plugin plugin) {
        this.myworlds = myworlds;
        this.mythicDungeons = (MythicDungeons) plugin;

        // These types must exist
        try {
            this.abstractInstanceType = Class.forName("net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance",
                    true, plugin.getClass().getClassLoader());
        } catch (Throwable t) {
            throw new UnsupportedOperationException("AbstractInstance type not found", t);
        }
        try {
            this.abstractDungeonType = Class.forName("net.playavalon.mythicdungeons.api.parents.dungeons.AbstractDungeon",
                    true, plugin.getClass().getClassLoader());
        } catch (Throwable t) {
            throw new UnsupportedOperationException("AbstractDungeon type not found", t);
        }

        ExtendedClassWriter<MythicDungeonsAPI> classWriter = ExtendedClassWriter.builder(MythicDungeonsAPI.class)
                .setFlags(ExtendedClassWriter.COMPUTE_MAXS)
                .setClassLoader(plugin.getClass().getClassLoader())
                .build();
        MethodVisitor methodVisitor;

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getActiveInstances", "(Ljava/lang/Object;)Ljava/util/List;", "(Ljava/lang/Object;)Ljava/util/List<Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, MPLType.getInternalName(MythicDungeons.class));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, MPLType.getInternalName(MythicDungeons.class), "getActiveInstances", "()Ljava/util/List;", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getInstanceWorld", "(Ljava/lang/Object;)" + MPLType.getDescriptor(World.class), null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, MPLType.getInternalName(abstractInstanceType));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, MPLType.getInternalName(abstractInstanceType), "getInstanceWorld", "()" + MPLType.getDescriptor(World.class));
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getDungeon", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, MPLType.getInternalName(abstractInstanceType));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, MPLType.getInternalName(abstractInstanceType), "getDungeon", "()" + MPLType.getDescriptor(abstractDungeonType), false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getEditSession", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, MPLType.getInternalName(abstractDungeonType));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, MPLType.getInternalName(abstractDungeonType), "getEditSession", "()Lnet/playavalon/mythicdungeons/api/parents/instances/InstanceEditable;", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getInstances", "(Ljava/lang/Object;)Ljava/util/List;", "(Ljava/lang/Object;)Ljava/util/List<Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, MPLType.getInternalName(abstractDungeonType));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, MPLType.getInternalName(abstractDungeonType), "getInstances", "()Ljava/util/List;", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }

        this.api = classWriter.generateInstanceNull();

        // We need to read the instance name field to know the name of the world before the world loads in
        // Instance world is null during this and cannot be used
        // If this fails (code changes?) then hopefully/maybe the next-tick task will fix it.
        this.instanceNameField = LogicUtil.tryCreate(() -> {
            FastField<String> f = new FastField<>(this.abstractInstanceType.getDeclaredField("instName"));
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
            for (Object abstractInstance : api.getActiveInstances(mythicDungeons)) {
                World instanceWorld = api.getInstanceWorld(abstractInstance);
                if (instanceWorld == null) {
                    if (instanceNameField == null) {
                        continue;
                    }
                    String name = instanceNameField.get(abstractInstance);
                    if (name == null || !world.getName().equals(name)) {
                        continue;
                    }
                } else {
                    if (instanceWorld != world) {
                        continue;
                    }
                }

                Object abstractDungeon = api.getDungeon(abstractInstance);
                List<World> result = new ArrayList<>();

                // Edit session first
                addToList(result, api.getEditSession(abstractDungeon), world);

                // Followed by active instances
                for (Object otherAbstractInstance : api.getInstances(abstractDungeon)) {
                    addToList(result, otherAbstractInstance, world);
                }

                // Done.
                return result;
            }
        } catch (Throwable t) {
            myworlds.getLogger().log(Level.SEVERE, "Failed to check whether world is a mythic dungeons world", t);
        }
        return Collections.emptyList();
    }

    private void addToList(List<World> worlds, Object abstractInstance, World except) {
        if (abstractInstance != null) {
            World w = api.getInstanceWorld(abstractInstance);
            if (w != null && w != except) {
                worlds.add(w);
            }
        }
    }

    /**
     * Implementation of this is generated at runtime using ASM.
     * This is because no API jar is available, and reflection doesn't work due to missing types.
     */
    public interface MythicDungeonsAPI {
        List<Object> getActiveInstances(Object mythicDungeons);
        World getInstanceWorld(Object abstractInstance);
        Object getDungeon(Object abstractInstance);
        Object getEditSession(Object abstractDungeon);
        List<Object> getInstances(Object abstractDungeon);
    }
}
