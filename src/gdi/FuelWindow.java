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
 * Shows how much fuel you've got left.
 */
package gdi;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import gdi.component.AstralBar;
import gdi.component.AstralWindow;
import java.awt.Color;

/**
 *
 * @author nwiehoff
 */
public class FuelWindow extends AstralWindow {
    AstralBar fuelBar = new AstralBar(300, 10);
    public FuelWindow(AssetManager assets) {
        super(assets, 300, 10);
        create();
    }
    
    
    private void create() {
        //color
        backColor = new Color(25, 25, 25, 200);
        setVisible(true);
        //create shield bar
        fuelBar.setName("shieldbar");
        fuelBar.setX(0);
        fuelBar.setY(0);
        fuelBar.setWidth(300);
        fuelBar.setHeight(10);
        fuelBar.setVisible(true);
        fuelBar.setBarColor(Color.BLUE);
        //pack
        addComponent(fuelBar);
    }
    
    public void updateFuel(Ship ship) {
        fuelBar.setPercentage(ship.getFuel()/ship.getMaxFuel());
    }
}
