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

    public Nozzle(Ship host, String type, int size, Vector3f loc) {
        super(host, type, size, loc);
    }

    @Override
    public void periodicUpdate(double tpf) {
        if (emitter != null) {
            emitter.getParticleInfluencer().setInitialVelocity(host.getLinearVelocity());
        }
    }

    public void start() {
        
    }

    public void stop() {
        
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
        node.attachChild(red);
    }

    @Override
    public void construct(AssetManager assets) {
        emitter = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 100);
        Material trailMat = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        trailMat.setTexture("Texture", assets.loadTexture("Effects/Trail/flame.png"));
        emitter.setMaterial(trailMat);
        emitter.setImagesX(2);
        emitter.setImagesY(2); // 2x2
        emitter.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        emitter.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        emitter.setStartSize(size);
        emitter.setEndSize(0f);
        emitter.setGravity(0f, 0f, 0f);
        emitter.setLowLife(0.5f);
        emitter.setHighLife(1.5f);
        emitter.getParticleInfluencer().setVelocityVariation(0.01f);
        emitter.setInWorldSpace(true);
        node.attachChild(emitter);
        emitter.setEnabled(true);
    }

    public void deconstruct() {
        emitter = null;
        node = null;
    }
}
