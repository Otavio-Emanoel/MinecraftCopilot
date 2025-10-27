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
import com.minecraftcopilot.state.GameMode;

public class MainMenuState extends BaseAppState {

    private static final String MAP_PLAY = "Menu_Play";
    private static final String MAP_EXIT = "Menu_Exit";
    private static final String MAP_CLICK = "Menu_Click";
    private static final String MAP_BACKSPACE = "Menu_Backspace";
    private static final String MAP_MINUS = "Menu_Minus";
    private static final String[] MAP_DIGITS = {
        "Menu_0","Menu_1","Menu_2","Menu_3","Menu_4","Menu_5","Menu_6","Menu_7","Menu_8","Menu_9"
    };

    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;
    private BitmapText title;
    private BitmapText titleShadow;
    private BitmapText subtitle;
    // Botão Settings (placeholder)
    private Geometry btnSettingsBg;
    private BitmapText btnSettingsText;
    private Geometry btnPlayBg;
    private BitmapText btnPlayText;
    private Geometry btnExitBg;
    private BitmapText btnExitText;
    private Geometry bgOverlay;

    // Seed UI
    private Geometry seedBoxBg;
    private BitmapText seedLabel;
    private BitmapText seedText;
    private boolean seedFocused = false;
    private StringBuilder seedBuilder = new StringBuilder();
    private float seedX, seedY, seedW, seedH;

    // Game mode UI
    private Geometry modeLabelBg;
    private BitmapText modeLabel;
    private Geometry modeCreativeBg;
    private BitmapText modeCreativeText;
    private Geometry modeSurvivalBg;
    private BitmapText modeSurvivalText;
    private float modeCX, modeCY, modeCW, modeCH;
    private float modeSX, modeSY, modeSW, modeSH;
    private GameMode selectedMode = GameMode.CREATIVE;

