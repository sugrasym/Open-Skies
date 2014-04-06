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
 * Defines a projectile. Projectiles are a particle effect that represents a
 * shot fired from a gun.
 */
package celestial;

import com.jme3.effect.ParticleEmitter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Projectile extends Celestial {
    //particle effect

    transient ParticleEmitter emitter;
    //stats
    private float shieldDamage;
    private float hullDamage;
    private float speed;
    private float range;
    //lifetime counter
    private float diff = 0;

    public Projectile(Universe universe, String name) {
        super(0, universe);
        setName(name);
    }

    /*
     * OOS, weapons always hit and no projectile is generated. This means that
     * the only one we have to worry about is alive().
     */
    @Override
    protected void alive() {
        //increment lifespan
        diff += getVelocity().length() * tpf;
        //has the projectile exceeded its max range?
        if (diff >= range) {
            //yep
            setState(State.DYING);
        } else {
            //nope
        }
    }

    @Override
    protected void dying() {
        setState(State.DEAD);
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

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }
}
