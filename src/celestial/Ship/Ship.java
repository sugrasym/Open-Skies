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
import cargo.Nozzle;
import cargo.Weapon;
import celestial.Celestial;
import celestial.Explosion;
import celestial.Jumphole;
import celestial.Planet;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import entity.Entity;
import java.util.ArrayList;
import java.util.Random;
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

    /*
     * Behaviors are over-arching goals and motivations such as hunting down
     * hostiles or trading. The behave() method will keep track of any
     * variables it needs and call autopilot functions as needed to realize
     * these goals.
     */
    public enum Behavior {

        NONE,
        TEST,
        PATROL,
        SECTOR_TRADE,
        UNIVERSE_TRADE, //requires a jump drive
        SUPPLY_HOMEBASE, //requires a jump drive
        REPRESENT_HOMEBASE, //requires a jump drive
    }

    /*
     * Autopilot functions are slices of behavior that are useful as part of
     * a big picture.
     */
    public enum Autopilot {

        NONE, //nothing
        WAIT, //waiting
        WAITED, //done waiting
        DOCK_STAGE1, //get permission, go to the alignment vector
        DOCK_STAGE2, //fly into docking port
        UNDOCK, //fly out of docking port
        FLY_TO_CELESTIAL, //fly to a celestial
        ATTACK_TARGET, //attack current target
        ALL_STOP, //slow down until velocity is 0
        FOLLOW, //follow a target at a range
    }

    public static final double PATROL_REFUEL_PERCENT = 0.5;
    public static final double PLAYER_AGGRO_SHIELD = 0.5;
    public static final float STOP_LOW_VEL_BOUND = 1.0f;
    public static final float NAV_ANGLE_TOLERANCE = 0.02f;
    public static final float COM_ANGLE_TOLERANCE = 0.008f;
    public static final float ROLL_LOCK = FastMath.PI / 32;
    public static final float STOP_CAUTION = 1.0f;
    public static final float MAX_JUMP_SHIELD_DAMAGE = 0.45f;
    public static final float JUMP_SAFETY_FUEL = 0.25f;
    public static final float TRADER_JD_SAFETY_FUEL = 0.40f;
    public static final float OOS_VEL_LOWBOUND = 5f;
    public static final double TRADER_RESERVE_PERCENT = 0.5;
    public static final double TRADER_REFUEL_PERCENT = 0.25;
    public static final double MAX_WAIT_TIME = 25;
    public static final double MIN_WAIT_TIME = 5;

    public enum EngineMode {

        NORMAL,
        NEWTON
    }
    public static final float NORMAL_DAMP = 0.26f;
    public static final float NEWTON_DAMP = 0;
    public static final float ANGULAR_DAMP = 0.99f;
    private Term type;
    protected String _class;
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
    //weapons
    private boolean firing = false;
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
    //behavior and autopilot
    protected Autopilot autopilot = Autopilot.NONE;
    protected Behavior behavior = Behavior.NONE;
    //behavior targets
    protected Celestial flyToTarget;
    protected Station homeBase;
    private float range;
    private boolean scanForContraband = false;
    //trading
    private Station buyFromStation;
    private int buyFromPrice;
    private Station sellToStation;
    private int sellToPrice;
    private Item workingWare;
    //timing and waiting
    private double waitTimer = 0;
    private double waitTimerLength = 0;
    //physics stats
    private float thrust; //engine force
    private float torque; //turning force
    //cargo
    protected double cargo;
    protected ArrayList<Item> cargoBay = new ArrayList<>();
    protected ArrayList<Hardpoint> hardpoints = new ArrayList<>();
    protected ArrayList<Nozzle> nozzles = new ArrayList<>();
    //loadout
    private String template = "";
    //money
    protected long cash = 0;
    //courage
    private double courage = 1;
    //detecting aggro
    private Ship lastBlow = this;
    //RNG
    Random rnd = new Random();

    public Ship(Universe universe, Term type, String faction) {
        super(Float.parseFloat(type.getValue("mass")), universe);
        this.type = type;
        //init stats
        initStats();
        initNav();
        initFaction(faction);
        initCash();
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
        _class = getType().getValue("class");
        installHardpoints(getType());
        installNozzles(getType());
    }

    private void initNav() {
        core = new Node();
        nav = new Node();
        nav.move(Vector3f.UNIT_Z);
        core.attachChild(nav);
    }

    public void addInitialEquipment(String equip) {
        /*
         * Equips the ship with equipment from the starting loadout
         */
        //equip player from install keyword
        String[] arr = equip.split("/");
        for (int a = 0; a < arr.length; a++) {
            Item test = new Item(arr[a]);
            /*
             * Cannons and launchers are both in the weapon class
             */
            try {
                if (test.getType().equals("cannon") || test.getType().equals("missile")
                        || test.getType().equals("battery") || test.getType().equals("turret")) {
                    Weapon wep = new Weapon(arr[a]);
                    fit(wep);
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private void initFaction(String name) {
        faction = new Faction(name);
    }

    private void initCash() {
        if (faction.getName().matches(Faction.PLAYER)) {
            //do not do this
        } else {
            //randomize start cash
            setCash(new Random().nextInt(10000000));
        }
    }

    @Override
    public void construct(AssetManager assets) {
        //Get name
        String name = getType().getValue("type");
        //load spatial
        loadSpatial(assets, name);
        //construct model and physics
        center = new Node();
        constructMaterial(assets, name);
        constructPhysics();
        //construct hardpoints
        constructHardpoints(assets);
        constructNozzles(assets);
    }

    @Override
    public void deconstruct() {
        spatial = null;
        mat = null;
        physics = null;
    }

    protected void loadSpatial(AssetManager assets, String name) {
        //load model
        try {
            spatial = assets.loadModel("Models/Ships/" + _class + "/Model.blend");
        } catch (Exception e) {
            System.out.println("Error: Model for ship " + _class + " not found! Using placeholder.");
            spatial = assets.loadModel("Models/Ships/UnknownShip/Model.blend");
        }
    }

    protected void constructPhysics() {
        //setup physics
        CollisionShape hullShape = CollisionShapeFactory.createDynamicMeshShape(spatial);
        physics = new RigidBodyControl(hullShape, getMass());
        center.addControl(physics);
        physics.setSleepingThresholds(0, 0);
        physics.setAngularDamping(ANGULAR_DAMP); //I do NOT want to deal with this at 0
        center.setName(this.getClass().getName());
        //store physics name control
        nameControl.setParent(this);
        center.addControl(nameControl);
    }

    protected void constructMaterial(AssetManager assets, String name) {
        //load texture
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/Ships/" + _class + "/tex.png"));
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
            //hardpoints.get(a).showDebugHardpoint(assets);
            //construct
            hardpoints.get(a).construct(assets);
            //store node with spatial
            center.attachChild(hardpoints.get(a).getNode());
        }
    }

    protected void constructNozzles(AssetManager assets) {
        for (int a = 0; a < nozzles.size(); a++) {
            //initialize node
            nozzles.get(a).initNode();
            //debug
            //nozzles.get(a).showDebugHardpoint(assets);
            //construct
            nozzles.get(a).construct(assets);
            //store node with spatial
            center.attachChild(nozzles.get(a).getNode());
        }
    }

    public Vector3f getRotationAxis() {
        if (physics != null) {
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
        } else {
            return null;
        }
    }

    /*
     * Methods for autopilot in-system
     */
    private void autopilot() {
        if (autopilot == Autopilot.NONE) {
        } else if (autopilot == Autopilot.FLY_TO_CELESTIAL) {
            autopilotFlyToCelestial();
        } else if (autopilot == Autopilot.ALL_STOP) {
            autopilotAllStop();
        } else if (autopilot == Autopilot.DOCK_STAGE1) {
            autopilotDockStageOne();
        } else if (autopilot == Autopilot.DOCK_STAGE2) {
            autopilotDockStageTwo();
        } else if (autopilot == Autopilot.UNDOCK) {
            autopilotUndock();
        } else if (autopilot == Autopilot.ATTACK_TARGET) {
            autopilotFightTarget();
        } else if (autopilot == Autopilot.WAIT) {
            autopilotWaitBlock();
        } else if (autopilot == Autopilot.FOLLOW) {
            autopilotFollow();
        }
    }

    private void autopilotFollow() {
        if (getAutopilot() == Autopilot.FOLLOW) {
            if (flyToTarget != null) {
                if (flyToTarget.getCurrentSystem() == currentSystem) {
                    double dist = distanceTo(flyToTarget);
                    if (dist > sensor) {
                        //no aim
                        cmdAbort();
                    } else {
                        if (dist < (range)) {
                            //back off
                            throttle = -1;
                        } else {
                            if (dist > (range * 4)) {
                                moveToPositionWithHold(flyToTarget.getPhysicsLocation(), getFollowHold());
                            } else {
                                //wait
                                throttle = 0;
                            }
                        }
                    }
                } else {
                    //determine if this is a system we could jump to
                    SolarSystem targetSystem = flyToTarget.getCurrentSystem();
                    if (canJump(targetSystem)) {
                        //jump to follow target
                        cmdJump(targetSystem);
                    } else {
                        //abort follow
                        cmdAllStop();
                    }
                }
            } else {
                autopilot = Autopilot.NONE;
            }
        }
    }

    private void autopilotFightTarget() {
        if (target != null) {
            if (target.getState() == State.ALIVE) {
                if (target.getCurrentSystem() == getCurrentSystem()) {
                    if (inSensorRange(target)) {
                        double distance = target.getLocation().distance(getLocation());
                        double minRange = getNearWeaponRange();
                        //rotate to face target
                        Vector3f solution = target.getLocation();
                        Vector3f steering = getSteeringData(solution, Vector3f.UNIT_Y);
                        boolean aligned = finePointNoseAtVector(steering, COM_ANGLE_TOLERANCE);
                        //keep at range
                        if (aligned) {
                            if (distance < (minRange / 3)) {
                                /*
                                 * The enemy is getting too close to the ship, so fire the reverse
                                 * thrusters.
                                 */
                                throttle = -1;
                            } else if (distance > (minRange / 2) && distance < (2 * minRange / 3)) {
                                /*
                                 * The enemy is getting too far away from the ship, fire the forward
                                 * thrusters.
                                 */
                                throttle = 1;
                            } else if (distance > (minRange)) {
                                /*
                                 * The enemy is out of weapons minRange and needs to be approached
                                 */
                                float dP = 0;
                                float d1 = getLocation().subtract(target.getLocation()).length();
                                Vector3f dv1 = getLocation().add(getLinearVelocity().mult((float) tpf));
                                Vector3f dv2 = target.getLocation().add(target.getLinearVelocity().mult((float) tpf));
                                float d2 = dv1.subtract(dv2).length();
                                dP = d2 - d1;
                                if (dP + (getAcceleration()) > 0) {
                                    throttle = 1;
                                } else {
                                    throttle = 0;
                                }
                            } else {
                                throttle = 0;
                            }
                            if (distance < minRange) {
                                fireActiveGuns(target);
                            }
                        }
                        if (distance < minRange) {
                            fireActiveTurrets(target);
                        }
                    } else {
                        cmdAbort();
                    }
                } else {
                    cmdAbort();
                }
            } else {
                cmdAbort();
            }
        } else {
            cmdAbort();
        }
    }

    private void autopilotUndock() {
        //get the docking align
        Vector3f align = port.getAlign().getWorldTranslation();
        //fly towards it
        moveToPositionWithHold(align, Float.POSITIVE_INFINITY);
        //abort when hold is reached
        if (physics.getLinearVelocity().length() >= DockingPort.DOCK_SPEED_LIMIT) {
            //all done
            cmdAbortDock();
        }
    }

    private void autopilotDockStageOne() {
        //make sure we have a flyToTarget
        if (flyToTarget != null) {
            //make sure it is a station
            if (flyToTarget instanceof Station && flyToTarget.getState() == State.ALIVE) {
                //make sure we can actually dock there
                Station tmp = (Station) flyToTarget;
                if (tmp.getCurrentSystem() == currentSystem) {
                    if (port == null) {
                        //get the docking port to use
                        port = tmp.requestDockingPort(this);
                    } else {
                        //get the docking align
                        Vector3f align = port.getAlign().getWorldTranslation();
                        //fly to it
                        float distance = align.distance(physics.getPhysicsLocation());
                        float velocity = physics.getLinearVelocity().length();
                        if (distance < port.getSize() / 2) {
                            autopilotAllStop();
                            if (velocity == 0 || autopilot == Autopilot.NONE) {
                                //next stage
                                setAutopilot(Autopilot.DOCK_STAGE2);
                            }
                        } else {
                            //determine correct hold to use
                            float hold = 0;
                            if (distance <= getFlightHold()) {
                                hold = distance;
                            } else {
                                hold = getFlightHold();
                            }
                            //move to position
                            moveToPositionWithHold(align, hold);
                            //detect if autopilot kicked off
                            if (autopilot == Autopilot.NONE) {
                                /*
                                 * moveToPosition() detects when the ship has stopped
                                 * moving and corrects itself by turning off the autopilot.
                                 */
                                setAutopilot(Autopilot.DOCK_STAGE1);
                            } else {
                                //do nothing, we are still on autopilot
                            }
                        }
                    }
                } else {
                    cmdAbortDock();
                }
            } else {
                cmdAbortDock();
            }
        } else {
            cmdAbortDock();
        }
    }

    private void autopilotDockStageTwo() {
        //make sure we have a flyToTarget
        if (flyToTarget != null) {
            //make sure it is a station
            if (flyToTarget instanceof Station && flyToTarget.getState() == State.ALIVE) {
                //make sure we can actually dock there
                Station tmp = (Station) flyToTarget;
                if (tmp.getCurrentSystem() == currentSystem) {
                    if (port == null) {
                        //abort because this is stage 2
                        cmdAbortDock();
                    } else {
                        //get the docking port
                        Vector3f dock = port.getNode().getWorldTranslation();
                        //get the hold
                        float hold = DockingPort.DOCK_SPEED_LIMIT / 2;
                        //fly to it
                        moveToPositionWithHold(dock, hold);
                        //detect if autopilot kicked off
                        if (autopilot == Autopilot.NONE) {
                            setAutopilot(Autopilot.DOCK_STAGE2);
                        } else {
                            if (physics.getLinearVelocity().length() > 0) {
                                //stop rotation
                                pitch = 0;
                                yaw = 0;
                                roll = 0;
                            }
                        }
                    }
                } else {
                    cmdAbortDock();
                }
            } else {
                cmdAbortDock();
            }
        } else {
            cmdAbortDock();
        }
    }

    protected void autopilotWaitBlock() {
        if (autopilot == Autopilot.WAIT) {
            waitTimer += tpf;
            if (waitTimer >= waitTimerLength) {
                autopilot = Autopilot.WAITED;
            }
        }
    }

    protected void autopilotAllStop() {
        //kill rotation
        pitch = 0;
        yaw = 0;
        roll = 0;
        //stop throttle
        throttle = 0;
        //get linear velocity
        Vector3f lVol = physics.getLinearVelocity();
        if (lVol.length() > getAcceleration()) {
            //use reverse thrusters
            Vector3f thrustTarget = getSteeringData(getPhysicsLocation().add(lVol), Vector3f.UNIT_Y);
            boolean safe = pointNoseAtVector(thrustTarget, NAV_ANGLE_TOLERANCE);
            if (safe) {
                throttle = -1;
            }
        } else if (lVol.length() > STOP_LOW_VEL_BOUND) {
            //apply counter force
            if (Math.abs(lVol.getX()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getX()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_X.mult(correction));
                    useFuel(correction);
                }
            } else {
                lVol = new Vector3f(0, lVol.getY(), lVol.getZ());
                physics.setLinearVelocity(lVol);
            }
            if (Math.abs(lVol.getY()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getY()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_Y.mult(correction));
                    useFuel(correction);
                }
            } else {
                lVol = new Vector3f(lVol.getX(), 0, lVol.getZ());
                physics.setLinearVelocity(lVol);
            }
            if (Math.abs(lVol.getZ()) > STOP_LOW_VEL_BOUND) {
                float correction = -Math.signum(lVol.getZ()) * thrust * STOP_CAUTION;
                if (sufficientFuel(correction)) {
                    physics.applyCentralForce(Vector3f.UNIT_Z.mult(correction));
                    useFuel(correction);
                }
            } else {
                lVol = new Vector3f(lVol.getX(), lVol.getY(), 0);
                physics.setLinearVelocity(lVol);
            }
        } else {
            physics.setLinearVelocity(Vector3f.ZERO);
            //we're done
            setAutopilot(Autopilot.NONE);
        }
    }

    private void autopilotFlyToCelestial() {
        if (flyToTarget == null) {
            //abort
            cmdAbort();
        } else {
            if (flyToTarget.getCurrentSystem() != getCurrentSystem()) {
                cmdAbort();
            } else {
                if (flyToTarget.getState() != State.ALIVE) {
                    cmdAbort();
                } else {
                    float dist = flyToTarget.getPhysicsLocation().distance(getPhysicsLocation());
                    if (dist < range) {
                        //stop the ship, we're there
                        cmdAllStop();
                    } else {
                        //determine correct hold to use
                        float hold = 0;
                        if (dist <= getFlightHold()) {
                            hold = dist;
                        } else {
                            hold = getFlightHold();
                        }
                        //move to position
                        moveToPositionWithHold(flyToTarget.getPhysicsLocation(), hold);
                        //detect if autopilot kicked off
                        if (autopilot == Autopilot.NONE) {
                            /*
                             * moveToPosition() detects when the ship has stopped
                             * moving and corrects itself by turning off the autopilot.
                             *
                             * Since we aren't here yet, we need to re-issue the command
                             * to fine tune our approach to the target.
                             */
                            cmdFlyToCelestial(flyToTarget, range);
                        } else {
                            //do nothing, we are still on autopilot
                        }
                    }
                }
            }
        }
    }

    private void moveToPosition(Vector3f end) {
        /*
         * Maintains compatibility with most flight methods.
         */
        moveToPositionWithHold(end, getFlightHold());
    }

    private void moveToPositionWithHold(Vector3f end, float hold) {
        Vector3f b = end.clone();
        //safety
        boolean canAccel = true;
        //see if we are there
        float dist = end.distance(getPhysicsLocation());
        if (dist < hold && hold != Float.POSITIVE_INFINITY) {
            autopilotAllStop();
        } else {
            //get steering to face target
            Vector3f dat = getSteeringData(b, Vector3f.UNIT_Y);
            //make sure we aren't getting further from the target
            Vector3f dPos = physics.getPhysicsLocation().add(physics.getLinearVelocity());
            float stepDistance = dPos.distance(b);
            float distance = physics.getPhysicsLocation().distance(b);
            if (stepDistance > distance) {
                //we are moving further away, all stop
                autopilotAllStop();
            } else {
                canAccel = pointNoseAtVector(dat, NAV_ANGLE_TOLERANCE);
                if (canAccel) {
                    if (physics.getLinearVelocity().length() < hold) {
                        throttle = 1;
                    } else {
                        throttle = 0;
                    }
                } else {
                    throttle = 0;
                    //do nothing
                }
            }
        }
    }

    /*
     * Methods for behaviors.
     * Behaviors are required to be functional either in or out of system and
     * since they only call autopilot functions they should be independent of
     * such low level code.
     */
    private void behave() {
        if (behavior == Behavior.NONE) {
        } else if (behavior == Behavior.TEST) {
            behaviorTest();
        } else if (behavior == Behavior.SECTOR_TRADE) {
            behaviorSectorTrade();
        } else if (behavior == Behavior.UNIVERSE_TRADE) {
            behaviorUniverseTrade();
        } else if (behavior == Behavior.PATROL) {
            behaviorPatrol();
        }
    }

    protected void behaviorPatrol() {
        if (!docked) {
            if ((fuel / maxFuel) > PATROL_REFUEL_PERCENT) {
                //target nearest enemy
                targetNearestHostileShip();
                if (target == null) {
                    targetNearestHostileStation();
                }
            }
            //handle what we got
            if (target == null) {
                /*
                 * Resume the patrol. Pick a station to fly within sensor
                 * range of and fly to it. If fuel is less than 50% go dock
                 * so it is replenished.
                 */
                if (autopilot == Autopilot.NONE) {
                    //fuel check
                    if ((fuel / maxFuel) <= PATROL_REFUEL_PERCENT) {
                        //dock at the nearest friendly station
                        Station near = getNearestDockableStationInSystem();
                        if (near != null) {
                            cmdDock(near);
                            System.out.println(getName() + " [P] is low on fuel and docking at "
                                    + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                        } else {
                            leaveSystem();
                        }
                    } else {
                        /*
                         * Get a random planet or station in system. Stations
                         * are preferred but if there aren't many available then
                         * fly to planets as well.
                         */
                        double pick = rnd.nextFloat();
                        Celestial near = null;
                        if (currentSystem.getStationList().size() < 4) {
                            if (pick <= 0.5) {
                                near = getRandomStationInSystem();
                            } else {
                                near = getRandomPlanetInSystem();
                            }
                        } else {
                            near = getRandomStationInSystem();
                        }
                        if (near != null) {
                            //fly within sensor range
                            float range = sensor;
                            cmdFlyToCelestial(near, range);
                        } else {
                            leaveSystem();
                        }
                    }
                } else {
                    //wait
                }
            } else {
                //fight current target
                if (target.isHostileToMe(this) || scanForContraband(target) || target == lastBlow) {
                    cmdFightTarget(target);
                }
            }
        } else {
            //undock
            cmdUndock();
        }
    }

    protected void behaviorUniverseTrade() {
        /*
         * Buy low sell high within systems within jump range of each other.
         */
        if (!docked) {
            if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                /*
                 * 1. Get a list of friendly stations to collate wares from
                 * 2. Build a list of all wares that can be traded in jumpable sectors
                 * 3. Find the one with the highest profit.
                 * 4. Fill up on the ware.
                 * 5. Drop off the ware.
                 * repeat
                 */
                if (getNumInCargoBay(workingWare) > 0) {
                    /*
                     * There are wares to be sold, this is stage 2.
                     */
                    if (canJump(sellToStation.getCurrentSystem())) {
                        if (sellToStation.getCurrentSystem() != currentSystem) {
                            cmdJump(sellToStation.getCurrentSystem());
                        }
                        cmdDock(sellToStation);
                    } else {
                        abortTrade();
                        leaveSystem();
                    }
                } else {
                    /*
                     * This is stage 1, find the best deal.
                     */
                    //get a list of friendly stations
                    ArrayList<Station> friendly = new ArrayList<>();
                    ArrayList<SolarSystem> zone = new ArrayList<>();
                    for (int a = 0; a < currentSystem.getUniverse().getSystems().size(); a++) {
                        if (canJump(currentSystem.getUniverse().getSystems().get(a))) {
                            ArrayList<Station> tmp = getDockableStationsInSystem(currentSystem.getUniverse().getSystems().get(a));
                            zone.add(currentSystem.getUniverse().getSystems().get(a));
                            friendly.addAll(tmp);
                        }
                    }
                    if (friendly.size() > 1) {
                        //build a list of wares that are being produced
                        ArrayList<String> produced = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationSelling();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName().toString();
                                if (!produced.contains(ware)) {
                                    produced.add(ware);
                                }
                            }
                        }
                        //build a list of wares that are being consumed
                        ArrayList<String> consumed = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationBuying();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName().toString();
                                if (!consumed.contains(ware)) {
                                    consumed.add(ware);
                                }
                            }
                        }
                        //cross reference the lists to find what's the same in both
                        ArrayList<String> sample = new ArrayList<>();
                        for (int a = 0; a < consumed.size(); a++) {
                            for (int b = 0; b < produced.size(); b++) {
                                if (consumed.get(a).equals(produced.get(b))) {
                                    sample.add(consumed.get(a));
                                    break;
                                }
                            }
                        }
                        //make sure there's a sample
                        if (sample.size() > 0) {
                            Station buyLoc = null;
                            Station sellLoc = null;
                            Item bestWare = null;
                            double gain = 0;
                            for (int a = 0; a < sample.size(); a++) {
                                Item ware = new Item(sample.get(a));
                                //get the best stations
                                Station pickUp = getBestPickup(zone, ware);
                                Station dropOff = getBestDropOff(zone, ware);
                                //get prices
                                if (pickUp != null && dropOff != null) {
                                    int pickUpPrice = pickUp.getPrice(ware);
                                    int dropOffPrice = dropOff.getPrice(ware);
                                    //find profit
                                    int profit = dropOffPrice - pickUpPrice;
                                    if (pickUpPrice != -1 && dropOffPrice != -1) {
                                        if (profit > 0) {
                                            if (profit > gain) {
                                                buyLoc = pickUp;
                                                sellLoc = dropOff;
                                                bestWare = ware;
                                                //store prices
                                                gain = profit;
                                                buyFromPrice = pickUpPrice;
                                                sellToPrice = dropOffPrice;
                                            }
                                        } else {
                                            //no point in trading this
                                        }
                                    }
                                } else {
                                    //something went wrong
                                }
                            }
                            if (bestWare != null) {
                                //store start and end
                                buyFromStation = buyLoc;
                                sellToStation = sellLoc;
                                workingWare = bestWare;
                                //start trading
                                if (canJump(buyFromStation.getCurrentSystem())) {
                                    if (buyFromStation.getCurrentSystem() != currentSystem) {
                                        cmdJump(buyFromStation.getCurrentSystem());
                                    }
                                    cmdDock(buyFromStation);
                                } else {
                                    abortTrade();
                                    leaveSystem();
                                }
                            } else {
                                /*
                                 * Universe traders roam the universe
                                 */
                                leaveSystem();
                            }
                        } else {
                            //maybe profit awaits us elsewhere
                            leaveSystem();
                        }
                    } else {
                        //profit definately awaits us elsewhere
                        leaveSystem();
                    }
                }
            } else {
                if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                    //dock at the nearest friendly station
                    Station near = getNearestDockableStationInSystem();
                    if (near != null) {
                        cmdDock(near);
                        System.out.println(getName() + " [UT] is low on fuel and docking at "
                                + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                    } else {
                        leaveSystem();
                    }
                } else {
                    //wait;
                }
            }
        } else {
            //setup wait
            if (autopilot == Autopilot.NONE && port != null) {
                //restore fuel
                fuel = maxFuel;
                //do buying and selling
                Station curr = port.getParent();
                if (curr == buyFromStation) {
                    //make sure the price is still ok
                    if ((curr.getPrice(workingWare) <= buyFromPrice)
                            && (sellToStation.getPrice(workingWare) >= sellToPrice)
                            && canJump(sellToStation.getCurrentSystem())) {
                        //how much of the ware can we carry
                        int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) workingWare.getVolume());
                        //how much can we carry if we want to follow reserve rules
                        int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                        //buy as much as we can carry
                        curr.buy(this, workingWare, q);
                        System.out.println(getName() + " bought " + getNumInCargoBay(workingWare)
                                + " " + workingWare.getName() + " from " + curr.getName());
                    } else {
                        //abort trading operation
                        abortTrade();
                        System.out.println(getName() + " aborted trading operation.");
                    }
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else if (curr == sellToStation) {
                    if (curr.getPrice(workingWare) >= sellToPrice) {
                        //try to dump all our wares at this price
                        int q = getNumInCargoBay(workingWare);
                        curr.sell(this, workingWare, q);
                        System.out.println(getName() + " sold " + (q - getNumInCargoBay(workingWare))
                                + " " + workingWare.getName() + " to " + curr.getName());
                    } else {
                        //System.out.println(getName() + " did not sell (Bad sell price)");
                    }
                    //wait
                    if (getNumInCargoBay(workingWare) == 0) {
                        double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                        double delt = rnd.nextDouble() * diff;
                        cmdWait(MIN_WAIT_TIME + delt);
                    } else {
                        //not everything sold yet
                    }
                } else {
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                }
            } //finally undock when waiting is over
            else if (autopilot == Autopilot.WAITED) {
                if (getNumInCargoBay(workingWare) > 0) {
                    cmdUndock();
                } else {
                    cmdUndock();
                }
            } else if (port == null) {
                abortTrade();
                cmdUndock();
            } else {
                //do nothing
            }
        }
    }

    protected void behaviorSectorTrade() {
        /*
         * Buy low sell high within one solar system.
         */
        if (!docked) {
            if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                /*
                 * 1. Get a list of friendly stations to collate wares from
                 * 2. Build a list of all wares that can be traded in the
                 * sector (a ware must have both a buyer and a seller)
                 * 3. Find the one with the highest profit.
                 * 4. Fill up on the ware.
                 * 5. Drop off the ware.
                 * repeat
                 */
                if (getNumInCargoBay(getWorkingWare()) > 0) {
                    /*
                     * There are wares to be sold, this is stage 2.
                     */
                    cmdDock(getSellToStation());
                } else {
                    /*
                     * This is stage 1, find the best deal.
                     */
                    //get a list of friendly stations
                    ArrayList<Station> friendly = getDockableStationsInSystem();
                    if (friendly.size() > 1) {
                        //build a list of wares that are being produced
                        ArrayList<String> produced = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationSelling();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName().toString();
                                if (!produced.contains(ware)) {
                                    produced.add(ware);
                                }
                            }
                        }
                        //build a list of wares that are being consumed
                        ArrayList<String> consumed = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationBuying();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName().toString();
                                if (!consumed.contains(ware)) {
                                    consumed.add(ware);
                                }
                            }
                        }
                        //cross reference the lists to find what's the same in both
                        ArrayList<String> sample = new ArrayList<>();
                        for (int a = 0; a < consumed.size(); a++) {
                            for (int b = 0; b < produced.size(); b++) {
                                if (consumed.get(a).equals(produced.get(b))) {
                                    sample.add(consumed.get(a));
                                    break;
                                }
                            }
                        }
                        //make sure there's a sample
                        if (sample.size() > 0) {
                            Station buyLoc = null;
                            Station sellLoc = null;
                            Item bestWare = null;
                            double gain = 0;
                            for (int a = 0; a < sample.size(); a++) {
                                Item ware = new Item(sample.get(a));
                                //get the best stations
                                ArrayList<SolarSystem> curr = new ArrayList<>();
                                curr.add(currentSystem);
                                Station pickUp = getBestPickup(curr, ware);
                                Station dropOff = getBestDropOff(curr, ware);
                                //get prices
                                if (pickUp != null && dropOff != null) {
                                    int pickUpPrice = pickUp.getPrice(ware);
                                    int dropOffPrice = dropOff.getPrice(ware);
                                    //find profit
                                    int profit = dropOffPrice - pickUpPrice;
                                    if (pickUpPrice != -1 && dropOffPrice != -1) {
                                        if (profit > 0) {
                                            if (profit > gain) {
                                                buyLoc = pickUp;
                                                sellLoc = dropOff;
                                                bestWare = ware;
                                                //store prices
                                                gain = profit;
                                                setBuyFromPrice(pickUpPrice);
                                                setSellToPrice(dropOffPrice);
                                            }
                                        } else {
                                            //no point in trading this
                                        }
                                    }
                                } else {
                                    //something went wrong
                                }
                            }
                            if (bestWare != null) {
                                //store start and end
                                setBuyFromStation(buyLoc);
                                setSellToStation(sellLoc);
                                setWorkingWare(bestWare);
                                //start trading
                                cmdDock(getBuyFromStation());
                            } else {
                                handleNoSectorTrades();
                            }
                        } else {
                            handleNoSectorTrades();
                        }
                    } else {
                        handleNoSectorTrades();
                    }
                }
            } else {
                if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                    //dock at the nearest friendly station
                    Station near = getNearestDockableStationInSystem();
                    if (near != null) {
                        cmdDock(near);
                        System.out.println(getName() + " [ST] is low on fuel and docking at "
                                + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                    } else {
                        leaveSystem();
                    }
                } else {
                    //wait;
                }
            }
        } else {
            if (autopilot == Autopilot.NONE && port != null) {
                //restore fuel
                fuel = maxFuel;
                //do buying and selling
                Station curr = port.getParent();
                if (curr == getBuyFromStation()) {
                    //make sure the price is still ok
                    if ((curr.getPrice(getWorkingWare()) <= getBuyFromPrice()) && (getSellToStation().getPrice(getWorkingWare()) >= getSellToPrice())) {
                        //how much of the ware can we carry
                        int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) getWorkingWare().getVolume());
                        //how much can we carry if we want to follow reserve rules
                        int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                        //buy as much as we can carry
                        curr.buy(this, getWorkingWare(), q);
                        System.out.println(getName() + " bought " + getNumInCargoBay(getWorkingWare())
                                + " " + getWorkingWare().getName() + " from " + curr.getName());
                    } else {
                        //abort trading operation
                        abortTrade();
                        System.out.println(getName() + " aborted trading operation (Bad buy price)");
                    }
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else if (curr == getSellToStation()) {
                    if (curr.getPrice(getWorkingWare()) >= getSellToPrice()) {
                        //try to dump all our wares at this price
                        int q = getNumInCargoBay(getWorkingWare());
                        curr.sell(this, getWorkingWare(), q);
                        System.out.println(getName() + " sold " + (q - getNumInCargoBay(getWorkingWare()))
                                + " " + getWorkingWare().getName() + " to " + curr.getName());
                    } else {
                        //System.out.println(getName() + " did not sell (Bad sell price)");
                    }
                    //wait
                    if (getNumInCargoBay(getWorkingWare()) == 0) {
                        double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                        double delt = rnd.nextDouble() * diff;
                        cmdWait(MIN_WAIT_TIME + delt);
                    } else {
                        //not everything sold yet
                    }
                } else {
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                }
            } else if (autopilot == Autopilot.WAITED) {
                //finally undock
                cmdUndock();
            } else if (port == null) {
                abortTrade();
                cmdUndock();
            } else {

            }
        }
    }

    private void handleNoSectorTrades() {
        if (isPlayerFaction()) {
            //todo: investigate and make sure that the ship can undock afterwards
            dockAtFriendlyStationInSystem();
        } else {
            /*
             * I honestly don't give a damn if some random NPC trader dies.
             * It probably keeps the universe more interesting.
             */
            leaveSystem();
        }
    }

    protected void behaviorTest() {
        if (autopilot == Autopilot.NONE) {
            //target nearest ship
            targetNearestShip();
            cmdFightTarget(target);
        } else {
            //fighting!
        }
    }

    /*
     * Contraband
     */
    private boolean scanForContraband(Ship ship) {
        /*
         * Only used for detecting contraband being carried by the
         * player.
         */
        if (ship.isPlayerFaction()) {
            if (scanForContraband) {
                ArrayList<Item> sc = ship.getCargoBay();
                for (int a = 0; a < sc.size(); a++) {
                    if (faction.isContraband(sc.get(a).getName())) {
                        /*//notify the player
                         if (conversation == null) {
                         if (myFaction.getContrabandNotifications().size() > 0) {
                         String pick = myFaction.getContrabandNotifications().
                         get(rnd.nextInt(myFaction.getContrabandNotifications().size()));
                         conversation = new Conversation(this, "Contraband " + sc.get(a).getName(), pick);
                         }
                         }*/
                        //return true
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /*
     * Methods for autopilot out of system
     */
    private void oosAutopilot() {
        if (autopilot == Autopilot.NONE) {
        } else if (autopilot == Autopilot.FLY_TO_CELESTIAL) {
            oosAutopilotFlyToCelestial();
        } else if (autopilot == Autopilot.ALL_STOP) {
            oosAutopilotAllStop();
        } else if (autopilot == Autopilot.DOCK_STAGE1) {
            oosAutopilotDockStageOne();
        } else if (autopilot == Autopilot.DOCK_STAGE2) {
            oosAutopilotDockStageTwo();
        } else if (autopilot == Autopilot.UNDOCK) {
            oosAutopilotUndock();
        } else if (autopilot == Autopilot.ATTACK_TARGET) {
            oosAutopilotFightTarget();
        } else if (autopilot == Autopilot.WAIT) {
            oosAutopilotWaitBlock();
        } else if (autopilot == Autopilot.FOLLOW) {
            oosAutopilotFollow();
        }
    }

    private void oosAutopilotFollow() {
        if (getAutopilot() == Autopilot.FOLLOW) {
            if (flyToTarget != null) {
                if (flyToTarget.getCurrentSystem() == currentSystem) {
                    double dist = distanceTo(flyToTarget);
                    if (dist > sensor) {
                        //no aim
                        cmdAbort();
                    } else {
                        if (dist < (range)) {
                            //back off
                            throttle = -1;
                        } else {
                            if (dist > (range * 4)) {
                                oosMoveToPositionWithHold(flyToTarget.getLocation(), getFollowHold());
                            } else {
                                //wait
                                throttle = 0;
                            }
                        }
                    }
                } else {
                    //determine if this is a system we could jump to
                    SolarSystem targetSystem = flyToTarget.getCurrentSystem();
                    if (canJump(targetSystem)) {
                        //jump to follow target
                        cmdJump(targetSystem);
                    } else {
                        //abort follow
                        cmdAllStop();
                    }
                }
            } else {
                autopilot = Autopilot.NONE;
            }
        }
    }

    protected void oosAutopilotWaitBlock() {
        if (autopilot == Autopilot.WAIT) {
            waitTimer += tpf;
            if (waitTimer >= waitTimerLength) {
                autopilot = Autopilot.WAITED;
            }
        }
    }

    private void oosAutopilotFightTarget() {
        if (target != null) {
            if (target.getState() == State.ALIVE) {
                if (target.getCurrentSystem() == getCurrentSystem()) {
                    if (inSensorRange(target)) {
                        double distance = target.getLocation().distance(getLocation());
                        double minRange = getNearWeaponRange();
                        //keep at range
                        if (distance < (minRange / 3)) {
                            /*
                             * The enemy is getting too close to the ship, so fire the reverse
                             * thrusters.
                             */
                            oosBurn(target.getLocation(), getAcceleration() * -1);
                        } else if (distance > (minRange / 2) && distance < (2 * minRange / 3)) {
                            /*
                             * The enemy is getting too far away from the ship, fire the forward
                             * thrusters.
                             */
                            oosBurn(target.getLocation(), getAcceleration() * 1);
                        } else if (distance > (minRange)) {
                            /*
                             * The enemy is out of weapons minRange and needs to be approached
                             */
                            float dP = 0;
                            float d1 = getLocation().subtract(target.getLocation()).length();
                            Vector3f dv1 = getLocation().add(getVelocity().mult((float) tpf));
                            Vector3f dv2 = target.getLocation().add(target.getVelocity().mult((float) tpf));
                            float d2 = dv1.subtract(dv2).length();
                            dP = d2 - d1;
                            if (dP + (getAcceleration()) > 0) {
                                oosBurn(target.getLocation(), getAcceleration() * 1);
                            } else {
                                throttle = 0;
                            }
                        } else {
                            throttle = 0;
                        }
                        if (distance < minRange) {
                            fireActiveGuns(target);
                            fireActiveTurrets(target);
                        }
                    } else {
                        cmdAbort();
                    }
                } else {
                    cmdAbort();
                }
            } else {
                cmdAbort();
            }
        } else {
            cmdAbort();
        }
    }

    private void oosAutopilotAllStop() {
        /*
         * OOS objects do not have rotation and behave as points.
         */
        float v = getVelocity().length();
        if (v > OOS_VEL_LOWBOUND) {
            //calculate acceleration in this tick
            float a = getAcceleration() * (float) tpf;
            //check for sufficient fuel for accelerating in this tick
            if (sufficientFuel(getThrust())) {
                //the assumption is that we are rotated the same as the velocity
                Vector3f rot = getVelocity().normalize();
                Vector3f del = rot.mult(-a);
                //apply the acceleration for this tick
                setVelocity(getVelocity().add(del));
                //use fuel
                useFuel(getThrust());
            } else {
                /*
                 * This is less than ideal. There is not enough fuel to finish
                 * the stop. This means the ship is adrift.
                 *
                 * Player ships are set adrift, NPC ships are removed.
                 */
                if (isPlayerFaction()) {
                    setAutopilot(Autopilot.NONE);
                } else {
                    setState(State.DYING);
                }
            }
        } else {
            //consider this an all stop
            setVelocity(Vector3f.ZERO);
            setAutopilot(Autopilot.NONE);
        }
    }

    private void oosAutopilotFlyToCelestial() {
        if (flyToTarget == null) {
            //abort
            cmdAbort();
        } else {
            if (flyToTarget.getCurrentSystem() != getCurrentSystem()) {
                cmdAbort();
            } else {
                if (flyToTarget.getState() != State.ALIVE) {
                    cmdAbort();
                } else {
                    float dist = flyToTarget.getLocation().distance(getLocation());
                    if (dist < range) {
                        //stop the ship, we're there
                        cmdAllStop();
                    } else {
                        //determine correct hold to use
                        float hold = 0;
                        if (dist <= getFlightHold()) {
                            hold = dist;
                        } else {
                            hold = getFlightHold();
                        }
                        //move to position
                        oosMoveToPositionWithHold(flyToTarget.getLocation(), hold);
                        //detect if autopilot kicked off
                        if (autopilot == Autopilot.NONE) {
                            /*
                             * moveToPosition() detects when the ship has stopped
                             * moving and corrects itself by turning off the autopilot.
                             *
                             * Since we aren't here yet, we need to re-issue the command
                             * to fine tune our approach to the target.
                             */
                            cmdFlyToCelestial(flyToTarget, range);
                        } else {
                            //do nothing, we are still on autopilot
                        }
                    }
                }
            }
        }
    }

    private void oosAutopilotDockStageOne() {
        //make sure we have a flyToTarget
        if (flyToTarget != null) {
            //make sure it is a station
            if (flyToTarget instanceof Station && flyToTarget.getState() == State.ALIVE) {
                //make sure we can actually dock there
                Station tmp = (Station) flyToTarget;
                if (tmp.getCurrentSystem() == currentSystem) {
                    if (port == null) {
                        //get the docking port to use
                        port = tmp.requestDockingPort(this);
                    } else {
                        //get the "docking align"
                        Vector3f align = port.rawAlignPosition();
                        //fly to it
                        float distance = align.distance(getLocation());
                        float velocity = getVelocity().length();
                        if (distance < port.getSize() / 2) {
                            oosAutopilotAllStop();
                            if (velocity == 0 || autopilot == Autopilot.NONE) {
                                //next stage
                                setAutopilot(Autopilot.DOCK_STAGE2);
                            }
                        } else {
                            //determine correct hold to use
                            float hold = 0;
                            if (distance <= getFlightHold()) {
                                hold = distance;
                            } else {
                                hold = getFlightHold();
                            }
                            //move to position
                            oosMoveToPositionWithHold(align, hold);
                            //detect if autopilot kicked off
                            if (autopilot == Autopilot.NONE) {
                                /*
                                 * moveToPosition() detects when the ship has stopped
                                 * moving and corrects itself by turning off the autopilot.
                                 */
                                setAutopilot(Autopilot.DOCK_STAGE1);
                            } else {
                                //do nothing, we are still on autopilot
                            }
                        }
                    }
                } else {
                    cmdAbortDock();
                }
            } else {
                cmdAbortDock();
            }
        } else {
            cmdAbortDock();
        }
    }

    private void oosAutopilotDockStageTwo() {
        //make sure we have a flyToTarget
        if (flyToTarget != null) {
            //make sure it is a station
            if (flyToTarget instanceof Station && flyToTarget.getState() == State.ALIVE) {
                //make sure we can actually dock there
                Station tmp = (Station) flyToTarget;
                if (tmp.getCurrentSystem() == currentSystem) {
                    if (port == null) {
                        //abort because this is stage 2
                        cmdAbortDock();
                    } else {
                        //get the docking port
                        Vector3f dock = port.rawPortPosition();
                        //get the hold
                        float hold = DockingPort.DOCK_SPEED_LIMIT / 2;
                        //fly to it
                        oosMoveToPositionWithHold(dock, hold);
                        //detect if autopilot kicked off
                        if (autopilot == Autopilot.NONE) {
                            setAutopilot(Autopilot.DOCK_STAGE2);
                        } else {
                            if (getVelocity().length() > 0) {
                                //stop rotation
                                pitch = 0;
                                yaw = 0;
                                roll = 0;
                            }
                        }
                    }
                } else {
                    cmdAbortDock();
                }
            } else {
                cmdAbortDock();
            }
        } else {
            cmdAbortDock();
        }
    }

    private void oosAutopilotUndock() {
        //get the docking align
        Vector3f align = port.rawAlignPosition();
        //fly towards it
        oosMoveToPositionWithHold(align, Float.POSITIVE_INFINITY);
        //abort when hold is reached
        if (getVelocity().length() >= DockingPort.DOCK_SPEED_LIMIT) {
            //all done
            cmdAbortDock();
        }
    }

    private void oosMoveToPosition(Vector3f end) {
        oosMoveToPositionWithHold(end, getFlightHold());
    }

    private void oosMoveToPositionWithHold(Vector3f end, float hold) {
        Vector3f b = end.clone();
        //see if we are there
        float dist = end.distance(getLocation());
        if (dist < hold && hold != Float.POSITIVE_INFINITY) {
            oosAutopilotAllStop();
        } else {
            //make sure we aren't getting further from the target
            Vector3f dPos = getLocation().add(getVelocity());
            float stepDistance = dPos.distance(b);
            float distance = getLocation().distance(b);
            if (stepDistance > distance) {
                //we are moving further away, all stop
                oosAutopilotAllStop();
            } else {
                if (getVelocity().length() < hold) {
                    //calculate acceleration in this tick
                    float a = getAcceleration() * (float) tpf;
                    //check for sufficient fuel for accelerating in this tick
                    if (sufficientFuel(getThrust())) {
                        oosBurn(end, a);
                    }
                } else {
                    //do nothing
                }
            }
        }
    }

    private void oosBurn(Vector3f end, float acceleration) {
        /*
         * OOS, objects do not have a real rotation. For the
         * acceleration we will assume the ship is rotated
         * to face the object.
         */
        Vector3f dif = end.subtract(getLocation());
        Vector3f rot = dif.normalize();
        Vector3f del = rot.mult(acceleration);
        //apply the acceleration for this tick
        setVelocity(getVelocity().add(del));
        //use fuel
        useFuel(getThrust());
    }

    /*
     * Methods that can be used no matter what system it is in
     */
    protected void aliveAlways() {
        /*
         * Contains methods to be called no matter if the ship is in system or
         * out of system
         */
        //sync standings
        if (isPlayerFaction()) {
            if (currentSystem.getUniverse() != null) {
                faction = currentSystem.getUniverse().getPlayerShip().getFaction();
                //messages = getUniverse().getPlayerShip().getMessages();
            }
        }
        //check docking updates
        if (docked) {
            //make sure the station still exists
            if (port != null) {
                if (port.getParent() != null) {
                    if (port.getParent().getState() == State.ALIVE) {
                        //no autopilot unless undocking
                        if (autopilot != Autopilot.UNDOCK
                                && autopilot != Autopilot.WAITED
                                && autopilot != Autopilot.WAIT) {
                            setAutopilot(Autopilot.NONE);
                        }
                        //refuel
                        fuel = maxFuel;
                    } else {
                        cmdUndock();
                    }
                } else {
                    cmdUndock();
                }
            } else {
                cmdUndock();
            }
        } else {
            //fire weapons if needed
            if (firing) {
                fireActiveModules();
            }
        }
        //update targeting
        updateTarget();
        //update hardpoints
        updateHardpoints();
        //update health
        updateHealth();
        //behave
        behave();
    }

    protected void updateTarget() {
        if (target != null) {
            if (target.getState() != State.ALIVE) {
                target = null;
            } else if (target.getCurrentSystem() != getCurrentSystem()) {
                target = null;
            } else if (target.getLocation().distance(getLocation()) > getSensor()) {
                target = null;
            }
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
            System.out.println(getName() + " was destroyed in " + currentSystem.getName() + " by " + getLastBlow().getName());
            //did the player destroy this ship?
            if (getLastBlow().getFaction().getName().equals(Faction.PLAYER)) {
                //adjust the player's standings accordingly
                if (!faction.getName().equals("Neutral")) {
                    getCurrentSystem().getUniverse().getPlayerShip().getFaction().derivedModification(faction, -1.0);
                }
            }
            setState(State.DYING);
        }
    }

    protected void updateHardpoints() {
        //update hard points
        for (int a = 0; a < hardpoints.size(); a++) {
            hardpoints.get(a).periodicUpdate(tpf);
        }
    }

    /*
     * Methods for in-system updating. It primarily uses the physics system.
     */
    protected void alive() {
        super.alive();
        aliveAlways();
        if (physicsSafe()) {
            //update center
            updateCenter();
            //update autopilot
            autopilot();
            //check throttle
            updateThrottle();
            updateTorque();
            updateNozzles();
            syncPhysics();
        }
    }

    protected void dying() {
        if (physicsSafe()) {
            dropExplosion();
            setState(State.DEAD);
        }
    }

    protected void dead() {
        if (physicsSafe()) {
            //nothing to do really
        }
    }

    protected void updateCenter() {
        if (center == null) {
            center = new Node();
        }
        center.setLocalTranslation(physics.getPhysicsLocation());
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

    protected void updateNozzles() {
        for (int a = 0; a < nozzles.size(); a++) {
            nozzles.get(a).periodicUpdate(tpf);
        }
    }

    /*
     * Methods for out of system updating, does not use any of the physics system.
     */
    @Override
    protected void oosAlive() {
        super.oosAlive();
        aliveAlways();
        oosAutopilot();
        //update position
        Vector3f dP = getVelocity().mult((float) tpf);
        setLocation(getLocation().add(dP));
    }

    @Override
    protected void oosDying() {
        super.oosDying();
        setState(State.DEAD);
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
        if (!isPlayerFaction()) {
            ret = "(" + getType().getValue("type") + ") - " + getName();
        } else {
            if (this.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem() == getCurrentSystem()) {
                ret = "(" + getType().getValue("type") + ") - " + getName();
            } else {
                ret = "(" + getType().getValue("type") + ") - " + getName() + ", " + getCurrentSystem().getName();
            }
        }
        return ret;
    }

    public boolean isPlayerFaction() {
        return faction.getName().equals(Faction.PLAYER);
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

    protected void installNozzles(Term relevant) throws NumberFormatException {
        String complex = relevant.getValue("nozzle");
        if (complex != null) {
            String rawStart = relevant.getValue("nozzleStartColor");
            String rawEnd = relevant.getValue("nozzleEndColor");
            String[] arr = complex.split("/");
            for (int a = 0; a < arr.length; a++) {
                String[] re = arr[a].split(",");
                String hType = re[0];
                int hSize = Integer.parseInt(re[1]);
                float hx = Float.parseFloat(re[2]);
                float hy = Float.parseFloat(re[3]);
                float hz = Float.parseFloat(re[4]);
                nozzles.add(new Nozzle(this, hType, hSize, new Vector3f(hx, hy, hz), rawStart, rawEnd));
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

    public boolean isHostileToMe(Ship ship) {
        if (getStandingsToMe(ship) <= Faction.HOSTILE_STANDING) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isFriendlyToMe(Ship ship) {
        if (getStandingsToMe(ship) >= Faction.FRIENDLY_STANDING) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isNeutralToMe(Ship ship) {
        boolean hostile = isHostileToMe(ship);
        boolean friendly = isFriendlyToMe(ship);
        return !hostile && !friendly;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    /*
     * Trading Helpers
     */
    public Station getBestDropOff(ArrayList<SolarSystem> systems, Item ware) {
        Station ret = null;
        {
            Station bStation = null;
            int bPrice = 0;
            for (int b = 0; b < systems.size(); b++) {
                ArrayList<Station> friendly = getDockableStationsInSystem(systems.get(b));
                if (friendly.size() > 0) {
                    for (int a = 0; a < friendly.size(); a++) {
                        Station test = friendly.get(a);
                        if (test.buysWare(ware)) {
                            if (bStation == null) {
                                bStation = test;
                                bPrice = test.getPrice(ware);
                            } else {
                                int nP = test.getPrice(ware);
                                if (nP > bPrice) {
                                    bStation = test;
                                }
                            }
                        }
                    }
                }
            }
            ret = bStation;
        }
        return ret;
    }

    public Station getBestPickup(ArrayList<SolarSystem> systems, Item ware) {
        Station ret = null;
        {
            Station bStation = null;
            int bPrice = 0;
            for (int b = 0; b < systems.size(); b++) {
                ArrayList<Station> friendly = getDockableStationsInSystem(systems.get(b));
                if (friendly.size() > 0) {
                    for (int a = 0; a < friendly.size(); a++) {
                        Station test = friendly.get(a);
                        if (test.sellsWare(ware)) {
                            if (bStation == null) {
                                bStation = test;
                                bPrice = test.getPrice(ware);
                            } else {
                                int nP = test.getPrice(ware);
                                if (nP < bPrice) {
                                    bStation = test;
                                }
                            }
                        }
                    }
                }
            }
            ret = bStation;
        }
        return ret;
    }

    /*
     * Autopilot commands
     * NOTE: These functions need to remain safe to call whether the ship is
     * in or out of the player's system. This means no access to physics, spatials,
     * or nodes.
     */
    public Autopilot getAutopilot() {
        return autopilot;
    }

    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    private void dockAtFriendlyStationInSystem() {
        abortTrade();
        //wait
        ArrayList<Station> fstat = getDockableStationsInSystem();
        Station near = fstat.get(rnd.nextInt(fstat.size()));
        if (near != null) {
            cmdDock(near);
        } else {
            cmdAllStop();
        }
    }

    private void leaveSystem() {
        /*
         * Finds a random jump hole and flies through it.
         */
        Jumphole njmp = getRandomJumpholeInSystem();
        cmdFlyToCelestial(njmp, 0);
    }

    private void abortTrade() {
        //end trade
        autopilot = Autopilot.NONE;
        setBuyFromStation(null);
        setSellToStation(null);
        setWorkingWare(null);
        setBuyFromPrice(0);
        setSellToPrice(0);
    }

    public void cmdAbort() {
        setAutopilot(Autopilot.NONE);
        if (port != null) {
            port.release();
            port = null;
        }
        throttle = 0;
        pitch = 0;
        yaw = 0;
        roll = 0;
    }

    public void cmdAllStop() {
        setAutopilot(Autopilot.ALL_STOP);
        if (port != null) {
            port.release();
            port = null;
        }
    }

    public void cmdAbortDock() {
        cmdAbort();
        if (port != null) {
            port.release();
            port = null;
        }
    }

    public void cmdDock(Station pick) {
        if (!docked) {
            //TODO: Make this a real behavior
            port = pick.requestDockingPort(this);
            if (port != null) {
                flyToTarget = pick;
                setAutopilot(Autopilot.DOCK_STAGE1);
            }
        }
    }

    public void cmdWait(double duration) {
        autopilot = Autopilot.WAIT;
        waitTimerLength = duration;
        waitTimer = 0;
    }

    public void cmdFightTarget(Ship pick) {
        target = pick;
        setAutopilot(Autopilot.ATTACK_TARGET);
    }

    public void setFlyToTarget(Celestial pick) {
        flyToTarget = pick;
    }

    public Celestial getFlyToTarget() {
        return flyToTarget;
    }

    public void cmdFlyToCelestial(Celestial flyToTarget, float range) {
        setAutopilot(Autopilot.FLY_TO_CELESTIAL);
        if (flyToTarget instanceof Planet) {
            //add radius to range for safety
            Planet tmp = (Planet) flyToTarget;
            range += tmp.getRadius();
        }
        setFlyToTarget(flyToTarget);
        //store range
        setRange(range);
    }

    public void cmdFollowShip(Ship ship, float range) {
        this.range = range;
        flyToTarget = ship;
        setAutopilot(Autopilot.FOLLOW);
    }

    public void cmdJump(SolarSystem pick) {
        if (canJump(pick)) {
            Random rnd = new Random();
            //drop jump effect
            dropJumpEffect();
            //determine fuel cost
            double fuelCost = getJumpFuelCost(pick);
            //deduct fuel
            fuel -= fuelCost;
            //pull from old system
            currentSystem.pullEntityFromSystem(this);
            //randomize location
            float x = rnd.nextInt(60000 * 2) - 60000;
            float y = rnd.nextInt(60000 * 2) - 60000;
            float z = rnd.nextInt(60000 * 2) - 60000;
            setLocation(new Vector3f(x, y, z));
            //put in new system
            pick.putEntityInSystem(this);
            //drop the jump effect
            dropJumpEffect();
        }
    }

    public Station getHomeBase() {
        return homeBase;
    }

    public void setHomeBase(Station homeBase) {
        this.homeBase = homeBase;
    }

    public void cmdUndock() {
        if (docked) {
            setDocked(false);
            setAutopilot(Autopilot.UNDOCK);
        }
    }

    public void clearHomeBase() {
        homeBase = null;
    }

    /*
     * Syncing physics for docking
     */
    public void setPhysicsLocation(Vector3f loc) {
        physics.setPhysicsLocation(loc);
    }

    public void nullVelocity() {
        physics.setLinearVelocity(Vector3f.ZERO);
        physics.setAngularVelocity(Vector3f.ZERO);
    }

    /*
     * Syncing physics for saving
     */
    public void syncPhysics() {
        if (physics != null) {
            setLocation(physics.getPhysicsLocation());
            setRotation(physics.getPhysicsRotation());
            setVelocity(physics.getLinearVelocity());
        }
    }

    /*
     * Safety check to make sure we have physics and we have graphics.
     * Both are needed for in-system updates to succeed.
     */
    private boolean physicsSafe() {
        return physics != null && currentSystem.hasGraphics();
    }

    /*
     * Range controls for autopilot functions
     */
    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    /*
     * Methods to determine "holds" which are AI limits on velocity based
     * on the acceleration of the craft and what the craft is doing
     */
    protected float getFlightHold() {
        return 3 * getAcceleration();
    }

    protected float getFollowHold() {
        return Float.POSITIVE_INFINITY;
    }

    /*
     * Methods to determine physics values
     */
    public float magnitude(float dx, float dy) {
        return (float) Math.sqrt((dx * dx) + (dy * dy));
    }

    public float getAcceleration() {
        return thrust / getMass();
    }

    public float getAngularAcceleration() {
        return torque / getMass();
    }

    public boolean inTolerance(float n1, float n2, float tolerance) {
        return (n1 >= n2 * (1 - tolerance) && n1 <= n2 * (1 + tolerance));
    }

    private Vector3f getSteeringData(Vector3f worldPosition, Vector3f up) {
        // RETREIVE LOCAL DIRECTION TO TARGET POSITION
        Vector3f steeringPosition = new Vector3f();
        getSpatial().getWorldRotation().inverse().multLocal(steeringPosition.set(worldPosition).subtractLocal(getSpatial().getWorldTranslation()));

        // RETREIVE LOCAL UP VECTOR DIRECTION
        Vector3f upPosition = new Vector3f(up);
        getSpatial().getWorldRotation().inverse().multLocal(upPosition);

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
    }

    private boolean finePointNoseAtVector(Vector3f dat, float tolerance) {
        boolean canAccel = true;
        //put controls in correct positions to face target
        if (Math.abs(dat.x) < FastMath.PI * (1 - tolerance)) {
            pitch = -(dat.x);
            canAccel = false;
        } else {
            pitch = -(dat.x) / 50.0f;
        }
        if (Math.abs(dat.y) < FastMath.PI * (1 - tolerance)) {
            yaw = -(dat.y);
            canAccel = false;
        } else {
            yaw = -(dat.y) / 50.0f;
        }
        if (Math.abs(dat.z) > FastMath.PI * tolerance) {
            roll = (dat.z);
            //canAccel = false;
        } else {
            //roll = (dat.z) / 100.0f;
            roll = 0;
        }
        return canAccel;
    }

    private boolean pointNoseAtVector(Vector3f dat, float tolerance) {
        boolean canAccel = true;
        //put controls in correct positions to face target
        if (Math.abs(dat.x) < FastMath.PI * (1 - tolerance)) {
            pitch = -(dat.x);
            canAccel = false;
        } else {
            pitch = 0;
        }
        if (Math.abs(dat.y) < FastMath.PI * (1 - tolerance)) {
            yaw = -(dat.y);
            canAccel = false;
        } else {
            yaw = 0;
        }
        if (Math.abs(dat.z) > FastMath.PI * tolerance) {
            roll = (dat.z);
            //canAccel = false;
        } else {
            roll = 0;
        }
        return canAccel;
    }

    /*
     * Used to find groups of objects
     */
    public boolean inSensorRange(Celestial celestial) {
        if (celestial.getLocation().distance(getLocation()) <= sensor) {
            return true;
        } else {
            return false;
        }
    }

    public void targetNearestShip() {
        //get a list of all nearby ships
        ArrayList<Entity> nearby = getCurrentSystem().getShipList();
        ArrayList<Ship> ships = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            if (nearby.get(a) instanceof Ship) {
                Ship tmp = (Ship) nearby.get(a);
                if (tmp != this) {
                    //make sure it is alive
                    if (tmp.getState() == State.ALIVE) {
                        //make sure it is in range
                        if (tmp.getLocation().distance(getLocation()) < getSensor()) {
                            ships.add(tmp);
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < ships.size(); a++) {
            if (closest == null) {
                closest = ships.get(a);
            } else {
                double distClosest = closest.getLocation().distance(getLocation());
                double distTest = ships.get(a).getLocation().distance(getLocation());
                if (distTest < distClosest) {
                    closest = ships.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestNeutralShip() {
        target = null;
        //get a list of all nearby ships
        ArrayList<Ship> nearby = getShipsInSensorRange();
        ArrayList<Ship> neutrals = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            if (nearby.get(a) instanceof Ship) {
                if (tmp != this) {
                    //make sure it is alive and isn't docked
                    if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                        //check standings
                        if (tmp.isNeutralToMe(this)) {
                            neutrals.add(tmp);
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < neutrals.size(); a++) {
            if (closest == null) {
                closest = neutrals.get(a);
            } else {
                double distClosest = closest.getLocation().distance(getLocation());
                double distTest = neutrals.get(a).getLocation().distance(getLocation());
                if (distTest < distClosest) {
                    closest = neutrals.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestFriendlyShip() {
        target = null;
        //get a list of all nearby ships
        ArrayList<Ship> nearby = getShipsInSensorRange();
        ArrayList<Ship> friendlies = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            if (nearby.get(a) instanceof Ship) {
                if (tmp != this) {
                    //make sure it is alive and isn't docked
                    if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                        //check standings
                        if (tmp.isFriendlyToMe(this)) {
                            friendlies.add(tmp);
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < friendlies.size(); a++) {
            if (closest == null) {
                closest = friendlies.get(a);
            } else {
                double distClosest = closest.getLocation().distance(getLocation());
                double distTest = friendlies.get(a).getLocation().distance(getLocation());
                if (distTest < distClosest) {
                    closest = friendlies.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestHostileShip() {
        target = null;
        //get a list of all nearby ships
        ArrayList<Ship> nearby = getShipsInSensorRange();
        ArrayList<Ship> hostiles = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            if (nearby.get(a) instanceof Ship) {
                if (tmp != this) {
                    //make sure it is alive and isn't docked
                    if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                        //check standings
                        if (tmp.isHostileToMe(this)) {
                            hostiles.add(tmp);
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < hostiles.size(); a++) {
            if (closest == null) {
                closest = hostiles.get(a);
            } else {
                double distClosest = closest.getLocation().distance(getLocation());
                double distTest = hostiles.get(a).getLocation().distance(getLocation());
                if (distTest < distClosest) {
                    closest = hostiles.get(a);
                }
            }
        }
        //see if it's being beaten on by the player
        if (shield / maxShield < PLAYER_AGGRO_SHIELD) {
            if (!isPlayerFaction()) {
                if (lastBlow == currentSystem.getUniverse().getPlayerShip()) {
                    if (closest != null) {
                        double distClosest = closest.getLocation().distance(getLocation());
                        double distTest = currentSystem.getUniverse().getPlayerShip().getLocation().distance(getLocation());
                        if (distTest < distClosest) {
                            closest = currentSystem.getUniverse().getPlayerShip();
                        }
                    } else {
                        closest = currentSystem.getUniverse().getPlayerShip();
                    }
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestHostileStation() {
        target = null;
        Station closest = null;
        //get a list of all nearby hostiles
        ArrayList<Entity> nearby = getCurrentSystem().getStationList();
        ArrayList<Station> hostiles = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Station tmp = (Station) nearby.get(a);
            //make sure it is in range
            if (distanceTo(tmp) < getSensor()) {
                //make sure it is alive
                if (tmp.getState() == State.ALIVE) {
                    //check standings
                    if (tmp.isHostileToMe(this)) {
                        hostiles.add(tmp);
                    }
                }
            }
        }
        //target the nearest one
        if (hostiles.size() > 0) {
            closest = hostiles.get(0);
            for (int a = 0; a < hostiles.size(); a++) {
                if (closest == null) {
                    closest = hostiles.get(a);
                } else {
                    double distClosest = distanceTo(closest);
                    double distTest = distanceTo(hostiles.get(a));
                    if (distTest < distClosest) {
                        closest = hostiles.get(a);
                    }
                }
            }
        } else {
            //nothing to target
        }
        //store
        target = closest;
    }

    public ArrayList<Ship> getShipsInSensorRange() {
        ArrayList<Ship> ret = new ArrayList<>();
        {
            //get ship list
            ArrayList<Entity> ships = currentSystem.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp != this) {
                    if (tmp.getLocation().distance(getLocation()) < sensor) {
                        ret.add(tmp);
                    }
                }
            }
            //get station list
            ArrayList<Entity> stations = currentSystem.getStationList();
            for (int a = 0; a < stations.size(); a++) {
                Station tmp = (Station) stations.get(a);
                if (tmp.getLocation().distance(getLocation()) < sensor) {
                    ret.add(tmp);
                }
            }
        }
        return ret;
    }

    public ArrayList<Station> getDockableStationsInSystem() {
        return getDockableStationsInSystem(currentSystem);
    }

    public ArrayList<Station> getDockableStationsInSystem(SolarSystem system) {
        ArrayList<Station> list = new ArrayList<>();
        {
            ArrayList<Entity> stations = system.getStationList();
            if (stations.size() > 0) {
                for (int a = 0; a < stations.size(); a++) {
                    Station test = (Station) stations.get(a);
                    if (test.canDock(this)) {
                        list.add(test);
                    }
                }
            }
        }
        return list;
    }

    public Station getNearestDockableStationInSystem() {
        Station ret = null;
        {
            ArrayList<Station> stations = getDockableStationsInSystem();
            if (stations.size() > 0) {
                Station closest = stations.get(0);
                for (int a = 0; a < stations.size(); a++) {
                    Station test = (Station) stations.get(a);
                    double old = closest.getLocation().distance(getLocation());
                    double next = test.getLocation().distance(getLocation());
                    if (next < old) {
                        closest = test;
                    }
                }
                ret = closest;
            } else {
                return null;
            }
        }
        //final check
        if (ret.canDock(this)) {
            return ret;
        } else {
            return null;
        }
    }

    public Station getRandomStationInSystem() {
        Station ret = null;
        {
            ArrayList<Entity> stations = currentSystem.getStationList();
            if (stations.size() > 0) {
                ret = (Station) stations.get(rnd.nextInt(stations.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public Celestial getRandomCelestialInSystem() {
        Celestial ret = null;
        {
            ArrayList<Entity> celestials = currentSystem.getCelestials();
            if (celestials.size() > 0) {
                ret = (Celestial) celestials.get(rnd.nextInt(celestials.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public Jumphole getRandomJumpholeInSystem() {
        Jumphole ret = null;
        {
            ArrayList<Entity> jumpHoles = currentSystem.getJumpholeList();
            if (jumpHoles.size() > 0) {
                ret = (Jumphole) jumpHoles.get(rnd.nextInt(jumpHoles.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public Planet getRandomPlanetInSystem() {
        Planet ret = null;
        {
            ArrayList<Entity> planets = currentSystem.getPlanetList();
            if (planets.size() > 0) {
                ret = (Planet) planets.get(rnd.nextInt(planets.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    /*
     * Jump drive
     */
    public boolean canJump(SolarSystem destination) {
        if (destination != null) {
            double safety;
            if (behavior == Behavior.UNIVERSE_TRADE) {
                safety = TRADER_JD_SAFETY_FUEL;
            } else {
                safety = JUMP_SAFETY_FUEL;
            }
            //make sure we have a jump drive group device
            if (hasGroupInCargo("jumpdrive")) {
                //fuel cost is linear
                if (fuel - getJumpFuelCost(destination) >= safety * maxFuel) {
                    return true;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    public double getJumpFuelCost(SolarSystem destination) {
        if (destination != null) {
            //calculate distance between current system and destination
            Vector3f cLoc = currentSystem.getLocation();
            Vector3f dLoc = destination.getLocation();
            double dist = cLoc.distance(dLoc);
            //fuel cost is linear
            double fuelCost = dist * 50;
            return fuelCost;
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    protected void dropJumpEffect() {
        //TODO
    }

    /*
     * Explosions!
     */
    protected void dropExplosion() {
        //TODO: Dynamic explosions from effects file
        Explosion explosion = new Explosion(getCurrentSystem().getUniverse(), 10, getName() + " Explosion");
        explosion.setLocation(getLocation());
        explosion.setRotation(getRotation());
        explosion.setVelocity(getVelocity());
        explosion.setpVel(getVelocity());
        getCurrentSystem().putEntityInSystem(explosion);
    }

    /*
     * For automatically firing shots
     */
    public boolean isFiring() {
        return firing;
    }

    public void setFiring(boolean firing) {
        this.firing = firing;
    }

    /*
     * Courage
     */
    public double getCourage() {
        return courage;
    }

    public void setCourage(double courage) {
        this.courage = courage;
    }

    /*
     * Contraband
     */
    public boolean isScanForContraband() {
        return scanForContraband;
    }

    public void setScanForContraband(boolean scanForContraband) {
        this.scanForContraband = scanForContraband;
    }

    /*
     * Loadout
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    /*
     * Detecting Aggro
     */
    public Ship getLastBlow() {
        return lastBlow;
    }

    public void setLastBlow(Ship lastBlow) {
        this.lastBlow = lastBlow;
    }

    /*
     * Sector and Universe Trade
     */
    public Station getBuyFromStation() {
        return buyFromStation;
    }

    public void setBuyFromStation(Station buyFromStation) {
        this.buyFromStation = buyFromStation;
    }

    public int getBuyFromPrice() {
        return buyFromPrice;
    }

    public void setBuyFromPrice(int buyFromPrice) {
        this.buyFromPrice = buyFromPrice;
    }

    public Station getSellToStation() {
        return sellToStation;
    }

    public void setSellToStation(Station sellToStation) {
        this.sellToStation = sellToStation;
    }

    public int getSellToPrice() {
        return sellToPrice;
    }

    public void setSellToPrice(int sellToPrice) {
        this.sellToPrice = sellToPrice;
    }

    public Item getWorkingWare() {
        return workingWare;
    }

    public void setWorkingWare(Item workingWare) {
        this.workingWare = workingWare;
    }
}
