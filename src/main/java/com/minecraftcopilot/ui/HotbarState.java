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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Box;
import com.jme3.util.BufferUtils;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
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
    private Spatial handGeom;
    private Node swordPivotNode; // quando a mão segura uma espada
    private Node swordModelNode; // modelo real da espada
    private Node swordTipNode;   // nó de referência na ponta

    // Rastro da espada (afterimage)
    private static class TrailSegment {
        Geometry geo; float life; float maxLife;
        TrailSegment(Geometry g, float life) { this.geo = g; this.life = life; this.maxLife = life; }
    }
    private final List<TrailSegment> swordTrails = new ArrayList<>();
    private Vector3f lastTipWS = null;

    // Animações de ação (quebrar/colocar)
    private float hitAnim = 0f;   // 0..1 decai no tempo (para blocos/itens comuns)
    private float placeAnim = 0f; // 0..1 decai no tempo
    // Espada
    private float swordAttack = 0f; // 0..1 decai no tempo
    private boolean swordBlocking = false;
    private static final String HB_ATTACK = "HB_Attack";
    private static final String HB_DEFEND = "HB_Defend";
    private static final String HB_POWER = "HB_Power"; // tecla F: rajada da espada

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

    // Disparadores de animação
    public void triggerBreakSwing() { this.hitAnim = 1f; }
    public void triggerPlaceSwing() { this.placeAnim = 1f; }
    public void triggerSwordAttack() { this.swordAttack = 1f; }
    public void setSwordBlocking(boolean blocking) { this.swordBlocking = blocking; }
    public boolean isSwordSelected() { return getSelectedBlock() == BlockType.SWORD; }

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
    // Luzes locais para Lighting nos itens da mão
    AmbientLight amb = new AmbientLight();
    amb.setColor(new ColorRGBA(0.35f, 0.35f, 0.4f, 1f));
    handNode.addLight(amb);
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.5f, -1f, -0.2f).normalizeLocal());
    sun.setColor(new ColorRGBA(1f,1f,1f,1f));
    handNode.addLight(sun);
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

    im.addMapping(HB_ATTACK, new com.jme3.input.controls.MouseButtonTrigger(com.jme3.input.MouseInput.BUTTON_LEFT));
    im.addMapping(HB_DEFEND, new com.jme3.input.controls.MouseButtonTrigger(com.jme3.input.MouseInput.BUTTON_RIGHT));
    im.addMapping(HB_POWER, new KeyTrigger(KeyInput.KEY_F));

    im.addListener(actionListener, HB_1, HB_2, HB_3, HB_4, HB_5, HB_6, HB_7, HB_8, HB_9, HB_ATTACK, HB_DEFEND, HB_POWER);
        im.addListener(analogListener, HB_NEXT, HB_PREV);
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        int before = selected;
        if (HB_ATTACK.equals(name)) {
            if (isPressed && isSwordSelected()) triggerSwordAttack();
            // deixa BlockInteractionState cuidar de quebrar blocos quando não for espada
            return;
        } else if (HB_DEFEND.equals(name)) {
            if (isSwordSelected()) setSwordBlocking(isPressed);
            return;
        } else if (HB_POWER.equals(name)) {
            if (!isSwordSelected()) return;
            if (isPressed) startSwordPowerCharge(); else releaseSwordPowerCharge();
            return;
        } else if (isPressed) {
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
        }
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

    // Cooldown da rajada
    private float swordPowerCooldown = 0f;
    // Charge da rajada (segurar F)
    private boolean swordPowerCharging = false;
    private float swordPowerChargeTime = 0f;
    private final float swordPowerChargeMax = 0.8f; // ~0.8s para carga máxima
    private Geometry swordChargeFX = null;

    private void rebuildHandItem() {
        if (handGeom != null) {
            handGeom.removeFromParent();
            handGeom = null;
        }
        // Limpa rastros antigos
        for (int i = swordTrails.size() - 1; i >= 0; i--) {
            var s = swordTrails.get(i);
            if (s.geo != null) s.geo.removeFromParent();
            swordTrails.remove(i);
        }
        swordPivotNode = null; swordModelNode = null; swordTipNode = null; lastTipWS = null;
        BlockType t = getSelectedBlock();
        if (t == null || t == BlockType.AIR) {
            // Mostra uma "mão" quando não há item selecionado
            handGeom = buildHandGeometry();
            handGeom.setLocalScale(1f);
        } else if (t == BlockType.SWORD) {
            handGeom = buildSwordModel();
            handGeom.setLocalScale(0.7f);
            if (handGeom instanceof Node n) {
                swordPivotNode = n;
                swordModelNode = (Node) n.getChild("swordModel");
                if (swordModelNode != null) swordTipNode = (Node) swordModelNode.getChild("tip");
            }
        } else {
            handGeom = buildBlockGeometry(t, blockMaterial);
            handGeom.setLocalScale(0.28f);
        }
        handNode.attachChild(handGeom);
    }

    private Geometry buildHandGeometry() {
        // Retângulo 3D simples representando a mão (estilo low-poly)
        Box box = new Box(0.12f, 0.08f, 0.22f); // largura, altura, profundidade
        Geometry g = new Geometry("hand-geo", box);
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        // Tom de pele neutro; pode ser ajustado nas preferências do jogador futuramente
        m.setColor("Color", new ColorRGBA(0.96f, 0.78f, 0.64f, 1f));
        g.setMaterial(m);
        return g;
    }

    private Node buildSwordModel() {
        // Pivot no punho: criamos um nó de pivô (gripPivot) e um nó de modelo que é transladado
        Node gripPivot = new Node("swordPivot");
        Node sword = new Node("swordModel");
        // Materiais simples (cores inspiradas na imagem)
        Material mBlade = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mBlade.setBoolean("UseMaterialColors", false);
        mBlade.setTexture("DiffuseMap", makeNoiseTex(64, 256, new ColorRGBA(0.88f,0.9f,0.95f,1f), 0.06f));
        mBlade.setColor("Specular", new ColorRGBA(1f,1f,1f,1f));
        mBlade.setFloat("Shininess", 48f);

        Material mEdge = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mEdge.setTexture("DiffuseMap", makeNoiseTex(64, 256, new ColorRGBA(1f,1f,1f,1f), 0.03f));
        mEdge.setColor("Specular", new ColorRGBA(1f,1f,1f,1f));
        mEdge.setFloat("Shininess", 96f);

        Material mGuard = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mGuard.setTexture("DiffuseMap", makeNoiseTex(64, 64, new ColorRGBA(0.96f,0.84f,0.20f,1f), 0.08f));
        mGuard.setColor("Specular", new ColorRGBA(1f,1f,0.6f,1f));
        mGuard.setFloat("Shininess", 32f);

        Material mHandle = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mHandle.setTexture("DiffuseMap", makeStripedTex(32, 64, new ColorRGBA(0.13f,0.10f,0.09f,1f), new ColorRGBA(0.35f,0.24f,0.18f,1f), 6));
        mHandle.setColor("Specular", new ColorRGBA(0.2f,0.2f,0.2f,1f));
        mHandle.setFloat("Shininess", 8f);

        Material mPommel = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mPommel.setTexture("DiffuseMap", makeNoiseTex(32, 32, new ColorRGBA(0.92f,0.82f,0.22f,1f), 0.07f));
        mPommel.setColor("Specular", new ColorRGBA(1f,1f,0.6f,1f));
        mPommel.setFloat("Shininess", 24f);

        Material mGem = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mGem.setColor("Color", new ColorRGBA(0.15f, 0.45f, 0.95f, 1f)); // gema azul brilhante

    // Lâmina longa e fina
    float bladeHalfZ = 0.90f; // meia extensão
    Geometry blade = new Geometry("blade", new Box(0.045f, 0.018f, bladeHalfZ));
    blade.setMaterial(mBlade);
    blade.setLocalTranslation(0f, 0.02f, 0.35f);
    sword.attachChild(blade);
    // Fio (bisel) levemente mais claro, sobre a lâmina
    Geometry edge = new Geometry("edge", new Box(0.040f, 0.005f, bladeHalfZ * 0.92f));
    edge.setMaterial(mEdge);
    edge.setLocalTranslation(0f, 0.038f, 0.38f);
    sword.attachChild(edge);

    // Guarda central (bloco) + braços angulados (em V)
    Geometry guardCore = new Geometry("guardCore", new Box(0.10f, 0.025f, 0.045f));
    guardCore.setMaterial(mGuard);
    guardCore.setLocalTranslation(0f, 0.0f, -0.05f);
    sword.attachChild(guardCore);

    Geometry guardLeft = new Geometry("guardLeft", new Box(0.16f, 0.02f, 0.035f));
    guardLeft.setMaterial(mGuard);
    guardLeft.setLocalTranslation(-0.14f, 0.0f, -0.05f);
    guardLeft.setLocalRotation(new Quaternion().fromAngles(0f, 0f, 0.42f));
    sword.attachChild(guardLeft);

    Geometry guardRight = new Geometry("guardRight", new Box(0.16f, 0.02f, 0.035f));
    guardRight.setMaterial(mGuard);
    guardRight.setLocalTranslation(0.14f, 0.0f, -0.05f);
    guardRight.setLocalRotation(new Quaternion().fromAngles(0f, 0f, -0.42f));
    sword.attachChild(guardRight);

    // Gema azul no centro da guarda
    Geometry gem = new Geometry("gem", new Box(0.02f, 0.02f, 0.01f));
    gem.setMaterial(mGem);
    gem.setLocalTranslation(0f, 0.035f, -0.05f);
    sword.attachChild(gem);

    // Cabo com listras alternadas (3 segmentos)
    Geometry handleA = new Geometry("handleA", new Box(0.03f, 0.03f, 0.06f));
    handleA.setMaterial(mHandle);
    handleA.setLocalTranslation(0f, -0.03f, -0.14f);
    sword.attachChild(handleA);

    Geometry handleB = new Geometry("handleB", new Box(0.03f, 0.03f, 0.06f));
    handleB.setMaterial(mHandle);
    handleB.setLocalTranslation(0f, -0.03f, -0.20f);
    sword.attachChild(handleB);

    Geometry handleC = new Geometry("handleC", new Box(0.03f, 0.03f, 0.06f));
    handleC.setMaterial(mHandle);
    handleC.setLocalTranslation(0f, -0.03f, -0.26f);
    sword.attachChild(handleC);

    // Pomo dourado simples
    Geometry pommel = new Geometry("pommel", new Box(0.034f, 0.018f, 0.022f));
    pommel.setMaterial(mPommel);
    pommel.setLocalTranslation(0f, -0.03f, -0.32f);
    sword.attachChild(pommel);

    // Mão que segura (punho)
        Material mSkin = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mSkin.setColor("Color", new ColorRGBA(0.96f, 0.78f, 0.64f, 1f));
        Geometry fist = new Geometry("fist", new Box(0.045f, 0.04f, 0.06f));
        fist.setMaterial(mSkin);
        fist.setLocalTranslation(0f, -0.03f, -0.24f);
        sword.attachChild(fist);

    // Antebraço (um pouco fora da tela, para dar a sensação de braço)
        Material mSleeve = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mSleeve.setColor("Color", new ColorRGBA(0.12f, 0.14f, 0.20f, 1f));
        Geometry forearm = new Geometry("forearm", new Box(0.045f, 0.045f, 0.22f));
        forearm.setMaterial(mSleeve);
        forearm.setLocalTranslation(0f, -0.03f, -0.48f);
        sword.attachChild(forearm);

    // Nó da ponta da lâmina para rastro e colisão
    Node tip = new Node("tip");
    tip.setLocalTranslation(0f, 0.02f, 0.35f + bladeHalfZ + 0.02f);
    sword.attachChild(tip);

        // Translate o modelo para que o punho (fist em 0,-0.03,-0.24) fique na origem do pivô (0,0,0)
        sword.setLocalTranslation(0f, 0.03f, 0.24f);
        gripPivot.attachChild(sword);

        return gripPivot;
    }

    private Texture2D makeNoiseTex(int w, int h, ColorRGBA base, float var) {
        Image img = new Image(Image.Format.RGBA8, w, h, BufferUtils.createByteBuffer(w * h * 4));
        var bb = img.getData(0);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float n = hash(x, y);
                float f = 1f + var * (n - 0.5f) * 2f;
                float r = clamp01(base.r * f);
                float g = clamp01(base.g * f);
                float b = clamp01(base.b * f);
                bb.put((byte)(r * 255)).put((byte)(g * 255)).put((byte)(b * 255)).put((byte)(base.a * 255));
            }
        }
        bb.flip();
        return new Texture2D(img);
    }

    private Texture2D makeStripedTex(int w, int h, ColorRGBA a, ColorRGBA b, int stripeH) {
        Image img = new Image(Image.Format.RGBA8, w, h, BufferUtils.createByteBuffer(w * h * 4));
        var bb = img.getData(0);
        for (int y = 0; y < h; y++) {
            boolean useA = ((y / stripeH) % 2) == 0;
            ColorRGBA c = useA ? a : b;
            for (int x = 0; x < w; x++) {
                bb.put((byte)(c.r * 255)).put((byte)(c.g * 255)).put((byte)(c.b * 255)).put((byte)(c.a * 255));
            }
        }
        bb.flip();
        return new Texture2D(img);
    }

    private static float hash(int x, int y) {
        int n = x * 374761393 + y * 668265263; // números primos
        n = (n ^ (n << 13)) * 1274126177;
        n = n ^ (n >> 16);
        return (n & 0x7fffffff) / (float)0x7fffffff;
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
        // Atualiza cooldown
        if (swordPowerCooldown > 0f) swordPowerCooldown = Math.max(0f, swordPowerCooldown - tpf);
        // Atualiza carga de poder
        if (swordPowerCharging) {
            swordPowerChargeTime = Math.min(swordPowerChargeMax, swordPowerChargeTime + tpf);
        }
        // Posicionar o item na mão relativo à câmera (inferior direita da tela, um pouco à frente)
        var cam = app.getCamera();
        Vector3f base = cam.getLocation().add(cam.getDirection().mult(0.45f))
                .add(cam.getLeft().mult(-0.45f))
                .add(cam.getUp().mult(-0.35f));

        // Sway da mão/Item baseado no head-bob do PlayerController
        var pc = app.getStateManager().getState(com.minecraftcopilot.player.PlayerController.class);
        float swayX = 0f, swayY = 0f;
        if (pc != null) {
            var sw = pc.getHandSway();
            swayX = sw.x; swayY = sw.y;
        }
    // Decaimento das animações de ação
    float decay = 6.0f; // mais alto = mais rápido
    hitAnim = Math.max(0f, hitAnim - decay * tpf);
    placeAnim = Math.max(0f, placeAnim - decay * tpf);
    // Ataque mais cinematográfico (duração ~0.45s)
    swordAttack = Math.max(0f, swordAttack - 2.6f * tpf);

    // Offsets adicionais por ação
    // Quebrar: swing para baixo e um pouco para a direita
    float hitPhase = (1f - hitAnim); // 0 -> 1 durante a animação
    float hitCurve = FastMath.sin(hitPhase * FastMath.PI); // sobe e desce
    float hitTiltX = -0.9f * hitCurve * hitAnim; // baixar
    float hitTiltZ = 0.35f * hitCurve * hitAnim; // girar levemente
    Vector3f hitOffset = cam.getUp().mult(-0.03f * hitCurve * hitAnim)
        .add(cam.getLeft().mult(0.02f * hitCurve * hitAnim));

    // Colocar: empurrar para frente e girar um pouco para dentro
    float placePhase = (1f - placeAnim);
    float placeCurve = FastMath.sin(placePhase * FastMath.PI);
    float placeTiltY = 0.25f * placeCurve * placeAnim;
    Vector3f placeOffset = cam.getDirection().mult(0.06f * placeCurve * placeAnim)
        .add(cam.getLeft().mult(-0.015f * placeCurve * placeAnim));

    // Animações específicas de espada
    Vector3f swordOffset = Vector3f.ZERO;
    float swordTiltX = 0f, swordTiltY = 0f, swordTiltZ = 0f;
    if (isSwordSelected()) {
        // Quando com espada, ignore swings de bloco para não bagunçar a pose
        hitOffset = Vector3f.ZERO; hitTiltX = 0f; hitTiltZ = 0f; placeOffset = Vector3f.ZERO; placeTiltY = 0f;

        // Pose de descanso: lâmina 100% vertical (Z local -> Up da câmera)
        Vector3f restOffset = cam.getDirection().mult(0.12f)
            .add(cam.getLeft().mult(-0.13f))
            .add(cam.getUp().mult(0.12f));
        // Rotação: -90° no X alinha Z->Up; Y/Z pequenos para naturalidade
        float restTiltX = -FastMath.HALF_PI; // vertical exata
        float restTiltY = -0.05f;            // leve giro para dentro
        float restTiltZ = 0.10f;             // leve roll

        swordOffset = swordOffset.add(restOffset);
        swordTiltX += restTiltX;
        swordTiltY += restTiltY;
        swordTiltZ += restTiltZ;

        // Animação de ataque: arco "anime" grande (anticipation -> slash -> follow -> settle)
        float t = 1f - swordAttack; // 0 -> 1
        float aDur = 0.16f, sDur = 0.18f, fDur = 0.07f, zDur = 0.08f; // total ~0.49
        float a = clamp01(t / aDur);
        float s = clamp01((t - aDur) / sDur);
        float f = clamp01((t - aDur - sDur) / fDur);
        float z = clamp01((t - aDur - sDur - fDur) / zDur);
        float aE = easeInOutCubic(a);
        float sE = easeOutCubic(s);
        float fE = easeOutCubic(f);
        float zE = easeInCubic(z);

        // Anticipation: carrega para cima/direita e gira a lâmina para trás
        swordOffset = swordOffset.add(
            cam.getUp().mult(0.08f * aE)
                .add(cam.getLeft().mult(0.10f * aE))
        );
        swordTiltX += 0.55f * aE;   // recua a ponta
        swordTiltY += 0.18f * aE;   // gira para dentro
        swordTiltZ += -0.25f * aE;  // roll opositor

        // Slash: arco enorme cruzando a tela para baixo/esquerda com avanço
        swordOffset = swordOffset.add(
            cam.getDirection().mult(0.45f * sE)
                .add(cam.getLeft().mult(-0.30f * sE))
                .add(cam.getUp().mult(-0.22f * sE))
        );
        swordTiltX += -1.80f * sE;  // grande pitch para frente
        swordTiltY += -1.05f * sE;  // grande yaw levando para esquerda
        swordTiltZ += 0.65f * sE;   // roll para dar drama

        // Impact jolt: pequeno tremor no ápice do corte
        float impact = (s > 0.85f && s < 1.0f) ? (s - 0.85f) / 0.15f : 0f;
        if (impact > 0f) {
            float k = 0.04f * (1f - impact);
            swordOffset = swordOffset.add(cam.getUp().mult(-k)).add(cam.getLeft().mult(k * 0.5f));
            swordTiltZ += 0.12f * (1f - impact);
        }

        // Follow-through: continua um pouco e começa a diminuir a rotação
        swordOffset = swordOffset.add(
            cam.getDirection().mult(0.08f * fE)
                .add(cam.getLeft().mult(-0.05f * fE))
                .add(cam.getUp().mult(-0.03f * fE))
        );
        swordTiltX += -0.35f * fE;
        swordTiltY += -0.25f * fE;
        swordTiltZ += 0.18f * fE;

        // Settle: retorna em direção à pose de descanso
        swordOffset = swordOffset.add(
            cam.getDirection().mult(-0.22f * zE)
                .add(cam.getUp().mult(0.10f * zE))
                .add(cam.getLeft().mult(0.06f * zE))
        );
        swordTiltX += 0.95f * zE;
        swordTiltY += 0.60f * zE;
        swordTiltZ += -0.40f * zE;

        if (swordBlocking) {
            // Defesa: eleva mais e gira para dentro; reduz efeito do swing
            swordOffset = swordOffset.add(cam.getUp().mult(0.10f)).add(cam.getLeft().mult(-0.08f));
            swordTiltX += -0.45f; swordTiltY += -0.20f; swordTiltZ += 0.38f;
        }

        // Carga do poder: puxa a espada para trás e acumula energia na ponta
        if (swordPowerCharging) {
            float charge = clamp01(swordPowerChargeTime / swordPowerChargeMax);
            swordOffset = swordOffset.add(cam.getDirection().mult(-0.10f * charge))
                                     .add(cam.getUp().mult(0.04f * charge));
            swordTiltX += 0.25f * charge;
            swordTiltY += 0.10f * charge;
            // FX de energia na ponta
            updateSwordChargeFX(charge);
        } else if (swordChargeFX != null) {
            // garantir remoção caso algo cancele a carga
            swordChargeFX.removeFromParent();
            swordChargeFX = null;
        }

        // RASTRO: se estamos no meio do slash, solta segmentos seguindo a ponta
        emitSwordTrail(tpf);
    }

    Vector3f pos = base.add(cam.getLeft().mult(-swayX)).add(cam.getUp().mult(swayY))
        .add(hitOffset).add(placeOffset).add(swordOffset);
    handNode.setLocalTranslation(pos);

    // Base tilt diferente para espada: removemos inclinações default para respeitar a verticalidade
    float baseTiltX = -0.25f, baseTiltY = 0.35f, baseTiltZ = 0.15f;
    if (isSwordSelected()) { baseTiltX = 0f; baseTiltY = 0f; baseTiltZ = 0f; }

    Quaternion rot = cam.getRotation().clone();
    Quaternion tilt = new Quaternion().fromAngles(baseTiltX + swayY * 0.7f + hitTiltX + swordTiltX,
                             baseTiltY + swayX * 0.8f + placeTiltY + swordTiltY,
                             baseTiltZ + swayX * 0.4f + hitTiltZ + swordTiltZ);
    rot = rot.mult(tilt);
    handNode.setLocalRotation(rot);
    }

    private void emitSwordTrail(float tpf) {
        if (swordTipNode == null || swordPivotNode == null) return;
        Vector3f tipWS = swordTipNode.getWorldTranslation().clone();
        if (lastTipWS != null) {
            Vector3f dir = tipWS.subtract(lastTipWS);
            float len = dir.length();
            if (len > 0.02f) { // segmento relevante
                dir.divideLocal(len);
                float width = 0.08f; float halfLen = len * 0.5f; // rastro um pouco mais largo
                Geometry trail = new Geometry("sword-trail", new Box(width, width * 0.15f, halfLen));
                Material tm = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                tm.setColor("Color", new ColorRGBA(0.9f, 0.95f, 1f, 0.35f));
                tm.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
                trail.setMaterial(tm);
                trail.setQueueBucket(RenderQueue.Bucket.Transparent);
                trail.setLocalTranslation(lastTipWS.add(tipWS).multLocal(0.5f));
                Quaternion q = new Quaternion();
                q.lookAt(dir, Vector3f.UNIT_Y);
                trail.setLocalRotation(q);
                app.getRootNode().attachChild(trail);
                swordTrails.add(new TrailSegment(trail, 0.18f));

                // HIT DETECTION: aplica dano nos mobs ao varrer o segmento
                var mm = getStateManager().getState(com.minecraftcopilot.mobs.MobManager.class);
                if (mm != null) {
                    float baseDmg = Math.min(12f, len * 60f); // quanto mais rápido o movimento, maior dano
                    // Aumenta o alcance: raio maior no sweep principal
                    mm.applySwordSweep(lastTipWS.clone(), tipWS.clone(), 0.28f, baseDmg, dir.clone());

                    // Alcance extra à frente ("poke"): varredura curta no sentido da câmera a partir da ponta
                    var camDir = app.getCamera().getDirection().normalize();
                    Vector3f pokeStart = tipWS.clone();
                    Vector3f pokeEnd = tipWS.add(camDir.mult(0.5f)); // ~0.5m de alcance adicional
                    mm.applySwordSweep(pokeStart, pokeEnd, 0.16f, baseDmg * 0.8f, camDir.clone());
                }
            }
        }
        lastTipWS = tipWS;

        // Atualiza e limpa segmentos
        for (int i = swordTrails.size() - 1; i >= 0; i--) {
            TrailSegment s = swordTrails.get(i);
            s.life -= tpf;
            float a = clamp01(s.life / s.maxLife);
            var m = s.geo.getMaterial();
            ColorRGBA c = (ColorRGBA) m.getParam("Color").getValue();
            m.setColor("Color", new ColorRGBA(c.r, c.g, c.b, 0.35f * a));
            s.geo.setLocalScale(1f, 1f, a);
            if (s.life <= 0f) {
                s.geo.removeFromParent();
                swordTrails.remove(i);
            }
        }
    }

    // Inicia a carga do poder (ao pressionar F)
    private void startSwordPowerCharge() {
        if (swordPowerCooldown > 0f || swordPowerCharging) return;
        swordPowerCharging = true;
        swordPowerChargeTime = 0f;
        // cria FX simples na ponta
        if (swordTipNode != null && swordChargeFX == null) {
            swordChargeFX = new Geometry("sword-charge-fx", new Box(0.06f, 0.06f, 0.06f));
            Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            m.setColor("Color", new ColorRGBA(0.5f, 0.8f, 1f, 0.6f));
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
            m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            swordChargeFX.setMaterial(m);
            swordChargeFX.setQueueBucket(RenderQueue.Bucket.Transparent);
            app.getRootNode().attachChild(swordChargeFX);
        }
    }

    // Solta a carga e dispara (ao soltar F ou auto no máximo)
    private void releaseSwordPowerCharge() {
        if (!swordPowerCharging) return;
        float f = clamp01(swordPowerChargeTime / swordPowerChargeMax);
        fireSwordWave(f);
        // limpar FX e estado
        if (swordChargeFX != null) { swordChargeFX.removeFromParent(); swordChargeFX = null; }
        swordPowerCharging = false;
        swordPowerChargeTime = 0f;
    }

    // Atualiza FX de carga na ponta
    private void updateSwordChargeFX(float f) {
        if (swordTipNode == null) return;
        Vector3f tip = swordTipNode.getWorldTranslation();
        if (swordChargeFX != null) {
            float s = 0.10f + 0.35f * f;
            swordChargeFX.setLocalTranslation(tip);
            swordChargeFX.setLocalScale(s);
            // pequeno pulso
            var col = (ColorRGBA) swordChargeFX.getMaterial().getParam("Color").getValue();
            float a = 0.35f + 0.45f * f * (0.75f + 0.25f * FastMath.sin(System.nanoTime() * 1e-9f * 10f));
            swordChargeFX.getMaterial().setColor("Color", new ColorRGBA(col.r, col.g, col.b, a));
        }
        // auto-disparo no máximo
        if (swordPowerChargeTime >= swordPowerChargeMax) {
            releaseSwordPowerCharge();
        }
    }

    // Dispara a rajada da espada (Getsuga) com fator de carga
    private void fireSwordWave(float charge) {
        if (swordPowerCooldown > 0f) return;
        Vector3f origin;
        Vector3f dir = app.getCamera().getDirection().normalize();
        if (swordTipNode != null) origin = swordTipNode.getWorldTranslation().clone();
        else origin = app.getCamera().getLocation().add(dir.mult(0.6f));

        var pm = getStateManager().getState(com.minecraftcopilot.mobs.ProjectileManager.class);
        if (pm != null) {
            float f = clamp01(charge);
            float speed = 18f + 12f * f;
            float damage = 14f + 22f * f;
            float outerRadius = 0.50f + 0.90f * f; // bem maior quando carregado
            float innerRadius = outerRadius * 0.55f;
            float arc = 2.6f + 0.3f * f; // abre um pouco mais
            float length = outerRadius * (2.6f + 1.0f * f);
            pm.spawnGetsugaCrescent(origin, dir, speed, damage, outerRadius, innerRadius, arc, length);
            swordPowerCooldown = 1.2f + 0.6f * f; // maior recarga para rajadas maiores
        }
    }

    // Compat: mantém método antigo chamando com carga média
    private void fireSwordWave() { fireSwordWave(0.6f); }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float easeOutCubic(float t) { double u = 1.0 - t; return (float)(1.0 - u*u*u); }
    private static float easeInCubic(float t) { return t*t*t; }
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float)Math.pow(-2f * t + 2f, 3) / 2f;
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
            if (im.hasMapping(HB_ATTACK)) im.deleteMapping(HB_ATTACK);
            if (im.hasMapping(HB_DEFEND)) im.deleteMapping(HB_DEFEND);
            if (im.hasMapping(HB_POWER)) im.deleteMapping(HB_POWER);
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
        // Remove quaisquer rastros remanescentes
        for (var s : swordTrails) if (s.geo != null) s.geo.removeFromParent();
        swordTrails.clear();
        if (swordChargeFX != null) { swordChargeFX.removeFromParent(); swordChargeFX = null; }
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
