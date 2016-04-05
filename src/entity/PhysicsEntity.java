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
 * Represents a physics entity.
 */
package entity;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import java.io.IOException;
import java.io.Serializable;
import jmeplanet.PlanetAppState;

/**
 *
 * @author Nathan Wiehoff
 */
public class PhysicsEntity implements Entity, Serializable {
    //name

    private String name = "";
    //texture and geometry crap
    protected transient Material mat;
    private transient Spatial spatial;
    
    //entity crap
    private State state = State.ALIVE;
    /*
     *  WARNING WARNING WARNING
     *  When changing geometry location you must do it in the physics space.
     *  Changing it in the mesh will cause bad things.
     */
    Vector3f location = new Vector3f(0, 0, 0);
    Quaternion rotation = Quaternion.ZERO;
    private Vector3f velocity = new Vector3f(0, 0, 0);
    //physics crap
    protected transient RigidBodyControl physics;
    private float mass = 0;
    protected PhysicsNameControl nameControl = new PhysicsNameControl();

    public PhysicsEntity(float mass) {
        this.mass = mass;
    }

    /*
     * Implementations of entity
     */
    @Override
    public void periodicUpdate(float tpf) {

    }

    @Override
    public void oosPeriodicUpdate(float tpf) {
        //called only when the player is not in the same system
    }

    @Override
    public void construct(AssetManager assets) {
        //create the mesh and material
        Box b = new Box(Vector3f.ZERO, 1, 1, 1);
        setSpatial(new Geometry("Box", b));
        mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        getSpatial().setMaterial(mat);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(1.0f);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, mass);
        //add physics to mesh
        getSpatial().addControl(physics);
    }

    @Override
    public void deconstruct() {
        //TODO
    }

    /*
     * Used when attaching or detaching from the same local space (solar system)
     * that the player is in. There is no out of system physics simulation.
     * 
     * For out of system calls the object just needs to be alive or dying and in
     * the entity list the game engine updates.
     */
    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        this.physics.setLinearVelocity(getVelocity().clone());
        this.physics.setPhysicsLocation(location.clone());
        this.physics.setPhysicsRotation(rotation.clone());
        node.attachChild(getSpatial());
        physics.getPhysicsSpace().add(getSpatial());
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        setVelocity(this.physics.getLinearVelocity().clone());
        location = this.physics.getPhysicsLocation().clone();
        rotation = this.physics.getPhysicsRotation().clone();
        node.detachChild(getSpatial());
        physics.getPhysicsSpace().remove(getSpatial());
    }

    /*
     *More state information
     */
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Vector3f getLocation() {
        return location.clone();
    }

    @Override
    public void setLocation(Vector3f loc) {
        location = loc.clone();
        if(physics != null) {
            physics.setPhysicsLocation(loc.clone());
        }
    }

    @Override
    public Quaternion getRotation() {
        return rotation.clone();
    }

    @Override
    public void setRotation(Quaternion rot) {
        rotation = rot.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /*
     * Physics stuff
     */
    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public void applyCentralForce(Vector3f force) {
        physics.applyCentralForce(force);
    }

    public void applyForce(Vector3f force, Vector3f location) {
        physics.applyForce(force, location);
    }

    public void applyTorque(Vector3f torque) {
        physics.applyTorque(torque);
    }

    public void clearForces() {
        physics.clearForces();
    }

    /*
     * For getting physics info
     */
    public Quaternion getPhysicsRotation() {
        return physics.getPhysicsRotation();
    }

    @Override
    public Vector3f getPhysicsLocation() {
        if (physics != null) {
            return physics.getPhysicsLocation();
        } else {
            return getLocation();
        }
    }

    public Vector3f getAngularVelocity() {
        return physics.getAngularVelocity();
    }

    public Vector3f getLinearVelocity() {
        return physics.getLinearVelocity();
    }
    
    public final float distanceTo(PhysicsEntity a) {
        return a.getLocation().distance(getLocation());
    }
    
    /*
     * For those Aristotlian worlds.
     */
    public void setDamping(float linear, float angular) {
        physics.setDamping(linear, angular);
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity = velocity;
    }

    /**
     * @return the spatial
     */
    public Spatial getSpatial() {
        return spatial;
    }

    /**
     * @param spatial the spatial to set
     */
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    /*
     * Control for dereferencing physics objects from an event name
     */
    public class PhysicsNameControl implements Serializable, Control {

        private Object parent;

        public Object getParent() {
            return parent;
        }

        public void setParent(Object object) {
            this.parent = object;
        }

        /*
         * Cruft
         */
        @Override
        public Control cloneForSpatial(Spatial spatial) {
            return null;
        }

        @Override
        public void setSpatial(Spatial spatial) {
            //
        }

        @Override
        public void update(float tpf) {
            //
        }

        @Override
        public void render(RenderManager rm, ViewPort vp) {
            //
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            //
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            //
        }
    }
}
