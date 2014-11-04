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

import celestial.Celestial;
import celestial.Ship.Ship;
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
 * @author Nathan Wiehoff
 */
public class Hardpoint implements Serializable {

    private String type;
    private int size;
    private Equipment mounted;
    Equipment empty = new Equipment("NOTHING");
    Ship host;
    //relative position in x,y,z from the origin
    private Vector3f loc;
    private Vector3f up;
    private transient Node node;

    public Hardpoint(Ship host, String type, int size, Vector3f loc, Vector3f up) {
        this.type = type;
        this.size = size;
        this.host = host;
        this.loc = loc;
        this.up = up;
        //mount nothing
        mount(empty);
    }

    public void activate(Celestial target) {
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
        equipment.deconstruct();
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
        this.mounted = mounted;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Vector3f getLoc() {
        return loc;
    }

    public void setLoc(Vector3f loc) {
        this.loc = loc;
    }
    
    public Vector3f getUp() {
        return up;
    }

    public void setUp(Vector3f up) {
        this.up = up;
    }
    
    public boolean notNothing() {
        return !mounted.getName().equals("NOTHING");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
    }
    
    public void showDebugHardpoint(AssetManager assets) {
        Box point = new Box(0.1f,0.1f,0.1f);      
        Geometry red = new Geometry("DebugHardpoint", point);
        red.setLocalTranslation(Vector3f.ZERO);
        Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat);
        //add to node
        node.attachChild(red);
    }
    
    public void construct(AssetManager assets) {
        if(mounted != null) {
            mounted.construct(assets);
        }
    }
    
    public void deconstruct() {
        if(mounted != null) {
            mounted.deconstruct();
        }
        killSound();
    }
    
    public void killSound() {
        if(mounted != null) {
            mounted.killSound();
        }
    }
}