    // Bounds para hit-test
    private float playX, playY, playW, playH;
    private float settingsX, settingsY, settingsW, settingsH;
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
            } else if (MAP_BACKSPACE.equals(name)) {
                if (seedFocused && seedBuilder.length() > 0) {
                    seedBuilder.deleteCharAt(seedBuilder.length()-1);
                    refreshSeedText();
                }
            } else if (MAP_MINUS.equals(name)) {
                if (seedFocused) {
                    if (seedBuilder.length() == 0) {
                        seedBuilder.append('-');
                        refreshSeedText();
                    }
                }
            } else {
                // dígitos
                for (int i = 0; i < MAP_DIGITS.length; i++) {
                    if (MAP_DIGITS[i].equals(name) && seedFocused) {
                        seedBuilder.append((char)('0' + i));
                        refreshSeedText();
                        break;
                    }
                }
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
    float titleY = height * 0.80f;
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
    subtitle.setLocalTranslation((width - subtitle.getLineWidth()) / 2f, height * 0.73f, 0);

    guiRoot.attachChild(titleShadow);
    guiRoot.attachChild(title);
    guiRoot.attachChild(subtitle);

    // Coluna de botões à esquerda (estilo Bedrock)
    float btnW = Math.max(280f, width * 0.24f);
    float btnH = 56f;
    float colX = width * 0.08f;
    float colTop = height * 0.60f;
    // Botão Jogar
    playW = btnW; playH = btnH;
    playX = colX;
    playY = colTop;
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

    // Botão Configurações (placeholder)
    settingsW = btnW; settingsH = btnH;
    settingsX = colX;
    settingsY = playY - (btnH + 14f);
    btnSettingsBg = new Geometry("btn-settings", new Quad(settingsW, settingsH));
    Material mset = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mset.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
    mset.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    btnSettingsBg.setMaterial(mset);
    btnSettingsBg.setQueueBucket(RenderQueue.Bucket.Gui);
    btnSettingsBg.setLocalTranslation(settingsX, settingsY, 0);
    guiRoot.attachChild(btnSettingsBg);

    btnSettingsText = new BitmapText(font);
    btnSettingsText.setText("Configurações");
    btnSettingsText.setSize(font.getCharSet().getRenderedSize() * 1.0f);
    btnSettingsText.setColor(new ColorRGBA(0.95f, 0.98f, 1f, 1f));
    btnSettingsText.setLocalTranslation(settingsX + (settingsW - btnSettingsText.getLineWidth())/2f,
        settingsY + (settingsH + btnSettingsText.getLineHeight())/2f, 0);
    guiRoot.attachChild(btnSettingsText);

    // Campo Seed (acima dos botões)
    seedW = Math.max(320f, width * 0.35f);
    seedH = 44f;
    seedX = (width - seedW) / 2f;
    seedY = height * 0.48f;
    seedBoxBg = new Geometry("seed-box", new Quad(seedW, seedH));
    Material sb = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    sb.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
    sb.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    seedBoxBg.setMaterial(sb);
    seedBoxBg.setQueueBucket(RenderQueue.Bucket.Gui);
    seedBoxBg.setLocalTranslation(seedX, seedY, 0);
    guiRoot.attachChild(seedBoxBg);

    seedLabel = new BitmapText(font);
    seedLabel.setText("Seed:");
    seedLabel.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    seedLabel.setColor(new ColorRGBA(0.9f, 0.95f, 1f, 0.9f));
    seedLabel.setLocalTranslation(seedX + 10f, seedY + (seedH + seedLabel.getLineHeight())/2f, 0);
    guiRoot.attachChild(seedLabel);

    seedText = new BitmapText(font);
    seedText.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    seedText.setColor(ColorRGBA.White);
    refreshSeedText();
    guiRoot.attachChild(seedText);

    // Modo de jogo (abaixo do seed)
    float labelY = height * 0.62f;
    modeLabel = new BitmapText(font);
    modeLabel.setText("Modo de Jogo:");
    modeLabel.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    modeLabel.setColor(new ColorRGBA(0.9f, 0.95f, 1f, 0.9f));
    modeLabel.setLocalTranslation((width - modeLabel.getLineWidth())/2f, labelY + modeLabel.getLineHeight(), 0);
    guiRoot.attachChild(modeLabel);

    // Botões Criativo / Survival
    modeCW = 120f; modeCH = 40f;
    modeSW = 120f; modeSH = 40f;
    float gap = 18f;
    float total = modeCW + modeSW + gap;
    modeCX = (width - total)/2f;
    modeCY = height * 0.57f;
    modeSX = modeCX + modeCW + gap;
    modeSY = modeCY;

    modeCreativeBg = new Geometry("mode-creative", new Quad(modeCW, modeCH));
    Material mc = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mc.setColor("Color", new ColorRGBA(0.2f, 0.5f, 0.25f, 0.9f));
    mc.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    modeCreativeBg.setMaterial(mc);
    modeCreativeBg.setQueueBucket(RenderQueue.Bucket.Gui);
    modeCreativeBg.setLocalTranslation(modeCX, modeCY, 0);
    guiRoot.attachChild(modeCreativeBg);

    modeCreativeText = new BitmapText(font);
    modeCreativeText.setText("Criativo");
    modeCreativeText.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    modeCreativeText.setColor(ColorRGBA.White);
    modeCreativeText.setLocalTranslation(modeCX + (modeCW - modeCreativeText.getLineWidth())/2f, modeCY + (modeCH + modeCreativeText.getLineHeight())/2f, 0);
    guiRoot.attachChild(modeCreativeText);

    modeSurvivalBg = new Geometry("mode-survival", new Quad(modeSW, modeSH));
    Material ms = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    ms.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
    ms.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    modeSurvivalBg.setMaterial(ms);
    modeSurvivalBg.setQueueBucket(RenderQueue.Bucket.Gui);
    modeSurvivalBg.setLocalTranslation(modeSX, modeSY, 0);
    guiRoot.attachChild(modeSurvivalBg);

    modeSurvivalText = new BitmapText(font);
    modeSurvivalText.setText("Survival");
    modeSurvivalText.setSize(font.getCharSet().getRenderedSize() * 0.9f);
    modeSurvivalText.setColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 0.9f));
    modeSurvivalText.setLocalTranslation(modeSX + (modeSW - modeSurvivalText.getLineWidth())/2f, modeSY + (modeSH + modeSurvivalText.getLineHeight())/2f, 0);
    guiRoot.attachChild(modeSurvivalText);

    // Botão Sair
    exitW = btnW; exitH = btnH;
    exitX = colX;
    exitY = settingsY - (btnH + 14f);
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
        app.getInputManager().addMapping(MAP_BACKSPACE, new KeyTrigger(KeyInput.KEY_BACK));
        app.getInputManager().addMapping(MAP_MINUS, new KeyTrigger(KeyInput.KEY_MINUS));
        for (int i = 0; i < MAP_DIGITS.length; i++) {
            app.getInputManager().addMapping(MAP_DIGITS[i], new KeyTrigger(KeyInput.KEY_0 + i));
        }
    app.getInputManager().addListener(listener, MAP_PLAY, MAP_EXIT, MAP_CLICK, MAP_BACKSPACE, MAP_MINUS);
        app.getInputManager().addListener(listener, MAP_DIGITS);
    }

    private void startGame() {
        // Desabilitar input do menu e trocar para jogo
        getStateManager().detach(this);
        int seed = getSelectedSeed();
        getStateManager().attach(new VoxelGameState(seed, selectedMode));
        getStateManager().attach(new LoadingState());
    }

    @Override
    protected void cleanup(Application application) {
        // Remover textos e mapeamentos
        if (title != null) title.removeFromParent();
        if (titleShadow != null) titleShadow.removeFromParent();
        if (subtitle != null) subtitle.removeFromParent();
        if (btnPlayBg != null) btnPlayBg.removeFromParent();
        if (btnPlayText != null) btnPlayText.removeFromParent();
        if (btnSettingsBg != null) btnSettingsBg.removeFromParent();
        if (btnSettingsText != null) btnSettingsText.removeFromParent();
        if (btnExitBg != null) btnExitBg.removeFromParent();
        if (btnExitText != null) btnExitText.removeFromParent();
        if (bgOverlay != null) bgOverlay.removeFromParent();
        if (seedBoxBg != null) seedBoxBg.removeFromParent();
        if (seedLabel != null) seedLabel.removeFromParent();
        if (seedText != null) seedText.removeFromParent();
        if (modeLabel != null) modeLabel.removeFromParent();
        if (modeCreativeBg != null) modeCreativeBg.removeFromParent();
        if (modeCreativeText != null) modeCreativeText.removeFromParent();
        if (modeSurvivalBg != null) modeSurvivalBg.removeFromParent();
        if (modeSurvivalText != null) modeSurvivalText.removeFromParent();
        if (app != null) {
            if (app.getInputManager().hasMapping(MAP_PLAY)) app.getInputManager().deleteMapping(MAP_PLAY);
            if (app.getInputManager().hasMapping(MAP_EXIT)) app.getInputManager().deleteMapping(MAP_EXIT);
            if (app.getInputManager().hasMapping(MAP_CLICK)) app.getInputManager().deleteMapping(MAP_CLICK);
            if (app.getInputManager().hasMapping(MAP_BACKSPACE)) app.getInputManager().deleteMapping(MAP_BACKSPACE);
            if (app.getInputManager().hasMapping(MAP_MINUS)) app.getInputManager().deleteMapping(MAP_MINUS);
            for (String m : MAP_DIGITS) {
                if (app.getInputManager().hasMapping(m)) app.getInputManager().deleteMapping(m);
            }
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
    boolean overSettings = hit(cur.x, cur.y, settingsX, settingsY, settingsW, settingsH);
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
        if (btnSettingsBg != null) {
            var m = btnSettingsBg.getMaterial();
            m.setColor("Color", overSettings ? new ColorRGBA(0.18f, 0.18f, 0.18f, 0.95f)
                                              : new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
        }

        // Seed box hover visual
        if (seedBoxBg != null) {
            boolean overSeed = hit(cur.x, cur.y, seedX, seedY, seedW, seedH);
            var m = seedBoxBg.getMaterial();
            m.setColor("Color", overSeed || seedFocused ? new ColorRGBA(0.16f, 0.16f, 0.16f, 0.95f)
                                                         : new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
        }

        // Modo hover e seleção
        if (modeCreativeBg != null && modeSurvivalBg != null) {
            boolean overC = hit(cur.x, cur.y, modeCX, modeCY, modeCW, modeCH);
            boolean overS = hit(cur.x, cur.y, modeSX, modeSY, modeSW, modeSH);
            var mc = modeCreativeBg.getMaterial();
            var ms = modeSurvivalBg.getMaterial();
            if (selectedMode == GameMode.CREATIVE) {
                mc.setColor("Color", overC ? new ColorRGBA(0.22f, 0.62f, 0.34f, 1f) : new ColorRGBA(0.2f, 0.5f, 0.25f, 0.9f));
                ms.setColor("Color", overS ? new ColorRGBA(0.2f, 0.2f, 0.2f, 0.95f) : new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
            } else {
                ms.setColor("Color", overS ? new ColorRGBA(0.22f, 0.62f, 0.34f, 1f) : new ColorRGBA(0.2f, 0.5f, 0.25f, 0.9f));
                mc.setColor("Color", overC ? new ColorRGBA(0.2f, 0.2f, 0.2f, 0.95f) : new ColorRGBA(0.12f, 0.12f, 0.12f, 0.85f));
            }
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
        } else if (hit(cur.x, cur.y, settingsX, settingsY, settingsW, settingsH)) {
            // Placeholder: pequena animação/feedback visual
            app.getViewPort().setBackgroundColor(new ColorRGBA(0.05f, 0.07f, 0.10f, 1f));
        } else if (hit(cur.x, cur.y, seedX, seedY, seedW, seedH)) {
            seedFocused = true;
        } else if (hit(cur.x, cur.y, modeCX, modeCY, modeCW, modeCH)) {
            selectedMode = GameMode.CREATIVE;
        } else if (hit(cur.x, cur.y, modeSX, modeSY, modeSW, modeSH)) {
            selectedMode = GameMode.SURVIVAL; // por enquanto não tem efeito no jogo
        } else {
            seedFocused = false;
        }
    }

    private void refreshSeedText() {
        String txt = (seedBuilder.length() == 0) ? "(aleatória)" : seedBuilder.toString();
        seedText.setText(txt);
        seedText.setLocalTranslation(seedX + 10f + seedLabelWidth(), seedY + (seedH + seedText.getLineHeight())/2f, 0);
    }

    private float seedLabelWidth() {
        return font.getLineWidth("Seed:") + 8f;
    }

    private int getSelectedSeed() {
        if (seedBuilder.length() == 0) {
            long t = System.nanoTime();
            int s = (int) (t ^ (t >>> 32));
            return (s == 0 ? 1337 : s);
        }
        try {
            long v = Long.parseLong(seedBuilder.toString());
            return (int) (v ^ (v >>> 32));
        } catch (NumberFormatException ex) {
            // fallback por hash
            int h = seedBuilder.toString().hashCode();
            return (h == 0 ? 1337 : h);
        }
    }
}
