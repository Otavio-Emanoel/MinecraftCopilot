package com.minecraftcopilot.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.minecraftcopilot.state.VoxelGameState;

public class MainMenuState extends BaseAppState {

    private static final String MAP_PLAY = "Menu_Play";
    private static final String MAP_EXIT = "Menu_Exit";

    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;
    private BitmapText title;
    private BitmapText play;
    private BitmapText exit;

    private final ActionListener listener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;
            if (MAP_PLAY.equals(name)) {
                startGame();
            } else if (MAP_EXIT.equals(name)) {
                app.stop();
            }
        }
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiRoot = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // Fundo sutil
        ViewPort vp = app.getViewPort();
        vp.setBackgroundColor(new ColorRGBA(0.10f, 0.12f, 0.16f, 1f));

        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();

        title = new BitmapText(font);
        title.setText("MinecraftCopilot");
        title.setColor(ColorRGBA.White);
        title.setSize(font.getCharSet().getRenderedSize() * 1.4f);
        title.setLocalTranslation((width - title.getLineWidth()) / 2f, height * 0.65f, 0);

        play = new BitmapText(font);
        play.setText("Pressione ENTER para Jogar");
        play.setColor(new ColorRGBA(0.8f, 0.9f, 1f, 1f));
        play.setLocalTranslation((width - play.getLineWidth()) / 2f, height * 0.45f, 0);

        exit = new BitmapText(font);
        exit.setText("Pressione ESC para Sair");
        exit.setColor(new ColorRGBA(0.7f, 0.8f, 0.9f, 1f));
        exit.setLocalTranslation((width - exit.getLineWidth()) / 2f, height * 0.35f, 0);

        guiRoot.attachChild(title);
        guiRoot.attachChild(play);
        guiRoot.attachChild(exit);

        app.getInputManager().addMapping(MAP_PLAY, new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping(MAP_EXIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(listener, MAP_PLAY, MAP_EXIT);
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
        if (play != null) play.removeFromParent();
        if (exit != null) exit.removeFromParent();
        if (app != null) {
            if (app.getInputManager().hasMapping(MAP_PLAY)) app.getInputManager().deleteMapping(MAP_PLAY);
            if (app.getInputManager().hasMapping(MAP_EXIT)) app.getInputManager().deleteMapping(MAP_EXIT);
            app.getInputManager().removeListener(listener);
        }
    }

    @Override
    protected void onEnable() {
        // Nada extra
        if (app != null && app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        // Nada
    }
}
