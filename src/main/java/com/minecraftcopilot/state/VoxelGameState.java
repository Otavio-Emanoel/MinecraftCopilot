package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.minecraftcopilot.world.ChunkManager;

public class VoxelGameState extends BaseAppState {

    private SimpleApplication app;
    private Node worldNode;
    private Material chunkMaterial;
    private ChunkManager chunkManager;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.worldNode = new Node("world");

        // Cor do fundo "dia"
        app.getViewPort().setBackgroundColor(new ColorRGBA(0.6f, 0.8f, 1f, 1f));

        // Habilitar câmera fly no jogo
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(true);
            app.getFlyByCamera().setMoveSpeed(20f);
            app.getFlyByCamera().setZoomSpeed(10f);
        }

        // Material com cores por vértice
        this.chunkMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        this.chunkMaterial.setBoolean("VertexColor", true);

        int seed = 1337;
        this.chunkManager = new ChunkManager(worldNode, chunkMaterial, seed, 6);
        app.getRootNode().attachChild(worldNode);

        app.getCamera().setLocation(new Vector3f(16, 25, 48));
        app.getCamera().lookAt(new Vector3f(16, 15, 16), Vector3f.UNIT_Y);
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
    }
}
