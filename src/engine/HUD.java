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
 * Manages window components
 */
package engine;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import engine.Core.GameState;
import entity.Entity;
import gdi.CargoWindow;
import gdi.EquipmentWindow;
import gdi.FuelWindow;
import gdi.HealthWindow;
import gdi.OverviewWindow;
import gdi.PropertyWindow;
import gdi.StarMapWindow;
import gdi.TradeWindow;
import gdi.HudMarker;
import gdi.SightMarker;
import gdi.component.AstralWindow;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class HUD {
    //resources

    private Node guiNode;
    private Universe universe;
    private AssetManager assets;
    //camera
    AstralCamera camera;
    //windows
    ArrayList<AstralWindow> windows = new ArrayList<>();
    HealthWindow health;
    FuelWindow fuel;
    OverviewWindow overview;
    EquipmentWindow equipment;
    CargoWindow cargoWindow;
    PropertyWindow propertyWindow;
    TradeWindow tradeWindow;
    StarMapWindow starMapWindow;
    //IFF Manager
    IFFManager iffManager = new IFFManager();
    private boolean resetWindowFlag;
    //display
    private int width;
    private int height;

    public HUD(Node guiNode, Universe universe, int width, int height, AssetManager assets) {
        this.guiNode = guiNode;
        this.universe = universe;
        this.assets = assets;
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        //health window
        health = new HealthWindow(assets);
        health.setX((width / 2) - health.getWidth() / 2);
        health.setY(15);
        health.setVisible(true);
        windows.add(health);
        //fuel window
        fuel = new FuelWindow(assets);
        fuel.setX((width / 2) - health.getWidth() / 2);
        fuel.setY(30);
        fuel.setVisible(true);
        windows.add(fuel);
        //overview window
        overview = new OverviewWindow(assets);
        overview.setX(width - 315);
        overview.setY(15);
        overview.setVisible(true);
        windows.add(overview);
        //equipment window
        equipment = new EquipmentWindow(assets);
        equipment.setX(15);
        equipment.setY(15);
        equipment.setVisible(true);
        windows.add(equipment);
        //cargo window
        cargoWindow = new CargoWindow(assets);
        cargoWindow.setX((width / 2) - cargoWindow.getWidth() / 2);
        cargoWindow.setY((height / 2) - cargoWindow.getHeight() / 2);
        windows.add(cargoWindow);
        //property window
        propertyWindow = new PropertyWindow(assets);
        propertyWindow.setX((width / 2) - propertyWindow.getWidth() / 2);
        propertyWindow.setY((height / 2) - propertyWindow.getHeight() / 2);
        windows.add(propertyWindow);
        //trade window
        tradeWindow = new TradeWindow(assets);
        tradeWindow.setX((width / 2) - tradeWindow.getWidth() / 2);
        tradeWindow.setY((height / 2) - tradeWindow.getHeight() / 2);
        windows.add(tradeWindow);
        //star map window
        starMapWindow = new StarMapWindow(assets);
        starMapWindow.setX((width / 2) - starMapWindow.getWidth() / 2);
        starMapWindow.setY((height / 2) - starMapWindow.getHeight() / 2);
        windows.add(starMapWindow);
    }

    public void add() {
        //add markers
        iffManager.add();
        //add windows
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).add(guiNode);
        }
    }

    public void remove() {
        //remove markers
        iffManager.remove();
        //remove windows
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).remove(guiNode);
        }
    }

    public void periodicUpdate(float tpf, AstralCamera camera) {
        try {
            this.camera = camera;
            //update iffs
            iffManager.periodicUpdate(tpf);
            //resets windows if a new marker or window was added
            if (resetWindowFlag) {
                remove();
                add();
                resetWindowFlag = false;
            }
            //store camera
            this.camera = camera;
            //special update on simple windows
            health.updateHealth(getUniverse().getPlayerShip());
            fuel.updateFuel(getUniverse().getPlayerShip());
            overview.updateOverview(getUniverse().getPlayerShip());
            equipment.update(getUniverse().getPlayerShip());
            cargoWindow.update(getUniverse().getPlayerShip());
            propertyWindow.update(getUniverse().getPlayerShip());
            tradeWindow.update(getUniverse().getPlayerShip());
            starMapWindow.updateMap(getUniverse());
            //periodic update on other windows
            for (int a = 0; a < windows.size(); a++) {
                windows.get(a).periodicUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void render(AssetManager assets) {
        //update windows
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).render(null);
        }
        //update markers
        iffManager.render(assets);
    }

    public void collect() {
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).collect();
        }
        iffManager.collect();
    }

    public void handleMouseAction(GameState state, String name, boolean mousePressed, Vector3f mouseLoc) {
        //check focus changes
        checkFocusChanges((int) mouseLoc.x, (int) mouseLoc.y);
        //handle event
        for (int a = 0; a < windows.size(); a++) {
            if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                Vector3f adjLoc = new Vector3f(mouseLoc.x, height - mouseLoc.y, height);
                if (!mousePressed) {
                    windows.get(a).handleMouseReleasedEvent(name, adjLoc);
                } else {
                    windows.get(a).handleMousePressedEvent(name, adjLoc);
                }
                break;
            }
        }
    }

    public boolean handleKeyAction(GameState state, String name, boolean keyPressed) {
        for (int a = 0; a < windows.size(); a++) {
            if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                if (!keyPressed) {
                    windows.get(a).handleKeyReleasedEvent(name);
                } else {
                    windows.get(a).handleKeyPressedEvent(name);
                }
                return true;
            }
        }
        return false;
    }

    /*
     * The following are window event handlers. Do not add game logic to them.
     */
    private void checkFocusChanges(int mouseX, int mouseY) {
        /*
         * Window focus is determined based on mouse position.
         */
        Rectangle mRect = new Rectangle(mouseX, mouseY, 1, 1);
        boolean foundOne = false;
        for (int a = 0; a < windows.size(); a++) {
            if (windows.get(a).intersects(mRect) && windows.get(a).isVisible() && !foundOne) {
                windows.get(a).setFocused(true);
                windows.get(a).setOrder(0);
                foundOne = true;
                //pull and push
                windows.get(a).remove(guiNode);
                windows.get(a).add(guiNode);
            } else {
                windows.get(a).setFocused(false);
                windows.get(a).setOrder(windows.get(a).getOrder() - 1);
            }
        }
        if (foundOne) {
            /*
             * Since sorting can be expensive, I only resort windows when the focus is known
             * to have changed.
             */
            AstralWindow arr[] = new AstralWindow[windows.size()];
            for (int a = 0; a < windows.size(); a++) {
                arr[a] = windows.get(a);
            }
            for (int a = 0; a < arr.length; a++) {
                for (int b = 0; b < arr.length; b++) {
                    if (arr[a].getOrder() > arr[b].getOrder()) {
                        AstralWindow tmp = arr[b];
                        arr[b] = arr[a];
                        arr[a] = tmp;
                    }
                }
            }
            windows.clear();
            windows.addAll(Arrays.asList(arr));
        }
    }

    public Universe getUniverse() {
        return universe;
    }

    public void setUniverse(Universe universe) {
        this.universe = universe;
    }
    
    public void hideCentralWindows() {
        cargoWindow.setVisible(false);
        propertyWindow.setVisible(false);
        cargoWindow.setVisible(false);
        tradeWindow.setVisible(false);
        starMapWindow.setVisible(false);
    }

    public void toggleSensorWindow() {
        overview.setVisible(!overview.isVisible());
    }

    public void toggleEquipmentWindow() {
        equipment.setVisible(!equipment.isVisible());
    }

    public void toggleCargoWindow() {
        boolean visible = !cargoWindow.isVisible();
        hideCentralWindows();
        cargoWindow.setVisible(visible);
    }

    public void togglePropertyWindow() {
        boolean visible = !propertyWindow.isVisible();
        hideCentralWindows();
        propertyWindow.setVisible(visible);
    }

    public void toggleTradeWindow() {
        boolean visible = !tradeWindow.isVisible();
        hideCentralWindows();
        tradeWindow.setVisible(visible);
    }

    public void toggleStarMapWindow() {
        boolean visible = !starMapWindow.isVisible();
        hideCentralWindows();
        starMapWindow.setVisible(visible);
    }

    /*
     * This segment is for managing the IFF icons that ships have.
     * 
     * It takes advantage of the existing windowing system using transparent
     * windows and GDI elements to display the status of an object.
     */
    public void clearMarkers() {
        iffManager.markers.clear();
    }

    private class IFFManager {

        ArrayList<HudMarker> markers = new ArrayList<>();
        SightMarker sightMarker;

        public IFFManager() {
        }

        public void periodicUpdate(float tpf) {
            if (sightMarker == null) {
                sightMarker = new SightMarker(assets, universe.getPlayerShip(), camera, 25, 25);
                sightMarker.add(guiNode);
            }
            //update sight marker
            sightMarker.update(universe.getPlayerShip(), camera);
            sightMarker.periodicUpdate();
            /*
             * Determine if any new ship markers need to be added
             */
            //get the player's system
            SolarSystem system = universe.getPlayerShip().getCurrentSystem();
            //get a list of ships in that system
            ArrayList<Entity> ships = new ArrayList(system.getShipList());
            ArrayList<Entity> stations = new ArrayList(system.getStationList());
            ArrayList<Entity> combinedList = new ArrayList<>();
            combinedList.addAll(ships);
            combinedList.addAll(stations);
            //remove anything from this list we already have markers for
            for (int a = 0; a < markers.size(); a++) {
                combinedList.remove(markers.get(a).getTarget());
            }
            //remove anything not in sensor range
            for (int a = 0; a < combinedList.size(); a++) {
                float dist = combinedList.get(a).getLocation().distance(universe.getPlayerShip().getLocation());
                if (dist <= universe.getPlayerShip().getSensor()) {
                    //safe
                } else {
                    combinedList.remove(combinedList.get(a));
                }
            }
            //is there anything new to add?
            if (combinedList.size() > 0) {
                //add it
                for (int a = 0; a < combinedList.size(); a++) {
                    //make sure it isn't the player ship
                    if (combinedList.get(a) != universe.getPlayerShip()) {
                        float dist = combinedList.get(a).getLocation().distance(universe.getPlayerShip().getLocation());
                        if (dist < universe.getPlayerShip().getSensor()) {
                            HudMarker m = new HudMarker(assets, camera, combinedList.get(a), 50, 50);
                            markers.add(m);
                            m.setVisible(true);
                            m.add(guiNode);
                        }
                    }
                }
                //reset windowing
                resetWindowFlag = true;
            }
            /*
             * Update existing markers
             */
            for (int a = 0; a < markers.size(); a++) {
                if (markers.get(a).isRelevant()) {
                    markers.get(a).periodicUpdate();
                } else {
                    markers.get(a).remove(guiNode);
                    markers.remove(markers.get(a));
                }
            }
        }

        public void render(AssetManager assets) {
            for (int a = 0; a < markers.size(); a++) {
                markers.get(a).render(null);
            }
            //render sight marker
            sightMarker.render(null);
        }

        public void collect() {
            for (int a = 0; a < markers.size(); a++) {
                markers.get(a).collect();
            }
            if (sightMarker != null) {
                sightMarker.collect();
            }
        }

        public void add() {
            for (int a = 0; a < markers.size(); a++) {
                markers.get(a).add(guiNode);
            }
        }

        public void remove() {
            for (int a = 0; a < markers.size(); a++) {
                markers.get(a).remove(guiNode);
            }
        }
    }
}
