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
 * Camera controller
 */
package engine;

import celestial.Ship.Ship;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author nwiehoff
 */
public class AstralCamera implements Serializable, Control {

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
                node.setLocalTranslation(spatial.getLocalTranslation());
                node.setLocalRotation(spatial.getLocalRotation());
                //copy to camera
                cam.setLocation(camNode.getWorldTranslation());
                cam.lookAt(lookNode.getWorldTranslation(),
                        target.getPhysicsRotation().mult(Vector3f.UNIT_Y));
            } else if (mode == Mode.TIGHT) {
                node.setLocalTranslation(spatial.getLocalTranslation());
                node.setLocalRotation(spatial.getLocalRotation());
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
        target.getSpatial().addControl(this);
        setupNode();
    }

    private void setupNode() {
        camNode.move(0, 4, 15);
        lookNode.move(0, 4, 0);
        node.attachChild(camNode);
        node.attachChild(lookNode);
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    @Override
    public void update(float tpf) {
        periodicUpdate(tpf);
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        //nothing
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        //nothing
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        //nothing
    }
}
