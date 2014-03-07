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
 * Basic class that represents a generic celestial object. It should NEVER EVER
 * be used directly, instead it should be extended.
 */
package celestial;

import entity.PhysicsEntity;
import java.io.Serializable;
import lib.astral.AstralIO;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Celestial extends PhysicsEntity implements Serializable {

    //protected variables that need to be accessed by children
    protected double tpf;
    protected Universe universe;
    //identity
    protected static AstralIO io = new AstralIO();
    protected SolarSystem currentSystem;

    public Celestial(float mass, Universe universe) {
        super(mass);
        this.universe = universe;
    }
    /*
     * For compartmentalizing behaviors. This is a cleaner solution than
     * overriding the periodicUpdate method itself.
     */

    protected void alive() {
        updateVectors();
    }

    protected void dying() {
        setState(State.DEAD);
    }

    protected void dead() {
    }

    public SolarSystem getCurrentSystem() {
        return currentSystem;
    }
    
    public void updateVectors() {
        setLocation(physics.getPhysicsLocation().clone());
        setRotation(physics.getPhysicsRotation().clone());
        setVelocity(physics.getLinearVelocity().clone());
    }

    public void setCurrentSystem(SolarSystem currentSystem) {
        this.currentSystem = currentSystem;
    }

    @Override
    public void periodicUpdate(float tpf) {
        this.tpf = tpf;
        if (getState() == State.ALIVE) {
            alive();
        } else if (getState() == State.DYING) {
            dying();
        } else if (getState() == State.DEAD) {
            dead(); //and why is this being updated?
        } else {
            throw new UnsupportedOperationException("Error: " + getName() + " is in an undefined state.");
        }
    }
}
