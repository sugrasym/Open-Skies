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
 * Now for some meat. This class represents a turret.
 */
package cargo;

import entity.Entity;
import java.util.ArrayList;
import lib.Parser;

/**
 *
 * @author Nathan Wiehoff
 */
public class Weapon extends Equipment {
    int width;
    int height;
    //weapon properties
    protected double damage;
    protected double speed;

    public Weapon(String name) {
        super(name);
        init();
    }

    private void init() {
        //get weapon stuff now
        Parser parse = new Parser("WEAPONS.txt");
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Weapon");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.matches(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            setName(relevant.getValue("name"));
            setType(relevant.getValue("type"));
            setMass(Double.parseDouble(relevant.getValue("mass")));
            setDamage(Double.parseDouble(relevant.getValue("damage")));
            setRange(Double.parseDouble(relevant.getValue("range")));
            setSpeed(Double.parseDouble(relevant.getValue("speed")));
            setCoolDown(Double.parseDouble(relevant.getValue("refire")));
        } else {
            System.out.println("Hades: The item " + getName() + " does not exist in WEAPONS.txt");
        }
    }

    @Override
    public void activate(Entity target) {
        if (getCoolDown() <= getActivationTimer() && enabled) {
            setActivationTimer(0); //restart cooldown
            fire();
        }
    }

    private void fire() {
        if (enabled) {
            //TODO: FIRE WEAPON PROJECTILE
        }
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
