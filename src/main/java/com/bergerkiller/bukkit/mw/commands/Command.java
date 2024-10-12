package com.bergerkiller.bukkit.mw.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.bergerkiller.bukkit.mw.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;

public abstract class Command {
    public MyWorlds plugin;
    public Permission permission;
    public String commandNode;
    public Player player;
    public CommandSender sender;
    public String commandRootLabel;
    public String commandLabel;
    public String[] args;
    public String worldname;
    public WorldMode forcedWorldMode;

    public Command(Permission permission, String commandNode) {
        this.permission = permission;
        this.commandNode = commandNode;
    }

    public void init(MyWorlds plugin, CommandSender sender, String commandRootLabel, String commandLabel, String[] args) {
        this.plugin = plugin;
        this.sender = sender;
        this.commandRootLabel = commandRootLabel;
        this.commandLabel = commandLabel;
        this.args = args;
        if (sender instanceof Player) {
            this.player = (Player) sender;
        }
    }

    /**
     * Main execute function
     */
    public abstract void execute();

    /**
     * Override to completely handle the auto-completion list
     * 
     * @return autocomplete list
     * @see {@link 
     */
    public List<String> autocomplete() {
        return null;
    }

    /**
     * Whether the console can use this Command
     * 
     * @return True if the console can use it, False if not
     */
    public boolean allowConsole() {
        return true;
    }

    /**
     * Removes a single argument from the arguments of this command and returns it
     * 
     * @param index of the argument to remove
     * @return removed argument
     */
    public String removeArg(int index) {
        String value = this.args[index];
        this.args = StringUtil.remove(this.args, index);
        return value;
    }

    public boolean hasPermission() {
        if (this.permission == null) {
            return true;
        }
        if (this.player == null) {
            return this.allowConsole();
        } else {
            return this.permission.has(this.player);
        }
    }

    public boolean handleWorld() {
        if (this.worldname == null) {
            locmessage(Localization.WORLD_NOTFOUND);
        }
        return this.worldname != null;
    }
    
    public void messageNoSpout() {
        if (MyWorlds.isSpoutPluginEnabled) return;
        this.message(ChatColor.YELLOW + "Note that Spout is not installed right now!");
    }
    public void message(String msg) {
        if (msg == null) return;
        CommonUtil.sendMessage(this.sender, msg);
    }

    public void locmessage(Localization node, String... arguments) {
        node.message(this.sender, arguments);
    }

    public void logAction(String action) {
        plugin.logAction(this.sender, action);
    }

    public boolean showInv() {
        return this.showInv(this.commandNode);
    }

    public boolean showInv(String node) {
        message(ChatColor.RED + "Invalid arguments for this command!");
        return showUsage(node);
    }

    public boolean showUsage() {
        return showUsage(this.commandNode);
    }

