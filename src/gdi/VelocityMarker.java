/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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
 * This is a special window that is used as a lead indicator.
 * 
 * It goes by your minimum weapon range.
 */
package gdi;

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

    private final int screenWidth;
    private final int screenHeight;

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
