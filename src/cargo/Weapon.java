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

import celestial.Projectile;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import entity.Entity;
import java.util.ArrayList;
import lib.astral.Parser;

/**
 *
 * @author Nathan Wiehoff
 */
public class Weapon extends Equipment {

    int width;
    int height;
    //weapon properties
    private float shieldDamage;
    private float hullDamage;
    private float speed;

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
            setMass(Float.parseFloat(relevant.getValue("mass")));
            setShieldDamage(Float.parseFloat(relevant.getValue("shieldDamage")));
            setHullDamage(Float.parseFloat(relevant.getValue("hullDamage")));
            setRange(Float.parseFloat(relevant.getValue("range")));
            setSpeed(Float.parseFloat(relevant.getValue("speed")));
            setCoolDown(Float.parseFloat(relevant.getValue("refire")));
        } else {
            System.out.println("Error: The item " + getName() + " does not exist in WEAPONS.txt");
        }
    }

    @Override
    public void activate(Entity target) {
        if (getCoolDown() <= getActivationTimer() && enabled) {
            setActivationTimer(0); //restart cooldown
            //determine if OOS or not
            if (host.getCurrentSystem() == host.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem()) {
                fire(target);
            } else {
                oosFire(target);
            }
        }
    }

    private void fire(Entity target) {
        /*
         * This is the one called in system. It uses the physics system and is
         * under the assumption that the host has been fully constructed. It
         * generates a projectile using precached values, assigns those values,
         * and finally drops the projectile into space.
         */
        if (enabled) {
            //generate projectile
            Projectile pro = new Projectile(host.getCurrentSystem().getUniverse(), getName());
            //store stats
            pro.setShieldDamage(shieldDamage);
            pro.setHullDamage(hullDamage);
            pro.setSpeed(speed);
            pro.setRange(speed);
            //determine world location and rotation
            Vector3f loc = getSocket().getNode().getWorldTranslation();
            Quaternion rot = getSocket().getNode().getWorldRotation();
            //interpolate velocity
            
            //store physics
            pro.setLocation(loc);
            pro.setRotation(rot);
        }
    }

    private void oosFire(Entity target) {
        if (enabled) {
            //TODO: DEAL DAMAGE TO TARGET DIRECTLY
        }
    }

    public float getShieldDamage() {
        return shieldDamage;
    }

    public void setShieldDamage(float shieldDamage) {
        this.shieldDamage = shieldDamage;
    }

    public float getHullDamage() {
        return hullDamage;
    }

    public void setHullDamage(float hullDamage) {
        this.hullDamage = hullDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
