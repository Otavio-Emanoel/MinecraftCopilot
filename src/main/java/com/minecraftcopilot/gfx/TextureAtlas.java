package com.minecraftcopilot.gfx;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TextureAtlas {
    private final int tileSize;
    private final int tiles;
    private final int width;
    private final int height;
    private final BufferedImage atlas;
    private final float uvInsetU;
    private final float uvInsetV;

    public TextureAtlas(int tileSize, int tiles) {
        this.tileSize = tileSize;
        this.tiles = tiles;
        this.width = tileSize * tiles;
        this.height = tileSize; // todos numa linha
        this.atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // inset de meia célula de pixel para evitar bleeding entre tiles
        this.uvInsetU = 0.5f / (float) width;
        this.uvInsetV = 0.5f / (float) height;
        buildDefaultTiles();
    }

    private void buildDefaultTiles() {
        // Indices: 0 grass_top, 1 grass_side, 2 dirt, 3 stone
        drawGrassTop(0);
        drawGrassSide(1);
        drawDirt(2);
        drawStone(3);
    }

    private void drawGrassTop(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        // base verde
        g.setColor(new Color(80, 150, 60));
        g.fillRect(x, 0, tileSize, tileSize);
        // ruído simples
        Random r = new Random(1337 + idx);
        for (int i = 0; i < tileSize * tileSize / 4; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = 10 + r.nextInt(30);
            g.setColor(new Color(80 + shade, 150 + shade/2, 60 + shade/3));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    private void drawGrassSide(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        // terra
        g.setColor(new Color(110, 75, 45));
        g.fillRect(x, 0, tileSize, tileSize);
        // faixa de grama no topo
        g.setColor(new Color(85, 160, 65));
        g.fillRect(x, 0, tileSize, Math.max(3, tileSize/5));
        // ruído
        Random r = new Random(2233 + idx);
        for (int i = 0; i < tileSize * tileSize / 4; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = r.nextInt(40);
            g.setColor(new Color(110 + shade/6, 75 + shade/8, 45));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    private void drawDirt(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        g.setColor(new Color(120, 80, 50));
        g.fillRect(x, 0, tileSize, tileSize);
        Random r = new Random(3456 + idx);
        for (int i = 0; i < tileSize * tileSize / 3; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = r.nextInt(50);
            g.setColor(new Color(120 + shade/8, 80 + shade/10, 50));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    private void drawStone(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        g.setColor(new Color(150, 150, 150));
        g.fillRect(x, 0, tileSize, tileSize);
        Random r = new Random(9876 + idx);
        for (int i = 0; i < tileSize * tileSize / 3; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = r.nextInt(60);
            g.setColor(new Color(120 + shade, 120 + shade, 120 + shade));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    public float[] getUV(int tileIndex) {
        float u0 = (tileIndex * tileSize) / (float) width + uvInsetU;
        float v0 = 0f + uvInsetV;
        float u1 = ((tileIndex + 1) * tileSize) / (float) width - uvInsetU;
        float v1 = 1f - uvInsetV;
        return new float[]{u0, v0, u1, v1};
    }

    public Texture2D buildTexture(AssetManager assetManager) {
        AWTLoader loader = new AWTLoader();
        Image img = loader.load(atlas, true);
        Texture2D tex = new Texture2D(img);
        // Visual pixelado e estável (sem mipmaps) para estilo voxel
        tex.setMagFilter(Texture2D.MagFilter.Nearest);
        tex.setMinFilter(Texture2D.MinFilter.NearestNoMipMaps);
        return tex;
    }
}
