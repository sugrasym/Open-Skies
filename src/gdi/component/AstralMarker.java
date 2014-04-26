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
 * This is a special window that can be attatched to a celestial. It will display
 * the status of this celestial and track its position on the HUD. It is used to
 * create IFF displays.
 */
package gdi.component;

import celestial.Celestial;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import engine.AstralCamera;
import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author nwiehoff
 */
public class AstralMarker extends AstralWindow {
    private Celestial target;
    private MarkerCanvas canvas;
    private AstralCamera camera;
    private boolean relevant = true;
    public AstralMarker(AssetManager assets, AstralCamera camera, Celestial target, int width, int height) {
        super(assets, width, height);
        this.target = target;
        this.camera = camera;
        init();
    }
    
    private void init() {
        //setup canvas
        canvas = new MarkerCanvas();
        //store canvas
        addComponent(canvas);
    }
    
    @Override
    public void periodicUpdate() {
        super.periodicUpdate();
        //determine if IFF is relevant
        determineRelevance();
        //lock to position of celestial
        lockToTarget();
    }
    
    private void determineRelevance() {
        /*
         * This method determines whether or not this control is still applicable
         * or if it should be removed from the GUI.
         */
        //TODO
    }
    
    private void lockToTarget() {
        //get target position
        Vector3f tLoc = target.getLocation();
        //get target screen position
        Vector3f sLoc = camera.getScreenCoordinates(tLoc);
        //update position
        setX((int) sLoc.x);
        setY((int) sLoc.y);
    }
    
    public boolean isRelevant() {
        return relevant;
    }
    
    private class MarkerCanvas extends AstralComponent
    {
        @Override
        public void render(Graphics f) {
            if(isVisible()) {
                f.setColor(Color.RED);
                f.fillRect(0, 0, width, height);
            }
        }
    }
}
