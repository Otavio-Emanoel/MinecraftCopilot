package com.minecraftcopilot.world;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.renderer.queue.RenderQueue;
import com.minecraftcopilot.Chunk;
import com.minecraftcopilot.BlockType;

import java.util.*;

public class ChunkManager {

    public static record ChunkCoord(int x, int z) {}

    private final Node worldNode;
    private final Material chunkMaterial;
    private final int seed;
    private final int viewRadius; // em chunks

    private static class LoadedChunk {
        final Chunk chunk;
        Geometry geom;
        LoadedChunk(Chunk c, Geometry g) { this.chunk = c; this.geom = g; }
    }

    private final Map<ChunkCoord, LoadedChunk> loaded = new HashMap<>();

    public ChunkManager(Node worldNode, Material chunkMaterial, int seed, int viewRadius) {
        this.worldNode = worldNode;
        this.chunkMaterial = chunkMaterial;
        this.seed = seed;
        this.viewRadius = Math.max(1, viewRadius);
    }

    private static int worldToChunk(float world) {
        return (int) Math.floor(world / Chunk.SIZE);
    }

    private static int worldToChunk(int world) {
        return Math.floorDiv(world, Chunk.SIZE);
    }

    private static Set<ChunkCoord> computeNeededAround(int ccx, int ccz, int radius) {
        Set<ChunkCoord> needed = new HashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = ccx + dx;
                int z = ccz + dz;
                needed.add(new ChunkCoord(x, z));
            }
        }
        return needed;
    }

    public void update(Vector3f camPos, int genBudgetPerFrame) {
        int ccx = worldToChunk(camPos.x);
        int ccz = worldToChunk(camPos.z);

        Set<ChunkCoord> needed = computeNeededAround(ccx, ccz, viewRadius);

        // Descarregar os que estão fora de alcance (com uma margem)
        int unloadRadius = viewRadius + 1;
        Set<ChunkCoord> keep = computeNeededAround(ccx, ccz, unloadRadius);
        Iterator<Map.Entry<ChunkCoord, LoadedChunk>> it = loaded.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkCoord, LoadedChunk> e = it.next();
            if (!keep.contains(e.getKey())) {
                e.getValue().geom.removeFromParent();
                it.remove();
            }
        }

        // Gerar alguns por frame para evitar travas
        int budget = genBudgetPerFrame;
        for (ChunkCoord c : needed) {
            if (budget <= 0) break;
            if (!loaded.containsKey(c)) {
                generateChunk(c);
                budget--;
            }
        }
    }

    private void generateChunk(ChunkCoord c) {
        Chunk chunk = new Chunk(c.x, c.z);
        // Por enquanto: chão plano
        chunk.generateFlat(18);
        Geometry geom = chunk.buildGeometry(chunkMaterial);
        worldNode.attachChild(geom);
        loaded.put(c, new LoadedChunk(chunk, geom));

    }

    public void clearAll() {
        for (LoadedChunk lc : loaded.values()) {
            lc.geom.removeFromParent();
        }
        loaded.clear();
    }

    public boolean setBlockAtWorld(int wx, int wy, int wz, BlockType type) {
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        ChunkCoord key = new ChunkCoord(cx, cz);
        LoadedChunk lc = loaded.get(key);
        if (lc == null) return false;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (wy < 0 || wy >= Chunk.HEIGHT || lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return false;
        lc.chunk.set(lx, wy, lz, type);
        // Reconstroi apenas este chunk
    Geometry newGeom = lc.chunk.buildGeometry(chunkMaterial);
    // Preserva a ordem/bucket
    newGeom.setQueueBucket(RenderQueue.Bucket.Opaque);
        lc.geom.removeFromParent();
        worldNode.attachChild(newGeom);
        lc.geom = newGeom;
        return true;
    }
}
