package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.math.Vector2f;
import com.minecraftcopilot.state.VoxelGameState;

public class MainMenuState extends BaseAppState {

    private static final String MAP_PLAY = "Menu_Play";
    private static final String MAP_EXIT = "Menu_Exit";
    private static final String MAP_CLICK = "Menu_Click";

    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;
    private BitmapText title;
    private BitmapText titleShadow;
    private BitmapText subtitle;
    private Geometry btnPlayBg;
    private BitmapText btnPlayText;
    private Geometry btnExitBg;
    private BitmapText btnExitText;
    private Geometry bgOverlay;

    // Bounds para hit-test
    private float playX, playY, playW, playH;
    private float exitX, exitY, exitW, exitH;

    private final ActionListener listener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;
            if (MAP_PLAY.equals(name)) {
                startGame();
            } else if (MAP_EXIT.equals(name)) {
                app.stop();
            } else if (MAP_CLICK.equals(name)) {
                handleClick();
            }
        }
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiRoot = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

    // Fundo sutil + overlay translúcido
    ViewPort vp = app.getViewPort();
    vp.setBackgroundColor(new ColorRGBA(0.06f, 0.08f, 0.11f, 1f));

        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();

    // Overlay leve
    bgOverlay = new Geometry("menu-overlay", new Quad(width, height));
    Material ov = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    ov.setColor("Color", new ColorRGBA(0.1f, 0.15f, 0.2f, 0.25f));
    ov.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    bgOverlay.setMaterial(ov);
    bgOverlay.setQueueBucket(RenderQueue.Bucket.Gui);
    bgOverlay.setLocalTranslation(0, 0, -1);
    guiRoot.attachChild(bgOverlay);

    // Título com sombra
    title = new BitmapText(font);
    title.setText("Minecraft Copilot");
    title.setColor(new ColorRGBA(0.95f, 0.98f, 1f, 1f));
    title.setSize(font.getCharSet().getRenderedSize() * 1.8f);
    float titleX = (width - title.getLineWidth()) / 2f;
    float titleY = height * 0.70f;
    title.setLocalTranslation(titleX, titleY, 0);

    titleShadow = new BitmapText(font);
    titleShadow.setText(title.getText());
    titleShadow.setColor(new ColorRGBA(0f, 0f, 0f, 0.5f));
    titleShadow.setSize(title.getSize());
    titleShadow.setLocalTranslation(titleX + 3f, titleY - 3f, -0.1f);

    subtitle = new BitmapText(font);
    subtitle.setText("Prototype • jMonkeyEngine 3");
    subtitle.setColor(new ColorRGBA(0.8f, 0.9f, 1f, 0.9f));
    subtitle.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    subtitle.setLocalTranslation((width - subtitle.getLineWidth()) / 2f, height * 0.62f, 0);

    guiRoot.attachChild(titleShadow);
    guiRoot.attachChild(title);
    guiRoot.attachChild(subtitle);

    // Botão Jogar
    float btnW = Math.max(260f, width * 0.22f);
    float btnH = 56f;
    playW = btnW; playH = btnH;
    playX = (width - btnW) / 2f;
    playY = height * 0.44f;
    btnPlayBg = new Geometry("btn-play", new Quad(btnW, btnH));
    Material mp = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mp.setColor("Color", new ColorRGBA(0.15f, 0.5f, 0.25f, 0.9f));
    mp.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    btnPlayBg.setMaterial(mp);
    btnPlayBg.setQueueBucket(RenderQueue.Bucket.Gui);
    btnPlayBg.setLocalTranslation(playX, playY, 0);
    guiRoot.attachChild(btnPlayBg);

    btnPlayText = new BitmapText(font);
    btnPlayText.setText("Jogar");
    btnPlayText.setSize(font.getCharSet().getRenderedSize() * 1.0f);
    btnPlayText.setColor(ColorRGBA.White);
    btnPlayText.setLocalTranslation(playX + (btnW - btnPlayText.getLineWidth())/2f,
        playY + (btnH + btnPlayText.getLineHeight())/2f, 0);
    guiRoot.attachChild(btnPlayText);

    // Botão Sair
    exitW = btnW; exitH = btnH;
    exitX = playX;
    exitY = playY - (btnH + 14f);
    btnExitBg = new Geometry("btn-exit", new Quad(exitW, exitH));
    Material me = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    me.setColor("Color", new ColorRGBA(0.35f, 0.15f, 0.15f, 0.9f));
    me.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    btnExitBg.setMaterial(me);
    btnExitBg.setQueueBucket(RenderQueue.Bucket.Gui);
    btnExitBg.setLocalTranslation(exitX, exitY, 0);
    guiRoot.attachChild(btnExitBg);

    btnExitText = new BitmapText(font);
    btnExitText.setText("Sair");
    btnExitText.setSize(font.getCharSet().getRenderedSize() * 1.0f);
    btnExitText.setColor(new ColorRGBA(1f, 0.9f, 0.9f, 1f));
    btnExitText.setLocalTranslation(exitX + (exitW - btnExitText.getLineWidth())/2f,
        exitY + (exitH + btnExitText.getLineHeight())/2f, 0);
    guiRoot.attachChild(btnExitText);

        app.getInputManager().addMapping(MAP_PLAY, new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping(MAP_EXIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
    app.getInputManager().addMapping(MAP_CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    app.getInputManager().addListener(listener, MAP_PLAY, MAP_EXIT, MAP_CLICK);
    }

    private void startGame() {
        // Desabilitar input do menu e trocar para jogo
        getStateManager().detach(this);
        getStateManager().attach(new VoxelGameState());
    }

    @Override
    protected void cleanup(Application application) {
        // Remover textos e mapeamentos
        if (title != null) title.removeFromParent();
        if (titleShadow != null) titleShadow.removeFromParent();
        if (subtitle != null) subtitle.removeFromParent();
        if (btnPlayBg != null) btnPlayBg.removeFromParent();
        if (btnPlayText != null) btnPlayText.removeFromParent();
        if (btnExitBg != null) btnExitBg.removeFromParent();
        if (btnExitText != null) btnExitText.removeFromParent();
        if (bgOverlay != null) bgOverlay.removeFromParent();
        if (app != null) {
            if (app.getInputManager().hasMapping(MAP_PLAY)) app.getInputManager().deleteMapping(MAP_PLAY);
            if (app.getInputManager().hasMapping(MAP_EXIT)) app.getInputManager().deleteMapping(MAP_EXIT);
            if (app.getInputManager().hasMapping(MAP_CLICK)) app.getInputManager().deleteMapping(MAP_CLICK);
            app.getInputManager().removeListener(listener);
        }
    }

    @Override
    protected void onEnable() {
        // Nada extra
        if (app != null) {
            if (app.getFlyByCamera() != null) {
                app.getFlyByCamera().setEnabled(false);
            }
            if (app.getInputManager() != null) {
                app.getInputManager().setCursorVisible(true);
            }
        }
    }

    @Override
    protected void onDisable() {
        // Nada
    }
    @Override
    public void update(float tpf) {
        // Hover visual nos botões
        if (app == null || app.getInputManager() == null) return;
        Vector2f cur = app.getInputManager().getCursorPosition();
        boolean overPlay = hit(cur.x, cur.y, playX, playY, playW, playH);
        boolean overExit = hit(cur.x, cur.y, exitX, exitY, exitW, exitH);
        if (btnPlayBg != null) {
            var m = btnPlayBg.getMaterial();
            m.setColor("Color", overPlay ? new ColorRGBA(0.20f, 0.65f, 0.32f, 1f)
                                          : new ColorRGBA(0.15f, 0.5f, 0.25f, 0.9f));
        }
        if (btnExitBg != null) {
            var m = btnExitBg.getMaterial();
            m.setColor("Color", overExit ? new ColorRGBA(0.55f, 0.20f, 0.20f, 1f)
                                          : new ColorRGBA(0.35f, 0.15f, 0.15f, 0.9f));
        }
    }

    private boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void handleClick() {
        Vector2f cur = app.getInputManager().getCursorPosition();
        if (hit(cur.x, cur.y, playX, playY, playW, playH)) {
            startGame();
        } else if (hit(cur.x, cur.y, exitX, exitY, exitW, exitH)) {
            app.stop();
        }
    }
}
