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
 * Represents a mission the player has been assigned by an NPC.
 */
package universe;

import cargo.Item;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import entity.Entity;
import entity.Entity.State;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import lib.Binling;
import lib.Faction;
import lib.astral.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class Mission implements Serializable {

    public enum Type {

        ESCORT_ME, //protect the ship for X time staying within 5k
        WARE_DELIVERY, //deliver wares to a station.
        BOUNTY_HUNT, //the player is given a ship to destroy
        DESTROY_STATION, //the player is given a station to destroy
    }
    private Type missionType;
    private boolean aborted = false;
    //rng
    Random rnd = new Random();
    //reward and agent
    private long reward;
    private double deltaStanding;
    private final Ship agent;
    //briefing
    private String briefing = "NO AIM";
    //destroy missions
    private final ArrayList<Entity> targets = new ArrayList<>();
    //ware delivery missions
    private Item deliver;
    private Entity deliverTo;
    //escort missions
    private Entity escort;
    //timer
    private double timer = 0;
    private double endTimer = 0;

    public Mission(Ship agent) {
        this.agent = agent;
        //generates a random mission
        ArrayList<Term> missions = Universe.getCache().getMissionCache().getTermsOfType("Mission");
        Term pick = missions.get(rnd.nextInt(missions.size()));
        build(pick);
    }

    public Mission(Ship agent, Term pick) {
        this.agent = agent;
        build(pick);
    }

    private void build(Term pick) {
        //get info on min and max
        String sCash = pick.getValue("cash");
        String sDelta = pick.getValue("delta");
        {
            //calculate payment
            long min = Long.parseLong(sCash.split(">")[0]);
            long max = Long.parseLong(sCash.split(">")[1]);
            long dCash = max - min;
            long dR = (long) (rnd.nextFloat() * dCash);
            reward = min + dR;
        }
        //calculate delta
        {
            double min = Double.parseDouble(sDelta.split(">")[0]);
            double max = Double.parseDouble(sDelta.split(">")[1]);
            double dStanding = max - min;
            double dR = (rnd.nextFloat() * dStanding);
            deltaStanding = min + dR;
        }
        //store timing
        {
            String timing = pick.getValue("time");
            if (timing != null) {
                double min = Double.parseDouble(timing.split(">")[0]);
                double max = Double.parseDouble(timing.split(">")[1]);
                int t = (int) (rnd.nextFloat() * (max - min) + min) + 1;
                t *= 60;
                //store
                endTimer = t;
            }
        }
        //determine mission type
        String rawType = pick.getValue("type");
        if (rawType.equals("DESTROY_STATION")) {
            missionType = Type.DESTROY_STATION;
        }
        if (rawType.equals("BOUNTY_HUNT")) {
            missionType = Type.BOUNTY_HUNT;
        }
        if (rawType.equals("WARE_DELIVERY")) {
            missionType = Type.WARE_DELIVERY;
        }
        if (rawType.equals("ESCORT_ME")) {
            missionType = Type.ESCORT_ME;
        }
        //store briefing
        briefing = pick.getValue("briefing");
        //build more based on type
        if (missionType == Type.DESTROY_STATION) {
            buildDestroyStation();
        } else if (missionType == Type.BOUNTY_HUNT) {
            buildBountyHunt();
        } else if (missionType == Type.WARE_DELIVERY) {
            buildWareDelivery();
        } else if (missionType == Type.ESCORT_ME) {
            buildEscortMe();
        } else {
            preAbort();
        }
    }

    private void buildEscortMe() {
        /*
         * In these missions you have to keep who you hailed from dying without
         * getting more than 5k away from it. If it dies, or you move out of
         * range, you fail the mission.
         */
        if (agent.distanceTo(agent.getUniverse().getPlayerShip()) < 5000) {
            escort = agent;
            briefing = briefing.replace("<TIME>", endTimer / 60 + "");
            briefing = briefing.replace("<REWARD>", reward + "");
        } else {
            preAbort();
        }
    }

    private void buildWareDelivery() {
        long base = reward;
        /*
         * In these missions you have to deliver an exact amount of a stacked
         * ware to the station requested. You will be paid both per unit of
         * item delivered and a fee for your time.
         *
         * If the station is destroyed before the mission is complete, the
         * mission is aborted.
         */
        //find a ware
        ArrayList<Term> wares = Universe.getCache().getItemCache().getTermsOfType("Item");
        ArrayList<Item> choices = new ArrayList<>();
        for (int a = 0; a < wares.size(); a++) {
            Item test = new Item(wares.get(a).getValue("name"));
            if (test.getType().equals("commodity")) {
                choices.add(test);
            }
        }
        //pick one
        if (choices.isEmpty()) {
            preAbort();
        } else {
            Item pick = choices.get(rnd.nextInt(choices.size()));
            //pick a quantity
            int q = rnd.nextInt(pick.getStore()) + 1;
            pick.setQuantity(q);
            //pick a price
            long p = (long) (rnd.nextFloat() * (pick.getMaxPrice() - pick.getMinPrice()) + pick.getMinPrice());
            //update reward
            long dR = p * q;
            reward += dR;
            //store item
            deliver = pick;
            //now pick one of this faction's stations to deliver to
            ArrayList<Entity> stations = new ArrayList<>();
            Entity dTo = null;
            for (int a = 0; a < agent.getUniverse().getSystems().size(); a++) {
                ArrayList<Entity> lStat = agent.getUniverse().getSystems().get(a).getStationList();
                for (int v = 0; v < lStat.size(); v++) {
                    Ship test = (Ship) lStat.get(v);
                    if (test.getFaction().getName().equals(agent.getFaction().getName()) && test.getState() == State.ALIVE) {
                        stations.add(lStat.get(v));
                    }
                }
            }
            //pick a station
            if (!stations.isEmpty()) {
                dTo = stations.get(rnd.nextInt(stations.size()));
            }
            if (dTo != null) {
                deliverTo = dTo;
                Station test = (Station) dTo;
                briefing = briefing.replace("<STATION>", test.getName());
                briefing = briefing.replace("<LOCATION>", test.getCurrentSystem().getName());
                briefing = briefing.replace("<Q>", deliver.getQuantity() + "");
                briefing = briefing.replace("<WARE>", deliver.getName() + "");
                briefing = briefing.replace("<UNIT PRICE>", p + "");
                briefing = briefing.replace("<BASE REWARD>", base + "");
            } else {
                preAbort();
            }
        }
    }

    private void buildDestroyStation() {
        /*
         * In these missions a random enemy station is selected somewhere
         * in the universe, and the player has to find a way to blow it up.
         *
         * There is no time limit, and as long as the station dies the mission
         * will complete.
         */
        //make a list of negative standings
        ArrayList<String> badStandings = new ArrayList<>();
        ArrayList<Binling> raw = agent.getFaction().getStandings();
        for (int a = 0; a < raw.size(); a++) {
            if (!raw.get(a).getString().equals(Faction.PLAYER)) {
                if (raw.get(a).getDouble() < 0) {
                    badStandings.add(raw.get(a).getString());
                }
            }
        }
        //safety
        if (badStandings.isEmpty()) {
            preAbort();
        } else {
            //pick a group
            String pick = badStandings.get(rnd.nextInt(badStandings.size()));
            //find one of their stations
            Entity toKill = null;
            //get a list of all their stations
            ArrayList<Entity> options = new ArrayList<>();
            for (int a = 0; a < agent.getUniverse().getSystems().size(); a++) {
                ArrayList<Entity> lStat = agent.getUniverse().getSystems().get(a).getStationList();
                for (int v = 0; v < lStat.size(); v++) {
                    Ship test = (Ship) lStat.get(v);
                    if (test.getFaction().getName().equals(pick) && test.getState() == State.ALIVE) {
                        options.add(lStat.get(v));
                    }
                }
            }
            //pick a station
            if (!options.isEmpty()) {
                toKill = options.get(rnd.nextInt(options.size()));
            }
            //continue
            if (toKill != null) {
                Station tmp = (Station) toKill;
                //add station to target list
                targets.add(toKill);
                //update briefing
                briefing = briefing.replace("<TARGET>", tmp.getName());
                briefing = briefing.replace("<LOCATION>", tmp.getCurrentSystem().getName());
            } else {
                preAbort();
            }
        }
    }

    private void buildBountyHunt() {
        /*
         * In these missions a randomy selected enemy NPC is selected and a
         * bounty placed on it. The player must destroy this NPC before it is
         * destroyed by other causes to get the reward.
         *
         * There is no time limit, but the player must destroy the target.
         * Otherwise the mission will be aborted.
         */
        //make a list of negative standings
        ArrayList<String> badStandings = new ArrayList<>();
        ArrayList<Binling> raw = agent.getFaction().getStandings();
        for (int a = 0; a < raw.size(); a++) {
            if (!raw.get(a).getString().equals(Faction.PLAYER)) {
                if (raw.get(a).getDouble() < 0) {
                    badStandings.add(raw.get(a).getString());
                }
            }
        }
        //safety
        if (badStandings.isEmpty()) {
            preAbort();
        } else {
            //pick a group
            String pick = badStandings.get(rnd.nextInt(badStandings.size()));
            //find one of their stations
            Entity toKill = null;
            //get a list of all their stations
            ArrayList<Entity> options = new ArrayList<>();
            for (int a = 0; a < agent.getUniverse().getSystems().size(); a++) {
                ArrayList<Entity> lStat = agent.getUniverse().getSystems().get(a).getShipList();
                for (int v = 0; v < lStat.size(); v++) {
                    Ship test = (Ship) lStat.get(v);
                    if (test.getFaction().getName().equals(pick) && test.getState() == State.ALIVE) {
                        options.add(lStat.get(v));
                    }
                }
            }
            //pick a ship
            if (!options.isEmpty()) {
                toKill = options.get(rnd.nextInt(options.size()));
            }
            //continue
            if (toKill != null) {
                Ship tmp = (Ship) toKill;
                //add station to target list
                targets.add(toKill);
                //update briefing
                briefing = briefing.replace("<NAME>", tmp.getPilot());
                briefing = briefing.replace("<SHIP>", tmp.getType().getValue("type"));
                briefing = briefing.replace("<SHIPNAME>", tmp.getName());
                briefing = briefing.replace("<LOCATION>", tmp.getCurrentSystem().getName());
            } else {
                preAbort();
            }
        }
    }


    /*
     * Used internally to complete or fail a mission based on periodic updates.
     */
    private void abortMission() {
        aborted = true;
        //remove this mission
        agent.getUniverse().getPlayerMissions().remove(this);
        //notify
        agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Revoked", "Recent events require us to revoke your contract. You can come back for a new mission at your leisure, however.", null);
    }

    private void failMission() {
        if (!aborted) {
            //update standing
            agent.getUniverse().getPlayerShip().getFaction().derivedModification(agent.getFaction(), -deltaStanding);
            //remove this mission
            agent.getUniverse().getPlayerMissions().remove(this);
            //notify
            agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Failed", "That was pretty sad work you did.", null);
        }
    }

    private void completeMission() {
        if (!aborted) {
            //pay player
            agent.getUniverse().getPlayerShip().setCash(agent.getUniverse().getPlayerShip().getCash() + reward);
            //update standing
            agent.getUniverse().getPlayerShip().getFaction().derivedModification(agent.getFaction(), deltaStanding);
            //remove this mission
            agent.getUniverse().getPlayerMissions().remove(this);
            //notify
            agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Completed", "Payment transfered. Have a nice day.", null);
        }
    }

    private void preAbort() {
        /*
         * Called if the mission cannot be generated for some reason.
         */
        //drop rewards
        reward = 0;
        deltaStanding = 0;
        //remove this mission
        agent.getUniverse().getPlayerMissions().remove(this);
        //notify
        briefing = null;
        agent.composeMessage(agent.getUniverse().getPlayerShip(), "Nevermind", "We don't have anything available at the moment.", null);
    }

    public void periodicUpdate(double tpf) {
        //update timer
        timer += tpf;
        //events
        if (!missionComplete()) {
            //waiting
        } else {
            completeMission();
        }
        if (missionFailed()) {
            //fail the mission
            failMission();
        }
    }

    private boolean missionComplete() {
        if (missionType == Type.DESTROY_STATION) {
            return checkDestroyStation();
        } else if (missionType == Type.BOUNTY_HUNT) {
            return checkBountyHunt();
        } else if (missionType == Type.WARE_DELIVERY) {
            return checkWareDelivery();
        } else if (missionType == Type.ESCORT_ME) {
            return checkEscortMe();
        }
        //undefinded
        return true;
    }

    private boolean missionFailed() {
        if (missionType == Type.ESCORT_ME) {
            return checkFailEscortMe();
        }
        return false;
    }

    private boolean checkFailEscortMe() {
        Ship test = (Ship) escort;
        if (test.getState() != State.ALIVE) {
            return true;
        }
        return test.distanceTo(agent.getUniverse().getPlayerShip()) > 5000;
    }

    private boolean checkEscortMe() {
        return timer > endTimer;
    }

    private boolean checkWareDelivery() {
        if (deliverTo.getState() == State.ALIVE) {
            //iterate through player ships
            for (int a = 0; a < agent.getUniverse().getPlayerProperty().size(); a++) {
                if (agent.getUniverse().getPlayerProperty().get(a) instanceof Ship) {
                    Ship test = (Ship) agent.getUniverse().getPlayerProperty().get(a);
                    //get port containers
                    Station dck = (Station) deliverTo;
                    if (dck.hasDocked(test)) {
                        //find the item
                        ArrayList<Item> bay = test.getCargoBay();
                        for (int b = 0; b < bay.size(); b++) {
                            Item t = bay.get(b);
                            if (t.getName().equals(deliver.getName())) {
                                if (t.getQuantity() == deliver.getQuantity()) {
                                    bay.remove(b);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            //abort
            abortMission();
            return false;
        }
        //if we got this far
        return false;
    }

    private boolean checkDestroyStation() {
        //are all targets dead?
        for (int a = 0; a < targets.size(); a++) {
            if (targets.get(a).getState() == State.ALIVE) {
                //nope
                return false;
            }
        }
        //if we made it this far
        return true;
    }

    private boolean checkBountyHunt() {
        //are all targets dead?
        for (int a = 0; a < targets.size(); a++) {
            if (targets.get(a).getState() == State.ALIVE) {
                //nope
                return false;
            } else {
                Ship test = (Ship) targets.get(a);
                if (test.getLastBlow().getFaction().getName().equals(Faction.PLAYER)) {
                    //this is good news
                } else {
                    //someone else got it first
                    abortMission();
                    return false;
                }
            }
        }
        //if we made it this far
        return true;
    }

    /*
     * Getters and setters
     */
    public long getReward() {
        return reward;
    }

    public double getDeltaStanding() {
        return deltaStanding;
    }

    public Ship getAgent() {
        return agent;
    }

    public String getBriefing() {
        return briefing;
    }
}
