package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;


public class MyWorlds extends JavaPlugin {
	public static boolean usePermissions = false;
	public static int teleportInterval = 2000;
	public static boolean useWaterTeleport = true;
	public static int timeLockInterval = 10;
	
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
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Highest, this);   
        pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Highest, this); 
        pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Highest, this);  
        pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WORLD_UNLOAD, worldListener, Priority.Monitor, this);  
        pm.registerEvent(Event.Type.WEATHER_CHANGE, weatherListener, Priority.Highest, this);  
        
        Configuration config = getConfiguration();
        usePermissions = config.getBoolean("usePermissions", usePermissions);
        teleportInterval = config.getInt("teleportInterval", teleportInterval);
        useWaterTeleport = config.getBoolean("useWaterTeleport", useWaterTeleport);
        timeLockInterval = config.getInt("timeLockInterval", timeLockInterval);
        config.setProperty("usePermissions", usePermissions);
        config.setProperty("teleportInterval", teleportInterval);
        config.setProperty("useWaterTeleport", useWaterTeleport);
        config.setProperty("timeLockInterval", timeLockInterval);
        config.save();
        //Permissions
		Permission.init(this);
				
		//Loaded worlds
		SafeReader reader = new SafeReader(root() + "LoadedWorlds.txt");
		String textline = null;
		while ((textline = reader.readLine()) != null) {
			if (WorldManager.worldExists(textline)) {
				if (WorldManager.getOrCreateWorld(textline) == null) {
					log(Level.SEVERE, "Failed to (pre)load world: " + textline);
				}
			} else {
				log(Level.WARNING, "World: " + textline + " no longer exists and has not been loaded!");
			}
		}
		reader.close();
		
		//Spawn control settings
		SpawnControl.load(root() + "WorldSpawnRestriction.txt");
		
		//Weather settings
		reader = new SafeReader(root() + "WeatherHoldWorlds.txt");
		while ((textline = reader.readNonEmptyLine()) != null) {
			MWWeatherListener.holdWorld(textline, true);
		}
		reader.close();
		
		//Time settings
		TimeControl.load(root() + "TimeLockedWorlds.txt");
		
		//PvP
		PvPData.load(root() + "PvPWorlds.txt");
		
		//Portals
		Portal.loadPortals(root() + "portals.txt");

        //Commands
        getCommand("tpp").setExecutor(this);
        getCommand("world").setExecutor(this);  
        
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		//Portals
		Portal.savePortals(root() + "portals.txt");
		
		//PvP
		PvPData.save(root() + "PvPWorlds.txt");
		
		//Spawn control settings
		SpawnControl.save(root() + "WorldSpawnRestriction.txt");
		
		//Weather
		SafeWriter writer = new SafeWriter(root() + "WeatherHoldWorlds.txt");
		for (String worldname : MWWeatherListener.holdWorlds) {
			writer.writeLine(worldname);
		}
		writer.close();
		
		//Time settings
		TimeControl.save(root() + "TimeLockedWorlds.txt");
		
		//Loaded worlds
		writer = new SafeWriter(root() + "LoadedWorlds.txt");
		for (World w : getServer().getWorlds()) {
			writer.writeLine(w.getName());
		}
		writer.close();
		
		System.out.println("My Worlds disabled!");
	}
	
	public boolean showInv(CommandSender sender, String command) {
		message(sender, ChatColor.RED + "Invalid arguments for this command!");
		return showUsage(sender, command);
	}
	
	public boolean showUsage(CommandSender sender, String command) {
		if (Permission.has(sender, command)) {
			String msg = null;
			if (command.equalsIgnoreCase("world.list")) {
				msg = "/world list - Lists all worlds of this server";
			} else if (command.equalsIgnoreCase("world.info")) {
				msg = "/world info ([worldname]) - Shows information about a world";
			} else if (command.equalsIgnoreCase("world.portals")) {
				msg = "/world portals ([worldname]) - Lists all the portals";
			} else if (command.equalsIgnoreCase("world.load")) {
				msg = "/world load [worldname] - Loads a world into memory";
			} else if (command.equalsIgnoreCase("world.unload")) {
				msg = "/world unload [worldname] - Unloads a world from memory";
			} else if (command.equalsIgnoreCase("world.create")) {
				msg = "/world create [worldname] ([seed]) - Creates a new world";
			} else if (command.equalsIgnoreCase("world.delete")) {
				msg = "/world delete [worldname] - Permanently deletes a world";
			} else if (command.equalsIgnoreCase("world.spawn")) {
				msg = "/world spawn [worldname] - Teleport to the world spawn";
			} else if (command.equalsIgnoreCase("world.copy")) {
				msg = "/world copy [worldname] [newname] - Copies a world under a new name";		
			} else if (command.equalsIgnoreCase("world.evacuate")) {
				msg = "/world evacuate [worldname] - Removes all players by teleportation or kicking";		
			} else if (command.equalsIgnoreCase("world.repair")) {
				msg = "/world repair [worldname] ([Seed]) - Repairs the level.dat";		
			} else if (command.equalsIgnoreCase("world.save")) {
				msg = "/world save [worldname] - Saves the world";
			} else if (command.equalsIgnoreCase("tpp")) {
				msg = "/tpp [Portalname/Worldname] - Teleport to a Portal or World";
			} else if (command.equalsIgnoreCase("world.togglepvp")) {
				msg = "/world togglepvp ([world]) - Toggles PvP on or off";
			} else if (command.equalsIgnoreCase("world.weather")) {
				msg = "/world weather ([hold]) [storm/sun] ([worldname])\n    Changes and holds weather states";
			} else if (command.equalsIgnoreCase("world.time")) {
				msg = "/world time ([lock]) [day/night/noon/12] ([worldname])\n    Changes and locks the time";
			} else if (command.equalsIgnoreCase("world.allowspawn")) {
				msg = "/world allowspawn [creature] ([worldname])\n    Allows a certain creature type to spawn";
			} else if (command.equalsIgnoreCase("world.denyspawn")) {
				msg = "/world denyspawn [creature] ([worldname])\n    Denies a certain creature type from spawning";
			}
			if (msg == null) return true;
			for (String line : msg.split("\n")) {
				if (sender instanceof Player) {
					sender.sendMessage(ChatColor.YELLOW + line);
				} else {
					sender.sendMessage(line);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	public static void message(CommandSender sender, String message) {
		if (!(sender instanceof Player)) {
			for (ChatColor cc : ChatColor.values()) {
				message = message.replace(cc.toString(), "");
			}
		}
		sender.sendMessage(message);
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
		            ChatColor.RED + "[Other world]");
		}
		message(sender, ChatColor.YELLOW + "Available portals: " + 
				portals.length + " Portal" + ((portals.length == 1) ? "s" : ""));
		if (portals.length > 0) {
			String msgpart = "";
			for (String portal : portals) {
				Location loc = Portal.getPortalLocation(portal);
				ChatColor color = ChatColor.GREEN;
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
				
				//display it
				if (msgpart.length() + portal.length() < 70) {
					if (msgpart != "") msgpart += ChatColor.WHITE + ", ";
					msgpart += color + portal;
				} else {
					message(sender, msgpart);
					msgpart = color + portal;
				}
			}
			//possibly forgot one?
			if (msgpart != "") message(sender, msgpart);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
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
				if (showUsage(sender, "world.weather")) hac = true;		
				if (showUsage(sender, "world.time")) hac = true;			
				if (showUsage(sender, "world.spawn")) hac = true;
				if (showUsage(sender, "world.list")) hac = true;
				if (showUsage(sender, "world.info")) hac = true;
				if (showUsage(sender, "world.portals")) hac = true;
				if (showUsage(sender, "world.togglepvp")) hac = true;
				if (showUsage(sender, "world.allowspawn")) hac = true;
				if (showUsage(sender, "world.denyspawn")) hac = true;
				if (hac) {
					if (args.length >= 1) message(sender, ChatColor.RED + "Unknown command: " + args[0]);
				} else {
					message(sender, ChatColor.RED + "You don't have permission to use this command!");
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
							message(sender, ChatColor.YELLOW + "Information about the world: " + worldname);
							message(sender, ChatColor.WHITE + "Internal name: " + info.name);
							message(sender, ChatColor.WHITE + "World seed: " + info.seed);
							if (info.size > 1000000) {
								message(sender, ChatColor.WHITE + "World size: " + (info.size / 1000000) + " Megabytes");
							} else if (info.size > 1000) {
								message(sender, ChatColor.WHITE + "World size: " + (info.size / 1000) + " Kilobytes");
							} else {
								message(sender, ChatColor.WHITE + "World size: " + (info.size) + " Bytes");
							}
							//PvP
							if (PvPData.isPvP(worldname)) { 
								message(sender, ChatColor.WHITE + "PvP: " + ChatColor.GREEN + "Enabled");
							} else {
								message(sender, ChatColor.WHITE + "PvP: " + ChatColor.YELLOW + "Disabled");
							}
							//Time
							String timestr = TimeControl.getTimeString(worldname, info.time);
							message(sender, ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timestr);
							//Weather
							if (MWWeatherListener.isHolding(worldname)) {
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
				} else if (node == "world.togglepvp") {
					//==========================================
					//===============TOGGLE PVP COMMAND=========
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
						PvPData.setPvP(worldname, !PvPData.isPvP(worldname));
						if (PvPData.isPvP(worldname)) {
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
				} else if (node == "world.allowspawn" || node == "world.denyspawn") {
					//==========================================
					//===============TOGGLE ANIMAL SPAWNING=====
					//==========================================
					String worldname = WorldManager.getWorldName(sender, args, args.length == 3);
					if (worldname != null) {
						SpawnControl.SpawnRestriction r = SpawnControl.get(worldname);
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
							CreatureType ctype = CreatureType.valueOf(type);
							if (ctype == null && type.endsWith("S")) {
								ctype = CreatureType.valueOf(type.substring(0, type.length() - 2));
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
									r.denyAnimals = false;
								} else if (type.equals("monster")) {
									r.denyMonsters = false;
								} else if (type.equals("mob")) {
									r.denyAnimals = false;
									r.denyMonsters = false;
									r.deniedCreatures.clear();
								} else {
									r.deniedCreatures.remove(CreatureType.valueOf(type.toUpperCase()));
								}
								if (WorldManager.isLoaded(worldname)) {
									message(sender, ChatColor.GREEN + type + "s are now allowed to spawn on world: '" + worldname + "'!");
								} else {
									message(sender, ChatColor.GREEN + type + "s are allowed to spawn on world: '" + worldname + "' once it is loaded!");
								}
							} else {
								if (type.equals("animal")) {
									r.denyAnimals = true;
								} else if (type.equals("monster")) {
									r.denyMonsters = true;
								} else if (type.equals("mob")) {
									r.denyAnimals = true;
									r.denyMonsters = true;
								} else {
									r.deniedCreatures.add(CreatureType.valueOf(type.toUpperCase()));
								}
								//Capitalize
								type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
								if (WorldManager.isLoaded(worldname)) {
									message(sender, ChatColor.YELLOW + type + "s can no longer spawn on world: '" + worldname + "'!");
								} else {
									message(sender, ChatColor.YELLOW + type + "s can no longer spawn on world: '" + worldname + "' once it is loaded!");
								}
							}
						} else {
							message(sender, ChatColor.RED + "Invalid creature type!");
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
							MWWeatherListener.holdWorld(worldname, setHold);
							World w = WorldManager.getWorld(worldname);
							if (w != null) {
								boolean holdchange = MWWeatherListener.isHolding(worldname) != setHold;
								if (setStorm && ((!w.hasStorm()) || (setThunder && !w.isThundering()) || holdchange)) {
									MWWeatherListener.setWeather(w, true, setHold);
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
									MWWeatherListener.setWeather(w, false, setHold);
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
							if (lock) {
								TimeControl.lockTime(worldname, time);
								if (!WorldManager.isLoaded(worldname)) {
									TimeControl.setLocking(worldname, false);
									message(sender, ChatColor.YELLOW + "World '" + worldname + 
											"' is not loaded, time will be locked to " + 
											TimeControl.getTimeString(time) + " as soon it is loaded!");
								} else {
									TimeControl.setLocking(worldname, true);
									message(sender, ChatColor.GREEN + "Time of world '" + worldname + "' locked to " + 
									        TimeControl.getTimeString(time) + "!");
								}
							} else {
								World w = WorldManager.getWorld(worldname);
								if (w != null) {
									if (TimeControl.isLocked(worldname)) {
										TimeControl.unlockTime(worldname);
										w.setFullTime(time);
										message(sender, ChatColor.GREEN + "Time of world '" + worldname + "' unlocked and set to " + 
										        TimeControl.getTimeString(time) + "!");
									} else {
										w.setFullTime(time);
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
								if (WorldManager.createWorld(worldname, null) != null) {
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
								if (Bukkit.getServer().unloadWorld(w, false)) {
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
						if (!WorldManager.worldExists(worldname)) {
							String seed = "";
							for (int i = 2;i < args.length;i++) {
								seed += args[i] + " ";
							}
							notifyConsole(sender, "Issued a world creation command for world: " + worldname);
							message(sender, ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
							if (WorldManager.createWorld(worldname, seed) != null) {
								MyWorlds.message(sender, ChatColor.GREEN + "World '" + worldname + "' has been created and is ready for use!");
							} else {
								MyWorlds.message(sender, ChatColor.RED + "World creation failed!");
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
						if (worldname == null) worldname = args[1];
						if (WorldManager.getDataFolder(worldname).exists()) {
							if (!WorldManager.isLoaded(worldname) && WorldManager.isBroken(worldname)) {
								String seed = "";
								for (int i = 2;i < args.length;i++) {
									seed += args[i] + " ";
								}
								if (WorldManager.generateData(worldname, seed) && WorldManager.getData(worldname) != null) {
									message(sender, ChatColor.GREEN + "World: '" + worldname + "' has been repaired!");
								} else {
									message(sender, ChatColor.RED + "Failed to repair world '" + worldname + "'!");
								}
							} else {
								message(sender, ChatColor.YELLOW + "This world is not broken, repair not needed!");
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
					if (args.length == 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						if (worldname != null) {
							World w = Bukkit.getServer().getWorld(worldname);
							if (w != null) {
								if (sender instanceof Player) {
									((Player) sender).teleport(w.getSpawnLocation());
									sender.sendMessage(ChatColor.GREEN + "You have been teleported to '" + worldname + "'");
								} else {
									sender.sendMessage("A player is expected!");
								}
							} else {
								message(sender, ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
							}
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showInv(sender, node);
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
					if (sender instanceof Player) {
						if (args.length == 1) {
							Location tele = Portal.getPortalLocation(args[0]);
							if (tele != null) {
						    	((Player) sender).teleport(tele);
						    	message(sender, ChatColor.GREEN + "You teleported to portal '" + args[0] + "'!");
							} else {
								//Match world
								String worldname = WorldManager.matchWorld(args[0]);
								World w = WorldManager.getWorld(worldname);
								if (w != null) {
									((Player) sender).teleport(w.getSpawnLocation());
									message(sender, ChatColor.GREEN + "You teleported to the spawn area of world: '" + worldname + "'!");
								} else {
									message(sender, ChatColor.RED + "Portal or world not found!");
									listPortals(sender, Portal.getPortals());
								}
							}
						} else {
							showInv(sender, node);
						}	
					} else {
						sender.sendMessage("This command is only for players!");
					}		
				}
			} else {
				message(sender, ChatColor.RED + "You don't have permission to use this command!");
			}	
		}
		return true;
	}

}
