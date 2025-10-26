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
import com.minecraftcopilot.world.ChunkManager;

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
    private static final float BODY_HEIGHT = 1.90f; // altura do corpo (igual aos olhos para simplificar)
    private static final float HALF_WIDTH = 0.3f;   // metade da largura (como MC ~0.6m)
    private static final float GRAVITY = 19.6f;  // m/s^2
    private static final float JUMP_SPEED = 6.8f;
    private static final float MOVE_SPEED = 5.0f;
    private static final float EPS = 1e-4f;

    private SimpleApplication app;
    private Vector3f position = new Vector3f(16, 30f, 16);
    private float vy = 0f;
    private boolean onGround = false;

    private boolean fwd, back, left, right, jump;
    private float yaw = 0f;   // rotação em torno de Y
    private float pitch = 0f; // rotação em torno de X (clamp)
    private float mouseSensitivity = 2.2f;
    private final ChunkManager chunkManager;

    private final ActionListener input = (name, isPressed, tpf) -> {
        if (!isEnabled()) return; // ignora entradas quando desabilitado (ex.: inventário aberto)
        boolean val = isPressed;
        if (MAP_FORWARD.equals(name)) fwd = val;
        else if (MAP_BACK.equals(name)) back = val;
        else if (MAP_LEFT.equals(name)) left = val;
        else if (MAP_RIGHT.equals(name)) right = val;
        else if (MAP_JUMP.equals(name)) jump = val;
    };

    private final AnalogListener mouse = (name, value, tpf) -> {
        if (!isEnabled()) return; // evita mudar yaw/pitch enquanto inventário está aberto
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

    public PlayerController(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

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
        if (move.lengthSquared() > 0) move.normalizeLocal().multLocal(MOVE_SPEED * tpf);

        // Gravidade simples
        vy -= GRAVITY * tpf;

        // Colisão voxel: resolve eixos Y, depois X e Z
        // 1) Y
        float dy = vy * tpf;
        onGround = false;
        if (dy != 0) {
            dy = resolveAxisY(dy);
        }
        // 2) X
        float dx = move.x;
        if (dx != 0) {
            dx = resolveAxisX(dx);
        }
        // 3) Z
        float dz = move.z;
        if (dz != 0) {
            dz = resolveAxisZ(dz);
        }
        position.addLocal(dx, dy, dz);

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
        // Limpa estados de movimento para não "grudar" ao reativar
        fwd = back = left = right = jump = false;
    }

    private void clampPitch() {
        // Limita a visão a cerca de 180° no total: de -89° a +89°
        float limit = FastMath.DEG_TO_RAD * 89f;
        if (pitch > limit) pitch = limit;
        if (pitch < -limit) pitch = -limit;
    }

    private int floor(float v) { return (int) Math.floor(v); }

    private boolean solidAt(float wx, float wy, float wz) {
        if (chunkManager == null) return false;
        return chunkManager.isSolidAtWorld(floor(wx), floor(wy), floor(wz));
    }

    private float resolveAxisY(float dy) {
        float feetY = position.y - EYE_HEIGHT;
        float newFeetY = feetY + dy;
        float minX = position.x - HALF_WIDTH;
        float maxX = position.x + HALF_WIDTH;
        float minZ = position.z - HALF_WIDTH;
        float maxZ = position.z + HALF_WIDTH;
        if (dy > 0) {
            // subindo: checar blocos no topo
            float topY = newFeetY + BODY_HEIGHT;
            int by = floor(topY);
            for (int ix = floor(minX); ix <= floor(maxX - EPS); ix++) {
                for (int iz = floor(minZ); iz <= floor(maxZ - EPS); iz++) {
                    if (solidAt(ix, by, iz)) {
                        // colide com o bloco: ajusta topo para a face inferior do bloco
                        topY = by - EPS;
                        newFeetY = topY - BODY_HEIGHT;
                        vy = 0f;
                        break;
                    }
                }
            }
        } else if (dy < 0) {
            // descendo: checar blocos no pé
            int by = floor(newFeetY);
            for (int ix = floor(minX); ix <= floor(maxX - EPS); ix++) {
                for (int iz = floor(minZ); iz <= floor(maxZ - EPS); iz++) {
                    if (solidAt(ix, by, iz)) {
                        // pisou no topo do bloco
                        newFeetY = by + 1 + EPS;
                        vy = 0f;
                        onGround = true;
                        break;
                    }
                }
            }
        }
        // aplica
        return newFeetY - feetY;
    }

    private float resolveAxisX(float dx) {
        float minY = position.y - EYE_HEIGHT;
        float maxY = minY + BODY_HEIGHT;
        float minZ = position.z - HALF_WIDTH;
        float maxZ = position.z + HALF_WIDTH;
        if (dx > 0) {
            float maxX = position.x + HALF_WIDTH + dx;
            int bx = floor(maxX);
            for (int by = floor(minY); by <= floor(maxY - EPS); by++) {
                for (int bz = floor(minZ); bz <= floor(maxZ - EPS); bz++) {
                    if (solidAt(bx, by, bz)) {
                        maxX = bx - EPS;
                        dx = maxX - (position.x + HALF_WIDTH);
                        return dx;
                    }
                }
            }
        } else if (dx < 0) {
            float minX = position.x - HALF_WIDTH + dx;
            int bx = floor(minX);
            for (int by = floor(minY); by <= floor(maxY - EPS); by++) {
                for (int bz = floor(minZ); bz <= floor(maxZ - EPS); bz++) {
                    if (solidAt(bx, by, bz)) {
                        minX = bx + 1 + EPS;
                        dx = minX - (position.x - HALF_WIDTH);
                        return dx;
                    }
                }
            }
        }
        return dx;
    }

    private float resolveAxisZ(float dz) {
        float minY = position.y - EYE_HEIGHT;
        float maxY = minY + BODY_HEIGHT;
        float minX = position.x - HALF_WIDTH;
        float maxX = position.x + HALF_WIDTH;
        if (dz > 0) {
            float maxZ = position.z + HALF_WIDTH + dz;
            int bz = floor(maxZ);
            for (int by = floor(minY); by <= floor(maxY - EPS); by++) {
                for (int bx = floor(minX); bx <= floor(maxX - EPS); bx++) {
                    if (solidAt(bx, by, bz)) {
                        maxZ = bz - EPS;
                        dz = maxZ - (position.z + HALF_WIDTH);
                        return dz;
                    }
                }
            }
        } else if (dz < 0) {
            float minZ = position.z - HALF_WIDTH + dz;
            int bz = floor(minZ);
            for (int by = floor(minY); by <= floor(maxY - EPS); by++) {
                for (int bx = floor(minX); bx <= floor(maxX - EPS); bx++) {
                    if (solidAt(bx, by, bz)) {
                        minZ = bz + 1 + EPS;
                        dz = minZ - (position.z - HALF_WIDTH);
                        return dz;
                    }
                }
            }
        }
        return dz;
    }
}
