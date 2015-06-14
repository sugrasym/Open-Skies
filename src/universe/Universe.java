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
 * It's kind of big
 */
package universe;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import engine.ResourceCache;
import entity.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class Universe implements Serializable {
    
    private static final transient ResourceCache cache;
    public static final int SOUND_RANGE = 2000;
    private ArrayList<SolarSystem> systems = new ArrayList<>();
    protected Ship playerShip;
    private transient AssetManager assets;
    //player property
    private final ArrayList<Entity> playerProperty = new ArrayList<>();
    //missions and scripting
    private final ArrayList<Mission> playerMissions = new ArrayList<>();
    //discovered space
    private final ArrayList<SolarSystem> discoveredSpace = new ArrayList<>();
    
    public Universe(AssetManager assets) {
        this.assets = assets;
        init();
    }
    
    static {
        cache = new ResourceCache();
    }
    
    private void init() {
        //create the universe parser
        Parser parse = new Parser("UNIVERSE.txt");
        //get all the solar system terms
        ArrayList<Term> solars = parse.getTermsOfType("System");
        //generate the systems and add them
        System.out.println("INFO: Found " + solars.size() + " systems to make.");
        for (int a = 0; a < solars.size(); a++) {
            getSystems().add(makeSystem(parse, solars.get(a)));
        }
        //generate the player
        ArrayList<Term> games = parse.getTermsOfType("NewGame");
        System.out.println("INFO: Found " + games.size() + " games to read.");
        //there should only be of these, pick the first one
        //makePlayer(games.get(0));
    }
    
    private SolarSystem makeSystem(Parser parse, Term thisSystem) {
        SolarSystem system = null;
        {
            system = new SolarSystem(this, thisSystem, parse);
            //get music
            String ambient = thisSystem.getValue("ambient");
            String danger = thisSystem.getValue("danger");
            if(ambient != null) {
                system.setAmbientMusic(ambient);
            }
            if(danger != null) {
                system.setDangerMusic(danger);
            }
            system.initSystem(assets);
            
        }
        System.out.println("Working: " + system.getName() + " solar system created. ");
        return system;
    }
    
    public ArrayList<SolarSystem> getSystems() {
        return systems;
    }
    
    public void setSystems(ArrayList<SolarSystem> systems) {
        this.systems = systems;
    }
    
    public Ship getPlayerShip() {
        return playerShip;
    }
    
    public void setPlayerShip(Ship playerShip) {
        this.playerShip = playerShip;
    }
    
    public SolarSystem getSystemWithName(String name) {
        for(int a = 0; a < systems.size(); a++) {
            if(systems.get(a).getName().equals(name)) {
                return systems.get(a);
            }
        }
        return null;
    }
    
    public static ResourceCache getCache() {
        return cache;
    }
    
    public ArrayList<Entity> getPlayerProperty() {
        return playerProperty;
    }

    public AssetManager getAssets() {
        return assets;
    }
    
    public void setAssets(AssetManager assets) {
        this.assets = assets;
    }

    public ArrayList<SolarSystem> getDiscoveredSpace() {
        return discoveredSpace;
    }
    
    public ArrayList<Mission> getPlayerMissions() {
        return playerMissions;
    }
}
