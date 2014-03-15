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
 * Impossibly dense clouds of rocks in space
 */

package celestial;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import jmeplanet.PlanetAppState;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Field extends Celestial implements Serializable {
    //location of the field
    private Universe universe;
    private Vector3f location = new Vector3f(0, 0, 0);
    //seed to generate a block
    private int seed;
    private Term type;
    //total volume of the field as an ellipsoid
    private Vector3f size;
    //materials and spatials
    private transient Spatial asteroid;
    private transient Material asteroidMat;
    //density and zoning
    private int count;
    private int blockSize;
    private int rockScale = 1;
    private int diversity = 1;
    //stores the position and rotation of each member of the block
    private Block[] patterns;
    private ArrayList<Node> zones = new ArrayList<>();
    //node for attatching blocks
    private Node node;

    public Field(Universe universe, String name, Term field, int seed, Vector3f location, Vector3f bounds) {
        super(Float.POSITIVE_INFINITY, universe);
        this.universe = universe;
        this.seed = seed;
        size = bounds.clone();
        this.location = bounds.clone();
        setName(name);
        //extract terms
        blockSize = Integer.parseInt(field.getValue("blockSize"));
        rockScale = Integer.parseInt(field.getValue("rockScale"));
        diversity = Integer.parseInt(field.getValue("diversity"));
        count = Integer.parseInt(field.getValue("count"));
        type = field;
        generatePatterns();
    }

    public void construct(AssetManager assets) {
        /*
         * Setup the cylinder and ~physics~
         */
        String ast = type.getValue("cylinder");
        spatial = assets.loadModel("Models/" + ast + "/Model.blend");
        mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap",
                assets.loadTexture("Models/" + ast + "/tex.png"));
        spatial.setMaterial(mat);
        spatial.setShadowMode(RenderQueue.ShadowMode.Off);
        //mat.getAdditionalRenderState().setAlphaTest(true);
        //mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        //mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //spatial.setQueueBucket(RenderQueue.Bucket.Transparent);
        spatial.setMaterial(mat);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(0.0f);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        physics.setPhysicsLocation(location);
        //add physics to mesh
        spatial.addControl(physics);
        //store physics name control
        nameControl.setParent(this);
        spatial.addControl(nameControl);
        /*
         * Make asteroid dummies
         */
        generateAsteroids(assets);
    }

    public void deconstruct() {
        super.deconstruct();
        for (int a = 0; a < patterns.length; a++) {
            patterns[a].deconstructBlock();
        }
        asteroid = null;
        asteroidMat = null;
    }

    private void generateAsteroids(AssetManager assets) {
        /*
         * Asteroid
         */
        String ast = type.getValue("asset");
        //load model
        asteroid = assets.loadModel("Models/" + ast + "/Model.blend");
        //load texture
        asteroidMat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        asteroidMat.setTexture("DiffuseMap",
                assets.loadTexture("Models/" + ast + "/tex.png"));
        //setup texture
        asteroid.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        asteroid.setMaterial(asteroidMat);
        /*
         * Now make all the patterns
         */
        for (int a = 0; a < patterns.length; a++) {
            patterns[a].constructBlock();
        }
    }

    private void generatePatterns() {
        setBlocks(new Block[diversity]);
        for (int x = 0; x < diversity; x++) {
            Random rnd = new Random(seed);
            Vector3f[] map = new Vector3f[count];
            Vector3f[] rot = new Vector3f[count];
            for (int a = 0; a < count; a++) {
                float dx = (rnd.nextFloat() * getBlockSize() * 2.0f) - getBlockSize();
                float dy = (rnd.nextFloat() * getBlockSize() * 2.0f) - getBlockSize();
                float dz = (rnd.nextFloat() * getBlockSize() * 2.0f) - getBlockSize();
                map[a] = new Vector3f(dx, dy, dz);
                //rotate
                float rx = rnd.nextFloat() * FastMath.TWO_PI;
                float ry = rnd.nextFloat() * FastMath.TWO_PI;
                float rz = rnd.nextFloat() * FastMath.TWO_PI;
                rot[a] = new Vector3f(rx, ry, rz);
            }
            getBlocks()[x] = new Block(map, rot);
        }
    }

    public boolean pointInsideField(Vector3f point) {
        //adjust
        Vector3f adj = point.subtract(location);
        return pointInsideEllipse(adj.x, adj.y, adj.z, size.x, size.z, size.y);
    }

    private boolean pointInsideEllipse(float x, float y, float z, float l, float w, float h) {
        float a = (x * x) / (l * l);
        float b = (z * z) / (w * w);
        float c = (y * y) / (h * h);
        float sum = a + b + c;
        if (sum <= 1) {
            return true;
        } else {
            return false;
        }
    }

    protected void alive() {
        Ship host = universe.getPlayerShip();
        try {
            if (pointInsideField(host.getPhysicsLocation())) {
                //calculate distance to block
                boolean inABlock = false;
                for (int a = 0; a < zones.size(); a++) {
                    Node localBlock = zones.get(a);
                    float dist = localBlock.getLocalTranslation().distance(host.getPhysicsLocation());
                    if (dist < getBlockSize() * 0.5) {
                        inABlock = true;
                    } else if (dist > getBlockSize() * 1.0) {
                        node.detachChild(localBlock);
                        zones.remove(localBlock);
                    }
                }
                if (inABlock == false) {
                    Node tmp = (Node) patterns[0].getBlock().clone();
                    tmp.setLocalTranslation(host.getPhysicsLocation());
                    zones.add(tmp);
                    node.attachChild(tmp);
                }
            } else {
                //out of field
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public Vector3f getSize() {
        return size;
    }

    public void setSize(Vector3f size) {
        this.size = size;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Vector3f getLocation() {
        return location;
    }

    public void setLocation(Vector3f location) {
        this.location = location;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getRockScale() {
        return rockScale;
    }

    public void setRockScale(int rockScale) {
        this.rockScale = rockScale;
    }

    public Block[] getBlocks() {
        return patterns;
    }

    public void setBlocks(Block[] blocks) {
        this.patterns = blocks;
    }

    private class Block {

        private Vector3f[] map;
        private Vector3f[] rot;
        private Spatial[] roids;
        private Node block;

        public Block(Vector3f[] map, Vector3f[] rot) {
            this.map = map;
            this.rot = rot;
            roids = new Spatial[rot.length];
        }

        public void constructBlock() {
            block = new Node();
            for (int a = 0; a < map.length; a++) {
                addRoid(map[a], rot[a], roids[a]);
                System.out.println("Working - " + ((float) a / (float) map.length) * 100.0f);
            }
        }

        private void addRoid(Vector3f loc, Vector3f rot, Spatial roid) {
            roid = asteroid.clone();
            roid.setLocalTranslation(loc.x, loc.y, loc.z);
            roid.rotate(rot.x, rot.y, rot.z);
            roid.scale(getRockScale());
            block.attachChild(roid);
        }

        public void deconstructBlock() {
            block = null;
            roids = null;
        }

        public Node getBlock() {
            return block;
        }

        public void setBlock(Node block) {
            this.block = block;
        }
    }
    
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        super.attach(node, physics, planetAppState);
        this.node = node;
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        super.detach(node, physics, planetAppState);
    }
}
