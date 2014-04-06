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
 * Equipment is a framwork class for things that can be mounted to hard points.
 */
package cargo;

import celestial.Ship.Ship;
import entity.Entity;
import java.io.Serializable;

/**
 *
 * @author nwiehoff
 */
public class Equipment extends Item implements Serializable {

    protected boolean active = false;
    protected boolean enabled = true;
    protected double activationTimer;
    protected double coolDown;
    protected Ship host;
    protected Hardpoint socket;
    protected float range;
    protected float tpf;

    public Equipment(String name) {
        super(name);
    }

    @Override
    public void periodicUpdate(double tpf) {
        //update timer
        if (getActivationTimer() <= getCoolDown()) {
            setActivationTimer(getActivationTimer() + tpf);
        }
        this.tpf = (float) tpf;
    }

    public void activate(Entity target) {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getActivationTimer() {
        return activationTimer;
    }

    public void setActivationTimer(double activationTimer) {
        this.activationTimer = activationTimer;
    }

    public double getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(double coolDown) {
        this.coolDown = coolDown;
    }

    public void mount(Ship host, Hardpoint socket) {
        this.host = host;
        this.setSocket(socket);
    }

    @Override
    public String toString() {
        String ret = "";
        if (enabled) {
            //return cooldown status
            double percentCooled = activationTimer / coolDown;
            int percent = (int) (100.0 * percentCooled);
            if (percent > 100) {
                percent = 100;
            }
            ret = "(" + percent + "%) " + getName();
        } else {
            ret = (getName()+ " [OFFLINE]");
        }
        if(quantity != 1) {
            ret += " ["+quantity+"]";
        }
        return ret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public Hardpoint getSocket() {
        return socket;
    }

    public void setSocket(Hardpoint socket) {
        this.socket = socket;
    }
}
