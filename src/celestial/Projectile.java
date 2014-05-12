/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Defines a projectile. Projectiles are a particle effect that represents a
 * shot fired from a gun.
 */
package celestial;

import cargo.Hardpoint;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Projectile extends Celestial {
    //particle effect

    transient ParticleEmitter emitter;
    //stats
    private float shieldDamage;
    private float hullDamage;
    private float speed;
    private float range;
    //graphics
    private float size;
    private ColorRGBA startColor;
    private ColorRGBA endColor;
    private float highLife;
    private float lowLife;
    private int numParticles;
    private float variation;
    private String texture;
    private float emitterRate;
    private Vector3f pVel;
    //lifetime counter
    private float diff = 0;
    //who fired?
    private Ship host;
    private Hardpoint origin;
    private boolean initialDistanceCheck = true;

    public Projectile(Universe universe, String name) {
        super(0.00000000001f, universe); //mass cannot be 0 or it is a static spatial in bullet physics
        setName(name);
    }

    @Override
    public void construct(AssetManager assets) {
        constructProjectile(assets);
        constructPhysics();
    }

    private void constructProjectile(AssetManager assets) {
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, numParticles);
        Material trailMat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        trailMat.setTexture("Texture", assets.loadTexture(texture));
        emitter.setMaterial(trailMat);
        emitter.setImagesX(1);
        emitter.setImagesY(1); // 1x1
        emitter.setStartSize(size);
        emitter.setEndSize(0);
        emitter.setGravity(0f, 0f, 0f);
        emitter.setLowLife(highLife);
        emitter.setHighLife(lowLife);
        emitter.getParticleInfluencer().setVelocityVariation(variation);
        emitter.getParticleInfluencer().setInitialVelocity(pVel);
        emitter.setInWorldSpace(false);
        emitter.setSelectRandomImage(true);
        emitter.setEnabled(true);
        emitter.setParticlesPerSec(emitterRate);
        //setup start color
        emitter.setStartColor(startColor);
        emitter.setEndColor(endColor);
        emitter.emitAllParticles();
        //store as spatial
        spatial = emitter;
    }

    private void constructPhysics() {
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(Math.max(size,0.5f));
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //keep it from going to sleep
        physics.setSleepingThresholds(0, 0);
        physics.setLinearDamping(0);
        physics.setAngularDamping(0);
        //start without collission
        physics.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_NONE);
        physics.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_NONE);
        //store name
        nameControl.setParent(this);
        //add physics to mesh
        spatial.addControl(physics);
        spatial.addControl(nameControl);
    }

    /*
     * OOS, weapons always hit and no projectile is generated. This means that
     * the only one we have to worry about is alive().
     */
    @Override
    protected void alive() {
        //check distance from origin
        if (diff > 0.25f && initialDistanceCheck) {
            //disable further testing
            initialDistanceCheck = false;
            //so it can hit everything
            physics.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_01);
            physics.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
        }
        //increment lifespan
        diff += getVelocity().length() * tpf;
        //has the projectile exceeded its max range?
        if (diff >= range) {
            //yep
            setState(State.DYING);
        } else {
            //nope
        }
    }

    @Override
    protected void dying() {
        setState(State.DEAD);
    }

    public float getShieldDamage() {
        return shieldDamage;
    }

    public void setShieldDamage(float shieldDamage) {
        this.shieldDamage = shieldDamage;
    }

    public float getHullDamage() {
        return hullDamage;
    }

    public void setHullDamage(float hullDamage) {
        this.hullDamage = hullDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public ColorRGBA getStartColor() {
        return startColor;
    }

    public void setStartColor(ColorRGBA startColor) {
        this.startColor = startColor;
    }

    public ColorRGBA getEndColor() {
        return endColor;
    }

    public void setEndColor(ColorRGBA endColor) {
        this.endColor = endColor;
    }

    public float getHighLife() {
        return highLife;
    }

    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    public float getLowLife() {
        return lowLife;
    }

    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }

    public int getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(int numParticles) {
        this.numParticles = numParticles;
    }

    public float getVariation() {
        return variation;
    }

    public void setVariation(float variation) {
        this.variation = variation;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public float getDiff() {
        return diff;
    }

    public void setDiff(float diff) {
        this.diff = diff;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getEmitterRate() {
        return emitterRate;
    }

    public void setEmitterRate(float emitterRate) {
        this.emitterRate = emitterRate;
    }

    public Vector3f getpVel() {
        return pVel;
    }

    public void setpVel(Vector3f pVel) {
        this.pVel = pVel;
    }

    public Ship getHost() {
        return host;
    }

    public void setHost(Ship host) {
        this.host = host;
    }

    public Hardpoint getOrigin() {
        return origin;
    }

    public void setOrigin(Hardpoint origin) {
        this.origin = origin;
    }
}
