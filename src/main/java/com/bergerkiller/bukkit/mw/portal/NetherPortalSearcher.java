package com.bergerkiller.bukkit.mw.portal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * Loads areas of the world, then asks the server to find nether portals
 * in those areas. Performs debouncing as to reduce the number of searches
 * performed. Caches successful results until those portals stop existing.
 */
public class NetherPortalSearcher {
    private static final int CACHE_LIFETIME = 200; // keep portal search results in cache for 200 ticks
    private static final int LOAD_LIFETIME = 100; // keep chunks loaded after each use for 100 ticks
    private final MyWorlds plugin;
    private final Map<String, List<SearchResult>> _cachedForWorld = new HashMap<String, List<SearchResult>>();
    private final Map<BlockLocation, SearchResult> _cachedByBlock = new HashMap<BlockLocation, SearchResult>();
    private Task _cleanupTask;

    public NetherPortalSearcher(MyWorlds plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        _cleanupTask = new Task(plugin) {
            @Override
            public void run() {
                int expireTime = CommonUtil.getServerTicks() - CACHE_LIFETIME;
                int unloadTime = CommonUtil.getServerTicks() - LOAD_LIFETIME;

                Iterator<List<SearchResult>> worldIter = _cachedForWorld.values().iterator();
                while (worldIter.hasNext()) {
                    List<SearchResult> worldResults = worldIter.next();
                    Iterator<SearchResult> resultIter = worldResults.iterator();
                    while (resultIter.hasNext()) {
                        SearchResult result = resultIter.next();

                        // Unload chunks when not used for a while
                        if (result._lastUsedTick == unloadTime) {
                            result.unloadChunks();
                        }

                        // Remove when it hasn't been used in a while
                        if (result._lastUsedTick < expireTime) {
                            result.unloadChunks();
                            result._status = SearchStatus.NOT_FOUND;
                            for (BlockLocation location : result._startLocations) {
                                SearchResult removed = _cachedByBlock.remove(location);
                                if (removed != result) {
                                    _cachedByBlock.put(location, removed); // restore
                                }
                            }
                            result._startLocations.clear();
                            resultIter.remove();
                        }
                    }

                    if (worldResults.isEmpty()) {
                        worldIter.remove();
                    }
                }
            }
        }.start(1, 1);
    }

    public void disable() {
        Task.stop(_cleanupTask);
        _cleanupTask = null;

        for (List<SearchResult> worldSearches : _cachedForWorld.values()) {
            for (SearchResult result : worldSearches) {
                result.unloadChunks();
            }
        }
        _cachedForWorld.clear();
        _cachedByBlock.clear();
    }

    /**
     * Searches for the nearest nether portal near a block on the world.
     * If not already searching, it will start loading chunks prior
     * to searching the area for a portal. If a portal was already
     * found before, then that result becomes instantly available,
     * after verifying the portal is still there.
     *
     * @param start Block around which to look for portals
     * @param radius Search radius around the block
     * @param createOptions Options for creating a portal if none exists.
     *        Use null to disallow creating new portals.
     * @return result
     */
    public SearchResult search(Block start, int radius, CreateOptions createOptions) {
        BlockLocation location = new BlockLocation(start);

        // Try to locate in the by-start block cache
        {
            SearchResult result = _cachedByBlock.get(location);
            if (result != null) {
                result._createOptions = CreateOptions.apply(result._createOptions, createOptions);
                result.verify();
                return result;
            }
        }

        // Try to locate in the per-world list of cached searches
        // If found near of it, store the position so we later don't have to
        List<SearchResult> cachedOnWorld = _cachedForWorld.computeIfAbsent(location.world,
                n -> new ArrayList<SearchResult>());
        for (SearchResult result : cachedOnWorld) {
            if (result.isNearOf(location)) {
                result._startLocations.add(location);
                _cachedByBlock.put(location, result);
                result._createOptions = CreateOptions.apply(result._createOptions, createOptions);
                result.verify();
                return result;
            }
        }

        // Start a new search
        final SearchResult result = new SearchResult(location, radius);
        _cachedByBlock.put(location, result);
        cachedOnWorld.add(result);

        // This will initiate the loading of the chunks
        result._createOptions = CreateOptions.apply(result._createOptions, createOptions);
        result.verify();

        return result;
    }

