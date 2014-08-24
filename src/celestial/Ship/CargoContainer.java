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
        if(life > maxLife) {
            setState(State.DYING);
        }
    }
    
    @Override
    protected void dyingAlways() {
        //do nothing
    }
    
    @Override
    public String toString() {
        return getName() + " " + 
                (int)((float)(1.0f-(life / maxLife)) * 100.0f)+"%";
    }
}
