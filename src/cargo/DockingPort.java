/*
 * Copyright (c) 2015 Nathan Wiehoff
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
import entity.Entity.State;
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
            if (client.getState() == State.ALIVE) {
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
            } else {
                //dead ships don't dock
                client = null;
            }
        }
    }

    public void oosPeriodicUpdate(double tpf) {
        if (client != null) {
            if (client.getState() == State.ALIVE) {
                if (!client.isDocked()) {
                    //make sure client is in the same solar system
                    if (client.getAutopilot() == Ship.Autopilot.UNDOCK) {
                        //don't interfere with undocking process
                    } else if (client.getCurrentSystem() == host.getCurrentSystem()) {
                        //get client position
                        Vector3f cLoc = client.getLocation();
                        //get node position
                        Vector3f nLoc = rawPortPosition();
                        //find distance between these points
                        float dist = cLoc.distance(nLoc);
                        if (dist < size && client.getVelocity().length() < DOCK_SPEED_LIMIT) {
                            //dock the ship
                            client.setDocked(true);
                            client.setLocation(nLoc);
                        }
                    }
                } else {
                    //keep client synced in bay
                    client.setLocation(rawPortPosition());
                    client.setVelocity(Vector3f.ZERO);
                }
            } else {
                //dead ships don't dock
                client = null;
            }
        }
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
    
    public Vector3f rawPortPosition() {
        return host.getLocation().add(loc);
    }
    
    public Vector3f rawAlignPosition() {
        return host.getLocation().add(align);
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
