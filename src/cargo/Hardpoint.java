/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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
    private transient Node upNode;
    private float gimbal;

    public Hardpoint(Ship host, String type, int size, Vector3f loc, Vector3f up, float gimbal) {
        this.type = type;
        this.size = size;
        this.host = host;
        this.loc = loc;
        this.up = up;
        this.gimbal = gimbal;
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
        return getMounted().getName().matches("NOTHING");
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
    
    public Node getUpNode() {
        return upNode;
    }

    public void setUpNode(Node upNode) {
        this.upNode = upNode;
    }
    
    public void initNodes() {
        node = new Node();
        node.move(loc);
        upNode = new Node();
        upNode.move(loc.add(up));
    }
    
    public void showDebugHardpoint(AssetManager assets) {
        addNodeDebug(assets);   
        addUpNodeDebug(assets);
    }

    private void addNodeDebug(AssetManager assets) {
        Box point = new Box(0.1f,0.1f,0.1f);
        Geometry red = new Geometry("DebugHardpoint", point);
        red.setLocalTranslation(Vector3f.ZERO);
        Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat);
        //add to node
        node.attachChild(red);
    }
    
    private void addUpNodeDebug(AssetManager assets) {
        Box point = new Box(0.1f,0.1f,0.1f);
        Geometry gray = new Geometry("DebugHardpoint", point);
        gray.setLocalTranslation(Vector3f.ZERO);
        Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.LightGray);
        gray.setMaterial(mat);
        //add to node
        upNode.attachChild(gray);
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

    public float getGimbal() {
        return gimbal;
    }

    public void setGimbal(float gimbal) {
        this.gimbal = gimbal;
    }
}
