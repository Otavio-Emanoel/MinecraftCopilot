package com.minecraftcopilot.mobs;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.minecraftcopilot.world.ChunkManager;
import com.minecraftcopilot.Chunk;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MobManager extends BaseAppState {
    private final ChunkManager chunkManager;
    private SimpleApplication app;
    private final Node mobRoot = new Node("mobs");
    private final List<SimpleMob> mobs = new ArrayList<>();
    private final Random rng = new Random(12345);
    private final List<EggEntity> eggs = new ArrayList<>();

    public MobManager(ChunkManager cm) {
        this.chunkManager = cm;
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        app.getRootNode().attachChild(mobRoot);
    }

    @Override
    public void update(float tpf) {
        // Atualiza IA e física simples
        // Ovos
        Iterator<EggEntity> eit = eggs.iterator();
        while (eit.hasNext()) {
            EggEntity e = eit.next();
            if (e.updateEgg(tpf)) {
                Vector3f p = e.getWorldTranslation().clone();
                // Chocar
                spawnChicken(p);
                spawnHatchBurst(p);
                e.removeFromParent();
                eit.remove();
            }
        }

        Iterator<SimpleMob> it = mobs.iterator();
        while (it.hasNext()) {
            SimpleMob m = it.next();
            if (!m.isAlive()) {
                m.removeFromParent();
                it.remove();
                continue;
            }
            m.update(tpf, chunkManager);
        }

        // Atualiza partículas de eclosão
        Iterator<ParticleBurst> bit = bursts.iterator();
        while (bit.hasNext()) {
            ParticleBurst b = bit.next();
            if (b.update(tpf)) {
                b.node.removeFromParent();
                bit.remove();
            }
        }
    }

    @Override
    protected void cleanup(Application application) {
        mobRoot.removeFromParent();
        for (SimpleMob m : mobs) m.removeFromParent();
        mobs.clear();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    public void spawnChicken(Vector3f worldPos) {
        // Ajusta para o chão
        Vector3f p = worldPos.clone();
        p.y = groundYAt(p.x, p.z, p.y) + 1.001f;
        ChickenMob c = new ChickenMob(app.getAssetManager());
        c.setLocalTranslation(p);
        mobRoot.attachChild(c);
        mobs.add(c);
        // "Explosão" leve: impulso para cima e partículas simples poderiam ser adicionados futuramente
        c.kick(new Vector3f((rng.nextFloat()-0.5f)*1.5f, 3f, (rng.nextFloat()-0.5f)*1.5f));
    }

    public void spawnEgg(Vector3f worldPos) {
        Vector3f p = worldPos.clone();
        EggEntity e = new EggEntity(app.getAssetManager(), 2.0f);
        // Assentar a base do ovo exatamente sobre o topo do bloco
        float ground = groundYAt(p.x, p.z, p.y);
        float vRadius = e.getVerticalRadius();
        p.y = ground + 1.0f + vRadius + 0.001f;
        e.setLocalTranslation(p);
        mobRoot.attachChild(e);
        eggs.add(e);
    }

    // Partículas simples de "casca" estourando (sem assets externos)
    private final List<ParticleBurst> bursts = new ArrayList<>();

    private void spawnHatchBurst(Vector3f p) {
        ParticleBurst b = new ParticleBurst(p);
        mobRoot.attachChild(b.node);
        bursts.add(b);
    }

    private static class BurstParticle {
        Geometry g;
        Vector3f v = new Vector3f();
        float life;
    }

    private class ParticleBurst {
        final Node node = new Node("hatchBurst");
        final List<BurstParticle> parts = new ArrayList<>();
        float time = 0f;
        final float duration = 0.5f;

        ParticleBurst(Vector3f origin) {
            Random r = rng;
            for (int i = 0; i < 12; i++) {
                // pequenos "fragmentos" coloridos
                Geometry g = new Geometry("frag", new com.jme3.scene.shape.Box(0.02f, 0.01f, 0.02f));
                Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                // alterna entre amarelo-claro e branco
                if (i % 2 == 0) m.setColor("Color", new com.jme3.math.ColorRGBA(1f, 0.95f, 0.6f, 0.9f));
                else m.setColor("Color", new com.jme3.math.ColorRGBA(1f, 1f, 1f, 0.9f));
                m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                g.setMaterial(m);
                g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
                g.setLocalTranslation(origin.x, origin.y + 0.1f, origin.z);

                BurstParticle bp = new BurstParticle();
                bp.g = g;
                // velocidade radial + impulso para cima
                bp.v.set((r.nextFloat()-0.5f)*3f, 2.5f + r.nextFloat()*1.5f, (r.nextFloat()-0.5f)*3f);
                bp.life = 0.3f + r.nextFloat()*0.3f;
                parts.add(bp);
                node.attachChild(g);
            }
        }

        boolean update(float tpf) {
            time += tpf;
            for (BurstParticle bp : parts) {
                // simples física aérea
                bp.v.y -= 9.8f * tpf;
                bp.g.move(bp.v.x * tpf, bp.v.y * tpf, bp.v.z * tpf);
                bp.life -= tpf;
                // fade out
                Material m = bp.g.getMaterial();
                com.jme3.math.ColorRGBA c = (com.jme3.math.ColorRGBA) m.getParam("Color").getValue();
                float a = Math.max(0f, Math.min(1f, bp.life / 0.3f));
                m.setColor("Color", new com.jme3.math.ColorRGBA(c.r, c.g, c.b, a));
            }
            return time >= duration;
        }
    }

    private float groundYAt(float x, float z, float startY) {
        int wx = (int)Math.floor(x);
        int wz = (int)Math.floor(z);
        int wy = Math.min(Chunk.HEIGHT - 1, (int)Math.floor(startY + 3));
        for (int y = wy; y >= 0; y--) {
            if (chunkManager.isBlockingAtWorld(wx, y, wz)) return y;
        }
        return 0f;
    }
}
