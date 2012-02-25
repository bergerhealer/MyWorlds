package com.bergerkiller.bukkit.mw;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;

public class LoadChunksTask extends Task {
	
	private static class ChunkCoord {
		public int x, z;
		public World world;
		public Task taskWhenFinished;
	}
	
	public static void abort() {
		remaining = new LinkedList<ChunkCoord>();
		Task.stop(task);
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
		Task.stop(task);
		task = new LoadChunksTask().start(0, 1);
	}
	
	private static Task task;
	
	private LoadChunksTask() {
		super(MyWorlds.plugin);
	}
	
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
