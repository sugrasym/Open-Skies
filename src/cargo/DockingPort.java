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
 * Represents a point on a station where you can dock with it.
 */
package cargo;

import celestial.Ship.Ship;
import celestial.Ship.Station;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nwiehoff
 */
public class DockingPort implements Serializable {
    protected String type;
    protected int size;
    protected Ship docked;
    protected Station host;
    //relative position in x,y,z from the origin
    protected Vector3f loc;
    protected transient Node node;

    public DockingPort(Station host, String type, int size, Vector3f loc) {
        this.type = type;
        this.size = size;
        this.host = host;
        this.loc = loc;
    }
    
    public Station getParent() {
        return host;
    }
}
