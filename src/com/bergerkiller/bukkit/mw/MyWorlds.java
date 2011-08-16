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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class MyWorlds extends JavaPlugin {
	public static MyWorlds plugin;
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, message);
	}
	
	private final MWEntityListener entityListener = new MWEntityListener(this);
	private final MWBlockListener blockListener = new MWBlockListener(this);
	private final MWWorldListener worldListener = new MWWorldListener(this);
	
	public void onEnable() {
		plugin = this;

		//Event registering
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.ENTITY_PORTAL_ENTER, entityListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Highest, this);   
        pm.registerEvent(Event.Type.BLOCK_CANBUILD, blockListener, Priority.Highest, this);   
        pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Highest, this);   
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Highest, this);  
        pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Monitor, this);  
        
        //Permissions
		Permission.init(this);
		
		//Portals
		Portal.loadPortals(getDataFolder() + File.separator + "portals.txt");
        
        //Commands
        getCommand("tpp").setExecutor(this);
        getCommand("world").setExecutor(this);
        
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		//Portals
		Portal.savePortals(getDataFolder() + File.separator + "portals.txt");
		
		System.out.println("World Travel disabled!");
	}
	
	public void showUsage(CommandSender sender, String command) {
		if (Permission.has(sender, command)) {
			String msg = (sender instanceof Player) ? ChatColor.YELLOW.toString() : "";
			if (command.equalsIgnoreCase("world.list")) {
				msg += "/world list - Lists all worlds of this server";
			} else if (command.equalsIgnoreCase("world.info")) {
				msg += "/world info ([worldname]) - Shows information about a world";
			} else if (command.equalsIgnoreCase("world.portals")) {
				msg += "/world portals ([worldname]) - Lists all the portals";
			} else if (command.equalsIgnoreCase("world.load")) {
				msg += "/world load [worldname] - Loads a world into memory";
			} else if (command.equalsIgnoreCase("world.unload")) {
				msg += "/world unload [worldname] - Unloads a world from memory";
			} else if (command.equalsIgnoreCase("world.create")) {
				msg += "/world create [worldname] ([seed]) - Creates a new world";
			} else if (command.equalsIgnoreCase("world.delete")) {
				msg += "/world delete [worldname] - Permanently deletes a world";
			} else if (command.equalsIgnoreCase("world.spawn")) {
				msg += "/world spawn [worldname] - Teleport to the world spawn";
			} else if (command.equalsIgnoreCase("world.copy")) {
				msg += "/world copy [worldname] [newname] - Copies a world under a new name";		
			} else if (command.equalsIgnoreCase("world.evacuate")) {
				msg += "/world evacuate [worldname] - Removes all players by teleportation or kicking";		
			} else if (command.equalsIgnoreCase("world.repair")) {
				msg += "/world repair [worldname] ([Seed]) - Repairs the level.dat";		
			} else if (command.equalsIgnoreCase("world.save")) {
				msg += "/world save [worldname] - Saves the world";
			} else if (command.equalsIgnoreCase("tpp")) {
				msg += "/tpp [Portalname/Worldname] - Teleport to a Portal or World";
			}
			sender.sendMessage(msg);
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
			log(Level.INFO, "[MyWorlds] " + ((Player) sender).getName() + " " + message);
		}
	}
	private void listPortals(CommandSender sender, String[] portals) {
		message(sender, ChatColor.YELLOW + "Available portals: " + portals.length + " Portals");
		if (portals.length > 0) {
			String msgpart = "";
			for (String portal : portals) {
				//display it
				if (msgpart.length() + portal.length() < 70) {
					if (msgpart != "") msgpart += ChatColor.WHITE + ", ";
					msgpart += ChatColor.GREEN + portal;
				} else {
					message(sender, msgpart);
					msgpart = ChatColor.GREEN + portal;
				}
			}
			//possibly forgot one?
			if (msgpart != "") message(sender, msgpart);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		boolean isplayer = sender instanceof Player;
		if (cmdLabel.equalsIgnoreCase("world")) {
			boolean showusage = false;
			boolean showarginv = true;
			if (args.length == 0) {
				showusage = true;
			} else {
				String subcommand = args[0];
				if (subcommand.equalsIgnoreCase("list") && Permission.has(sender, "world.list")) {
					//==========================================
					//===============LIST COMMAND===============
					//==========================================
					if (isplayer) {
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
				} else if (subcommand.equalsIgnoreCase("portals") && Permission.has(sender, "world.portals")) {
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
				} else if (subcommand.equalsIgnoreCase("info") && Permission.has(sender, "world.info")) {
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
							World w = Bukkit.getServer().getWorld(worldname);
							if (w != null) {
								int playercount = w.getPlayers().size();
								if (playercount > 0) {
									String msg = ChatColor.WHITE + "World status: " + ChatColor.GREEN + "Loaded" + ChatColor.WHITE + " with ";
									msg += playercount + ((playercount <= 1) ? " player" : " players");
									message(sender, msg);
								} else {
									message(sender, ChatColor.WHITE + "World status: " + ChatColor.YELLOW + "Stand-by");
								}
							} else {
								message(sender, ChatColor.WHITE + "World status: " + ChatColor.RED + "Unloaded");
							}
						}
					} else {
						message(sender, ChatColor.RED + "World not found!");
					}
				} else if (subcommand.equalsIgnoreCase("load") && Permission.has(sender, "world.load")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("unload") && Permission.has(sender, "world.unload")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("evacuate") && Permission.has(sender, "world.evacuate")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("spawn") && Permission.has(sender, "world.spawn")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("create") && Permission.has(sender, "world.create")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("repair") && Permission.has(sender, "world.repair")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("save") && Permission.has(sender, "world.save")) {
					//==========================================
					//===============SAVE COMMAND===============
					//==========================================
					if (args.length >= 2) {
						String worldname = WorldManager.matchWorld(args[1]);
						World w = WorldManager.getWorld(worldname);
						if (w != null) {
							message(sender, ChatColor.YELLOW + "Saving world '" + worldname + "'...");
							w.save();
							message(sender, ChatColor.GREEN + "World saved!");
						} else {
							message(sender, ChatColor.RED + "World not found!");
						}
					} else {
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("delete") && Permission.has(sender, "world.delete")) {
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
						showusage = true;
					}
				} else if (subcommand.equalsIgnoreCase("copy") && Permission.has(sender, "world.copy")) {
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
						showusage = true;
					}
				} else {
					message(sender, ChatColor.DARK_RED + "Unknown command: " + subcommand);
					showusage = true;
					showarginv = false;
				}
			}
			if (showusage) {
				if (showarginv) {
					message(sender, ChatColor.DARK_RED + "Invalid amount of arguments.");
				}
				showUsage(sender, "world.list");
				showUsage(sender, "world.portals");
				showUsage(sender, "world.info");
				showUsage(sender, "world.spawn");
				showUsage(sender, "world.save");
				showUsage(sender, "world.load");
				showUsage(sender, "world.unload");
				showUsage(sender, "world.create");
				showUsage(sender, "world.repair");
				showUsage(sender, "world.delete");
				showUsage(sender, "world.rename");
				showUsage(sender, "world.copy");
			}
		} else if (cmdLabel.equalsIgnoreCase("tpp") && Permission.has(sender, "tpp")) {
			if (args.length == 1) {
				String portal = args[0];
				Location tele = Portal.getPortalLocation(portal);
				if (tele != null) {
				    if (sender instanceof Player) {
				    	((Player) sender).teleport(tele);
				    } else {
				       sender.sendMessage("This command is only for players!");
				    }
				} else {
					message(sender, ChatColor.RED + "Portal not found!");
					listPortals(sender, Portal.getPortals());
				}
			} else {
				message(sender, ChatColor.DARK_RED + "Invalid amount of arguments.");
				showUsage(sender, "tpp");
			}
		} else {
			message(sender, ChatColor.DARK_RED + "Unknown command!");
		}
		return true;
	}

}
