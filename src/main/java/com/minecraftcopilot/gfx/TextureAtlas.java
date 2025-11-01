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
        // Indices:
        // 0 grass_top, 1 grass_side, 2 dirt, 3 stone, 4 log_side, 5 log_top, 6 leaves,
        // 7 water_f0, 8 water_f1, 9 water_f2, 10 egg_item, 11 devfest_item, 12 sword_item, 13 dummy_item, 14 bow_item, 15 arrow_item
        drawGrassTop(0);
        drawGrassSide(1);
        drawDirt(2);
        drawStone(3);
        drawLogSide(4);
        drawLogTop(5);
        drawLeaves(6);
        drawWater(7, 0);
        drawWater(8, 1);
        drawWater(9, 2);
        // Item ovo (ícone)
        drawEgg(10);
        // Ícone DevFest
        drawDevFest(11);
        // Ícone da Espada
        drawSword(12);
        // Ícone do Boneco de Treino
        drawDummy(13);
        // Ícone do Arco
        drawBow(14);
        // Ícone da Flecha
        drawArrowIcon(15);
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

    private void drawLogSide(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        // base marrom do tronco
        g.setColor(new Color(102, 78, 52));
        g.fillRect(x, 0, tileSize, tileSize);
        // veios verticais
        g.setColor(new Color(84, 64, 43));
        for (int i = 0; i < tileSize; i += Math.max(1, tileSize/8)) {
            g.fillRect(x + i, 0, 1, tileSize);
        }
        // ruído suave
        Random r = new Random(5555 + idx);
        for (int i = 0; i < tileSize * tileSize / 5; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = r.nextInt(30);
            g.setColor(new Color(102 - shade/6, 78 - shade/8, 52 - shade/10));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    private void drawLogTop(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        // seção transversal com anéis
        g.setColor(new Color(160, 120, 80));
        g.fillRect(x, 0, tileSize, tileSize);
        g.setColor(new Color(140, 100, 65));
        int cx = x + tileSize/2;
        int cy = tileSize/2;
        for (int r = tileSize/2; r > 0; r -= Math.max(1, tileSize/10)) {
            g.drawOval(cx - r, cy - r, r*2, r*2);
        }
        g.dispose();
    }

    private void drawLeaves(int idx) {
        Graphics2D g = atlas.createGraphics();
        int x = idx * tileSize;
        g.setColor(new Color(70, 140, 60));
        g.fillRect(x, 0, tileSize, tileSize);
        Random r = new Random(7777 + idx);
        for (int i = 0; i < tileSize * tileSize / 2; i++) {
            int px = x + r.nextInt(tileSize);
            int py = r.nextInt(tileSize);
            int shade = r.nextInt(60);
            g.setColor(new Color(70 + shade/3, 140 + shade/4, 60 + shade/6));
            g.fillRect(px, py, 1, 1);
        }
        g.dispose();
    }

    private void drawWater(int idx, int phase) {
        // Procedural: base azul com ondas senoidais e leve gradiente vertical
        int ox = idx * tileSize;
        final float p = phase / 3.0f; // fase 0..1
        for (int yy = 0; yy < tileSize; yy++) {
            float fy = yy / (float) (tileSize - 1);
            for (int xx = 0; xx < tileSize; xx++) {
                float fx = xx / (float) (tileSize - 1);
                // Ondas com duas frequências e deslocamento por phase
                double w1 = Math.sin((fx * 7.6 + p * 1.15 + fy * 0.4) * Math.PI * 2.0);
                double w2 = Math.sin((fx * 12.3 + fy * 3.1 + p * 2.1) * Math.PI * 2.0);
                double waves = 0.5 * w1 + 0.5 * w2;
                // Gradiente vertical (mais claro no topo)
                float grad = 0.12f * (1f - fy);
                // Brilho das cristas
                float crest = (float) Math.max(0.0, waves * 0.5 + 0.5);
                // Cor base
                int br = clamp255((int) (30 + 20 * grad));
                int bg = clamp255((int) (110 + 30 * grad));
                int bb = clamp255((int) (200 + 30 * grad));
                // Realce pela crista
                br = clamp255((int) (br + crest * 20));
                bg = clamp255((int) (bg + crest * 35));
                bb = clamp255((int) (bb + crest * 45));
                int a = 255; // textura opaca; transparência controlada por vertex color
                int argb = ((a & 0xFF) << 24) | ((br & 0xFF) << 16) | ((bg & 0xFF) << 8) | (bb & 0xFF);
                atlas.setRGB(ox + xx, yy, argb);
            }
        }
    }

    private void drawEgg(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo transparente total
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(x0, 0, tileSize, tileSize);
        g.setComposite(AlphaComposite.SrcOver);
        // ovo simples: elipse amarela com contorno
        int pad = Math.max(2, tileSize/6);
        int w = tileSize - pad*2;
        int h = tileSize - pad*2;
        g.setColor(new Color(255, 235, 120));
        g.fillOval(x0 + pad, pad, w, h);
        g.setColor(new Color(200, 180, 80));
        g.setStroke(new BasicStroke(Math.max(1f, tileSize/32f)));
        g.drawOval(x0 + pad, pad, w, h);
        // brilho
        g.setColor(new Color(255, 255, 220, 170));
        g.fillOval(x0 + pad + w/3, pad + h/6, w/3, h/4);
        g.dispose();
    }

    private void drawDevFest(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo transparente
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(x0, 0, tileSize, tileSize);
        g.setComposite(AlphaComposite.SrcOver);
        // fundo sutil azul escuro
        g.setColor(new Color(20, 30, 60, 220));
        g.fillRect(x0, 0, tileSize, tileSize);
        // moldura
        g.setColor(new Color(40, 60, 110, 240));
        g.setStroke(new BasicStroke(Math.max(1f, tileSize/24f)));
        g.drawRect(x0 + 1, 1, tileSize - 2, tileSize - 2);
        // letras DF estilizadas
        g.setColor(new Color(255, 215, 0)); // dourado
        int pad = Math.max(2, tileSize/8);
        int mid = x0 + tileSize/2;
        int h = tileSize - pad*2;
        int w = (tileSize - pad*3)/2;
        // D
        g.fillRoundRect(x0 + pad, pad, w, h, pad, pad);
        g.setColor(new Color(20, 30, 60));
        g.fillRoundRect(x0 + pad + Math.max(2, w/3), pad + Math.max(2, h/6), w - Math.max(4, w/3), h - Math.max(4, h/3), pad/2, pad/2);
        // F
        g.setColor(new Color(255, 215, 0));
        int xF = mid + pad/2;
        g.fillRect(xF, pad, Math.max(2, w/5), h);
        g.fillRect(xF, pad, w, Math.max(2, h/5));
        g.fillRect(xF, pad + h/2 - Math.max(1, h/12), (int)(w*0.75), Math.max(2, h/5));
        g.dispose();
    }

    private void drawSword(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo translúcido escuro
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(15, 17, 22, 220));
        g.fillRect(x0, 0, tileSize, tileSize);
        // lâmina
        int cx = x0 + tileSize/2;
        g.setColor(new Color(220, 220, 225));
        g.fillPolygon(new int[]{cx-2, cx+6, cx+10, cx+2}, new int[]{3, 7, tileSize-5, tileSize-9}, 4);
        // fio da lâmina
        g.setColor(new Color(255, 255, 255));
        g.drawLine(cx+6, 7, cx+9, tileSize-6);
        // guarda
        g.setColor(new Color(180, 160, 50));
        g.fillRect(cx-6, tileSize-12, 12, 3);
        // cabo
        g.setColor(new Color(25, 30, 45));
        g.fillRect(cx-2, tileSize-12, 4, 10);
        // pomo
        g.setColor(new Color(200, 40, 40));
        g.fillRect(cx-2, tileSize-2, 4, 2);
        g.dispose();
    }

    private void drawDummy(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo translúcido para destacar o ícone
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(15, 17, 22, 200));
        g.fillRect(x0, 0, tileSize, tileSize);

        // Silhueta do boneco de treino (madeira)
        Color wood = new Color(140, 110, 75);
        Color dark = new Color(95, 75, 50);
        int pad = Math.max(2, tileSize/10);
        int cx = x0 + tileSize/2;

        // Tronco
        g.setColor(wood);
        g.fillRoundRect(cx - tileSize/10, pad + tileSize/6, tileSize/5, tileSize/2, 4, 4);

        // Cabeça
        g.setColor(wood);
        g.fillOval(cx - tileSize/8, pad, tileSize/4, tileSize/4);
        // Olhos simples
        g.setColor(dark);
        g.fillRect(cx - tileSize/20, pad + tileSize/12, 2, 2);
        g.fillRect(cx + tileSize/20 - 2, pad + tileSize/12, 2, 2);

        // Braços em T
        g.setColor(wood);
        g.fillRoundRect(cx - tileSize/3, pad + tileSize/3, (int)(2*tileSize/3.0), tileSize/10, 3, 3);

        // Base/suporte
        g.setColor(dark);
        g.fillRect(cx - tileSize/6, pad + (int)(tileSize*0.75), tileSize/3, tileSize/12);

        g.dispose();
    }

    private void drawBow(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo translúcido escuro
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(15, 17, 22, 210));
        g.fillRect(x0, 0, tileSize, tileSize);

        // arco estilizado (curva) + corda
        int pad = Math.max(2, tileSize / 8);
        int cx = x0 + tileSize / 2;
        int cy = tileSize / 2;

        // corpo do arco (marrom)
        g.setColor(new Color(120, 90, 55));
        int w = tileSize - pad * 2;
        int h = tileSize - pad * 2;
        // Desenha duas curvas Bezier aproximadas como segmentos
        g.setStroke(new BasicStroke(Math.max(2f, tileSize / 18f)));
        for (int i = 0; i < 3; i++) {
            int ox = (int) (pad * 0.6);
            g.drawArc(x0 + ox, pad + i, w - ox * 2, h - i * 2, 100, 280);
        }

        // corda (clara)
        g.setColor(new Color(220, 220, 220));
        g.setStroke(new BasicStroke(Math.max(1.5f, tileSize / 32f)));
        int topX = x0 + pad + (int)(w * 0.12);
        int botX = x0 + pad + (int)(w * 0.12);
        g.drawLine(topX, pad, cx + (int)(w * 0.20), cy);
        g.drawLine(cx + (int)(w * 0.20), cy, botX, pad + h);

        // destaque
        g.setColor(new Color(170, 135, 85));
        g.drawArc(x0 + pad, pad + 1, w - pad, h - 2, 100, 280);
        g.dispose();
    }

    private void drawArrowIcon(int idx) {
        int x0 = idx * tileSize;
        Graphics2D g = atlas.createGraphics();
        // fundo translúcido escuro
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(15, 17, 22, 210));
        g.fillRect(x0, 0, tileSize, tileSize);

        int pad = Math.max(2, tileSize / 8);
        int x1 = x0 + pad;
        int y1 = pad + tileSize / 3;
        int x2 = x0 + tileSize - pad;
        int y2 = pad + (int)(tileSize * 0.66);

        // haste
        g.setColor(new Color(180, 170, 150));
        g.setStroke(new BasicStroke(Math.max(2f, tileSize / 28f)));
        g.drawLine(x1, y1, x2, y2);

        // ponta
        g.setColor(new Color(230, 230, 230));
        int hx = x2;
        int hy = y2;
        Polygon tip = new Polygon(
                new int[]{hx, hx - tileSize / 8, hx - tileSize / 12},
                new int[]{hy, hy - tileSize / 16, hy + tileSize / 16}, 3);
        g.fillPolygon(tip);

        // penas
        g.setColor(new Color(200, 60, 60));
        int fx = x1;
        int fy = y1;
        g.fillPolygon(new int[]{fx, fx + tileSize / 10, fx + tileSize / 14}, new int[]{fy, fy - tileSize / 10, fy + tileSize / 10}, 3);
        g.dispose();
    }

    private static int clamp255(int v) {
        return (v < 0 ? 0 : (v > 255 ? 255 : v));
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
