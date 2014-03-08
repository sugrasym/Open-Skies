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
 * Impossibly dense clouds of colorful stuff in space
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
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
    public static final int MIN_PARTICLES = 5;
    public static final int MAX_PARTICLES = 1000;
    //end constants
    Random rnd = new Random();
    private Term type;
    private ColorRGBA color = ColorRGBA.Black;
    NebulaEmitter emitter;
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
        emitter.setMaterial(mat);
        //setup emitter
        emitter.setEnabled(true);
        emitter.setImagesX(Integer.parseInt(type.getValue("x")));
        emitter.setImagesY(Integer.parseInt(type.getValue("y")));
        emitter.setEndColor(color);
        emitter.setStartColor(color); // IRRELEVENT
        emitter.setStartSize(20000f);
        emitter.setEndSize(19000f);
        emitter.setGravity(0, 0, 0);
        emitter.setLowLife(Float.MAX_VALUE);
        emitter.setHighLife(Float.MAX_VALUE);
        emitter.getParticleInfluencer().setVelocityVariation(0.3f);
        emitter.emittParticleVolume();
    }

    public void deconstruct() {
        super.deconstruct();
        emitter.setEnabled(false);
        emitter.killAllParticles();
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
