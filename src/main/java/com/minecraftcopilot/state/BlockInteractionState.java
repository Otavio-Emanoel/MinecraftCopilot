package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Box;
import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;
import com.minecraftcopilot.world.ChunkManager;
import com.minecraftcopilot.ui.HotbarState;
import com.minecraftcopilot.mobs.MobManager;
import com.minecraftcopilot.world.DevFestBuilder;

public class BlockInteractionState extends BaseAppState {

    private static final String MAP_BREAK = "BI_Break";

    private SimpleApplication app;
    private final Node worldNode;
    private final ChunkManager chunkManager;
    private final HotbarState hotbar;

    // highlight
    private Geometry outline;
    private int selWx, selWy, selWz;
    private boolean hasSelection = false;
    private Vector3f lastHitContact;
    private Vector3f lastRayDir;
    // AABB do jogador (deve bater com PlayerController)
    private static final float PC_HALF_WIDTH = 0.3f;
    private static final float PC_EYE_HEIGHT = 1.90f;
    private static final float PC_BODY_HEIGHT = 1.90f;

    private ActionListener action;

    // Partículas simples de detrito ao quebrar bloco
    private static class Debris {
        Geometry geo; Vector3f vel; float life; float maxLife;
        Debris(Geometry g, Vector3f v, float l){ this.geo=g; this.vel=v; this.life=l; this.maxLife=l; }
    }
    private final java.util.List<Debris> debrisList = new java.util.ArrayList<>();

    // Hold-to-repeat (quebrar/colocar)
    private boolean breakHeld = false;
    private boolean placeHeld = false;
    private float breakRepeatTimer = 0f;
    private float placeRepeatTimer = 0f;
    private static final float BREAK_REPEAT_PERIOD = 0.12f; // segundos entre ações
    private static final float PLACE_REPEAT_PERIOD = 0.10f;

    public BlockInteractionState(Node worldNode, ChunkManager chunkManager, HotbarState hotbar) {
        this.worldNode = worldNode;
        this.chunkManager = chunkManager;
        this.hotbar = hotbar;
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        app.getInputManager().addMapping(MAP_BREAK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("BI_Place", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        // Define o listener agora que a instância já foi construída (chunkManager está pronto)
        this.action = (name, isPressed, tpf) -> {
            if (MAP_BREAK.equals(name)) {
                breakHeld = isPressed;
                if (isPressed && hasSelection) {
                    // ação imediata
                    performBreakAtSelection();
                    breakRepeatTimer = BREAK_REPEAT_PERIOD;
                }
            } else if ("BI_Place".equals(name)) {
                placeHeld = isPressed;
                if (isPressed && hasSelection && hotbar != null) {
                    performPlaceAtSelection();
                    placeRepeatTimer = PLACE_REPEAT_PERIOD;
                }
            }
        };
        app.getInputManager().addListener(action, MAP_BREAK, "BI_Place");
    }

    @Override
    public void update(float tpf) {
        // Ray a partir da câmera
    Vector3f origin = app.getCamera().getLocation();
    Vector3f dir = app.getCamera().getDirection().normalize();
        Ray ray = new Ray(origin, dir);
        ray.setLimit(6f); // alcance de mineração curto

        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);
        if (results.size() == 0) {
            clearSelection();
            return;
        }
        CollisionResult hit = results.getClosestCollision();
        if (hit == null) { clearSelection(); return; }

        Geometry geom = hit.getGeometry();
        if (geom == null) { clearSelection(); return; }

    Vector3f contact = hit.getContactPoint();
    // Usar a direção do raio para decidir o lado: avançar levemente "para dentro" do bloco atingido
    // Isso evita depender do sentido do normal do triângulo (que pode estar invertido em algumas faces)
    Vector3f chunkOrigin = geom.getWorldTranslation();
    Vector3f local = contact.subtract(chunkOrigin).add(dir.mult(1e-3f));

        int lx = (int) Math.floor(local.x);
        int ly = (int) Math.floor(local.y);
        int lz = (int) Math.floor(local.z);

        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE || ly < 0 || ly >= Chunk.HEIGHT) {
            clearSelection();
            return;
        }

        int wx = (int) (chunkOrigin.x + lx);
        int wy = ly;
        int wz = (int) (chunkOrigin.z + lz);

        setSelection(wx, wy, wz);
        lastHitContact = contact;
        lastRayDir = dir;

        // Atualiza partículas de detrito
        updateDebris(tpf);

        // Repetição contínua se segurando botões
        if (breakHeld) {
            breakRepeatTimer -= tpf;
            if (breakRepeatTimer <= 0f) {
                if (hasSelection) performBreakAtSelection();
                breakRepeatTimer = BREAK_REPEAT_PERIOD;
            }
        }
        if (placeHeld) {
            placeRepeatTimer -= tpf;
            if (placeRepeatTimer <= 0f) {
                if (hasSelection && hotbar != null) performPlaceAtSelection();
                placeRepeatTimer = PLACE_REPEAT_PERIOD;
            }
        }
    }

