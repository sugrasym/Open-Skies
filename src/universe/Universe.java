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

import celestial.Ship.Player;
import com.jme3.asset.AssetManager;
import java.io.Serializable;
import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class Universe implements Serializable {
    
    private ArrayList<SolarSystem> systems = new ArrayList<>();
    private Player player;
    transient AssetManager assets;
    
    public Universe(AssetManager assets) {
        this.assets = assets;
        init();
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
    }
    
    private SolarSystem makeSystem(Parser parse, Term thisSystem) {
        SolarSystem system = null;
        {
            system = new SolarSystem(this, thisSystem, parse);
            system.initSystem();
        }
        System.out.println(system.getName() + " solar system created. ");
        return system;
    }
    
    public void deconstruct() {
        for(int a = 0; a < systems.size(); a++) {
            systems.get(a).deconstruct();
        }
    }
    
    public SolarSystem getSystemWithName(String name) {
        for(int a = 0; a < systems.size(); a++) {
            if(systems.get(a).getName().matches(name)) {
                return systems.get(a);
            }
        }
        return null;
    }
    
    public ArrayList<SolarSystem> getSystems() {
        return systems;
    }
    
    public void setSystems(ArrayList<SolarSystem> systems) {
        this.systems = systems;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
