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
 * Contains a list of all sprites (THAT ARE NOT GENERATED ON THE FLY) for
 * easy retrieval.
 */
package engine;

import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class ResourceCache {
    //parser cache
    private Parser universeCache = new Parser("UNIVERSE.txt");
    private Parser shipCache = new Parser("SHIP.txt");
    private Parser itemCache = new Parser("ITEM.txt");
    private Parser weaponCache = new Parser("WEAPONS.txt");
    private Parser factionCache = new Parser("FACTIONS.txt");
    private Parser stationCache = new Parser("STATION.txt");
    //private Parser explosionCache = new Parser("EXPLOSIONS.txt");
    private Parser processCache = new Parser("PROCESSES.txt");
    private Parser skyCache = new Parser("SKY.txt");
    private Parser loadoutCache = new Parser("LOADOUTS.txt");
    private Parser conversationCache = new Parser("CONVERSATIONS.txt");
    private Parser planetCache = new Parser("PLANET.txt");
    private Parser missionCache = new Parser("MISSIONS.txt");
    private Parser nameCache = new Parser("NAMES.txt");
    private Parser quoteCache = new Parser("QUOTES.txt");
    
    private Term cargoContainerTerm;

    public ResourceCache() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        //init the cargo container term
        ArrayList<Term> shipTerms = shipCache.getTermsOfType("Ship");
        for(int a = 0; a < shipTerms.size(); a++) {
            Term test = shipTerms.get(a);
            if(test.getValue("type").equals("Container")) {
                cargoContainerTerm = test;
                break;
            }
        }
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

    public Parser getFactionCache() {
        return factionCache;
    }

    public Parser getStationCache() {
        return stationCache;
    }

    /*public Parser getExplosionCache() {
        return explosionCache;
    }*/

    public Parser getProcessCache() {
        return processCache;
    }

    public Parser getSkyCache() {
        return skyCache;
    }

    public Parser getLoadoutCache() {
        return loadoutCache;
    }

    public Parser getConversationCache() {
        return conversationCache;
    }

    public Parser getPlanetCache() {
        return planetCache;
    }

    public Parser getMissionCache() {
        return missionCache;
    }

    public Parser getNameCache() {
        return nameCache;
    }
    
    public Parser getQuoteCache() {
        return quoteCache;
    }
    
    public Term getCargoContainerTerm() {
        return cargoContainerTerm;
    }
}