package com.minecraftcopilot.mobs;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.minecraftcopilot.world.ChunkManager;

public class ChickenMob extends SimpleMob {
    // Pivôs para animação
    private final Node legLPivot = new Node("legLPivot");
    private final Node legRPivot = new Node("legRPivot");
    private final Node wingLPivot = new Node("wingLPivot");
    private final Node wingRPivot = new Node("wingRPivot");
    private final Node headPivot = new Node("headPivot");

    // Parâmetros do modelo
    private final float legH = 0.25f;
    private final float bodyH = 0.35f;
    private final float bodyY = legH + bodyH * 0.5f;
    private final float headH = 0.18f;
    private final float headY = bodyY + bodyH * 0.5f + headH * 0.5f + 0.05f;

    // Animação
    private float stepPhase = 0f;
    private Vector3f lastDir = new Vector3f(0,0,1);
    private float wingFlapTimer = 0f;

    public ChickenMob(AssetManager assets) {
        // Ajusta hitbox aproximada para o novo modelo
        this.radius = 0.25f; // ~largura/2
        this.height = 0.8f;  // topo da cabeça

        // Materiais
        Material white = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        white.setColor("Color", ColorRGBA.White);
        Material yellow = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        yellow.setColor("Color", new ColorRGBA(1f, 0.85f, 0.2f, 1f));
        Material orange = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        orange.setColor("Color", new ColorRGBA(1f, 0.6f, 0.1f, 1f));
        Material red = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        red.setColor("Color", new ColorRGBA(0.9f, 0.1f, 0.1f, 1f));

        // Pernas (geometria e pivôs)
        Box legBox = new Box(0.025f, legH * 0.5f, 0.025f);
        Geometry legL = new Geometry("legL", legBox);
        legL.setMaterial(yellow);
        legL.setLocalTranslation(0f, -legH * 0.5f, 0f);
        legLPivot.setLocalTranslation(-0.08f, legH, 0.05f);
        legLPivot.attachChild(legL);

        Geometry legR = legL.clone();
        legR.setName("legR");
        legRPivot.setLocalTranslation(0.08f, legH, 0.05f);
        legRPivot.attachChild(legR);

        // Pés (filhos dos pivôs das pernas)
        Box footBox = new Box(0.06f, 0.015f, 0.07f);
        Geometry footL = new Geometry("footL", footBox);
        footL.setMaterial(orange);
        footL.setLocalTranslation(0f, -legH - 0.015f, 0.02f);
        legLPivot.attachChild(footL);

        Geometry footR = footL.clone();
        footR.setName("footR");
        legRPivot.attachChild(footR);

        attachChild(legLPivot);
        attachChild(legRPivot);

        // Corpo
        Box bodyBox = new Box(0.22f, bodyH * 0.5f, 0.30f);
        Geometry body = new Geometry("body", bodyBox);
        body.setMaterial(white);
        body.setLocalTranslation(0f, bodyY, 0f);
        attachChild(body);

        // Asas (pivôs para flap)
        Box wingBox = new Box(0.03f, 0.12f, 0.20f);
        Geometry wingL = new Geometry("wingL", wingBox);
        wingL.setMaterial(white);
        wingL.setLocalTranslation(0f, 0f, 0f);
        wingLPivot.setLocalTranslation(-0.25f, bodyY + 0.02f, 0f);
        wingLPivot.attachChild(wingL);

        Geometry wingRGeom = wingL.clone();
        wingRGeom.setName("wingR");
        wingRPivot.setLocalTranslation(0.25f, bodyY + 0.02f, 0f);
        wingRPivot.attachChild(wingRGeom);

        attachChild(wingLPivot);
        attachChild(wingRPivot);

        // Cabeça e adornos
        Box headBox = new Box(0.12f, headH * 0.5f, 0.12f);
        Geometry head = new Geometry("head", headBox);
        head.setMaterial(white);
        head.setLocalTranslation(0f, 0f, 0.18f);
        headPivot.setLocalTranslation(0f, headY, 0f);
        headPivot.attachChild(head);
        attachChild(headPivot);

        // Bico
        Box beakBox = new Box(0.06f, 0.035f, 0.08f);
        Geometry beak = new Geometry("beak", beakBox);
        beak.setMaterial(orange);
        beak.setLocalTranslation(0f, headY - 0.01f, 0.28f);
        attachChild(beak);

        // Crista (topo da cabeça)
        Box combBox = new Box(0.05f, 0.03f, 0.02f);
        Geometry comb = new Geometry("comb", combBox);
        comb.setMaterial(red);
        comb.setLocalTranslation(0f, headY + headH * 0.5f + 0.03f, 0.14f);
        attachChild(comb);

        // Barbela (abaixo do bico)
        Box wattleBox = new Box(0.03f, 0.025f, 0.02f);
        Geometry wattle = new Geometry("wattle", wattleBox);
        wattle.setMaterial(red);
        wattle.setLocalTranslation(0f, headY - headH * 0.5f - 0.03f, 0.23f);
        attachChild(wattle);
    }

    @Override
    public void update(float tpf, ChunkManager cm) {
        // Atualiza física/IA básica
        super.update(tpf, cm);

        // Orientação para direção de movimento
        Vector3f v = getVelocity();
        Vector3f horiz = new Vector3f(v.x, 0, v.z);
        float speed = horiz.length();
        if (speed > 0.05f) {
            horiz.normalizeLocal();
            // Yaw para olhar na direção do movimento (modelo aponta +Z)
            float yaw = FastMath.atan2(horiz.x, horiz.z);
            setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));

            // Animação de passos: pernas em oposição
            float stepSpeed = 8f; // rad/s base
            stepPhase += stepSpeed * tpf * FastMath.clamp(speed / 1.2f, 0.2f, 1.5f);
            float amp = 0.5f; // amplitude em rad
            legLPivot.setLocalRotation(new Quaternion().fromAngles(amp * FastMath.sin(stepPhase), 0, 0));
            legRPivot.setLocalRotation(new Quaternion().fromAngles(-amp * FastMath.sin(stepPhase), 0, 0));

            // Detecta mudança de direção (flap de asas)
            float dot = horiz.dot(lastDir);
            if (dot < 0.7f) {
                wingFlapTimer = 0.25f; // aciona batida curta
            }
            lastDir.set(horiz);
        } else {
            // parado: relaxa pernas
            legLPivot.setLocalRotation(Quaternion.IDENTITY);
            legRPivot.setLocalRotation(Quaternion.IDENTITY);
        }

        // Batida de asas temporária quando direção muda
        if (wingFlapTimer > 0f) {
            wingFlapTimer -= tpf;
            float t = FastMath.clamp(wingFlapTimer / 0.25f, 0f, 1f);
            // Flap ease-out
            float a = (1f - t) * 0.8f * FastMath.sin((1f - t) * FastMath.PI);
            wingLPivot.setLocalRotation(new Quaternion().fromAngles(0, 0, a));
            wingRPivot.setLocalRotation(new Quaternion().fromAngles(0, 0, -a));
        } else {
            wingLPivot.setLocalRotation(Quaternion.IDENTITY);
            wingRPivot.setLocalRotation(Quaternion.IDENTITY);
        }
    }
}
