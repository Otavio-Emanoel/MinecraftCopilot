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
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;

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
    private final List<Geometry> entryBgs = new ArrayList<>();
    private final List<Geometry> entryIcons = new ArrayList<>();
    private BitmapText title;
    private BitmapText tip;
    private Geometry overlayBg;
    private Geometry hoverHighlight;

    // Grid estilo Minecraft (3 linhas x 9 colunas)
    private final int cols = 9;
    private final int rows = 3;
    private final float slotSize = 64f;
    private final float slotGap = 12f;
    private float gridStartX;
    private float gridStartY;

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

    // Fundo escurecido
    overlayBg = new Geometry("inv-overlay", new Quad(w, h));
    Material ovMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    ovMat.setColor("Color", new ColorRGBA(0,0,0,0.4f));
    ovMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    overlayBg.setMaterial(ovMat);
    overlayBg.setQueueBucket(RenderQueue.Bucket.Gui);
    overlayBg.setLocalTranslation(0, 0, -1);
    guiRoot.attachChild(overlayBg);

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

        // Grid de itens com ícones (3x9)
    BlockType[] available = new BlockType[]{ BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.WOOD, BlockType.LEAVES, BlockType.WATER };
        float gridW = cols * slotSize + (cols - 1) * slotGap;
        gridStartX = (w - gridW) / 2f;
        gridStartY = h * 0.6f;
        int total = cols * rows;
        for (int i = 0; i < total; i++) {
            float x = gridStartX + (i % cols) * (slotSize + slotGap);
            float y = gridStartY - (i / cols) * (slotSize + slotGap);

            // BG
            Geometry bg = new Geometry("inv-bg-"+i, new Quad(slotSize, slotSize));
            Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.35f));
            bgMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
            bg.setMaterial(bgMat);
            bg.setQueueBucket(RenderQueue.Bucket.Gui);
            bg.setLocalTranslation(x, y, 0);
            guiRoot.attachChild(bg);
            entryBgs.add(bg);

            // Ícone
            Geometry icon = (i < available.length) ? buildItemIcon(available[i]) : null;
            if (icon != null) {
                float pad = slotSize * 0.1f;
                icon.setLocalTranslation(x + pad, y + pad, 0);
                icon.setLocalScale(slotSize - 2f * pad);
                guiRoot.attachChild(icon);
            }
            entryIcons.add(icon);

            // Bounds p/ click
            ItemEntry e = new ItemEntry();
            e.type = (i < available.length) ? available[i] : null;
            e.text = null;
            e.x = x; e.y = y; e.w = slotSize; e.h = slotSize;
            entries.add(e);
        }

        // Destaque de hover
        hoverHighlight = new Geometry("inv-hover", new Quad(slotSize, slotSize));
        Material hh = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        hh.setColor("Color", new ColorRGBA(1f,1f,1f,0.12f));
        hh.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        hoverHighlight.setMaterial(hh);
        hoverHighlight.setQueueBucket(RenderQueue.Bucket.Gui);
        hoverHighlight.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        guiRoot.attachChild(hoverHighlight);

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
                            if (e.type != null) {
                                int idx = hotbar.getSelectedIndex();
                                hotbar.setSlot(idx, e.type);
                            }
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
    public void update(float tpf) {
        if (hoverHighlight == null) return;
        Vector2f cur = app.getInputManager().getCursorPosition();
        boolean any = false;
        for (ItemEntry e : entries) {
            if (cur.x >= e.x && cur.x <= e.x + e.w && cur.y >= e.y && cur.y <= e.y + e.h) {
                hoverHighlight.setLocalTranslation(e.x, e.y, 0);
                hoverHighlight.setCullHint(com.jme3.scene.Spatial.CullHint.Inherit);
                any = true;
                break;
            }
        }
        if (!any) hoverHighlight.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

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
        if (overlayBg != null) overlayBg.removeFromParent();
        if (hoverHighlight != null) hoverHighlight.removeFromParent();
        for (ItemEntry e : entries) if (e.text != null) e.text.removeFromParent();
        for (var g : entryBgs) if (g != null) g.removeFromParent();
        for (var g : entryIcons) if (g != null) g.removeFromParent();
        entries.clear();
        entryBgs.clear();
        entryIcons.clear();
        // a visibilidade do cursor será controlada fora (VoxelGameState) ao fechar
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

    private Geometry buildItemIcon(BlockType type) {
        if (type == null || type == BlockType.AIR) return null;
        float[] uv = (Chunk.ATLAS != null) ? Chunk.ATLAS.getUV(type.tileForFace(2)) : new float[]{0,0,1,1};
        Mesh mesh = new Mesh();
        var pb = BufferUtils.createFloatBuffer(new float[]{0,0,0,  1,0,0,  1,1,0,  0,1,0});
        var tb = BufferUtils.createFloatBuffer(new float[]{uv[0],uv[1],  uv[2],uv[1],  uv[2],uv[3],  uv[0],uv[3]});
        var ib = BufferUtils.createIntBuffer(new int[]{0,1,2, 0,2,3});
        mesh.setBuffer(VertexBuffer.Type.Position, 3, pb);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tb);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, ib);
        mesh.updateBound();

        Geometry g = new Geometry("inv-icon", mesh);
        Material m = hotbar.getBlockMaterial().clone();
        if (m.getParam("VertexColor") != null) m.setBoolean("VertexColor", false);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        m.getAdditionalRenderState().setDepthTest(false);
        g.setMaterial(m);
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        return g;
    }
}
