package com.minecraftcopilot.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import com.minecraftcopilot.BlockType;
import com.minecraftcopilot.Chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class HotbarState extends BaseAppState {

    private static final String HB_NEXT = "HB_Next";
    private static final String HB_PREV = "HB_Prev";
    private static final String HB_1 = "HB_1";
    private static final String HB_2 = "HB_2";
    private static final String HB_3 = "HB_3";
    private static final String HB_4 = "HB_4";
    private static final String HB_5 = "HB_5";
    private static final String HB_6 = "HB_6";
    private static final String HB_7 = "HB_7";
    private static final String HB_8 = "HB_8";
    private static final String HB_9 = "HB_9";

    private final Material blockMaterial;
    private SimpleApplication app;
    private Node guiRoot;
    private BitmapFont font;

    private int selected = 0; // 0..8
    private final BlockType[] slots = new BlockType[9];

    // UI
    private Geometry highlight;
    private final List<BitmapText> labels = new ArrayList<>();
    private final List<Geometry> slotBgs = new ArrayList<>();
    private final List<Geometry> slotIcons = new ArrayList<>();
    private float slotSize = 48f;
    private float slotGap = 8f;
    private float barY = 20f;

    // Hand item
    private Node handNode;
    private Geometry handGeom;

    public HotbarState(Material blockMaterial) {
        this.blockMaterial = blockMaterial;
        // Inicia vazio (o jogador pegará itens do inventário)
        for (int i = 0; i < slots.length; i++) slots[i] = null;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public BlockType getSelectedBlock() {
        return slots[selected];
    }

    public int getSelectedIndex() { return selected; }

    public void setSlot(int index, BlockType type) {
        if (index < 0 || index >= slots.length) return;
        slots[index] = type;
        updateLabelText(index);
        updateSlotIcon(index);
        if (index == selected) rebuildHandItem();
    }

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiRoot = app.getGuiNode();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

    // Hotbar com slots quadrados (BG) + ícone do bloco
        float totalW = 9 * slotSize + 8 * slotGap;
        float startX = (app.getCamera().getWidth() - totalW) / 2f;

        // Destaque do slot selecionado
        Quad quad = new Quad(slotSize, slotSize);
        highlight = new Geometry("hb-highlight", quad);
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.12f));
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        highlight.setMaterial(m);
    highlight.setQueueBucket(RenderQueue.Bucket.Gui);
        guiRoot.attachChild(highlight);

        for (int i = 0; i < 9; i++) {
            float x = startX + i * (slotSize + slotGap);

            // BG do slot
            Geometry bg = new Geometry("hb-bg-"+i, new Quad(slotSize, slotSize));
            Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.35f));
            bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bg.setMaterial(bgMat);
            bg.setQueueBucket(RenderQueue.Bucket.Gui);
            bg.setLocalTranslation(x, barY, 0);
            guiRoot.attachChild(bg);
            slotBgs.add(bg);

            // Número pequeno do slot (1..9)
            BitmapText num = new BitmapText(font);
            num.setText(String.valueOf(i + 1));
            num.setColor(new ColorRGBA(0.9f, 0.95f, 1f, 1f));
            num.setSize(font.getCharSet().getRenderedSize() * 0.6f);
            num.setLocalTranslation(x + 4f, barY + 12f, 0);
            guiRoot.attachChild(num);
            labels.add(num);

            // Ícone do item (se houver)
            Geometry icon = buildItemIcon(slots[i]);
            if (icon != null) {
                float pad = slotSize * 0.1f;
                icon.setLocalTranslation(x + pad, barY + pad, 0);
                icon.setLocalScale((slotSize - 2f * pad) / 1f);
                guiRoot.attachChild(icon);
            }
            slotIcons.add(icon);
        }
        updateHighlightPosition();

        // Nó e geometria do item na mão
        handNode = new Node("handNode");
        app.getRootNode().attachChild(handNode);
        rebuildHandItem();

        // Inputs
        mapInputs();
    }

    private void mapInputs() {
        var im = app.getInputManager();
        im.addMapping(HB_NEXT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        im.addMapping(HB_PREV, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        im.addMapping(HB_1, new KeyTrigger(KeyInput.KEY_1));
        im.addMapping(HB_2, new KeyTrigger(KeyInput.KEY_2));
        im.addMapping(HB_3, new KeyTrigger(KeyInput.KEY_3));
        im.addMapping(HB_4, new KeyTrigger(KeyInput.KEY_4));
        im.addMapping(HB_5, new KeyTrigger(KeyInput.KEY_5));
        im.addMapping(HB_6, new KeyTrigger(KeyInput.KEY_6));
        im.addMapping(HB_7, new KeyTrigger(KeyInput.KEY_7));
        im.addMapping(HB_8, new KeyTrigger(KeyInput.KEY_8));
        im.addMapping(HB_9, new KeyTrigger(KeyInput.KEY_9));

        im.addListener(actionListener, HB_1, HB_2, HB_3, HB_4, HB_5, HB_6, HB_7, HB_8, HB_9);
        im.addListener(analogListener, HB_NEXT, HB_PREV);
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        int before = selected;
        switch (name) {
            case HB_1 -> selected = 0;
            case HB_2 -> selected = 1;
            case HB_3 -> selected = 2;
            case HB_4 -> selected = 3;
            case HB_5 -> selected = 4;
            case HB_6 -> selected = 5;
            case HB_7 -> selected = 6;
            case HB_8 -> selected = 7;
            case HB_9 -> selected = 8;
        }
        if (selected != before) onSelectionChanged();
    };

    private final AnalogListener analogListener = (name, value, tpf) -> {
        int before = selected;
        if (HB_NEXT.equals(name)) {
            selected = (selected + 1) % 9;
        } else if (HB_PREV.equals(name)) {
            selected = (selected + 8) % 9; // -1 com wrap
        }
        if (selected != before) onSelectionChanged();
    };

    private void onSelectionChanged() {
        updateHighlightPosition();
        updateLabelsColor();
        rebuildHandItem();
    }

    private void updateSlotIcon(int i) {
        if (i < 0 || i >= slotIcons.size()) return;
        Geometry old = slotIcons.get(i);
        if (old != null) old.removeFromParent();
        Geometry icon = buildItemIcon(slots[i]);
        slotIcons.set(i, icon);
        if (icon != null) {
            float totalW = 9 * slotSize + 8 * slotGap;
            float startX = (app.getCamera().getWidth() - totalW) / 2f;
            float x = startX + i * (slotSize + slotGap);
            float pad = slotSize * 0.1f;
            icon.setLocalTranslation(x + pad, barY + pad, 0);
            icon.setLocalScale((slotSize - 2f * pad) / 1f);
            guiRoot.attachChild(icon);
        }
    }

    private void updateLabelsColor() { /* os números permanecem claros sempre */ }

    private void updateLabelText(int index) { /* sem nomes agora */ }

    private void updateHighlightPosition() {
        float totalW = 9 * slotSize + 8 * slotGap;
        float startX = (app.getCamera().getWidth() - totalW) / 2f;
        float x = startX + selected * (slotSize + slotGap);
        highlight.setLocalTranslation(x, barY, 0);
    }

    private void rebuildHandItem() {
        if (handGeom != null) {
            handGeom.removeFromParent();
            handGeom = null;
        }
        BlockType t = getSelectedBlock();
        if (t == null || t == BlockType.AIR) return;
        handGeom = buildBlockGeometry(t, blockMaterial);
        handGeom.setLocalScale(0.28f);
        handNode.attachChild(handGeom);
    }

    private Geometry buildItemIcon(BlockType type) {
        if (type == null || type == BlockType.AIR) return null;
        // Quad 1x1 com UVs do tile (usaremos face +Y como ícone)
        float[] uv = (Chunk.ATLAS != null) ? Chunk.ATLAS.getUV(type.tileForFace(2)) : new float[]{0,0,1,1};

        Mesh mesh = new Mesh();
        FloatBuffer pb = BufferUtils.createFloatBuffer(new float[]{0,0,0,  1,0,0,  1,1,0,  0,1,0});
        FloatBuffer tb = BufferUtils.createFloatBuffer(new float[]{uv[0],uv[1],  uv[2],uv[1],  uv[2],uv[3],  uv[0],uv[3]});
        IntBuffer ib = BufferUtils.createIntBuffer(new int[]{0,1,2, 0,2,3});
        mesh.setBuffer(VertexBuffer.Type.Position, 3, pb);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tb);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, ib);
        mesh.updateBound();

        Geometry g = new Geometry("hb-icon", mesh);
        Material m = blockMaterial.clone();
        // Ícone 2D não usa VertexColor; evita azul quando não há buffer de cor
        if (m.getParam("VertexColor") != null) m.setBoolean("VertexColor", false);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        m.getAdditionalRenderState().setDepthTest(false);
        g.setMaterial(m);
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        return g;
    }

    @Override
    public void update(float tpf) {
        // Posicionar o item na mão relativo à câmera (inferior direita da tela, um pouco à frente)
        var cam = app.getCamera();
    // Posiciona no canto inferior direito (mais próximo e discreto)
    Vector3f pos = cam.getLocation().add(cam.getDirection().mult(0.45f))
        .add(cam.getLeft().mult(-0.45f))
        .add(cam.getUp().mult(-0.35f));
        handNode.setLocalTranslation(pos);
        Quaternion rot = cam.getRotation().clone();
        Quaternion tilt = new Quaternion().fromAngles(-0.25f, 0.35f, 0.15f);
        rot = rot.mult(tilt);
        handNode.setLocalRotation(rot);
    }

    @Override
    protected void cleanup(Application application) {
        var im = app.getInputManager();
        if (im != null) {
            if (im.hasMapping(HB_NEXT)) im.deleteMapping(HB_NEXT);
            if (im.hasMapping(HB_PREV)) im.deleteMapping(HB_PREV);
            if (im.hasMapping(HB_1)) im.deleteMapping(HB_1);
            if (im.hasMapping(HB_2)) im.deleteMapping(HB_2);
            if (im.hasMapping(HB_3)) im.deleteMapping(HB_3);
            if (im.hasMapping(HB_4)) im.deleteMapping(HB_4);
            if (im.hasMapping(HB_5)) im.deleteMapping(HB_5);
            if (im.hasMapping(HB_6)) im.deleteMapping(HB_6);
            if (im.hasMapping(HB_7)) im.deleteMapping(HB_7);
            if (im.hasMapping(HB_8)) im.deleteMapping(HB_8);
            if (im.hasMapping(HB_9)) im.deleteMapping(HB_9);
            im.removeListener(actionListener);
            im.removeListener(analogListener);
        }
        if (highlight != null) highlight.removeFromParent();
        for (var t : labels) t.removeFromParent();
        labels.clear();
        for (var bg : slotBgs) if (bg != null) bg.removeFromParent();
        slotBgs.clear();
        for (var ic : slotIcons) if (ic != null) ic.removeFromParent();
        slotIcons.clear();
        if (handNode != null) {
            handNode.removeFromParent();
            handNode.detachAllChildren();
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    // --- Utilitário: gera um cubo 1x1x1 com UVs por face de acordo com o BlockType ---
    private Geometry buildBlockGeometry(BlockType type, Material mat) {
        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int face = 0; face < 6; face++) {
            addFace(positions, colors, uvs, indices, 0, 0, 0, face, type);
        }

        Mesh mesh = new Mesh();
        FloatBuffer posBuf = BufferUtils.createFloatBuffer(positions.size());
        for (Float f : positions) posBuf.put(f);
        posBuf.flip();
        FloatBuffer colBuf = BufferUtils.createFloatBuffer(colors.size());
        for (Float f : colors) colBuf.put(f);
        colBuf.flip();
        FloatBuffer uvBuf = BufferUtils.createFloatBuffer(uvs.size());
        for (Float f : uvs) uvBuf.put(f);
        uvBuf.flip();
        IntBuffer idxBuf = BufferUtils.createIntBuffer(indices.size());
        for (Integer v : indices) idxBuf.put(v);
        idxBuf.flip();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, colBuf);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvBuf);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, idxBuf);
        mesh.updateBound();

        Geometry g = new Geometry("hand-block", mesh);
        g.setMaterial(mat);
        return g;
    }

    private static void addFace(List<Float> positions, List<Float> colors, List<Float> uvs, List<Integer> indices,
                                int x, int y, int z, int face, BlockType type) {
        float[][] v = new float[4][3];
        switch (face) {
            case 0 -> { // +X
                v[0] = new float[]{x + 1, y, z};
                v[1] = new float[]{x + 1, y, z + 1};
                v[2] = new float[]{x + 1, y + 1, z + 1};
                v[3] = new float[]{x + 1, y + 1, z};
            }
            case 1 -> { // -X
                v[0] = new float[]{x, y, z + 1};
                v[1] = new float[]{x, y, z};
                v[2] = new float[]{x, y + 1, z};
                v[3] = new float[]{x, y + 1, z + 1};
            }
            case 2 -> { // +Y
                v[0] = new float[]{x, y + 1, z};
                v[1] = new float[]{x + 1, y + 1, z};
                v[2] = new float[]{x + 1, y + 1, z + 1};
                v[3] = new float[]{x, y + 1, z + 1};
            }
            case 3 -> { // -Y
                v[0] = new float[]{x, y, z + 1};
                v[1] = new float[]{x + 1, y, z + 1};
                v[2] = new float[]{x + 1, y, z};
                v[3] = new float[]{x, y, z};
            }
            case 4 -> { // +Z
                v[0] = new float[]{x + 1, y, z + 1};
                v[1] = new float[]{x, y, z + 1};
                v[2] = new float[]{x, y + 1, z + 1};
                v[3] = new float[]{x + 1, y + 1, z + 1};
            }
            case 5 -> { // -Z
                v[0] = new float[]{x, y, z};
                v[1] = new float[]{x + 1, y, z};
                v[2] = new float[]{x + 1, y + 1, z};
                v[3] = new float[]{x, y + 1, z};
            }
        }

        float shade;
        switch (face) {
            case 2 -> shade = 1.00f;
            case 0, 1 -> shade = 0.80f;
            case 4, 5 -> shade = 0.90f;
            default -> shade = 0.70f;
        }
        ColorRGBA c = type.color.clone();
        c.r *= shade; c.g *= shade; c.b *= shade;

        int base = positions.size() / 3;
        for (int i = 0; i < 4; i++) {
            positions.add(v[i][0]); positions.add(v[i][1]); positions.add(v[i][2]);
            colors.add(c.r); colors.add(c.g); colors.add(c.b); colors.add(c.a);
        }

        float[] uv = (Chunk.ATLAS != null) ? Chunk.ATLAS.getUV(type.tileForFace(face)) : new float[]{0,0,1,1};
        switch (face) {
            case 0,1,4,5 -> {
                uvs.add(uv[0]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[3]);
                uvs.add(uv[0]); uvs.add(uv[3]);
            }
            case 2,3 -> {
                uvs.add(uv[0]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[1]);
                uvs.add(uv[2]); uvs.add(uv[3]);
                uvs.add(uv[0]); uvs.add(uv[3]);
            }
        }

        indices.add(base); indices.add(base + 1); indices.add(base + 2);
        indices.add(base); indices.add(base + 2); indices.add(base + 3);
    }
}
