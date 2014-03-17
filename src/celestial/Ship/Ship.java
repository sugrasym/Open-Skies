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
 * Represents a ship.
 */
package celestial.Ship;

import celestial.Celestial;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmeplanet.PlanetAppState;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Ship extends Celestial {

    public enum EngineMode {

        NORMAL,
        NEWTON
    }
    public static final float NORMAL_DAMP = 0.25f;
    public static final float NEWTON_DAMP = 0;
    private Term type;
    //health
    private float shield;
    private float shieldRecharge;
    private float hull;
    private float maxShield;
    private float maxHull;
    //fuel
    private float fuel;
    private float maxFuel;
    //fuel efficiency
    private float burnMultiplier = 1;
    //navigation
    transient Node core;
    transient Node nav;
    private EngineMode engine = EngineMode.NORMAL;
    private float throttle = 0;
    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;
    //sensor
    private float sensor;
    private Ship target;
    //physics stats
    private float thrust; //engine force
    private float torque; //turning force

    public Ship(Universe universe, Term type) {
        super(Float.parseFloat(type.getValue("mass")), universe);
        this.type = type;
        //init stats
        initStats();
        initNav();
    }

    private void initStats() {
        setThrust(Float.parseFloat(getType().getValue("thrust")));
        torque = Float.parseFloat(getType().getValue("torque"));
        setMaxShield(shield = Float.parseFloat(getType().getValue("shield")));
        shieldRecharge = Float.parseFloat(getType().getValue("shieldRecharge"));
        setMaxHull(hull = Float.parseFloat(getType().getValue("hull")));
        setMaxFuel(fuel = Float.parseFloat(getType().getValue("fuel")));
        setSensor(Float.parseFloat(getType().getValue("sensor")));
    }

    private void initNav() {
        core = new Node();
        nav = new Node();
        nav.move(Vector3f.UNIT_Z);
        core.attachChild(nav);
    }

    public void construct(AssetManager assets) {
        //Get name
        String name = getType().getValue("type");
        //load model
        spatial = assets.loadModel("Models/" + name + "/Model.blend");
        //load texture
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/" + name + "/tex.png"));
        //setup texture
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        spatial.setMaterial(mat);
        //setup physics
        CollisionShape hullShape = CollisionShapeFactory.createDynamicMeshShape(spatial);
        physics = new RigidBodyControl(hullShape, getMass());
        spatial.addControl(physics);
        physics.setSleepingThresholds(0, 0);
        physics.setAngularDamping(0.99f); //i do NOT want to deal with this at 0
        spatial.setName(this.getClass().getName());
        //store physics name control
        nameControl.setParent(this);
        spatial.addControl(nameControl);
    }

    public void deconstruct() {
        spatial = null;
        mat = null;
        physics = null;
    }

    public Vector3f getRotationAxis() {
        if (nav == null || core == null) {
            initNav();
        }
        /*
         * Returns a vector that represents a position vector being rotated
         * around the axis of the ship.
         */
        Vector3f eul = null;
        {
            core.setLocalRotation(getPhysicsRotation().clone());
            eul = nav.getWorldTranslation().clone();
        }
        return eul;
    }

    /*
     * Methods for in-system updating. It primarily uses the physics system.
     */
    protected void alive() {
        super.alive();
        //check health
        updateHealth();
        //check throttle
        updateThrottle();
        updateTorque();
    }

    protected void dying() {
        setState(State.DEAD);
    }

    protected void dead() {
        try {
            throw new Exception("Not yet implemented");
        } catch (Exception ex) {
            Logger.getLogger(Ship.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void updateHealth() {
        //recharge shield
        if (shield < maxShield) {
            shield += (shieldRecharge * tpf);
        }
        //bounds check
        if (shield < 0) {
            shield = 0;
        }
        if (hull <= 0) {
            setState(State.DYING);
        }
    }

    protected void updateThrottle() {
        /*
         * Checks to see if the throttle on the ship is up. If it is, then use
         * the correct engine. Also perform bound checks.
         */
        if (throttle < -1) {
            throttle = -1;
        } else if (throttle > 1) {
            throttle = 1;
        }
        if (throttle > 0) {
            fireRearThrusters(throttle);
        } else if (throttle < 0) {
            fireForwardThrusters(Math.abs(throttle));
        }
        /*
         * Without fuel you won't have any inertial engines so it makes sense
         * to drop to newton mode in that case
         */
        if (fuel <= 0) {
            setEngine(EngineMode.NEWTON);
            physics.setLinearDamping(NEWTON_DAMP);
        } else {
            /*
             * Update the drag coefficient based on whether we are accelerating.
             * When accelerating, drag is applied.
             */
            if (throttle != 0) {
                physics.setLinearDamping(NORMAL_DAMP);
            } else {
                physics.setLinearDamping(NEWTON_DAMP);
            }
        }
    }

    protected void updateTorque() {
        /*
         * Uses the pitch, yaw, and roll targets to rotate the
         * ship.
         */
        if (pitch < -1) {
            pitch = -1;
        } else if (pitch > 1) {
            pitch = 1;
        }
        pitch(pitch);
        if (yaw < -1) {
            yaw = -1;
        } else if (yaw > 1) {
            yaw = 1;
        }
        yaw(yaw);
        if (roll < -1) {
            roll = -1;
        } else if (roll > 1) {
            roll = 1;
        }
        roll(roll);
    }

    /*
     * Methods for out of system updating, does not use any of the physics system.
     */
    @Override
    protected void oosAlive() {
        super.oosAlive();
    }

    @Override
    protected void oosDying() {
        super.oosDying();
    }

    @Override
    protected void oosDead() {
        super.oosDead();
    }

    /*
     * For applying damage and buffs/debuffs
     */
    public void applyDamage(float damage) {
        /*
         * Some things apply damage uniformly to both shield and
         * hull.
         */
        shield -= damage;
        if (shield < 0) {
            hull += shield;
        }
    }

    public void applyDamage(float shieldDmg, float hullDmg) {
        /*
         * Some weapons will be better against shields than hull. This
         * damage applicator takes that into account.
         */
        shield -= shieldDmg;
        if (shield < 0) {
            hull -= hullDmg;
        }
    }

    /*
     * For applying thrust and torque
     */
    public void roll(float percent) {
        applyTorque(torque * percent, physics.getPhysicsRotation().mult(Vector3f.UNIT_Z));
    }

    public void yaw(float percent) {
        applyTorque(torque * percent, physics.getPhysicsRotation().mult(Vector3f.UNIT_Y));
    }

    public void pitch(float percent) {
        applyTorque(torque * percent, physics.getPhysicsRotation().mult(Vector3f.UNIT_X));
    }

    public void applyTorque(float force, Vector3f axis) {
        if (sufficientFuel(force)) {
            physics.applyTorque(axis.mult(force));
            useFuel(force);
        }
    }

    public void fireRearThrusters(float percent) {
        applyThrust(-getThrust() * percent);
    }

    public void fireForwardThrusters(float percent) {
        applyThrust(getThrust() * percent);
    }

    public void applyThrust(float force) {
        if (engine != EngineMode.NEWTON) {
            if (sufficientFuel(force)) {
                Vector3f direction = physics.getPhysicsRotation().mult(Vector3f.UNIT_Z);
                physics.applyCentralForce(direction.mult(force));
                useFuel(force);
            }
        }
    }

    /*
     * Adding and removing from the scene
     */
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        super.attach(node, physics, planetAppState);
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        super.detach(node, physics, planetAppState);
    }

    /*
     * For the camera
     */
    public Spatial getSpatial() {
        return spatial;
    }

    /*
     * Access and mutation
     */
    public float getThrust() {
        return thrust;
    }

    public void setThrust(float thrust) {
        this.thrust = thrust;
    }

    public float getShield() {
        return shield;
    }

    public void setShield(float shield) {
        this.shield = shield;
    }

    public float getHull() {
        return hull;
    }

    public void setHull(float hull) {
        this.hull = hull;
    }

    public float getMaxShield() {
        return maxShield;
    }

    public void setMaxShield(float maxShield) {
        this.maxShield = maxShield;
    }

    public float getMaxHull() {
        return maxHull;
    }

    public void setMaxHull(float maxHull) {
        this.maxHull = maxHull;
    }

    public float getFuel() {
        return fuel;
    }

    public void setFuel(float fuel) {
        this.fuel = fuel;
    }

    public float getMaxFuel() {
        return maxFuel;
    }

    public void setMaxFuel(float maxFuel) {
        this.maxFuel = maxFuel;
    }

    public float getSensor() {
        return sensor;
    }

    public void setSensor(float sensor) {
        this.sensor = sensor;
    }

    public float getThrottle() {
        return throttle;
    }

    public void setThrottle(float throttle) {
        this.throttle = throttle;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getShieldRecharge() {
        return shieldRecharge;
    }

    public void setShieldRecharge(float shieldRecharge) {
        this.shieldRecharge = shieldRecharge;
    }

    public Ship getTarget() {
        return target;
    }

    public void setTarget(Ship target) {
        this.target = target;
    }

    /*
     * Utility and reporting
     */
    @Override
    public String toString() {
        String ret = "";
        {
            ret = "(" + getType().getValue("type") + ") - " + getName() + ", " /*+ faction*/;
        }
        return ret;
    }

    public Term getType() {
        return type;
    }

    public EngineMode getEngine() {
        return engine;
    }

    public void setEngine(EngineMode engine) {
        this.engine = engine;
    }

    private void useFuel(float force) {
        fuel -= Math.abs(force * burnMultiplier) * tpf;
    }

    private boolean sufficientFuel(float force) {
        return fuel - Math.abs(force * burnMultiplier) * tpf >= 0;
    }
}
