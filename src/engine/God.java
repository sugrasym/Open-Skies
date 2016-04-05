/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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
 * Responsible for keeping the universe active. Specifically,
 * 1. Manage patrols and traders - spawn replacements as needed
 * 2. Manage stations - spawn replacements as needed
 * 3. Add 'fun' disasters to the universe. - TODO
 */
package engine;

import celestial.Celestial;
import celestial.Planet;
import celestial.Ship.Ship;
import celestial.Ship.Ship.Behavior;
import celestial.Ship.Station;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import entity.Entity;
import java.util.ArrayList;
import java.util.Random;
import lib.Binling;
import lib.Faction;
import lib.SuperFaction;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class God {

    private final Universe universe;
    private final ArrayList<SuperFaction> factions = new ArrayList<>();
    private final Random rnd = new Random();
    boolean firstRun = true;
    //sample
    private final char[] basicSample = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
        'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2',
        '3', '4', '5', '6', '7', '8', '9', '0'};
    //timing
    private long lastFrame;

    public God(Universe universe) {
        this.universe = universe;
        //generate lists
        initFactions();
    }

    private void initFactions() {
        //make a list of all factions
        Parser fParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = fParse.getTermsOfType("Faction");
        for (int a = 0; a < terms.size(); a++) {
            factions.add(new SuperFaction(universe, terms.get(a).getValue("name")));
        }
    }

    public void periodicUpdate() {
        //get time since last frame
        long dt = System.nanoTime() - lastFrame;
        //calculate time per frame
        double tpf = Math.abs(dt / 1000000000.0);
        //only run it every 16 minutes because it's a performance hog!
        if (tpf > 960 || firstRun) {
            firstRun = false;
            //store time
            lastFrame = System.nanoTime();
            //update
            checkStations();
            checkPatrols();
            checkTraders();
            checkMerchants();
            System.out.println("God cycled.");
        }
    }

    /*
     * Hooks
     */
    private void checkMerchants() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doMerchants(factions.get(a));
        }
    }

    private void checkTraders() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doTraders(factions.get(a));
        }
    }

    private void checkPatrols() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doPatrols(factions.get(a));
        }
    }

    private void checkStations() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doStations(factions.get(a));
        }
        //make sure none are ontop of each other
        for (int a = 0; a < universe.getSystems().size(); a++) {
            SolarSystem curr = universe.getSystems().get(a);
            ArrayList<Entity> stations = curr.getStationList();
            for (int y = 0; y < stations.size(); y++) {
                Station prim = (Station) stations.get(y);
                for (int x = 0; x < stations.size(); x++) {
                    Station sub = (Station) stations.get(x);
                    //make sure they aren't the same
                    if (stations.get(y) != stations.get(x)) {
                        /*//check for collission
                         if (prim.collideWith(sub)) {
                         //push the sub station away
                         sub.getLocation().setX(rnd.nextInt(64000) - 32000);
                         sub.getLocation().setZ(rnd.nextInt(64000) - 32000);
                         //report
                         System.out.println("Station " + sub + " was moved.");
                         }*/
                        //todo: better testing
                        double d = prim.getLocation().distance(sub.getLocation());
                        if (d < 10) {
                            //push the sub station away
                            sub.getLocation().setX(rnd.nextInt(64000) - 32000);
                            sub.getLocation().setZ(rnd.nextInt(64000) - 32000);
                            //report
                            System.out.println("Station " + sub + " was moved.");
                        }
                    }
                }
            }
        }
    }

    /*
     * Implementations
     */
    private void doStations(SuperFaction faction) {
        /*
         * 1. Make sure this faction has stations
         * 2. Count the number of each type
         * 3. Spawn more of each as needed.
         */
        if (faction.getStations().size() > 0) {
            int count[] = new int[faction.getStations().size()];
            if (faction.isEmpire()) {
                //this faction holds sov
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each station
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String type = faction.getStations().get(v).getString();
                        int num = countStations(faction, sys, type);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each station
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String type = faction.getStations().get(v).getString();
                        int num = countStations(faction, sys, type);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density;
                //calculate density based on entity type
                if (faction.isEmpire()) {
                    density = 1 + faction.getStations().get(a).getDouble() * faction.getSov().size();
                } else {
                    density = 1 + faction.getStations().get(a).getDouble() * faction.getSovHost().size();
                }
                //generate if needed
                //System.out.println(faction.getStations().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    //celestials
                    Celestial host;
                    SolarSystem pick;
                    //branch based on entity type
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //make a point
                    Vector3f pnt = pointNearCelestial(host);
                    //pick rotation
                    float tiltX = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                    float tiltY = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                    float tiltZ = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                    //spawn
                    spawnStation(faction, pick, pnt,
                            new Vector3f(tiltX, tiltY, tiltZ),
                            faction.getStations().get(a));
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doMerchants(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getMerchants().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getMerchants().size()];
            if (faction.isEmpire()) {
                //get a count of the number of traders in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getMerchants().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getMerchants().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getMerchants().get(a).getDouble();
                //System.out.println(faction.getMerchants().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //make a point
                    Vector3f pnt = pointNearCelestial(host);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getMerchants().get(a), Behavior.UNIVERSE_TRADE);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doTraders(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getTraders().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getTraders().size()];
            if (faction.isEmpire()) {
                //get a count of the number of traders in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getTraders().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getTraders().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getTraders().get(a).getDouble();
                //System.out.println(faction.getTraders().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //make a point
                    Vector3f pnt = pointNearCelestial(host);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getTraders().get(a), Behavior.SECTOR_TRADE);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doPatrols(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getPatrols().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getPatrols().size()];
            if (faction.isEmpire()) {
                //get a count of the number of patrols in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getPatrols().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getPatrols().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getPatrols().get(a).getDouble();
                //System.out.println(faction.getPatrols().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestials();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    Vector3f pnt = pointNearCelestial(host);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getPatrols().get(a), Behavior.PATROL);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private Vector3f pointNearCelestial(Celestial host) {
        float x;
        float y;
        float z;
        if (host instanceof Planet) {
            //pick a point outside of the planet
            Planet tmp = (Planet) host;
            float r = tmp.getRadius() * 2;
            float dx = (rnd.nextInt(10000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
            float dy = (rnd.nextInt(2000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
            float dz = (rnd.nextInt(10000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
            //store
            x = host.getLocation().getX() + dx;
            y = host.getLocation().getY() + dy;
            z = host.getLocation().getZ() + dz;
        } else {
            //pick a point near the celestial
            x = host.getLocation().getX() + rnd.nextInt(10000) - 5000;
            y = host.getLocation().getY() + rnd.nextInt(2000) - 1000;
            z = host.getLocation().getZ() + rnd.nextInt(10000) - 5000;
        }
        //make a point
        Vector3f pnt = new Vector3f(x, y, z);
        return pnt;
    }

    /*
     * Tools
     */
    public int countStations(Faction faction, SolarSystem system, String type) {
        int count = 0;
        {
            //get station list
            ArrayList<Entity> stations = system.getStationList();
            for (int a = 0; a < stations.size(); a++) {
                Station tmp = (Station) stations.get(a);
                if (tmp.getState() == Ship.State.ALIVE) {
                    if (tmp.getFaction().getName().equals(faction.getName())) {
                        if (tmp.getType().getValue("type").equals(type)) {
                            count++;
                        }
                    }
                } else if (tmp.getState() == Ship.State.DEAD) {
                    System.out.println(tmp.getName() + " dead but not cleaned up");
                }
            }
        }
        return count;
    }

    public int countShipsByLoadout(Faction faction, SolarSystem system, String loadout) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getState() == Ship.State.ALIVE) {
                    if (tmp.getFaction().getName().equals(faction.getName())) {
                        if (tmp.getTemplate().equals(loadout)) {
                            count++;
                        }
                    }
                } else if (tmp.getState() == Ship.State.DEAD) {
                    System.out.println(tmp.getName() + " dead but not cleaned up");
                }
            }
        }
        return count;
    }

    public int countShipsByRole(Faction faction, SolarSystem system, Behavior behavior) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getFaction().getName().equals(faction.getName())) {
                    if (tmp.getBehavior() == behavior) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private Station makeStation(String type, String name, String faction) {
        Parser tmp = Universe.getCache().getStationCache();
        ArrayList<Term> list = tmp.getTermsOfType("Station");
        Term hull = null;
        for (int a = 0; a < list.size(); a++) {
            if (list.get(a).getValue("type").equals(type)) {
                hull = list.get(a);
                break;
            }
        }
        Station ret = new Station(universe, hull, faction);
        ret.setName(name);
        return ret;
    }

    private Ship makeShip(String template, String name, String faction) {
        /*
         * Generates a ship from a template.
         */
        Ship ret;
        {
            String cargo = "";
            String install = "";
            String ship = "Mass Testing Brick";
            String cargoScan = "false";
            String minCourage = null;
            String maxCourage = null;
            if (template != null) {
                //load this template
                Parser lParse = Universe.getCache().getLoadoutCache();
                ArrayList<Term> lods = lParse.getTermsOfType("Loadout");
                for (int a = 0; a < lods.size(); a++) {
                    if (lods.get(a).getValue("name").equals(template)) {
                        //get terms
                        cargo = lods.get(a).getValue("cargo");
                        install = lods.get(a).getValue("install");
                        ship = lods.get(a).getValue("ship");
                        String cs = lods.get(a).getValue("cargoScan");
                        if (cs != null) {
                            cargoScan = cs;
                        }
                        //bailing
                        minCourage = lods.get(a).getValue("minCourage");
                        maxCourage = lods.get(a).getValue("maxCourage");
                        break;
                    }
                }
            }

            //find type
            Parser tmp = Universe.getCache().getShipCache();
            ArrayList<Term> list = tmp.getTermsOfType("Ship");
            Term hull = null;
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("type").equals(ship)) {
                    hull = list.get(a);
                    break;
                }
            }

            //create ship
            ret = new Ship(universe, hull, faction);
            if (template != null) {
                ret.setTemplate(template);
            }
            ret.setName(name);
            //check template
            ret.addInitialEquipment(install);
            ret.addInitialCargo(cargo);
            ret.setScanForContraband(Boolean.parseBoolean(cargoScan));
            //bailing
            if (minCourage != null && maxCourage != null) {
                double lowC = Double.parseDouble(minCourage);
                double hiC = Double.parseDouble(maxCourage);
                //pick a random number between these
                double d = hiC - lowC;
                double del = rnd.nextDouble() * d;
                double pick = lowC + del;
                //store courage
                ret.setCourage(pick);
            }
        }
        return ret;
    }

    public String randomIDTag(int size, char[] sample) {
        String ret = "";
        {
            for (int a = 0; a < size; a++) {
                char pick = sample[rnd.nextInt(sample.length)];
                ret += pick;
            }
        }
        return ret;
    }

    public void spawnShip(Faction faction, SolarSystem system, Vector3f loc, Binling loadout, Behavior behavior) {
        String name = loadout.getString() + " " + randomIDTag(5, basicSample);
        //get a basic ship to work with
        Ship tmp = makeShip(loadout.getString(), name, faction.getName());
        //push coordinates
        tmp.setLocation(loc);
        //push behavior
        tmp.setBehavior(behavior);
        //finalize
        tmp.setCurrentSystem(system);
        system.putEntityInSystem(tmp);
        //report
        System.out.println("Spawned " + loadout.getString() + " in " + system.getName() + " for " + faction.getName());
    }

    public void spawnStation(Faction faction, SolarSystem system, Vector3f loc,
            Vector3f tilt, Binling loadout) {
        String name = loadout.getString() + " " + randomIDTag(5, basicSample);
        //get a basic ship to work with
        Station tmp = makeStation(loadout.getString(), name, faction.getName());
        //push coordinates
        tmp.setLocation(loc);
        tmp.setRotation(new Quaternion().fromAngles(tilt.x, tilt.y, tilt.z));
        //finalize
        tmp.setCurrentSystem(system);
        system.putEntityInSystem(tmp);
        //report
        System.out.println("Spawned " + loadout.getString() + " in " + system.getName() + " for " + faction.getName());
    }
}
