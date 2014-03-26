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
import com.jme3.scene.shape.Sphere;
import java.io.Serializable;

/**
 *
 * @author nwiehoff
 */
public class DockingPort implements Serializable {

    public static final int DOCK_SPEED_LIMIT = 10;
    protected String type;
    protected int size;
    protected Ship client;
    protected Station host;
    //relative position in x,y,z from the origin
    protected Vector3f loc;
    protected Vector3f align;
    protected transient Node node;
    protected transient Node alignNode;

    public DockingPort(Station host, String type, int size, Vector3f loc, Vector3f align) {
        this.type = type;
        this.size = size;
        this.host = host;
        this.loc = loc;
        this.align = align;
    }

    public Station getParent() {
        return host;
    }

    public void periodicUpdate(double tpf) {
        /*
         * This periodic update method is for in-system use only and therefore
         * uses the physics system.
         */
        if (client != null) {
            if (!client.isDocked()) {
                //make sure client is in the same solar system
                if (client.getAutopilot() == Ship.Autopilot.UNDOCK) {
                    //don't interfere with undocking process
                } else if (client.getCurrentSystem() == host.getCurrentSystem()) {
                    //get client position
                    Vector3f cLoc = client.getPhysicsLocation();
                    //get node position
                    Vector3f nLoc = node.getWorldTranslation();
                    //find distance between these points
                    float dist = cLoc.distance(nLoc);
                    if (dist < size && client.getLinearVelocity().length() < DOCK_SPEED_LIMIT) {
                        //dock the ship
                        client.setDocked(true);
                        client.clearForces();
                        client.setPhysicsLocation(nLoc);
                    }
                }
            } else {
                //keep client synced in bay
                client.setPhysicsLocation(node.getWorldTranslation());
                client.nullVelocity();
            }
        }
    }

    public void oosPeriodicUpdate(double tpf) {
        //TODO
    }

    public boolean isEmpty() {
        return (client == null);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getAlign() {
        return alignNode;
    }

    public void setAlign(Node align) {
        this.alignNode = align;
    }

    public void setClient(Ship client) {
        this.client = client;
    }

    public Ship getClient() {
        return client;
    }

    public int getSize() {
        return size;
    }

    public void initNode() {
        node = new Node();
        node.move(loc);
        alignNode = new Node();
        alignNode.move(align);
        //System.out.println(loc);
    }

    public void release() {
        //releases the docked ship
        if (client != null) {
            client.setDocked(false);
            client = null;
        }
    }

    public void showDebugHardpoint(AssetManager assets) {
        //show docking node
        {
            Sphere point = new Sphere(size, size, size);
            Geometry blue = new Geometry("DebugDockingPort", point);
            blue.setLocalTranslation(Vector3f.ZERO);
            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            blue.setMaterial(mat);
            //add to node
            node.attachChild(blue);
        }
        //show align node
        {
            Sphere point = new Sphere(size, size, size);
            Geometry blue = new Geometry("DebugDockingAlign", point);
            blue.setLocalTranslation(Vector3f.ZERO);
            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Magenta);
            blue.setMaterial(mat);
            //add to node
            alignNode.attachChild(blue);
        }
    }
}
