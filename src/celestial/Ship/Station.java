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
 * Space station!
 */
package celestial.Ship;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Station extends Ship {
    public Station(Universe universe, Term type) {
        super(universe, type);
    }
    
    protected void alive() {
        //update parent
        super.alive();
        //update station stuff
    }
    
    public void construct(AssetManager assets) {
        //Get name
        String name = getType().getValue("type");
        //load model
        spatial = assets.loadModel("Models/" + name + "/Model.blend");
        //load texture
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/" + name + "/tex.png"));
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //setup texture
        spatial.setMaterial(mat);
        //setup physics
        CollisionShape hullShape = CollisionShapeFactory.createMeshShape(spatial);
        physics = new RigidBodyControl(hullShape, 0);
        spatial.addControl(physics);
        physics.setSleepingThresholds(0, 0);
        physics.setAngularDamping(0.99f); //i do NOT want to deal with this at 0
        physics.setLinearDamping(0.99f);
        spatial.setName(this.getClass().getName());
        //store physics name control
        nameControl.setParent(this);
        spatial.addControl(nameControl);
    }
}
