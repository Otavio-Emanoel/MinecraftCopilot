package com.minecraftcopilot.mobs;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.minecraftcopilot.world.ChunkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gerencia projéteis (ex.: onda de corte da espada).
 */
public class ProjectileManager extends BaseAppState {
    private final ChunkManager chunkManager;
    private SimpleApplication app;
    private final Node root = new Node("projectiles");
    private final List<WaveProjectile> waves = new ArrayList<>();

    public ProjectileManager(ChunkManager cm) {
        this.chunkManager = cm;
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        app.getRootNode().attachChild(root);
    }

    @Override
    protected void cleanup(Application application) {
        root.removeFromParent();
        for (WaveProjectile w : waves) w.detach();
        waves.clear();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    @Override
    public void update(float tpf) {
        Iterator<WaveProjectile> it = waves.iterator();
        var mm = getStateManager().getState(MobManager.class);
        while (it.hasNext()) {
            WaveProjectile w = it.next();
            if (w.update(tpf, app, chunkManager, mm)) {
                w.detach();
                it.remove();
            }
        }
    }

    public void spawnGetsuga(Vector3f pos, Vector3f dir, float speed, float damage, float radius) {
        // Compat: cria um arco padrão com parâmetros derivados do raio
        float outer = Math.max(0.35f, radius);
        float inner = outer * 0.55f;
        float arc = 2.6f; // ~149°
        float length = outer * 2.4f;
        WaveProjectile w = new WaveProjectile(app, pos, dir, speed, damage, outer, inner, arc, length);
        root.attachChild(w.geo);
        waves.add(w);
    }

    public void spawnGetsugaCrescent(Vector3f pos, Vector3f dir, float speed, float damage,
                                     float outerRadius, float innerRadius, float arcAngleRad, float length) {
        WaveProjectile w = new WaveProjectile(app, pos, dir, speed, damage, outerRadius, innerRadius, arcAngleRad, length);
        root.attachChild(w.geo);
        waves.add(w);
    }

    private static class WaveProjectile {
        Geometry geo;
        Vector3f pos;
        Vector3f dir;
        float speed;
        float damage;
        float outerRadius;
        float innerRadius;
        float arcAngle;
        float length;
        float life = 2.0f; // segundos máx
        boolean done = false;

        WaveProjectile(SimpleApplication app, Vector3f start, Vector3f dir, float speed, float damage,
                        float outerRadius, float innerRadius, float arcAngle, float length) {
            this.pos = start.clone();
            this.dir = dir.normalize().clone();
            this.speed = speed;
            this.damage = damage;
            this.outerRadius = outerRadius;
            this.innerRadius = Math.max(0f, Math.min(outerRadius * 0.95f, innerRadius));
            this.arcAngle = FastMath.clamp(arcAngle, 0.5f, FastMath.TWO_PI);
            this.length = Math.max(0.4f, length);

            // Visual: arco em forma de ")" (setor de anel) com brilho azul
            Mesh crescent = buildCrescentMesh(this.outerRadius, this.innerRadius, this.arcAngle, 48);
            geo = new Geometry("getsuga", crescent);
            Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            m.setColor("Color", new ColorRGBA(0.4f, 0.75f, 1f, 0.45f));
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
            m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            geo.setMaterial(m);
            geo.setQueueBucket(RenderQueue.Bucket.Transparent);
            geo.setLocalTranslation(pos);
            Quaternion q = new Quaternion();
            q.lookAt(this.dir, Vector3f.UNIT_Y);
            geo.setLocalRotation(q);
        }

        boolean update(float tpf, SimpleApplication app, ChunkManager cm, MobManager mm) {
            if (done) return true;
            float step = speed * tpf;
            Vector3f from = pos.clone();
            Vector3f to = pos.add(dir.mult(step));

            // Dano em mobs ao longo do segmento. Para parar no primeiro hit, verifique retorno >0
            if (mm != null) {
                int hits = mm.applySwordSweep(from, to, outerRadius, damage, dir);
                if (hits > 0) {
                    done = true;
                }
            }

            // Colisão com mundo: amostragem ao longo do segmento (passos ~0.15m)
            float segLen = to.subtract(from).length();
            int samples = Math.max(1, (int) FastMath.ceil(segLen / 0.15f));
            Vector3f stepVec = to.subtract(from).divide(samples);
            Vector3f p = from.clone();
            for (int i = 0; i < samples; i++) {
                p = p.add(stepVec);
                int wx = (int) FastMath.floor(p.x);
                int wy = (int) FastMath.floor(p.y);
                int wz = (int) FastMath.floor(p.z);
                if (cm.isBlockingAtWorld(wx, wy, wz)) {
                    done = true; break;
                }
            }

            // Atualiza pose e tempo de vida
            pos.set(to);
            geo.setLocalTranslation(pos);
            life -= tpf;
            // Fade gradual
            var col = (ColorRGBA) geo.getMaterial().getParam("Color").getValue();
            float a = Math.max(0f, Math.min(1f, life / 2.0f));
            geo.getMaterial().setColor("Color", new ColorRGBA(col.r, col.g, col.b, 0.2f + 0.5f * a));

            return done || life <= 0f;
        }

        void detach() {
            if (geo != null) geo.removeFromParent();
        }

        // Gera um setor de anel (crescent/arc). Eixo local Z aponta para frente (será alinhado com dir)
        private static Mesh buildCrescentMesh(float outerR, float innerR, float arcAngle, int segments) {
            segments = Math.max(8, segments);
            int verts = (segments + 1) * 2;
            int tris = segments * 2;

            float[] positions = new float[verts * 3];
            float[] colors = new float[verts * 4];
            int[] indices = new int[tris * 3];

            float start = -arcAngle * 0.5f;
            float step = arcAngle / segments;

            int vi = 0;
            int ci = 0;
            for (int i = 0; i <= segments; i++) {
                float ang = start + i * step;
                float ca = FastMath.cos(ang);
                float sa = FastMath.sin(ang);
                // Outer
                positions[vi++] = outerR * ca; // x
                positions[vi++] = outerR * sa; // y
                positions[vi++] = 0f;          // z
                // Inner
                positions[vi++] = innerR * ca;
                positions[vi++] = innerR * sa;
                positions[vi++] = 0f;

                // Cor: mais forte no meio do arco
                float t = i / (float)segments;
                float glow = 0.85f + 0.15f * FastMath.sin(t * FastMath.PI);
                for (int k = 0; k < 2; k++) { // outer+inner
                    colors[ci++] = 0.4f * glow; // r
                    colors[ci++] = 0.75f * glow; // g
                    colors[ci++] = 1.0f * glow; // b
                    colors[ci++] = 1.0f; // a - será modulado pelo material
                }
            }

            int ii = 0;
            for (int i = 0; i < segments; i++) {
                int base = i * 2;
                int next = base + 2;
                int o0 = base;
                int i0 = base + 1;
                int o1 = next;
                int i1 = next + 1;
                // Tri 1: o0, o1, i1
                indices[ii++] = o0; indices[ii++] = o1; indices[ii++] = i1;
                // Tri 2: o0, i1, i0
                indices[ii++] = o0; indices[ii++] = i1; indices[ii++] = i0;
            }

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
            mesh.setBuffer(VertexBuffer.Type.Color, 4, colors);
            mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
            mesh.updateBound();
            return mesh;
        }
    }
}
