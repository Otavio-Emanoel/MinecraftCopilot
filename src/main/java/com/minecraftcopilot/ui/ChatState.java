package com.minecraftcopilot.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.renderer.queue.RenderQueue;
import com.minecraftcopilot.player.PlayerController;
import com.minecraftcopilot.state.BlockInteractionState;
import com.minecraftcopilot.world.ChunkManager;
import com.minecraftcopilot.world.DevFestBuilder;
import com.jme3.math.Vector3f;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat simples: abre com T, digita comando e envia com Enter.
 * Por enquanto executa /cleanwater.
 */
public class ChatState extends BaseAppState {

    private static final String MAP_TOGGLE = "CHAT_TOGGLE";

    private final ChunkManager chunkManager;

    private SimpleApplication app;
    private Node gui;
    private BitmapFont font;

    private boolean open = false;
    private StringBuilder buffer = new StringBuilder();
    private Geometry bg;
    private BitmapText label;
    private final List<String> history = new ArrayList<>();
    private final int historyLimit = 8;
    private final List<BitmapText> historyLines = new ArrayList<>();

    public ChatState(ChunkManager cm) {
        this.chunkManager = cm;
    }

    public boolean isOpen() { return open; }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.gui = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // Mapeia a tecla T para abrir/fechar
        var im = app.getInputManager();
        if (!im.hasMapping(MAP_TOGGLE)) {
            im.addMapping(MAP_TOGGLE, new KeyTrigger(KeyInput.KEY_T));
        }
        im.addListener(toggleListener, MAP_TOGGLE);

        // Prepara os elementos, mas não anexa ainda
        Quad q = new Quad(600, 28);
        bg = new Geometry("chat-bg", q);
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", new ColorRGBA(0, 0, 0, 0.55f));
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bg.setMaterial(m);
        bg.setQueueBucket(RenderQueue.Bucket.Gui);

