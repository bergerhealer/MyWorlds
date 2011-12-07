package com.bergerkiller.bukkit.mw;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.World;

public class LoadChunksTask implements Runnable {
	
	private static class ChunkCoord {
		public int x, z;
		public World world;
		public Task taskWhenFinished;
	}
	
	public static void abort() {
		remaining = new LinkedList<ChunkCoord>();
		if (taskid != -1) {
			MyWorlds.plugin.getServer().getScheduler().cancelTask(taskid);
			taskid = -1;
		}
	}
	
	public static void init() {
		remaining = new LinkedList<ChunkCoord>();
	}
	
	public static void deinit() {
		abort();
		remaining.clear();
		remaining = null;
	}
	
	private static Queue<ChunkCoord> remaining;
	public static void add(World world, int cx, int cz) {
		add(world, cx, cz, null);
	}
	public static void add(World world, int cx, int cz, Task taskWhenFinished) {
		ChunkCoord coord = new ChunkCoord();
		coord.x = cx;
		coord.z = cz;
		coord.world = world;
		coord.taskWhenFinished = taskWhenFinished;
		remaining.offer(coord);
		if (taskid == -1) {
			taskid = MyWorlds.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWorlds.plugin, new LoadChunksTask(), 0, 1);
		}
	}
	private static int taskid = -1;
	
	private LoadChunksTask() {}
	
	@Override
	public void run() {
		ChunkCoord next = remaining.poll();
		if (next == null) {
			abort();
		} else {
			if (!next.world.isChunkLoaded(next.x, next.z)) {
				next.world.loadChunk(next.x, next.z);
			}
			if (next.taskWhenFinished != null) {
				next.taskWhenFinished.run();
			}
		}
	}

}
