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
import celestial.Ship.Station;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Listener;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
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
import entity.Entity.State;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmeplanet.PlanetAppState;
import lib.Faction;
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
        QUOTE,
        MAIN_MENU,
        IN_SPACE,
        GAME_OVER
    }
    private GameState state = GameState.QUOTE;
    public static final float DEFAULT_TICK = 0.016666668f;
    //game objects
    private Universe universe;
    God god;
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
    Listener listener;
    private float tpf;
    //render safety
    boolean hudRendering = false;
    boolean hasFocus = true;
    //music
    private static final String menuTrack = "Audio/Music/Acclimated.wav";
    private String ambientTrack = "";
    private String dangerTrack = "";
    boolean isAmbient = true;
    private AudioNode music;
    //control mapping
    private int PITCH_AXIS;
    private int YAW_AXIS;
    private int ROLL_AXIS;
    private int THROTTLE_AXIS;

    public Core(Node rootNode, Node guiNode, BulletAppState bulletAppState,
            AssetManager assets, PlanetAppState planetAppState,
            InputManager input, AppSettings settings, Listener listener) {
        this.rootNode = rootNode;
        this.guiNode = guiNode;
        this.bulletAppState = bulletAppState;
        this.assets = assets;
        this.planetAppState = planetAppState;
        this.input = input;
        this.settings = settings;
        this.listener = listener;
        //load controls
        loadControls();
        //initialize
        init();
    }
    
    private void loadControls() {
        /*
         * This is going to one day allow total remapping of controls, but
         * for now it exists to make the joystick axes customizable.
         */
        //try to parse out the mappings
        try {
            Parser p = new Parser(AstralIO.getPayloadFile(), false);
            ArrayList<Term> maps = p.getTermsOfType("Mapper");
            for(int a = 0; a < maps.size(); a++) {
                Term map = maps.get(a);
                if(map.getValue("name").equals("Controls")) {
                    
                    //get raw data
                    String pitchString = map.getValue("j_pitch_axis");
                    String yawString = map.getValue("j_yaw_axis");
                    String rollString = map.getValue("j_roll_axis");
                    String throttleString = map.getValue("j_throttle_axis");
                    
                    //parse into mappings
                    PITCH_AXIS = Integer.parseInt(pitchString);
                    YAW_AXIS = Integer.parseInt(yawString);
                    ROLL_AXIS = Integer.parseInt(rollString);
                    THROTTLE_AXIS = Integer.parseInt(throttleString);
                    
                    System.out.println("Sucessfully applied custom mappings from "+AstralIO.getPayloadFile());
                    
                    break;
                }
            }
        } catch(Exception e) {
            System.out.println("Error: Unable to parse payload file "+AstralIO.getPayloadFile());
            System.out.println("Setting controls to defaults as fallback!");
            //default mappings
            PITCH_AXIS = 0;
            YAW_AXIS = 1;
            ROLL_AXIS = 2;
            THROTTLE_AXIS = 3;
            e.printStackTrace();
        }
    }

    private void init() {
        initKeys();
        initMouse();
        initJoyStick();
        //newGame("Default");
        //do last
        initPhysicsListeners();
        initHud();
        //bulletAppState.getPhysicsSpace().enableDebug(assets);
    }

    private void initHud() {
        hud = new HUD(guiNode, settings.getWidth(),
                settings.getHeight(), assets);
        hud.add();
    }

    private void initPhysicsListeners() {
        CollisionListener listener = new CollisionListener();
        bulletAppState.getPhysicsSpace().addCollisionListener(listener);
    }

    private void initGod() {
        god = null;
        god = new God(getUniverse());
    }

    public void newGame(String name) {
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
        resetScene();
        setUniverse(new Universe(assets));
        //determine start system
        String sysName = game.getValue("system");
        SolarSystem start = getUniverse().getSystemWithName(sysName);
        //determine start ship
        Parser ships = new Parser("SHIP.txt");
        String shipName = game.getValue("ship");
        Ship ship = null;
        ArrayList<Term> types = ships.getTermsOfType("Ship");
        for (int a = 0; a < types.size(); a++) {
            if (types.get(a).getValue("type").equals(shipName)) {
                ship = new Ship(getUniverse(), types.get(a), Faction.PLAYER);
                ship.setName("Your " + shipName);
                break;
            }
        }
        //put ship in start location
        float x = Float.parseFloat(game.getValue("x"));
        float y = Float.parseFloat(game.getValue("y"));
        float z = Float.parseFloat(game.getValue("z"));
        long cash = Long.parseLong(game.getValue("cash"));
        ship.setLocation(new Vector3f(x, y, z));
        ship.setCurrentSystem(start);
        ship.setCash(cash);
        start.putEntityInSystem(ship);
        //store initial cargo
        String cargo = game.getValue("cargo");
        ship.addInitialCargo(cargo);
        //store initial equipment
        String install = game.getValue("install");
        ship.addInitialEquipment(install);
        //setup start system
        addSystem(start);
        //center planetappstate on player ship
        planetAppState.setCameraShip(ship);
        //setup player shortcut
        getUniverse().setPlayerShip(ship);
        //inform hud of new universe
        hud.reset();
        hud.setUniverse(getUniverse());
        //start god
        initGod();
        //start game
        setState(GameState.IN_SPACE);

    }

    private void initMouse() {
        //mouse buttons
        input.addMapping("MOUSE_LClick", new MouseButtonTrigger(0));
        input.addMapping("MOUSE_RClick", new MouseButtonTrigger(1));
        input.addMapping("MOUSE_CClick", new MouseButtonTrigger(2));
        //mouse axis
        input.addMapping("Mouse_MOVELEFT", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        input.addMapping("MOUSE_MOVERIGHT", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        input.addMapping("MOUSE_MOVEUP", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        input.addMapping("MOUSE_MOVEDOWN", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        //store
        input.addListener(actionListener, new String[]{
            "MOUSE_LClick",
            "MOUSE_RClick",
            "MOUSE_CClick"});
        input.addListener(analogListener, new String[]{
            "MOUSE_MOVELEFT",
            "MOUSE_MOVERIGHT",
            "MOUSE_MOVEUP",
            "MOUSE_MOVEDOWN"});
    }

    private void initJoyStick() {
        input.addRawInputListener(new JoystickEventListener());
    }

    private void initKeys() {
        //remove escape key to exit
        input.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        input.addMapping("KEY_ESCAPE", new KeyTrigger(KeyInput.KEY_ESCAPE));
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
        input.addMapping("KEY_LEFT", new KeyTrigger(KeyInput.KEY_LEFT));
        input.addMapping("KEY_RIGHT", new KeyTrigger(KeyInput.KEY_RIGHT));
        //quick load and quick save dedicated keys
        input.addMapping("QuickSave", new KeyTrigger(KeyInput.KEY_INSERT));
        input.addMapping("QuickLoad", new KeyTrigger(KeyInput.KEY_PAUSE));
        //end key
        input.addMapping("KEY_END", new KeyTrigger(KeyInput.KEY_END));
        //home key
        input.addMapping("KEY_HOME", new KeyTrigger(KeyInput.KEY_HOME));
        //page keys
        input.addMapping("KEY_PGUP", new KeyTrigger(KeyInput.KEY_PGUP));
        input.addMapping("KEY_PGDN", new KeyTrigger(KeyInput.KEY_PGDN));
        //function keys
        input.addMapping("KEY_F1", new KeyTrigger(KeyInput.KEY_F1));
        input.addMapping("KEY_F2", new KeyTrigger(KeyInput.KEY_F2));
        input.addMapping("KEY_F3", new KeyTrigger(KeyInput.KEY_F3));
        input.addMapping("KEY_F4", new KeyTrigger(KeyInput.KEY_F4));
        input.addMapping("KEY_F5", new KeyTrigger(KeyInput.KEY_F5));
        input.addMapping("KEY_F6", new KeyTrigger(KeyInput.KEY_F6));
        input.addMapping("KEY_F7", new KeyTrigger(KeyInput.KEY_F7));
        input.addMapping("KEY_F8", new KeyTrigger(KeyInput.KEY_F8));
        input.addMapping("KEY_F9", new KeyTrigger(KeyInput.KEY_F9));
        input.addMapping("KEY_F10", new KeyTrigger(KeyInput.KEY_F10));
        input.addMapping("KEY_F11", new KeyTrigger(KeyInput.KEY_F11));
        input.addMapping("KEY_F12", new KeyTrigger(KeyInput.KEY_F12));
        //dash
        input.addMapping("KEY_MINUS", new KeyTrigger(KeyInput.KEY_MINUS));
        //add
        input.addListener(actionListener, new String[]{
            "KEY_0", "KEY_1", "KEY_2", "KEY_3", "KEY_4", "KEY_5", "KEY_6",
            "KEY_7", "KEY_8", "KEY_9",
            "KEY_B", "KEY_C", "KEY_F", "KEY_G", "KEY_H", "KEY_I", "KEY_J",
            "KEY_K", "KEY_L", "KEY_M", "KEY_N", "KEY_O", "KEY_P", "KEY_R",
            "KEY_T", "KEY_U", "KEY_V", "KEY_X", "KEY_Y", "KEY_Z",
            "KEY_W", "KEY_A", "KEY_S", "KEY_D", "KEY_SPACE", "KEY_RETURN",
            "KEY_Q", "KEY_E", "KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT",
            "KEY_BACKSPACE", "QuickSave", "QuickLoad", "KEY_END", "KEY_HOME",
            "KEY_PGUP", "KEY_PGDN", "KEY_F1", "KEY_F2", "KEY_F3", "KEY_F4",
            "KEY_F5", "KEY_F6", "KEY_F7", "KEY_F8", "KEY_F9", "KEY_F10",
            "KEY_F11", "KEY_F12", "KEY_MINUS", "KEY_ESCAPE"});
    }
    
    private AnalogListener analogListener = new AnalogListener() {

        @Override
        public void onAnalog(String string, float f, float f1) {
            String[] split = string.split("_");
            Vector2f origin = input.getCursorPosition();
            if(split[0].equals("MOUSE")) {
                hud.handleMouseMoved(state, string, 
                        new Vector3f(origin.x, origin.y, 0));
            }
        }
        
    };
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            Vector2f origin = input.getCursorPosition();
            String[] split = name.split("_");
            if (split[0].equals("KEY")) {
                if (!hud.handleKeyAction(getState(), name, keyPressed)) {
                    if (getState() == GameState.IN_SPACE) {
                        handleInSpaceKeys(name, keyPressed);
                    }
                }
            } else if (split[0].equals("MOUSE")) {
                hud.handleMouseAction(getState(), name, keyPressed,
                        new Vector3f(origin.x, origin.y, 0));
            } else {
                if (getState() == GameState.IN_SPACE) {
                    //quickload and quicksave
                    if (name.equals("QuickSave")) {
                        save("Quick");
                    } else if (name.equals("QuickLoad")) {
                        load("Quick");
                    }
                }
            }

        }

        private void handleInSpaceKeys(String name, boolean keyPressed) {
            //these keys show and hide GDI elements
            if (keyPressed) {
                if (name.equals("KEY_1")) {
                    //toggle equipment window
                    hud.toggleEquipmentWindow();
                }
                if (name.equals("KEY_2")) {
                    //toggle overview window
                    hud.toggleSensorWindow();
                }
                if (name.equals("KEY_3")) {
                    //toggle cargo window
                    hud.toggleCargoWindow();
                }
                if (name.equals("KEY_4")) {
                    //toggle property window
                    hud.togglePropertyWindow();
                }
                if (name.equals("KEY_5")) {
                    //toggle property window
                    hud.toggleStarMapWindow();
                }
                // * 6 is handled when docked * //
                if (name.equals("KEY_7")) {
                    //toggle standing window
                    hud.toggleStandingWindow();
                }
                if (name.equals("KEY_F4")) {
                    //toggle main menu window
                    hud.toggleMenuHomeWindow();
                }
            }
            if (!universe.getPlayerShip().isDocked()) {
                //docking
                if (name.equals("KEY_F1")) {
                    if (keyPressed) {
                        if (getUniverse().getPlayerShip().getTarget() instanceof Station) {
                            getUniverse().getPlayerShip().cmdDock((Station) getUniverse().getPlayerShip().getTarget());
                        }
                    }
                }
                //fire
                if (name.equals("KEY_SPACE")) {
                    getUniverse().getPlayerShip().setFiring(keyPressed);
                }
                //all stop
                if (name.equals("KEY_HOME")) {
                    getUniverse().getPlayerShip().cmdAllStop();
                }
                //handle nav actions
                if (name.equals("KEY_Q")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setRoll(1);
                    } else {
                        getUniverse().getPlayerShip().setRoll(0);
                    }
                }
                if (name.equals("KEY_E")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setRoll(-1);
                    } else {
                        getUniverse().getPlayerShip().setRoll(0);
                    }
                }
                if (name.equals("KEY_W")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setThrottle(1);
                    } else {
                        getUniverse().getPlayerShip().setThrottle(0);
                    }
                }
                if (name.equals("KEY_S")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setThrottle(-1);
                    } else {
                        getUniverse().getPlayerShip().setThrottle(0);
                    }
                }
                if (name.equals("KEY_A")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setYaw(1);
                    } else {
                        getUniverse().getPlayerShip().setYaw(0);
                    }
                }
                if (name.equals("KEY_D")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setYaw(-1);
                    } else {
                        getUniverse().getPlayerShip().setYaw(0);
                    }
                }
                if (name.equals("KEY_UP")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setPitch(-1);
                    } else {
                        getUniverse().getPlayerShip().setPitch(0);
                    }
                }
                if (name.equals("KEY_DOWN")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().setPitch(1);
                    } else {
                        getUniverse().getPlayerShip().setPitch(0);
                    }
                }
                if (name.equals("KEY_J")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().toggleMissiles();
                    }
                }
                if (name.equals("KEY_K")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().toggleCannons();
                    }
                }
                if (name.equals("KEY_R")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().targetNearestHostileShip();
                    }
                }
                if (name.equals("KEY_T")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().targetNearestFriendlyShip();
                    }
                }
                if (name.equals("KEY_Y")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().targetNearestNeutralShip();
                    }
                }
            } else {
                //docked only windows
                if (keyPressed) {
                    if (name.equals("KEY_6")) {
                        //toggle trade window
                        hud.toggleTradeWindow();
                    }
                }
                //undock
                if (name.equals("KEY_F1")) {
                    if (keyPressed) {
                        getUniverse().getPlayerShip().cmdUndock();
                    }
                }
            }
        }
    };

    protected class JoystickEventListener implements RawInputListener {

        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if (getState() == GameState.IN_SPACE) {
                if (!universe.getPlayerShip().isDocked()) {
                    if (evt.getAxis().getAxisId() == PITCH_AXIS) {
                        getUniverse().getPlayerShip().setYaw(-evt.getValue());
                    } else if (evt.getAxis().getAxisId() == YAW_AXIS) {
                        getUniverse().getPlayerShip().setPitch(evt.getValue());
                    } else if (evt.getAxis().getAxisId() == ROLL_AXIS) {
                        getUniverse().getPlayerShip().setRoll(-evt.getValue());
                    } /*
                     * POV / HAT used for thrust
                     */ else if (evt.getAxis().getAxisId() == THROTTLE_AXIS) {
                        if (Math.abs(evt.getValue()) > 0) {
                            getUniverse().getPlayerShip().setThrottle(evt.getValue());
                        } else {
                            getUniverse().getPlayerShip().setThrottle(0);
                        }
                    }
                }
            }
        }

        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if (getState() == GameState.IN_SPACE) {
                if (!universe.getPlayerShip().isDocked()) {
                    if (evt.getButton().getButtonId() == 0) {
                        getUniverse().getPlayerShip().setFiring(evt.isPressed());
                    } else if (evt.getButton().getButtonId() == 1) {
                        getUniverse().getPlayerShip().toggleMissiles();
                    }
                }
            }
        }

        public void beginInput() {
        }

        public void endInput() {
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent mme) {

        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent mbe) {

        }

        @Override
        public void onKeyEvent(KeyInputEvent kie) {
        }

        @Override
        public void onTouchEvent(TouchEvent te) {

        }
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
        /*
         * In-game updating
         */
        if (getState() == GameState.IN_SPACE) {
            doSpaceUpdate(tpf);
        } else if (getState() == GameState.MAIN_MENU) {
            doMenuUpdate(tpf);
        } else if (getState() == GameState.GAME_OVER) {
            doGameOverUpdate(tpf);
        } else if (getState() == GameState.QUOTE) {
            doQuoteUpdate(tpf);
        }
        //store tpf
        this.tpf = tpf;
    }
    
    private void doQuoteUpdate(float tpf) {
        /*
         * This state displays a quote from a file when the game first
         * starts. Once it elapses it will never be reached again within
         * my defined flow of the session.
         */
    }

    private void doGameOverUpdate(float tpf) {
        //todo: make this a proper state
        //for now return to menu
        suicide();
        setState(GameState.MAIN_MENU);
        isAmbient = true;
        ambientTrack = "";
        dangerTrack = "";
    }

    private void suicide() {
        planetAppState.freeCamera();
        unloadUniverse();
        resetScene();
        hud.reset();
    }

    private void doMenuUpdate(float tpf) {
        updateMusic();
    }

    private void doSpaceUpdate(float tpf) {
        if (!handlePlayerDeath()) {
            boolean godSafe = true;
            //update systems
            for (int a = 0; a < getUniverse().getSystems().size(); a++) {
                if (getUniverse().getSystems().get(a) != getUniverse().getPlayerShip().getCurrentSystem()) {
                    getUniverse().getSystems().get(a).oosPeriodicUpdate(tpf);
                } else {
                    //make sure there is no transition to be done
                    if (getUniverse().getPlayerShip().getCurrentSystem().hasGraphics()) {
                        //update
                        getUniverse().getPlayerShip().getCurrentSystem().periodicUpdate(tpf);
                    } else {
                        //transition to the new system
                        resetScene();
                        addSystem(getUniverse().getPlayerShip().getCurrentSystem());
                        resetCamera();
                        //make sure the new system is flagged for graphics
                        getUniverse().getPlayerShip().getCurrentSystem().forceGraphics();
                        godSafe = false;
                    }
                }
            }
            //update god
            if (godSafe) {
                god.periodicUpdate();
            }
            //see if we need to reset the camera
            if (planetAppState.getAstralCamera() != null) {
                if (getUniverse().getPlayerShip() != planetAppState.getAstralCamera().getTarget()) {
                    resetCamera();
                    resetHUD();
                }
            } else {
                resetCamera();
                resetHUD();
            }
            //update sound
            updateSpaceAudio();
            updateMusic();
        }
    }

    private boolean handlePlayerDeath() {
        if (getUniverse().getPlayerShip() == null
                || getUniverse().getPlayerShip().getState() == State.DEAD) {
            setState(GameState.GAME_OVER);
            return true;
        }
        return false;
    }

    public void render(RenderManager rm) {
        doHUDRendering();
    }

    private void doHUDRendering() {
        if (hudRendering) {
            //wait
        } else {
            if (hasFocus) {
                //collect from the previous render thread
                hud.collect();
                //update
                hud.periodicUpdate(tpf, this);
                //now start another render thread
                final Core passCore = this;
                Thread t = new Thread() {
                    public void run() {
                        try {
                            //flag
                            hudRendering = true;
                            //render hud
                            hud.render(assets, passCore);
                        } catch (Exception e) {
                            System.out.println("Hud rendering messed up");
                        }
                        //dismiss
                        hudRendering = false;
                    }
                };
                t.start();
            } else {
                //don't do any gui rendering without focus and do not collect!
            }
        }
    }

    private void updateSpaceAudio() {
        //center audio listener on player
        listener.setLocation(getUniverse().getPlayerShip().getLocation());
        //play sound effects for ships
        SolarSystem playerSystem = getUniverse().getPlayerShip().getCurrentSystem();
        ArrayList<Entity> celestials = playerSystem.getCelestials();
        for (int a = 0; a < celestials.size(); a++) {
            if (celestials.get(a) instanceof Ship) {
                Ship tmp = (Ship) celestials.get(a);
                if (tmp.distanceTo(getUniverse().getPlayerShip()) < Universe.SOUND_RANGE) {
                    if (tmp.getSoundQue() != null) {
                        for (int b = 0; b < tmp.getSoundQue().size(); b++) {
                            //I'm not permitting looping sounds to be played by ships using the que
                            tmp.getSoundQue().get(b).setLooping(false);
                            //play the sound and pop it off
                            tmp.getSoundQue().get(b).play();
                            tmp.getSoundQue().remove(b);
                        }
                    } else {
                        //this ship has not initialized its sound que
                    }
                } else {
                    //too far away to care about
                    tmp.getSoundQue().clear();
                }
            }
        }
    }

    /*
     * Loading and saving
     */
    public void save(String gameName) {
        try {
            //save
            new AstralIO().saveGame(getUniverse(), gameName);
        } catch (Exception ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void load(String gameName) {
        try {
            unloadUniverse();
            resetScene();
            //get everything
            Everything everything;
            FileInputStream fis
                    = new FileInputStream(AstralIO.getSaveDir() + gameName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            everything = (Everything) ois.readObject();
            //unpack universe
            setUniverse(everything.getUniverse());
            //enter the player's system
            addSystem(getUniverse().getPlayerShip().getCurrentSystem());
            //reset camera
            resetCamera();
            //restore HUD
            if (hud != null) {
                hud.setUniverse(getUniverse());
            }
            //restore assets
            getUniverse().setAssets(assets);
            //restore god
            initGod();
            //go
            setState(GameState.IN_SPACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unloadUniverse() {
        //unload universe
        if (getUniverse() != null) {
            removeSystem(getUniverse().getPlayerShip().getCurrentSystem());
            getUniverse().setPlayerShip(null);
            setUniverse(null);
        }
    }

    private void resetScene() {
        System.gc();
        if (hud != null) {
            clearHUD();
        }
        //clear nodes
        rootNode.detachAllChildren();
        //clear lights
        rootNode.getLocalLightList().clear();
        rootNode.getWorldLightList().clear();
        //clear physics
        bulletAppState.getPhysicsSpace().destroy();
        bulletAppState.getPhysicsSpace().create();
        bulletAppState.getPhysicsSpace().setGravity(Vector3f.ZERO);
        bulletAppState.getPhysicsSpace().setAccuracy(DEFAULT_TICK / 4);
        initPhysicsListeners();
        addHUD();
        System.gc();
    }

    private void updateMusic() {
        if (state == GameState.MAIN_MENU) {
            if (!ambientTrack.equals(menuTrack)) {
                switchTrack(true, menuTrack);
            }
        } else if (state == GameState.IN_SPACE) {
            if (universe.getPlayerShip() != null) {
                boolean danger = false;
                ArrayList<Ship> tests = universe.getPlayerShip().getShipsInSensorRange();
                for (int a = 0; a < tests.size(); a++) {
                    if (universe.getPlayerShip().isHostileToMe(tests.get(a))) {
                        danger = true;
                        break;
                    }
                }
                if (!danger) {
                    if (ambientTrack.equals(universe.getPlayerShip().getCurrentSystem().getAmbientMusic()) && isAmbient) {
                        //do nothing
                    } else {
                        switchTrack(true, universe.getPlayerShip().getCurrentSystem().getAmbientMusic());
                    }
                } else if (danger) {
                    if (dangerTrack.equals(universe.getPlayerShip().getCurrentSystem().getDangerMusic()) && !isAmbient) {
                        //do nothing
                    } else {
                        switchTrack(false, universe.getPlayerShip().getCurrentSystem().getDangerMusic());
                    }
                }
            }
        }
    }

    private void switchTrack(boolean ambient, String target) {
        try {
            if (music != null) {
                music.setLooping(false);
                music.stop();
            }
            music = new AudioNode(assets, target);
            music.setLooping(true);
            music.setPositional(false);
            music.play();
            isAmbient = ambient;
            if (ambient) {
                ambientTrack = target;
            } else {
                dangerTrack = target;
            }
        } catch (Exception e) {
            System.out.println("Failed to switch track");
        }
    }

    private void addHUD() {
        //add hud
        if (hud != null) {
            hud.add();
        }
    }

    private void clearHUD() {
        hud.reset();
        //undo hud
        hud.remove();
        //clear markers
        hud.clearMarkers();
    }

    private void resetHUD() {
        clearHUD();
        addHUD();
    }

    private void resetCamera() {
        //restore camera
        planetAppState.freeCamera();
        planetAppState.setCameraShip(getUniverse().getPlayerShip());
    }

    public AstralCamera getCamera() {
        return planetAppState.getAstralCamera();
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    public void setFocus(boolean hasFocus) {
        this.hasFocus = hasFocus;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public Universe getUniverse() {
        return universe;
    }

    public void setUniverse(Universe universe) {
        this.universe = universe;
    }
}
