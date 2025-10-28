package com.minecraftcopilot.mobs;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.minecraftcopilot.world.ChunkManager;

/**
 * Boneco de treino com visual mais próximo de um dummy real (poste de madeira com tronco, braços e base),
 * e estágios de dano progressivos.
 */
public class TrainingDummy extends SimpleMob {
    private final AssetManager assets;
    private final float maxHealth;

    // Estrutura do modelo
    private final Node model = new Node("dummyModel");
    private final Node baseNode = new Node("base");
    private final Node postNode = new Node("post");
    private final Node torsoNode = new Node("torso");
    private final Node armsNode = new Node("arms");
    private final Node headNode = new Node("head");
    private final Node bandsNode = new Node("bands");

    // Materiais
    private final Material mWood;
    private final Material mWoodDark;
    private final Material mWoodBurnt;
    private final Material mMetal;

    // Geometrias principais
    private Geometry head, torso, post;
    private Geometry armL, armR;
    private Geometry baseA, baseB;
    private Geometry bandTorso, bandPost;

    public TrainingDummy(AssetManager assets) {
        this.assets = assets;
        this.maxHealth = 60f;
        this.health = maxHealth;
        this.radius = 0.45f;
        this.height = 1.8f;

        // Materiais Lighting simples
        mWood = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mWood.setBoolean("UseMaterialColors", true);
        mWood.setColor("Diffuse", new ColorRGBA(0.72f, 0.56f, 0.34f, 1f));
        mWood.setColor("Specular", new ColorRGBA(0.15f, 0.12f, 0.08f, 1f));
        mWood.setFloat("Shininess", 4f);

        mWoodDark = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mWoodDark.setBoolean("UseMaterialColors", true);
        mWoodDark.setColor("Diffuse", new ColorRGBA(0.50f, 0.36f, 0.20f, 1f));
        mWoodDark.setColor("Specular", new ColorRGBA(0.12f, 0.10f, 0.06f, 1f));
        mWoodDark.setFloat("Shininess", 3f);

        mWoodBurnt = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mWoodBurnt.setBoolean("UseMaterialColors", true);
        mWoodBurnt.setColor("Diffuse", new ColorRGBA(0.25f, 0.20f, 0.12f, 1f));
        mWoodBurnt.setColor("Specular", new ColorRGBA(0.08f, 0.06f, 0.04f, 1f));
        mWoodBurnt.setFloat("Shininess", 2f);

        mMetal = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mMetal.setBoolean("UseMaterialColors", true);
        mMetal.setColor("Diffuse", new ColorRGBA(0.65f, 0.67f, 0.72f, 1f));
        mMetal.setColor("Specular", new ColorRGBA(0.9f, 0.9f, 0.95f, 1f));
        mMetal.setFloat("Shininess", 16f);

        // Base em cruz
        baseA = new Geometry("baseA", new Box(0.55f, 0.03f, 0.12f));
        baseB = new Geometry("baseB", new Box(0.12f, 0.03f, 0.55f));
        baseA.setMaterial(mWoodDark);
        baseB.setMaterial(mWoodDark);
        baseA.setLocalTranslation(0f, 0.03f, 0f);
        baseB.setLocalTranslation(0f, 0.03f, 0f);
        baseNode.attachChild(baseA);
        baseNode.attachChild(baseB);

        // Poste vertical (cilindro alinhado ao eixo Y via rotação)
        Cylinder cPost = new Cylinder(12, 16, 0.09f, 1.2f, true);
        post = new Geometry("post", cPost);
        post.setMaterial(mWoodDark);
        post.setLocalRotation(new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0));
        postNode.attachChild(post);
        postNode.setLocalTranslation(0f, 0.6f, 0f);

