package com.minecraftcopilot.world;

import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simulador simples de água estilo Minecraft:
 * Níveis de meta:
 * - 0: fonte (cheio)
 * - 1..7: água corrente com força decrescente (1 é próximo da fonte)
 * - 8: água caindo (cortina)
 * Regras:
 * - Se há ar abaixo, cria água caindo (8) abaixo.
 * - Espalhamento lateral: fonte (0) cria 1, corrente k cria k+1 (até 7).
 * - Água caindo (8) espalha lateralmente com nível 1.
 * - Nunca aumenta o nível existente; só substitui se o novo nível for menor (mais forte) ou se não houver água.
 */
public class WaterSimulator {
    private final ChunkManager cm;
    private final Deque<int[]> queue = new ArrayDeque<>(); // wx,wy,wz

    public WaterSimulator(ChunkManager cm) {
        this.cm = cm;
    }

    public void enqueue(int wx, int wy, int wz) {
        queue.add(new int[]{wx, wy, wz});
    }

    public void step(int budget) {
        int count = 0;
        while (count < budget && !queue.isEmpty()) {
            int[] c = queue.pollFirst();
            int wx = c[0], wy = c[1], wz = c[2];
            processCell(wx, wy, wz);
            count++;
        }
    }

    private void processCell(int wx, int wy, int wz) {
        var block = cm.getBlockAtWorld(wx, wy, wz);
        if (block != BlockType.WATER) return;
        int level = cm.getMetaAtWorld(wx, wy, wz);
        // fonte padrão em 0
        if (level < 0) level = 0;

        // 1) Tenta cair para baixo como água caindo (8)
        if (wy > 0) {
            var below = cm.getBlockAtWorld(wx, wy - 1, wz);
            if (below == BlockType.AIR || (below == BlockType.WATER && cm.getMetaAtWorld(wx, wy - 1, wz) > 1)) {
                // Setar como "cortina"
                cm.setBlockAndMetaAtWorld(wx, wy - 1, wz, BlockType.WATER, 8);
                enqueue(wx, wy - 1, wz);
                // não retorna; também pode espalhar lateralmente
            }
        }

        // 2) Espalhamento lateral
        int sideLevel;
        if (level == 8) sideLevel = 1; // cortina derrama lado com 1
        else if (level == 0) sideLevel = 1; // fonte gera 1
        else sideLevel = Math.min(7, level + 1);

        if (sideLevel <= 7) {
            trySpread(wx + 1, wy, wz, sideLevel);
            trySpread(wx - 1, wy, wz, sideLevel);
            trySpread(wx, wy, wz + 1, sideLevel);
            trySpread(wx, wy, wz - 1, sideLevel);
        }
    }

    private void trySpread(int wx, int wy, int wz, int level) {
        var b = cm.getBlockAtWorld(wx, wy, wz);
        if (b == BlockType.AIR) {
            cm.setBlockAndMetaAtWorld(wx, wy, wz, BlockType.WATER, level);
            enqueue(wx, wy, wz);
        } else if (b == BlockType.WATER) {
            int cur = cm.getMetaAtWorld(wx, wy, wz);
            // Só substitui se o novo nível for mais forte (menor valor exceto 8)
            int curEff = (cur == 8 ? 8 : cur);
            int newEff = (level == 8 ? 8 : level);
            if (curEff == 0) return; // já é fonte, não substitui
            if (newEff < curEff) {
                cm.setMetaAtWorld(wx, wy, wz, level);
                enqueue(wx, wy, wz);
            }
        }
    }
}
