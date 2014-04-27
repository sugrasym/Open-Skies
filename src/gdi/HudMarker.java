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
package gdi;

import celestial.Celestial;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
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

/**
 *
 * @author nwiehoff
 */
public class HudMarker extends AstralWindow {

    private Entity target;
    private MarkerCanvas canvas;
    private AstralCamera camera;
    private boolean relevant = true;

    public HudMarker(AssetManager assets, AstralCamera camera, Entity target, int width, int height) {
        super(assets, width, height, true);
        this.target = target;
        this.camera = camera;
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
        if (visible) {
            //lock to position of celestial
            lockToTarget();
        }
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
        //update position
        setX((int) sLoc.x - width / 2);
        setY((int) sLoc.y - height / 2);
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
                        Ship player = tmp.getCurrentSystem().getUniverse().getPlayerShip();
                        if (tmp == player.getTarget()) {
                            gfx.setColor(Color.YELLOW);
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
        }
    }
}
