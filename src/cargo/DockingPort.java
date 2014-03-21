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
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.io.Serializable;

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

    public void periodicUpdate(double tpf) {
        //
    }

    public boolean isEmpty() {
        return (docked == null);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void initNode() {
        node = new Node();
        node.move(loc);
        System.out.println(loc);
    }

    public void showDebugHardpoint(AssetManager assets) {
        Box point = new Box(size,size,size);
        Geometry blue = new Geometry("DebugHardpoint", point);
        blue.setLocalTranslation(Vector3f.ZERO);
        Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        blue.setMaterial(mat);
        //add to node
        node.attachChild(blue);
    }
}
