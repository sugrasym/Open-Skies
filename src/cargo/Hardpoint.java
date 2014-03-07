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
 * Hardpoints are the locations where equipment is mounted.
 */
package cargo;

import celestial.Ship.Ship;
import entity.Entity;
import java.io.Serializable;

/**
 *
 * @author Nathan Wiehoff
 */
public class Hardpoint implements Serializable {

    String type;
    protected int size;
    protected Equipment mounted;
    Equipment empty = new Equipment("NOTHING");
    Ship host;
    //position in polar coodinates
    protected double r; //distance from center
    protected double t; //angle

    public Hardpoint(Ship host, String type, int size, double r, double t) {
        this.type = type;
        this.size = size;
        this.host = host;
        this.r = r;
        this.t = t;
        //mount nothing
        mount(empty);
    }

    public void activate(Entity target) {
        getMounted().activate(target);
    }

    public final void mount(Equipment equipment) {
        if ((int) equipment.getVolume() <= getSize()) {
            setMounted(equipment);
            getMounted().mount(host, this);
        }
    }

    public void unmount(Equipment equipment) {
        setMounted(empty);
        equipment.setSocket(null);
    }

    public void periodicUpdate(double tpf) {
        getMounted().periodicUpdate(tpf);
    }

    public boolean isEmpty() {
        if (getMounted().getName().matches("NOTHING")) {
            return true;
        } else {
            return false;
        }
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    @Override
    public String toString() {
        return getMounted().toString();
    }
    
    public boolean isEnabled() {
        return getMounted().isEnabled();
    }
    
    public void setEnabled(boolean enabled) {
        getMounted().setEnabled(enabled);
    }

    public Equipment getMounted() {
        return mounted;
    }

    private void setMounted(Equipment mounted) {
        //THIS DAMN METHOD HAD BETTER FUCKING STAY PRIVATE
        this.mounted = mounted;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
