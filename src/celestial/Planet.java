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
 * Defines a planet. Planets in this simulation have infinite mass so they will
 * stay put.
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Node;
import com.jme3.texture.Texture2D;
import java.util.Random;
import jmeplanet.FractalDataSource;
import jmeplanet.PlanetAppState;
import jmeplanet.Utility;
import lib.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Planet extends Celestial {

    private Texture2D tex;
    jmeplanet.Planet fractalPlanet;
    private Term type;
    private int seed = 0;
    protected float radius;

    public Planet(Universe universe, String name, Term type, float radius) {
        super(Float.POSITIVE_INFINITY, universe);
        setName(name);
        this.type = type;
        this.radius = radius;
    }

    public void construct(AssetManager assets) {
        generateProceduralPlanet(assets);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //add physics to mesh
        spatial.addControl(physics);
    }

    public void deconstruct() {
        setTex(null);
        mat = null;
        spatial = null;
        physics = null;
    }

    private void generateProceduralPlanet(AssetManager assets) {
        Random sRand = new Random(seed);
        // Add planet
        FractalDataSource planetDataSource = new FractalDataSource(seed);
        planetDataSource.setHeightScale(0.015f*radius);
        fractalPlanet = Utility.createEarthLikePlanet(assets, radius, null, planetDataSource);
        spatial = fractalPlanet;
    }

    protected void alive() {
        if (physics != null) {
            if (spatial != null) {
                //planets do not move
                physics.setPhysicsLocation(getLocation());
                physics.setPhysicsRotation(getRotation());
                spatial.setLocalRotation(getRotation());
            }
        }
    }

    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.attachChild(spatial);
        physics.getPhysicsSpace().add(spatial);
        if (fractalPlanet != null) {
            planetAppState.addPlanet(fractalPlanet);
        }
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.detachChild(spatial);
        physics.getPhysicsSpace().remove(spatial);
        if (fractalPlanet != null) {
            planetAppState.removePlanet(fractalPlanet);
        }
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Term getType() {
        return type;
    }

    public void setType(Term type) {
        this.type = type;
    }

    public Texture2D getTex() {
        return tex;
    }

    public void setTex(Texture2D tex) {
        this.tex = tex;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }
}
