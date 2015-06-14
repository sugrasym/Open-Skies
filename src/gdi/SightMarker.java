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
public class SightMarker extends AstralWindow {

    private Ship host;
    private SightCanvas canvas;
    private AstralCamera camera;

    public SightMarker(AssetManager assets, Ship host, AstralCamera camera, int width, int height) {
        super(assets, width, height, true);
        this.host = host;
        this.camera = camera;
        init();
    }

    private void init() {
        //setup canvas
        canvas = new SightCanvas();
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
            float speed = host.getAverageCannonSpeed();
            Vector3f del = host.leadTargetLocation(target, speed);
            //get screen coordinates
            Vector3f sLoc = camera.getScreenCoordinates(del);
            //set position
            setX((int) sLoc.getX() - width / 2);
            setY((int) sLoc.getY() - height / 2);
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

    private class SightCanvas extends AstralComponent {

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
                        //pick by range
                        float range = (float) host.getNearWeaponRange();
                        float distance = host.getLocation().distance(host.getTarget().getLocation());
                        if (range < distance) {
                            gfx.setColor(Color.orange);
                        } else {
                            gfx.setColor(Color.blue);
                        }
                        //draw
                        gfx.drawRect(5, 5, width - 10, height - 10);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error rendering sight marker");
            }
        }
    }
}
