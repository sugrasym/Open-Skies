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
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import java.util.Random;
import jmeplanet.PlanetAppState;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Star extends Planet {
    
    PointLight light;
    transient ParticleEmitter emitter;
    private int seed;
    Random rnd;
    
    public Star(Universe universe, String name, Term texture, float radius) {
        super(universe, name, texture, radius);
    }
    
    public void construct(AssetManager assets) {
        //create geometry
        Sphere objectSphere = new Sphere(64, 64, radius);
        spatial = new Geometry("StarSphere", objectSphere);
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
        //setup particle
        setupCoreParticle(assets, col);
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
    
    private void setupCoreParticle(AssetManager assets, ColorRGBA color) {
        //create the emitter
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 1);
        mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Star/basic.png"));
        //mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //mat.getAdditionalRenderState().setAlphaTest(true);
        emitter.setMaterial(mat);
        //setup emitter
        emitter.setEnabled(true);
        emitter.setImagesX(1);
        emitter.setImagesY(1);
        emitter.setEndColor(color);
        emitter.setStartColor(color); // IRRELEVENT
        emitter.setStartSize(radius * 2);
        emitter.setEndSize(radius * 2);
        emitter.setGravity(0, 0, 0);
        emitter.setLowLife(Float.MAX_VALUE);
        emitter.setHighLife(Float.MAX_VALUE);
        emitter.getParticleInfluencer().setVelocityVariation(0f);
        /*
         * TODO : ONCE PLAYER SHIP EXISTS ADD NEBULA FOG AND LIGHTNING
         */
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
        //node.attachChild(spatial);
        physics.getPhysicsSpace().add(spatial);
        node.addLight(light);
        node.attachChild(emitter);
    }
    
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //node.detachChild(spatial);
        physics.getPhysicsSpace().remove(spatial);
        node.removeLight(light);
        node.detachChild(emitter);
    }
    
    protected void alive() {
        super.alive();
        if (light != null) {
            light.setPosition(getLocation());
        }
        if (emitter != null) {
            emitter.setLocalTranslation(getLocation());
        }
    }
    
    public int getSeed() {
        return seed;
    }
    
    public void setSeed(int seed) {
        this.seed = seed;
    }
}
