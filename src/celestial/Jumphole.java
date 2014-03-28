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
import java.util.Random;
import jmeplanet.PlanetAppState;
import lib.astral.Parser;
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
    public static final ColorRGBA START_COLOR = new ColorRGBA(1, 1, 1, 1);
    public static final ColorRGBA END_COLOR = new ColorRGBA(0, 0, 1, 0.5f);
    //hole stuff
    private Jumphole outGate;
    protected String out = "n/n";
    
    public Jumphole(Universe universe, String name) {
        super(universe, name, null, 15);
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
}
