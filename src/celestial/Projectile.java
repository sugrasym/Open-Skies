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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import entity.Entity;
import java.util.ArrayList;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Projectile extends Celestial {
    
    public static final float ANGULAR_DAMP = 0.99f;
    public static final float STOP_LOW_VEL_BOUND = 1.0f;
    public static final float STOP_CAUTION = 0.75f;
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
    private float life = 0;
    //guidance
    private Celestial target;
    private boolean isGuided = false;
    private float thrust = 0;
    private float turning = 0;
    //internal steering
    float pitch;
    float yaw;
    float roll;
    float throttle;
    //who fired?
    private Ship host;
    private Hardpoint origin;
    private boolean initialDistanceCheck = true;
    //collission testing delay
    private float delay;
    private float maxLife;
    private float proximityFuse;
    
    public Projectile(Universe universe, Celestial target, String name, float mass) {
        super(mass, universe); //mass cannot be 0 or it is a static spatial in bullet physics
        setName(name);
        setTarget(target);
        setMass(mass);
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
        if (!isGuided) {
            emitter.setInWorldSpace(false);
        } else {
            emitter.setInWorldSpace(true);
        }
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
        SphereCollisionShape sphereShape = new SphereCollisionShape(Math.max(size, 0.5f));
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
        //check distance from origin and test delay
        if (diff > (0.25 + size) && life > delay && initialDistanceCheck) {
            //disable further testing
            initialDistanceCheck = false;
            //so it can hit everything
            physics.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_01);
            physics.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
        }
        //increment lifespan
        diff += getVelocity().length() * tpf;
        life += tpf;
        //nope
        if (isGuided()) {
            doGuided();
        } else {
            doDumbfire();
        }
        //sync physics
        syncPhysics();
    }
    
    public void doDumbfire() {
        //not guided
        //check max range
        if (diff >= range) {
            setState(State.DYING);
        }
    }
    
    public void doGuided() {
        //check lifetime
        if (life >= getMaxLife()) {
            setState(State.DYING);
        } else {
            //update steering
            boolean safe = seekTarget();
            //apply changes
            steer();
            if (physics != null && target != null && life > delay) {
                if (target.getState() == State.ALIVE) {
                    if (safe) {
                        physics.setAngularVelocity(Vector3f.ZERO);
                        if (!gettingCloser(2)) {
                            throttle = 1;
                        } else {
                            throttle = 0;
                        }
                    } else {
                        if (getVelocity().length() > getAcceleration() * tpf * 3) {
                            decelerate();
                        }
                    }
                } else {
                    throttle = 1;
                }
                if (getProximityFuse() > 0) {
                    System.out.println(getProximityFuse());
                    float dist = target.distanceTo(this);
                    if (dist <= getProximityFuse()) {
                        setState(State.DYING);
                        //explode damaging all ships in fuse range
                        ArrayList<Entity> shipEntities = this.getCurrentSystem().getShipList();
                        ArrayList<Entity> stationEntities = this.getCurrentSystem().getStationList();
                        ArrayList<Entity> combinedList = new ArrayList<>();
                        combinedList.addAll(shipEntities);
                        combinedList.addAll(stationEntities);
                        for (int a = 0; a < combinedList.size(); a++) {
                            if (combinedList.get(a) instanceof Ship) {
                                Ship tmp = (Ship) combinedList.get(a);
                                if (tmp.distanceTo(this) < getProximityFuse()) {
                                    tmp.applyDamage(shieldDamage, hullDamage);
                                    tmp.setLastBlow(host);
                                }
                            }
                        }
                    }
                }
            } else {
                throttle = 0.1f;
            }
            if (throttle != 0) {
                thrust(throttle);
            }
        }
    }
    
    protected boolean gettingCloser(double tolerance) {
        if (target != null) {
            float dt = (float) tpf;
            Vector3f tp = target.getLocation();
            Vector3f tp2 = target.getLocation().add(target.getVelocity().mult(dt));
            Vector3f pp = getLocation();
            Vector3f pp2 = getLocation().add(getVelocity().mult(dt));
            float d1 = tp.distance(pp);
            float d2 = tp2.distance(pp2);
            if (d1 > (d2 * tolerance)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    
    protected void decelerate() {
        //stop throttle
        throttle = 0;
        //get linear velocity
        Vector3f lVol = physics.getLinearVelocity();
        //apply counter force
        if (Math.abs(lVol.getX()) > STOP_LOW_VEL_BOUND) {
            float correction = -Math.signum(lVol.getX()) * thrust * STOP_CAUTION;
            physics.applyCentralForce(Vector3f.UNIT_X.mult(correction));
        } else {
            lVol = new Vector3f(0, lVol.getY(), lVol.getZ());
            physics.setLinearVelocity(lVol);
        }
        if (Math.abs(lVol.getY()) > STOP_LOW_VEL_BOUND) {
            float correction = -Math.signum(lVol.getY()) * thrust * STOP_CAUTION;
            physics.applyCentralForce(Vector3f.UNIT_Y.mult(correction));
        } else {
            lVol = new Vector3f(lVol.getX(), 0, lVol.getZ());
            physics.setLinearVelocity(lVol);
        }
        if (Math.abs(lVol.getZ()) > STOP_LOW_VEL_BOUND) {
            float correction = -Math.signum(lVol.getZ()) * thrust * STOP_CAUTION;
            physics.applyCentralForce(Vector3f.UNIT_Z.mult(correction));
        } else {
            lVol = new Vector3f(lVol.getX(), lVol.getY(), 0);
            physics.setLinearVelocity(lVol);
        }
    }
    
    protected void steer() {
        //clamp torque between -1 and 1
        if (pitch > 1) {
            pitch = 1;
        } else if (pitch < -1) {
            pitch = -1;
        }
        if (yaw > 1) {
            yaw = 1;
        } else if (yaw < -1) {
            yaw = -1;
        }
        if (roll > 1) {
            roll = 1;
        } else if (roll < -1) {
            roll = -1;
        }
        //steer
        physics.applyTorque(Vector3f.UNIT_X.mult(turning * pitch));
        physics.applyTorque(Vector3f.UNIT_Y.mult(turning * yaw));
        physics.applyTorque(Vector3f.UNIT_Z.mult(turning * roll));
        //reset steering
        pitch = 0;
        yaw = 0;
        roll = 0;
    }
    
    protected void thrust(float percent) {
        //thrust
        Vector3f direction = getRotation().mult(Vector3f.UNIT_Z);
        physics.applyCentralForce(direction.mult(thrust * -percent));
    }
    
    protected void reverseThrust(float percent) {
        //thrust
        Vector3f direction = getRotation().mult(Vector3f.UNIT_Z);
        physics.applyCentralForce(direction.mult(thrust * percent));
    }
    
    protected boolean seekTarget() {
        boolean safe = false;
        if (target != null) {
            if (target.getState() == State.ALIVE) {
                if (target.getCurrentSystem() == getCurrentSystem()) {
                    //make sure we have drag
                    physics.setAngularDamping(ANGULAR_DAMP);
                    /*
                     * Greedy algorithm
                     * Face the target and accelerate towards it.
                     */
                    Vector3f dat = getSteeringData(target.getPhysicsLocation().add(target.getVelocity().mult((float) tpf)), Vector3f.UNIT_Y);
                    safe = grossPointNoseAtVector(dat);
                } else {
                    //
                }
            } else {
                //
            }
        } else {
            //
        }
        return safe;
    }
    
    public void syncPhysics() {
        if (physics != null) {
            setLocation(physics.getPhysicsLocation());
            setRotation(physics.getPhysicsRotation());
            setVelocity(physics.getLinearVelocity());
        }
    }
    
    public float getAcceleration() {
        return thrust / getMass();
    }
    
    private boolean grossPointNoseAtVector(Vector3f dat) {
        boolean canAccel = false;
        //put controls in correct positions to face target
        pitch = -dat.x;
        yaw = -dat.y;
        roll = dat.z;
        if (Math.abs(dat.x - 1.5f) > FastMath.PI && Math.abs(dat.y - 1.5f) > FastMath.PI) {
            canAccel = true;
        }
        return canAccel;
    }
    
    private Vector3f getSteeringData(Vector3f worldPosition, Vector3f up) {
        if (emitter != null) {
            // RETREIVE LOCAL DIRECTION TO TARGET POSITION
            Vector3f steeringPosition = new Vector3f();
            physics.getPhysicsRotation().inverse().multLocal(steeringPosition.set(worldPosition).subtractLocal(physics.getPhysicsLocation()));

            // RETREIVE LOCAL UP VECTOR DIRECTION
            Vector3f upPosition = new Vector3f(up);
            physics.getPhysicsRotation().inverse().multLocal(upPosition);

            // CREATE 2D-VECTORS TO COMPARE
            Vector3f elevatorPos = new Vector3f(steeringPosition).normalizeLocal();
            elevatorPos.x = 0;
            Vector3f rudderPos = new Vector3f(steeringPosition).normalizeLocal();
            rudderPos.y = 0;
            Vector3f aileronPos = new Vector3f(upPosition).normalizeLocal();
            aileronPos.z = 0;

            // CALCULATE ANGLES BETWEEN VECTORS AND INVERT STEERING DIRECTION IF NEEDED
            Vector3f steeringData = new Vector3f();
            steeringData.x = Vector3f.UNIT_Z.angleBetween(elevatorPos);
            if (elevatorPos.y > 0) {
                steeringData.x *= -1;
            }
            steeringData.y = Vector3f.UNIT_Z.angleBetween(rudderPos);
            if (rudderPos.x < 0) {
                steeringData.y *= -1;
            }
            steeringData.z = Vector3f.UNIT_Y.angleBetween(aileronPos);
            if (aileronPos.x > 0) {
                steeringData.z *= -1;
            }

            // RETURN THE DATA
            return steeringData;
        } else {
            return Vector3f.ZERO;
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
    
    public boolean isGuided() {
        return isGuided;
    }
    
    public void setGuided(boolean isGuided) {
        this.isGuided = isGuided;
    }
    
    public Celestial getTarget() {
        return target;
    }
    
    public final void setTarget(Celestial target) {
        this.target = target;
    }
    
    public float getThrust() {
        return thrust;
    }
    
    public void setThrust(float accel) {
        this.thrust = accel;
    }
    
    public float getTurning() {
        return turning;
    }
    
    public void setTurning(float turning) {
        this.turning = turning;
    }
    
    public float getDelay() {
        return delay;
    }
    
    public void setDelay(float delay) {
        this.delay = delay;
    }

    public float getMaxLife() {
        return maxLife;
    }

    public void setMaxLife(float maxLife) {
        this.maxLife = maxLife;
    }

    public float getProximityFuse() {
        return proximityFuse;
    }

    public void setProximityFuse(float proximityFuse) {
        this.proximityFuse = proximityFuse;
    }
}
