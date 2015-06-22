/*
 * Copyright (c) 2015 Nathan Wiehoff
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * Solar systems are a collection of planets and other celestials in a convenient
 * package. It provides zoning for the universe.
 */
package universe;

import cargo.Equipment;
import cargo.Hardpoint;
import cargo.Weapon;
import celestial.Celestial;
import celestial.Field;
import celestial.Jumphole;
import celestial.Nebula;
import celestial.Planet;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import celestial.Star;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import entity.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import jmeplanet.PlanetAppState;
import jmeplanet.Utility;
import lib.Faction;
import lib.astral.Parser;
import lib.astral.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class SolarSystem implements Entity, Serializable {
    //this system

    transient Spatial skybox;
    protected String name;
    float x;
    float y;
    float z;
    //what it contains
    private ArrayList<Entity> celestials = new ArrayList<>();
    //what contains it
    private final Universe universe;
    //engine resources
    private final Parser info;
    private final Term thisSystem;
    private boolean hasGraphics = true;
    private transient Node rootNode;
    private transient BulletAppState physics;
    private transient PlanetAppState planetAppState;
    //lists
    private final ArrayList<Entity> stationList = new ArrayList<>();
    private final ArrayList<Entity> shipList = new ArrayList<>();
    private final ArrayList<Entity> planetList = new ArrayList<>();
    private final ArrayList<Entity> jumpholeList = new ArrayList<>();
    //music
    private String ambientMusic = "Audio/Music/Undefined.wav";
    private String dangerMusic = "Audio/Music/Committing.wav";
    //sov
    private String owner = "Neutral";

    public SolarSystem(Universe universe, Term thisSystem, Parser parse) {
        name = thisSystem.getValue("name");
        this.universe = universe;
        this.info = parse;
        this.thisSystem = thisSystem;
        //store location
        x = Float.parseFloat(thisSystem.getValue("x"));
        y = Float.parseFloat(thisSystem.getValue("y"));
        z = Float.parseFloat(thisSystem.getValue("z"));
        //store owner
        String tmpOwner = thisSystem.getValue("owner");
        if (owner != null) {
            owner = tmpOwner;
        }
    }

    public final void initSystem() {
        /*
         * Adds all member objects. Member objects are any object that is
         * a member of this system according to the "system" param and is
         * one of the following
         *
         * Planet
         * Star
         * Nebula
         */
        //nebula
        ArrayList<Term> field = info.getTermsOfType("Field");
        for (int a = 0; a < field.size(); a++) {
            if (field.get(a).getValue("system").equals(getName())) {
                //this star needs to be created and stored
                putEntityInSystem(makeField(field.get(a)));
            }
        }
        //nebula
        ArrayList<Term> nebula = info.getTermsOfType("Nebula");
        for (int a = 0; a < nebula.size(); a++) {
            if (nebula.get(a).getValue("system").equals(getName())) {
                //this star needs to be created and stored
                putEntityInSystem(makeNebula(nebula.get(a)));
            }
        }
        //star
        ArrayList<Term> stars = info.getTermsOfType("Star");
        for (int a = 0; a < stars.size(); a++) {
            if (stars.get(a).getValue("system").equals(getName())) {
                //this star needs to be created and stored
                putEntityInSystem(makeStar(stars.get(a)));
            }
        }
        //planet
        ArrayList<Term> planets = info.getTermsOfType("Planet");
        for (int a = 0; a < planets.size(); a++) {
            if (planets.get(a).getValue("system").equals(getName())) {
                //this planet needs to be created and stored
                putEntityInSystem(makePlanet(planets.get(a)));
            }
        }
        //station
        ArrayList<Term> stations = info.getTermsOfType("Station");
        for (int a = 0; a < stations.size(); a++) {
            if (stations.get(a).getValue("system").equals(getName())) {
                //this ship needs to be created and stored
                putEntityInSystem(makeStation(stations.get(a)));
            }
        }
        //ship
        ArrayList<Term> ships = info.getTermsOfType("Ship");
        for (int a = 0; a < ships.size(); a++) {
            if (ships.get(a).getValue("system").equals(getName())) {
                //this ship needs to be created and stored
                putEntityInSystem(makeShip(ships.get(a)));
            }
        }
        //jumphole
        ArrayList<Term> jumpholes = info.getTermsOfType("Jumphole");
        for (int a = 0; a < jumpholes.size(); a++) {
            if (jumpholes.get(a).getValue("system").equals(getName())) {
                //this planet needs to be created and stored
                putEntityInSystem(makeJumphole(jumpholes.get(a)));
            }
        }
    }

    private Station makeStation(Term shipTerm) {
        Station station;
        {
            String type = shipTerm.getValue("station");
            Parser tmp = new Parser("STATION.txt");
            ArrayList<Term> list = tmp.getTermsOfType("Station");
            Term hull = null;
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("type").equals(type)) {
                    hull = list.get(a);
                    break;
                }
            }
            //extract terms
            String sName = shipTerm.getValue("name");
            float sx = Float.parseFloat(shipTerm.getValue("x"));
            float sy = Float.parseFloat(shipTerm.getValue("y"));
            float sz = Float.parseFloat(shipTerm.getValue("z"));
            String faction = shipTerm.getValue("faction");
            //create ship
            station = new Station(universe, hull, faction);
            //position ship
            station.setLocation(new Vector3f(sx, sy, sz));
            station.setCurrentSystem(this);
            station.setName(sName);
        }
        return station;
    }

    private Ship makeShip(Term shipTerm) {
        Ship ship;
        {
            String type = shipTerm.getValue("ship");
            Parser tmp = new Parser("SHIP.txt");
            ArrayList<Term> list = tmp.getTermsOfType("Ship");
            Term hull = null;
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("type").equals(type)) {
                    hull = list.get(a);
                    break;
                }
            }
            //extract terms
            String sName = shipTerm.getValue("name");
            float sx = Float.parseFloat(shipTerm.getValue("x"));
            float sy = Float.parseFloat(shipTerm.getValue("y"));
            float sz = Float.parseFloat(shipTerm.getValue("z"));
            String cargo = shipTerm.getValue("cargo");
            String faction = shipTerm.getValue("faction");
            String install = shipTerm.getValue("install");
            //create ship
            ship = new Ship(universe, hull, faction);
            //position ship
            ship.setLocation(new Vector3f(sx, sy, sz));
            ship.setCurrentSystem(this);
            ship.setName(sName);
            //store cargo
            if (cargo != null) {
                ship.addInitialCargo(cargo);
            }
            //store initial equipment
            if (install != null) {
                ship.addInitialEquipment(install);
            }
        }
        return ship;
    }

    private Planet makePlanet(Term planetTerm) {
        Planet planet;
        {
            String texture = planetTerm.getValue("texture");
            //find logical texture
            Parser tmp = new Parser("PLANET.txt");
            Term tex = null;
            ArrayList<Term> list = tmp.getTermsOfType("Planet");
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("name").equals(texture)) {
                    tex = list.get(a);
                    break;
                }
            }
            //extract terms
            String pName = planetTerm.getValue("name");
            float radius = Integer.parseInt(planetTerm.getValue("r"));
            float px = Float.parseFloat(planetTerm.getValue("x"));
            float py = Float.parseFloat(planetTerm.getValue("y"));
            float pz = Float.parseFloat(planetTerm.getValue("z"));
            float tiltX = Float.parseFloat(planetTerm.getValue("tiltX"));
            float tiltY = Float.parseFloat(planetTerm.getValue("tiltY"));
            float tiltZ = Float.parseFloat(planetTerm.getValue("tiltZ"));
            int seed = Integer.parseInt(planetTerm.getValue("seed"));
            //make planet and store
            planet = new Planet(universe, pName, tex, radius,
                    new Vector3f(tiltX, tiltY, tiltZ));
            planet.setSeed(seed);
            planet.setLocation(new Vector3f(px, py, pz));
        }
        return planet;
    }

    private Star makeStar(Term starTerm) {
        Star star;
        {
            String texture = starTerm.getValue("texture");
            //find the logical texture
            Parser tmp = new Parser("PLANET.txt");
            Term tex = null;
            ArrayList<Term> list = tmp.getTermsOfType("Star");
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("name").equals(texture)) {
                    tex = list.get(a);
                    break;
                }
            }
            //extract terms
            String pName = starTerm.getValue("name");
            float radius = Integer.parseInt(starTerm.getValue("r"));
            float px = Float.parseFloat(starTerm.getValue("x"));
            float py = Float.parseFloat(starTerm.getValue("y"));
            float pz = Float.parseFloat(starTerm.getValue("z"));
            //make planet and store
            star = new Star(universe, pName, tex, radius);
            star.setLocation(new Vector3f(px, py, pz));
        }
        return star;
    }

    private Jumphole makeJumphole(Term jumpholeTerm) {
        Jumphole jumphole;
        {
            //extract terms
            String pName = jumpholeTerm.getValue("name");
            String out = jumpholeTerm.getValue("out");
            float px = Float.parseFloat(jumpholeTerm.getValue("x"));
            float py = Float.parseFloat(jumpholeTerm.getValue("y"));
            float pz = Float.parseFloat(jumpholeTerm.getValue("z"));
            //make planet and store
            jumphole = new Jumphole(universe, pName);
            jumphole.setOut(out);
            jumphole.setLocation(new Vector3f(px, py, pz));
        }
        return jumphole;
    }

    private Field makeField(Term fieldTerm) {
        Field field;
        {
            //extract terms
            String pName = fieldTerm.getValue("name");
            String texture = fieldTerm.getValue("type");
            int seed = Integer.parseInt(fieldTerm.getValue("seed"));
            //position
            float px = Float.parseFloat(fieldTerm.getValue("x"));
            float py = Float.parseFloat(fieldTerm.getValue("y"));
            float pz = Float.parseFloat(fieldTerm.getValue("z"));
            //dimension
            float l = Float.parseFloat(fieldTerm.getValue("l"));
            float w = Float.parseFloat(fieldTerm.getValue("w"));
            float h = Float.parseFloat(fieldTerm.getValue("h"));
            //texture
            Parser tmp = new Parser("FIELD.txt");
            ArrayList<Term> terms = tmp.getTermsOfType("Field");
            Term fin = null;
            for (int o = 0; o < terms.size(); o++) {
                if (terms.get(o).getValue("name").equals(texture)) {
                    fin = terms.get(o);
                    break;
                }
            }
            //make planet and store
            field = new Field(universe, pName, fin, seed, new Vector3f(px, py, pz), new Vector3f(l, w, h));
            field.setLocation(new Vector3f(px, py, pz));
        }
        return field;
    }

    private Nebula makeNebula(Term nebulaTerm) {
        Nebula nebula;
        {
            //extract terms
            String pName = nebulaTerm.getValue("name");
            String texture = nebulaTerm.getValue("type");
            //position
            float px = Float.parseFloat(nebulaTerm.getValue("x"));
            float py = Float.parseFloat(nebulaTerm.getValue("y"));
            float pz = Float.parseFloat(nebulaTerm.getValue("z"));
            //dimension
            float l = Float.parseFloat(nebulaTerm.getValue("l"));
            float w = Float.parseFloat(nebulaTerm.getValue("w"));
            float h = Float.parseFloat(nebulaTerm.getValue("h"));
            //color
            String col = nebulaTerm.getValue("color");
            String[] colArr = col.split(",");
            float r = Float.parseFloat(colArr[0]);
            float g = Float.parseFloat(colArr[1]);
            float b = Float.parseFloat(colArr[2]);
            float a = Float.parseFloat(colArr[3]);
            //texture
            Parser tmp = new Parser("PARTICLE.txt");
            ArrayList<Term> terms = tmp.getTermsOfType("Nebula");
            Term fin = null;
            for (int o = 0; o < terms.size(); o++) {
                if (terms.get(o).getValue("name").equals(texture)) {
                    fin = terms.get(o);
                    break;
                }
            }
            //make planet and store
            nebula = new Nebula(universe, pName, fin, new ColorRGBA(r, g, b, a), new Vector3f(l, w, h));
            nebula.setLocation(new Vector3f(px, py, pz));
        }
        return nebula;
    }

    public ArrayList<Entity> getCelestials() {
        return celestials;
    }

    public void setCelestials(ArrayList<Entity> celestials) {
        this.celestials = celestials;
    }

    public void putEntityInSystem(Entity entity) {
        celestials.add(entity);
        if (entity instanceof Celestial) {
            Celestial tmp = (Celestial) entity;
            tmp.setCurrentSystem(this);
            /*
             * If this is the system the player is in then graphics need to be
             * constructed and added to the scene.
             */
            if (universe.getPlayerShip() != null) {
                if (this == universe.getPlayerShip().getCurrentSystem()) {
                    tmp.construct(universe);
                    if (hasGraphics) {
                        tmp.attach(rootNode, physics, planetAppState);
                    } else {
                        //do not add to scene, system does not have graphics yet
                    }
                }
            }
        }
        //check to see if this is player property
        if (universe.getPlayerProperty().contains(entity)) {
            //already in list
        } else {
            if (entity instanceof Ship) {
                Ship property = (Ship) entity;
                if (property.getFaction().getName().equals(Faction.PLAYER)) {
                    universe.getPlayerProperty().add(entity);
                }
            }
        }
        /*
         * Check to see what lists to add it to
         */
        if (entity instanceof Station) {
            stationList.add(entity);
        } else if (entity instanceof Ship) {
            shipList.add(entity);
        } else if (entity instanceof Jumphole) {
            jumpholeList.add(entity);
        } else if (entity instanceof Planet) {
            planetList.add(entity);
        }
    }

    public void pullEntityFromSystem(Entity entity) {
        if (entity instanceof Celestial) {
            Celestial tmp = (Celestial) entity;
            tmp.setCurrentSystem(null);
            if (universe.getPlayerShip() != null) {
                /*
                 * If this entity is in the same system as the player, we need
                 * to remove it from the scene.
                 */
                if (this == universe.getPlayerShip().getCurrentSystem()) {
                    tmp.detach(rootNode, physics, planetAppState);
                }
                tmp.deconstruct();
            }
        }
        //remove from lists
        celestials.remove(entity);
        stationList.remove(entity);
        shipList.remove(entity);
        planetList.remove(entity);
        jumpholeList.remove(entity);
        universe.getPlayerProperty().remove(entity);
    }

    @Override
    public void periodicUpdate(float tpf) {
        try {
            checkPlayerPresence();
            for (int a = 0; a < celestials.size(); a++) {
                if (celestials.get(a).getState() == Entity.State.DEAD) {
                    //remove the entity
                    pullEntityFromSystem(celestials.get(a));
                } else {
                    doAlways(a);
                    //but leave this
                    celestials.get(a).periodicUpdate(tpf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAlways(int a) {
        enforcePlanetZones(a);
        enforceJumpholeZones(a);
    }

    private void enforcePlanetZones(int a) {
        //enforce planet rules
        //todo: remove this when surfaces are done
        /*
         * Currently, planets don't have much to do. In fact, they
         * don't even have collidable surfaces! So they are kind of
         * off limits in this build.
         *
         * If you want to explore planet surfaces anyway, just
         * comment out the below if block and recompile. This
         * thing is open source :)
         *
         * Note that this also handles star death zones
         */
        if (celestials.get(a) instanceof Ship) {
            Ship s = (Ship) celestials.get(a);
            for (int b = 0; b < planetList.size(); b++) {
                Planet test = (Planet) planetList.get(b);
                float shellR = test.getRadius()
                        + (test.getRadius() * test.getAtmosphereScaler());
                if (test.distanceTo(s) < shellR) {
                    s.applyDamage(10000 * (1 - (test.distanceTo(s) / shellR)));
                }
            }
        }
    }

    private void enforceJumpholeZones(int a) {
        /*
         * This guarantees that a jumphole will never be located inside
         * of a planet.
         */
        if (celestials.get(a) instanceof Jumphole) {
            Jumphole s = (Jumphole) celestials.get(a);
            for (int b = 0; b < planetList.size(); b++) {
                Planet test = (Planet) planetList.get(b);
                float shellR = test.getRadius()
                        + (test.getRadius() * test.getAtmosphereScaler());
                if (test.distanceTo(s) < shellR * 3) {
                    //move the jumphole
                    s.setLocation(s.getLocation().add(new Vector3f(4 * shellR, 0, 4 * shellR)));
                    System.out.println(s.toString() + " was moved because it intersected " + test.toString());
                }
            }
        }
    }

    @Override
    public void oosPeriodicUpdate(float tpf) {
        try {
            checkPlayerPresence();
            for (int a = 0; a < celestials.size(); a++) {
                if (celestials.get(a).getState() == Entity.State.DEAD) {
                    //remove the entity
                    pullEntityFromSystem(celestials.get(a));
                } else {
                    doAlways(a);
                    //do integrity checks
                    checkEntity(celestials.get(a));
                    //update as normal
                    celestials.get(a).oosPeriodicUpdate(tpf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkEntity(Entity entity) {
        if (entity instanceof Ship) {
            if (!celestials.contains(universe.getPlayerShip())) {
                Ship test = (Ship) entity;
                //don't do OOS checks on player property obviously
                if (!test.isPlayerFaction()) {
                    //remove entities the player can't see that are out of fuel
                    double fuelPercent = test.getFuel() / test.getMaxFuel();
                    if (fuelPercent < 0.03) {
                        System.out.println("Removing derelict ship [F] " + test.getName() + " :: " + test.getAutopilot());
                        test.setState(State.DYING);
                    }
                    //see if this entity has a weapon with ammo left
                    boolean hasAmmo = false;
                    ArrayList<Hardpoint> hp = test.getHardpoints();
                    if (hp.size() > 0) {
                        for (int l = 0; l < hp.size(); l++) {
                            Equipment mounted = hp.get(l).getMounted();
                            if (mounted instanceof Weapon) {
                                Weapon tmp = (Weapon) mounted;
                                if (tmp.hasAmmo()) {
                                    hasAmmo = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        hasAmmo = true;
                    }
                    //remove entities that are completely out of ammo
                    if (hasAmmo) {
                        //do nothing
                    } else {
                        System.out.println("Removing derelict ship [A] " + test.getName() + " :: " + test.getAutopilot());
                        test.setState(State.DYING);
                    }
                } else {
                    discover();
                }
            } else {
                discover();
            }
        }
    }

    private void checkPlayerPresence() {
        if (celestials.contains(universe.getPlayerShip())) {
            hasGraphics = true;
            discover();
        } else {
            if (hasGraphics) {
                deconstruct();
                System.out.println("System " + getName() + " disposed graphics.");
                hasGraphics = false;
            }
        }
    }

    @Override
    public State getState() {
        return State.ALIVE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void construct(AssetManager assets) {
        //construct children
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).construct(assets);
        }
        //construct skybox
        Parser sky = new Parser("SKY.txt");
        ArrayList<Term> boxes = sky.getTermsOfType("Skybox");
        for (int a = 0; a < boxes.size(); a++) {
            if (boxes.get(a).getValue("name").equals(thisSystem.getValue("sky"))) {
                skybox = Utility.createSkyBox(assets, "Textures/Skybox/" + boxes.get(a).getValue("asset"), true);
                break;
            }
        }
    }

    @Override
    public void deconstruct() {
        skybox = null;
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).deconstruct();
        }
    }

    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //store references to add future objects
        this.physics = physics;
        this.planetAppState = planetAppState;
        rootNode = node;
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).attach(node, physics, planetAppState);
        }
        node.attachChild(skybox);
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).detach(node, physics, planetAppState);
        }
        node.detachChild(skybox);
    }

    @Override
    public void setState(State state) {
        //do nothing
    }

    @Override
    public Vector3f getLocation() {
        return new Vector3f(x, y, z);
    }

    @Override
    public void setLocation(Vector3f loc) {
        x = loc.x;
        y = loc.y;
        z = loc.z;
    }

    @Override
    public Quaternion getRotation() {
        return Quaternion.ZERO;
    }

    @Override
    public void setRotation(Quaternion rot) {
        //do nothing
    }

    @Override
    public Vector3f getPhysicsLocation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Universe getUniverse() {
        return universe;
    }

    public ArrayList<Entity> getStationList() {
        return stationList;
    }

    public ArrayList<Entity> getShipList() {
        return shipList;
    }

    public ArrayList<Entity> getPlanetList() {
        return planetList;
    }

    public ArrayList<Entity> getJumpholeList() {
        return jumpholeList;
    }

    public boolean hasGraphics() {
        return hasGraphics;
    }

    public void forceGraphics() {
        hasGraphics = true;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    private void discover() {
        //add to discovered list if needed
        if (universe.getDiscoveredSpace().contains(this)) {
            //do nothing
        } else {
            //add to discovered space
            universe.getDiscoveredSpace().add(this);
        }
    }

    public String getAmbientMusic() {
        return ambientMusic;
    }

    public void setAmbientMusic(String ambientMusic) {
        this.ambientMusic = ambientMusic;
    }

    public String getDangerMusic() {
        return dangerMusic;
    }

    public void setDangerMusic(String dangerMusic) {
        this.dangerMusic = dangerMusic;
    }
}
