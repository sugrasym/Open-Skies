/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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

    @Override
    protected void dying() {
        //since this extends projectile we don't want to draw another explosion
        setState(State.DEAD);
    }
}
