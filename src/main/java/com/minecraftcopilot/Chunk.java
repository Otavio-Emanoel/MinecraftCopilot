package com.minecraftcopilot;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.minecraftcopilot.gfx.TextureAtlas;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final int cx, cz; // coordenadas do chunk no mundo
    private final byte[] blocks; // [x + SIZE * (z + SIZE * y)]
    private final byte[] meta;   // metadados por bloco (ex.: nível da água 0..7)

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.blocks = new byte[SIZE * SIZE * HEIGHT];
        this.meta = new byte[SIZE * SIZE * HEIGHT];
    }

    public static TextureAtlas ATLAS; // definido em VoxelGameState
    // Animação simples da água: alterna entre 3 tiles (7,8,9)
    private static volatile int WATER_ANIM_FRAME = 0; // 0..2
    public static void setWaterAnimFrame(int frame) {
        WATER_ANIM_FRAME = Math.max(0, Math.min(2, frame));
    }
    public static int getWaterAnimFrame() { return WATER_ANIM_FRAME; }

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

    public byte getMeta(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return 0;
        return meta[idx(x, y, z)];
    }

    public void setMeta(int x, int y, int z, int value) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        meta[idx(x, y, z)] = (byte) Math.max(0, Math.min(127, value));
    }

    public void generateTerrain(int seed) {
        // Parâmetros de terreno
        final float baseScale = 0.06f; // frequência base (maior -> mais suave)
        final int baseHeight = 18;     // nível médio do terreno
        final int baseAmp = 12;        // amplitude dos morros principais

        // Warping para variar padrões (distorção do domínio)
        final float warpScale = 0.02f; // frequência do warp
        final float warpAmp = 8.0f;    // força do warp em metros

        // Ruído de cristas (ridge) para picos mais marcados
        final float ridgeScale = 0.04f;
        final float ridgeAmp = 6.0f;

        int[][] heightMap = new int[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                float wx = (cx * SIZE + x);
                float wz = (cz * SIZE + z);

                // Domain warp: desloca coordenadas por um ruído lento
                float warpX = (Noise2D.fbm(wx * warpScale, wz * warpScale, seed + 1337, 3, 2.0f, 0.5f) * 2f - 1f) * warpAmp;
                float warpZ = (Noise2D.fbm(wx * warpScale + 57.0f, wz * warpScale - 91.0f, seed + 4242, 3, 2.0f, 0.5f) * 2f - 1f) * warpAmp;
                float xw = wx + warpX;
                float zw = wz + warpZ;

                // Base hills com FBM
                float baseNoise = Noise2D.fbm(xw * baseScale, zw * baseScale, seed, 4, 2.0f, 0.5f);
                // Ridge: |2*fbm-1| para enfatizar cristas
                float ridgeNoise = Math.abs(2f * Noise2D.fbm(xw * ridgeScale, zw * ridgeScale, seed + 911, 3, 2.0f, 0.5f) - 1f);

                float heightF = baseHeight + baseNoise * baseAmp + ridgeNoise * ridgeAmp;
                int h = Math.round(heightF);
                if (h < 1) h = 1;
                if (h >= HEIGHT) h = HEIGHT - 1;
                heightMap[x][z] = h;

                // Materiais: topo grama, subsuperfície dirt, abaixo stone
                // Se muito alto, chance de topo rochoso (sem grama)
                boolean rockyTop = (h >= baseHeight + (int) (baseAmp * 0.75f)) && (Noise2D.noise(wx * 0.1f, wz * 0.1f, seed + 7) > 0.6f);

                for (int y = 0; y <= h; y++) {
                    BlockType type;
                    if (y == h) {
                        type = rockyTop ? BlockType.STONE : BlockType.GRASS;
                    } else if (y >= h - 3) {
                        type = BlockType.DIRT;
                    } else {
                        type = BlockType.STONE;
                    }
                    set(x, y, z, type);
                }
            }
        }

        // Preencher água em depressões até um nível (lagos/"mar")
        final int WATER_LEVEL = baseHeight - 2; // nível d'água global simples
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int h = heightMap[x][z];
                if (h + 1 <= WATER_LEVEL) {
                    for (int y = h + 1; y <= Math.min(WATER_LEVEL, HEIGHT - 1); y++) {
                        set(x, y, z, BlockType.WATER);
                        setMeta(x, y, z, 0); // fonte estática
                    }
                }
            }
        }

        // Colocar árvores em alguns topos de grama (evitando água acima)
        placeTrees(seed, heightMap);
    }

    private void placeTrees(int seed, int[][] heightMap) {
        int trees = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int h = heightMap[x][z];
                if (h <= 1 || h >= HEIGHT - 8) continue; // espaço mínimo para árvore
                // precisa ser GRASS ou STONE no topo (permite pinheiros em áreas rochosas) e sem água acima
                BlockType top = get(x, h, z);
                if (top != BlockType.GRASS && top != BlockType.STONE) continue;
                if (h + 1 < HEIGHT && get(x, h + 1, z) == BlockType.WATER) continue;

                float wx = (cx * SIZE + x);
                float wz = (cz * SIZE + z);
                // probabilidade agrupada por ruído
                float prob = Noise2D.noise(wx * 0.08f, wz * 0.08f, seed + 3001);
                if (prob < 0.65f) continue; // ~35% em áreas favorecidas pelo ruído

                // Variação de tamanho da árvore
                float sizeN = Noise2D.noise(wx * 0.21f, wz * 0.21f, seed + 901); // 0..1
                int trunkH = 4 + (int) Math.floor(sizeN * 5.0f); // 4..9
                // conífera ocasional: mais alta e copa mais estreita
                boolean tallNarrow = Noise2D.noise(wx * 0.09f, wz * 0.09f, seed + 1618) > 0.7f;
                int r = (tallNarrow ? 2 : 2 + (int) Math.floor(Noise2D.noise(wx * 0.17f, wz * 0.17f, seed + 2718) * 2.5f)); // 2..3 (às vezes 4)
                r = Math.min(r, 3);

                // Permite árvores próximas à borda do chunk; copas serão "recortadas" nos limites

                // tronco
                for (int i = 1; i <= trunkH; i++) {
                    if (h + i >= HEIGHT) break;
                    set(x, h + i, z, BlockType.WOOD);
                }
                int topY = Math.min(HEIGHT - 1, h + trunkH);

                // copa: volume arredondado com possível alongamento vertical
                int yStretch = tallNarrow ? 2 : 1;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        for (int dy = -r; dy <= r + (tallNarrow ? 1 : 0); dy++) {
                            int xx = x + dx;
                            int yy = topY + dy;
                            int zz = z + dz;
                            if (yy < 0 || yy >= HEIGHT) continue;
                            if (xx < 0 || xx >= SIZE || zz < 0 || zz >= SIZE) continue;
                            // distância elipsoidal
                            float ndx = dx / (float) r;
                            float ndz = dz / (float) r;
                            float ndy = dy / (float) (r / (float) yStretch + 0.0001f);
                            float d2 = ndx * ndx + ndz * ndz + ndy * ndy;
                            if (d2 <= 1.0f) {
                                BlockType cur = get(xx, yy, zz);
                                if (cur == BlockType.AIR || cur == BlockType.GRASS) {
                                    set(xx, yy, zz, BlockType.LEAVES);
                                }
                            }
                        }
                    }
                }
                // folha extra no topo para "finalizar" o formato
                if (topY + 1 < HEIGHT) set(x, topY + 1, z, BlockType.LEAVES);
                trees++;
            }
        }
        // Fallback: se nenhum árvore foi plantada no chunk, planta uma próxima do centro (se possível)
        if (trees == 0) {
            int cxm = SIZE / 2;
            int czm = SIZE / 2;
            int h = heightMap[cxm][czm];
            if (h > 1 && h < HEIGHT - 8) {
                BlockType top = get(cxm, h, czm);
                if (top == BlockType.GRASS || top == BlockType.STONE) {
                    int trunkH = 6; // altura mediana
                    int r = 2;
                    // tronco
                    for (int i = 1; i <= trunkH && h + i < HEIGHT; i++) {
                        set(cxm, h + i, czm, BlockType.WOOD);
                    }
                    int topY = Math.min(HEIGHT - 1, h + trunkH);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            for (int dy = -r; dy <= r; dy++) {
                                int xx = cxm + dx;
                                int yy = topY + dy;
                                int zz = czm + dz;
                                if (yy < 0 || yy >= HEIGHT || xx < 0 || xx >= SIZE || zz < 0 || zz >= SIZE) continue;
                                float d2 = (dx*dx + dz*dz + dy*dy);
                                if (d2 <= (r*r + 1)) {
                                    BlockType cur = get(xx, yy, zz);
                                    if (cur == BlockType.AIR || cur == BlockType.GRASS) set(xx, yy, zz, BlockType.LEAVES);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void generateFlat(int heightY) {
        int h = Math.max(1, Math.min(HEIGHT - 1, heightY));
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
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
    List<Float> uvs = new ArrayList<>();
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
                        addFace(positions, colors, uvs, indices, x, y, z, f, t);
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

    FloatBuffer uvBuf = BufferUtils.createFloatBuffer(uvs.size());
    for (Float f : uvs) uvBuf.put(f);
    uvBuf.flip();

    IntBuffer idxBuf = BufferUtils.createIntBuffer(indices.size());
    for (Integer v : indices) idxBuf.put(v);
    idxBuf.flip();

    mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
    mesh.setBuffer(VertexBuffer.Type.Color, 4, colBuf);
    mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvBuf);
    mesh.setBuffer(VertexBuffer.Type.Index, 3, idxBuf);
        mesh.updateBound();

        Geometry geom = new Geometry("chunk-" + cx + "," + cz, mesh);
        geom.setMaterial(mat);
        geom.setLocalTranslation(new Vector3f(cx * SIZE, 0, cz * SIZE));
        return geom;
    }

    private static void addFace(List<Float> positions, List<Float> colors, List<Float> uvs, List<Integer> indices,
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
        // Água meio transparente
        if (type == BlockType.WATER) {
            c.a = 0.65f;
        }

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

        // UVs usando atlas (água usa frame animado)
        int tileIndex = type.tileForFace(face);
        if (type == BlockType.WATER) {
            // base 7 + frame (0..2)
            tileIndex = 7 + getWaterAnimFrame();
        }
        float[] uv = (ATLAS != null) ? ATLAS.getUV(tileIndex) : new float[]{0,0,1,1};
        // ordem consistente com os vértices v[0..3]
        // mapeamento retangular padrão
        uvs.add(uv[0]); uvs.add(uv[0] == uv[1] ? uv[1] : uv[1]); // dummy avoided; set below properly
        // Para clareza, definimos diretamente por face
        uvs.remove(uvs.size()-1); uvs.remove(uvs.size()-1);
        switch (face) {
            case 0,1,4,5 -> { // faces verticais: u ao longo de X/Z, v ao longo de Y
                // v[0],v[1],v[2],v[3]
                uvs.add(uv[0]); uvs.add(uv[1]); // 0
                uvs.add(uv[2]); uvs.add(uv[1]); // 1
                uvs.add(uv[2]); uvs.add(uv[3]); // 2
                uvs.add(uv[0]); uvs.add(uv[3]); // 3
            }
            case 2,3 -> { // topo/baixo: u->x, v->z
                uvs.add(uv[0]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[3]);
                uvs.add(uv[0]); uvs.add(uv[3]);
            }
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
