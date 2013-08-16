package com.bergerkiller.bukkit.mw;

import java.util.Iterator;
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
		// Load a maximum of 10 chunks at a time
		for (int i = 0; i < 10;) {
			ChunkCoord next = remaining.poll();
			if (next == null) {
				abort();
				break;
			} else {
				if (!next.world.isChunkLoaded(next.x, next.z)) {
					next.world.loadChunk(next.x, next.z);
					i++;
				}
				if (next.taskWhenFinished != null) {
					next.taskWhenFinished.run();
				}
			}
		}
	}

	public static void abortWorld(World world) {
		if (task == null) {
			return;
		}
		Iterator<ChunkCoord> iter = task.remaining.iterator();
		while (iter.hasNext()) {
			ChunkCoord coord = iter.next();
			if (coord.world == world) {
				iter.remove();
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