    public boolean showUsage(String commandNode) {
        if (hasPermission()) {
            this.sender.sendMessage(plugin.getCommandUsage(commandNode));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Simple auto-completion with fixed values.
     * Assumes only one parameter is accepted, and so only handles
     * the first parameter.
     * 
     * @param values
     * @return autocomplete list
     */
    public List<String> processBasicAutocomplete(String... values) {
        if (args.length > 1) {
            return null;
        } else {
            return processAutocomplete(Stream.of(values));
        }
    }

    /**
     * Performs autocompletion on the first arguments using the values specified.
     * Following arguments are treated as the world name
     * 
     * @param values
     * @return autocomplete list
     */
    public List<String> processBasicAutocompleteOrWorldName(String... values) {
        if (args.length > 1) {
            return processWorldNameAutocomplete();
        } else {
            return processBasicAutocomplete(values);
        }
    }

    /**
     * Performs auto-completion of the last argument as a world name
     * 
     * @return autocomplete list
     */
    public List<String> processWorldNameAutocomplete() {
        return processAutocomplete(WorldConfigStore.all().stream().map(cfg -> cfg.worldname));
    }

    /**
     * Performs auto-completion of the last argument as a list of player names
     * 
     * @return autocomplete list
     */
    public List<String> processPlayerNameAutocomplete() {
        return processAutocomplete(Bukkit.getOnlinePlayers().stream().map(Player::getName));
    }

    /**
     * Processes a stream of possible auto-complete suggestions
     * and produces possible values that match the current argument
     * the player is typing.
     * 
     * @param values
     * @return autocomplete list
     */
    public List<String> processAutocomplete(Stream<String> values) {
        final String match = (args.length == 0) ? "" : args[args.length-1].toLowerCase(Locale.ENGLISH);
        List<String> result = values.filter(v -> v.toLowerCase(Locale.ENGLISH).startsWith(match)).collect(Collectors.toList());
        return result.isEmpty() ? null : result;
    }

    public void listPortals(String[] portals) {
        MessageBuilder builder = new MessageBuilder();
        builder.green("[Very near] ").dark_green("[Near] ").yellow("[Far] ");
        builder.red("[Other world] ").dark_red("[Unavailable]").newLine();
        builder.yellow("Available portals: ").white(portals.length, " Portal");
        if (portals.length != 1) builder.append('s');
        if (portals.length > 0) {
            builder.setIndent(2).setSeparator(ChatColor.WHITE, " / ").newLine();
            final Location ploc;
            if (sender instanceof Player) {
                ploc = ((Player) sender).getLocation();
            } else {
                ploc = null;
            }
            for (String portal : portals) {
                Location loc = Portal.getPortalLocation(portal, null);
                if (loc != null && ploc != null) {
                    if (ploc.getWorld() == loc.getWorld()) {
                        double d = ploc.distance(loc);
                        if (d <= 10) {
                            builder.green(portal);
                        } else if (d <= 100) {
                            builder.dark_green(portal);
                        } else {
                            builder.yellow(portal);
                        }
                    } else {
                        builder.red(portal);
                    }
                } else {
                    builder.dark_red(portal);
                }
            }
        }
        builder.send(sender);
    }

    /**
     * Finds out the world to operate in, checking the command arguments if possible.
     * 
     * @param preceedingArgCount expected before the world argument
     * @return True if a world was taken from the last arg, False if not
     */
    public boolean genWorldname(int preceedingArgCount) {
        if (args.length > 0 && args.length > preceedingArgCount) {
            this.worldname = WorldManager.matchWorld(args[args.length - 1]);
            if (this.worldname != null) {
                return true;
            }
        }
        if (player != null) {
            this.worldname = player.getWorld().getName();
        } else {
            this.worldname = WorldUtil.getWorlds().iterator().next().getName();
        }
        return false;
    }

    /**
     * Same as {@link #genWorldname(int)} but removes the last argument
     * if parsed.
     *
     * @param preceedingArgCount expected before the world argument
     */
    public void genConsumeWorldName(int preceedingArgCount) {
        if (genWorldname(preceedingArgCount)) {
            args = StringUtil.remove(args, args.length - 1);
        }
    }

    /**
     * Reads a World Mode set on the world name using the /-parameter
     * For example, /world create world1/nether will read the nether forced mode.
     * IF // is used, then it is seen as part of the world name, and replaced with
     * a single /
     */
    public void genForcedWorldMode() {
        this.forcedWorldMode = null;

        for (int i = 0; i < this.worldname.length() - 1; i++) {
            char c = this.worldname.charAt(i);
            if (c == '/') {
                if (this.worldname.charAt(i + 1) == '/') {
                    // Unescape // -> /
                    this.worldname = this.worldname.substring(0, i) + this.worldname.substring(i + 1);
                    continue;
                }

                // Forced world mode identified
                this.forcedWorldMode = WorldMode.get(this.worldname.substring(i + 1), WorldMode.NORMAL);
                this.worldname = this.worldname.substring(0, i);
                break;
            }
        }
    }

    /**
     * Extracts the generator name including generator arguments from the world name previously parsed<br>
     * Requires a worldname to be generated first
     * 
     * @return generator name and arguments, or null if there are none
     */
    public String getGeneratorName() {
        String gen = null;
        int gen_start = this.worldname.indexOf(':');
        if (gen_start != -1) {
            gen = this.worldname.substring(gen_start + 1);
            this.worldname = this.worldname.substring(0, gen_start);
        } else {
            // Use defaults from server.properties when creating a world and no args are set
            gen = WorldManager.readDefaultGeneratorSettings();
            if (!gen.isEmpty() && !gen.equals("{}")) {
                plugin.log(Level.INFO, "Using world generator settings from server.properties: " + gen);
                gen = ":" + gen;
            } else {
                gen = null;
            }
        }
        return gen;
    }

    public void loadSpawnArea(World world) {
        //load chunks
        final int keepdimension = 11;
        final int keepedgedim = 2 * keepdimension + 1;
        final int total = keepedgedim * keepedgedim;

        // Callback called every time a new chunk is loaded (or fails to load-)
        // Show a progress message every so often
        final AtomicInteger lastPercentage = new AtomicInteger(-10);
        final IntConsumer onChunkLoaded = loadedCount -> {
            int percent = (loadedCount==total) ? 100 : (100 * loadedCount / total);
            if ((percent - lastPercentage.get()) == 10) {
                lastPercentage.set(percent);
                message(ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
                plugin.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
            }
        };
        onChunkLoaded.accept(0); // Initial

        // Start loading all the spawn chunks asynchronously
        // Every time a new chunk is loaded, increment a counter
        int spawnx = world.getSpawnLocation().getBlockX() >> 4;
        int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
        final List<ForcedChunk> forcedChunks = new ArrayList<ForcedChunk>();
        final List<CompletableFuture<?>> loadFutures = new ArrayList<>();
        final AtomicInteger chunkCtr = new AtomicInteger();
        for (int x = -keepdimension; x <= keepdimension; x++) {
            for (int z = -keepdimension; z <= keepdimension; z++) {
                int cx = spawnx + x;
                int cz = spawnz + z;
                ForcedChunk chunk = WorldUtil.forceChunkLoaded(world, cx, cz);
                loadFutures.add(chunk.getChunkAsync().whenComplete((a, t) -> {
                    onChunkLoaded.accept(chunkCtr.incrementAndGet());
                }));
                forcedChunks.add(chunk);
            }
        }

        // Run this once loading of spawn completes
        CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, error) -> {
                    // If something went wrong, log it and show a message
                    // Still do the post-load logic, because that stuff must run anyway
                    if (error != null) {
                        MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to load some spawn chunks of " + world.getName(), error);
                        message(ChatColor.RED + "Failed to fully load spawn area of '" + world.getName() + "': " + error.getMessage());
                    }

                    WorldConfig loaded_wc = WorldConfig.get(world);

                    // Set to True, any mistakes in loading chunks will be corrected here
                    world.setKeepSpawnInMemory(true);

                    // Fix up the spawn location
                    loaded_wc.fixSpawnLocation();

                    // Call onLoad (it was ignored while initing to avoid chunk loads when finding spawn)
                    loaded_wc.onWorldLoad(world);

                    // It's now safe to release the chunks
                    forcedChunks.forEach(ForcedChunk::close);
                    forcedChunks.clear();

                    // Confirmation message
                    message(ChatColor.GREEN + "World '" + world.getName() + "' has been loaded and is ready for use!");
                    plugin.log(Level.INFO, "World '"+ world.getName() + "' loaded.");
                });
    }
}