    /**
     * Checks whether, at the block specified, a real functional
     * nether portal exists.
     * 
     * @param block
     */
    private static boolean isValidNetherPortal(Block block) {
        return MaterialUtil.ISNETHERPORTAL.get(block);
    }

    public static class SearchResult {
        private final BlockLocation _start;
        private final Set<BlockLocation> _startLocations;
        private SearchStatus _status = SearchStatus.SEARCHING;
        private List<ForcedChunk> _loadingChunks = Collections.emptyList();
        private final int _searchRadius;
        private int _lastUsedTick = -1;
        private BlockLocation _result;
        private CreateOptions _createOptions;

        public SearchResult(BlockLocation start, int radius) {
            this._start = start;
            this._startLocations = new HashSet<BlockLocation>(Collections.singleton(start));
            this._searchRadius = radius;
            this._createOptions = null;
        }

        public SearchStatus getStatus() {
            return _status;
        }

        public BlockLocation getResult() {
            return (_status == SearchStatus.FOUND) ? _result : null;
        }

        /**
         * Checks whether this search result is near enough to another
         * start location to make the result reusable.
         * 
         * @param block
         * @return True if the block is near the start block of this search
         */
        public boolean isNearOf(BlockLocation block) {
            int dx = Math.abs(block.x - _start.x);
            int dy = Math.abs(block.y - _start.y);
            int dz = Math.abs(block.z - _start.z);
            IntVector3 radius = MyWorlds.portalSearchMatchRadius;
            return dx <= radius.x && dy <= radius.y && dz <= radius.z;
        }

        /**
         * Calls for all chunks kept loaded for this search result, to unload again
         */
        private void unloadChunks() {
            if (!_loadingChunks.isEmpty()) {
                for (ForcedChunk chunk : this._loadingChunks) {
                    chunk.close();
                }
                _loadingChunks = Collections.emptyList();
            }
            if (_status == SearchStatus.FOUND) {
                _status = SearchStatus.FOUND_UNLOADED;
            }
        }

        /**
         * Marks this search result as used, delaying the automatic
         * removal of not-found portals or the unloading of the chunks
         * used to find it.
         */
        private void verify() {
            this._lastUsedTick = CommonUtil.getServerTicks();

            // Restart the search if previously we did without a create
            if (_createOptions != null && _status == SearchStatus.NOT_FOUND) {
                _status = SearchStatus.SEARCHING;
            }

            // If currently found, check that there really is a portal there
            if (_status == SearchStatus.FOUND) {
                Block portalBlock = _result.getBlock();
                if (portalBlock == null) {
                    _status = SearchStatus.NOT_FOUND;
                } else if (!isValidNetherPortal(portalBlock)) {
                    _status = SearchStatus.SEARCHING;
                    whenLoaded();
                }
            }

            // If not found or problems occurred, do nothing
            if (_status == SearchStatus.NOT_FOUND || _status == SearchStatus.ERROR) {
                unloadChunks();
                return;
            }

            // If chunks aren't currently loaded, load them
            if (_loadingChunks.isEmpty()) {
                // Retrieve the World
                World world = this._start.getWorld();
                if (world == null) {
                    _status = SearchStatus.NOT_FOUND;
                    unloadChunks();
                    return;
                }

                // Start asynchronously loading all the chunks required to perform this search
                int center_cx = MathUtil.toChunk(_start.x);
                int center_cz = MathUtil.toChunk(_start.z);
                int radius = 128; //TODO: Configuration?
                int radius_chunks = MathUtil.ceil((double) radius / 16.0);
                int loaded_chunk_row = 2 * radius_chunks + 1;
                _loadingChunks = new ArrayList<ForcedChunk>(loaded_chunk_row*loaded_chunk_row);
                for (int dcx = -radius_chunks; dcx <= radius_chunks; dcx++) {
                    for (int dcz = -radius_chunks; dcz <= radius_chunks; dcz++) {
                        this._loadingChunks.add(WorldUtil.forceChunkLoaded(world,
                                center_cx + dcx, center_cz + dcz));
                    }
                }

                // Combine into one big future
                CompletableFuture<Void> future;
                future = CompletableFuture.allOf(this._loadingChunks.stream()
                        .map(f -> f.getChunkAsync()).toArray(CompletableFuture[]::new));

                // When all chunks are loaded, proceed to the next step
                future.whenComplete((ignored, error) -> {
                    // If unused, do nothing
                    if (_startLocations.isEmpty()) {
                        unloadChunks();
                        return;
                    }

                    if (error != null) {
                        MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to load chunks when locating nether portal", error);
                        _status = SearchStatus.ERROR;
                        unloadChunks();
                    } else {
                        try {
                            whenLoaded();
                        } catch (Throwable t) {
                            MyWorlds.plugin.getLogger().log(Level.WARNING, "Failed to prepare destination portal", t);
                            _status = SearchStatus.ERROR;
                            unloadChunks();
                        }
                    }
                });
            }
        }

