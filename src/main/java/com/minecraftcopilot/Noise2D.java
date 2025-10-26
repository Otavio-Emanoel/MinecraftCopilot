package com.minecraftcopilot;

public final class Noise2D {
    private Noise2D() {}

    private static float smooth(float t) {
        return t * t * (3f - 2f * t); // smoothstep
    }

    private static int hash(int x, int z, int seed) {
        int h = seed;
        h ^= (x * 0x27d4eb2d);
        h = Integer.rotateLeft(h, 13);
        h ^= (z * 0x85ebca6b);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return h;
    }

    private static float rand2D(int x, int z, int seed) {
        int h = hash(x, z, seed);
        // Mapear para [0,1)
        return (h & 0x7fffffff) / (float) 0x80000000;
    }

    public static float noise(float x, float z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float sx = smooth(x - x0);
        float sz = smooth(z - z0);

        float v00 = rand2D(x0, z0, seed);
        float v10 = rand2D(x1, z0, seed);
        float v01 = rand2D(x0, z1, seed);
        float v11 = rand2D(x1, z1, seed);

        float ix0 = v00 + (v10 - v00) * sx;
        float ix1 = v01 + (v11 - v01) * sx;
        return ix0 + (ix1 - ix0) * sz;
    }

    public static float fbm(float x, float z, int seed, int octaves, float lacunarity, float gain) {
        float amp = 0.5f;
        float freq = 1.0f;
        float sum = 0f;
        float norm = 0f;
        for (int i = 0; i < octaves; i++) {
            sum += noise(x * freq, z * freq, seed + i * 1013) * amp;
            norm += amp;
            freq *= lacunarity;
            amp *= gain;
        }
        return sum / Math.max(norm, 1e-6f);
    }
}
