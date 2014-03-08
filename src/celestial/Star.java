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
 * "Planet" that also contains a light emitter.
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import jmeplanet.PlanetAppState;
import lib.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Star extends Planet {

    PointLight light;

    public Star(Universe universe, String name, Term texture, float radius) {
        super(universe,name, texture, radius);
    }

    public void construct(AssetManager assets) {
        //create geometry
        Sphere objectSphere = new Sphere(256, 256, radius);
        spatial = new Geometry("Planet", objectSphere);
        //retrieve texture
        mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        //setup color
        String[] arr = getType().getValue("color").split(",");
        float r = Float.parseFloat(arr[0]);
        float g = Float.parseFloat(arr[1]);
        float b = Float.parseFloat(arr[2]);
        ColorRGBA col = new ColorRGBA(r, g, b, 1f);
        mat.setColor("Color", col);
        spatial.setMaterial(mat);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //add physics to mesh
        spatial.addControl(physics);
        //setup light
        light = new PointLight();
        light.setRadius(Float.MAX_VALUE);
        light.setColor(col);
    }

    public void deconstruct() {
        super.deconstruct();
        light = null;
    }

    /*private ColorRGBA adjustAmbientLighting(ColorRGBA light) {
     //acuire color info
     float r = light.getRed();
     float g = light.getGreen();
     float b = light.getBlue();
     //Adjusts the star color to be 50% dimmer
     r -= (0.0 * r);
     g -= (0.0 * g);
     b -= (0.0 * b);
     return new ColorRGBA(r, g, b, light.getAlpha());
     }*/
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.attachChild(spatial);
        physics.getPhysicsSpace().add(spatial);
        node.addLight(light);
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.detachChild(spatial);
        physics.getPhysicsSpace().remove(spatial);
        node.removeLight(light);
    }

    protected void alive() {
        super.alive();
        if (light != null) {
            light.setPosition(getLocation());
        }
    }
}
