package com.minecraftcopilot.player;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class PlayerController extends BaseAppState {

    private static final String MAP_FORWARD = "PC_Forward";
    private static final String MAP_BACK = "PC_Back";
    private static final String MAP_LEFT = "PC_Left";
    private static final String MAP_RIGHT = "PC_Right";
    private static final String MAP_JUMP = "PC_Jump";
    private static final String LOOK_LEFT = "PC_LookLeft";
    private static final String LOOK_RIGHT = "PC_LookRight";
    private static final String LOOK_UP = "PC_LookUp";
    private static final String LOOK_DOWN = "PC_LookDown";

    private static final float EYE_HEIGHT = 1.90f; // altura dos olhos (personagem mais alto)
    // Em geração plana, o topo do bloco está em (h + 1). Como usamos h=18, o topo é 19.
    // Mantemos o valor do topo para que os "pés" fiquem exatamente acima da superfície.
    private static final float GROUND_Y = 19.0f; // Y do topo do chão plano atual
    private static final float GRAVITY = 19.6f;  // m/s^2
    private static final float JUMP_SPEED = 6.8f;
    private static final float MOVE_SPEED = 5.0f;

    private SimpleApplication app;
    private Vector3f position = new Vector3f(16, GROUND_Y + EYE_HEIGHT + 12f, 16);
    private float vy = 0f;
    private boolean onGround = false;

    private boolean fwd, back, left, right, jump;
    private float yaw = 0f;   // rotação em torno de Y
    private float pitch = 0f; // rotação em torno de X (clamp)
    private float mouseSensitivity = 2.2f;

    private final ActionListener input = (name, isPressed, tpf) -> {
        boolean val = isPressed;
        if (MAP_FORWARD.equals(name)) fwd = val;
        else if (MAP_BACK.equals(name)) back = val;
        else if (MAP_LEFT.equals(name)) left = val;
        else if (MAP_RIGHT.equals(name)) right = val;
        else if (MAP_JUMP.equals(name)) jump = val;
    };

    private final AnalogListener mouse = (name, value, tpf) -> {
        float delta = value * mouseSensitivity;
        if (LOOK_LEFT.equals(name)) {
            yaw += delta;
        } else if (LOOK_RIGHT.equals(name)) {
            yaw -= delta;
        } else if (LOOK_UP.equals(name)) {
            pitch -= delta;
        } else if (LOOK_DOWN.equals(name)) {
            pitch += delta;
        }
        clampPitch();
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

        // Desabilita FlyByCamera para não conflitar e usa mapeamento próprio de mouse (com clamp de pitch)
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(false);
        }

        // Mouse look com clamp de pitch
        app.getInputManager().addMapping(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        app.getInputManager().addMapping(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        app.getInputManager().addMapping(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        app.getInputManager().addMapping(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        app.getInputManager().addListener(mouse, LOOK_LEFT, LOOK_RIGHT, LOOK_UP, LOOK_DOWN);

        // Inicializa yaw/pitch a partir da rotação atual da câmera (mantém direção inicial)
        float[] angles = new float[3];
        app.getCamera().getRotation().toAngles(angles);
        pitch = angles[0];
        yaw = angles[1];
        clampPitch();
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
        if (feetY <= ground + 1e-4f) { // tolerância numérica
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

        // Aplicar rotação (com clamp) e posicionar câmera
        Quaternion rot = new Quaternion();
        rot.fromAngles(pitch, yaw, 0f);
        app.getCamera().setRotation(rot);
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
            if (app.getInputManager().hasMapping(LOOK_LEFT)) app.getInputManager().deleteMapping(LOOK_LEFT);
            if (app.getInputManager().hasMapping(LOOK_RIGHT)) app.getInputManager().deleteMapping(LOOK_RIGHT);
            if (app.getInputManager().hasMapping(LOOK_UP)) app.getInputManager().deleteMapping(LOOK_UP);
            if (app.getInputManager().hasMapping(LOOK_DOWN)) app.getInputManager().deleteMapping(LOOK_DOWN);
            app.getInputManager().removeListener(input);
            app.getInputManager().removeListener(mouse);
        }
    }

    @Override
    protected void onEnable() {
        if (app != null && app.getInputManager() != null) {
            app.getInputManager().setCursorVisible(false);
        }
    }

    @Override
    protected void onDisable() {
        if (app != null && app.getInputManager() != null) {
            app.getInputManager().setCursorVisible(true);
        }
    }

    private void clampPitch() {
        // Limita a visão a cerca de 180° no total: de -89° a +89°
        float limit = FastMath.DEG_TO_RAD * 89f;
        if (pitch > limit) pitch = limit;
        if (pitch < -limit) pitch = -limit;
    }
}
