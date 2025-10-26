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
    private final WaterSimulator waterSim = new WaterSimulator(this);
    // Animação simples da água e orçamento de rebuilds para atualizar UVs
    private float waterAnimAccum = 0f;
    private int waterAnimFrame = 0;
    private final List<ChunkCoord> animKeys = new ArrayList<>();
    private int waterAnimCursor = 0;

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

    public void update(Vector3f camPos, float tpf, int genBudgetPerFrame) {
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

    // Passo da simulação de água por frame em ticks discretos
    waterSim.step(tpf);

        // Avança animação da água e reconstrói gradualmente os chunks para aplicar novo frame de UV
        waterAnimAccum += tpf;
        if (waterAnimAccum >= 0.6f) {
            waterAnimAccum = 0f;
            waterAnimFrame = (waterAnimFrame + 1) % 3;
            Chunk.setWaterAnimFrame(waterAnimFrame);
            // Prepara a lista de chunks a atualizar UVs
            animKeys.clear();
            animKeys.addAll(loaded.keySet());
            waterAnimCursor = 0;
        }
        // Rebuild orçamento pequeno por frame para aplicar o novo frame
        int animBudget = 2;
        while (animBudget > 0 && waterAnimCursor < animKeys.size()) {
            ChunkCoord k = animKeys.get(waterAnimCursor++);
            LoadedChunk lc = loaded.get(k);
            if (lc != null) {
                Geometry g = lc.chunk.buildGeometry(chunkMaterial);
                g.setQueueBucket(RenderQueue.Bucket.Transparent);
                lc.geom.removeFromParent();
                worldNode.attachChild(g);
                lc.geom = g;
            }
            animBudget--;
        }
    }

    private void generateChunk(ChunkCoord c) {
        Chunk chunk = new Chunk(c.x, c.z);
        // Terreno procedural com morros usando seed
        chunk.generateTerrain(seed);
    Geometry geom = chunk.buildGeometry(chunkMaterial);
    geom.setQueueBucket(RenderQueue.Bucket.Transparent);
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
        // Bucket transparente para permitir alpha nos vértices de água
        newGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
        lc.geom.removeFromParent();
        worldNode.attachChild(newGeom);
        lc.geom = newGeom;

        // Se o voxel alterado estiver na borda do chunk, reconstruir vizinhos para expor/ocultar faces adjacentes
        if (lx == 0) rebuildNeighbor(cx - 1, cz);
        if (lx == Chunk.SIZE - 1) rebuildNeighbor(cx + 1, cz);
        if (lz == 0) rebuildNeighbor(cx, cz - 1);
        if (lz == Chunk.SIZE - 1) rebuildNeighbor(cx, cz + 1);
        return true;
    }

    private void rebuildNeighbor(int ncx, int ncz) {
        LoadedChunk nlc = loaded.get(new ChunkCoord(ncx, ncz));
        if (nlc == null) return;
    Geometry g = nlc.chunk.buildGeometry(chunkMaterial);
    g.setQueueBucket(RenderQueue.Bucket.Transparent);
        nlc.geom.removeFromParent();
        worldNode.attachChild(g);
        nlc.geom = g;
    }

    public boolean isSolidAtWorld(int wx, int wy, int wz) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return false;
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        LoadedChunk lc = loaded.get(new ChunkCoord(cx, cz));
        if (lc == null) return false; // tratar não carregado como vazio
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return false;
        return lc.chunk.get(lx, wy, lz).isSolid();
    }

    // Para colisão: considera água como não bloqueante
    public boolean isBlockingAtWorld(int wx, int wy, int wz) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return false;
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        LoadedChunk lc = loaded.get(new ChunkCoord(cx, cz));
        if (lc == null) return false;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return false;
        return lc.chunk.get(lx, wy, lz).isBlocking();
    }

    // --- Métodos utilitários de acesso global a blocos/meta ---
    public BlockType getBlockAtWorld(int wx, int wy, int wz) {
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        LoadedChunk lc = loaded.get(new ChunkCoord(cx, cz));
        if (lc == null) return BlockType.AIR;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (wy < 0 || wy >= Chunk.HEIGHT || lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return BlockType.AIR;
        return lc.chunk.get(lx, wy, lz);
    }

    public int getMetaAtWorld(int wx, int wy, int wz) {
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        LoadedChunk lc = loaded.get(new ChunkCoord(cx, cz));
        if (lc == null) return 0;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (wy < 0 || wy >= Chunk.HEIGHT || lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return 0;
        return lc.chunk.getMeta(lx, wy, lz);
    }

    public void setMetaAtWorld(int wx, int wy, int wz, int value) {
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        LoadedChunk lc = loaded.get(new ChunkCoord(cx, cz));
        if (lc == null) return;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (wy < 0 || wy >= Chunk.HEIGHT || lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return;
        lc.chunk.setMeta(lx, wy, lz, value);
    }

    public void setBlockAndMetaAtWorld(int wx, int wy, int wz, BlockType type, int meta) {
        int cx = worldToChunk(wx);
        int cz = worldToChunk(wz);
        ChunkCoord key = new ChunkCoord(cx, cz);
        LoadedChunk lc = loaded.get(key);
        if (lc == null) return;
        int lx = wx - cx * Chunk.SIZE;
        int lz = wz - cz * Chunk.SIZE;
        if (wy < 0 || wy >= Chunk.HEIGHT || lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) return;
        lc.chunk.set(lx, wy, lz, type);
        lc.chunk.setMeta(lx, wy, lz, meta);
        // Rebuild local chunk mesh
    Geometry newGeom = lc.chunk.buildGeometry(chunkMaterial);
    newGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
        lc.geom.removeFromParent();
        worldNode.attachChild(newGeom);
        lc.geom = newGeom;
        // Rebuild neighbors if na borda
        if (lx == 0) rebuildNeighbor(cx - 1, cz);
        if (lx == Chunk.SIZE - 1) rebuildNeighbor(cx + 1, cz);
        if (lz == 0) rebuildNeighbor(cx, cz - 1);
        if (lz == Chunk.SIZE - 1) rebuildNeighbor(cx, cz + 1);
    }

    public void enqueueWaterUpdate(int wx, int wy, int wz) {
        waterSim.enqueue(wx, wy, wz);
    }
}
