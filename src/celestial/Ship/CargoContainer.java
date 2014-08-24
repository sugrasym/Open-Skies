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

import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */

public class CargoContainer extends Ship {
    public CargoContainer(Universe universe, Term type, String faction) {
        super(universe, type, faction);
    }
}
