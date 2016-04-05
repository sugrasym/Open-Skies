/*
 * Copyright (c) 2015 Nathan Wiehoff
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
    public static final float TICK_DIVIDER = 4.0f;
    //game objects
    private Universe universe;
    private Ship playerShip;
    
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
    private int JOYSTICK_PITCH_AXIS;
    private int JOYSTICK_YAW_AXIS;
    private int JOYSTICK_ROLL_AXIS;
    private int JOYSTICK_THROTTLE_AXIS;
    private String KEY_YAW_LEFT;
    private String KEY_YAW_RIGHT;
    private String KEY_FORWARD_THRUST;
    private String KEY_REVERSE_THRUST;
    private String KEY_PITCH_UP;
    private String KEY_PITCH_DOWN;
    private String KEY_ROLL_LEFT;
    private String KEY_ROLL_RIGHT;
    private String KEY_EQUIPMENT_WINDOW;
    private String KEY_OVERVIEW_WINDOW;
    private String KEY_CARGO_WINDOW;
    private String KEY_PROPERTY_WINDOW;
    private String KEY_STARMAP_WINDOW;
    private String KEY_TRADE_WINDOW;
    private String KEY_STANDING_WINDOW;
    private String KEY_COMM_WINDOW;
    private String KEY_TGT_NEAREST_ENEMY;
    private String KEY_TGT_NEAREST_FRIENDLY;
    private String KEY_TGT_NEAREST_NEUTRAL;
    private String KEY_FIRE;
    private String KEY_STOP;
    private String KEY_TOGGLE_MISSILES;
    private String KEY_TOGGLE_CANNONS;
    private String KEY_TOGGLE_TURRETS;
    private String KEY_CONFIGURE_COMBAT;
    private String KEY_CONFIGURE_CRUISE;
    private int JOYSTICK_FIRE_BUTTON;
    private int JOYSTICK_SEC_BUTTON;
    private float JOYSTICK_DEADZONE_ROTATION;
    private float JOYSTICK_DEADZONE_THROTTLE;
    
    //control switches
    private boolean shiftDown = false;

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
         * Allows controls to be remapped by editing a text file.
         */
        //try to parse out the mappings
        try {
            Parser p = new Parser(AstralIO.getPayloadFile(), false);
            ArrayList<Term> maps = p.getTermsOfType("Mapper");
            for (int a = 0; a < maps.size(); a++) {
                Term map = maps.get(a);
                if (map.getValue("name").equals("Controls")) {

                    //get joystick axis
                    String pitchString = map.getValue("j_pitch_axis");
                    String yawString = map.getValue("j_yaw_axis");
                    String rollString = map.getValue("j_roll_axis");
                    String throttleString = map.getValue("j_throttle_axis");

                    String keyForwardThrustString = map.getValue("k_forward_thrust");
                    String keyReverseThrustString = map.getValue("k_reverse_thrust");
                    String keyYawLeftString = map.getValue("k_yaw_left");
                    String keyYawRightString = map.getValue("k_yaw_right");

                    String keyPitchUpString = map.getValue("k_pitch_up");
                    String keyPitchDownString = map.getValue("k_pitch_down");

                    String keyRollLeftString = map.getValue("k_roll_left");
                    String keyRollRightString = map.getValue("k_roll_right");

                    String keyEquipmentWindowString = map.getValue("k_equipment_window");
                    String keyOverviewWindowString = map.getValue("k_overview_window");
                    String keyCargoWindowString = map.getValue("k_cargo_window");
                    String keyPropertyWindowString = map.getValue("k_property_window");
                    String keyStarmapWindowString = map.getValue("k_starmap_window");
                    String keyTradeWindowString = map.getValue("k_trade_window");
                    String keyStandingWindowString = map.getValue("k_standing_window");
                    String keyCommWindowString = map.getValue("k_comm_window");

                    String keyNearestEnemyString = map.getValue("k_tgt_nearest_enemy");
                    String keyNearestFriendlyString = map.getValue("k_tgt_nearest_friendly");
                    String keyNearestNeutralString = map.getValue("k_tgt_nearest_neutral");

                    String keyFireString = map.getValue("k_fire");
                    String keyStopString = map.getValue("k_stop");

                    String keyToggleMissilesString = map.getValue("k_toggle_missiles");
                    String keyToggleCannonsString = map.getValue("k_toggle_cannons");
                    String keyToggleTurretsString = map.getValue("k_toggle_turrets");

                    String keyConfigureCombatString = map.getValue("k_configure_combat");
                    String keyConfigureCruiseString = map.getValue("k_configure_cruise");

                    String joyFireButton = map.getValue("j_fire");
                    String joySecButton = map.getValue("j_sec");

                    String joyDeadThrottle = map.getValue("j_deadzone_throttle");
                    String joyDeadRotation = map.getValue("j_deadzone_rotation");

                    //parse into mappings
                    JOYSTICK_PITCH_AXIS = Integer.parseInt(pitchString);
                    JOYSTICK_YAW_AXIS = Integer.parseInt(yawString);
                    JOYSTICK_ROLL_AXIS = Integer.parseInt(rollString);
                    JOYSTICK_THROTTLE_AXIS = Integer.parseInt(throttleString);

                    KEY_FORWARD_THRUST = keyForwardThrustString.trim();
                    KEY_REVERSE_THRUST = keyReverseThrustString.trim();
                    KEY_YAW_LEFT = keyYawLeftString.trim();
                    KEY_YAW_RIGHT = keyYawRightString.trim();

                    KEY_PITCH_UP = keyPitchUpString.trim();
                    KEY_PITCH_DOWN = keyPitchDownString.trim();

                    KEY_ROLL_LEFT = keyRollLeftString.trim();
                    KEY_ROLL_RIGHT = keyRollRightString.trim();

                    KEY_EQUIPMENT_WINDOW = keyEquipmentWindowString.trim();
                    KEY_OVERVIEW_WINDOW = keyOverviewWindowString.trim();
                    KEY_CARGO_WINDOW = keyCargoWindowString.trim();
                    KEY_PROPERTY_WINDOW = keyPropertyWindowString.trim();
                    KEY_STARMAP_WINDOW = keyStarmapWindowString.trim();
                    KEY_TRADE_WINDOW = keyTradeWindowString.trim();
                    KEY_STANDING_WINDOW = keyStandingWindowString.trim();
                    KEY_COMM_WINDOW = keyCommWindowString.trim();

                    KEY_TGT_NEAREST_ENEMY = keyNearestEnemyString.trim();
                    KEY_TGT_NEAREST_FRIENDLY = keyNearestFriendlyString.trim();
                    KEY_TGT_NEAREST_NEUTRAL = keyNearestNeutralString.trim();

                    KEY_FIRE = keyFireString.trim();
                    KEY_STOP = keyStopString.trim();

                    KEY_TOGGLE_MISSILES = keyToggleMissilesString.trim();
                    KEY_TOGGLE_CANNONS = keyToggleCannonsString.trim();
                    KEY_TOGGLE_TURRETS = keyToggleTurretsString.trim();

                    KEY_CONFIGURE_COMBAT = keyConfigureCombatString.trim();
                    KEY_CONFIGURE_CRUISE = keyConfigureCruiseString.trim();

                    JOYSTICK_FIRE_BUTTON = Integer.parseInt(joyFireButton.trim());
                    JOYSTICK_SEC_BUTTON = Integer.parseInt(joySecButton.trim());

                    JOYSTICK_DEADZONE_THROTTLE = Float.parseFloat(joyDeadThrottle.trim());
                    JOYSTICK_DEADZONE_ROTATION = Float.parseFloat(joyDeadRotation.trim());

                    System.out.println("Sucessfully applied custom mappings from " + AstralIO.getPayloadFile());

                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Unable to parse payload file " + AstralIO.getPayloadFile());
            System.out.println("Setting controls to defaults as fallback!");

            //default mappings
            JOYSTICK_PITCH_AXIS = 0;
            JOYSTICK_YAW_AXIS = 1;
            JOYSTICK_ROLL_AXIS = 2;
            JOYSTICK_THROTTLE_AXIS = 3;
            KEY_FORWARD_THRUST = "KEY_W";
            KEY_REVERSE_THRUST = "KEY_S";
            KEY_YAW_LEFT = "KEY_A";
            KEY_YAW_RIGHT = "KEY_D";
            KEY_PITCH_UP = "KEY_UP";
            KEY_PITCH_DOWN = "KEY_DOWN";
            KEY_ROLL_LEFT = "KEY_Q";
            KEY_ROLL_RIGHT = "KEY_E";
            KEY_EQUIPMENT_WINDOW = "KEY_1";
            KEY_OVERVIEW_WINDOW = "KEY_2";
            KEY_CARGO_WINDOW = "KEY_3";
            KEY_PROPERTY_WINDOW = "KEY_4";
            KEY_STARMAP_WINDOW = "KEY_5";
            KEY_TRADE_WINDOW = "KEY_6";
            KEY_STANDING_WINDOW = "KEY_7";
            KEY_COMM_WINDOW = "KEY_8";
            KEY_TGT_NEAREST_ENEMY = "KEY_R";
            KEY_TGT_NEAREST_FRIENDLY = "KEY_T";
            KEY_TGT_NEAREST_NEUTRAL = "KEY_Y";
            KEY_FIRE = "KEY_SPACE";
            KEY_STOP = "KEY_HOME";
            KEY_TOGGLE_MISSILES = "KEY_J";
            KEY_TOGGLE_CANNONS = "KEY_K";
            KEY_TOGGLE_TURRETS = "KEY_L";
            KEY_CONFIGURE_COMBAT = "KEY_C";
            KEY_CONFIGURE_CRUISE = "KEY_V";
            JOYSTICK_FIRE_BUTTON = 0;
            JOYSTICK_SEC_BUTTON = 1;
            JOYSTICK_DEADZONE_THROTTLE = 0.1f;
            JOYSTICK_DEADZONE_ROTATION = 0.01f;

            System.out.println("Redeploying payload file");
            AstralIO.forceDeployControlPayload();
        }
    }

    private void init() {
        initKeys();
        initMouse();
        initJoyStick();
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
        
        ArrayList<Term> types = ships.getTermsOfType("Ship");
        for (int a = 0; a < types.size(); a++) {
            if (types.get(a).getValue("type").equals(shipName)) {
                playerShip = new Ship(getUniverse(), types.get(a), Faction.PLAYER);
                playerShip.setName("Your " + shipName);
                break;
            }
        }
        //put ship in start location
        float x = Float.parseFloat(game.getValue("x"));
        float y = Float.parseFloat(game.getValue("y"));
        float z = Float.parseFloat(game.getValue("z"));
        long cash = Long.parseLong(game.getValue("cash"));
        playerShip.setLocation(new Vector3f(x, y, z));
        playerShip.setCurrentSystem(start);
        playerShip.setCash(cash);
        start.putEntityInSystem(playerShip);
        //store initial cargo
        String cargo = game.getValue("cargo");
        playerShip.addInitialCargo(cargo);
        //store initial equipment
        String install = game.getValue("install");
        playerShip.addInitialEquipment(install);
        //setup start system
        addSystem(start);
        //setup player shortcut
        getUniverse().setPlayerShip(playerShip);
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

        input.addMapping("MOUSE_SCROLLUP", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        input.addMapping("MOUSE_SCROLLDOWN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        //store
        input.addListener(actionListener, new String[]{
            "MOUSE_LClick",
            "MOUSE_RClick",
            "MOUSE_CClick"});
        input.addListener(analogListener, new String[]{
            "MOUSE_MOVELEFT",
            "MOUSE_MOVERIGHT",
            "MOUSE_MOVEUP",
            "MOUSE_MOVEDOWN",
            "MOUSE_SCROLLUP",
            "MOUSE_SCROLLDOWN"});
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
        //shift keys
        input.addMapping("KEY_LSHIFT", new KeyTrigger(KeyInput.KEY_LSHIFT));
        input.addMapping("KEY_RSHIFT", new KeyTrigger(KeyInput.KEY_RSHIFT));
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
            "KEY_F11", "KEY_F12", "KEY_MINUS", "KEY_ESCAPE", "KEY_LSHIFT",
            "KEY_RSHIFT"});
    }

    private final AnalogListener analogListener = (String string, float f, float f1) -> {
        String[] split = string.split("_");
        Vector2f origin = input.getCursorPosition();
        if (split[0].equals("MOUSE")) {
            hud.handleMouseMoved(state, string,
                    new Vector3f(origin.x, origin.y, 0));
        }
    };

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("KEY_LSHIFT")) {
                shiftDown = keyPressed;
            }
            Vector2f origin = input.getCursorPosition();
            String[] split = name.split("_");
            switch (split[0]) {
                case "KEY":
                    if (!hud.handleKeyAction(getState(), name, keyPressed, shiftDown)) {
                        if (getState() == GameState.IN_SPACE) {
                            handleInSpaceKeys(name, keyPressed);
                        }
                    }
                    break;
                case "MOUSE":
                    hud.handleMouseAction(getState(), name, keyPressed,
                            new Vector3f(origin.x, origin.y, 0));
                    break;
                default:
                    if (getState() == GameState.IN_SPACE) {
                        //quickload and quicksave
                        switch (name) {
                            case "QuickSave":
                                save("Quick");
                                break;
                            case "QuickLoad":
                                load("Quick");
                                break;
                        }
                    }
                    break;
            }

        }

        private void handleInSpaceKeys(String name, boolean keyPressed) {
            //these keys show and hide GDI elements
            if (keyPressed) {
                if (name.equals(KEY_EQUIPMENT_WINDOW)) {
                    //toggle equipment window
                    hud.toggleEquipmentWindow();
                }
                if (name.equals(KEY_OVERVIEW_WINDOW)) {
                    //toggle overview window
                    hud.toggleSensorWindow();
                }
                if (name.equals(KEY_CARGO_WINDOW)) {
                    //toggle cargo window
                    hud.toggleCargoWindow();
                }
                if (name.equals(KEY_PROPERTY_WINDOW)) {
                    //toggle property window
                    hud.togglePropertyWindow();
                }
                if (name.equals(KEY_STARMAP_WINDOW)) {
                    //toggle property window
                    hud.toggleStarMapWindow();
                }
                // * 6 is handled when docked * //
                if (name.equals(KEY_STANDING_WINDOW)) {
                    //toggle standing window
                    hud.toggleStandingWindow();
                }
                if (name.equals(KEY_COMM_WINDOW)) {
                    //toggle comm window
                    hud.toggleCommWindow();
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
                        if (playerShip.getTarget() instanceof Station) {
                            playerShip.cmdDock((Station) playerShip.getTarget());
                        }
                    }
                }
                if (name.equals("KEY_H")) {
                    if (keyPressed) {
                        if (playerShip.getTarget() != null) {
                            playerShip.getTarget().hail();
                            hud.commWindow.setVisible(true);
                        }
                    }
                }
                //fire
                if (name.equals(KEY_FIRE)) {
                    playerShip.setFiring(keyPressed);
                }
                //all stop
                if (name.equals(KEY_STOP)) {
                    playerShip.cmdAllStop();
                }
                //handle nav actions
                if (name.equals(KEY_ROLL_LEFT)) {
                    if (keyPressed) {
                        playerShip.setRoll(1);
                    } else {
                        playerShip.setRoll(0);
                    }
                }
                if (name.equals(KEY_ROLL_RIGHT)) {
                    if (keyPressed) {
                        playerShip.setRoll(-1);
                    } else {
                        playerShip.setRoll(0);
                    }
                }
                if (name.equals(KEY_FORWARD_THRUST)) {
                    if (keyPressed) {
                        playerShip.setThrottle(1);
                    } else {
                        playerShip.setThrottle(0);
                    }
                }
                if (name.equals(KEY_REVERSE_THRUST)) {
                    if (keyPressed) {
                        playerShip.setThrottle(-1);
                    } else {
                        playerShip.setThrottle(0);
                    }
                }
                if (name.equals(KEY_YAW_LEFT)) {
                    if (keyPressed) {
                        playerShip.setYaw(1);
                    } else {
                        playerShip.setYaw(0);
                    }
                }
                if (name.equals(KEY_YAW_RIGHT)) {
                    if (keyPressed) {
                        playerShip.setYaw(-1);
                    } else {
                        playerShip.setYaw(0);
                    }
                }
                if (name.equals(KEY_PITCH_UP)) {
                    if (keyPressed) {
                        playerShip.setPitch(-1);
                    } else {
                        playerShip.setPitch(0);
                    }
                }
                if (name.equals(KEY_PITCH_DOWN)) {
                    if (keyPressed) {
                        playerShip.setPitch(1);
                    } else {
                        playerShip.setPitch(0);
                    }
                }
                if (name.equals(KEY_TOGGLE_MISSILES)) {
                    if (keyPressed) {
                        playerShip.toggleMissiles();
                    }
                }
                if (name.equals(KEY_TOGGLE_CANNONS)) {
                    if (keyPressed) {
                        playerShip.toggleCannons();
                    }
                }
                if (name.equals(KEY_TOGGLE_TURRETS)) {
                    if (keyPressed) {
                        playerShip.toggleTurrets();
                    }
                }
                if (name.equals(KEY_CONFIGURE_COMBAT)) {
                    if (keyPressed) {
                        playerShip.configureForCombat();
                    }
                }
                if (name.equals(KEY_CONFIGURE_CRUISE)) {
                    if (keyPressed) {
                        playerShip.configureForCruise();
                    }
                }
                if (name.equals(KEY_TGT_NEAREST_ENEMY)) {
                    if (keyPressed) {
                        playerShip.targetNearestHostileShip();
                    }
                }
                if (name.equals(KEY_TGT_NEAREST_FRIENDLY)) {
                    if (keyPressed) {
                        playerShip.targetNearestFriendlyShip();
                    }
                }
                if (name.equals(KEY_TGT_NEAREST_NEUTRAL)) {
                    if (keyPressed) {
                        playerShip.targetNearestNeutralShip();
                    }
                }
            } else {
                //docked only windows
                if (keyPressed) {
                    if (name.equals(KEY_TRADE_WINDOW)) {
                        //toggle trade window
                        hud.toggleTradeWindow();
                    }
                }
                //undock
                if (name.equals("KEY_F1")) {
                    if (keyPressed) {
                        playerShip.cmdUndock();
                    }
                }
            }
        }
    };

    protected class JoystickEventListener implements RawInputListener {

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if (getState() == GameState.IN_SPACE) {
                if (!universe.getPlayerShip().isDocked()) {
                    if (Math.abs(evt.getAxis().getAxisId()) == JOYSTICK_PITCH_AXIS) {
                        if (Math.abs(evt.getValue()) > JOYSTICK_DEADZONE_ROTATION) {
                            playerShip.setYaw(-evt.getValue());
                        } else {
                            playerShip.setYaw(0);
                        }
                    } else if (evt.getAxis().getAxisId() == JOYSTICK_YAW_AXIS) {
                        if (Math.abs(evt.getValue()) > JOYSTICK_DEADZONE_ROTATION) {
                            playerShip.setPitch(evt.getValue());
                        } else {
                            playerShip.setPitch(0);
                        }
                    } else if (evt.getAxis().getAxisId() == JOYSTICK_ROLL_AXIS) {
                        if (Math.abs(evt.getValue()) > JOYSTICK_DEADZONE_ROTATION) {
                            playerShip.setRoll(-evt.getValue());
                        } else {
                            playerShip.setRoll(0);
                        }
                    } /*
                     * POV / HAT used for thrust
                     */ else if (evt.getAxis().getAxisId() == JOYSTICK_THROTTLE_AXIS) {
                        if (Math.abs(evt.getValue()) > JOYSTICK_DEADZONE_THROTTLE) { //deadzone but it really should be mapped to a HAT axis
                            playerShip.setThrottle(evt.getValue());
                        } else {
                            playerShip.setThrottle(0);
                        }
                    }
                }
            }
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if (getState() == GameState.IN_SPACE) {
                if (!universe.getPlayerShip().isDocked()) {
                    if (evt.getButton().getButtonId() == JOYSTICK_FIRE_BUTTON) {
                        playerShip.setFiring(evt.isPressed());
                    } else if (evt.getButton().getButtonId() == JOYSTICK_SEC_BUTTON) {
                        playerShip.toggleMissiles();
                    }
                }
            }
        }

        @Override
        public void beginInput() {
        }

        @Override
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
        //System.out.println("tpf: "+tpf+" , "+"tick: "+(DEFAULT_TICK/TICK_DIVIDER)+" , "+" magic: "+((DEFAULT_TICK/TICK_DIVIDER) + tpf) / 2.0f);
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
        planetAppState.getAstralCamera().freeCamera();
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
                if (getUniverse().getSystems().get(a) != playerShip.getCurrentSystem()) {
                    getUniverse().getSystems().get(a).oosPeriodicUpdate(tpf);
                } else {
                    //make sure there is no transition to be done
                    if (playerShip.getCurrentSystem().hasGraphics()) {
                        //update
                        playerShip.getCurrentSystem().periodicUpdate(tpf);
                    } else {
                        //transition to the new system
                        resetScene();
                        addSystem(playerShip.getCurrentSystem());
                        planetAppState.getAstralCamera().setTarget(playerShip);
                        //make sure the new system is flagged for graphics
                        playerShip.getCurrentSystem().forceGraphics();
                        godSafe = false;
                    }
                }
            }
            //update god
            if (godSafe) {
                god.periodicUpdate();
            }
            //update player missions
            for (int a = 0; a < universe.getPlayerMissions().size(); a++) {
                universe.getPlayerMissions().get(a).periodicUpdate(tpf);
            }
            //see if we need to reset the camera
            AstralCamera camera = planetAppState.getAstralCamera();
            if (camera != null) {
                if (playerShip != camera.getTarget()) {
                    camera.setTarget(playerShip);
                    resetHUD();
                }
            }
            
            //update sound
            updateSpaceAudio();
            updateMusic();
        }
    }

    private boolean handlePlayerDeath() {
        if (playerShip == null
                || playerShip.getState() == State.DEAD) {
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
                    @Override
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
        listener.setLocation(playerShip.getLocation());
        //play sound effects for ships
        SolarSystem playerSystem = playerShip.getCurrentSystem();
        ArrayList<Entity> celestials = playerSystem.getCelestials();
        for (int a = 0; a < celestials.size(); a++) {
            if (celestials.get(a) instanceof Ship) {
                Ship tmp = (Ship) celestials.get(a);
                if (tmp.distanceTo(playerShip) < Universe.SOUND_RANGE) {
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
            playerShip = universe.getPlayerShip();
            addSystem(playerShip.getCurrentSystem());
            //reset camera
            planetAppState.getAstralCamera().setTarget(playerShip);
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
            removeSystem(playerShip.getCurrentSystem());
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
        bulletAppState.getPhysicsSpace().setAccuracy(DEFAULT_TICK / TICK_DIVIDER);
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
