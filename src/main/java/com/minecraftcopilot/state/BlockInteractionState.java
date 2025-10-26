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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;
import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;
import com.minecraftcopilot.world.ChunkManager;

public class BlockInteractionState extends BaseAppState {

    private static final String MAP_BREAK = "BI_Break";

    private SimpleApplication app;
    private final Node worldNode;
    private final ChunkManager chunkManager;

    // highlight
    private Geometry outline;
    private int selWx, selWy, selWz;
    private boolean hasSelection = false;

    private ActionListener action;

    public BlockInteractionState(Node worldNode, ChunkManager chunkManager) {
        this.worldNode = worldNode;
        this.chunkManager = chunkManager;
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        app.getInputManager().addMapping(MAP_BREAK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        // Define o listener agora que a instância já foi construída (chunkManager está pronto)
        this.action = (name, isPressed, tpf) -> {
            if (!isPressed) return;
            if (MAP_BREAK.equals(name) && hasSelection) {
                // Quebrar o bloco selecionado
                chunkManager.setBlockAtWorld(selWx, selWy, selWz, BlockType.AIR);
                // Após quebrar, força re-avaliar a seleção no próximo update
                hasSelection = false;
                if (outline != null) outline.removeFromParent();
                outline = null;
            }
        };
        app.getInputManager().addListener(action, MAP_BREAK);
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
    }

    @Override
    protected void cleanup(Application application) {
        if (app != null && app.getInputManager() != null) {
            if (app.getInputManager().hasMapping(MAP_BREAK)) app.getInputManager().deleteMapping(MAP_BREAK);
            app.getInputManager().removeListener(action);
        }
        clearSelection();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
