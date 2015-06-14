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
 * Impossibly dense clouds of colorful stuff in space
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.Random;
import jmeplanet.PlanetAppState;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Nebula extends Celestial {
    //constants

    public static final double DENSITY = (1000.0f / 80000000000000.0f);
    public static final int MIN_PARTICLES = 50;
    public static final int MAX_PARTICLES = 2000;
    //end constants
    Random rnd = new Random();
    private final Term type;
    private ColorRGBA color = ColorRGBA.Black;
    private transient NebulaEmitter emitter;
    Vector3f volume;

    public Nebula(Universe universe, String name, Term type, ColorRGBA color, Vector3f volume) {
        super(Float.POSITIVE_INFINITY, universe);
        this.type = type;
        this.color = color;
        this.volume = volume;
        setName(name);
    }

    public void construct(AssetManager assets) {
        //calculate particle count based on the density constant
        int particles = (int) (DENSITY * (volume.x * volume.y * volume.z));
        if (particles > MAX_PARTICLES) {
            particles = MAX_PARTICLES;
        } else if (particles < MIN_PARTICLES) {
            particles = MIN_PARTICLES;
        }
        //create the emitter
        emitter = new NebulaEmitter("Emitter", ParticleMesh.Type.Triangle, particles);
        mat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assets.loadTexture("Effects/Nebula/" + type.getValue("asset")));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Color);
        emitter.setMaterial(mat);
        //setup emitter
        emitter.setEnabled(true);
        emitter.setImagesX(Integer.parseInt(type.getValue("x")));
        emitter.setImagesY(Integer.parseInt(type.getValue("y")));
        emitter.setEndColor(color);
        emitter.setStartColor(color); // IRRELEVENT
        emitter.setStartSize(32000f);
        emitter.setEndSize(16000f);
        emitter.setGravity(0, 0, 0);
        emitter.setLowLife(Float.MAX_VALUE);
        emitter.setHighLife(Float.MAX_VALUE);
        emitter.getParticleInfluencer().setVelocityVariation(0f);
        emitter.emittParticleVolume();
    }

    public void deconstruct() {
        super.deconstruct();
        if (emitter != null) {
            emitter.setEnabled(false);
            emitter.killAllParticles();
        }
        emitter = null;
    }

    protected void alive() {
        if (emitter != null) {
            emitter.setLocalTranslation(getLocation().x, getLocation().y,
                    getLocation().z);
        }
    }

    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.attachChild(emitter);
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.detachChild(emitter);
    }

    public class NebulaEmitter extends ParticleEmitter {

        public NebulaEmitter(String name, ParticleMesh.Type type, int numParticles) {
            super(name, type, numParticles);
        }

        public void emittParticleVolume() {
            /*
             * Emitts a volume of particles so that there is no "fill in"
             * effect.
             */
            //shove everything out the door
            emitter.setInWorldSpace(false);
            emitter.killAllParticles();
            emitter.setLocalTranslation(getLocation());
            emitAllParticles();
            //update each particle so it has a random location inside the volume
            Particle[] particles = getParticles();
            for (int a = 0; a < particles.length; a++) {
                boolean ok = false;
                //pick location
                int dx = 0;
                int dy = 0;
                int dz = 0;
                while (!ok) {
                    dx = rnd.nextInt((int) volume.x * 2) - (int) volume.x;
                    dy = rnd.nextInt((int) volume.y * 2) - (int) volume.y;
                    dz = rnd.nextInt((int) volume.z * 2) - (int) volume.z;
                    //make sure this is in the cylindrical plane
                    Vector3f testVec = new Vector3f(dx, dy, dz);
                    if (testVec.length() < (volume.x + volume.y + volume.z) / 3) {
                        ok = true;
                    }
                }
                //pick age
                int age = rnd.nextInt((int) particles[a].life);
                //store
                particles[a].position.set(dx, dy, dz);
                particles[a].life = age;
            }
        }
    }
}
