package com.bergerkiller.bukkit.mw;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import net.minecraft.server.CompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NextTickListEntry;
import net.minecraft.server.RegionFile;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.getspout.spout.chunkstore.ChunkStore;
import org.getspout.spout.chunkstore.SimpleChunkDataManager;
import org.getspout.spout.chunkstore.SimpleRegionFile;
import org.getspout.spoutapi.SpoutManager;

import com.bergerkiller.bukkit.mw.Tag.Type;

@SuppressWarnings("rawtypes")
public class WorldManager {
	private static HashMap regionfiles;
	private static Field rafField;
	public static boolean initRegionFiles() {
		try {
        	Field a = net.minecraft.server.RegionFileCache.class.getDeclaredField("a");
        	a.setAccessible(true);
			regionfiles = (HashMap) a.get(null);
			rafField = net.minecraft.server.RegionFile.class.getDeclaredField("c");
			rafField.setAccessible(true);
        	MyWorlds.log(Level.INFO, "Successfully bound variable to region file cache.");
			MyWorlds.log(Level.INFO, "File references to unloaded worlds will be cleared!");
			return true;
		} catch (Throwable t) {
			MyWorlds.log(Level.WARNING, "Failed to bind to region file cache.");
			MyWorlds.log(Level.WARNING, "Files will stay referenced after being unloaded!");
			t.printStackTrace();
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
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
			MyWorlds.log(Level.WARNING, "Exception while removing world reference for '" + worldname + "'!");
			ex.printStackTrace();
		}
		try {
			//Spout
			if (Bukkit.getServer().getPluginManager().isPluginEnabled("Spout")) {
				//Close the friggin' meta streams!
				SimpleChunkDataManager manager = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
				Field chunkstore = SimpleChunkDataManager.class.getDeclaredField("chunkStore");
				chunkstore.setAccessible(true);
				ChunkStore store = (ChunkStore) chunkstore.get(manager);
				Field regionfiles = ChunkStore.class.getDeclaredField("regionFiles");
				regionfiles.setAccessible(true);
				HashMap<UUID, HashMap<Long, SimpleRegionFile>> regionFiles;
				regionFiles = (HashMap<UUID, HashMap<Long, SimpleRegionFile>>) regionfiles.get(store);
				//operate on region files
				HashMap<Long, SimpleRegionFile> data = regionFiles.remove(world.getUID());
				if (data != null) {
					//close streams...
					for (SimpleRegionFile file : data.values()) {
						file.close();
					}
				}
			}
		} catch (Exception ex) {
			MyWorlds.log(Level.WARNING, "Exception while removing Spout world reference for '" + worldname + "'!");
			ex.printStackTrace();
		}
		for (Object key : removedKeys) {
			regionfiles.remove(key);
		}
		return true;
	}

	private static HashMap<String, Boolean> keepSpawnMemory = new HashMap<String, Boolean>();
	private static HashMap<String, Environment> worldEnvirons = new HashMap<String, Environment>();
	private static HashMap<String, String> worldGens = new HashMap<String, String>();
	private static HashMap<String, Difficulty> worldDiff = new HashMap<String, Difficulty>();
	private static HashMap<String, Position> spawnPoints = new HashMap<String, Position>();
	
