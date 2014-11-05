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
 * This is a special window that is used as a lead indicator.
 * 
 * It goes by your minimum weapon range.
 */
package gdi;

import celestial.Celestial;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import engine.AstralCamera;
import gdi.component.AstralComponent;
import gdi.component.AstralWindow;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 *
 * @author nwiehoff
 */
public class VelocityMarker extends AstralWindow {

    private Ship host;
    private VelocityCanvas canvas;
    private AstralCamera camera;

    private int screenWidth;
    private int screenHeight;

    public VelocityMarker(AssetManager assets, Ship host, AstralCamera camera,
            int width, int height, int screenWidth, int screenHeight) {
        super(assets, width, height, true);
        this.host = host;
        this.camera = camera;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        init();
    }

    private void init() {
        //setup canvas
        canvas = new VelocityCanvas();
        canvas.setVisible(true);
        canvas.setX(0);
        canvas.setY(0);
        canvas.setWidth(width);
        canvas.setHeight(height);
        //store canvas
        addComponent(canvas);
        //save colors
        setBackColor(transparent);
        //make visible
        setVisible(true);
    }

    @Override
    public void periodicUpdate() {
        super.periodicUpdate();
        lockToSight();
    }

    public Ship getHost() {
        return host;
    }

    public void setHost(Ship host) {
        this.host = host;
    }

    private void lockToSight() {
        Ship target = host.getTarget();
        if (target != null) {
            setVisible(true);
            //get target position
            Vector3f tPos = target.getLocation();
            //get target velocity
            Vector3f tVel = target.getVelocity().subtract(host.getVelocity());
            //add
            Vector3f del = tPos.add(tVel);
            //get screen coordinates
            Vector3f sLoc = camera.getScreenCoordinates(del);
            //set position
            int sx = (int) sLoc.getX() - width / 2;
            int sy = (int) sLoc.getY() - height / 2;

            //lock to inside screen
            if (sx > screenWidth) {
                sx = screenWidth - width;
            }
            if (sx < 0) {
                sx = 0;
            }
            if (sy > screenHeight) {
                sy = screenHeight - height;
            }
            if (sy < 0) {
                sy = 0;
            }

            setX(sx);
            setY(sy);
        } else {
            setVisible(false);
        }
    }

    public AstralCamera getCamera() {
        return camera;
    }

    public void setCamera(AstralCamera camera) {
        this.camera = camera;
    }

    public void update(Ship host, AstralCamera camera) {
        this.host = host;
        this.camera = camera;
    }

    private class VelocityCanvas extends AstralComponent {

        @Override
        public void render(Graphics f) {
            try {
                if (isVisible()) {
                    //setup graphics
                    Graphics2D gfx = (Graphics2D) f;
                    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //redo backdrop
                    gfx.setComposite(AlphaComposite.Clear);
                    gfx.fillRect(0, 0, width, height);
                    gfx.setComposite(AlphaComposite.Src);
                    //render marker
                    if (host.getTarget() != null) {
                        //draw marker
                        gfx.setStroke(new BasicStroke(2));
                        gfx.setColor(Color.cyan);
                        //draw
                        gfx.drawOval(5, 5, width - 10, height - 10);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error rendering sight marker");
            }
        }
    }
}
