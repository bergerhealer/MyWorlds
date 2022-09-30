package com.bergerkiller.bukkit.mw.portal.handlers;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.portal.PortalFilter;
import com.bergerkiller.mountiplex.reflection.declarations.Template;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;

/**
 * Provides access to the BetterPortals "API" to check whether at a given
 * position a BetterPortal is created
 */
public class BetterPortalsHandler implements PortalFilter {
    private final Plugin plugin;
    private final PortalManagerHandle handle;
    private final Object portalManager;

    public BetterPortalsHandler(Plugin plugin) {
        // Shut up class loading warnings by loading it first
        preload("bukkit.BetterPortals");
        preload("bukkit.portal.IPortalManager");
        preload("bukkit.portal.IPortal");
        preload("bukkit.portal.Portal");
        preload("api.PortalPosition");

        this.plugin = plugin;
        this.handle = Template.Class.create(PortalManagerHandle.class);
        this.handle.forceInitialization();
        this.portalManager = this.handle.getPortalManager(plugin);
    }

    private static void preload(String name) {
        Resolver.loadClass("com.lauriethefish.betterportals." + name, true, BetterPortalsHandler.class.getClassLoader());
    }

    @Override
    public boolean isPortalFiltered(PortalType portalType, Block portalBlock) {
        return handle.hasPortalAt(portalManager, portalBlock);
    }

    @Override
    public boolean usesPlugin(Plugin plugin) {
        return this.plugin == plugin;
    }

    @Template.Optional
    @Template.Import("com.lauriethefish.betterportals.bukkit.BetterPortals")
    @Template.Import("com.lauriethefish.betterportals.bukkit.portal.IPortal")
    @Template.Import("com.lauriethefish.betterportals.bukkit.portal.Portal")
    @Template.Import("com.bergerkiller.bukkit.common.utils.MathUtil")
    @Template.InstanceType("com.lauriethefish.betterportals.bukkit.portal.IPortalManager")
    public static abstract class PortalManagerHandle extends Template.Class<Template.Handle> {

        @Template.Generated("public static IPortalManager getPortalManager(BetterPortals plugin) {\n" +
                            "    #require BetterPortals private com.lauriethefish.betterportals.bukkit.portal.IPortalManager portalManager;\n" +
                            "    return plugin#portalManager;\n" +
                            "}")
        public abstract Object getPortalManager(Plugin plugin);

        @Template.Generated("public static boolean hasPortalAt(IPortalManager manager, org.bukkit.block.Block portalBlock) {\n" +
                            "    org.bukkit.World world = portalBlock.getWorld();\n" +
                            "    int x = portalBlock.getX();\n" +
                            "    int y = portalBlock.getY();\n" +
                            "    int z = portalBlock.getZ();\n" +
                            "\n" +
                            "    java.util.Iterator iter = manager.getAllPortals().iterator();\n" +
                            "    while (iter.hasNext()) {\n" +
                            "        Portal portal = (Portal) iter.next();\n" +
                            "        org.bukkit.Location origin = portal.getOriginPos().getLocation();\n" +
                            "        if (origin.getWorld() != world) {\n" +
                            "            continue;\n" +
                            "        }\n" +
                            "        org.bukkit.util.Vector size = portal.getSize();\n" +
                            "\n" +
                            "        int min_x = MathUtil.floor(origin.getX() - 0.5 * size.getX());\n" +
                            "        int min_y = MathUtil.floor(origin.getY() - 0.5 * size.getY());\n" +
                            "        int min_z = MathUtil.floor(origin.getZ() - 0.5 * size.getZ());\n" +
                            "        int max_x = MathUtil.floor(origin.getX() + 0.5 * size.getX());\n" +
                            "        int max_y = MathUtil.floor(origin.getY() + 0.5 * size.getY());\n" +
                            "        int max_z = MathUtil.floor(origin.getZ() + 0.5 * size.getZ());\n" +
                            "\n" +
                            "        if (x >= min_x && x <= max_x &&\n" +
                            "            z >= min_z && z <= max_z &&\n" +
                            "            y >= min_y && y <= max_y\n" +
                            "        ) {\n" +
                            "            return true;\n" +
                            "        }\n"+
                            "    }\n" +
                            "    return false;\n" +
                            "}")
        public abstract boolean hasPortalAt(Object manager, Block portalBlock);
    }
}