	public static void load(Configuration config, String worldname) {
		String env = config.getString(worldname + ".environment", WorldManager.getEnvironment(worldname).name());
		for (Environment e : Environment.values()) {
			if (e.name().equalsIgnoreCase(env)) {
				worldEnvirons.put(worldname, e);
				break;
			}
		}
		String gen = config.getString(worldname + ".chunkGenerator", "default");
		if (!gen.equalsIgnoreCase("default") && !gen.equals("")) {
			worldGens.put(worldname, gen);
		}
		if (config.getBoolean(worldname + ".keepSpawnLoaded", true)) {
			setKeepSpawnInMemory(worldname, true);
		} else {
			setKeepSpawnInMemory(worldname, false);
		}
		Difficulty diff = parseDifficulty(config.getString(worldname + "difficulty", null));
		if (diff != null) {
			setDifficulty(worldname, diff);
		}
		String worldspawn = config.getString(worldname + ".spawn.world", null);
		if (worldspawn != null) {
			double x = config.getDouble(worldname + ".spawn.x", 0);
			double y = config.getDouble(worldname + ".spawn.y", 64);
			double z = config.getDouble(worldname + ".spawn.z", 0);
			float yaw = (float) config.getDouble(worldname + ".spawn.yaw", 0);
			float pitch = (float) config.getDouble(worldname + ".spawn.pitch", 0);
			spawnPoints.put(worldname, new Position(worldspawn, x, y, z, yaw, pitch));
		}
	}
	public static void save(Configuration config) {
		for (Map.Entry<String, Environment> entry : worldEnvirons.entrySet()) {
			config.setProperty(entry.getKey() + ".environment", entry.getValue().name());
		}
		for (Map.Entry<String, String> entry : worldGens.entrySet()) {
			config.setProperty(entry.getKey() + ".chunkGenerator", entry.getValue());
		}
		for (Map.Entry<String, Boolean> entry : keepSpawnMemory.entrySet()) {
			config.setProperty(entry.getKey() + ".keepSpawnLoaded", entry.getValue());
		}
		for (Map.Entry<String, Difficulty> entry : worldDiff.entrySet()) {
			config.setProperty(entry.getKey() + ".difficulty", entry.getValue().toString());
		}
		for (Map.Entry<String, Position> entry : spawnPoints.entrySet()) {
			config.setProperty(entry.getKey() + ".spawn.world", entry.getValue().getWorldName());
			config.setProperty(entry.getKey() + ".spawn.x", entry.getValue().getX());
			config.setProperty(entry.getKey() + ".spawn.y", entry.getValue().getY());
			config.setProperty(entry.getKey() + ".spawn.z", entry.getValue().getZ());
			config.setProperty(entry.getKey() + ".spawn.yaw", entry.getValue().getYaw());
			config.setProperty(entry.getKey() + ".spawn.pitch", entry.getValue().getPitch());
		}
	}
	
	/*
	 * World spawn points
	 */
	public static void setSpawnLocation(String forWorld, Location destination) {
		setSpawn(forWorld, new Position(destination));
	}
	public static void setSpawn(String forWorld, Position destination) {
		spawnPoints.put(forWorld.toLowerCase(), destination);
	}
	public static Position getSpawn(String ofWorld) {
		Position pos = spawnPoints.get(ofWorld.toLowerCase());
		if (pos == null) {
			//Try to store the default location
			World world = getWorld(ofWorld);
			if (world != null) {
				pos = new Position(world.getSpawnLocation());
				setSpawn(ofWorld, pos);
			}
		}
		return pos;
	}
	public static Location getSpawnLocation(String ofWorld) {
		Position pos = getSpawn(ofWorld);
		if (pos != null) {
			Location loc = pos.toLocation();
			if (loc.getWorld() != null) {
				return loc;
			}
		}
		return null;
	}
	public static Location getSpawnLocation(World ofWorld) {
		return getSpawnLocation(ofWorld.getName());
	}
	public static Position[] getSpawnPoints() {
		return spawnPoints.values().toArray(new Position[0]);
	}
	public static Position[] getSpawnPoints(World onWorld) {
		return getSpawnPoints(onWorld.getName());
	}
	public static Position[] getSpawnPoints(String onWorld) {
		ArrayList<Position> pos = new ArrayList<Position>();
		for (Position p : spawnPoints.values()) {
			if (p.getWorldName().equalsIgnoreCase(onWorld)) {
				pos.add(p);
			}
		}
		return pos.toArray(new Position[0]);
	}
	
	/*
	 * World difficulty
	 */
	public static void updateDifficulty(World world) {
		Difficulty dif = worldDiff.get(world.getName().toLowerCase());
		if (dif != null) {
			world.setDifficulty(dif);
		}
	}
	public static Difficulty parseDifficulty(String name) {
		if (name != null) {
			int val;
			try {
				val = Integer.parseInt(name);
			} catch (Exception ex) {
				val = -1;
			}
			for (Difficulty d : Difficulty.values()) {
				if (d.toString().equalsIgnoreCase(name) || (val != -1 && d.getValue() == val)) {
					return d;
				}
			}
		}
		return null;
	}
	public static Difficulty getDifficulty(String worldname) {
		worldname = worldname.toLowerCase();
		Difficulty dif = worldDiff.get(worldname);
		if (dif == null) {
			World w = getWorld(worldname);
			if (w != null) {
				dif = w.getDifficulty();
			} else {
				dif = Difficulty.NORMAL;
			}
			worldDiff.put(worldname, dif);
		}
		return dif;
	}
	public static Difficulty getDifficulty(World world) {
		return getDifficulty(world.getName());
	}
	public static void setDifficulty(String worldname, Difficulty diff) {
		if (diff != null) {
			worldDiff.put(worldname.toLowerCase(), diff);
			World w = getWorld(worldname);
			if (w != null) w.setDifficulty(diff);
		}
	}
	public static void setDifficulty(World world, Difficulty diff) {
		setDifficulty(world.getName(), diff);
	}
	
