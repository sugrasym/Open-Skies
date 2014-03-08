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
 * The main area for game logic.
 */
package engine;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import entity.Entity;
import jmeplanet.PlanetAppState;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author Nathan Wiehoff
 */
public class Core {

    public enum GameState {

        PAUSED,
        IN_SPACE,
    }
    private GameState state = GameState.PAUSED;
    //game objects
    /*
     * Although you can chuck any game object into this list, it is best to maintain the hierchy.
     * - Universe
     * -- Solar System
     * ---- Celestial
     * ----- Celestial children (asteroids?)
     * ---- Ship / Station
     */
    Universe universe;
    //nodes
    Node rootNode;
    Node guiNode;
    //engine resources
    BulletAppState bulletAppState;
    PlanetAppState planetAppState;
    AssetManager assets;

    public Core(Node rootNode, Node guiNode, BulletAppState bulletAppState, AssetManager assets, PlanetAppState planetAppState) {
        this.rootNode = rootNode;
        this.guiNode = guiNode;
        this.bulletAppState = bulletAppState;
        this.assets = assets;
        this.planetAppState = planetAppState;
        //initialize
        init();
        test();
    }

    private void init() {
        //TODO: init code
    }

    private void test() {
        /*
         * Used for testing
         */
        //ambient white light
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(ambient);
        /*
         * Testing Code
         */
        universe = new Universe(assets);
        addSystem(universe.getSystems().get(0));
    }

    /*
     * Facilities for adding and removing game entities seamlessly FROM THE
     * SCENE, NOT FROM THE UNIVERSE
     */
    public final void addSystem(SolarSystem system) {
        addEntity(system);
    }

    public final void removeSystem(SolarSystem system) {
        removeEntity(system);
    }

    public final void addEntity(Entity entity) {
        entity.construct(assets);
        entity.attach(rootNode, bulletAppState, planetAppState);
    }

    public final void removeEntity(Entity entity) {
        entity.detach(rootNode, bulletAppState, planetAppState);
        entity.deconstruct();
    }

    /*
     * Moves an entity between solar systems.
     */
    public final void moveEntity(Entity entity, SolarSystem in, SolarSystem out) {
        in.pullEntityFromSystem(entity);
        out.putEntityInSystem(entity);
    }

    /*
     * Taking over some important jobs from the Main class.
     */
    public void periodicUpdate(float tpf) {
        for (int a = 0; a < universe.getSystems().size(); a++) {
            universe.getSystems().get(a).periodicUpdate(tpf);
        }
    }

    public void render(RenderManager rm) {
        //TODO
    }
}
