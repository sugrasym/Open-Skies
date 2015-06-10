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
 * Extended version of the faction class for use by God when repairing and
 * maintaining the universe.
 */
package lib;

import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class SuperFaction extends Faction {
    //universe

    private final Universe universe;
    //sov
    private final ArrayList<SolarSystem> sov = new ArrayList<>();
    private final ArrayList<SolarSystem> sovHost = new ArrayList<>();
    //loadout lists
    private ArrayList<Binling> patrols = new ArrayList<>();
    private final ArrayList<Binling> traders = new ArrayList<>();
    private final ArrayList<Binling> merchants = new ArrayList<>();
    //station list
    private ArrayList<Binling> stations = new ArrayList<>();
    //music list
    private final ArrayList<String> ambientMusic = new ArrayList<>();
    private final ArrayList<String> dangerMusic = new ArrayList<>();
    /*
     * Like a faction, except it stores information about loadout types,
     * station types, etc that god needs.
     */

    public SuperFaction(Universe universe, String name) {
        super(name);
        this.universe = universe;
        if (universe != null) {
            initStations();
            initLoadouts();
            initSov();
            initSovHosts();
        }
        //check music
        if (isEmpire() || name.equals("Neutral")) {
            initAmbientMusic();
            initDangerMusic();
        }
    }

    private void initStations() {
        //get a list of stations for this faction
        Parser sParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = sParse.getTermsOfType("Stations");
        Term stat = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").equals(getName())) {
                stat = terms.get(a);
            }
        }
        if (stat != null) {
            //get types of stations
            int a = 0;
            String type = "";
            while ((type = stat.getValue("station" + a)) != null) {
                //get station info
                String ty = type.split(",")[0];
                double spread = Float.parseFloat(type.split(",")[1]);
                stations.add(new Binling(ty, spread));
                //iterate
                a++;
            }
        } else {
            System.out.println(getName() + " doesn't have any stations!");
        }
    }

    private void initAmbientMusic() {
        //get a list of stations for this faction
        Parser sParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = sParse.getTermsOfType("Music");
        Term muse = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").equals(getName())) {
                muse = terms.get(a);
            }
        }
        if (muse != null) {
            //get types of stations
            int a = 0;
            String type = "";
            while ((type = muse.getValue("ambient" + a)) != null) {
                //store
                getAmbientMusic().add(type.toString());
                //iterate
                a++;
            }
        } else {
            System.out.println(getName() + " doesn't have any ambient music!");
        }
    }

    private void initDangerMusic() {
        //get a list of stations for this faction
        Parser sParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = sParse.getTermsOfType("Music");
        Term muse = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").equals(getName())) {
                muse = terms.get(a);
            }
        }
        if (muse != null) {
            //get types of stations
            int a = 0;
            String type = "";
            while ((type = muse.getValue("danger" + a)) != null) {
                //store
                getDangerMusic().add(type.toString());
                //iterate
                a++;
            }
        } else {
            System.out.println(getName() + " doesn't have any danger music!");
        }
    }

    private void initLoadouts() {

        //get a list of patrol loadouts for this faction
        Parser sParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = sParse.getTermsOfType("Loadout");
        Term stat = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").equals(getName())) {
                stat = terms.get(a);
            }
        }
        if (stat != null) {
            /*
             * Patrol loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("patrol" + a)) != null) {
                    //get patrol info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    patrols.add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
            /*
             * Trader loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("trader" + a)) != null) {
                    //get trader info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    traders.add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
            /*
             * Universe Trader Loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("merchant" + a)) != null) {
                    //get merchant info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    getMerchants().add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
        } else {
            System.out.println(getName() + " doesn't have any loadouts!");
        }
    }

    private void initSov() {
        /*
         * Makes a list of all the systems this faction controls.
         */
        ArrayList<SolarSystem> systems = universe.getSystems();
        for (int a = 0; a < systems.size(); a++) {
            if (systems.get(a).getOwner().equals(getName())) {
                sov.add(systems.get(a));
            }
        }
    }

    private void initSovHosts() {
        /*
         * Makes a list of all the systems this faction can spawn in if it is
         * not a sov holder.
         */
        ArrayList<SolarSystem> systems = universe.getSystems();
        for (int a = 0; a < systems.size(); a++) {
            if (canSpawnIn(universe.getSystems().get(a))) {
                sovHost.add(systems.get(a));
            }
        }
    }

    public ArrayList<Binling> getPatrols() {
        return patrols;
    }

    public void setPatrols(ArrayList<Binling> patrols) {
        this.patrols = patrols;
    }

    public ArrayList<Binling> getStations() {
        return stations;
    }

    public void setStations(ArrayList<Binling> stations) {
        this.stations = stations;
    }

    public ArrayList<SolarSystem> getSov() {
        return sov;
    }

    public ArrayList<SolarSystem> getSovHost() {
        return sovHost;
    }

    private boolean canSpawnIn(SolarSystem get) {
        for (int a = 0; a < getHosts().size(); a++) {
            if (get.getOwner().equals(getHosts().get(a))) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Binling> getTraders() {
        return traders;
    }

    public ArrayList<String> getAmbientMusic() {
        return ambientMusic;
    }

    public ArrayList<String> getDangerMusic() {
        return dangerMusic;
    }

    public ArrayList<Binling> getMerchants() {
        return merchants;
    }
}