	/*
	 * Keep spawn in memory
	 */
	public static void setKeepSpawnInMemory(String worldname, boolean value) {
		keepSpawnMemory.put(worldname.toLowerCase(), value);
		World w = getWorld(worldname);
		if (w != null) {
			w.setKeepSpawnInMemory(value);
		}
	}
	public static boolean getKeepSpawnInMemory(String worldname) {
		worldname = worldname.toLowerCase();
		if (!keepSpawnMemory.containsKey(worldname)) {
			keepSpawnMemory.put(worldname, true);
			return true;
		} else {
			return keepSpawnMemory.get(worldname);
		}
	}
	public static void updateKeepSpawnMemory(World world) {
		boolean keep = getKeepSpawnInMemory(world.getName());
		if (keep != world.getKeepSpawnInMemory()) {
			world.setKeepSpawnInMemory(keep);
		}
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
				if (cmain.getMethod("getDefaultWorldGenerator", String.class, String.class).getDeclaringClass() != JavaPlugin.class) {
					gens.add(plugin.getDescription().getName());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return gens.toArray(new String[0]);
	}
	public static String getGeneratorPlugin(String forWorld) {
		return worldGens.get(forWorld.toLowerCase());
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
		worldGens.put(worldname.toLowerCase(), name);
	}

	/*
	 * General world fields
	 */
	private static File serverfolder;
	public static File getServerFolder() {
		if (serverfolder == null) serverfolder = MyWorlds.plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
		return serverfolder;
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
	
	public static Environment getEnvironment(String worldname) {
		Environment e = worldEnvirons.get(worldname.toLowerCase());
		if (e != null) return e;
		for (Environment env : Environment.values()) {
			if (worldname.toUpperCase().contains(env.toString())) {
				worldEnvirons.put(worldname.toLowerCase(), env);
				return env;
			}
		}
		worldEnvirons.put(worldname.toLowerCase(), Environment.NORMAL);
		return Environment.NORMAL;
	}
	public static String getGeneratorName(String worldname) {
		return worldGens.get(worldname.toLowerCase());
	}
	public static void setGeneratorName(String worldname, String genname) {
		worldGens.put(worldname.toLowerCase(), genname);
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
		        info.environment = getEnvironment(worldname);
		        info.chunkGenerator = getGeneratorName(worldname);
		        info.keepSpawnInMemory = getKeepSpawnInMemory(worldname);
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
	        info.environment = w.getEnvironment();
	        info.keepSpawnInMemory = getKeepSpawnInMemory(worldname);
	        info.chunkGenerator = getGeneratorName(worldname);
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
	public static World getWorld(String worldname) {
		if (worldname == null) return null;
		try {
			return Bukkit.getServer().getWorld(worldname);
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
		return Bukkit.getServer().unloadWorld(world, false);
	}

	public static World createWorld(String worldname, long seed) {
		String gen = getGeneratorPlugin(worldname);
		if (gen == null) {
			MyWorlds.log(Level.INFO, "Loading or creating world: '" + worldname + "' using seed " + seed);
		} else {
			MyWorlds.log(Level.INFO, "Loading or creating world: '" + worldname + "' using seed " + seed + " and chunk generator: '" + gen + "'");
		}
		final int retrycount = 3;
		World w = null;
		int i = 0;
		boolean useDepr = false;
		try {
			Class.forName("org.bukkit.WorldCreator");
		} catch (Exception ex) {
			useDepr = true;
		}
		ChunkGenerator cgen = null;
		try {
			if (gen != null) {
				cgen = getGenerator(worldname, gen);
			}
		} catch (Exception ex) {}
		if (gen != null && cgen == null) {
			MyWorlds.log(Level.SEVERE, "World '" + worldname + "' could not be loaded because the chunk generator '" + gen + "' was not found!");
			return null;
		}
		for (i = 0; i < retrycount + 1; i++) {
			try {
				Environment env = getEnvironment(worldname);
				if (useDepr) {
					Bukkit.getServer().createWorld(worldname, env, seed, cgen);
				} else {
					WorldCreator c = new WorldCreator(worldname);
					c.environment(env);
					c.seed(seed);
					c.generator(cgen);
					w = c.createWorld();
				}
			} catch (Exception ex) {
				MyWorlds.log(Level.WARNING, "World load issue: " + ex.getMessage());
			}
			if (w != null) break;
		}
		if (w != null) {
			PvPData.updatePvP(w);
			//Data file is made?
			if (!worldExists(worldname)) {
				w.save();
			}
		}
		if (w == null) {
			MyWorlds.log(Level.WARNING, "Operation failed after " + i + " retries!");
		} else if (i == 1) {
			MyWorlds.log(Level.INFO, "Operation succeeded after 1 retry!");
		} else if (i > 0) {
			MyWorlds.log(Level.INFO, "Operation succeeded after " + i + " retries!");
		}
		return w;
	}
	
	private static List redstoneInfo;	
	private static Field tickListField;
	private static boolean initFields = false;
	public static void setTime(World world, long time) {
		long timedif = time - world.getFullTime();
		net.minecraft.server.World w = ((CraftWorld) world).getHandle();
		w.setTimeAndFixTicklists(time);
		try {
			if (!initFields) {
				Field info = net.minecraft.server.BlockRedstoneTorch.class.getDeclaredField("b");
				info.setAccessible(true);
				redstoneInfo = (List) info.get(null);
				tickListField = net.minecraft.server.World.class.getDeclaredField("N");
				tickListField.setAccessible(true);
			}
			if (redstoneInfo != null && tickListField != null) {
				TreeSet ticklist = (TreeSet) tickListField.get(w);
		        NextTickListEntry nextticklistentry;

		        for (Iterator iterator = ticklist.iterator(); iterator.hasNext(); nextticklistentry.e += timedif) {
		            nextticklistentry = (NextTickListEntry) iterator.next();
		        }
				redstoneInfo.clear();
			}
		} catch (Exception ex) {
			MyWorlds.log(Level.SEVERE, "Failed to fix redstone while setting time!");
			ex.printStackTrace();
		}
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
				Location loc = Portal.getPortalLocation(name, true);
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
		MyWorlds.log(Level.INFO, "Performing repairs on region file: " + chunkfile.getName());
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(chunkfile, "rw");
			File backupfile = new File(backupfolder + File.separator + chunkfile.getName());
			int[] locations = new int[1024];
			for (int i = 0; i < 1024; i++) {
				locations[i] = raf.readInt();
			}
			//Validate the data
			int editcount = 0;
			for (int i = 0; i < locations.length; i++) {
				int location = locations[i];
				if (location == 0) continue;
				try {
					int offset = location >> 8;
                    int size = location & 255;
					raf.seek((long) (offset * 4096));
					int length = raf.readInt();
					//Read and test the data
					if (length > 4096 * size) {
						editcount++;
						locations[i] = 0;
						MyWorlds.log(Level.WARNING, "Invalid length: " + length + " > 4096 * " + size);
						//Invalid length
					} else if (size > 0 && length > 0) {
						byte version = raf.readByte();
						byte[] data = new byte[length - 1];
						raf.read(data);
						ByteArrayInputStream bais = new ByteArrayInputStream(data);
						//Try to load it all...
						DataInputStream stream;
						if (version == 1) {
							stream = new DataInputStream(new GZIPInputStream(bais));
						} else if (version == 2) {
							stream = new DataInputStream(new InflaterInputStream(bais));
						} else {
							stream = null;
							//Unknown version
							MyWorlds.log(Level.WARNING, "Unknown region version: " + version + " (we probably need an update here!)");
						}
						if (stream != null) {
							//Validate the stream and close
							try {
								NBTTagCompound nbttagcompound = CompressedStreamTools.a((DataInput) stream);
								if (nbttagcompound == null) {
									MyWorlds.log(Level.WARNING, "Damaged data at chunk " + i);
									editcount++;
									locations[i] = 0;
								}
							} catch (Exception ex) {
								//Invalid.
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
					MyWorlds.log(Level.WARNING, "Failed to make a copy of the file, no changes are made.");
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