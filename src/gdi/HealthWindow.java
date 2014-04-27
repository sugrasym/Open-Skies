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
 * This displays ship health using two progress bars.
 */
package gdi;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import gdi.component.AstralBar;
import gdi.component.AstralWindow;
import java.awt.Color;

/**
 *
 * @author Nathan Wiehoff
 */
public class HealthWindow extends AstralWindow {

    AstralBar shieldBar = new AstralBar(140, 10);
    AstralBar hullBar = new AstralBar(140, 10);

    public HealthWindow(AssetManager assets) {
        super(assets, 300, 10, false);
        create();
    }

    private void create() {
        //color
        backColor = new Color(25, 25, 25, 200);
        setVisible(true);
        //create shield bar
        shieldBar.setName("shieldbar");
        shieldBar.setX(0);
        shieldBar.setY(0);
        shieldBar.setVisible(true);
        shieldBar.setBarColor(Color.GREEN);
        //create hull bar
        hullBar.setName("hullbar");
        hullBar.setX(160);
        hullBar.setY(0);
        hullBar.setVisible(true);
        hullBar.setBarColor(Color.RED);
        //pack
        addComponent(shieldBar);
        addComponent(hullBar);
    }

    public void updateHealth(Ship ship) {
        float shieldPercent = ship.getShield() / ship.getMaxShield();
        float hullPercent = ship.getHull() / ship.getMaxHull();
        shieldBar.setPercentage(shieldPercent);
        hullBar.setPercentage(hullPercent);
    }
}
