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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import jmeplanet.PlanetAppState;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Star extends Planet {

    transient PointLight light;
    transient ParticleEmitter emitter;
    private int seed;
    private final String color;

    public Star(Universe universe, String name, Term texture, String color,
            float radius) {
        super(universe, name, texture, radius, Vector3f.ZERO);
        this.color = color;
    }

    @Override
    public void construct(AssetManager assets) {
        //create geometry
        Sphere objectSphere = new Sphere(64, 64, radius);
        setSpatial(new Geometry("StarSphere", objectSphere));
        //retrieve texture
        mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        //setup color
        String[] arr = color.split(",");
        float r = Float.parseFloat(arr[0]);
        float g = Float.parseFloat(arr[1]);
        float b = Float.parseFloat(arr[2]);
        ColorRGBA col = new ColorRGBA(r, g, b, 1f);
        mat.setColor("Color", col);
        getSpatial().setMaterial(mat);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //setup particle
        setupCoreParticle(assets, col);
        //add physics to mesh
        getSpatial().addControl(physics);
        //setup light
        light = new PointLight();
        light.setRadius(Float.MAX_VALUE);
        light.setColor(col);
        //store physics name control
        nameControl.setParent(this);
        getSpatial().addControl(nameControl);
        //death zone
        setAtmosphereScaler(0.1f);
    }

    @Override
    public void deconstruct() {
        super.deconstruct();
        light = null;
    }

    private void setupCoreParticle(AssetManager assets, ColorRGBA color) {
        //create the emitter
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 1);
        mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Star/"
                + getType().getValue("asset")));
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

    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //node.attachChild(spatial);
        physics.getPhysicsSpace().add(getSpatial());
        node.addLight(light);
        node.attachChild(emitter);
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //node.detachChild(spatial);
        physics.getPhysicsSpace().remove(getSpatial());
        node.removeLight(light);
        node.detachChild(emitter);
    }

    @Override
    protected void alive() {
        super.alive();
        if (light != null) {
            light.setPosition(getLocation());
        }
        if (emitter != null) {
            emitter.killAllParticles();
            emitter.emitAllParticles();
            emitter.setLocalTranslation(getLocation());
        }
    }

    @Override
    public int getSeed() {
        return seed;
    }

    @Override
    public void setSeed(int seed) {
        this.seed = seed;
    }
}
