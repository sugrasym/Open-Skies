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
 * Formerly a camera controller, now provides calculations to support
 * PlanetAppState as a camera controller.
 */
package engine;

import celestial.Ship.Ship;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author nwiehoff
 */
public class AstralCamera {

    enum Mode {

        LOOSE,
        TIGHT,
        CHASE
    }
    Mode mode = Mode.LOOSE;
    //engine resources
    Node node = new Node();
    Node camNode = new Node();
    Node lookNode = new Node();
    private Camera cam;
    //target
    private Ship target;
    private Spatial spatial;

    public AstralCamera(Camera cam) {
        this.cam = cam;
        //setup node
        setupNode();
    }

    public void periodicUpdate(float tpf) {
        if (spatial != null) {
            if (mode == Mode.LOOSE) {
                node.setLocalTranslation(spatial.getWorldTranslation());
                node.setLocalRotation(spatial.getWorldRotation());
                //copy to camera
                cam.setLocation(camNode.getWorldTranslation());
                cam.lookAt(lookNode.getWorldTranslation(),
                        target.getPhysicsRotation().mult(Vector3f.UNIT_Y));
            } else if (mode == Mode.TIGHT) {
                node.setLocalTranslation(spatial.getWorldTranslation());
                node.setLocalRotation(spatial.getWorldRotation());
                cam.setLocation(camNode.getWorldTranslation());
                cam.lookAt(node.getWorldTranslation(),
                        target.getPhysicsRotation().mult(Vector3f.UNIT_Y));
            }
        } else {
            spatial = target.getSpatial();
        }
    }

    public Ship getTarget() {
        return target;
    }

    public void setTarget(Ship target) {
        /*
         * WARNING: When you set a target, the target should have no rotation.
         */
        node.detachAllChildren();
        this.target = target;
        this.spatial = target.getSpatial();
        setupNode();
    }
    
    public Vector3f getScreenCoordinates(Vector3f position)
    {
        return cam.getScreenCoordinates(position);
    }
    
    public Vector3f getLocation() {
        return cam.getLocation();
    }
    
    public Vector3f getDirection() {
        return cam.getDirection();
    }

    private void setupNode() {
        camNode.move(0, 4, 15);
        lookNode.move(0, 4, 0);
        node.attachChild(camNode);
        node.attachChild(lookNode);
    }
}
