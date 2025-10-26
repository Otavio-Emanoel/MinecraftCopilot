package com.minecraftcopilot;

import com.jme3.math.ColorRGBA;

public enum BlockType {
    AIR(0, new ColorRGBA(0, 0, 0, 0), -1, -1, -1),
    // tileTop, tileSide, tileBottom (índices no atlas)
    GRASS(1, new ColorRGBA(0.36f, 0.6f, 0.2f, 1f), 0, 1, 2),
    DIRT(2, new ColorRGBA(0.45f, 0.30f, 0.15f, 1f), 2, 2, 2),
    STONE(3, new ColorRGBA(0.62f, 0.62f, 0.62f, 1f), 3, 3, 3);

    public final byte id;
    public final ColorRGBA color;
    public final int tileTop;
    public final int tileSide;
    public final int tileBottom;

    BlockType(int id, ColorRGBA color, int tileTop, int tileSide, int tileBottom) {
        this.id = (byte) id;
        this.color = color;
        this.tileTop = tileTop;
        this.tileSide = tileSide;
        this.tileBottom = tileBottom;
    }

    public boolean isSolid() {
        return this != AIR;
    }

    public int tileForFace(int face) {
        // face: 0 +X, 1 -X, 2 +Y (topo), 3 -Y (baixo), 4 +Z, 5 -Z
        return switch (face) {
            case 2 -> tileTop;
            case 3 -> tileBottom;
            default -> tileSide;
        };
    }

    public static BlockType fromId(byte id) {
        for (BlockType t : values()) {
            if (t.id == id) return t;
        }
        return AIR;
    }
}
