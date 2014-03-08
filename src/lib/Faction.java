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
 * Maps a set of likes and dislikes to a celestial. Useful for starting fights
 * and restricting docking.
 * 
 * It is accepted that
 *  - Standings are symmetrical.
 * Therefore it is ok to ask the enemy how much they like you, because you
 * WILL get a brutally honest answer.
 */
package lib;

import java.io.Serializable;
import java.util.ArrayList;
import lib.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class Faction implements Serializable {

    private String name;
    private Term standings;

    public Faction(String name) {
        this.name = name;
        init();
    }

    private void init() {
        Parser tmp = new Parser("FACTIONS.txt");
        ArrayList<Term> factions = tmp.getTermsOfType("Faction");
        System.out.println(factions.size());
        for (int a = 0; a < factions.size(); a++) {
            if (factions.get(a).getValue("name").matches(name)) {
                standings = factions.get(a);
                break;
            }
        }
    }

    public int getStanding(String faction) {
        if (standings != null) {
            String tmp = standings.getValue(faction);
            if (tmp != null) {
                return Integer.parseInt(tmp);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void setStanding(String faction, int value) {
        if (value < -10) {
            value = -10;
        } else if (value > 10) {
            value = 10;
        }
        standings.setValue(faction, value + "");
    }
}
