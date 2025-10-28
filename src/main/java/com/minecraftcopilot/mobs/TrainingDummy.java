package com.minecraftcopilot.mobs;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.minecraftcopilot.world.ChunkManager;

/**
 * Boneco de treino parado, com vida e estágios visuais de dano.
 */
public class TrainingDummy extends SimpleMob {
    private final AssetManager assets;
    private final float maxHealth;

    private final Node model = new Node("dummyModel");
    private final Node headNode = new Node("head");
    private final Node torsoNode = new Node("torso");
    private final Node postNode = new Node("post");

    private final Material mWood;
    private final Material mWoodDark;
    private final Material mWoodBurnt;

    private Geometry head, torso, post;

    public TrainingDummy(AssetManager assets) {
        this.assets = assets;
        this.maxHealth = 60f;
        this.health = maxHealth;
        this.radius = 0.35f;
        this.height = 1.6f;

        // Materiais
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

        // Geometrias básicas
        head = new Geometry("dummyHead", new Box(0.25f, 0.20f, 0.14f));
        head.setMaterial(mWood);
        headNode.attachChild(head);
        headNode.setLocalTranslation(0f, 1.35f, 0f);

        torso = new Geometry("dummyTorso", new Box(0.35f, 0.35f, 0.18f));
        torso.setMaterial(mWood);
        torsoNode.attachChild(torso);
        torsoNode.setLocalTranslation(0f, 0.85f, 0f);

        post = new Geometry("dummyPost", new Box(0.10f, 0.80f, 0.10f));
        post.setMaterial(mWoodDark);
        postNode.attachChild(post);
        postNode.setLocalTranslation(0f, 0.80f, 0f);

        model.attachChild(postNode);
        model.attachChild(torsoNode);
        model.attachChild(headNode);
        attachChild(model);
    }

    @Override
    public void damage(float amt) {
        super.damage(amt);
        updateVisualByHealth();
    }

    @Override
    public void kick(Vector3f impulse) {
        // Boneco não se move; mas dá um feedback de balanço no topo
        float ang = Math.min(0.35f, impulse.length() * 0.05f);
        headNode.setLocalRotation(new Quaternion().fromAngles(ang, 0f, 0f));
    }

    private void updateVisualByHealth() {
        float h = Math.max(0f, health) / maxHealth;
        if (h > 0.66f) {
            // intacto
            head.setMaterial(mWood);
            torso.setMaterial(mWood);
            post.setMaterial(mWoodDark);
            headNode.setLocalRotation(Quaternion.IDENTITY);
            torsoNode.setLocalRotation(Quaternion.IDENTITY);
        } else if (h > 0.33f) {
            // danificado: escurece e inclina a cabeça
            head.setMaterial(mWoodDark);
            torso.setMaterial(mWoodDark);
            headNode.setLocalRotation(new Quaternion().fromAngles(0.18f, 0.12f, -0.10f));
        } else if (h > 0f) {
            // crítico: ainda mais escuro, cabeça bem torta e torso levemente girado
            head.setMaterial(mWoodBurnt);
            torso.setMaterial(mWoodBurnt);
            headNode.setLocalRotation(new Quaternion().fromAngles(0.40f, -0.12f, 0.22f));
            torsoNode.setLocalRotation(new Quaternion().fromAngles(0f, 0.22f, 0f));
        } else {
            // destruído: abaixa um pouco a cabeça
            headNode.setLocalRotation(new Quaternion().fromAngles(0.60f, 0f, 0f));
        }
    }

    @Override
    public void update(float tpf, ChunkManager cm) {
        // Sem IA: só garante que fica no chão
        Vector3f pos = getWorldTranslation().clone();
        velocity.set(0, velocity.y, 0); // sem movimento horizontal
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
        // usa mesmos cálculos do SimpleMob
        int wy = (int)Math.floor(y);
        int wx1 = (int)Math.floor(x - radius);
        int wx2 = (int)Math.floor(x + radius);
        int wz1 = (int)Math.floor(z - radius);
        int wz2 = (int)Math.floor(z + radius);
        return cm.isBlockingAtWorld(wx1, wy, wz1) || cm.isBlockingAtWorld(wx2, wy, wz1)
                || cm.isBlockingAtWorld(wx1, wy, wz2) || cm.isBlockingAtWorld(wx2, wy, wz2);
    }
}
