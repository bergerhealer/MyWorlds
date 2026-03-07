package com.bergerkiller.bukkit.mw.mythicdungeons;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.mountiplex.reflection.util.ExtendedClassWriter;
import com.bergerkiller.mountiplex.reflection.util.FastField;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;
import net.playavalon.mythicdungeons.api.MythicDungeonsService;
import org.bukkit.World;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static org.objectweb.asm.Opcodes.*;

/**
 * Used for MythicDungeons 2.0.0+
 */
public class MythicDungeonsHelper_2_0_1 implements MythicDungeonsHelper {
    private final MyWorlds myworlds;
    private final MythicDungeonsService mythicDungeons;
    private final MythicDungeonsAPIHelper apiHelper;
    private final FastField<String> instanceNameField;

    public MythicDungeonsHelper_2_0_1(MyWorlds myworlds, MythicDungeonsService service, MythicDungeonsAPIHelper apiHelper) {
        this.myworlds = myworlds;
        this.mythicDungeons = service;
        this.apiHelper = apiHelper;

        // These types must exist
        Class<?> abstractInstanceType;
        try {
            abstractInstanceType = Class.forName("net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance",
                    true, service.getClass().getClassLoader());
        } catch (Throwable t) {
            throw new UnsupportedOperationException("AbstractDungeon type not found", t);
        }

        // We need to read the instance name field to know the name of the world before the world loads in
        // Instance world is null during this and cannot be used
        // If this fails (code changes?) then hopefully/maybe the next-tick task will fix it.
        this.instanceNameField = LogicUtil.tryCreate(() -> {
            FastField<String> f = new FastField<>(abstractInstanceType.getDeclaredField("instName"));
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
            for (Object abstractDungeon : this.apiHelper.getAllDungeons(this.mythicDungeons)) {
                Object abstractInstance = findAbstractInstance(abstractDungeon, world);
                if (abstractInstance != null) {
                    List<World> result = new ArrayList<>();

                    // Edit session first
                    addToList(result, this.apiHelper.getEditSession(abstractDungeon), world);

                    // Followed by active instances
                    for (Object otherAbstractInstance : this.apiHelper.getInstances(abstractDungeon)) {
                        addToList(result, otherAbstractInstance, world);
                    }

                    // Done.
                    return result;
                }
            }
        } catch (Throwable t) {
            myworlds.getLogger().log(Level.SEVERE, "Failed to check whether world is a mythic dungeons world", t);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isDungeonWorld(World world) {
        try {
            for (Object abstractDungeon : this.apiHelper.getAllDungeons(this.mythicDungeons)) {
                Object abstractInstance = findAbstractInstance(abstractDungeon, world);
                if (abstractInstance != null) {
                    return true;
                }
            }
        } catch (Throwable t) {
            myworlds.getLogger().log(Level.SEVERE, "Failed to check whether world is a mythic dungeons world", t);
        }
        return false;
    }

    private Object findAbstractInstance(Object abstractDungeon, World world) {
        for (Object abstractInstance : this.apiHelper.getInstances(abstractDungeon)) {
            World instanceWorld = this.apiHelper.getInstanceWorld(abstractInstance);
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

            return abstractInstance;
        }
        return null;
    }

    private void addToList(List<World> worlds, Object abstractInstance, World except) {
        if (abstractInstance != null) {
            World w = this.apiHelper.getInstanceWorld(abstractInstance);
            if (w != null && w != except) {
                worlds.add(w);
            }
        }
    }

    public static MythicDungeonsAPIHelper createAPIHelper() throws Throwable {
        ExtendedClassWriter<MythicDungeonsAPIHelper> classWriter = ExtendedClassWriter.builder(MythicDungeonsAPIHelper.class)
                .setFlags(ExtendedClassWriter.COMPUTE_MAXS)
                .setClassLoader(MythicDungeonsService.class.getClassLoader())
                .build();
        MethodVisitor methodVisitor;

        // We need these types
        Class<?> abstractInstanceType = Class.forName("net.playavalon.mythicdungeons.api.parents.instances.AbstractInstance");
        Class<?> abstractDungeonType = Class.forName("net.playavalon.mythicdungeons.api.parents.dungeons.AbstractDungeon");

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getAllDungeons",
                    "(" + MPLType.getDescriptor(MythicDungeonsService.class) + ")Ljava/util/Collection;",
                    "(" + MPLType.getDescriptor(MythicDungeonsService.class) + ")Ljava/util/Collection<Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, MPLType.getInternalName(MythicDungeonsService.class),
                    "getAllDungeons", "()Ljava/util/Collection;", true);
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

        return classWriter.generateInstanceNull();
    }

    /**
     * Implementation of this is generated at runtime using ASM.
     * This is because no API jar is available, and reflection doesn't work due to missing types.
     */
    public interface MythicDungeonsAPIHelper {
        /** Returns a Collection of AbstractDungeon instances that exist */
        Collection<Object> getAllDungeons(MythicDungeonsService service);
        /** Gets the loaded Bukkit world of an abstract instance, if it exists */
        World getInstanceWorld(Object abstractInstance);
        /** Gets the edit session instance of a dungeon, if it exists. */
        Object getEditSession(Object abstractDungeon);
        /** Returns all active AbstractInstance of a dungeon that exist */
        List<Object> getInstances(Object abstractDungeon);
    }
}
