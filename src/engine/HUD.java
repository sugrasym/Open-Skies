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
import gdi.EquipmentWindow;
import gdi.FuelWindow;
import gdi.HealthWindow;
import gdi.OverviewWindow;
import gdi.component.AstralWindow;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
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
    //windows
    ArrayList<AstralWindow> windows = new ArrayList<>();
    HealthWindow health;
    FuelWindow fuel;
    OverviewWindow overview;
    EquipmentWindow equipment;
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
    }

    public void add() {
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).add(guiNode);
        }
    }

    public void remove() {
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).remove(guiNode);
        }
    }

    public void periodicUpdate(float tpf) {
        try {
        //special update on simple windows
        health.updateHealth(getUniverse().getPlayerShip());
        fuel.updateFuel(getUniverse().getPlayerShip());
        overview.updateOverview(getUniverse().getPlayerShip());
        equipment.update(getUniverse().getPlayerShip());
        //periodic update on other windows
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).periodicUpdate();
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void render(AssetManager assets) {
        for (int a = 0; a < windows.size(); a++) {
            windows.get(a).render(null);
        }
    }

    public void handleMouseAction(GameState state, String name, boolean mousePressed, Vector3f mouseLoc) {
        //check focus changes
        checkFocusChanges((int) mouseLoc.x, (int) mouseLoc.y);
        //handle event
        for (int a = 0; a < windows.size(); a++) {
            if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                Vector3f adjLoc = new Vector3f(mouseLoc.x, height-mouseLoc.y,height);
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
    
    public void toggleSensorWindow() {
        overview.setVisible(!overview.isVisible());
    }
    
    public void toggleEquipmentWindow() {
        equipment.setVisible(!equipment.isVisible());
    }
}
