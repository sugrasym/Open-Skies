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
        super(assets, 300, 10, false);
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