        private void whenLoaded() {
            // When we just wanted to load the chunks and had already found the portal
            if (_status == SearchStatus.FOUND_UNLOADED) {
                _status = SearchStatus.FOUND;
                return;
            }

            // Try to find an existing portal
            Block startBlock = _start.getBlock();
            Block resultBlock = (startBlock == null) ? null : WorldUtil.findNetherPortal(startBlock, this._searchRadius);
            if (resultBlock == null && _createOptions != null) {
                resultBlock = WorldUtil.createNetherPortal(startBlock, _createOptions._orientation, _createOptions._initiator);
            }
            if (resultBlock != null) {
                _result = new BlockLocation(resultBlock);
                _status = SearchStatus.FOUND;
                return;
            }

            _status = (_createOptions != null) ? SearchStatus.NOT_CREATED : SearchStatus.NOT_FOUND;
            unloadChunks();
        }
    }

    /**
     * The status of a portal search operation
     */
    public static enum SearchStatus {
        /** Currently busy loading chunks, will search for a portal afterwards */
        SEARCHING(true, true),
        /** An error occurred while loading the chunks */
        ERROR(false, false),
        /** No portal could be found and create is false */
        NOT_FOUND(false, false),
        /** No room to create a new portal */
        NOT_CREATED(false, false),
        /** All chunks are loaded and a portal was found */
        FOUND(true, false),
        /** A portal was found before, but the chunks aren't loaded yet */
        FOUND_UNLOADED(false, true);

        private final boolean _keepChunksLoaded;
        private final boolean _busy;

        private SearchStatus(boolean keepChunksLoaded, boolean busy) {
            _keepChunksLoaded = keepChunksLoaded;
            _busy = busy;
        }

        public boolean isKeepingChunksLoaded() {
            return _keepChunksLoaded;
        }

        /**
         * Gets whether something is going on in the background, and that this
         * status will change in the future as a result of this.
         * 
         * @return True if busy
         */
        public boolean isBusy() {
            return _busy;
        }
    }

    /**
     * Defines special options when creating a portal if none
     * is available.
     */
    public static class CreateOptions {
        private Entity _initiator = null;
        private BlockFace _orientation = BlockFace.SELF;

        private CreateOptions() {
        }

        /**
         * Sets the Entity that initiated the creation of the portal.
         * Optional.
         * 
         * @param entity
         * @return this
         */
        public CreateOptions initiator(Entity entity) {
            this._initiator = entity;
            return this;
        }

        /**
         * Sets the orientation of the portal to create. This is the direction
         * the horizontal obsidian blocks are placed along. Optional.
         * 
         * @param orientation
         * @return this
         */
        public CreateOptions orientation(BlockFace orientation) {
            _orientation = orientation;
            return this;
        }

        public static CreateOptions create() {
            return new CreateOptions();
        }

        private static CreateOptions apply(CreateOptions oldOptions, CreateOptions newOptions) {
            if (oldOptions == null) {
                return newOptions;
            } else if (newOptions == null) {
                return oldOptions;
            } else {
                CreateOptions merged = create();
                if (oldOptions._initiator != null) {
                    merged._initiator = oldOptions._initiator;
                } else {
                    merged._initiator = newOptions._initiator;
                }
                if (oldOptions._orientation != BlockFace.SELF) {
                    merged._orientation = oldOptions._orientation;
                } else {
                    merged._orientation = newOptions._orientation;
                }
                return merged;
            }
        }
    }
}
