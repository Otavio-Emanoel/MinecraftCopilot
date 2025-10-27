package com.minecraftcopilot.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.minecraftcopilot.player.PlayerController;

/**
 * Overlay simples para efeitos visuais de água: tonalidade azul e leve oscilação quando submerso.
 */
public class WaterFXState extends BaseAppState {
    private SimpleApplication app;
    private Geometry overlay;
    private final ColorRGBA color = new ColorRGBA(0.1f, 0.35f, 0.6f, 0.0f);
    private float time = 0f;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();
        Quad q = new Quad(w, h);
        overlay = new Geometry("water-overlay", q);
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color.clone());
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        overlay.setMaterial(m);
        overlay.setQueueBucket(RenderQueue.Bucket.Gui);
        overlay.setLocalTranslation(0, 0, 0);
        overlay.setCullHint(com.jme3.scene.Spatial.CullHint.Always); // começa oculto
        app.getGuiNode().attachChild(overlay);
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        PlayerController pc = getStateManager().getState(PlayerController.class);
        if (pc == null) return;
        boolean under = pc.isHeadUnderwater();
        if (under) {
            // Oscila levemente a opacidade
            float baseA = 0.28f;
            float osc = 0.05f * (FastMath.sin(time * 2.1f) * 0.5f + 0.5f);
            float a = FastMath.clamp(baseA + osc, 0f, 0.6f);
            Material m = overlay.getMaterial();
            ColorRGBA c = color.clone();
            c.a = a;
            m.setColor("Color", c);
            overlay.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        } else {
            overlay.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    @Override
    protected void cleanup(Application application) {
        if (overlay != null) overlay.removeFromParent();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