        label = new BitmapText(font);
        label.setText("");
        label.setColor(ColorRGBA.White);
        label.setQueueBucket(RenderQueue.Bucket.Gui);
    }

    private final ActionListener toggleListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        setOpen(!open);
    };

    private final RawInputListener raw = new RawInputListener() {
        @Override public void onKeyEvent(KeyInputEvent evt) {
            if (!open) return;
            try { evt.setConsumed(); } catch (Throwable ignored) {}
            if (!evt.isPressed()) return;
            int code = evt.getKeyCode();
            // Controle
            if (code == KeyInput.KEY_ESCAPE) {
                setOpen(false);
                return;
            }
            if (code == KeyInput.KEY_RETURN) {
                submit();
                return;
            }
            if (code == KeyInput.KEY_BACK) {
                if (buffer.length() > 0) buffer.deleteCharAt(buffer.length() - 1);
                updateLabel();
                return;
            }
            if (code == KeyInput.KEY_SPACE) {
                buffer.append(' ');
                updateLabel();
                return;
            }
            Character ch = getPrintableChar(evt);
            if (ch == null) ch = mapKeyToChar(code);
            if (ch != null) {
                buffer.append(ch.charValue());
                updateLabel();
            }
        }
        @Override public void beginInput() {}
        @Override public void endInput() {}
        @Override public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {
            if (open) try { evt.setConsumed(); } catch (Throwable ignored) {}
        }
        @Override public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) {
            if (open) try { evt.setConsumed(); } catch (Throwable ignored) {}
        }
        @Override public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent evt) {
            if (open) try { evt.setConsumed(); } catch (Throwable ignored) {}
        }
        @Override public void onMouseButtonEvent(com.jme3.input.event.MouseButtonEvent evt) {
            if (open) try { evt.setConsumed(); } catch (Throwable ignored) {}
        }
        @Override public void onTouchEvent(com.jme3.input.event.TouchEvent evt) {
            if (open) try { evt.setConsumed(); } catch (Throwable ignored) {}
        }
    };

    private Character mapKeyToChar(int code) {
        // Dígitos
        if (code >= KeyInput.KEY_0 && code <= KeyInput.KEY_9) {
            return (char) ('0' + (code - KeyInput.KEY_0));
        }
        // Letras A..Z
        if (code >= KeyInput.KEY_A && code <= KeyInput.KEY_Z) {
            char base = (char) ('a' + (code - KeyInput.KEY_A));
            return base; // comandos não precisam de maiúsculas
        }
        // Sinais básicos
        if (code == KeyInput.KEY_SLASH) return '/';
        if (code == KeyInput.KEY_MINUS) return '-';
        if (code == KeyInput.KEY_UNDERLINE) return '_';
        if (code == KeyInput.KEY_PERIOD) return '.';
        if (code == KeyInput.KEY_COMMA) return ',';
        return null;
    }

    private void setOpen(boolean value) {
        if (this.open == value) return;
        this.open = value;
        var im = app.getInputManager();
        if (open) {
            // Mostrar UI
            attachUI();
            im.addRawInputListener(raw);
            // Pausa interações e movimento
            var bi = getStateManager().getState(BlockInteractionState.class);
            if (bi != null) getStateManager().detach(bi);
            var pc = getStateManager().getState(PlayerController.class);
            if (pc != null) pc.setEnabled(false);
            im.setCursorVisible(true);
        } else {
            // Ocultar UI
            detachUI();
            im.removeRawInputListener(raw);
            // Retoma jogo
            var pc = getStateManager().getState(PlayerController.class);
            if (pc != null) pc.setEnabled(true);
            var bi = getStateManager().getState(BlockInteractionState.class);
            if (bi != null && !getStateManager().hasState(bi)) getStateManager().attach(bi);
            im.setCursorVisible(false);
        }
    }

    private void attachUI() {
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();
        bg.setLocalTranslation((w - 600) / 2f, 40, 0);
        gui.attachChild(bg);

        label.setLocalTranslation((w - 600) / 2f + 10, 40 + 20, 0);
        buffer.setLength(0);
        buffer.append('/');
        updateLabel();
        gui.attachChild(label);

        // Renderiza histórico acima da barra
        for (BitmapText t : historyLines) t.removeFromParent();
        historyLines.clear();
        float y = 40 + 28 + 8;
        int start = Math.max(0, history.size() - historyLimit);
        for (int i = start; i < history.size(); i++) {
            BitmapText line = new BitmapText(font);
            line.setText(history.get(i));
            line.setColor(new ColorRGBA(1,1,1,0.95f));
            line.setQueueBucket(RenderQueue.Bucket.Gui);
            line.setLocalTranslation((w - 600) / 2f + 10, y, 0);
            gui.attachChild(line);
            historyLines.add(line);
            y += font.getCharSet().getRenderedSize() + 2;
        }
    }

    private void detachUI() {
        if (bg != null) bg.removeFromParent();
        if (label != null) label.removeFromParent();
        for (BitmapText t : historyLines) t.removeFromParent();
        historyLines.clear();
    }

    private void updateLabel() {
        label.setText(buffer.toString());
    }

    private void submit() {
        String cmd = buffer.toString().trim();
        if (cmd.startsWith("/")) {
            handleCommand(cmd.substring(1));
        } else if (!cmd.isEmpty()) {
            addMessage(cmd);
        }
        setOpen(false);
    }

    private void handleCommand(String cmd) {
        String c = cmd.toLowerCase();
        if (c.equals("abc") || c.equals("cleanwater")) {
            if (chunkManager != null) {
                chunkManager.clearAllWater();
            }
            addMessage("[Sistema] Água removida.");
        } else if (c.equals("devfest")) {
            Vector3f camPos = app.getCamera().getLocation().clone();
            Vector3f camDir = app.getCamera().getDirection().clone();
            DevFestBuilder.placeDevFest(chunkManager, camPos, camDir);
            addMessage("[Sistema] DevFest instalado! 🎉");
        } else {
            addMessage("[Sistema] Comando desconhecido: /" + cmd);
        }
    }

    private void addMessage(String msg) {
        history.add(msg);
        while (history.size() > historyLimit) history.remove(0);
        if (open) attachUI();
    }

    // Mensagem de sistema pública para outros estados (ex.: PlayerController)
    public void systemMessage(String msg) {
        addMessage("[Sistema] " + msg);
    }

    private Character getPrintableChar(KeyInputEvent evt) {
        try {
            Method m = evt.getClass().getMethod("getCharacter");
            Object r = m.invoke(evt);
            if (r instanceof Character) {
                char c = (Character) r;
                if (c >= 32 && c != 127) return c;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    protected void cleanup(Application application) {
        var im = app.getInputManager();
        if (im != null) {
            if (im.hasMapping(MAP_TOGGLE)) im.deleteMapping(MAP_TOGGLE);
            im.removeListener(toggleListener);
            im.removeRawInputListener(raw);
        }
        detachUI();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
