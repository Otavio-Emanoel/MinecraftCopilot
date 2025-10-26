package com.minecraftcopilot.world;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.minecraftcopilot.Chunk;

import java.util.*;

public class ChunkManager {

    public static record ChunkCoord(int x, int z) {}

    private final Node worldNode;
    private final Material chunkMaterial;
    private final int seed;
    private final int viewRadius; // em chunks

    private final Map<ChunkCoord, Geometry> loaded = new HashMap<>();

    public ChunkManager(Node worldNode, Material chunkMaterial, int seed, int viewRadius) {
        this.worldNode = worldNode;
        this.chunkMaterial = chunkMaterial;
        this.seed = seed;
        this.viewRadius = Math.max(1, viewRadius);
    }

    private static int worldToChunk(float world) {
        return (int) Math.floor(world / Chunk.SIZE);
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
        Iterator<Map.Entry<ChunkCoord, Geometry>> it = loaded.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkCoord, Geometry> e = it.next();
            if (!keep.contains(e.getKey())) {
                e.getValue().removeFromParent();
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
        loaded.put(c, geom);
    }

    public void clearAll() {
        for (Geometry g : loaded.values()) {
            g.removeFromParent();
        }
        loaded.clear();
    }
}
