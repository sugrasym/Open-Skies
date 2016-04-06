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
 * Represents a special ship that is a cargo container. This container
 * when collided with will transfer its contents into the ship that
 * collided with it.
 */
package celestial.Ship;

import cargo.Item;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class CargoContainer extends Ship {

    private static final float MAX_LIFE = 5000;
    private final float maxLife;
    private float life = 0;

    public CargoContainer(Universe universe, Item contents) {
        super(universe, Universe.getCache().getCargoContainerTerm(), "Neutral");
        addToCargoBay(contents);
        setName(contents.toString());
        maxLife = rnd.nextFloat() * MAX_LIFE;
    }

    @Override
    protected void aliveAlways() {
        super.aliveAlways();
        life += tpf;
        if (life > maxLife) {
            setState(State.DYING);
        }
    }

    @Override
    protected void dyingAlways() {
        //do nothing
    }

    @Override
    public String toString() {
        return getName() + " "
                + (int) ((float) (1.0f - (life / maxLife)) * 100.0f) + "%";
    }
}
