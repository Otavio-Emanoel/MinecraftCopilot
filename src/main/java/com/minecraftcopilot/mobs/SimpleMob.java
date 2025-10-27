package com.minecraftcopilot.mobs;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.minecraftcopilot.world.ChunkManager;

import java.util.Random;

public abstract class SimpleMob extends Node {
    protected float health = 10f;
    protected Vector3f velocity = new Vector3f();
    protected float wanderTimer = 0f;
    protected final Random rng = new Random();
    protected float radius = 0.3f;   // raio horizontal da hitbox
    protected float height = 0.9f;   // altura da hitbox

    public boolean isAlive() { return health > 0f; }

    public void damage(float amt) {
        health -= amt;
    }

    public void kick(Vector3f impulse) {
        velocity.addLocal(impulse);
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void update(float tpf, ChunkManager cm) {
        // Gravidade simples
        velocity.y -= 9.8f * tpf;

        // Wander: escolhe direção a cada 2..5s; às vezes fica parado
        wanderTimer -= tpf;
        if (wanderTimer <= 0f) {
            wanderTimer = 2f + rng.nextFloat() * 3f;
            if (rng.nextFloat() < 0.3f) {
                // para
                velocity.x = 0f; velocity.z = 0f;
            } else {
                float ang = rng.nextFloat() * FastMath.TWO_PI;
                float speed = 1.2f;
                velocity.x = FastMath.cos(ang) * speed;
                velocity.z = FastMath.sin(ang) * speed;
            }
        }

        // Movimento e colisão voxel simplificada (somente chão e obstáculos frontais baixos)
        Vector3f pos = getWorldTranslation().clone();

    // XZ slide simples com hitbox
        Vector3f next = pos.add(velocity.x * tpf, 0, velocity.z * tpf);
    if (!isBlockingFoot(cm, next.x, pos.y, pos.z)) pos.x = next.x; else velocity.x = 0f;
    if (!isBlockingFoot(cm, pos.x, pos.y, next.z)) pos.z = next.z; else velocity.z = 0f;

        // Y
        float ny = pos.y + velocity.y * tpf;
        if (velocity.y <= 0) {
            if (isBlockingFoot(cm, pos.x, ny, pos.z)) {
                pos.y = (float)Math.floor(ny) + 1.001f; // fica sobre o bloco
                velocity.y = 0f;
            } else pos.y = ny;
        } else {
            // colisão no topo da hitbox
            if (isBlockingHead(cm, pos.x, ny, pos.z)) {
                velocity.y = 0f;
            } else pos.y = ny;
        }

        setLocalTranslation(pos);
    }

    private boolean isBlockingFoot(ChunkManager cm, float x, float y, float z) {
        int wy = (int)Math.floor(y);
        int wx1 = (int)Math.floor(x - radius);
        int wx2 = (int)Math.floor(x + radius);
        int wz1 = (int)Math.floor(z - radius);
        int wz2 = (int)Math.floor(z + radius);
        return cm.isBlockingAtWorld(wx1, wy, wz1) || cm.isBlockingAtWorld(wx2, wy, wz1)
                || cm.isBlockingAtWorld(wx1, wy, wz2) || cm.isBlockingAtWorld(wx2, wy, wz2);
    }

    private boolean isBlockingHead(ChunkManager cm, float x, float y, float z) {
        int wy = (int)Math.floor(y + height);
        int wx1 = (int)Math.floor(x - radius);
        int wx2 = (int)Math.floor(x + radius);
        int wz1 = (int)Math.floor(z - radius);
        int wz2 = (int)Math.floor(z + radius);
        return cm.isBlockingAtWorld(wx1, wy, wz1) || cm.isBlockingAtWorld(wx2, wy, wz1)
                || cm.isBlockingAtWorld(wx1, wy, wz2) || cm.isBlockingAtWorld(wx2, wy, wz2);
    }
}
