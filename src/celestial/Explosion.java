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
 * A projectile that has been modified to be used as an explosion effect
 */
package celestial;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.math.ColorRGBA;
import java.util.Random;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Explosion extends Projectile {
    //default color constants

    private static final ColorRGBA START_COLOR = new ColorRGBA(1, 1, 0, 1f);
    private static final ColorRGBA END_COLOR = new ColorRGBA(1, 0, 0, 0.1f);
    private static final String DEFAULT_TEXTURE = "Effects/Trail/flamelet.png";
    //life constants
    private static final float MAX_LIFE = 3;
    private static final float MIN_LIFE = 0.5f;
    //count constants
    private static final int MAX_COUNT = 15;
    private static final int MIN_COUNT = 7;
    //end of constants
    private float maxLife;
    private float life;

    public Explosion(Universe universe, float size, String name) {
        super(universe, null, name, 0.0001f);
        setSize(size);
        initStats();
    }

    private void initStats() {
        //the explosion shouldn't be easily predictable unless tampered with before spawning
        Random rnd = new Random();
        setNumParticles((int) (rnd.nextFloat() * (MAX_COUNT - MIN_COUNT)) + MIN_COUNT);
        maxLife = (int) (rnd.nextFloat() * (MAX_LIFE - MIN_LIFE)) + MIN_LIFE;
        setHighLife(maxLife);
        setLowLife(MIN_LIFE);
        setVariation(rnd.nextFloat());
        setEmitterRate(0); //no new particles should be spawned
        setStartColor(START_COLOR);
        setEndColor(END_COLOR);
        setTexture(DEFAULT_TEXTURE);
    }

    @Override
    protected void alive() {
        //make sure it doesn't collide
        physics.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_NONE);
        physics.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_NONE);
        //increment lifetime
        life += tpf;
        if (life >= maxLife) {
            setState(State.DYING);
        }
    }
}
