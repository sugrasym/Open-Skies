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
 * Window for displaying the status of a ship's equipment.
 */
package gdi;

import cargo.Hardpoint;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import entity.Entity;
import gdi.component.AstralBar;
import gdi.component.AstralComponent;
import gdi.component.AstralLabel;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class EquipmentWindow extends AstralWindow {

    AstralList weaponList = new AstralList(this, 1, 1);
    AstralList targetList = new AstralList(this, 1, 1);
    OverviewCanvas overview = new OverviewCanvas();
    AstralLabel targetName = new AstralLabel(1, 1);
    AstralLabel targetType = new AstralLabel(1, 1);
    AstralLabel targetFaction = new AstralLabel(1, 1);
    AstralLabel targetDistance = new AstralLabel(1, 1);
    AstralLabel comm = new AstralLabel(1, 1);
    AstralBar targetShield = new AstralBar(1, 1);
    AstralBar targetHull = new AstralBar(1, 1);
    Font targetFont = new Font("Monospaced", Font.PLAIN, 10);
    private Ship ship;

    public EquipmentWindow(AssetManager assets) {
        super(assets, 300, 300, false);
        generate();
    }

    public void update(Ship ship) {
        if (ship != null) {
            this.ship = ship;
            //clear list
            weaponList.clearList();
            targetList.clearList();
            for (int a = 0; a < ship.getHardpoints().size(); a++) {
                weaponList.addToList(ship.getHardpoints().get(a));
            }
            ArrayList<Entity> celestials = ship.getCurrentSystem().getCelestials();
            for (int a = 0; a < celestials.size(); a++) {
                if (celestials.get(a) instanceof Ship) {
                    /*if (!(celestials.get(a) instanceof Projectile)) {*/
                    Ship tmp = (Ship) celestials.get(a);
                    if (ship.getPhysicsLocation().distance(tmp.getPhysicsLocation())
                            < ship.getSensor() && ship != tmp) {
                        targetList.addToList(tmp);
                    }
                    /*}*/
                }
            }
            //update targeting components
            Ship tmp = ship.getTarget();
            if (tmp != null) {
                targetName.setText(tmp.getName());
                targetType.setText(tmp.getType().getValue("type"));
                targetFaction.setText("[" + ship.getStandingsToMe(tmp) + "] " + tmp.getFaction().getName());
                targetDistance.setText((int) ship.getPhysicsLocation().distance(ship.getTarget().getPhysicsLocation()) + "");
                if (tmp instanceof Station) {
                    Station st = (Station) tmp;
                    if (ship.isDocked()) {
                        comm.setText("Press F1 To Launch");
                    } else {
                        if (ship.getPort() != null) {
                            comm.setText("Request Accepted");
                        } else {
                            if (st.canDock(ship)) {
                                comm.setText("Press F1 For Docking");
                            } else {
                                comm.setText("Docking Denied");
                            }
                        }
                    }
                } else if (tmp instanceof Ship && !ship.isDocked()) {
                    comm.setText("Press H To Hail");
                } else {
                    comm.setText("");
                }
                targetShield.setPercentage(ship.getTarget().getShield() / ship.getTarget().getMaxShield());
                targetHull.setPercentage(ship.getTarget().getHull() / ship.getTarget().getMaxHull());
            } else {
                targetName.setText("NO AIM");
                targetType.setText("");
                targetFaction.setText("");
                targetDistance.setText("");
                comm.setText("");
                targetShield.setPercentage(0);
                targetHull.setPercentage(0);
            }
        } else {
            //nothing to use for update
        }
    }

    private void generate() {
        backColor = windowBlue;
        setVisible(true);
        //setup the list
        weaponList.setX(0);
        weaponList.setY(0);
        weaponList.setWidth(getWidth());
        weaponList.setHeight((getHeight() / 4) - 1);
        weaponList.setVisible(true);
        //setup the list
        targetList.setX(0);
        targetList.setY(getHeight() / 4);
        targetList.setWidth(getWidth());
        targetList.setHeight((getHeight() / 4));
        targetList.setVisible(true);
        //setup the target name label
        targetName.setName("target");
        targetName.setText("TARGET");
        targetName.setX(0);
        targetName.setY((getHeight() / 2) + 1);
        targetName.setFont(targetFont);
        targetName.setWidth(getWidth() / 2);
        targetName.setHeight(targetFont.getSize() + 1);
        targetName.setVisible(true);
        //setup the target type label
        targetType.setName("type");
        targetType.setText("TYPE");
        targetType.setX(0);
        targetType.setY((getHeight() / 2) + 1 + targetFont.getSize());
        targetType.setFont(targetFont);
        targetType.setWidth(getWidth() / 2);
        targetType.setHeight(targetFont.getSize() + 1);
        targetType.setVisible(true);
        //setup the target faction label
        targetFaction.setName("faction");
        targetFaction.setText("FACTION");
        targetFaction.setX(0);
        targetFaction.setY((getHeight() / 2) + 1 + 2 * targetFont.getSize());
        targetFaction.setFont(targetFont);
        targetFaction.setWidth(getWidth() / 2);
        targetFaction.setHeight(targetFont.getSize() + 1);
        targetFaction.setVisible(true);
        //setup the target distance
        targetDistance.setName("distance");
        targetDistance.setText("DISTANCE");
        targetDistance.setX(0);
        targetDistance.setY((getHeight() / 2) + 1 + 3 * targetFont.getSize());
        targetDistance.setFont(targetFont);
        targetDistance.setWidth(getWidth() / 2);
        targetDistance.setHeight(targetFont.getSize() + 1);
        targetDistance.setVisible(true);
        //setup the target distance
        comm.setName("dock");
        comm.setText("DOCK");
        comm.setX(0);
        comm.setY((getHeight() / 2) + 1 + 5 * targetFont.getSize());
        comm.setFont(targetFont);
        comm.setWidth(getWidth() / 2);
        comm.setHeight((targetFont.getSize() + 1) * 2);
        comm.setVisible(true);
        //setup the shield bar
        targetShield.setX(0);
        targetShield.setName("shield");
        targetShield.setY(getHeight() - 2 * targetFont.getSize());
        targetShield.setWidth(getWidth() / 2);
        targetShield.setHeight(targetFont.getSize() + 1);
        targetShield.setBarColor(Color.GREEN);
        targetShield.setVisible(true);
        //setup the hull
        targetHull.setX(0);
        targetHull.setName("shield");
        targetHull.setY(getHeight() - 1 * targetFont.getSize());
        targetHull.setWidth(getWidth() / 2);
        targetHull.setHeight(targetFont.getSize() + 1);
        targetHull.setBarColor(Color.RED);
        targetHull.setVisible(true);
        //setup the overview canvas
        overview.setName("overview");
        overview.setX(getWidth() / 2);
        overview.setY(getHeight() / 2 + 1);
        overview.setWidth(getWidth() / 2);
        overview.setHeight(getHeight() / 2 - 1);
        overview.setVisible(true);
        //pack
        addComponent(weaponList);
        addComponent(targetList);
        addComponent(targetName);
        addComponent(targetType);
        addComponent(targetFaction);
        addComponent(targetDistance);
        addComponent(comm);
        addComponent(targetShield);
        addComponent(targetHull);
        addComponent(overview);
    }

    private class OverviewCanvas extends AstralComponent {

        Font radarFont = new Font("Monospaced", Font.PLAIN, 9);

        public OverviewCanvas() {
            super(1, 1);
        }

        @Override
        public void render(Graphics f) {
            BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            if (ship != null) {
                //get graphics
                Graphics2D gfx = (Graphics2D) frame.getGraphics();
                //draw stuff
                if (ship.getTarget() != null) {
                    fillRadar(gfx);
                }
                //draw circle
                gfx.setColor(Color.BLUE);
                gfx.drawOval(0, 0, getWidth(), getHeight());
                //draw border
                gfx.setColor(whiteForeground);
                gfx.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            f.drawImage(frame, getX(), getY(), getWidth(), getHeight(), null);
        }

        private void fillRadar(Graphics2D gfx) {
            //get sensor strength
            double range = ship.getSensor();
            //get coordinates
            double ex = ship.getTarget().getPhysicsLocation().getX();
            double ey = ship.getTarget().getPhysicsLocation().getZ();
            //adjust for player loc
            ex -= ship.getPhysicsLocation().getX();
            ey -= ship.getPhysicsLocation().getZ();
            //calculate distance
            double dist = magnitude(ex, ey);
            if (dist <= range) {
                //adjust for size
                ex /= range;
                ey /= range;
                ex *= getWidth() / 2;
                ey *= getHeight() / 2;
                /*
                 * Draw the ship and its vector lines
                 */
                drawShipOnRadar(gfx, ex, ey);
                drawVectorLines(gfx, ex, ey);
            }
        }

        protected void drawShipOnRadar(Graphics2D gfx, double ex, double ey) {
            gfx.setColor(Color.WHITE);
            gfx.fillRect((int) ex + (getWidth() / 2) - 2, (int) ey + (getHeight() / 2) - 2, 4, 4);
            gfx.setFont(radarFont);
        }

        protected void drawVectorLines(Graphics2D gfx, double ex, double ey) {
            try {
                /*
                 * Shows the vectors of the target ship, useful in an intercept or
                 * a fight.
                 */
                //draw the range of your craft's selected equipment
                Hardpoint tmp = (Hardpoint) weaponList.getItemAtIndex(weaponList.getIndex());
                if (tmp != null) {
                    double range = tmp.getMounted().getRange();
                    range /= ship.getSensor();
                    gfx.setColor(Color.RED);
                    int w = (int) (getWidth() * range);
                    int h = (int) (getHeight() * range);
                    gfx.drawOval((getWidth() / 2) - w / 2, (getHeight() / 2) - h / 2, w, h);
                }
                //horizontal line of sight of your craft
                gfx.setColor(Color.CYAN);
                Vector3f pointer = ship.getRotationAxis();
                double dTheta = Math.atan2(pointer.z, pointer.x) - FastMath.PI;
                double dpx = Math.cos(dTheta) * getWidth() / 2;
                double dpy = Math.sin(dTheta) * getHeight() / 2;
                gfx.drawLine(getWidth() / 2, (getHeight() / 2), (int) dpx + (getWidth() / 2), (int) dpy + (getHeight() / 2));
                //line between your craft and the target
                gfx.setColor(Color.PINK);
                gfx.drawLine(getWidth() / 2, getHeight() / 2, (int) ex + (getWidth() / 2), (int) ey + (getHeight() / 2));
            } catch (Exception e) {
                System.out.println("Error drawing vector lines in overview window");
            }
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        //get the module and toggle its enabled status
        if (weaponList.isFocused()) {
            /*Hardpoint tmp = (Hardpoint) weaponList.getItemAtIndex(weaponList.getIndex());
             tmp.setEnabled(!tmp.isEnabled());*/
        }
        if (targetList.isFocused()) {
            ship.setTarget((Ship) targetList.getItemAtIndex(targetList.getIndex()));
        }
    }

    public void scrollUp() {
        weaponList.scrollUp();
    }

    public void scrollDown() {
        weaponList.scrollDown();
    }

    private synchronized double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }
}