        // Torso (colchão cilíndrico)
        Cylinder cTorso = new Cylinder(12, 24, 0.24f, 0.5f, true);
        torso = new Geometry("torso", cTorso);
        torso.setMaterial(mWood);
        torso.setLocalRotation(new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0));
        torsoNode.attachChild(torso);
        torsoNode.setLocalTranslation(0f, 1.10f, 0f);

        // Anéis (faixas metálicas)
        Torus tTorso = new Torus(16, 12, 0.02f, 0.24f);
        bandTorso = new Geometry("bandTorso", tTorso);
        bandTorso.setMaterial(mMetal);
        bandTorso.setLocalRotation(new Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        bandsNode.attachChild(bandTorso);
        bandsNode.setLocalTranslation(0f, 1.10f, 0f);

        Torus tPost = new Torus(16, 10, 0.015f, 0.09f);
        bandPost = new Geometry("bandPost", tPost);
        bandPost.setMaterial(mMetal);
        bandPost.setLocalRotation(new Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        Node bandPostNode = new Node("bandPostNode");
        bandPostNode.attachChild(bandPost);
        bandPostNode.setLocalTranslation(0f, 0.95f, 0f);
        bandsNode.attachChild(bandPostNode);

        // Braços (cilindros ao longo do eixo X)
        Cylinder cArm = new Cylinder(10, 16, 0.06f, 0.6f, true);
        armL = new Geometry("armL", cArm);
        armR = new Geometry("armR", cArm);
        armL.setMaterial(mWood);
        armR.setMaterial(mWood);
        // alinhar Z->X
        Quaternion armRot = new Quaternion().fromAngles(0, -FastMath.HALF_PI, 0);
        armL.setLocalRotation(armRot);
        armR.setLocalRotation(armRot);
        armL.setLocalTranslation(-0.33f, 1.10f, 0f);
        armR.setLocalTranslation(+0.33f, 1.10f, 0f);
        armsNode.attachChild(armL);
        armsNode.attachChild(armR);

        // Cabeça (esfera acolchoada)
        head = new Geometry("head", new Sphere(16, 16, 0.18f));
        head.setMaterial(mWood);
        headNode.attachChild(head);
        headNode.setLocalTranslation(0f, 1.48f, 0f);

        // Monta o modelo
        model.attachChild(baseNode);
        model.attachChild(postNode);
        model.attachChild(torsoNode);
        model.attachChild(armsNode);
        model.attachChild(headNode);
        model.attachChild(bandsNode);
        attachChild(model);

        updateVisualByHealth();
    }

    @Override
    public void damage(float amt) {
        super.damage(amt);
        updateVisualByHealth();
    }

    @Override
    public void kick(Vector3f impulse) {
        // Feedback sutil de balanço
        float k = Math.min(0.35f, impulse.length() * 0.05f);
        headNode.setLocalRotation(new Quaternion().fromAngles(k, 0f, 0f));
        torsoNode.setLocalRotation(torsoNode.getLocalRotation().mult(new Quaternion().fromAngles(0f, k * 0.25f, 0f)));
    }

    private void setArmsVisible(boolean v) {
        armsNode.setCullHint(v ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    private void updateVisualByHealth() {
        float h = Math.max(0f, health) / maxHealth;
        if (h > 0.66f) {
            // Intacto
            head.setMaterial(mWood);
            torso.setMaterial(mWood);
            post.setMaterial(mWoodDark);
            armL.setMaterial(mWood);
            armR.setMaterial(mWood);
            headNode.setLocalRotation(Quaternion.IDENTITY);
            torsoNode.setLocalRotation(Quaternion.IDENTITY);
            armL.setLocalRotation(new Quaternion().fromAngles(0f, -FastMath.HALF_PI, 0f));
            armR.setLocalRotation(new Quaternion().fromAngles(0f, -FastMath.HALF_PI, 0f));
            setArmsVisible(true);
            bandsNode.setCullHint(Spatial.CullHint.Inherit);
        } else if (h > 0.33f) {
            // Danificado
            head.setMaterial(mWoodDark);
            torso.setMaterial(mWoodDark);
            armL.setMaterial(mWoodDark);
            armR.setMaterial(mWoodDark);
            headNode.setLocalRotation(new Quaternion().fromAngles(0.20f, 0.10f, -0.12f));
            torsoNode.setLocalRotation(new Quaternion().fromAngles(0f, 0.08f, 0f));
            // Braços levemente caídos
            armL.setLocalRotation(new Quaternion().fromAngles(0.25f, -FastMath.HALF_PI, 0.10f));
            armR.setLocalRotation(new Quaternion().fromAngles(-0.25f, -FastMath.HALF_PI, -0.10f));
            setArmsVisible(true);
            bandsNode.setCullHint(Spatial.CullHint.Inherit);
        } else if (h > 0f) {
            // Crítico
            head.setMaterial(mWoodBurnt);
            torso.setMaterial(mWoodBurnt);
            armL.setMaterial(mWoodBurnt);
            armR.setMaterial(mWoodBurnt);
            headNode.setLocalRotation(new Quaternion().fromAngles(0.45f, -0.18f, 0.25f));
            torsoNode.setLocalRotation(new Quaternion().fromAngles(0f, 0.22f, 0f));
            armL.setLocalRotation(new Quaternion().fromAngles(0.60f, -FastMath.HALF_PI, 0.25f));
            armR.setLocalRotation(new Quaternion().fromAngles(-0.40f, -FastMath.HALF_PI, -0.18f));
            setArmsVisible(true);
            // Uma das faixas pode "sumir" como se tivesse soltado
            bandsNode.setCullHint(Spatial.CullHint.Inherit);
        } else {
            // Destruído: braços somem e cabeça baixa
            headNode.setLocalRotation(new Quaternion().fromAngles(0.80f, 0f, 0f));
            setArmsVisible(false);
            torsoNode.setLocalRotation(new Quaternion().fromAngles(0f, 0.30f, 0f));
            // mantém materiais queimados
            head.setMaterial(mWoodBurnt);
            torso.setMaterial(mWoodBurnt);
            post.setMaterial(mWoodDark);
            bandsNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    @Override
    public void update(float tpf, ChunkManager cm) {
        // Sem IA: fica preso ao chão com gravidade básica
        Vector3f pos = getWorldTranslation().clone();
        velocity.set(0, velocity.y, 0);
        velocity.y -= 9.8f * tpf;
        float ny = pos.y + velocity.y * tpf;
        if (velocity.y <= 0) {
            if (isBlockingFoot(cm, pos.x, ny, pos.z)) {
                pos.y = (float)Math.floor(ny) + 1.001f;
                velocity.y = 0f;
            } else pos.y = ny;
        }
        setLocalTranslation(pos);
    }

    // Torna métodos de colisão do SimpleMob acessíveis aqui
    private boolean isBlockingFoot(ChunkManager cm, float x, float y, float z) {
        int wy = (int)Math.floor(y);
        int wx1 = (int)Math.floor(x - radius);
        int wx2 = (int)Math.floor(x + radius);
        int wz1 = (int)Math.floor(z - radius);
        int wz2 = (int)Math.floor(z + radius);
        return cm.isBlockingAtWorld(wx1, wy, wz1) || cm.isBlockingAtWorld(wx2, wy, wz1)
                || cm.isBlockingAtWorld(wx1, wy, wz2) || cm.isBlockingAtWorld(wx2, wy, wz2);
    }
}
