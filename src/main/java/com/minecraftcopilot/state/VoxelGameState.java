package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.minecraftcopilot.Chunk;

public class VoxelGameState extends BaseAppState {

    private SimpleApplication app;
    private Node worldNode;
    private Material chunkMaterial;

    private static final int WORLD_CHUNKS_X = 2;
    private static final int WORLD_CHUNKS_Z = 2;

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
        for (int cx = 0; cx < WORLD_CHUNKS_X; cx++) {
            for (int cz = 0; cz < WORLD_CHUNKS_Z; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                chunk.generateTerrain(seed);
                Geometry geom = chunk.buildGeometry(chunkMaterial);
                worldNode.attachChild(geom);
            }
        }

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
    }

    @Override
    protected void onEnable() {
        // Nada extra
    }

    @Override
    protected void onDisable() {
        // Nada
    }
}
