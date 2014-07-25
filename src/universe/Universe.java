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
    private ArrayList<Entity> playerProperty = new ArrayList<>();
    //discovered space
    private ArrayList<SolarSystem> discoveredSpace = new ArrayList<>();
    
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
}
