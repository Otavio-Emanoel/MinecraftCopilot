package com.minecraftcopilot.player;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;

public class PlayerController extends BaseAppState {

    private static final String MAP_FORWARD = "PC_Forward";
    private static final String MAP_BACK = "PC_Back";
    private static final String MAP_LEFT = "PC_Left";
    private static final String MAP_RIGHT = "PC_Right";
    private static final String MAP_JUMP = "PC_Jump";

    private static final float EYE_HEIGHT = 1.62f;
    private static final float GROUND_Y = 18.0f; // terreno plano atual
    private static final float GRAVITY = 19.6f;  // m/s^2
    private static final float JUMP_SPEED = 6.8f;
    private static final float MOVE_SPEED = 5.0f;

    private SimpleApplication app;
    private Vector3f position = new Vector3f(16, GROUND_Y + EYE_HEIGHT + 12f, 16);
    private float vy = 0f;
    private boolean onGround = false;

    private boolean fwd, back, left, right, jump;

    private final ActionListener input = (name, isPressed, tpf) -> {
        boolean val = isPressed;
        if (MAP_FORWARD.equals(name)) fwd = val;
        else if (MAP_BACK.equals(name)) back = val;
        else if (MAP_LEFT.equals(name)) left = val;
        else if (MAP_RIGHT.equals(name)) right = val;
        else if (MAP_JUMP.equals(name)) jump = val;
    };

    public PlayerController() { }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        // Mapear inputs (WASD + Espaço)
        app.getInputManager().addMapping(MAP_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping(MAP_BACK, new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping(MAP_LEFT, new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping(MAP_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping(MAP_JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addListener(input, MAP_FORWARD, MAP_BACK, MAP_LEFT, MAP_RIGHT, MAP_JUMP);

        // FlyCam: apenas girar com o mouse
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setMoveSpeed(0f);
            app.getFlyByCamera().setDragToRotate(false);
        }
    }

    @Override
    public void update(float tpf) {
        // Movimento no plano XZ baseado na câmera
        Vector3f camDir = app.getCamera().getDirection().clone();
        camDir.y = 0;
        if (camDir.lengthSquared() > 0) camDir.normalizeLocal();
        Vector3f camLeft = app.getCamera().getLeft().clone();
        camLeft.y = 0;
        if (camLeft.lengthSquared() > 0) camLeft.normalizeLocal();

        Vector3f move = new Vector3f();
        if (fwd) move.addLocal(camDir);
        if (back) move.addLocal(camDir.negate());
        if (left) move.addLocal(camLeft);
        if (right) move.addLocal(camLeft.negate());
        if (move.lengthSquared() > 0) {
            move.normalizeLocal().multLocal(MOVE_SPEED * tpf);
            position.addLocal(move);
        }

        // Gravidade simples
        vy -= GRAVITY * tpf;
        position.y += vy * tpf;

        float feetY = position.y - EYE_HEIGHT;
        float ground = GROUND_Y; // futuro: consultar gerador/Chunk para altura real
        if (feetY <= ground) {
            position.y = ground + EYE_HEIGHT;
            vy = 0f;
            onGround = true;
        } else {
            onGround = false;
        }

        // Pulo
        if (jump && onGround) {
            vy = JUMP_SPEED;
            onGround = false;
        }

        // Posicionar câmera
        app.getCamera().setLocation(position);
    }

    @Override
    protected void cleanup(Application application) {
        if (app != null && app.getInputManager() != null) {
            if (app.getInputManager().hasMapping(MAP_FORWARD)) app.getInputManager().deleteMapping(MAP_FORWARD);
            if (app.getInputManager().hasMapping(MAP_BACK)) app.getInputManager().deleteMapping(MAP_BACK);
            if (app.getInputManager().hasMapping(MAP_LEFT)) app.getInputManager().deleteMapping(MAP_LEFT);
            if (app.getInputManager().hasMapping(MAP_RIGHT)) app.getInputManager().deleteMapping(MAP_RIGHT);
            if (app.getInputManager().hasMapping(MAP_JUMP)) app.getInputManager().deleteMapping(MAP_JUMP);
            app.getInputManager().removeListener(input);
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
