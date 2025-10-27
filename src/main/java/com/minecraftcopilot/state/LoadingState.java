package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.minecraftcopilot.world.ChunkManager;

/**
 * Overlay de carregamento: exibe uma barra de progresso enquanto os chunks iniciais são gerados.
 */
public class LoadingState extends BaseAppState {

    private SimpleApplication app;
    private Node guiRoot;
    private Geometry overlayBg;
    private Geometry barBg;
    private Geometry barFill;
    private BitmapText title;
    private BitmapFont font;
    private float startTime;
    private float minShowTime = 1.0f; // mínimo em segundos para evitar "flash"
    private float timeout = 6.0f; // tempo máximo de loading antes de seguir mesmo sem 100%

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiRoot = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();

        overlayBg = new Geometry("loading-overlay", new Quad(w, h));
        Material ov = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        ov.setColor("Color", new ColorRGBA(0, 0, 0, 0.55f));
        ov.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        overlayBg.setMaterial(ov);
        overlayBg.setQueueBucket(RenderQueue.Bucket.Gui);
        overlayBg.setLocalTranslation(0, 0, -1);
        guiRoot.attachChild(overlayBg);

        float bw = Math.max(320f, w * 0.35f);
        float bh = 22f;
        float bx = (w - bw) / 2f;
        float by = h * 0.3f;

        barBg = new Geometry("loading-bar-bg", new Quad(bw, bh));
        Material bb = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bb.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.12f, 0.9f));
        bb.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        barBg.setMaterial(bb);
        barBg.setQueueBucket(RenderQueue.Bucket.Gui);
        barBg.setLocalTranslation(bx, by, 0);
        guiRoot.attachChild(barBg);

        barFill = new Geometry("loading-bar-fill", new Quad(1, bh));
        Material bf = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bf.setColor("Color", new ColorRGBA(0.22f, 0.62f, 0.34f, 1f));
        bf.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        barFill.setMaterial(bf);
        barFill.setQueueBucket(RenderQueue.Bucket.Gui);
        barFill.setLocalTranslation(bx, by, 0);
        guiRoot.attachChild(barFill);

        title = new BitmapText(font);
        title.setText("Carregando mundo...");
        title.setSize(font.getCharSet().getRenderedSize() * 1.0f);
        title.setColor(new ColorRGBA(0.95f, 0.98f, 1f, 1f));
        title.setLocalTranslation((w - title.getLineWidth()) / 2f, by + bh + 30f, 0);
        guiRoot.attachChild(title);

        startTime = 0f; // será setado no primeiro update
    }

    @Override
    public void update(float tpf) {
        if (startTime == 0f) startTime = getApplication().getTimer().getTimeInSeconds();
        float now = getApplication().getTimer().getTimeInSeconds();
        float elapsed = now - startTime;

        VoxelGameState vgs = getStateManager().getState(VoxelGameState.class);
        if (vgs == null) return; // nada para fazer
        ChunkManager cm = vgs.getChunkManager();
        if (cm == null) return;

        // Progresso estimado: chunks carregados / (2r+1)^2
        int loaded = cm.getLoadedChunkCount();
        int r = cm.getViewRadius();
        int expected = (2 * r + 1) * (2 * r + 1);
        float progress = Math.max(0.02f, Math.min(1f, (float) loaded / (float) expected));

    // Atualiza barra
    float bw = ((Quad) barBg.getMesh()).getWidth();
        float bh = ((Quad) barBg.getMesh()).getHeight();
        float bx = barBg.getLocalTranslation().x;
        float by = barBg.getLocalTranslation().y;
        float fillW = Math.max(1f, bw * progress);
        ((Quad) barFill.getMesh()).updateGeometry(fillW, bh);
        barFill.setLocalTranslation(bx, by, 0);

        // Condições de saída: 80%+ e mínimo de tempo, ou timeout absoluto
        if ((progress >= 0.80f && elapsed >= minShowTime) || elapsed >= timeout) {
            getStateManager().detach(this);
        }
    }

    @Override
    protected void cleanup(Application application) {
        if (overlayBg != null) overlayBg.removeFromParent();
        if (barBg != null) barBg.removeFromParent();
        if (barFill != null) barFill.removeFromParent();
        if (title != null) title.removeFromParent();
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }
}