    private void setSelection(int wx, int wy, int wz) {
        this.selWx = wx; this.selWy = wy; this.selWz = wz;
        this.hasSelection = true;
        if (outline == null) {
            outline = new Geometry("block-outline", new WireBox(0.5f, 0.5f, 0.5f));
            Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            m.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.9f));
            outline.setMaterial(m);
            outline.setQueueBucket(RenderQueue.Bucket.Transparent);
            app.getRootNode().attachChild(outline);
        }
        outline.setLocalTranslation(new Vector3f(wx + 0.5f, wy + 0.5f, wz + 0.5f));
    }

    private void clearSelection() {
        hasSelection = false;
        if (outline != null) outline.removeFromParent();
        outline = null;
        lastHitContact = null;
        lastRayDir = null;
    }

    private boolean wouldIntersectPlayer(int bx, int by, int bz) {
        // AABB do bloco
        float bMinX = bx, bMaxX = bx + 1f;
        float bMinY = by, bMaxY = by + 1f;
        float bMinZ = bz, bMaxZ = bz + 1f;
        // AABB do jogador (a partir da câmera)
        Vector3f camPos = app.getCamera().getLocation();
        float pMinX = camPos.x - PC_HALF_WIDTH;
        float pMaxX = camPos.x + PC_HALF_WIDTH;
        float pMinY = camPos.y - PC_EYE_HEIGHT;
        float pMaxY = pMinY + PC_BODY_HEIGHT;
        float pMinZ = camPos.z - PC_HALF_WIDTH;
        float pMaxZ = camPos.z + PC_HALF_WIDTH;
        boolean overlapX = pMinX < bMaxX && pMaxX > bMinX;
        boolean overlapY = pMinY < bMaxY && pMaxY > bMinY;
        boolean overlapZ = pMinZ < bMaxZ && pMaxZ > bMinZ;
        return overlapX && overlapY && overlapZ;
    }

    @Override
    protected void cleanup(Application application) {
        if (app != null && app.getInputManager() != null) {
            if (app.getInputManager().hasMapping(MAP_BREAK)) app.getInputManager().deleteMapping(MAP_BREAK);
            if (app.getInputManager().hasMapping("BI_Place")) app.getInputManager().deleteMapping("BI_Place");
            app.getInputManager().removeListener(action);
        }
        clearSelection();
        // Limpa partículas remanescentes
        for (Debris d : debrisList) if (d.geo != null) d.geo.removeFromParent();
        debrisList.clear();
        breakHeld = false; placeHeld = false;
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    // --- Partículas de quebra ---
    private void emitBreakParticles(int wx, int wy, int wz, BlockType type, Vector3f rayDir) {
        if (type == null || type == BlockType.AIR) return;
        // Cores derivadas do bloco
        ColorRGBA base = type.color != null ? type.color.clone() : new ColorRGBA(0.8f,0.8f,0.8f,1f);
        int count = 18; // quantidade moderada
        Vector3f center = new Vector3f(wx + 0.5f, wy + 0.5f, wz + 0.5f);
        for (int i = 0; i < count; i++) {
            float sx = (FastMath.nextRandomFloat() - 0.5f);
            float sy = (FastMath.nextRandomFloat() - 0.5f);
            float sz = (FastMath.nextRandomFloat() - 0.5f);
            Vector3f local = new Vector3f(sx, sy, sz).multLocal(0.5f);
            // tamanho do fragmento
            float s = 0.04f + 0.04f * FastMath.nextRandomFloat();
            Geometry g = new Geometry("debris", new Box(s, s, s));
            Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            // leve variação de cor
            float v = 0.85f + 0.15f * FastMath.nextRandomFloat();
            m.setColor("Color", new ColorRGBA(base.r * v, base.g * v, base.b * v, 0.95f));
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            g.setMaterial(m);
            g.setQueueBucket(RenderQueue.Bucket.Transparent);
            g.setLocalTranslation(center.add(local));
            app.getRootNode().attachChild(g);

            // velocidade inicial: empurrar levemente para fora do bloco + aleatório
            Vector3f out = rayDir.negate().normalizeLocal().multLocal(1.3f + 0.6f * FastMath.nextRandomFloat());
            Vector3f rand = new Vector3f(
                    (FastMath.nextRandomFloat() - 0.5f),
                    (FastMath.nextRandomFloat()),
                    (FastMath.nextRandomFloat() - 0.5f)
            ).multLocal(1.1f);
            Vector3f vel = out.addLocal(rand).multLocal(2.0f * (0.6f + 0.8f * FastMath.nextRandomFloat()));
            float life = 0.6f + 0.5f * FastMath.nextRandomFloat();
            debrisList.add(new Debris(g, vel, life));
        }
    }

    private void updateDebris(float tpf) {
        if (debrisList.isEmpty()) return;
        for (int i = debrisList.size() - 1; i >= 0; i--) {
            Debris d = debrisList.get(i);
            d.life -= tpf;
            // física simples: gravidade e amortecimento
            d.vel.y -= 9.8f * 0.9f * tpf;
            d.vel.multLocal(1.0f - 0.8f * tpf); // arrasto leve
            // integra posição
            d.geo.move(d.vel.mult(tpf));
            // fade e encolher
            float a = Math.max(0f, Math.min(1f, d.life / d.maxLife));
            var col = (ColorRGBA) d.geo.getMaterial().getParam("Color").getValue();
            d.geo.getMaterial().setColor("Color", new ColorRGBA(col.r, col.g, col.b, 0.2f + 0.75f * a));
            d.geo.setLocalScale(0.8f + 0.4f * a);
            if (d.life <= 0f) {
                d.geo.removeFromParent();
                debrisList.remove(i);
            }
        }
    }

    // --- Ações auxiliares ---
    private void performBreakAtSelection() {
        if (!hasSelection) return;
        // Se item selecionado é espada ou arco, não quebrar blocos (usa lógica própria)
        if (hotbar != null) {
            BlockType sel = hotbar.getSelectedBlock();
            if (sel == BlockType.SWORD || sel == BlockType.SWORD2 || sel == BlockType.BOW) return;
        }

        // emite partículas e remove bloco
        BlockType broken = chunkManager.getBlockAtWorld(selWx, selWy, selWz);
        emitBreakParticles(selWx, selWy, selWz, broken, lastRayDir != null ? lastRayDir : new Vector3f(0,1,0));
        chunkManager.setBlockAtWorld(selWx, selWy, selWz, BlockType.AIR);
        // água ao redor
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    chunkManager.enqueueWaterUpdate(selWx + dx, selWy + dy, selWz + dz);
        // Reavaliar seleção
        hasSelection = false;
        if (outline != null) outline.removeFromParent();
        outline = null;
        if (hotbar != null) hotbar.triggerBreakSwing();
    }

    private void performPlaceAtSelection() {
        if (!hasSelection || hotbar == null) return;
        BlockType toPlace = hotbar.getSelectedBlock();
        if (toPlace == null || toPlace == BlockType.AIR || lastHitContact == null || lastRayDir == null) return;
    if (toPlace == BlockType.SWORD || toPlace == BlockType.SWORD2 || toPlace == BlockType.BOW || toPlace == BlockType.ARROW) return; // não coloca itens de arma/munição

        Vector3f outside = lastHitContact.subtract(lastRayDir.mult(1e-3f));
        int pwx = (int) Math.floor(outside.x);
        int pwy = (int) Math.floor(outside.y);
        int pwz = (int) Math.floor(outside.z);
        if (pwx == selWx && pwy == selWy && pwz == selWz) {
            Vector3f d = lastRayDir;
            if (Math.abs(d.x) >= Math.abs(d.y) && Math.abs(d.x) >= Math.abs(d.z)) {
                pwx += (d.x > 0 ? -1 : 1);
            } else if (Math.abs(d.y) >= Math.abs(d.x) && Math.abs(d.y) >= Math.abs(d.z)) {
                pwy += (d.y > 0 ? -1 : 1);
            } else {
                pwz += (d.z > 0 ? -1 : 1);
            }
        }
        if (wouldIntersectPlayer(pwx, pwy, pwz)) return;

        if (toPlace == BlockType.EGG) {
            MobManager mm = getStateManager().getState(MobManager.class);
            if (mm != null) {
                Vector3f spawn = new Vector3f(pwx + 0.5f, pwy + 1.0f, pwz + 0.5f);
                mm.spawnEgg(spawn);
            }
        } else if (toPlace == BlockType.DUMMY) {
            MobManager mm = getStateManager().getState(MobManager.class);
            if (mm != null) {
                Vector3f spawn = new Vector3f(pwx + 0.5f, pwy + 1.0f, pwz + 0.5f);
                mm.spawnTrainingDummy(spawn);
            }
        } else if (toPlace == BlockType.DEVFEST) {
            DevFestBuilder.placeDevFest(chunkManager, app.getCamera().getLocation(), app.getCamera().getDirection().normalize());
        } else if (toPlace == BlockType.WATER) {
            chunkManager.setBlockAndMetaAtWorld(pwx, pwy, pwz, BlockType.WATER, 0);
            chunkManager.enqueueWaterUpdate(pwx, pwy, pwz);
        } else {
            // Evita rebuild redundante se já for o mesmo bloco
            if (chunkManager.getBlockAtWorld(pwx, pwy, pwz) != toPlace) {
                chunkManager.setBlockAtWorld(pwx, pwy, pwz, toPlace);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++)
                            chunkManager.enqueueWaterUpdate(pwx + dx, pwy + dy, pwz + dz);
            }
        }
        hotbar.triggerPlaceSwing();
    }
}
