package com.minecraftcopilot.ui;

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
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.minecraftcopilot.BlockType;

import java.util.ArrayList;
import java.util.List;

public class InventoryState extends BaseAppState {

    private static final String INV_CLOSE = "INV_Close"; // tecla E
    private static final String INV_PICK = "INV_Pick";  // clique esquerdo

    private final HotbarState hotbar;
    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;
    private final List<ItemEntry> entries = new ArrayList<>();
    private BitmapText title;
    private BitmapText tip;

    private static class ItemEntry {
        BlockType type;
        BitmapText text;
        float x, y, w, h; // bounds para click
    }

    public InventoryState(HotbarState hotbar) {
        this.hotbar = hotbar;
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiRoot = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();

        title = new BitmapText(font);
        title.setText("Inventário");
        title.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation((w - title.getLineWidth()) / 2f, h * 0.8f, 0);
        guiRoot.attachChild(title);

        tip = new BitmapText(font);
        tip.setText("Clique em um item para enviar ao slot selecionado da hotbar • Pressione E para fechar");
        tip.setSize(font.getCharSet().getRenderedSize() * 0.7f);
        tip.setColor(new ColorRGBA(0.9f, 0.95f, 1f, 1f));
        tip.setLocalTranslation((w - tip.getLineWidth()) / 2f, h * 0.75f, 0);
        guiRoot.attachChild(tip);

        // Lista simples de itens disponíveis
        BlockType[] available = new BlockType[]{ BlockType.GRASS, BlockType.DIRT, BlockType.STONE };
        float startY = h * 0.6f;
        float lineH = font.getCharSet().getRenderedSize() * 1.1f;
        for (int i = 0; i < available.length; i++) {
            BlockType t = available[i];
            BitmapText bt = new BitmapText(font);
            bt.setText("[" + t.name() + "] - clique para colocar na hotbar");
            bt.setColor(ColorRGBA.White);
            float tw = bt.getLineWidth();
            float tx = (w - tw) / 2f;
            float ty = startY - i * lineH;
            bt.setLocalTranslation(tx, ty, 0);
            guiRoot.attachChild(bt);

            ItemEntry e = new ItemEntry();
            e.type = t;
            e.text = bt;
            e.x = tx; e.y = ty - lineH; e.w = tw; e.h = lineH; // bounds aproximado
            entries.add(e);
        }

        mapInputs();
        // mostra cursor enquanto inventário aberto
        app.getInputManager().setCursorVisible(true);
    }

    private void mapInputs() {
        var im = app.getInputManager();
        if (!im.hasMapping(INV_CLOSE)) im.addMapping(INV_CLOSE, new KeyTrigger(KeyInput.KEY_E));
        if (!im.hasMapping(INV_PICK)) im.addMapping(INV_PICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        if (action == null) {
            action = (name, isPressed, tpf) -> {
                if (!isPressed) return;
                if (INV_CLOSE.equals(name)) {
                    getStateManager().detach(this);
                    return;
                }
                if (INV_PICK.equals(name)) {
                    Vector2f cur = app.getInputManager().getCursorPosition();
                    // Converter para sistema do guiNode (y cresce para cima em BitmapText localTranslation)
                    float mx = cur.x;
                    float my = cur.y;
                    for (ItemEntry e : entries) {
                        // bounds: x..x+w, y..y+h (com y base semelhante ao setLocalTranslation Y)
                        if (mx >= e.x && mx <= e.x + e.w && my >= e.y && my <= e.y + e.h) {
                            int idx = hotbar.getSelectedIndex();
                            hotbar.setSlot(idx, e.type);
                            break;
                        }
                    }
                }
            };
        }
        im.addListener(action, INV_CLOSE, INV_PICK);
    }

    private ActionListener action;

    @Override
    protected void cleanup(Application application) {
        var im = app.getInputManager();
        if (im != null) {
            if (action != null) im.removeListener(action);
            if (im.hasMapping(INV_CLOSE)) im.deleteMapping(INV_CLOSE);
            if (im.hasMapping(INV_PICK)) im.deleteMapping(INV_PICK);
        }
        if (title != null) title.removeFromParent();
        if (tip != null) tip.removeFromParent();
        for (ItemEntry e : entries) if (e.text != null) e.text.removeFromParent();
        entries.clear();
        // a visibilidade do cursor será controlada fora (VoxelGameState) ao fechar
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }
}
