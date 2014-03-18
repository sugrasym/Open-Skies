/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Contains a list of all sprites (THAT ARE NOT GENERATED ON THE FLY) for
 * easy retrieval.
 */
package engine;

import java.awt.Image;
import java.util.ArrayList;
import lib.astral.Parser;

/**
 *
 * @author nwiehoff
 */
public class ResourceCache {
    //sprite cache

    private ArrayList<Spriteling> ships = new ArrayList<>();
    private ArrayList<Spriteling> stations = new ArrayList<>();
    private ArrayList<Spriteling> projectiles = new ArrayList<>();
    private ArrayList<Spriteling> explosions = new ArrayList<>();
    //parser cache
    private Parser universeCache = new Parser("UNIVERSE.txt");
    private Parser shipCache = new Parser("SHIP.txt");
    private Parser itemCache = new Parser("ITEM.txt");
    private Parser weaponCache = new Parser("WEAPONS.txt");
    //private Parser factionCache = new Parser("FACTIONS.txt");
    private Parser stationCache = new Parser("STATION.txt");
    //private Parser explosionCache = new Parser("EXPLOSIONS.txt");
    //private Parser processCache = new Parser("PROCESSES.txt");
    private Parser skyCache = new Parser("SKY.txt");
    //private Parser loadoutCache = new Parser("LOADOUTS.txt");
    //private Parser conversationCache = new Parser("CONVERSATIONS.txt");
    private Parser planetCache = new Parser("PLANET.txt");
    //private Parser missionCache = new Parser("MISSIONS.txt");
    //private Parser nameCache = new Parser("NAMES.txt");

    public ResourceCache() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        //TODO
    }

    public Parser getUniverseCache() {
        return universeCache;
    }

    public Parser getShipCache() {
        return shipCache;
    }

    public Parser getItemCache() {
        return itemCache;
    }

    public Parser getWeaponCache() {
        return weaponCache;
    }

    /*public Parser getFactionCache() {
        return factionCache;
    }*/

    public Parser getStationCache() {
        return stationCache;
    }

    /*public Parser getExplosionCache() {
        return explosionCache;
    }

    public Parser getProcessCache() {
        return processCache;
    }*/

    public Parser getSkyCache() {
        return skyCache;
    }

    /*public Parser getLoadoutCache() {
        return loadoutCache;
    }

    public Parser getConversationCache() {
        return conversationCache;
    }*/

    public Parser getPlanetCache() {
        return planetCache;
    }

    /*public Parser getMissionCache() {
        return missionCache;
    }

    public Parser getNameCache() {
        return nameCache;
    }*/

    public ArrayList<Spriteling> getStations() {
        return stations;
    }

    private class Spriteling {
        /*
         * Simple structure for linking a sprite to its name so it can be
         * easily referenced.
         */

        private final String name;
        private final Image sprite;
        private final int hash;

        public Spriteling(String name, Image sprite) {
            //hash
            this.name = name;
            this.hash = name.hashCode();
            //store image
            this.sprite = sprite;
        }

        public boolean matches(String test) {
            return test.hashCode() == hash;
        }

        public String getName() {
            return name;
        }

        public Image getSprite() {
            return sprite;
        }

        public int getHash() {
            return hash;
        }
    }
}