package com.bergerkiller.bukkit.mw;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;

public class LoadChunksTask extends Task {
	private static LoadChunksTask task;
	private final Queue<ChunkCoord> remaining = new LinkedList<ChunkCoord>();

	private LoadChunksTask() {
		super(MyWorlds.plugin);
	}

	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
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

	public static void abort() {
		Task.stop(task);
		task = null;
	}

	public static void add(World world, int cx, int cz) {
		add(world, cx, cz, null);
	}

	public static void add(World world, int cx, int cz, Runnable taskWhenFinished) {
		ChunkCoord coord = new ChunkCoord();
		coord.x = cx;
		coord.z = cz;
		coord.world = world;
		coord.taskWhenFinished = taskWhenFinished;
		if (task == null) {
			task = new LoadChunksTask();
			task.start(0, 1);
		}
		synchronized (task) {
			task.remaining.offer(coord);
		}
	}

	private static class ChunkCoord {
		public int x, z;
		public World world;
		public Runnable taskWhenFinished;
	}
}
