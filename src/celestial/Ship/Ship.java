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

import cargo.DockingPort;
import cargo.Equipment;
import cargo.Hardpoint;
import cargo.Item;
import cargo.Weapon;
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
import entity.Entity;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmeplanet.PlanetAppState;
import lib.Faction;
import lib.astral.Parser.Term;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Ship extends Celestial {

    public static final double STOP_LOW_VEL_BOUND = 1;
    public static final float STOP_CAUTION = 0.25f;
    public enum EngineMode {

        NORMAL,
        NEWTON
    }
    public static final float NORMAL_DAMP = 0.26f;
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
    private boolean allStop = false;
    //central node
    transient Node center;
    //faction
    protected Faction faction;
    //docking
    protected boolean docked = false;
    private DockingPort port;
    //sensor
    private float sensor;
    private Ship target;
    //behavior targets
    protected Celestial flyToTarget;
    protected Station homeBase;
    //physics stats
    private float thrust; //engine force
    private float torque; //turning force
    //cargo
    protected double cargo;
    protected ArrayList<Item> cargoBay = new ArrayList<>();
    protected ArrayList<Hardpoint> hardpoints = new ArrayList<>();
    //money
    protected long cash = 0;

    public Ship(Universe universe, Term type, String faction) {
        super(Float.parseFloat(type.getValue("mass")), universe);
        this.type = type;
        //init stats
        initStats();
        initNav();
        initFaction(faction);
    }

    private void initStats() {
        setThrust(Float.parseFloat(getType().getValue("thrust")));
        torque = Float.parseFloat(getType().getValue("torque"));
        setMaxShield(shield = Float.parseFloat(getType().getValue("shield")));
        shieldRecharge = Float.parseFloat(getType().getValue("shieldRecharge"));
        setMaxHull(hull = Float.parseFloat(getType().getValue("hull")));
        setMaxFuel(fuel = Float.parseFloat(getType().getValue("fuel")));
        setSensor(Float.parseFloat(getType().getValue("sensor")));
        setCargo(Float.parseFloat(getType().getValue("cargo")));
        installHardpoints(getType());
    }

    private void initNav() {
        core = new Node();
        nav = new Node();
        nav.move(Vector3f.UNIT_Z);
        core.attachChild(nav);
    }

    private void initFaction(String name) {
        faction = new Faction(name);
    }

    public void construct(AssetManager assets) {
        //Get name
        String name = getType().getValue("type");
        //load model
        spatial = assets.loadModel("Models/" + name + "/Model.blend");
        //construct model and physics
        center = new Node();
        constructMaterial(assets, name);
        constructPhysics();
        //construct hardpoints
        constructHardpoints(assets);
    }

    public void deconstruct() {
        spatial = null;
        mat = null;
        physics = null;
    }

    protected void constructPhysics() {
        //setup physics
        CollisionShape hullShape = CollisionShapeFactory.createDynamicMeshShape(spatial);
        physics = new RigidBodyControl(hullShape, getMass());
        center.addControl(physics);
        physics.setSleepingThresholds(0, 0);
        physics.setAngularDamping(0.99f); //I do NOT want to deal with this at 0
        center.setName(this.getClass().getName());
        //store physics name control
        nameControl.setParent(this);
        center.addControl(nameControl);
    }

    protected void constructMaterial(AssetManager assets, String name) {
        //load texture
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/" + name + "/tex.png"));
        //setup texture
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        spatial.setMaterial(mat);
        //store
        center.attachChild(spatial);
    }

    protected void constructHardpoints(AssetManager assets) {
        for (int a = 0; a < hardpoints.size(); a++) {
            //initialize node
            hardpoints.get(a).initNode();
            //debug
            hardpoints.get(a).showDebugHardpoint(assets);
            //store node with spatial
            center.attachChild(hardpoints.get(a).getNode());
        }
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
        aliveAlways();
        //update center
        updateCenter();
        //check health
        updateHealth();
        //check throttle
        if (!allStop) {
            updateThrottle();
        } else {
            updateAllStop();
        }
        updateTorque();
        updateHardpoints();
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

    protected void aliveAlways() {
        /*
         * Contains methods to be called no matter if the ship is in system or
         * out of system
         */
        //sync standings
        if (faction.getName().equals(Faction.PLAYER)) {
            if (currentSystem.getUniverse() != null) {
                faction = currentSystem.getUniverse().getPlayerShip().getFaction();
                //messages = getUniverse().getPlayerShip().getMessages();
                //alternateString = true;
            }
        }
    }

    protected void updateCenter() {
        if (center == null) {
            center = new Node();
        }
        center.setLocalTranslation(physics.getPhysicsLocation());
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

    protected void updateAllStop() {
        //get linear velocity
        Vector3f lVol = physics.getLinearVelocity();
        if (lVol.length() > STOP_LOW_VEL_BOUND) {
            //apply counter force
            if (Math.abs(lVol.getX()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getX()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_X.mult(correction));
                    useFuel(correction);
                }
            } else {
                physics.setLinearVelocity(new Vector3f(0, lVol.getY(), lVol.getZ()));
            }
            if (Math.abs(lVol.getY()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getY()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_Y.mult(correction));
                    useFuel(correction);
                }
            } else {
                physics.setLinearVelocity(new Vector3f(lVol.getX(), 0, lVol.getY()));
            }
            if (Math.abs(lVol.getZ()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getZ()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_Z.mult(correction));
                    useFuel(correction);
                }
            } else {
                physics.setLinearVelocity(new Vector3f(lVol.getX(), lVol.getY(), 0));
            }
        } else {
            physics.setLinearVelocity(Vector3f.ZERO);
            //we're done
            allStop = false;
        }
    }

    protected void updateHardpoints() {
        //update hard points
        for (int a = 0; a < hardpoints.size(); a++) {
            hardpoints.get(a).periodicUpdate(tpf);
        }
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
        node.attachChild(center);
        physics.getPhysicsSpace().add(center);
        this.physics.setLinearVelocity(getVelocity().clone());
        this.physics.setPhysicsLocation(getLocation().clone());
        this.physics.setPhysicsRotation(getRotation().clone());
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        setVelocity(this.physics.getLinearVelocity().clone());
        setLocation(this.physics.getPhysicsLocation().clone());
        setRotation(this.physics.getPhysicsRotation().clone());
        node.detachChild(center);
        physics.getPhysicsSpace().remove(center);
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
            ret = "(" + getType().getValue("type") + ") - " + getName() + ", " + faction.getName();
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

    public boolean isAllStop() {
        return allStop;
    }

    public void setAllStop(boolean allStop) {
        this.allStop = allStop;
    }

    /*
     * Cargo code
     */
    public double getCargo() {
        return cargo;
    }

    public void setCargo(double cargo) {
        this.cargo = cargo;
    }

    public ArrayList<Item> getCargoBay() {
        return cargoBay;
    }

    public boolean addToCargoBay(Item item) {
        if (item != null) {
            /*
             * Puts an item into the cargo bay if there is space available.
             */
            double used = 0;
            for (int a = 0; a < cargoBay.size(); a++) {
                used += cargoBay.get(a).getVolume();
            }
            double fVol = 0;
            if (cargoBay.contains(item)) {
                fVol = item.getVolume() / item.getQuantity();
            } else {
                fVol = item.getVolume();
            }
            if ((cargo - used) > fVol) {
                if (!cargoBay.contains(item)) {
                    cargoBay.add(item);
                } else {
                    item.setQuantity(item.getQuantity() + 1);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void removeFromCargoBay(Item item) {
        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
        } else {
            cargoBay.remove(item);
        }
    }

    public int getNumInCargoBay(Item item) {
        int count = 0;
        if (item != null) {
            String iname = item.getName();
            String itype = item.getType();
            String group = item.getGroup();
            for (int a = 0; a < cargoBay.size(); a++) {
                Item tmp = cargoBay.get(a);
                if (iname.equals(tmp.getName())) {
                    if (itype.equals(tmp.getType())) {
                        if (group.equals(tmp.getGroup())) {
                            count += tmp.getQuantity();
                        }
                    }
                }
            }
        }
        return count;
    }

    public double getBayUsed() {
        double cmass = 0;
        for (int a = 0; a < cargoBay.size(); a++) {
            cmass += cargoBay.get(a).getVolume();
        }
        return cmass;
    }

    public boolean hasInCargo(Item item) {
        return cargoBay.contains(item);
    }

    public boolean hasInCargo(String item) {
        for (int a = 0; a < cargoBay.size(); a++) {
            if (cargoBay.get(a).getName().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGroupInCargo(String group) {
        for (int a = 0; a < cargoBay.size(); a++) {
            if (cargoBay.get(a).getGroup().equals(group)) {
                return true;
            }
        }
        return false;
    }

    public void addInitialCargo(String cargo) {
        if (cargo != null) {
            String[] stuff = cargo.split("/");
            for (int a = 0; a < stuff.length; a++) {
                String[] tb = stuff[a].split("~");
                Item tmp = new Item(tb[0]);
                int count = 1;
                if (tb.length == 2) {
                    count = Integer.parseInt(tb[1]);
                }
                for (int v = 0; v < count; v++) {
                    addToCargoBay(tmp);
                }
            }
        }
    }

    /*
     * Docking
     */
    public boolean isDocked() {
        return docked;
    }

    public void setDocked(boolean docked) {
        this.docked = docked;
    }
    
    public DockingPort getPort() {
        return port;
    }

    /*
     * Cash
     */
    public long getCash() {
        return cash;
    }

    public void setCash(long cash) {
        this.cash = cash;
    }

    /*
     * Hardpoints
     */
    protected void installHardpoints(Term relevant) throws NumberFormatException {
        /*
         * Equips the ship with hardpoints
         */
        String complex = relevant.getValue("hardpoint");
        if (complex != null) {
            String[] arr = complex.split("/");
            for (int a = 0; a < arr.length; a++) {
                String[] re = arr[a].split(",");
                String hType = re[0];
                int hSize = Integer.parseInt(re[1]);
                float hx = Float.parseFloat(re[2]);
                float hy = Float.parseFloat(re[3]);
                float hz = Float.parseFloat(re[4]);
                hardpoints.add(new Hardpoint(this, hType, hSize, new Vector3f(hx, hy, hz)));
            }
        }
    }

    /*
     * Fitting
     */
    public void fit(Equipment equipment) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (equipment.getQuantity() == 1) {
                if (hardpoints.get(a).isEmpty()) {
                    if (hardpoints.get(a).getSize() >= equipment.getVolume()) {
                        if (hardpoints.get(a).getType().equals(equipment.getType())) {
                            hardpoints.get(a).mount(equipment);
                            //is this a weapon?
                            Weapon wep = (Weapon) equipment;
                            //wep.initGraphics();
                            //remove from cargo
                            cargoBay.remove(equipment);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void unfit(Equipment equipment) {
        try {
            for (int a = 0; a < hardpoints.size(); a++) {
                if (hardpoints.get(a).getMounted() == equipment) {
                    if (getBayUsed() + equipment.getVolume() <= cargo) {
                        hardpoints.get(a).unmount(equipment);
                        cargoBay.add(equipment);
                    } else {
                        //not enough room
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Ship.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void fireActiveTurrets(Entity target) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).getType().equals(Item.TYPE_TURRET) || hardpoints.get(a).getType().equals(Item.TYPE_BATTERY)) {
                hardpoints.get(a).activate(target);
            }
        }
    }

    public void fireActiveGuns(Entity target) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).getType().equals(Item.TYPE_CANNON) || hardpoints.get(a).getType().equals(Item.TYPE_MISSILE)) {
                hardpoints.get(a).activate(target);
            }
        }
    }

    public void fireActiveModules() {
        fireActiveTurrets(target);
        fireActiveGuns(target);
    }

    public double getNearWeaponRange() {
        /*
         * Returns the range of the closest range onlined weapon.
         */
        double range = Double.MAX_VALUE;
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).isEnabled()) {
                if (hardpoints.get(a).notNothing()) {
                    if (hardpoints.get(a).getMounted().getRange() < range) {
                        range = hardpoints.get(a).getMounted().getRange();
                    }
                }
            }
        }
        return range;
    }

    public ArrayList<Hardpoint> getHardpoints() {
        return hardpoints;
    }

    /*
     * Faction standings checks
     */
    public int getStandingsToMe(Faction test) {
        return (int) faction.getStanding(test.getName());
    }

    public int getStandingsToMe(Ship ship) {
        if (ship.getFaction().getName().equals(Faction.PLAYER)) {
            return (int) ship.getFaction().getStanding(getFaction().getName());
        } else {
            return (int) faction.getStanding(ship.getFaction().getName());
        }
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    /*
     * Autopilot commands
     */
    public void cmdAbortDock() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void cmdDock(Station pick) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void cmdFightTarget(Ship pick) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setFlyToTarget(Celestial pick) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Celestial getFlyToTarget() {
        return flyToTarget;
    }

    public void cmdFlyToCelestial(Celestial flyToTarget, Double range) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void cmdFollowShip(Ship ship, Double range) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void cmdJump(SolarSystem pick) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Station getHomeBase() {
        return homeBase;
    }

    public void setHomeBase(Station homeBase) {
        this.homeBase = homeBase;
    }

    public void cmdUndock() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clearHomeBase() {
        homeBase = null;
    }
}
