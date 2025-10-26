package com.minecraftcopilot;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final int cx, cz; // coordenadas do chunk no mundo
    private final byte[] blocks; // [x + SIZE * (z + SIZE * y)]

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.blocks = new byte[SIZE * SIZE * HEIGHT];
    }

    private static int idx(int x, int y, int z) {
        return x + SIZE * (z + SIZE * y);
    }

    public BlockType get(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return BlockType.AIR;
        return BlockType.fromId(blocks[idx(x, y, z)]);
    }

    public void set(int x, int y, int z, BlockType type) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        blocks[idx(x, y, z)] = type.id;
    }

    public void generateTerrain(int seed) {
        float scale = 0.06f; // maior = mais suave
        int base = 18;
        int amp = 12;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                float wx = (cx * SIZE + x);
                float wz = (cz * SIZE + z);
                float hNoise = Noise2D.fbm(wx * scale, wz * scale, seed, 4, 2.0f, 0.5f);
                int h = base + Math.round(hNoise * amp);
                if (h < 1) h = 1;
                if (h >= HEIGHT) h = HEIGHT - 1;

                for (int y = 0; y <= h; y++) {
                    BlockType type;
                    if (y == h) type = BlockType.GRASS;
                    else if (y >= h - 3) type = BlockType.DIRT;
                    else type = BlockType.STONE;
                    set(x, y, z, type);
                }
            }
        }
    }

    public Geometry buildGeometry(Material mat) {
        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Direções: +X, -X, +Y, -Y, +Z, -Z
        final int[][] DIRS = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1}
        };

        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    BlockType t = get(x, y, z);
                    if (!t.isSolid()) continue;

                    for (int f = 0; f < 6; f++) {
                        int nx = x + DIRS[f][0];
                        int ny = y + DIRS[f][1];
                        int nz = z + DIRS[f][2];
                        BlockType n = get(nx, ny, nz);
                        if (n.isSolid()) continue; // face interna
                        addFace(positions, colors, indices, x, y, z, f, t);
                    }
                }
            }
        }

    Mesh mesh = new Mesh();
    // jME/LWJGL2 exigem NIO buffers diretos
    FloatBuffer posBuf = BufferUtils.createFloatBuffer(positions.size());
    for (Float f : positions) posBuf.put(f);
    posBuf.flip();

    FloatBuffer colBuf = BufferUtils.createFloatBuffer(colors.size());
    for (Float f : colors) colBuf.put(f);
    colBuf.flip();

    IntBuffer idxBuf = BufferUtils.createIntBuffer(indices.size());
    for (Integer v : indices) idxBuf.put(v);
    idxBuf.flip();

    mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
    mesh.setBuffer(VertexBuffer.Type.Color, 4, colBuf);
    mesh.setBuffer(VertexBuffer.Type.Index, 3, idxBuf);
        mesh.updateBound();

        Geometry geom = new Geometry("chunk-" + cx + "," + cz, mesh);
        geom.setMaterial(mat);
        geom.setLocalTranslation(new Vector3f(cx * SIZE, 0, cz * SIZE));
        return geom;
    }

    private static void addFace(List<Float> positions, List<Float> colors, List<Integer> indices,
                                int x, int y, int z, int face, BlockType type) {
        // Define vértices da face com base no eixo
        float[][] v = new float[4][3];
        switch (face) {
            // +X
            case 0 -> {
                v[0] = new float[]{x + 1, y, z};
                v[1] = new float[]{x + 1, y, z + 1};
                v[2] = new float[]{x + 1, y + 1, z + 1};
                v[3] = new float[]{x + 1, y + 1, z};
            }
            // -X
            case 1 -> {
                v[0] = new float[]{x, y, z + 1};
                v[1] = new float[]{x, y, z};
                v[2] = new float[]{x, y + 1, z};
                v[3] = new float[]{x, y + 1, z + 1};
            }
            // +Y (topo)
            case 2 -> {
                v[0] = new float[]{x, y + 1, z};
                v[1] = new float[]{x + 1, y + 1, z};
                v[2] = new float[]{x + 1, y + 1, z + 1};
                v[3] = new float[]{x, y + 1, z + 1};
            }
            // -Y (baixo)
            case 3 -> {
                v[0] = new float[]{x, y, z + 1};
                v[1] = new float[]{x + 1, y, z + 1};
                v[2] = new float[]{x + 1, y, z};
                v[3] = new float[]{x, y, z};
            }
            // +Z
            case 4 -> {
                v[0] = new float[]{x + 1, y, z + 1};
                v[1] = new float[]{x, y, z + 1};
                v[2] = new float[]{x, y + 1, z + 1};
                v[3] = new float[]{x + 1, y + 1, z + 1};
            }
            // -Z
            case 5 -> {
                v[0] = new float[]{x, y, z};
                v[1] = new float[]{x + 1, y, z};
                v[2] = new float[]{x + 1, y + 1, z};
                v[3] = new float[]{x, y + 1, z};
            }
        }

        float shade;
        switch (face) {
            case 2 -> shade = 1.00f; // topo
            case 0, 1 -> shade = 0.80f; // lados X
            case 4, 5 -> shade = 0.90f; // lados Z
            default -> shade = 0.70f; // baixo
        }
        ColorRGBA c = type.color.clone();
        c.r *= shade; c.g *= shade; c.b *= shade;

        int base = positions.size() / 3;
        for (int i = 0; i < 4; i++) {
            positions.add(v[i][0]);
            positions.add(v[i][1]);
            positions.add(v[i][2]);
            colors.add(c.r);
            colors.add(c.g);
            colors.add(c.b);
            colors.add(c.a);
        }

        // Triângulos (CCW)
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base);
        indices.add(base + 2);
        indices.add(base + 3);
    }
}
