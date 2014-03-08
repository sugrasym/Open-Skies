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

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import entity.Entity;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmeplanet.PlanetAppState;
import lib.astral.AstralIO;
import lib.astral.AstralIO.Everything;
import lib.astral.Parser;
import lib.astral.Parser.Term;
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
    //hud
    HUD hud;
    //engine resources
    BulletAppState bulletAppState;
    PlanetAppState planetAppState;
    AppSettings settings;
    AssetManager assets;
    InputManager input;

    public Core(Node rootNode, Node guiNode, BulletAppState bulletAppState, AssetManager assets, PlanetAppState planetAppState, InputManager input, AppSettings settings) {
        this.rootNode = rootNode;
        this.guiNode = guiNode;
        this.bulletAppState = bulletAppState;
        this.assets = assets;
        this.planetAppState = planetAppState;
        this.input = input;
        this.settings = settings;
        //initialize
        init();
    }

    private void init() {
        initKeys();
        initMouse();
        newGame("Default");
        //do last
        initPhysicsListeners();
        initHud();
    }

    private void initHud() {
        hud = new HUD(guiNode, universe, settings.getWidth(),
                settings.getHeight(), assets);
        hud.add();
    }

    private void initPhysicsListeners() {
        CollisionListener listener = new CollisionListener();
        bulletAppState.getPhysicsSpace().addCollisionListener(listener);
    }

    private void newGame(String name) {
        //get the game from the universe
        Parser parse = new Parser("UNIVERSE.txt");
        ArrayList<Term> games = parse.getTermsOfType("NewGame");
        //find the one we want
        Term game = null;
        for (int a = 0; a < games.size(); a++) {
            if (games.get(a).getValue("name").equals(name)) {
                game = games.get(a);
            }
        }

        //generate the world
        universe = new Universe(assets);
        //determine start system
        String sysName = game.getValue("system");
        SolarSystem start = universe.getSystemWithName(sysName);
        //determine start ship
        Parser ships = new Parser("SHIP.txt");
        String shipName = game.getValue("ship");
        Ship ship = null;
        ArrayList<Term> types = ships.getTermsOfType("Ship");
        for (int a = 0; a < types.size(); a++) {
            if (types.get(a).getValue("type").equals(shipName)) {
                ship = new Ship(universe, types.get(a));
                break;
            }
        }
        //put ship in start location
        float x = Float.parseFloat(game.getValue("x"));
        float y = Float.parseFloat(game.getValue("y"));
        float z = Float.parseFloat(game.getValue("z"));
        ship.setLocation(new Vector3f(x, y, z));
        ship.setCurrentSystem(start);
        start.putEntityInSystem(ship);
        //setup start system
        addSystem(start);

        //center planetappstate on player ship
        planetAppState.setCameraShip(ship);
        //setup player shortcut
        universe.setPlayerShip(ship);

        //start game
        state = GameState.IN_SPACE;
        //TODO: init code

    }
    
    private void initMouse() {
        //mouse buttons
        input.addMapping("MOUSE_LClick", new MouseButtonTrigger(0));
        input.addMapping("MOUSE_RClick", new MouseButtonTrigger(1));
        input.addMapping("MOUSE_CClick", new MouseButtonTrigger(2));
        //store
        input.addListener(actionListener, new String[]{"MOUSE_LClick",
                    "MOUSE_RClick",
                    "MOUSE_CClick"});
    }

    private void initKeys() {
        //Number keys
        input.addMapping("KEY_0", new KeyTrigger(KeyInput.KEY_0));
        input.addMapping("KEY_1", new KeyTrigger(KeyInput.KEY_1));
        input.addMapping("KEY_2", new KeyTrigger(KeyInput.KEY_2));
        input.addMapping("KEY_3", new KeyTrigger(KeyInput.KEY_3));
        input.addMapping("KEY_4", new KeyTrigger(KeyInput.KEY_4));
        input.addMapping("KEY_5", new KeyTrigger(KeyInput.KEY_5));
        input.addMapping("KEY_6", new KeyTrigger(KeyInput.KEY_6));
        input.addMapping("KEY_7", new KeyTrigger(KeyInput.KEY_7));
        input.addMapping("KEY_8", new KeyTrigger(KeyInput.KEY_8));
        input.addMapping("KEY_9", new KeyTrigger(KeyInput.KEY_9));
        //Letter keys except WASD and EQ
        input.addMapping("KEY_B", new KeyTrigger(KeyInput.KEY_B));
        input.addMapping("KEY_C", new KeyTrigger(KeyInput.KEY_C));
        input.addMapping("KEY_F", new KeyTrigger(KeyInput.KEY_F));
        input.addMapping("KEY_G", new KeyTrigger(KeyInput.KEY_G));
        input.addMapping("KEY_H", new KeyTrigger(KeyInput.KEY_H));
        input.addMapping("KEY_I", new KeyTrigger(KeyInput.KEY_I));
        input.addMapping("KEY_J", new KeyTrigger(KeyInput.KEY_J));
        input.addMapping("KEY_K", new KeyTrigger(KeyInput.KEY_K));
        input.addMapping("KEY_L", new KeyTrigger(KeyInput.KEY_L));
        input.addMapping("KEY_M", new KeyTrigger(KeyInput.KEY_M));
        input.addMapping("KEY_N", new KeyTrigger(KeyInput.KEY_N));
        input.addMapping("KEY_O", new KeyTrigger(KeyInput.KEY_O));
        input.addMapping("KEY_P", new KeyTrigger(KeyInput.KEY_P));
        input.addMapping("KEY_R", new KeyTrigger(KeyInput.KEY_R));
        input.addMapping("KEY_T", new KeyTrigger(KeyInput.KEY_T));
        input.addMapping("KEY_U", new KeyTrigger(KeyInput.KEY_U));
        input.addMapping("KEY_V", new KeyTrigger(KeyInput.KEY_V));
        input.addMapping("KEY_X", new KeyTrigger(KeyInput.KEY_X));
        input.addMapping("KEY_Y", new KeyTrigger(KeyInput.KEY_Y));
        input.addMapping("KEY_Z", new KeyTrigger(KeyInput.KEY_Z));
        //space bar
        input.addMapping("KEY_SPACE", new KeyTrigger(KeyInput.KEY_SPACE));
        //return and backspace
        input.addMapping("KEY_RETURN", new KeyTrigger(KeyInput.KEY_RETURN));
        input.addMapping("KEY_BACKSPACE", new KeyTrigger(KeyInput.KEY_BACK));
        //WASD keys
        input.addMapping("KEY_W", new KeyTrigger(KeyInput.KEY_W));
        input.addMapping("KEY_A", new KeyTrigger(KeyInput.KEY_A));
        input.addMapping("KEY_S", new KeyTrigger(KeyInput.KEY_S));
        input.addMapping("KEY_D", new KeyTrigger(KeyInput.KEY_D));
        //QE keys for rolling
        input.addMapping("KEY_Q", new KeyTrigger(KeyInput.KEY_Q));
        input.addMapping("KEY_E", new KeyTrigger(KeyInput.KEY_E));
        //arrow keys
        input.addMapping("KEY_UP", new KeyTrigger(KeyInput.KEY_UP));
        input.addMapping("KEY_DOWN", new KeyTrigger(KeyInput.KEY_DOWN));
        //function keys dedicated to engine control
        input.addMapping("Normal", new KeyTrigger(KeyInput.KEY_F1));
        input.addMapping("Cruise", new KeyTrigger(KeyInput.KEY_F2));
        input.addMapping("Newton", new KeyTrigger(KeyInput.KEY_F3));
        //quick load and quick save dedicated keys
        input.addMapping("QuickSave", new KeyTrigger(KeyInput.KEY_INSERT));
        input.addMapping("QuickLoad", new KeyTrigger(KeyInput.KEY_PAUSE));
        //end key
        input.addMapping("KEY_END", new KeyTrigger(KeyInput.KEY_END));
        //page keys
        input.addMapping("KEY_PGUP", new KeyTrigger(KeyInput.KEY_PGUP));
        input.addMapping("KEY_PGDN", new KeyTrigger(KeyInput.KEY_PGDN));
        //add
        input.addListener(actionListener, new String[]{
            "KEY_0", "KEY_1", "KEY_2", "KEY_3", "KEY_4", "KEY_5", "KEY_6",
            "KEY_7", "KEY_8", "KEY_9",
            "KEY_B", "KEY_C", "KEY_F", "KEY_G", "KEY_H", "KEY_I", "KEY_J",
            "KEY_K", "KEY_L", "KEY_M", "KEY_N", "KEY_O", "KEY_P", "KEY_R",
            "KEY_T", "KEY_U", "KEY_V", "KEY_X", "KEY_Y", "KEY_Z",
            "KEY_W", "KEY_A", "KEY_S", "KEY_D", "KEY_SPACE", "KEY_RETURN",
            "KEY_Q", "KEY_E", "KEY_UP", "KEY_DOWN", "KEY_BACKSPACE",
            "Normal", "Cruise",
            "Newton", "QuickSave", "QuickLoad",
            "KEY_END", "KEY_PGUP", "KEY_PGDN"});
    }
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            Vector2f origin = input.getCursorPosition();
            String[] split = name.split("_");
            if (split[0].matches("KEY")) {
                if (!hud.handleKeyAction(state, name, keyPressed)) {
                    //handle nav actions
                    if (name.matches("KEY_Q")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setRoll(1);
                        } else {
                            universe.getPlayerShip().setRoll(0);
                        }
                    }
                    if (name.matches("KEY_E")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setRoll(-1);
                        } else {
                            universe.getPlayerShip().setRoll(0);
                        }
                    }
                    if (name.matches("KEY_W")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setThrottle(1);
                        } else {
                            universe.getPlayerShip().setThrottle(0);
                        }
                    }
                    if (name.matches("KEY_S")) {
                        if (keyPressed) {
                            if (keyPressed) {
                                universe.getPlayerShip().setThrottle(-1);
                            } else {
                                universe.getPlayerShip().setThrottle(0);
                            }
                        }
                    }
                    if (name.matches("KEY_A")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setYaw(1);
                        } else {
                            universe.getPlayerShip().setYaw(0);
                        }
                    }
                    if (name.matches("KEY_D")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setYaw(-1);
                        } else {
                            universe.getPlayerShip().setYaw(0);
                        }
                    }
                    if (name.matches("KEY_UP")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setPitch(-1);
                        } else {
                            universe.getPlayerShip().setPitch(0);
                        }
                    }
                    if (name.matches("KEY_DOWN")) {
                        if (keyPressed) {
                            universe.getPlayerShip().setPitch(1);
                        } else {
                            universe.getPlayerShip().setPitch(0);
                        }
                    }
                }
            } else if (split[0].matches("MOUSE")) {
                hud.handleMouseAction(state, name, keyPressed, new Vector3f(origin.x, origin.y, 0));
            } else {
                /*
                 * These events are handled here because they are for the
                 * engine.
                 */
                //engine mode selection
                if (name.matches("Normal")) {
                    universe.getPlayerShip().setEngine(Ship.EngineMode.NORMAL);
                } else if (name.matches("Cruise")) {
                    universe.getPlayerShip().setEngine(Ship.EngineMode.CRUISE);
                } else if (name.matches("Newton")) {
                    universe.getPlayerShip().setEngine(Ship.EngineMode.NEWTON);
                }
                //quickload and quicksave
                if (name.matches("QuickSave")) {
                    save("Quick");
                } else if (name.matches("QuickLoad")) {
                    load("Quick");
                }
            }

        }
    };

    /*protected class JoystickEventListener implements RawInputListener {

     public void onJoyAxisEvent(JoyAxisEvent evt) {
     if (state == GameState.IN_SPACE) {
     if (evt.getAxis().getAxisId() == 0) {
     universe.getPlayer().getActiveShip().setYaw(-evt.getValue());
     } else if (evt.getAxis().getAxisId() == 1) {
     universe.getPlayer().getActiveShip().setPitch(evt.getValue());
     } else if (evt.getAxis().getAxisId() == 2) {
     universe.getPlayer().getActiveShip().setRoll(-evt.getValue());
     } else if (evt.getAxis().getAxisId() == 3) {
     universe.getPlayer().getActiveShip().setThrottle(-evt.getValue());
     }
     }
     }

     public void onJoyButtonEvent(JoyButtonEvent evt) {
     }

     public void beginInput() {
     }

     public void endInput() {
     }

     public void onMouseMotionEvent(MouseMotionEvent evt) {
     }

     public void onMouseButtonEvent(MouseButtonEvent evt) {
     }

     public void onKeyEvent(KeyInputEvent evt) {
     }

     public void onTouchEvent(TouchEvent evt) {
     }
     }*/

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
        /*
         * In-game updating
         */
        if (state == GameState.IN_SPACE) {
            //update world
            for (int a = 0; a < universe.getSystems().size(); a++) {
                universe.getSystems().get(a).periodicUpdate(tpf);
            }
            //update HUD
            hud.periodicUpdate(tpf);
        }
    }

    public void render(RenderManager rm) {
        //render hud
        hud.render(assets);
    }

    /*
     * Loading and saving
     */
    public void save(String gameName) {
        try {
            //save
            new AstralIO().saveGame(universe, gameName);
        } catch (Exception ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void load(String gameName) {
        try {
            //unload universe
            if (universe != null) {
                removeSystem(universe.getPlayerShip().getCurrentSystem());
                universe.setPlayerShip(null);
                universe = null;
            }
            //get everything
            Everything everything;
            FileInputStream fis = new FileInputStream(gameName + ".hab");
            ObjectInputStream ois = new ObjectInputStream(fis);
            everything = (Everything) ois.readObject();
            //unpack universe
            universe = everything.getUniverse();
            //enter the player's system
            addSystem(universe.getPlayerShip().getCurrentSystem());
            //restore camera
            planetAppState.freeCamera();
            planetAppState.setCameraShip(universe.getPlayerShip());
            //restore HUD
            if (hud != null) {
                hud.setUniverse(universe);
            }
            //go
            state = GameState.IN_SPACE;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
