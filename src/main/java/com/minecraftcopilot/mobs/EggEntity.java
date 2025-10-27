package com.minecraftcopilot.mobs;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

public class EggEntity extends Node {
    private float elapsed = 0f;
    private final float duration;
    private final Geometry geom;
    private final float baseRadius;
    private final float yScale;

    public EggEntity(AssetManager assets, float durationSec) {
        this.duration = durationSec;
        this.baseRadius = 0.25f;
        this.yScale = 1.2f;
        Sphere s = new Sphere(12, 12, baseRadius);
        geom = new Geometry("egg", s);
        Material m = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", new ColorRGBA(1f, 0.95f, 0.4f, 1f));
        m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        geom.setMaterial(m);
        attachChild(geom);
        // levemente achatado para parecer um ovo
        setLocalScale(1f, yScale, 1f);
    }

    // Atualiza tremor e retorna true quando deve chocar
    public boolean updateEgg(float tpf) {
        elapsed += tpf;
        float t = Math.min(1f, elapsed / duration);
        // tremor: pequena oscilação em yaw/pitch e bounce no Y
        float wob = 0.2f * FastMath.sin(elapsed * 25f) * (0.3f + 0.7f * t);
        Quaternion rot = new Quaternion().fromAngles(0.05f * wob, 0.07f * FastMath.sin(elapsed * 19f), 0);
        setLocalRotation(rot);
        Vector3f base = getLocalTranslation();
        setLocalTranslation(base.x, base.y + 0.03f * FastMath.sin(elapsed * 18f), base.z);
        return elapsed >= duration;
    }

    // Raio vertical efetivo após o scale em Y (para assentar corretamente sobre o chão)
    public float getVerticalRadius() {
        return baseRadius * yScale;
    }
}
