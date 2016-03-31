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
 * Space station!
 */
package celestial.Ship;

import cargo.DockingPort;
import cargo.Item;
import cargo.Job;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import java.util.ArrayList;
import java.util.Random;
import lib.Faction;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Station extends Ship {
    //market

    protected ArrayList<Item> stationSelling = new ArrayList<>();
    protected ArrayList<Item> stationBuying = new ArrayList<>();
    //docking
    protected ArrayList<DockingPort> ports = new ArrayList<>();
    //manufacturing
    protected ArrayList<Job> jobs = new ArrayList<>();
    protected boolean economyExempt = false;
    private int buyPrice;
    private int sellPrice;

    public Station(Universe universe, Term type, String faction) {
        super(universe, type, faction);
        installDockingPorts(getType());
        installJobs(getType());
        installEconomics(getType());
    }

    @Override
    public void construct(AssetManager assets) {
        super.construct(assets);
        constructDockingPorts(assets);
    }

    @Override
    protected void loadSpatial(AssetManager assets, String name) {
        //load model
        try {
            spatial = assets.loadModel("Models/Stations/" + _class + "/Model.blend");
        } catch (Exception e) {
            System.out.println("Error: Model for station " + _class + " not found! Using placeholder.");
            spatial = assets.loadModel("Models/Stations/UnknownStation/Model.blend");
        }
    }

    @Override
    protected void constructMaterial(AssetManager assets, String name) {
        //load texture
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/Stations/" + _class + "/tex.png"));
        //setup texture
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        spatial.setMaterial(mat);
        //store
        center.attachChild(spatial);
    }

    @Override
    protected void constructPhysics() {
        //setup physics
        CollisionShape meshShape = CollisionShapeFactory.createMeshShape(spatial);
        physics = new RigidBodyControl(meshShape, getMass());
        center.addControl(physics);
        physics.setSleepingThresholds(0, 0);
        physics.setAngularDamping(0.99f); //I do NOT want to deal with this at 0
        center.setName(this.getClass().getName());
        //store physics name control
        nameControl.setParent(this);
        center.addControl(nameControl);
    }

    protected void constructDockingPorts(AssetManager assets) {
        for (int a = 0; a < ports.size(); a++) {
            //initialize node
            ports.get(a).initNode();
            //debug
            //ports.get(a).showDebugHardpoint(assets);
            //store node with spatial
            center.attachChild(ports.get(a).getNode());
            center.attachChild(ports.get(a).getAlign());
        }
    }

    @Override
    protected void alive() {
        //update parent
        super.alive();
        //update docking ports
        updateDockingPorts();
    }

    @Override
    protected void aliveAlways() {
        super.aliveAlways();
        updateJobs();
        updateEconomics();
    }

    /*
     * OOS Updates
     */
    @Override
    protected void oosAlive() {
        super.oosAlive();
        //update docking ports
        oosUpdateDockingPorts();
    }

    /*
     * Docking code
     */
    public ArrayList<DockingPort> getPorts() {
        return ports;
    }

    public DockingPort requestDockingPort(Ship client) {
        if (canDock(client)) {
            for (int a = 0; a < ports.size(); a++) {
                if (ports.get(a).isEmpty()) {
                    if (!client.isHostileToMe(this)) {
                        ports.get(a).setClient(client);
                        return ports.get(a);
                    }
                } else {
                    //unavailable
                }
            }
        }
        return null;
    }

    public boolean canDock(Ship ship) {
        for (int a = 0; a < ports.size(); a++) {
            if (ports.get(a).isEmpty()) {
                if (!ship.isHostileToMe(this)) {
                    return true;
                }
            } else {
                //unavailable
            }
        }
        return false;
    }
    
    @Override
    public void discover() {
        //any station can be discovered
        setDiscoveredByPlayer(true);
    }
    
    @Override
    protected void deathPenalty() {
        //did the player destroy this ship?
        if (getLastBlow().getFaction().getName().equals(Faction.PLAYER)) {
            //adjust the player's standings accordingly
            if (!faction.getName().equals("Neutral")) {
                getCurrentSystem().getUniverse().getPlayerShip().getFaction().derivedModification(faction, Faction.STATION_KILL_PENALTY);
            }
        }
    }

    private void updateDockingPorts() {
        for (int a = 0; a < ports.size(); a++) {
            ports.get(a).periodicUpdate(tpf);
        }
    }

    private void oosUpdateDockingPorts() {
        for (int a = 0; a < ports.size(); a++) {
            ports.get(a).oosPeriodicUpdate(tpf);
        }
    }

    private void installDockingPorts(Term relevant) throws NumberFormatException {
        /*
         * Equips the station with docking ports
         */
        String complex = relevant.getValue("port");
        if (complex != null) {
            String[] arr = complex.split("/");
            for (int a = 0; a < arr.length; a++) {
                String[] re = arr[a].split(",");
                String hType = re[0];
                int hSize = Integer.parseInt(re[1]);
                float hx = Float.parseFloat(re[2]);
                float hy = Float.parseFloat(re[3]);
                float hz = Float.parseFloat(re[4]);
                float ax = Float.parseFloat(re[5]);
                float ay = Float.parseFloat(re[6]);
                float az = Float.parseFloat(re[7]);
                ports.add(new DockingPort(this, hType, hSize, new Vector3f(hx, hy, hz), new Vector3f(ax, ay, az)));
            }
        }
    }

    private void installEconomics(Term relevant) {
        /*
         * Determines if the station is economy exempt or not. Exempt stations
         * do not go out of business and are used for things like customs offices
         * or ship yards.
         * 
         * Player ships will never be exempt.
         */
        if (isPlayerFaction()) {
            //do not do this
            economyExempt = false;
        } else {
            randomizeInitialGoods(new Random().nextInt());
            String status = relevant.getValue("economyExempt");
            if (status != null) {
                economyExempt = Boolean.parseBoolean(status);
                System.out.println(getType().getValue("type") + " has an exemption status of " + economyExempt);
            } else {
                economyExempt = false;
            }
        }
    }

    /*
     * Market code
     */
    public ArrayList<Item> getStationSelling() {
        return stationSelling;
    }

    public ArrayList<Item> getStationBuying() {
        return stationBuying;
    }

    public void buy(Ship ship, Item item, int quantity) {
        //get current offer
        int price = getPrice(item);
        if (buyPrice != 0) price = buyPrice;
        Item tmp = new Item(item.getName());
        //repeat buy procedure
        for (int lx = 0; lx < quantity; lx++) {
            Item rel = null;
            //validate the item is available
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().equals(item.getName())) {
                    //make sure there is something available
                    if (stationSelling.get(a).getQuantity() > 0) {
                        rel = stationSelling.get(a);
                    }
                    break;
                }
            }
            if (rel != null) {
                //validate the player can cover the charge
                if (ship.getCash() - price >= 0) {
                    //branch based on regular item or ship
                    if (rel.getType().equals("ship")) {
                        /*
                         * This one is a little more complicated.
                         */
                        //make a ship
                        Parser t = Universe.getCache().getShipCache();
                        ArrayList<Term> list = t.getTermsOfType("Ship");
                        Term hull = null;
                        for (int a = 0; a < list.size(); a++) {
                            if (list.get(a).getValue("type").equals(item.getName())) {
                                hull = list.get(a);
                                break;
                            }
                        }

                        //create ship
                        Ship newShip = new Ship(getCurrentSystem().getUniverse(), hull, Faction.PLAYER);
                        newShip.setName("Your "+item.getName());
                        //find an open hanger
                        DockingPort pick = null;
                        for (int a = 0; a < ports.size(); a++) {
                            if (ports.get(a).isEmpty()) {
                                //got one
                                pick = ports.get(a);
                                break;
                            }
                        }
                        if (pick != null) {
                            //decrement stocks
                            rel.setQuantity(rel.getQuantity() - 1);
                            //drop it in the current solar system
                            newShip.setCurrentSystem(currentSystem);
                            currentSystem.putEntityInSystem(newShip);
                            //drop it in that port
                            pick.setClient(newShip);
                            newShip.setPort(pick);
                            //allow the port to naturally pick it up when it collides
                            newShip.setPhysicsLocation(pick.getNode().getWorldTranslation());
                            newShip.setAutopilot(Autopilot.DOCK_STAGE2);
                            //transfer funds
                            ship.setCash(ship.getCash() - price);
                            setCash(getCash() + price);
                            //make sure it doesn't have funds
                            newShip.setCash(0);
                        }
                    } else {
                        /*
                         * This is pretty simple
                         */
                        //attempt transfer of item
                        if (ship.addToCargoBay(tmp)) {
                            //decrement stocks
                            rel.setQuantity(rel.getQuantity() - 1);
                            //transfer funds
                            ship.setCash(ship.getCash() - price);
                            setCash(getCash() + price);
                            //adjust standings
                            if(ship.getFaction().getName().equals(Faction.PLAYER)) {
                                double scaler = ship.getStandingsToMe(this) / (double)Faction.PERMA_GREEN;
                                double delta = price * Faction.MARKET_DELTA * Math.abs(scaler);
                                getCurrentSystem().getUniverse().getPlayerShip().getFaction().derivedModification(faction, delta);
                            }
                        }
                    }
                }
            }
        }
    }

    public void sell(Ship ship, Item item, int quantity) {
        //get current offer
        int price = getPrice(item);
        if (sellPrice != 0) price = sellPrice;
        //repeat sell procedure
        for (int lx = 0; lx < quantity; lx++) {
            Item rel = null;
            //validate the item is in the cargo bay
            for (int a = 0; a < ship.getCargoBay().size(); a++) {
                if (ship.getCargoBay().get(a).getName().equals(item.getName())) {
                    rel = ship.getCargoBay().get(a);
                    break;
                }
            }
            if (rel != null) {
                //send to station
                for (int a = 0; a < getStationBuying().size(); a++) {
                    //make sure station can cover it
                    if (getCash() - price >= 0) {
                        if (rel.getName().equals(getStationBuying().get(a).getName())) {
                            getStationBuying().get(a).setQuantity(getStationBuying().get(a).getQuantity() + 1);
                            //remove from cargo
                            ship.removeFromCargoBay(rel);
                            //pay the ship
                            ship.setCash(ship.getCash() + price);
                            //remove funds from station wallet
                            setCash(getCash() - price);
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean buysWare(Item ware) {
        {
            for (int a = 0; a < stationBuying.size(); a++) {
                if (stationBuying.get(a).getName().equals(ware.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean sellsWare(Item ware) {
        {
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().equals(ware.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getPrice(Item item) {
        int max = 0;
        int min = 0;
        int q = 0;
        int s = 1;
        //get the right commodity
        boolean found = false;
        for (int a = 0; a < stationBuying.size(); a++) {
            if (stationBuying.get(a).getName().equals(item.getName())) {
                max = stationBuying.get(a).getMaxPrice();
                min = stationBuying.get(a).getMinPrice();
                q = stationBuying.get(a).getQuantity();
                s = stationBuying.get(a).getStore();
                found = true;
                break;
            }
        }
        if (!found) {
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().equals(item.getName())) {
                    max = stationSelling.get(a).getMaxPrice();
                    min = stationSelling.get(a).getMinPrice();
                    q = stationSelling.get(a).getQuantity();
                    s = stationSelling.get(a).getStore();
                    found = true;
                    break;
                }
            }
        }
        //calculate price
        int d = max - min;
        float per = (float) q / (float) s;
        int x = (int) (d * (1 - per));
        int price = min + x;
        if (price < min) {
            price = min;
        } else if (price > max) {
            price = max;
        }
        return price;
    }

    protected void randomizeInitialGoods(int seed) {
        Random rnd = new Random(seed);
        if (stationSelling.size() > 0) {
            for (int a = 0; a < stationSelling.size(); a++) {
                stationSelling.get(a).setQuantity(rnd.nextInt(stationSelling.get(a).getStore()));
            }
        }
        if (stationBuying.size() > 0) {
            for (int a = 0; a < stationBuying.size(); a++) {
                stationBuying.get(a).setQuantity(rnd.nextInt(stationBuying.get(a).getStore()));
            }
        }
    }

    /*
     * Manufacturing processes
     */
    private void installJobs(Term relevant) throws NumberFormatException {
        //generates the processes that were linked to this station
        String raw = relevant.getValue("job");
        if (raw != null) {
            String[] arr = raw.split("/");
            for (int a = 0; a < arr.length; a++) {
                Job p = new Job(this, arr[a], stationSelling, stationBuying);
                jobs.add(p);
            }
        }
    }

    public ArrayList<Job> getJobs() {
        return jobs;
    }

    public void setProcesses(ArrayList<Job> jobs) {
        this.jobs = jobs;
    }

    protected void updateJobs() {
        //check processes
        for (int a = 0; a < jobs.size(); a++) {
            jobs.get(a).periodicUpdate(tpf);
        }
    }

    protected void updateEconomics() {
        if (isPlayerFaction()) {
            //don't do anything
        } else {
            if (getCash() <= 0) {
                if (economyExempt) {
                    setCash(10000000);
                } else {
                    //we are out of business :(
                    setState(State.DYING);
                }
            }
        }
    }
    
    public void clearWares() {
        setCash(0);
        for(int a = 0; a < stationSelling.size(); a++) {
            stationSelling.get(a).setQuantity(0);
        }
        for(int a = 0; a < stationBuying.size(); a++) {
            stationBuying.get(a).setQuantity(0);
        }
        economyExempt = false;
    }

    public boolean isEconomyExcempt() {
        return economyExempt;
    }

    public void setEconomyExcempt(boolean economyExcempt) {
        this.economyExempt = economyExcempt;
    }

    /*
     * Utility and reporting
     */
    @Override
    public String toString() {
        String ret;
        if (!isPlayerFaction()) {
            ret = getName();
        } else {
            if (this.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem() == getCurrentSystem()) {
                ret = getName();
            } else {
                ret = getName() + ", " + getCurrentSystem().getName();
            }
        }
        return ret;
    }
    
    public boolean hasDocked(Ship ship) {
        for (int a = 0; a < ports.size(); a++) {
            if (ports.get(a).getClient() == ship && ship.isDocked()) {
                return true;
            }
        }
        return false;
    }
    
    public int getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(int price) {
        buyPrice = price;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(int price) {
        sellPrice = price;
    }
}
