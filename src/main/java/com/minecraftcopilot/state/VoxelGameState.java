package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.minecraftcopilot.world.ChunkManager;
import com.minecraftcopilot.player.PlayerController;
import com.minecraftcopilot.gfx.TextureAtlas;
import com.minecraftcopilot.ui.HotbarState;
import com.minecraftcopilot.Chunk;

public class VoxelGameState extends BaseAppState {

    private SimpleApplication app;
    private Node worldNode;
    private Material chunkMaterial;
    private ChunkManager chunkManager;
    private BitmapText crosshair;
    private BitmapFont font;
    private PlayerController player;
    private HotbarState hotbar;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.worldNode = new Node("world");

        // Cor do fundo "dia"
        app.getViewPort().setBackgroundColor(new ColorRGBA(0.6f, 0.8f, 1f, 1f));

        // Habilitar câmera fly no jogo e capturar mouse
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(true);
            app.getFlyByCamera().setMoveSpeed(12f);
            app.getFlyByCamera().setRotationSpeed(1.5f);
            app.getFlyByCamera().setDragToRotate(false); // rotaciona sempre, sem precisar clicar
            app.getFlyByCamera().setZoomSpeed(0f); // desabilita zoom por rolagem (estilo Minecraft)
        }
        if (app.getInputManager() != null) {
            app.getInputManager().setCursorVisible(false); // esconde e "gruda" o mouse na janela
        }

    // Material com textura (atlas) + vertex color para sombreamento simples
    this.chunkMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    this.chunkMaterial.setBoolean("VertexColor", true);
    TextureAtlas atlas = new TextureAtlas(16, 4);
    Chunk.ATLAS = atlas;
    this.chunkMaterial.setTexture("ColorMap", atlas.buildTexture(app.getAssetManager()));
    // Podemos manter culling Off por robustez no protótipo
    this.chunkMaterial.getAdditionalRenderState().setFaceCullMode(
        com.jme3.material.RenderState.FaceCullMode.Off);

    int seed = 1337;
    this.chunkManager = new ChunkManager(worldNode, chunkMaterial, seed, 6);
        app.getRootNode().attachChild(worldNode);

    app.getCamera().setLocation(new Vector3f(16, 30, 48));
    app.getCamera().lookAt(new Vector3f(16, 15, 16), Vector3f.UNIT_Y);

    // Jogador controlado sem Bullet (cinemático, com gravidade e pulo sobre chão plano)
    player = new PlayerController();
    getStateManager().attach(player);

    // Hotbar com 9 slots e item na mão
    hotbar = new HotbarState(chunkMaterial);
    getStateManager().attach(hotbar);

        // Mira (crosshair) central
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        this.crosshair = new BitmapText(font);
        crosshair.setText("+");
        crosshair.setColor(ColorRGBA.White);
        crosshair.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        app.getGuiNode().attachChild(crosshair);
        centerCrosshair();
    }

    @Override
    protected void cleanup(Application application) {
        if (worldNode != null) {
            worldNode.removeFromParent();
            worldNode.detachAllChildren();
        }
        if (chunkManager != null) {
            chunkManager.clearAll();
        }
        if (crosshair != null) {
            crosshair.removeFromParent();
            crosshair = null;
        }
        if (hotbar != null) {
            getStateManager().detach(hotbar);
            hotbar = null;
        }
        if (app != null && app.getInputManager() != null) {
            app.getInputManager().setCursorVisible(true);
        }
    }

    @Override
    protected void onEnable() {
        // Nada extra
    }

    @Override
    protected void onDisable() {
        // Nada
    }

    @Override
    public void update(float tpf) {
        if (chunkManager != null) {
            // Gera até 4 chunks por frame para evitar travamento
            chunkManager.update(app.getCamera().getLocation(), 4);
        }
        centerCrosshair();
    }

    private void centerCrosshair() {
        if (crosshair == null) return;
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        crosshair.setLocalTranslation((w - crosshair.getLineWidth()) / 2f,
                (h + crosshair.getLineHeight()) / 2f, 0);
    }
}
