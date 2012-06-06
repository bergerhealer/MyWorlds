package com.bergerkiller.bukkit.mw;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTCompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.RegionFile;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.mw.Tag.Type;

@SuppressWarnings("rawtypes")
public class WorldManager {
	private static HashMap regionfiles;
	private static Field rafField;
	public static boolean init() {
		try {
        	Field a = net.minecraft.server.RegionFileCache.class.getDeclaredField("a");
        	a.setAccessible(true);
			regionfiles = (HashMap) a.get(null);
			rafField = net.minecraft.server.RegionFile.class.getDeclaredField("c");
			rafField.setAccessible(true);
        	MyWorlds.plugin.log(Level.INFO, "Successfully bound variable to region file cache.");
			MyWorlds.plugin.log(Level.INFO, "File references to unloaded worlds will be cleared!");
			return true;
		} catch (Throwable t) {
			MyWorlds.plugin.log(Level.WARNING, "Failed to bind to region file cache.");
			MyWorlds.plugin.log(Level.WARNING, "Files will stay referenced after being unloaded!");
			t.printStackTrace();
			return false;
		}
	}
	public static void deinit() {
		regionfiles = null;
		rafField = null;
		serverfolder = null;
	}
	
	public static boolean clearWorldReference(World world) {
		String worldname = world.getName();
		if (regionfiles == null) return false;
		if (rafField == null) return false;
		ArrayList<Object> removedKeys = new ArrayList<Object>();
		try {
			for (Object o : regionfiles.entrySet()) {
				Map.Entry e = (Map.Entry) o;
				File f = (File) e.getKey();
				if (f.toString().startsWith("." + File.separator + worldname)) {
					SoftReference ref = (SoftReference) e.getValue();
					try {
						RegionFile file = (RegionFile) ref.get();
						if (file != null) {
							RandomAccessFile raf = (RandomAccessFile) rafField.get(file);
							raf.close();
							removedKeys.add(f);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		} catch (Exception ex) {
			MyWorlds.plugin.log(Level.WARNING, "Exception while removing world reference for '" + worldname + "'!");
			ex.printStackTrace();
		}
		for (Object key : removedKeys) {
			regionfiles.remove(key);
		}
		return true;
	}
	
	/*
	 * Get or set the respawn point of a world
	 */
	public static void setSpawnLocation(String forWorld, Location destination) {
		setSpawn(forWorld, new Position(destination));
	}
	public static void setSpawn(String forWorld, Position destination) {
		WorldConfig.get(forWorld).spawnPoint = destination;
	}
	public static Position getRespawn(String ofWorld) {
		return WorldConfig.get(ofWorld).spawnPoint;
	}
	public static Location getRespawnLocation(String ofWorld) {
		Position pos = getRespawn(ofWorld);
		if (pos != null) {
			Location loc = pos.toLocation();
			if (loc.getWorld() != null) {
				return loc;
			}
		}
		return null;
	}
	public static Location getRespawnLocation(World ofWorld) {
		return getRespawnLocation(ofWorld.getName());
	}
	
	/*
	 * Gets a possible teleport position on a certain world
	 */
	public static Location getSpawnLocation(World onWorld) {
		Position[] pos = getSpawnPoints(onWorld);
		if (pos.length > 0) return pos[0].toLocation();
		return onWorld.getSpawnLocation();
	}
	public static Position[] getSpawnPoints() {
		Collection<WorldConfig> all = WorldConfig.all();
		Position[] pos = new Position[all.size()];
		int i = 0;
		for (WorldConfig wc : all) {
			pos[i] = wc.spawnPoint;
			i++;
		}
		return pos;
	}
	public static Position[] getSpawnPoints(World onWorld) {
		return getSpawnPoints(onWorld.getName());
	}
	public static Position[] getSpawnPoints(String onWorld) {
		ArrayList<Position> pos = new ArrayList<Position>();
		for (Position p : getSpawnPoints()) {
			if (p.getWorldName().equalsIgnoreCase(onWorld)) {
				pos.add(p);
			}
		}
		return pos.toArray(new Position[0]);
	}
	
	/*
	 * Chunk generators
	 */
	@SuppressWarnings("unchecked")
	public static String[] getGeneratorPlugins() {
		ArrayList<String> gens = new ArrayList<String>();
		for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
			try {
				String mainclass = plugin.getDescription().getMain();
				Class cmain = Class.forName(mainclass);
				if (cmain == null) continue;
				if (cmain.getMethod("getDefaultWorldGenerator", String.class, String.class).getDeclaringClass() != JavaPlugin.class) {
					gens.add(plugin.getDescription().getName());
				}
			} catch (Throwable t) {}
		}
		return gens.toArray(new String[0]);
	}
	public static String getGeneratorPlugin(String forWorld) {
		return WorldConfig.get(forWorld).chunkGeneratorName;
	}
	public static String fixGeneratorName(String name) {
		if (name == null) return null;
		String id = "";
		int index = name.indexOf(":");
		if (index != -1) {
			id = name.substring(index + 1);
			name = name.substring(0, index);
		}
		if (!name.equals("")) {
			name = name.toLowerCase();
			//get plugin
			String pname = null;
			String[] plugins = getGeneratorPlugins();
			for (String plugin : plugins) {
				if (plugin.toLowerCase().equals(name)) {
					pname = plugin;
					break;
				}
			}
			if (pname == null) {
				for (String plugin : plugins) {
					if (plugin.toLowerCase().contains(name)) {
						pname = plugin;
						break;
					}
				}
			}
			if (pname == null) {
				return name + ":" + id;
			} else {
				return pname + ":" + id;
			}
		} else {
			return name + ":" + id;
		}
	}
	public static ChunkGenerator getGenerator(String worldname, String name) {
		if (name == null) return null;
		String id = "";
		int index = name.indexOf(":");
		if (index != -1) {
			id = name.substring(index + 1);
			name = name.substring(0, index);
		}
		Plugin plug = Bukkit.getServer().getPluginManager().getPlugin(name);
		if (plug != null) {
			return plug.getDefaultWorldGenerator(worldname, id);
		} else {
			return null;
		}
	}
	public static void setGenerator(String worldname, String name) {
		WorldConfig.get(worldname).chunkGeneratorName = name;
	}
	/*
	 * General world fields
	 */
	private static File serverfolder;
	public static File getServerFolder() {
		if (serverfolder == null) serverfolder = MyWorlds.plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
		return serverfolder;
	}
	
	public static File getPlayerDataFile(Player player) {
		return getPlayerDataFile(player.getName());
	}
	public static File getPlayerDataFile(String playerName) {
		return new File(getPlayerDataFolder(), playerName + ".dat");
	}
	public static File getPlayerDataFolder() {
		return new File(getServerFolder(), Bukkit.getWorlds().get(0).getName() + File.separator + "players");
	}
	
	public static long getSeed(String seed) {
		if (seed == null) return 0;
		long seedval = 0;
		try {
			seedval = Long.parseLong(seed);
		} catch (Exception ex) {
			seedval = seed.hashCode();
		}
		return seedval;
	}
	public static long getRandomSeed(String seed) {
		long seedval = getSeed(seed);
		if (seedval == 0) {
			seedval = new Random().nextLong();
		}
		return seedval;
	}
		
	public static String getWorldName(CommandSender sender, String[] args, boolean useAlternative) {
		return getWorldName(sender, args[args.length - 1], useAlternative);
	}
	public static String getWorldName(CommandSender sender, String alternative, boolean useAlternative) {
		String worldname = null;
		if (useAlternative) {
			worldname = WorldManager.matchWorld(alternative);
		} else if (sender instanceof Player) {
			worldname = ((Player) sender).getWorld().getName();
		} else {
			for (World w : Bukkit.getServer().getWorlds()) {
				worldname = w.getName();
				break;
			}
		}
		return worldname;
	}
		
	public static boolean generateData(String worldname, String seed) {
		return generateData(worldname, getRandomSeed(seed));
	}
	public static boolean generateData(String worldname, long seed) {
		Tag data = new Tag(Type.TAG_Compound, "Data", new Tag[] {
				new Tag(Type.TAG_Byte, "thundering", (byte) 0), 
				new Tag(Type.TAG_Long, "LastPlayed", System.currentTimeMillis()),
				new Tag(Type.TAG_Long, "RandomSeed", seed), 
				new Tag(Type.TAG_Int, "version", (int) 19132), 
				new Tag(Type.TAG_Long, "Time", 0L),
				new Tag(Type.TAG_Byte, "raining", (byte) 0), 
				new Tag(Type.TAG_Int, "SpawnX", 0), 
				new Tag(Type.TAG_Int, "thunderTime", (int) 200000000), 
				new Tag(Type.TAG_Int, "SpawnY", 64), 
				new Tag(Type.TAG_Int, "SpawnZ", 0), 
				new Tag(Type.TAG_String, "LevelName", worldname),
				new Tag(Type.TAG_Long, "SizeOnDisk", getWorldSize(worldname)),
				new Tag(Type.TAG_Int, "rainTime", (int) 50000), 
				new Tag(Type.TAG_End, null, null)});
		Tag finaltag = new Tag(Type.TAG_Compound, null, new Tag[] {data, new Tag(Type.TAG_End, null, null)});
				
		//write the data
	    return setData(worldname, finaltag);
	}
	public static Tag getData(String worldname) {
		File f = getDataFile(worldname);
		if (!f.exists()) return null;
		try {
			FileInputStream fis = new FileInputStream(f);
			Tag t = Tag.readFrom(fis);
			fis.close();
			return t;
		} catch (Exception ex) {
			return null;
		}
	}
	public static boolean setData(String worldname, Tag data) {
    	File datafile = getDataFile(worldname);
    	try {
			OutputStream s = new FileOutputStream(datafile);
			data.writeTo(s);
			s.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static File getDataFolder(String worldname) {
		return new File(getServerFolder() + File.separator + worldname);
	}
	public static File getDataFile(String worldname) {
		return new File(getDataFolder(worldname) + File.separator + "level.dat");
	}
	public static File getUIDFile(String worldname) {
		return new File(getDataFolder(worldname) + File.separator + "uid.dat");
	}

	public static long getWorldSize(String worldname) {
		return getFolderSize(getDataFolder(worldname));
	}
	public static WorldInfo getInfo(String worldname) {
		WorldInfo info = null;
		try {
			Tag t = getData(worldname);
			if (t != null) {
				info = new WorldInfo();
				info.name = t.findTagByName("LevelName").getValue().toString();
				info.seed = (Long) t.findTagByName("RandomSeed").getValue();
				info.size = (Long) t.findTagByName("SizeOnDisk").getValue();
				info.time = (Long) t.findTagByName("Time").getValue();
				info.raining = ((Byte) t.findTagByName("raining").getValue()) != 0;
		        info.thundering = ((Byte) t.findTagByName("thundering").getValue()) != 0;
				if (info.size == 0) info.size = getWorldSize(worldname);
			}
		} catch (Exception ex) {}
		World w = getWorld(worldname);
		if (w != null) {
			if (info == null) {
				info = new WorldInfo();
				info.size = getWorldSize(worldname);
			}
			info.name = w.getName();
			info.seed = w.getSeed();
			info.time = w.getFullTime();
			info.raining = w.hasStorm();
	        info.thundering = w.isThundering();
		}
		return info;
	}
	public static String[] getWorlds() {
		ArrayList<String> rval = new ArrayList<String>();
		for (String world : getServerFolder().list()) {
			if (worldExists(world) || isLoaded(world)) {
				rval.add(world);
			}
		}
		return rval.toArray(new String[0]);
	}
	public static String matchWorld(String matchname) {
		String[] worldnames = getWorlds();
		for (String worldname : worldnames) {
			if (worldname.equalsIgnoreCase(matchname)) return worldname;
		}
		matchname = matchname.toLowerCase();
		for (String worldname : worldnames) {
			if (worldname.toLowerCase().contains(matchname)) return worldname;
		}
		return null;
	}
	public static CraftWorld getWorld(String worldname) {
		if (worldname == null) return null;
		try {
			return (CraftWorld) Bukkit.getServer().getWorld(worldname);
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static boolean isBroken(String worldname) {
		if (getData(worldname) == null) {
			return getWorld(worldname) == null;
		}
		return false;
	}
	public static boolean isLoaded(String worldname) {
		return getWorld(worldname) != null;
	}
	public static boolean worldExists(String worldname) {
		return getDataFile(worldname).exists();
	}
	
	public static World getOrCreateWorld(String worldname) {
		World w = getWorld(worldname);
		if (w != null) return w;
		return createWorld(worldname, 0);
	}
	
	public static boolean unload(World world) {
		if (world == null) return false;
		return Bukkit.getServer().unloadWorld(world, world.isAutoSave());
	}

	public static World createWorld(String worldname, long seed) {
		String gen = getGeneratorPlugin(worldname);
		StringBuilder msg = new StringBuilder().append("Loading or creating world '").append(worldname).append("'");
		if (seed == 0) {
			if (gen != null) {
				msg.append(" using chunk generator: '").append(gen).append("'");
			}
		} else {
			msg.append(" using seed ").append(seed);
			if (gen != null) {
				msg.append(" and chunk generator: '").append(gen).append("'");
			}
		}
		MyWorlds.plugin.log(Level.INFO, msg.toString());
		
		final int retrycount = 3;
		World w = null;
		int i = 0;
		ChunkGenerator cgen = null;
		try {
			if (gen != null) {
				cgen = getGenerator(worldname, gen);
			}
		} catch (Exception ex) {}
		if (cgen == null) {
			if (gen != null) {
				MyWorlds.plugin.log(Level.SEVERE, "World '" + worldname + "' could not be loaded because the chunk generator '" + gen + "' was not found!");
				return null;
			}
		}
		WorldConfig wc = WorldConfig.get(worldname);
		wc.chunkGeneratorName = gen;
		for (i = 0; i < retrycount + 1; i++) {
			try {
				WorldCreator c = new WorldCreator(worldname);
				wc.worldmode.apply(c);
				if (seed != 0) c.seed(seed);
				c.generator(cgen);
				w = c.createWorld();
			} catch (Exception ex) {
				MyWorlds.plugin.log(Level.WARNING, "World load issue: " + ex.getMessage());
				for (StackTraceElement el : ex.getStackTrace()) {
					if (el.getClassName().equals("com.bergerkiller.bukkit.mw.WorldManager")) break;
					System.out.println("    at " + el.toString());
				}
			}
			if (w != null) break;
		}
		if (w != null) {
			wc.update(w);
			//Data file is made?
			if (!worldExists(worldname)) {
				//w.save();
			}
		}
		if (w == null) {
			MyWorlds.plugin.log(Level.WARNING, "Operation failed after " + i + " retries!");
		} else if (i == 1) {
			MyWorlds.plugin.log(Level.INFO, "Operation succeeded after 1 retry!");
		} else if (i > 0) {
			MyWorlds.plugin.log(Level.INFO, "Operation succeeded after " + i + " retries!");
		}
		return w;
	}
	
	public static void setTime(World world, long time) {
		net.minecraft.server.World w = ((CraftWorld) world).getHandle();
		w.setTimeAndFixTicklists(time);
	}
	
	private static boolean delete(File folder) {
		if (folder.isDirectory()) {
			for (File f : folder.listFiles()) {
				if (!delete(f)) return false;
			}
		}
		return folder.delete();
	}
    public static boolean copy(File sourceLocation , File targetLocation) {
    	try {
            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists()) {
                    targetLocation.mkdir();
                }
                for (String child : sourceLocation.list()) {
                    if (!copy(new File(sourceLocation, child), new File(targetLocation, child))) {
                    	return false;
                    }
                }
            } else {
                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);
                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
            return true;
    	} catch (IOException ex) {
    		ex.printStackTrace();
    		return false;
    	}
    }
    @SuppressWarnings("unused")
	private static boolean rename(File sourceLocation, File targetLocation) {
    	return sourceLocation.renameTo(targetLocation);
    }
	private static long getFolderSize(File folder) {
		if (!folder.exists()) return 0;
		if (folder.isDirectory()) {
			long size = 0;
			for (File subfile : folder.listFiles()) {
				size += getFolderSize(subfile);
			}
			return size;
		} else {
			return folder.length();
		}
	}
    private static boolean renameWorld(String worldname, String newname) {
    	if (isLoaded(worldname)) return false;
    	Tag t = getData(worldname);
    	if (t == null) return false;
    	t = t.findTagByName("Data");
    	if (t == null || t.getType() != Type.TAG_Compound) return false;
    	int i = 0;
    	for (Tag tt : (Tag[]) t.getValue()) {
    		if (tt.getName().equals("LevelName")) {
    			t.removeTag(i);
    			t.insertTag(new Tag(Type.TAG_String, "LevelName", newname), i);
    			break;
    		}
    		i++;
    	}
    	return setData(worldname, t);
    }
    
	public static boolean deleteWorld(String worldname) {
		return delete(getDataFolder(worldname));
	}
	public static boolean copyWorld(String worldname, String newname) {
		if (!copy(getDataFolder(worldname), getDataFolder(newname))) return false;;
		renameWorld(newname, newname);
		File uid = new File(getDataFolder(newname) + File.separator + "uid.dat");
		if (uid.exists()) uid.delete();
		return true;
	}
	
	public static Location getEvacuation(Player player) {
		World world = player.getWorld();
		String[] portalnames;
		if (Permission.has(player, "world.spawn") || Permission.has(player, "tpp")) {
			for (Position pos : getSpawnPoints()) {
				Location loc = pos.toLocation();
				if (loc.getWorld() == null) continue;
				if (Permission.canEnter(player, loc.getWorld())) {
					return loc;
				}
			}
			portalnames = Portal.getPortals();
		} else {
			portalnames = Portal.getPortals(world);
		}
		for (String name : portalnames) {
			if (Permission.canEnterPortal(player, name)) {
				Location loc = Portal.getPortalLocation(name, player.getWorld().getName(), true);
				if (loc == null) continue;
				if (loc.getWorld() == null) continue;
				if (Permission.canEnter(player, loc.getWorld())) {
					return loc;
				}
			}
		}
		return null;
	}

	public static void evacuate(World world, String message) {
		for (Player player : world.getPlayers()) {
			Location loc = getEvacuation(player);
			if (loc != null) {
				player.teleport(loc);
				player.sendMessage(ChatColor.RED + message);
			} else {
				player.kickPlayer(message);
			}
		}
	}
	public static void teleportToClonedWorld(World from, World cloned) {
		for (Player p : from.getPlayers().toArray(new Player[0])) {
			Location loc = p.getLocation();
			loc.setWorld(cloned);
			p.teleport(loc);
		}
	}
	public static void teleportToWorldSpawn(World from, World to) {
		for (Player p : from.getPlayers().toArray(new Player[0])) {
			p.teleport(to.getSpawnLocation());
		}
	}
	
	/**
	 * Repairs the chunk region file
	 * Returns -1 if the file had to be removed
	 * Returns -2 if we had no access
	 * Returns -3 if file removal failed (from -1)
	 * Returns the amount of changed chunks otherwise
	 * @param chunkfile
	 * @param backupfolder
	 * @return
	 */
	public static int repairRegion(File chunkfile, File backupfolder) {
		MyWorlds.plugin.log(Level.INFO, "Performing repairs on region file: " + chunkfile.getName());
		RandomAccessFile raf = null;
		try {
			String name = chunkfile.getName().substring(2);
			String xname = name.substring(0, name.indexOf('.'));
			name = name.substring(xname.length() + 1);
			String zname = name.substring(0, name.indexOf('.'));
			
			int xOffset = 32 * Integer.parseInt(xname);
			int zOffset = 32 * Integer.parseInt(zname);
			
			raf = new RandomAccessFile(chunkfile, "rw");
			File backupfile = new File(backupfolder + File.separator + chunkfile.getName());
			int[] locations = new int[1024];
			for (int i = 0; i < 1024; i++) {
				locations[i] = raf.readInt();
			}
			//Validate the data
			int editcount = 0;
			int chunkX = 0;
			int chunkZ = 0;
			
			//x = 5
			//z = 14
			//i = 5 + 14 * 32 = 453
			//x = i % 32 = 5
			//z = [(453 - 5)]  448 / 32 = 
			
			byte[] data = new byte[8096];
			for (int i = 0; i < locations.length; i++) {
				chunkX = i % 32;
				chunkZ = (i - chunkX) >> 5;
				chunkX += xOffset;
				chunkZ += zOffset;
				int location = locations[i];
				if (location == 0) continue;
				try {
					int offset = location >> 8;
                    int size = location & 255;
                    long seekindex = (long) (offset * 4096);
					raf.seek(seekindex);
					int length = raf.readInt();
					//Read and test the data
					if (length > 4096 * size) {
						editcount++;
						locations[i] = 0;
						MyWorlds.plugin.log(Level.WARNING, "Invalid length: " + length + " > 4096 * " + size);
						//Invalid length
					} else if (size > 0 && length > 0) {
						byte version = raf.readByte();
						if (data.length < length + 10) {
							data = new byte[length - 1];
						}
						raf.read(data);
						ByteArrayInputStream bais = new ByteArrayInputStream(data);
						//Try to load it all...
						DataInputStream stream;
						if (version == 1) {
							stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(bais)));
						} else if (version == 2) {
							stream = new DataInputStream(new BufferedInputStream(new InflaterInputStream(bais)));
						} else {
							stream = null;
							//Unknown version
							MyWorlds.plugin.log(Level.WARNING, "Unknown region version: " + version + " (we probably need an update here!)");
						}
						if (stream != null) {
							
							//Validate the stream and close
							try {
								NBTBase base = NBTTagCompound.b((DataInput) stream);
								if (base == null) {
									editcount++;
									locations[i] = 0;
									MyWorlds.plugin.log(Level.WARNING, "Invalid tag compound at chunk " + chunkX + "/" + chunkZ);
								} else if (!(base instanceof NBTTagCompound)) {
									editcount++;
									locations[i] = 0;
									MyWorlds.plugin.log(Level.WARNING, "Invalid tag compound at chunk " + chunkX + "/" + chunkZ);
								} else {
									//correct location?
									NBTTagCompound comp = (NBTTagCompound) base;
									if (comp.hasKey("Level")) {
										NBTTagCompound level = comp.getCompound("Level");
										int xPos = level.getInt("xPos");
										int zPos = level.getInt("zPos");
										//valid coordinates?
										if (xPos != chunkX || zPos != chunkZ) {
											MyWorlds.plugin.log(Level.WARNING, "Chunk [" + xPos + "/" + zPos + "] was stored at [" + chunkX + "/" + chunkZ + "], moving...");
											level.setInt("xPos", chunkX);
											level.setInt("zPos", chunkZ);
											//rewrite to stream
											ByteArrayOutputStream baos = new ByteArrayOutputStream(8096);
									        DataOutputStream dataoutputstream = new DataOutputStream(new DeflaterOutputStream(baos));
									        NBTCompressedStreamTools.a(level, (DataOutput) dataoutputstream);
									        dataoutputstream.close();
									        //write to region file
									        raf.seek(seekindex);
									        byte[] newdata = baos.toByteArray();
									        raf.writeInt(newdata.length + 1);
									        raf.writeByte(2);
									        raf.write(newdata, 0, newdata.length);
									        editcount++;
										}
									} else {
										editcount++;
										locations[i] = 0;
										MyWorlds.plugin.log(Level.WARNING, "Invalid tag compound at chunk " + chunkX + "/" + chunkZ);
									}
								}
							} catch (Exception ex) {
								//Invalid.
								editcount++;
								locations[i] = 0;
								MyWorlds.plugin.log(Level.WARNING, "Stream  " + i);
								ex.printStackTrace();
							}
							stream.close();
						}
					}
				} catch (Exception ex) {
					editcount++;
					locations[i] = 0;
					ex.printStackTrace();
				}
			}
			if (editcount > 0) {
				if (backupfolder.mkdirs() && copy(chunkfile, backupfile)) {
					//Write out the new locations
					raf.seek(0);
					for (int location : locations) {
						raf.writeInt(location);
					}
				} else {
					MyWorlds.plugin.log(Level.WARNING, "Failed to make a copy of the file, no changes are made.");
					return -2;
				}
			}
			//Done.
			raf.close();
			return editcount;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			if (raf != null) raf.close();
		} catch (Exception ex) {}
		
		try {
			chunkfile.delete();
			return -1;
		} catch (Exception ex) {
			return -3;
		}
	}
}