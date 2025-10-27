package com.minecraftcopilot.world;

import com.minecraftcopilot.BlockType;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simulador de água inspirado no Minecraft.
 * Níveis (meta): 0=fONTE, 1..7=fluindo (força decresce), 8=queda.
 * Regras principais:
 * - Cai para baixo como 8 se houver ar abaixo; se já houver água abaixo fraca, substitui por 8.
 * - Lateral: 0 cria 1; k cria k+1 até 7; 8 cria 1 nos lados. Nível 7 não se espalha.
 * - Não substitui por nível mais fraco; fontes (0) são estáveis.
 * - Estabilização de fonte: se houver >=2 vizinhos laterais nível 0 e não houver ar abaixo, vira 0.
 * Pacing temporal: a propagação ocorre em ondas discretas (ticks); cada frame acumula tempo até um intervalo
 * fixo e processa um orçamento limitado de células. Vizinhos afetados entram na fila do próximo tick.
 */
public class WaterSimulator {
    private final ChunkManager cm;

    // Filas por "camadas" (onda atual e próxima)
    private final Deque<int[]> queueNow = new ArrayDeque<>();
    private final Deque<int[]> queueNext = new ArrayDeque<>();

    // Pacing temporal
    private float tickAccum = 0f;
    private final float tickInterval = 0.08f; // ~12.5 ticks/seg (ajustável)
    private final int perTickBudget = 64;     // máximo de células por tick (ajustável)
    private final int MAX_SIDE_LEVEL = 4;     // alcance lateral máximo (1..7). Menor que 7 para não espalhar tanto

    public WaterSimulator(ChunkManager cm) {
        this.cm = cm;
    }

    public void reset() {
        queueNow.clear();
        queueNext.clear();
        tickAccum = 0f;
    }

    public void enqueue(int wx, int wy, int wz) {
        // Eventos externos (colocar/quebrar) entram para a onda atual
        queueNow.add(new int[]{wx, wy, wz});
    }

    public void step(float tpf) {
        tickAccum += tpf;
        if (tickAccum < tickInterval) return;
        tickAccum -= tickInterval;

        int processed = 0;
        while (processed < perTickBudget && !queueNow.isEmpty()) {
            int[] c = queueNow.pollFirst();
            processCell(c[0], c[1], c[2]);
            processed++;
        }

        // Se esgotamos a onda atual, avançamos para a próxima na iteração seguinte
        if (queueNow.isEmpty() && !queueNext.isEmpty()) {
            queueNow.addAll(queueNext);
            queueNext.clear();
        }
    }

    private void scheduleNext(int wx, int wy, int wz) {
        queueNext.add(new int[]{wx, wy, wz});
    }

    private void processCell(int wx, int wy, int wz) {
        var block = cm.getBlockAtWorld(wx, wy, wz);
        if (block != BlockType.WATER) return;
        int level = cm.getMetaAtWorld(wx, wy, wz);
        if (level < 0) level = 0; // sanity

        // 1) Queda
        if (wy > 0) {
            var below = cm.getBlockAtWorld(wx, wy - 1, wz);
            if (below == BlockType.AIR) {
                cm.setBlockAndMetaAtWorld(wx, wy - 1, wz, BlockType.WATER, 8);
                scheduleNext(wx, wy - 1, wz);
            } else if (below == BlockType.WATER) {
                int blMeta = cm.getMetaAtWorld(wx, wy - 1, wz);
                if (blMeta != 0 && blMeta != 8) { // substitui corrente fraca por queda
                    cm.setMetaAtWorld(wx, wy - 1, wz, 8);
                    scheduleNext(wx, wy - 1, wz);
                }
            }
        }

        // 2) Lateral
    int sideLevel;
    if (level == 8) sideLevel = 1;
    else if (level == 0) sideLevel = 1;
    else if (level >= MAX_SIDE_LEVEL) sideLevel = -1; // não espalha além do limite
    else sideLevel = level + 1;

    // Se o nível calculado ultrapassa o limite, cancela espalhamento
    if (sideLevel > MAX_SIDE_LEVEL) sideLevel = -1;

        if (sideLevel > 0) {
            trySpread(wx + 1, wy, wz, sideLevel);
            trySpread(wx - 1, wy, wz, sideLevel);
            trySpread(wx, wy, wz + 1, sideLevel);
            trySpread(wx, wy, wz - 1, sideLevel);
        }

        // 3) Estabilização de fonte (dois vizinhos fonte laterais e suporte abaixo)
        if (level > 0 && level != 8) {
            int sources = countSideSources(wx, wy, wz);
            var below = cm.getBlockAtWorld(wx, wy - 1, wz);
            boolean supported = (below != BlockType.AIR);
            if (sources >= 2 && supported) {
                cm.setMetaAtWorld(wx, wy, wz, 0);
                scheduleNext(wx, wy, wz);
            }
        }
    }

    private int countSideSources(int wx, int wy, int wz) {
        int s = 0;
        s += isSource(wx + 1, wy, wz) ? 1 : 0;
        s += isSource(wx - 1, wy, wz) ? 1 : 0;
        s += isSource(wx, wy, wz + 1) ? 1 : 0;
        s += isSource(wx, wy, wz - 1) ? 1 : 0;
        return s;
    }

    private boolean isSource(int wx, int wy, int wz) {
        if (cm.getBlockAtWorld(wx, wy, wz) != BlockType.WATER) return false;
        return cm.getMetaAtWorld(wx, wy, wz) == 0;
    }

    private void trySpread(int wx, int wy, int wz, int level) {
        var b = cm.getBlockAtWorld(wx, wy, wz);
        if (b == BlockType.AIR) {
            cm.setBlockAndMetaAtWorld(wx, wy, wz, BlockType.WATER, level);
            scheduleNext(wx, wy, wz);
        } else if (b == BlockType.WATER) {
            int cur = cm.getMetaAtWorld(wx, wy, wz);
            int curEff = (cur == 8 ? 8 : cur);
            int newEff = (level == 8 ? 8 : level);
            if (curEff == 0) return;
            if (newEff < curEff) {
                cm.setMetaAtWorld(wx, wy, wz, level);
                scheduleNext(wx, wy, wz);
            }
        }
    }
}
