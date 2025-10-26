package com.minecraftcopilot;

import com.jme3.math.ColorRGBA;

public enum BlockType {
    AIR(0, new ColorRGBA(0, 0, 0, 0)),
    GRASS(1, new ColorRGBA(0.36f, 0.6f, 0.2f, 1f)),
    DIRT(2, new ColorRGBA(0.45f, 0.30f, 0.15f, 1f)),
    STONE(3, new ColorRGBA(0.62f, 0.62f, 0.62f, 1f));

    public final byte id;
    public final ColorRGBA color;

    BlockType(int id, ColorRGBA color) {
        this.id = (byte) id;
        this.color = color;
    }

    public boolean isSolid() {
        return this != AIR;
    }

    public static BlockType fromId(byte id) {
        for (BlockType t : values()) {
            if (t.id == id) return t;
        }
        return AIR;
    }
}
