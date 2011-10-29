package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.bergerkiller.bukkit.mw.Configuration.Property;

public class MyWorlds extends JavaPlugin {
	public static Property<Boolean> usePermissions;
	public static Property<Integer> teleportInterval;
	public static Property<Boolean> useWaterTeleport;
	public static Property<Integer> timeLockInterval;
	public static Property<Boolean> useWorldEnterPermissions;
	public static Property<Boolean> usePortalEnterPermissions;
	public static Property<Boolean> useWorldTeleportPermissions;
	public static Property<Boolean> usePortalTeleportPermissions;
	public static Property<Boolean> allowPortalNameOverride;
	public static Property<Boolean> useWorldOperators;
	
	public static MyWorlds plugin;
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[MyWorlds] " + message);
	}
	
	private final MWEntityListener entityListener = new MWEntityListener();
	private final MWBlockListener blockListener = new MWBlockListener();
	private final MWWorldListener worldListener = new MWWorldListener();
	private final MWPlayerListener playerListener = new MWPlayerListener();
	private final MWWeatherListener weatherListener = new MWWeatherListener();
	
	public String root() {
		return getDataFolder() + File.separator;
	}
	
	public void onEnable() {
		plugin = this;

		//Event registering
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.ENTITY_PORTAL_ENTER, entityListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Lowest, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Highest, this); 
        pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Highest, this);  
        pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_UNLOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_INIT, worldListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.WEATHER_CHANGE, weatherListener, Priority.Highest, this); 
        

        
        Configuration config = new Configuration(this);
        usePermissions = config.getProperty("usePermissions", false);
        teleportInterval = config.getProperty("teleportInterval", 2000);
        useWaterTeleport = config.getProperty("useWaterTeleport", true);
        timeLockInterval = config.getProperty("timeLockInterval", 20);
        useWorldEnterPermissions = config.getProperty("useWorldEnterPermissions", false);
        usePortalEnterPermissions = config.getProperty("usePortalEnterPermissions", false);
        useWorldTeleportPermissions = config.getProperty("useWorldTeleportPermissions", false);
        usePortalTeleportPermissions = config.getProperty("usePortalTeleportPermissions", false);
        allowPortalNameOverride = config.getProperty("allowPortalNameOverride", false);
        useWorldOperators = config.getProperty("useWorldOperators", false);
        Property<String> locale = config.getProperty("locale", "default");
        config.init();
        
        //Localization
        Localization.init(this, locale.get());
        
        //Permissions
		Permission.init(this);
		
		//Portals
		Portal.loadPortals(root() + "portals.txt");

		//World info
		WorldConfig.loadAll(root() + "worlds.yml");
		
        //Commands
        getCommand("tpp").setExecutor(this);
        getCommand("world").setExecutor(this);  
        
        //Chunk cache
        WorldManager.initRegionFiles();
        
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println("[MyWorlds] version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		//Portals
		Portal.savePortals(root() + "portals.txt");
		
		//World info
		WorldConfig.saveAll(root() + "worlds.yml");
		
		//Abort chunk loader
		LoadChunksTask.abort();
		
		System.out.println("My Worlds disabled!");
	}
	
	public boolean showInv(CommandSender sender, String command) {
		message(sender, ChatColor.RED + "Invalid arguments for this command!");
		return showUsage(sender, command);
	}
	
	public boolean showUsage(CommandSender sender, String command) {
		if (Permission.has(sender, command)) {
			String msg = Localization.get("help." + command, "");
			if (msg == null) return true;
			message(sender, msg);
			return true;
		} else {
			return false;
		}
	}
	public static void message(Object sender, String message) {
		if (message != null && message.length() > 0) {
			if (sender instanceof CommandSender) {
				if (!(sender instanceof Player)) {
					message = ChatColor.stripColor(message);
				}
				for (String line : message.split("\n")) {
					((CommandSender) sender).sendMessage(line);
				}
			}
		}
	}
	public static void notifyConsole(CommandSender sender, String message) {
		if (sender instanceof Player) {
			log(Level.INFO, ((Player) sender).getName() + " " + message);
		}
	}
	private void listPortals(CommandSender sender, String[] portals) {
		if (sender instanceof Player) {
			message(sender, ChatColor.GREEN + "[Very near] " + 
		            ChatColor.DARK_GREEN + "[Near] " + 
					ChatColor.YELLOW + "[Far] " + 
		            ChatColor.RED + "[Other world] " + 
					ChatColor.DARK_RED + "[Unavailable]");
		}
		message(sender, ChatColor.YELLOW + "Available portals: " + 
				portals.length + " Portal" + ((portals.length == 1) ? "" : "s"));
		if (portals.length > 0) {
			int index = 0;
			for (String portal : portals) {
				Location loc = Portal.getPortalLocation(portal, null);
				ChatColor color = ChatColor.DARK_RED;
				if (loc != null) {
					if (sender instanceof Player) {
						Location ploc = ((Player) sender).getLocation();
						if (ploc.getWorld() == loc.getWorld()) {
							double d = ploc.distance(loc);
							if (d <= 10) {
								color = ChatColor.GREEN;
							} else if (d <= 100) {
								color = ChatColor.DARK_GREEN;
							} else {
								color = ChatColor.YELLOW;
							}
						} else {
							color = ChatColor.RED;
						}
					}
				}
				portals[index] = color + portal;
				index++;
			}
		}
		Util.list(sender, ", ", portals);
	}
	
	public static String[] convertArgs(String[] args) {
		ArrayList<String> tmpargs = new ArrayList<String>();
		boolean isCommenting = false;
		for (String arg : args) {
			if (!isCommenting && (arg.startsWith("\"") || arg.startsWith("'"))) {
				if (arg.endsWith("\"") && arg.length() > 1) {
					tmpargs.add(arg.substring(1, arg.length() - 1));
				} else {
					isCommenting = true;
					tmpargs.add(arg.substring(1));
				}
			} else if (isCommenting && (arg.endsWith("\"") || arg.endsWith("'"))) {
				arg = arg.substring(0, arg.length() - 1);
				arg = tmpargs.get(tmpargs.size() - 1) + " " + arg;
				tmpargs.set(tmpargs.size() - 1, arg);
				isCommenting = false;
			} else if (isCommenting) {
				arg = tmpargs.get(tmpargs.size() - 1) + " " + arg;
				tmpargs.set(tmpargs.size() - 1, arg);
			} else {
				tmpargs.add(arg);
			}
		}
		return tmpargs.toArray(new String[0]);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		//First of all, convert the args so " " is in one arg.
		args = convertArgs(args);
		
		String node = null; //generate a permission node from this command
		if (cmdLabel.equalsIgnoreCase("world")
				|| cmdLabel.equalsIgnoreCase("myworlds")
				|| cmdLabel.equalsIgnoreCase("worlds")
				|| cmdLabel.equalsIgnoreCase("mw")) {
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("list")) {
					node = "world.list";
				} else if (args[0].equalsIgnoreCase("info")) {
					node = "world.info";
				} else if (args[0].equalsIgnoreCase("i")) {
					node = "world.info";
				} else if (args[0].equalsIgnoreCase("portals")) {
					node = "world.portals";
				} else if (args[0].equalsIgnoreCase("portal")) {
					node = "world.portals";
				} else if (args[0].equalsIgnoreCase("load")) {
					node = "world.load";
				} else if (args[0].equalsIgnoreCase("unload")) {
					node = "world.unload";
				} else if (args[0].equalsIgnoreCase("create")) {
					node = "world.create";
				} else if (args[0].equalsIgnoreCase("spawn")) {
					node = "world.spawn";
				} else if (args[0].equalsIgnoreCase("evacuate")) {
					node = "world.evacuate";
				} else if (args[0].equalsIgnoreCase("evac")) {
					node = "world.evacuate";
				} else if (args[0].equalsIgnoreCase("repair")) {
					node = "world.repair";
				} else if (args[0].equalsIgnoreCase("rep")) {
					node = "world.repair";
				} else if (args[0].equalsIgnoreCase("save")) {
					node = "world.save";
				} else if (args[0].equalsIgnoreCase("delete")) {
					node = "world.delete";
				} else if (args[0].equalsIgnoreCase("del")) {
					node = "world.delete";
				} else if (args[0].equalsIgnoreCase("copy")) {
					node = "world.copy";
				} else if (args[0].equalsIgnoreCase("togglepvp")) {
					node = "world.togglepvp";
				} else if (args[0].equalsIgnoreCase("tpvp")) {
					node = "world.togglepvp";	
				} else if (args[0].equalsIgnoreCase("pvp")) {
					node = "world.togglepvp";			
				} else if (args[0].equalsIgnoreCase("weather")) {
				    node = "world.weather";
				} else if (args[0].equalsIgnoreCase("w")) {
				    node = "world.weather";
				} else if (args[0].equalsIgnoreCase("time")) {
				    node = "world.time";
				} else if (args[0].equalsIgnoreCase("t")) {
				    node = "world.time";
				} else if (args[0].equalsIgnoreCase("allowspawn")) {
				    node = "world.allowspawn";
				} else if (args[0].equalsIgnoreCase("denyspawn")) {
				    node = "world.denyspawn";
				} else if (args[0].equalsIgnoreCase("spawnallow")) {
				    node = "world.allowspawn";
				} else if (args[0].equalsIgnoreCase("spawndeny")) {
				    node = "world.denyspawn";
				} else if (args[0].equalsIgnoreCase("allowspawning")) {
				    node = "world.allowspawn";
				} else if (args[0].equalsIgnoreCase("denyspawning")) {
				    node = "world.denyspawn";
				} else if (args[0].equalsIgnoreCase("setportal")) {
					node = "world.setportal";
				} else if (args[0].equalsIgnoreCase("setdefaultportal")) {
					node = "world.setportal";
				} else if (args[0].equalsIgnoreCase("setdefportal")) {
					node = "world.setportal";
				} else if (args[0].equalsIgnoreCase("setdefport")) {
					node = "world.setportal";
				} else if (args[0].equalsIgnoreCase("setspawn")) {
					node = "world.setspawn";
				} else if (args[0].equalsIgnoreCase("gamemode")) {
					node = "world.gamemode";
				} else if (args[0].equalsIgnoreCase("setgamemode")) {
					node = "world.gamemode";
				} else if (args[0].equalsIgnoreCase("gm")) {
					node = "world.gamemode";
				} else if (args[0].equalsIgnoreCase("setgm")) {
					node = "world.gamemode";
				} else if (args[0].equalsIgnoreCase("generators")) {
					node = "world.listgenerators";
				} else if (args[0].equalsIgnoreCase("gen")) {
					node = "world.listgenerators";
				} else if (args[0].equalsIgnoreCase("listgenerators")) {
					node = "world.listgenerators";
				} else if (args[0].equalsIgnoreCase("listgen")) {
					node = "world.listgenerators";
				} else if (args[0].equalsIgnoreCase("togglespawnloaded")) {
					node = "world.togglespawnloaded";
				} else if (args[0].equalsIgnoreCase("spawnloaded")) {
					node = "world.togglespawnloaded";
				} else if (args[0].equalsIgnoreCase("keepspawnloaded")) {
					node = "world.togglespawnloaded";
				} else if (args[0].equalsIgnoreCase("difficulty")) {
					node = "world.difficulty";
				} else if (args[0].equalsIgnoreCase("difficult")) {
					node = "world.difficulty";
				} else if (args[0].equalsIgnoreCase("diff")) {
					node = "world.difficulty";
				} else if (args[0].equalsIgnoreCase("op")) {
					node = "world.op";
				} else if (args[0].equalsIgnoreCase("deop")) {
					node = "world.deop";
				} else if (args[0].equalsIgnoreCase("setsave")) {
					node = "world.setsaving";
				} else if (args[0].equalsIgnoreCase("setsaving")) {
					node = "world.setsaving";
				} else if (args[0].equalsIgnoreCase("saving")) {
					node = "world.setsaving";
				} else if (args[0].equalsIgnoreCase("autosave")) {
					node = "world.setsaving";
				}
			}
			if (node == null) {
				//show default usage for /world
				boolean hac = false; //has available commands
				if (showUsage(sender, "world.repair")) hac = true;
				if (showUsage(sender, "world.delete")) hac = true;
				if (showUsage(sender, "world.rename")) hac = true;
				if (showUsage(sender, "world.copy")) hac = true;
				if (showUsage(sender, "world.save")) hac = true;
				if (showUsage(sender, "world.load")) hac = true;
				if (showUsage(sender, "world.unload")) hac = true;
				if (showUsage(sender, "world.create")) hac = true;
				if (showUsage(sender, "world.listgenerators")) hac = true;
				if (showUsage(sender, "world.weather")) hac = true;		
				if (showUsage(sender, "world.time")) hac = true;			
				if (showUsage(sender, "world.spawn")) hac = true;
				if (showUsage(sender, "world.setspawn")) hac = true;
				if (showUsage(sender, "world.list")) hac = true;
				if (showUsage(sender, "world.info")) hac = true;
				if (showUsage(sender, "world.portals")) hac = true;
				if (showUsage(sender, "world.gamemode")) hac = true;
				if (showUsage(sender, "world.togglepvp")) hac = true;
				if (showUsage(sender, "world.op")) hac = true;
				if (showUsage(sender, "world.deop")) hac = true;
				if (showUsage(sender, "world.allowspawn")) hac = true;
				if (showUsage(sender, "world.denyspawn")) hac = true;		
				if (hac) {
					if (args.length >= 1) message(sender, ChatColor.RED + "Unknown command: " + args[0]);
				} else {
					Localization.message(sender, "command.nopermission");
				}
			}
		} else if (cmdLabel.equalsIgnoreCase("tpp")) {
			node = "tpp";
		}
		if (node != null) {
			if (Permission.has(sender, node)) {
				//nodes made, commands can now be executed (finally!)
				if (node == "world.list") {
					//==========================================
					//===============LIST COMMAND===============
					//==========================================
					if (sender instanceof Player) {
						//perform some nice layout coloring
						sender.sendMessage("");
						sender.sendMessage(ChatColor.GREEN + "[Loaded/Online] " + ChatColor.RED + "[Unloaded/Offline] " + ChatColor.DARK_RED + "[Broken/Dead]");
						sender.sendMessage(ChatColor.YELLOW + "Available worlds: ");
						String msgpart = "";
						for (String world : WorldManager.getWorlds()) {
							//prepare it
							if (WorldManager.isLoaded(world)) {
								world = ChatColor.GREEN + world;
							} else if (WorldManager.getData(world) == null) {
								world = ChatColor.DARK_RED + world;
							} else {
								world = ChatColor.RED + world;
							}
							//display it
							if (msgpart.length() + world.length() < 70) {
								if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
								msgpart += world;
							} else {
								sender.sendMessage(msgpart);
								msgpart = world;
							}
						}
						//possibly forgot one?
						if (msgpart != "") sender.sendMessage(msgpart);
					} else {
						//plain world per line
						sender.sendMessage("Available worlds:");
						for (String world : WorldManager.getWorlds()) {
							String status = "[Unloaded]";
							if (WorldManager.isLoaded(world)) {
								status = "[Loaded]";
							} else if (WorldManager.getData(world) == null) {
								status = "[Broken]";
							}
							sender.sendMessage("    " + world + " " + status);
						}
					}
				} else if (node == "world.portals") {
					//==========================================
					//===============PORTALS COMMAND============
					//==========================================
					String[] portals;
					if (args.length == 2) {
						World w = WorldManager.getWorld(WorldManager.matchWorld(args[1]));
						if (w != null) {
							portals = Portal.getPortals(w);
						} else {
							message(sender, ChatColor.RED + "World not found!");
							return true;
						}
					} else {
						portals = Portal.getPortals();
					}
					listPortals(sender, portals);
				} else if (node == "world.listgenerators") {
					//==================================================
					//===============LIST GENERATORS COMMAND============
					//==================================================
					message(sender, ChatColor.YELLOW + "Available chunk generators:");
					String msgpart = "";
					for (String plugin : WorldManager.getGeneratorPlugins()) {
						plugin = ChatColor.GREEN + plugin;
						//display it
						if (msgpart.length() + plugin.length() < 70) {
							if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
							msgpart += plugin;
						} else {
							sender.sendMessage(msgpart);
							msgpart = plugin;
						}
					}
					message(sender, msgpart);
				} else if (node == "world.setportal") {
					//==============================================
					//===============SET DEFAULT PORTAL COMMAND=====
					//==============================================
					if (args.length > 1) {
						String dest = args[1];
						String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
						if (worldname != null) {
							if (dest.equals("")) {
								WorldConfig.get(worldname).defaultPortal = null;
								message(sender, ChatColor.GREEN + "Default destination of world '" + worldname + "' cleared!");
							} else if (Portal.getPortalLocation(dest, null) != null) {
								WorldConfig.get(worldname).defaultPortal = dest;
								message(sender, ChatColor.GREEN + "Default destination of world '" + worldname + "' set to portal: '" + dest + "'!");
							} else if ((dest = WorldManager.matchWorld(dest)) != null) {
								WorldConfig.get(worldname).defaultPortal = dest;
								message(sender, ChatColor.GREEN + "Default destination of world '" + worldname + "' set to world: '" + dest + "'!");
								if (!WorldManager.isLoaded(dest)) {
									message(sender, ChatColor.YELLOW + "Note that this world is not loaded, so nothing happens yet!");
								}
							} else {
								message(sender, ChatColor.RED + "Destination is not a world or portal!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.info") {
					//==========================================
					//===============INFO COMMAND===============
					//==========================================
					String worldname = null;
					if (args.length == 2) {
						worldname = WorldManager.matchWorld(args[1]);
					} else if (sender instanceof Player) {
						worldname = ((Player) sender).getWorld().getName();
					} else {
						for (World w : getServer().getWorlds()) {
							worldname = w.getName();
							break;
						}
					}
					if (worldname != null) {
						WorldInfo info = WorldManager.getInfo(worldname);
						if (info == null) {
							message(sender, ChatColor.RED + "' " + worldname + "' is broken, no information can be shown!");
						} else {
							WorldConfig wc = WorldConfig.get(worldname);
							message(sender, ChatColor.YELLOW + "Information about the world: " + worldname);
							message(sender, ChatColor.WHITE + "Internal name: " + ChatColor.YELLOW + info.name);
							message(sender, ChatColor.WHITE + "Environment: " + ChatColor.YELLOW + wc.environment.name().toLowerCase());
							if (wc.chunkGeneratorName == null) {
								message(sender, ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + "Default");
							} else {
								message(sender, ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + wc.chunkGeneratorName);
							}
							message(sender, ChatColor.WHITE + "Auto-saving: " + ChatColor.YELLOW + wc.autosave);
							message(sender, ChatColor.WHITE + "Keep spawn loaded: " + ChatColor.YELLOW + wc.keepSpawnInMemory);
							message(sender, ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + info.seed);
							if (info.size > 1000000) {
								message(sender, ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000000) + " Megabytes");
							} else if (info.size > 1000) {
								message(sender, ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000) + " Kilobytes");
							} else {
								message(sender, ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size) + " Bytes");
							}
							//PvP
							if (wc.pvp) { 
								message(sender, ChatColor.WHITE + "PvP: " + ChatColor.GREEN + "Enabled");
							} else {
								message(sender, ChatColor.WHITE + "PvP: " + ChatColor.YELLOW + "Disabled");
							}
							//Difficulty
							message(sender, ChatColor.WHITE + "Difficulty: " + ChatColor.YELLOW + wc.difficulty.toString().toLowerCase());
							//Game mode
							GameMode mode = wc.gameMode;
							if (mode == null) {
								message(sender, ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + "Not set");
							} else {
								message(sender, ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + mode.name().toLowerCase());
							}
							//Time
							String timestr = wc.timeControl.getTime(info.time);
							message(sender, ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timestr);
							//Weather
							if (wc.holdWeather) {
								if (info.raining) {
									if (info.thundering) {
										message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless storm with lightning");
									} else {
										message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless rain and snow");
									}
								} else {
									message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "No bad weather expected");
								}
							} else {
								if (info.raining) {
									if (info.thundering) {
										message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Stormy with lightning");
									} else {
										message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Rain and snow");
									}
								} else {
									message(sender, ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "The sky is clear");
								}
							}							
							World w = Bukkit.getServer().getWorld(worldname);
							if (w != null) {
								int playercount = w.getPlayers().size();
								if (playercount > 0) {
									String msg = ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Loaded" + ChatColor.WHITE + " with ";
									msg += playercount + ((playercount <= 1) ? " player" : " players");
									message(sender, msg);
								} else {
									message(sender, ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Stand-by");
								}
							} else {
								message(sender, ChatColor.WHITE + "Status: " + ChatColor.RED + "Unloaded");
							}
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.difficulty") {
					//=========================================
					//===============DIFFICULTY COMMAND========
					//=========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
					if (worldname != null) {
						WorldConfig wc = WorldConfig.get(worldname);
					    if (args.length == 1) {
					    	String diff = wc.difficulty.toString().toLowerCase();
					    	message(sender, ChatColor.YELLOW + "Difficulty of world '" + worldname + "' is set at " + ChatColor.WHITE + diff);
					    } else {
					    	Difficulty diff = Util.parseDifficulty(args[1], Difficulty.NORMAL);
					    	if (diff != null) {
								wc.difficulty = diff;
								wc.updateDifficulty(wc.getWorld());
								message(sender, ChatColor.YELLOW + "Difficulty of world '" + worldname + "' set to " + ChatColor.WHITE + diff.toString().toLowerCase());
					    	} else {
					    		message(sender, ChatColor.RED + "Difficulty '" + args[1] + "' has not been recognized!");
					    	}
					    }
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.gamemode") {
					//=========================================
					//===============GAME MODE COMMAND=========
					//=========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
					if (worldname != null) {
						if (args.length == 1) {
							//display
							GameMode mode = WorldConfig.get(worldname).gameMode;
							String msg = ChatColor.YELLOW + "Current game mode of world '" + worldname + "': ";
							if (mode == null) {
								message(sender, msg + ChatColor.YELLOW + "Not set");
							} else {
								message(sender, msg + ChatColor.YELLOW + mode.name().toLowerCase());
							}
						} else {
							//Parse the gamemode
							GameMode mode = Util.parseGameMode(args[1], null);
							WorldConfig wc = WorldConfig.get(worldname);
							wc.setGameMode(mode);
							if (mode == null) {
								message(sender, ChatColor.YELLOW + "Game mode of World '" + worldname + "' cleared!");
							} else {
								message(sender, ChatColor.YELLOW + "Game mode of World '" + worldname + "' set to " + mode.name().toLowerCase() + "!");
							}
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.togglepvp") {
					//==========================================
					//===============TOGGLE PVP COMMAND=========
					//==========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 2);
					if (worldname != null) {
						WorldConfig wc = WorldConfig.get(worldname);
						wc.pvp = !wc.pvp;
						wc.updatePVP(wc.getWorld());
						if (wc.pvp) {
							message(sender, ChatColor.GREEN + "PvP on World: '" + worldname + "' enabled!");
						} else {
							message(sender, ChatColor.YELLOW + "PvP on World: '" + worldname + "' disabled!");
						}
						if (!WorldManager.isLoaded(worldname)) {
							message(sender, ChatColor.YELLOW + "Please note that this world is not loaded!");
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.op" || node == "world.deop") {
					//=====================================
					//===============(DE)OP COMMAND========
					//=====================================
					if (args.length >= 2) {
						boolean all = args.length == 3 && (args[2].startsWith("*") || args[2].equalsIgnoreCase("all"));
						String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
						if (worldname != null) {
							WorldConfig wc = WorldConfig.get(worldname);
							String playername = args[1];
							if (playername.startsWith("*")) {
								wc.OPlist.clear();
								if (node == "world.op") {
									wc.OPlist.add("*");
									message(sender, ChatColor.YELLOW + "Everyone on world '" + worldname + "' is an operator now!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped everyone on world '" + worldname + "'!");
									}
								} else {
									message(sender, ChatColor.YELLOW + "Operators on world '" + worldname + "' have been cleared!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped everyone on world '" + worldname + "'!");
									}
								}
							} else {
								if (node == "world.op") {
									wc.OPlist.add(playername.toLowerCase());
									message(sender, ChatColor.WHITE + playername + ChatColor.YELLOW + " is now an operator on world '" + worldname + "'!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped '" + playername + "' on world '" + worldname + "'!");
									}
								} else {
									wc.OPlist.remove(playername.toLowerCase());
									message(sender, ChatColor.WHITE + playername + ChatColor.YELLOW + " is no longer an operator on world '" + worldname + "'!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped '" + playername + "' on world '" + worldname + "'!");
									}
								}
							}
							wc.updateOP(wc.getWorld());
						} else if (all) {
							String playername = args[1];
							if (playername.startsWith("*")) {
								for (WorldConfig wc : WorldConfig.all()) {
									wc.OPlist.clear();
									if (node == "world.op") {
										wc.OPlist.add("*");
									}
									wc.updateOP(wc.getWorld());
								}
								if (node == "world.op") {
									message(sender, ChatColor.YELLOW + "Everyone is now an operator on all worlds!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped everyone on all worlds!");
									}
								} else {
									message(sender, ChatColor.YELLOW + "Everyone is no longer an operator on any world!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped everyone on all worlds!");
									}
								}
							} else {
								for (WorldConfig wc : WorldConfig.all()) {
									if (node == "world.op") {
										wc.OPlist.add(playername.toLowerCase());
									} else {
										wc.OPlist.remove(playername.toLowerCase());
									}
									wc.updateOP(wc.getWorld());
								}
								if (node == "world.op") {
									message(sender, ChatColor.WHITE + playername + ChatColor.YELLOW + " is now an operator on all worlds!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped '" + playername + "' on all worlds!");
									}
								} else {
									message(sender, ChatColor.WHITE + playername + ChatColor.YELLOW + " is no longer an operator on any world!");
									if (sender instanceof Player) {
										MyWorlds.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped '" + playername + "' on all worlds!");
									}
								}
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						//list Operators
						WorldConfig wc = WorldConfig.get(WorldManager.getWorldName(sender, args, false));
						message(sender, ChatColor.YELLOW + "Operators of world '" + wc.worldname + "':");
						if (sender instanceof Player) {
							//perform some nice layout coloring
							String msgpart = "";
							for (String player : wc.OPlist) {
								//prepare it
								if (Bukkit.getServer().getPlayer(player) != null) {
									player = ChatColor.GREEN + player;
								} else {
									player = ChatColor.RED + player;
								}
								//display it
								if (msgpart.length() + player.length() < 70) {
									if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
									msgpart += player;
								} else {
									sender.sendMessage(msgpart);
									msgpart = player;
								}
							}
							//possibly forgot one?
							if (msgpart != "") sender.sendMessage(msgpart);
						} else {
							//plain world per line
							for (String player : wc.OPlist) {
								String status = "[Offline]";
								if (Bukkit.getServer().getPlayer(player) != null) {
									status = "[Online]";
								}
								sender.sendMessage("    " + player + " " + status);
							}
						}
					}
				} else if (node == "world.togglespawnloaded") {
					//==========================================
					//===============TOGGLE SPAWN LOADED========
					//==========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 2);
					if (worldname != null) {
						WorldConfig wc = WorldConfig.get(worldname);
						wc.keepSpawnInMemory = !wc.keepSpawnInMemory;
						wc.updateKeepSpawnInMemory(wc.getWorld());
						if (wc.keepSpawnInMemory) {
							message(sender, ChatColor.GREEN + "The spawn area on World: '" + worldname + "' is now kept loaded!");
						} else {
							message(sender, ChatColor.YELLOW + "The spawn area on World: '" + worldname + "' is no longer kept loaded!");
						}
						if (!WorldManager.isLoaded(worldname)) {
							message(sender, ChatColor.YELLOW + "These settings will be used as soon this world is loaded.");
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.allowspawn" || node == "world.denyspawn") {
					//==========================================
					//===============TOGGLE ANIMAL SPAWNING=====
					//==========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
					if (worldname != null) {
						if (args.length >= 2) {
							SpawnControl sc = WorldConfig.get(worldname).spawnControl;
							//Get the type to set
							String type = null;
							if (args[1].equalsIgnoreCase("animal")) {
								type = "animal";
							} else if (args[1].equalsIgnoreCase("animals")) {
								type = "animal";
							} else if (args[1].equalsIgnoreCase("monster")) {
								type = "monster";
							} else if (args[1].equalsIgnoreCase("monsters")) {
								type = "monster";
							} else if (args[1].equalsIgnoreCase("mob")) {
								type = "mob";
							} else if (args[1].equalsIgnoreCase("mobs")) {
								type = "mob";	
							} else if (args[1].equalsIgnoreCase("creature")) {
								type = "mob";
							} else if (args[1].equalsIgnoreCase("creatures")) {
								type = "mob";
							} else if (args[1].equalsIgnoreCase("all")) {
								type = "mob";
							} else {
								type = args[1].toUpperCase();
								CreatureType ctype = null;
								try {
									ctype = CreatureType.valueOf(type);
								} catch (Exception e) {}
								if (ctype == null && type.endsWith("S")) {
									try {
										ctype = CreatureType.valueOf(type.substring(0, type.length() - 2));
									} catch (Exception e) {}
								}
								if (ctype != null) {
									type = ctype.name().toLowerCase();
								} else {
									type = null;
								}
							}
							//Set it, of course
							if (type != null) {
								if (node == "world.allowspawn") {
									if (type.equals("animal")) {
										sc.setAnimals(false);
									} else if (type.equals("monster")) {
										sc.setMonsters(false);
									} else if (type.equals("mob")) {
										sc.deniedCreatures.clear();
									} else {
										sc.deniedCreatures.remove(CreatureType.valueOf(type.toUpperCase()));
									}
									if (WorldManager.isLoaded(worldname)) {
										message(sender, ChatColor.GREEN + type + "s are now allowed to spawn on world: '" + worldname + "'!");
									} else {
										message(sender, ChatColor.GREEN + type + "s are allowed to spawn on world: '" + worldname + "' once it is loaded!");
									}
								} else {
									if (type.equals("animal")) {
										sc.setAnimals(true);
									} else if (type.equals("monster")) {
										sc.setMonsters(true);
									} else if (type.equals("mob")) {
										sc.setAnimals(true);
										sc.setMonsters(true);
									} else {
										sc.deniedCreatures.add(CreatureType.valueOf(type.toUpperCase()));
									}
									//Capitalize
									type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
									if (WorldManager.isLoaded(worldname)) {
										message(sender, ChatColor.YELLOW + type + "s can no longer spawn on world: '" + worldname + "'!");
									} else {
										message(sender, ChatColor.YELLOW + type + "s can no longer spawn on world: '" + worldname + "' once it is loaded!");
									}
								}
								World w = WorldManager.getWorld(worldname);
								if (w != null) {
									for (Entity e : w.getEntities()) {
										if (sc.isDenied(e)) {
											e.remove();
										}
									}
								}
							} else {
								message(sender, ChatColor.RED + "Invalid creature type!");
							}
						}
						//Mobs
						SpawnControl sc = WorldConfig.get(worldname).spawnControl;
						if (sc.deniedCreatures.size() == 0) {
							message(sender, ChatColor.WHITE + "All mobs are allowed to spawn right now.");
						} else {
							message(sender, ChatColor.WHITE + "The following mobs are denied from spawning:");
							String message = ChatColor.YELLOW.toString();
							boolean first = true;
							for (CreatureType type : sc.deniedCreatures) {
								if (first) {
									message += type.getName();
									first = false;
								} else {
									message += ", " + type.getName();
								}
							}
							message(sender, message);
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}					
				} else if (node == "world.weather") {
					//=======================================
					//===============WEATHER COMMAND=========
					//=======================================
					if (args.length > 1) {
						boolean setStorm = false;
						boolean setSun = false;
						boolean setHold = false;
						boolean useWorld = true;
						boolean setThunder = false;
						for (String command : args) {
							if (command.equalsIgnoreCase("hold")) {
								setHold = true;
							} else if (command.equalsIgnoreCase("always")) {
								setHold = true;
							} else if (command.equalsIgnoreCase("endless")) { 
								setHold = true;
							} else if (command.equalsIgnoreCase("sun")) {
								setSun = true;
							} else if (command.equalsIgnoreCase("sunny")) { 
								setSun = true;
							} else if (command.equalsIgnoreCase("endless")) {
								setSun = true;
							} else if (command.equalsIgnoreCase("storm")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("stormy")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("rain")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("rainy")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("snow")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("snowy")) {
								setStorm = true;
							} else if (command.equalsIgnoreCase("thunder")) {
								setThunder = true;
							} else if (command.equalsIgnoreCase("lightning")) {
								setThunder = true;
							} else if (command.equalsIgnoreCase("heavy")) {
								setThunder = true;
							} else if (command.equalsIgnoreCase("big")) {
								setThunder = true;
							} else if (command.equalsIgnoreCase("huge")) {
								setThunder = true;
							} else {
								continue;
							}
							//Used the last argument as command?
							if (command == args[args.length - 1]) useWorld = false;
						}
						String worldname = WorldManager.getWorldName(sender, args, useWorld);
						if (worldname != null) {
							WorldConfig wc = WorldConfig.get(worldname);
							World w = wc.getWorld();
							if (w != null) {
								boolean holdchange = wc.holdWeather != setHold;
								wc.holdWeather = setHold;
								if (setStorm && ((!w.hasStorm()) || (setThunder && !w.isThundering()) || holdchange)) {
									MWWeatherListener.setWeather(w, true);
									if (setThunder) {
										 w.setThundering(true);
									}
									String a = "";
									if (setThunder) a = "rumbling ";
									if (setHold) {
										if (setThunder) w.setThunderDuration(Integer.MAX_VALUE);
										message(sender, ChatColor.GREEN + "You started an endless " +  a + "storm on world: '" + worldname + "'!");
									} else {
										message(sender, ChatColor.GREEN + "You started a " +  a + "storm on world: '" + worldname + "'!");
									}
								} else if (setSun && (w.hasStorm() || holdchange)) {
									MWWeatherListener.setWeather(w, false);
									if (setHold) {
										message(sender, ChatColor.GREEN + "You stopped the formation of storms on world: '" + worldname + "'!");
									} else {
										message(sender, ChatColor.GREEN + "You stopped a storm on world: '" + worldname + "'!");
									}
								} else if (setHold) {
									message(sender, ChatColor.GREEN + "Weather changes on world: '" + worldname + "' are now being prevented!");
								} else {
									message(sender, ChatColor.YELLOW + "Unknown syntax or the settings were already applied!");
								}
							} else {
								message(sender, ChatColor.YELLOW + "World: '" + worldname + "' is not loaded, only hold settings are applied!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.setspawn") {
					//====================================
					//===============SET SPAWN COMMAND====
					//====================================
					if (sender instanceof Player) {
						Player p = (Player) sender;
						Position pos = new Position(p.getLocation());
						String worldname = WorldManager.getWorldName(sender, args, args.length == 2);
						if (worldname != null) {
							WorldManager.setSpawn(worldname, pos);
							if (worldname.equalsIgnoreCase(p.getWorld().getName())) {
								p.getWorld().setSpawnLocation(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
							}
							sender.sendMessage(ChatColor.GREEN + "Spawn location of world '" + worldname + "' set to your position!");
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						sender.sendMessage("This command is only for players!");
					}
				} else if (node == "world.time") {
					//====================================
					//===============TIME COMMAND=========
					//====================================
					boolean lock = false;
					boolean useWorld = false;
					long time = -1;
					boolean firstcheck = true;
					for (String command : args) {
						if (firstcheck) {
							firstcheck = false;
							continue;
						}
						//Time reading
						if (time == -1) {
							time = TimeControl.getTime(command);
						}
						if (command.equalsIgnoreCase("lock")) {
							lock = true;
						} else if (command.equalsIgnoreCase("locked")) {
							lock = true;
						} else if (command.equalsIgnoreCase("always")) {
							lock = true;
						} else if (command.equalsIgnoreCase("endless")) {
							lock = true;
						} else if (command.equalsIgnoreCase("l")) {
							lock = true;
						} else if (command.equalsIgnoreCase("-l")) {
							lock = true;	
						} else if (command.equalsIgnoreCase("stop")) {
							lock = true;
						} else if (command.equalsIgnoreCase("freeze")) {
							lock = true;
						} else {
							time = TimeControl.getTime(command);
							if (time == -1) {
								//Used the last argument as command?
								if (command == args[args.length - 1]) useWorld = true;
							}
						}
					}
					String worldname = WorldManager.getWorldName(sender, args, useWorld);
					if (worldname != null) {
						if (time == -1) {
							World w = WorldManager.getWorld(worldname);
							if (w == null) {
								WorldInfo i = WorldManager.getInfo(worldname);
								if (i == null) {
									time = 0;
								} else {
									time = i.time;
								}
							} else {
								time = w.getFullTime();
							}
						}
						if (args.length == 1) {
							message(sender, ChatColor.YELLOW + "The current time of world '" + 
									worldname + "' is " + TimeControl.getTimeString(time));
						} else {
							TimeControl tc = WorldConfig.get(worldname).timeControl;
							if (lock) {
								tc.lockTime(time);
								if (!WorldManager.isLoaded(worldname)) {
									tc.setLocking(false);
									message(sender, ChatColor.YELLOW + "World '" + worldname + 
											"' is not loaded, time will be locked to " + 
											TimeControl.getTimeString(time) + " as soon it is loaded!");
								} else {
									tc.setLocking(true);
									message(sender, ChatColor.GREEN + "Time of world '" + worldname + "' locked to " + 
									        TimeControl.getTimeString(time) + "!");
								}
							} else {
								World w = WorldManager.getWorld(worldname);
								if (w != null) {
									if (tc.isLocked()) {
										tc.unlockTime();
										WorldManager.setTime(w, time);
										message(sender, ChatColor.GREEN + "Time of world '" + worldname + "' unlocked and set to " + 
										        TimeControl.getTimeString(time) + "!");
									} else {
										WorldManager.setTime(w, time);
										message(sender, ChatColor.GREEN + "Time of world '" + worldname + "' set to " + 
										        TimeControl.getTimeString(time) + "!");
									}
								} else {
									message(sender, ChatColor.RED + "World '" + worldname + "' is not loaded, time is not changed!");
								}
							}
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.load") {
					//==========================================
					//===============LOAD COMMAND===============
					//==========================================
					if (args.length == 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						if (worldname != null) {
							if (WorldManager.isLoaded(worldname)) {
								message(sender, ChatColor.YELLOW + "World '" + worldname + "' is already loaded!");
							} else {
								notifyConsole(sender, "Issued a load command for world: " + worldname);
								message(sender, ChatColor.YELLOW + "Loading world: '" + worldname + "'...");
								if (WorldManager.createWorld(worldname, 0) != null) {
									message(sender, ChatColor.GREEN + "World loaded!");
								} else {
									message(sender, ChatColor.RED + "Failed to load world, it is probably broken!");
								}
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.unload") {
					//============================================
					//===============UNLOAD COMMAND===============
					//============================================
					if (args.length == 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						if (worldname != null) {
							World w = Bukkit.getServer().getWorld(worldname);
							if (w != null) {
								notifyConsole(sender, "Issued an unload command for world: " + worldname);
								if (WorldManager.unload(w)) {
									message(sender, ChatColor.GREEN + "World '" + worldname + "' has been unloaded!");
								} else {
									message(sender, ChatColor.RED + "Failed to unload the world (main world or online players?)");
								}
							} else {
								message(sender, ChatColor.YELLOW + "World '" + worldname + "' is already unloaded!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.evacuate") {
					//==============================================
					//===============EVACUATE COMMAND===============
					//==============================================
					if (args.length >= 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						if (worldname != null) {
							World w = Bukkit.getServer().getWorld(worldname);
							if (w != null) {
								String message = "";
								for (int i = 2;i < args.length;i++) {
									message += args[i] + " ";
								}
								if (message == "") {
									message = "Your world has been closed down!";
								} else {
									message = message.trim();
								}
								notifyConsole(sender, "Evacuated world: " + worldname +  " ('" + message + "')");
								WorldManager.evacuate(w, message);
							} else {
								message(sender, ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.create") {
					//============================================
					//===============CREATE COMMAND===============
					//============================================
					if (args.length >= 2) {
						String worldname = args[1];
						String gen = null;
						if (worldname.contains(":")) {
							String[] parts = worldname.split(":");
							if (parts.length == 2) {
								worldname = parts[0];
								gen = parts[1];
							} else {
								worldname = parts[0];
								gen = parts[1] + ":" + parts[2];
							}
						}
						if (!WorldManager.worldExists(worldname)) {
							String seed = "";
							for (int i = 2;i < args.length;i++) {
								if (seed != "") seed += " ";
								seed += args[i];
							}
							long seedval = WorldManager.getRandomSeed(seed);
							notifyConsole(sender, "Issued a world creation command for world: " + worldname);
					        WorldConfig.remove(worldname);
							if (gen == null) {
								message(sender, ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
							} else {
								String fixgen = WorldManager.fixGeneratorName(gen);
								if (fixgen == null) {
									message(sender, ChatColor.RED + "Failed to create world because the generator '" + gen + "' is missing!");
								} else {
									WorldManager.setGenerator(worldname, fixgen);
									message(sender, ChatColor.YELLOW + "Creating world '" + worldname + "' using generator '" + fixgen + "' (this can take a while) ...");
								}
							}
					        message(sender, ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + seedval);
					        MWWorldListener.ignoreWorld(worldname);
					        World world = WorldManager.createWorld(worldname, seedval);
							if (world != null) {
								//load chunks
								final int keepdimension = 14;
								final int total = 4 * keepdimension * keepdimension;
								int current = 0;
								int spawnx = world.getSpawnLocation().getBlockX() >> 4;
							    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
								for (int x = -keepdimension; x < keepdimension; x++) {
									boolean first = true;
									for (int z = -keepdimension; z < keepdimension; z++) {
										int cx = spawnx + x;
										int cz = spawnz + z;
										Task t = null;
										if (first || (current + 2) == total) {
											int per = 100;
											if (first) per = 100 * current / total;
											t = new Task(sender, per) {
												public void run() {
													CommandSender sender = (CommandSender) getArg(0);
													int percent = getIntArg(1);
												    message(sender, ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
												    MyWorlds.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
												}
											};
											first = false;
										}
										if (++current == total) {
											t = new Task(sender, world) {
												public void run() {
													CommandSender sender = (CommandSender) getArg(0);
													World world = (World) getArg(1);
													world.setKeepSpawnInMemory(true);
												    message(sender, ChatColor.GREEN + "World '" + world.getName() + "' has been loaded and is ready for use!");
												    MyWorlds.log(Level.INFO, "World '"+ world.getName() + "' loaded.");
												}
											};
										}
										LoadChunksTask.add(world, cx, cz, t);
									}
								}
							} else {
								message(sender, ChatColor.RED + "World creation failed!");
							}
						} else {
							message(sender, ChatColor.RED + "World already exists!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.repair") {
					//============================================
					//===============REPAIR COMMAND===============
					//============================================
					if (args.length >= 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						//get seed
						String seed = "";
						for (int i = 2;i < args.length;i++) {
							if (seed != "") seed += " ";
							seed += args[i];
						}
						if (WorldManager.getDataFolder(worldname).exists()) {
							if (!WorldManager.isLoaded(worldname)) {
								AsyncHandler.repair(sender, worldname, WorldManager.getRandomSeed(seed));
							} else {
								message(sender, ChatColor.YELLOW + "Can't repair a loaded world!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.spawn") {
					//===========================================
					//===============SPAWN COMMAND===============
					//===========================================
					if (sender instanceof Player) {
						String worldname = WorldManager.getWorldName(sender, args, args.length >= 2);
						if (worldname != null) {
							World world = WorldManager.getWorld(worldname);
							if (world != null) {
								Location loc = world.getSpawnLocation();
								for (Position pos : WorldManager.getSpawnPoints(world)) {
									loc = pos.toLocation();
									break;
								}
								if (Permission.handleTeleport((Player) sender, loc)) {
									//Success
								}
							} else {
								message(sender, ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						sender.sendMessage("A player is expected!");
					}
				} else if (node == "world.setsaving") {
					//==========================================
					//===========AUTO SAVE TOGGLE COMMAND=======
					//==========================================
					boolean display = true;
					if (args.length > 1) {
						display = !Util.isBool(args[1]);
					}
					String worldname = WorldManager.getWorldName(sender, args, args.length >= 3 || (args.length == 2 && display));
					if (worldname != null) {
						WorldConfig wc = WorldConfig.get(worldname);
						if (display) {
							if (wc.autosave) {
								message(sender, ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is enabled!");
							} else {
								message(sender, ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is disabled!");
							}
						} else {
							boolean set = Util.getBool(args[1]);
							wc.autosave = set;
							wc.updateAutoSave(wc.getWorld());
							if (set) {
								message(sender, ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is now enabled!");
							} else {
								message(sender, ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is now disabled!");
							}
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.save") {
					//==========================================
					//===============SAVE COMMAND===============
					//==========================================
					String worldname;
					if (args.length >= 2) {
						worldname = WorldManager.matchWorld(args[1]);
						if (worldname == null) worldname = args[1];
					} else if (sender instanceof Player) {
						worldname = ((Player) sender).getWorld().getName();
					} else {
						worldname = "*";
					}
					World w = WorldManager.getWorld(worldname);
					if (w != null) {
						message(sender, ChatColor.YELLOW + "Saving world '" + worldname + "'...");
						w.save();
						message(sender, ChatColor.GREEN + "World saved!");
					} else if (worldname.equalsIgnoreCase("all") || worldname.equals("*")) {
						message(sender, ChatColor.YELLOW + "Forcing a global world save...");	
						for (World ww : getServer().getWorlds()) {
							ww.save();
						}
						message(sender, ChatColor.GREEN + "All worlds have been saved!");			
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (node == "world.delete") {
					//============================================
					//===============DELETE COMMAND===============
					//============================================
					if (args.length == 2) {
						String worldname = args[1];
						if (WorldManager.worldExists(worldname)) {
							if (!WorldManager.isLoaded(worldname)) {
								notifyConsole(sender, "Issued a world deletion command for world: " + worldname);
								WorldConfig.remove(worldname);
								AsyncHandler.delete(sender, worldname);
							} else {
								message(sender, ChatColor.RED + "World is loaded, please unload the world first!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "world.copy") {
					//============================================
					//===============COPY COMMAND=================
					//============================================
					if (args.length == 3) {
						String worldname = args[1];
						if (WorldManager.worldExists(worldname)) {
							String newname = args[2];
							if (!WorldManager.worldExists(newname)) {
								notifyConsole(sender, "Issued a world copy command for world: " + worldname + " to '" + newname + "'!");
								message(sender, ChatColor.YELLOW + "Copying world '" + worldname + "' to '" + newname + "'...");
								AsyncHandler.copy(sender, worldname, newname);
							} else {
								message(sender, ChatColor.RED + "Can not copy to an existing world!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
					}
				} else if (node == "tpp") {
					//================================================
					//===============TELEPORT PORTAL COMMAND==========
					//================================================
					if (args.length >= 1) {
						Player[] targets = null;
						String dest = null;
						if (args.length > 1) {
							HashSet<Player> found = new HashSet<Player>();
							for (int i = 0; i < args.length - 1; i++) {
								Player player = Bukkit.getServer().getPlayer(args[i]);
								if (player == null) {
									message(sender, ChatColor.RED + "Player '" + args[i] + "' has not been found!");
								} else {
									found.add(player);
								}
							}
							targets = found.toArray(new Player[0]);
							dest = args[args.length - 1];
						} else if (sender instanceof Player) {
							targets = new Player[] {(Player) sender};
							dest = args[0];
						} else {
							sender.sendMessage("This command is only for players!");
							return true;
						}
						if (targets.length > 0) {
							//Get prefered world
							World world = targets[0].getWorld();
							if (sender instanceof Player) world = ((Player) sender).getWorld();
							//Get portal
							Location tele = Portal.getPortalLocation(dest, world.getName(), true);
							if (tele != null) {
								//Perform portal teleports
								int succcount = 0;
								for (Player target : targets) {
									if (Permission.canTeleportPortal(target, dest)) {
										if (Permission.handleTeleport(target, dest, tele)) {
											//Success
											succcount++;
										}
									} else {
										Localization.message(target, "portal.noaccess");
									}
								}
								if (targets.length > 1 || targets[0] != sender) {
									message(sender, ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
											" Players have been teleported to portal '" + dest + "'!");
								}
							} else {
								//Match world
								String worldname = WorldManager.matchWorld(dest);
								if (worldname != null) {
									World w = WorldManager.getWorld(worldname);
									if (w != null) {
										//Perform world teleports
										int succcount = 0;
										for (Player target : targets) {
											if (Permission.canTeleportWorld(target, w.getName())) {
												if (Permission.handleTeleport(target, w.getSpawnLocation())) {
													//Success
													succcount++;
												}
											} else {
												Localization.message(target, "world.noaccess");
											}
										}
										if (targets.length > 1 || targets[0] != sender) {
											message(sender, ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
													" Players have been teleported to world '" + w.getName() + "'!");
										}
									} else {
										message(sender, ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
									}
								} else {
									Localization.message(sender, "portal.notfound");
									listPortals(sender, Portal.getPortals());
								}
							}
						}
					} else {
						showInv(sender, node);
					}	
				}
			} else {
				Localization.message(sender, "command.nopermission");
			}	
		}
		return true;
	}

}
