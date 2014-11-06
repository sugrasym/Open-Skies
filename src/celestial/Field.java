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
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import entity.Entity;
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
    private transient Block[] patterns;
    private transient ArrayList<Block> zones;
    private int step = 0;
    //node for attatching blocks
    private transient Node node;
    private transient BulletAppState bulletAppState;
    //mining
    private boolean mineable = false;
    private String resource;

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
        
        String mineableRaw = field.getValue("mineable");
        if(mineableRaw != null) {
            mineable = Boolean.parseBoolean(mineableRaw);
            if(mineable) {
                resource = field.getValue("resource");
            }
        }
        
        type = field;
    }

    public void construct(AssetManager assets) {
        /*
         * Make asteroid dummies
         */
        zones = new ArrayList<>();
        nameControl.setParent(this);
        generatePatterns();
        generateAsteroids(assets);
    }

    public void deconstruct() {
        super.deconstruct();
        if (patterns != null) {
            for (int a = 0; a < patterns.length; a++) {
                patterns[a].deconstructBlock();
            }
        }
        patterns = null;
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
            Random rnd = new Random(seed + x);
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

    private boolean noExclusionZone() {
        for (int a = 0; a < universe.getPlayerShip().getCurrentSystem().getCelestials().size(); a++) {
            Entity test = universe.getPlayerShip().getCurrentSystem().getCelestials().get(a);
            if (test instanceof Planet) {
                Planet tmp = (Planet) test;
                double dist = tmp.getLocation().distance(universe.getPlayerShip().getPhysicsLocation());
                if (Math.max(dist - tmp.getRadius(), 0) < blockSize) {
                    //we are in an exclusion zone!
                    return false;
                }
            }
        }
        return true;
    }

    protected void alive() {
        Ship host = universe.getPlayerShip();
        try {
            if (noExclusionZone()) {
                if (pointInsideField(host.getPhysicsLocation())) {
                    //calculate distance to block
                    boolean inABlock = false;
                    for (int a = 0; a < zones.size(); a++) {
                        Block localBlock = zones.get(a);
                        float dist = localBlock.getLocation().distance(host.getPhysicsLocation());
                        if (dist < getBlockSize() * 0.5) {
                            inABlock = true;
                        } else if (dist >= getBlockSize()) {
                            localBlock.remove(node);
                            zones.remove(localBlock);
                        }
                    }
                    if (inABlock == false) {
                        Block tmp = new Block(patterns[step]);
                        tmp.setLocation(host.getPhysicsLocation());
                        zones.add(tmp);
                        tmp.add(node);
                        //increment through cycle
                        step++;
                        step %= diversity;
                    }
                } else {
                    //out of field
                }
            } else {
                //remove asteroids, we are in an exclusion zone
                for (int a = 0; a < zones.size(); a++) {
                    Block localBlock = zones.get(a);
                    localBlock.remove(node);
                    zones.remove(localBlock);
                }
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

    public boolean isMineable() {
        return mineable;
    }

    public void setMineable(boolean mineable) {
        this.mineable = mineable;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    private class Block {

        private Vector3f[] map;
        private Vector3f[] rot;
        private Spatial[] roids;
        //private Node block;
        //location
        private Vector3f location;

        public Block(Vector3f[] map, Vector3f[] rot) {
            this.map = map;
            this.rot = rot;
            roids = new Spatial[rot.length];
        }

        public Block(Block toClone) {
            this.map = toClone.getMap().clone();
            this.rot = toClone.getRot().clone();
            this.roids = toClone.getRoids().clone();
            //this.block = toClone.getBlock().clone(true);
        }

        public void constructBlock() {
            //block = new Node();
            for (int a = 0; a < map.length; a++) {
                roids[a] = asteroid.clone();
                roids[a].setLocalTranslation(map[a].x, map[a].y, map[a].z);
                roids[a].rotate(rot[a].x, rot[a].y, rot[a].z);
                roids[a].scale(getRockScale());
                CollisionShape hullShape = CollisionShapeFactory.createDynamicMeshShape(roids[a]);
                RigidBodyControl box = new RigidBodyControl(hullShape);
                //box.setMass(0);
                //box.setKinematic(false);
                roids[a].addControl(box);
                roids[a].addControl(nameControl);
                System.out.println("Working - " + ((float) a / (float) map.length) * 100.0f);
            }
        }

        public void deconstructBlock() {
            //block = null;
            roids = null;
        }

        public Spatial[] getRoids() {
            return roids;
        }

        public void setRoids(Spatial[] roids) {
            this.roids = roids;
        }

        public Vector3f[] getMap() {
            return map;
        }

        public void setMap(Vector3f[] map) {
            this.map = map;
        }

        public Vector3f[] getRot() {
            return rot;
        }

        public void setRot(Vector3f[] rot) {
            this.rot = rot;
        }

        public Vector3f getLocation() {
            return location;
        }

        public void setLocation(Vector3f location) {
            this.location = location;
        }

        private void remove(Node node) {
            for (int a = 0; a < roids.length; a++) {
                node.detachChild(roids[a]);
                bulletAppState.getPhysicsSpace().remove(roids[a]);
            }
        }

        private void add(Node node) {
            for (int a = 0; a < roids.length; a++) {
                node.attachChild(roids[a]);
                roids[a].getControl(RigidBodyControl.class).setPhysicsLocation(location.add(map[a]));
                roids[a].getControl(RigidBodyControl.class).setLinearVelocity(Vector3f.ZERO);
                roids[a].getControl(RigidBodyControl.class).setAngularVelocity(Vector3f.ZERO);
                roids[a].getControl(RigidBodyControl.class).clearForces();
                bulletAppState.getPhysicsSpace().add(roids[a]);
            }
        }
    }

    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        this.node = node;
        this.bulletAppState = physics;
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
    }
}
