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
 * Nozzles are special hardpoints that carry particle effects used for engine
 * flame and trails.
 */
package cargo;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 *
 * @author nwiehoff
 */
public class Nozzle extends Hardpoint {

    transient ParticleEmitter emitter;
    private String rawStart;
    private String rawEnd;

    public Nozzle(Ship host, String type, int size, Vector3f loc, String rawStart, String rawEnd) {
        super(host, type, size, loc, Vector3f.UNIT_Z);
        this.rawEnd = rawEnd;
        this.rawStart = rawStart;
    }

    @Override
    public void periodicUpdate(double tpf) {
        if (emitter != null) {
            if (host.getThrottle() != 0 && host.getFuel() > host.getThrust()) {
                if (host.getThrottle() > 0) {
                    if (getType().equals("rear")) {
                        emitter.setParticlesPerSec(100);
                    } else {
                        emitter.setParticlesPerSec(0);
                    }
                } else {
                    if (getType().equals("forward")) {
                        emitter.setParticlesPerSec(100);
                    } else {
                        emitter.setParticlesPerSec(0);
                    }
                }
            } else {
                emitter.setParticlesPerSec(0);
            }
            //emitter.getParticleInfluencer().setInitialVelocity(Vector3f.UNIT_Z.mult(-host.getLinearVelocity().length())/*.mult((float) tpf)*/);
            emitter.getParticleInfluencer().setInitialVelocity(getUp().mult((float) Math.sqrt(host.getAcceleration()) * host.getThrottle()));
        }
    }

    @Override
    public void showDebugHardpoint(AssetManager assets) {
        Box point = new Box(0.1f, 0.1f, 0.1f);
        Geometry red = new Geometry("DebugNozzle", point);
        red.setLocalTranslation(Vector3f.ZERO);
        Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        red.setMaterial(mat);
        //add to node
        getNode().attachChild(red);
    }

    @Override
    public void construct(AssetManager assets) {
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 100);
        Material trailMat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        trailMat.setTexture("Texture", assets.loadTexture("Effects/Trail/point.png"));
        emitter.setMaterial(trailMat);
        emitter.setImagesX(1);
        emitter.setImagesY(1); // 1x1
        emitter.setStartSize((float) getSize() / 2);
        emitter.setEndSize(0);
        emitter.setGravity(0f, 0f, 0f);
        emitter.setLowLife(0.9f);
        emitter.setHighLife(1f);
        emitter.getParticleInfluencer().setVelocityVariation(0.05f);
        emitter.setInWorldSpace(false);
        emitter.setSelectRandomImage(true);
        getNode().attachChild(emitter);
        emitter.setEnabled(true);
        //setup start color
        {
            String[] arr = rawStart.split(",");
            float r = Float.parseFloat(arr[0]);
            float g = Float.parseFloat(arr[1]);
            float b = Float.parseFloat(arr[2]);
            float a = Float.parseFloat(arr[3]);
            ColorRGBA col = new ColorRGBA(r, g, b, a);
            emitter.setStartColor(col);
        }
        //setup end color
        {
            String[] arr = rawEnd.split(",");
            float r = Float.parseFloat(arr[0]);
            float g = Float.parseFloat(arr[1]);
            float b = Float.parseFloat(arr[2]);
            float a = Float.parseFloat(arr[3]);
            ColorRGBA col = new ColorRGBA(r, g, b, a);
            emitter.setEndColor(col);
        }
    }

    @Override
    public void deconstruct() {
        emitter = null;
        setNode(null);
        setUpNode(null);
    }
}
