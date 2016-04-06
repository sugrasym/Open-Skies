/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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
 * Basic class that represents a generic celestial object. It should NEVER EVER
 * be used directly, instead it should be extended.
 */
package celestial;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import entity.PhysicsEntity;
import java.io.Serializable;
import lib.astral.AstralIO;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Celestial extends PhysicsEntity implements Serializable {

    //protected variables that need to be accessed by children
    protected double tpf;
    //identity
    protected static AstralIO io = new AstralIO();
    protected SolarSystem currentSystem;
    //discovery
    private boolean discoveredByPlayer = false;

    //camera
    private transient Node cameraRestPoint;
    private transient Node lineOfSightPoint;

    public Celestial(float mass, Universe universe) {
        super(mass);
    }
    /*
     * For compartmentalizing behaviors. This is a cleaner solution than
     * overriding the periodicUpdate method itself.
     */

    protected void alive() {
    }

    protected void dying() {
    }

    protected void dead() {
    }

    protected void oosAlive() {
    }

    protected void oosDying() {
    }

    protected void oosDead() {
    }

    public SolarSystem getCurrentSystem() {
        return currentSystem;
    }

    public void setCurrentSystem(SolarSystem currentSystem) {
        this.currentSystem = currentSystem;
    }

    @Override
    public void periodicUpdate(float tpf) {
        super.periodicUpdate(tpf);
        this.tpf = tpf;
        if (getState() == State.ALIVE) {
            alive();
        } else if (getState() == State.DYING) {
            dying();
        } else if (getState() == State.DEAD) {
            dead(); //and why is this being updated?
        } else {
            throw new UnsupportedOperationException("Error: " + getName() + " is in an undefined state.");
        }
    }

    @Override
    public void oosPeriodicUpdate(float tpf) {
        super.oosPeriodicUpdate(tpf);
        this.tpf = tpf;
        if (getState() == State.ALIVE) {
            oosAlive();
        } else if (getState() == State.DYING) {
            oosDying();
        } else if (getState() == State.DEAD) {
            oosDead(); //and why is this being updated?
        } else {
            throw new UnsupportedOperationException("Error: " + getName() + " is in an undefined state.");
        }
    }

    public void construct(Universe universe) {
        construct(universe.getAssets());
    }
    
    @Override
    public void deconstruct() {
        super.deconstruct();
        
        //reset camera tracking aids
        cameraRestPoint = null;
        lineOfSightPoint = null;
    }

    public boolean isDiscoveredByPlayer() {
        return discoveredByPlayer;
    }

    public void setDiscoveredByPlayer(boolean discoveredByPlayer) {
        this.discoveredByPlayer = discoveredByPlayer;
    }

    public void discover() {
        this.discoveredByPlayer = true;
    }

    public Vector3f getCameraRestPoint() {
        //hasn't been constructed so just stare into space
        if (cameraRestPoint == null) {
            cameraRestPoint = new Node();
            cameraRestPoint.move(0, 4, 15);
            ((Node) getSpatial()).attachChild(cameraRestPoint);
        }
        return cameraRestPoint.getWorldTranslation();
    }

    public Vector3f getLineOfSightPoint() {
        //default camera will be looking at where the front of this object is pointing
        if (lineOfSightPoint == null) {
            lineOfSightPoint = new Node();
            lineOfSightPoint.move(0, 0, -1000);
            ((Node) getSpatial()).attachChild(lineOfSightPoint);
        }
        return lineOfSightPoint.getWorldTranslation();
    }
}
