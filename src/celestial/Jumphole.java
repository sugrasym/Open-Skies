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
 * "Planet" that is a particle effect that magically transports you to another
 * system.
 */
package celestial;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import entity.Entity;
import java.util.ArrayList;
import jmeplanet.PlanetAppState;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Jumphole extends Planet {

    transient ParticleEmitter emitter;
    private int seed;
    //colors
    public static final ColorRGBA START_COLOR = new ColorRGBA(1, 1, 0.5f, 1);
    public static final ColorRGBA END_COLOR = new ColorRGBA(0, 0, 1, 0.5f);
    //hole stuff
    private Jumphole outGate;
    protected String out = "n/n";

    public Jumphole(Universe universe, String name) {
        super(universe, name, null, 15, Vector3f.ZERO);
    }

    @Override
    public void construct(AssetManager assets) {
        //create geometry
        Sphere objectSphere = new Sphere(64, 64, radius);
        spatial = new Geometry("JumpholeSphere", objectSphere);
        //retrieve texture
        mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", START_COLOR);
        spatial.setMaterial(mat);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //setup particle
        setupCoreParticle(assets);
        //add physics to mesh
        spatial.addControl(physics);
        nameControl.setParent(this);
        spatial.addControl(nameControl);
    }

    private void setupCoreParticle(AssetManager assets) {
        //create the emitter
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 120);
        mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Star/basic.png"));
        //mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //mat.getAdditionalRenderState().setAlphaTest(true);
        emitter.setMaterial(mat);
        //setup emitter
        emitter.setEnabled(true);
        emitter.setImagesX(1);
        emitter.setImagesY(1);
        emitter.setEndColor(END_COLOR);
        emitter.setStartColor(START_COLOR); // IRRELEVENT
        emitter.setStartSize(radius * 2);
        emitter.setEndSize(0);
        emitter.setGravity(0, 0, 0);
        emitter.setLowLife(15);
        emitter.setHighLife(60);
        emitter.getParticleInfluencer().setVelocityVariation(1f);
        emitter.getParticleInfluencer().setInitialVelocity(Vector3f.UNIT_XYZ);
        emitter.setParticlesPerSec(2);
    }

    @Override
    public void deconstruct() {
        super.deconstruct();
    }

    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //node.attachChild(spatial);
        physics.getPhysicsSpace().add(spatial);
        node.attachChild(emitter);
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        //node.detachChild(spatial);
        physics.getPhysicsSpace().remove(spatial);
        node.detachChild(emitter);
    }

    @Override
    protected void alive() {
        super.alive();
        if (emitter != null) {
            emitter.setLocalTranslation(getLocation());
        }
        aliveAlways();
    }

    @Override
    protected void oosAlive() {
        super.oosAlive();
        aliveAlways();
    }

    @Override
    public int getSeed() {
        return seed;
    }

    @Override
    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void createLink(String out) {
        /*
         * Locates this gate's partner in the target solar system.
         */
        String outSysTmp = out.split("/")[0];
        String outGateTmp = out.split("/")[1];
        //find the out link
        Universe universe = getCurrentSystem().getUniverse();
        for (int a = 0; a < universe.getSystems().size(); a++) {
            SolarSystem curr = universe.getSystems().get(a);
            if (curr.getName().equals(outSysTmp)) {
                for (int b = 0; b < curr.getCelestials().size(); b++) {
                    Entity entity = curr.getCelestials().get(b);
                    if (entity instanceof Jumphole) {
                        if (entity.getName().equals(outGateTmp)) {
                            outGate = (Jumphole) entity;
                            outGate.linkWithPartner(this);
                        }
                    }
                }
            }
        }
    }

    public void linkWithPartner(Jumphole gate) {
        outGate = gate;
    }

    public void setOut(String out) {
        this.out = out;
    }

    private void jumpShip(Ship ship) {
        SolarSystem start = ship.getCurrentSystem();
        SolarSystem end = outGate.getCurrentSystem();
        //pull from old system
        start.pullEntityFromSystem(ship);
        //store location
        Vector3f diff = getLocation().subtract(ship.getLocation());
        ship.setLocation(outGate.getLocation().add(diff.mult(2)));
        //complete transfer
        end.putEntityInSystem(ship);

    }

    private void checkForJumpers() {
        //check for any ships to jump
        ArrayList<Entity> near = getCurrentSystem().getShipList();
        for (int a = 0; a < near.size(); a++) {
            Ship tmp = (Ship) near.get(a);
            float dist = tmp.getLocation().distance(getLocation());
            if (dist <= 1.5f * radius) {
                //jump
                jumpShip(tmp);
            }
        }
    }

    private void aliveAlways() {
        //guarantee link
        if (outGate == null) {
            createLink(out);
        }
        //see if anything can jump through
        checkForJumpers();
    }

    public String getOut() {
        return out;
    }

    public Jumphole getOutGate() {
        return outGate;
    }

    @Override
    public float getSafetyZone(float caution) {
        return 0;
    }
}
