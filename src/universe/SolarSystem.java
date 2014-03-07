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
 * Solar systems are a collection of planets and other celestials in a convenient
 * package. It provides zoning for the universe.
 */
package universe;

import celestial.Field;
import celestial.Planet;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import celestial.Star;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import entity.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import jmeplanet.PlanetAppState;
import lib.astral.Parser;
import lib.astral.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class SolarSystem implements Entity, Serializable {
    //this system

    transient Spatial skybox;
    protected String name;
    float x;
    float y;
    float z;
    //what it contains
    private ArrayList<Entity> celestials = new ArrayList<>();
    //what contains it
    private Universe universe;
    //engine resources
    private Parser info;
    private Term thisSystem;

    public SolarSystem(Universe universe, Term thisSystem, Parser parse) {
        name = thisSystem.getValue("name");
        this.universe = universe;
        this.info = parse;
        this.thisSystem = thisSystem;
    }

    public final void initSystem() {
        /*
         * Adds all member objects
         //nebula
         /*ArrayList<Term> nebula = info.getTermsOfType("Nebula");
         for (int a = 0; a < nebula.size(); a++) {
         if (nebula.get(a).getValue("system").matches(getName())) {
         //this star needs to be created and stored
         getCelestials().add(makeNebula(nebula.get(a)));
         }
         }*/
        //star
        ArrayList<Term> stars = info.getTermsOfType("Star");
        for (int a = 0; a < stars.size(); a++) {
            if (stars.get(a).getValue("system").matches(getName())) {
                //this star needs to be created and stored
                getCelestials().add(makeStar(stars.get(a)));
            }
        }
        //field
        ArrayList<Term> fields = info.getTermsOfType("Field");
        for (int a = 0; a < fields.size(); a++) {
            if (fields.get(a).getValue("system").matches(getName())) {
                //this field needs to be created and stored
                getCelestials().add(makeField(fields.get(a)));
            }
        }
        //planet
        ArrayList<Term> planets = info.getTermsOfType("Planet");
        for (int a = 0; a < planets.size(); a++) {
            if (planets.get(a).getValue("system").matches(getName())) {
                //this planet needs to be created and stored
                getCelestials().add(makePlanet(planets.get(a)));
            }
        }
        //station
        ArrayList<Term> stations = info.getTermsOfType("Station");
        for (int a = 0; a < stations.size(); a++) {
            if (stations.get(a).getValue("system").matches(getName())) {
                //this ship needs to be created and stored
                getCelestials().add(makeStation(stations.get(a)));
            }
        }
        //ship
        ArrayList<Term> ships = info.getTermsOfType("Ship");
        for (int a = 0; a < ships.size(); a++) {
            if (ships.get(a).getValue("system").matches(getName())) {
                //this ship needs to be created and stored
                getCelestials().add(makeShip(ships.get(a)));
            }
        }
    }

    private Station makeStation(Term shipTerm) {
        Station station = null;
        {
            String type = shipTerm.getValue("station");
            Parser tmp = new Parser("STATION.txt");
            ArrayList<Term> list = tmp.getTermsOfType("Station");
            Term hull = null;
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("type").matches(type)) {
                    hull = list.get(a);
                    break;
                }
            }
            //extract terms
            String sName = shipTerm.getValue("name");
            float sx = Float.parseFloat(shipTerm.getValue("x"));
            float sy = Float.parseFloat(shipTerm.getValue("y"));
            float sz = Float.parseFloat(shipTerm.getValue("z"));
            //create ship
            station = new Station(universe, hull);
            //position ship
            station.setLocation(new Vector3f(sx, sy, sz));
            station.setCurrentSystem(this);
            station.setName(sName);
        }
        return station;
    }

    private Ship makeShip(Term shipTerm) {
        Ship ship = null;
        {
            String type = shipTerm.getValue("ship");
            Parser tmp = new Parser("SHIP.txt");
            ArrayList<Term> list = tmp.getTermsOfType("Ship");
            Term hull = null;
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("type").matches(type)) {
                    hull = list.get(a);
                    break;
                }
            }
            //extract terms
            String sName = shipTerm.getValue("name");
            float sx = Float.parseFloat(shipTerm.getValue("x"));
            float sy = Float.parseFloat(shipTerm.getValue("y"));
            float sz = Float.parseFloat(shipTerm.getValue("z"));
            //create ship
            ship = new Ship(universe, hull);
            //position ship
            ship.setLocation(new Vector3f(sx, sy, sz));
            ship.setCurrentSystem(this);
            ship.setName(sName);
        }
        return ship;
    }

    private Planet makePlanet(Term planetTerm) {
        Planet planet = null;
        {
            String texture = planetTerm.getValue("texture");
            //find logical texture
            Parser tmp = new Parser("PLANET.txt");
            Term tex = null;
            ArrayList<Term> list = tmp.getTermsOfType("Planet");
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("name").matches(texture)) {
                    tex = list.get(a);
                    break;
                }
            }
            //extract terms
            String pName = planetTerm.getValue("name");
            float radius = Integer.parseInt(planetTerm.getValue("r"));
            float px = Float.parseFloat(planetTerm.getValue("x"));
            float py = Float.parseFloat(planetTerm.getValue("y"));
            float pz = Float.parseFloat(planetTerm.getValue("z"));
            int seed = Integer.parseInt(planetTerm.getValue("seed"));
            //make planet and store
            planet = new Planet(universe, pName, tex, radius);
            planet.setSeed(seed);
            planet.setLocation(new Vector3f(px, py, pz));
        }
        return planet;
    }

    private Star makeStar(Term starTerm) {
        Star star = null;
        {
            String texture = starTerm.getValue("texture");
            //find the logical texture
            Parser tmp = new Parser("PLANET.txt");
            Term tex = null;
            ArrayList<Term> list = tmp.getTermsOfType("Star");
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("name").matches(texture)) {
                    tex = list.get(a);
                    break;
                }
            }
            //extract terms
            String pName = starTerm.getValue("name");
            float radius = Integer.parseInt(starTerm.getValue("r"));
            float px = Float.parseFloat(starTerm.getValue("x"));
            float py = Float.parseFloat(starTerm.getValue("y"));
            float pz = Float.parseFloat(starTerm.getValue("z"));
            //seed
            int seed = Integer.parseInt(starTerm.getValue("seed"));
            //make planet and store
            star = new Star(universe, pName, tex, radius);
            star.setSeed(seed);
            star.setLocation(new Vector3f(px, py, pz));
        }
        return star;
    }

    private Field makeField(Term fieldTerm) {
        Field field = null;
        {
            //extract terms
            String pName = fieldTerm.getValue("name");
            String texture = fieldTerm.getValue("type");
            //find logical texture
            Parser tmp = new Parser("FIELD.txt");
            Term tex = null;
            ArrayList<Term> list = tmp.getTermsOfType("Field");
            for (int a = 0; a < list.size(); a++) {
                if (list.get(a).getValue("name").matches(texture)) {
                    tex = list.get(a);
                    break;
                }
            }
            //position
            float px = Float.parseFloat(fieldTerm.getValue("x"));
            float py = Float.parseFloat(fieldTerm.getValue("y"));
            float pz = Float.parseFloat(fieldTerm.getValue("z"));
            //dimension
            float l = Float.parseFloat(fieldTerm.getValue("l"));
            float w = Float.parseFloat(fieldTerm.getValue("w"));
            float h = Float.parseFloat(fieldTerm.getValue("h"));
            //seed
            int seed = Integer.parseInt(fieldTerm.getValue("seed"));
            //calculate params
            Vector3f location = new Vector3f(px,py,pz);
            Vector3f bounds = new Vector3f(l,h,w);
            //create field
            field = new Field(universe, tex, seed, location, bounds);
            field.setName(pName);
        }
        return field;
    }

    /*private Nebula makeNebula(Term nebulaTerm) {
     Nebula nebula = null;
     {
     //extract terms
     String pName = nebulaTerm.getValue("name");
     String texture = nebulaTerm.getValue("type");
     //position
     float px = Float.parseFloat(nebulaTerm.getValue("x"));
     float py = Float.parseFloat(nebulaTerm.getValue("y"));
     float pz = Float.parseFloat(nebulaTerm.getValue("z"));
     //dimension
     float l = Float.parseFloat(nebulaTerm.getValue("l"));
     float w = Float.parseFloat(nebulaTerm.getValue("w"));
     float h = Float.parseFloat(nebulaTerm.getValue("h"));
     //color
     String col = nebulaTerm.getValue("color");
     String[] colArr = col.split(",");
     float r = Float.parseFloat(colArr[0]);
     float g = Float.parseFloat(colArr[1]);
     float b = Float.parseFloat(colArr[2]);
     float a = Float.parseFloat(colArr[3]);
     //seed
     int seed = Integer.parseInt(nebulaTerm.getValue("seed"));
     //texture
     Parser tmp = new Parser("PARTICLE.txt");
     ArrayList<Term> terms = tmp.getTermsOfType("Nebula");
     Term fin = null;
     for (int o = 0; o < terms.size(); o++) {
     if (terms.get(o).getValue("name").matches(texture)) {
     fin = terms.get(o);
     break;
     }
     }
     //make planet and store
     nebula = new Nebula(universe,pName, fin, new ColorRGBA(r, g, b, a), new Vector3f(l, w, h));
     nebula.setSeed(seed);
     nebula.setLocation(new Vector3f(px, py, pz));
     }
     return nebula;
     }*/
    public ArrayList<Entity> getCelestials() {
        return celestials;
    }

    public Entity getCelestialWithPhysicsName(String physicsName) {
        Entity entity = null;
        {
            for (int a = 0; a < celestials.size(); a++) {
            }
        }
        return entity;
    }

    public void setCelestials(ArrayList<Entity> celestials) {
        this.celestials = celestials;
    }

    public void putEntityInSystem(Entity entity) {
        celestials.add(entity);
    }

    public void pullEntityFromSystem(Entity entity) {
        celestials.remove(entity);
    }

    @Override
    public void periodicUpdate(float tpf) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).periodicUpdate(tpf);
            if (celestials.get(a).getState() == Entity.State.DEAD) {
                //remove the entity
                celestials.remove(a);
            }
        }
    }

    @Override
    public State getState() {
        return State.ALIVE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void construct(AssetManager assets) {
        //construct children
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).construct(assets);
        }
        //construct skybox
        Parser sky = new Parser("SKY.txt");
        ArrayList<Term> boxes = sky.getTermsOfType("Skybox");
        for (int a = 0; a < boxes.size(); a++) {
            if (boxes.get(a).getValue("name").matches(thisSystem.getValue("sky"))) {
                skybox = SkyFactory.createSky(assets, "Textures/Skybox/" + boxes.get(a).getValue("asset"), false);
                skybox.setQueueBucket(Bucket.Sky);
                skybox.setShadowMode(RenderQueue.ShadowMode.Off);
                break;
            }
        }
    }

    @Override
    public void deconstruct() {
        skybox = null;
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).deconstruct();
        }
    }

    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).attach(node, physics, planetAppState);
        }
        node.attachChild(skybox);
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).detach(node, physics, planetAppState);
        }
        node.detachChild(skybox);
    }

    @Override
    public void setState(State state) {
        //do nothing
    }

    @Override
    public Vector3f getLocation() {
        return new Vector3f(x, y, z);
    }

    @Override
    public void setLocation(Vector3f loc) {
        x = loc.x;
        y = loc.y;
        z = loc.z;
    }

    @Override
    public Quaternion getRotation() {
        return Quaternion.ZERO;
    }

    @Override
    public void setRotation(Quaternion rot) {
        //do nothing
    }

    @Override
    public Vector3f getPhysicsLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
