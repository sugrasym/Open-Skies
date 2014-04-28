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
