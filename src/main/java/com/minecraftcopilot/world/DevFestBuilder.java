package com.minecraftcopilot.world;

import com.jme3.math.Vector3f;
import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;

/**
 * Constrói um letreiro "DEVFEST" no mundo, próximo ao jogador.
 */
public final class DevFestBuilder {
    private DevFestBuilder() {}

    public static void placeDevFest(ChunkManager cm, Vector3f camPos, Vector3f camDir) {
        if (cm == null || camPos == null || camDir == null) return;
        // Posição base alguns blocos à frente da câmera
        int bx = floor(camPos.x + camDir.x * 6f);
        int bz = floor(camPos.z + camDir.z * 6f);
        int by = groundYAt(cm, bx, (int)Math.floor(camPos.y) + 6, bz) + 1;

        // Texto: DEVFEST em fontes 5x7, 1 bloco de espaçamento
        String text = "DEVFEST";
        int x = bx;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            drawLetter(cm, ch, x, by, bz);
            x += 6; // 5 de largura + 1 de espaço
        }
    }

    private static void drawLetter(ChunkManager cm, char ch, int bx, int by, int bz) {
        boolean[][] m = letter(ch);
        for (int yy = 0; yy < 7; yy++) {
            for (int xx = 0; xx < 5; xx++) {
                if (m[yy][xx]) {
                    cm.setBlockAtWorld(bx + xx, by + (6 - yy), bz, BlockType.WOOD);
                }
            }
        }
    }

    // Bitmaps 5x7 (true = bloco)
    private static boolean[][] letter(char c) {
        switch (Character.toUpperCase(c)) {
            case 'D': return new boolean[][]{
                    {true,true,true,true,false},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,true,true,true,false}
            };
            case 'E': return new boolean[][]{
                    {true,true,true,true,true},
                    {true,false,false,false,false},
                    {true,true,true,false,false},
                    {true,false,false,false,false},
                    {true,true,true,true,true},
                    {true,false,false,false,false},
                    {true,true,true,true,true}
            };
            case 'V': return new boolean[][]{
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {true,false,false,false,true},
                    {false,true,false,true,false},
                    {false,true,false,true,false},
                    {false,false,true,false,false}
            };
            case 'F': return new boolean[][]{
                    {true,true,true,true,true},
                    {true,false,false,false,false},
                    {true,true,true,false,false},
                    {true,false,false,false,false},
                    {true,false,false,false,false},
                    {true,false,false,false,false},
                    {true,false,false,false,false}
            };
            case 'S': return new boolean[][]{
                    {false,true,true,true,true},
                    {true,false,false,false,false},
                    {true,false,false,false,false},
                    {false,true,true,true,false},
                    {false,false,false,false,true},
                    {false,false,false,false,true},
                    {true,true,true,true,false}
            };
            case 'T': return new boolean[][]{
                    {true,true,true,true,true},
                    {false,false,true,false,false},
                    {false,false,true,false,false},
                    {false,false,true,false,false},
                    {false,false,true,false,false},
                    {false,false,true,false,false},
                    {false,false,true,false,false}
            };
            default:
                return new boolean[7][5];
        }
    }

    private static int groundYAt(ChunkManager cm, int wx, int startY, int wz) {
        int y = Math.min(Chunk.HEIGHT - 1, Math.max(0, startY));
        for (int yy = y; yy >= 0; yy--) {
            if (cm.isBlockingAtWorld(wx, yy, wz)) return yy;
        }
        return 0;
    }

    private static int floor(float v) { return (int)Math.floor(v); }
}
