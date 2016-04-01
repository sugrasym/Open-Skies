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
 * This is a special window that can be attatched to a celestial. It will display
 * the status of this celestial and track its position on the HUD. It is used to
 * create IFF displays.
 */
package gdi;

import celestial.Celestial;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import engine.AstralCamera;
import entity.Entity;
import entity.Entity.State;
import gdi.component.AstralComponent;
import gdi.component.AstralWindow;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import lib.Faction;

/**
 *
 * @author nwiehoff
 */
public class HudMarker extends AstralWindow {

    private Entity target;
    private MarkerCanvas canvas;
    private final Ship playerShip;
    private final AstralCamera camera;
    private boolean relevant = true;

    public HudMarker(AssetManager assets, AstralCamera cam, Ship playerShip, Entity target, int width, int height) {
        super(assets, width, height, true);
        this.target = target;
        this.camera = cam;
        this.playerShip = playerShip;
        init();
    }

    private void init() {
        //setup canvas
        canvas = new MarkerCanvas();
        canvas.setVisible(true);
        canvas.setX(0);
        canvas.setY(0);
        canvas.setWidth(width);
        canvas.setHeight(height);
        //store canvas
        addComponent(canvas);
        //save colors
        setBackColor(transparent);
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
        //make sure we have a target
        if (target != null) {
            //make sure the entity is alive
            if (target.getState() != State.ALIVE) {
                relevant = false;
            }
            //if it is a celestial there are more tests to do
            Ship test = playerShip;
            if (target instanceof Ship) {
                Ship ship = (Ship) target;
                Vector3f tLoc = ship.getLocation();
                if (test.getLocation().distance(tLoc) > test.getSensor()) {
                    relevant = false;
                }
                if (test.getCurrentSystem() != ship.getCurrentSystem()) {
                    relevant = false;
                }
            }
            //make sure it is in the current system as the player
        } else {
            relevant = false;
        }
    }

    private void lockToTarget() {
        //get target position
        Vector3f tLoc = target.getLocation();
        //get target screen position
        Vector3f sLoc = camera.getScreenCoordinates(tLoc);
        //make sure target is in sensor range
        Ship test = playerShip;
        if (test.getLocation().distance(tLoc) <= test.getSensor()) {
            //calculate dot product between camera angle and camera-relative target position
            Vector3f pos = tLoc.subtract(camera.getLocation());
            float dot = camera.getDirection().dot(pos);
            //make sure that the camera is behind the target
            if (dot > 0) {
                setVisible(true);
                //update position
                setX((int) sLoc.x - width / 2);
                setY((int) sLoc.y - height / 2);
            } else {
                setVisible(false);
            }
        } else {
            //hide the marker
            setVisible(false);
        }
    }

    public boolean isRelevant() {
        return relevant;
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    private class MarkerCanvas extends AstralComponent {

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
                    if (target instanceof Celestial) {
                        if (target instanceof Ship) {
                            //draw marker
                            gfx.setStroke(new BasicStroke(3));
                            Ship tmp = (Ship) target;
                            Ship player = playerShip;
                            float standing = player.getStandingsToMe(tmp);
                            if (tmp == player.getTarget()) {
                                gfx.setColor(Color.YELLOW);
                            } else if (tmp.getFaction().getName().equals(Faction.PLAYER)) {
                                gfx.setColor(Color.MAGENTA);
                            } else if (standing <= Faction.HOSTILE_STANDING) {
                                gfx.setColor(Color.RED);
                            } else if (standing >= Faction.FRIENDLY_STANDING) {
                                gfx.setColor(Color.GREEN);
                            } else {
                                gfx.setColor(Color.WHITE);
                            }
                            gfx.drawOval(5, 5, width - 10, height - 10);
                            //draw health bars
                            if (tmp == player.getTarget()) {
                                //I only want health bars for player's target
                                float shieldPercent = tmp.getShield() / tmp.getMaxShield();
                                float hullPercent = tmp.getHull() / tmp.getMaxHull();
                                //draw hull
                                gfx.setColor(Color.RED);
                                gfx.fillRect(0, 0, (int) (width * hullPercent), 3);
                                //draw shield
                                gfx.setColor(Color.GREEN);
                                gfx.fillRect(0, 0, (int) (width * shieldPercent), 3);
                            }
                        }
                    } else {
                        gfx.setStroke(new BasicStroke(3));
                        gfx.setColor(Color.WHITE);
                        gfx.drawRect(0, 0, width - 1, height - 1);
                    }
                }
            } catch (Exception e) {
                System.out.println("An error occured updating a marker");
            }
        }
    }
}
