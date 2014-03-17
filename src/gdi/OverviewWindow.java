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

package gdi;

import celestial.Planet;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import entity.Entity;
import gdi.component.AstralComponent;
import gdi.component.AstralLabel;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class OverviewWindow extends AstralWindow {

    AstralLabel rangeLabel = new AstralLabel(125, 25);
    AstralLabel modeLabel = new AstralLabel(125, 25);
    AstralLabel stationLabel = new AstralLabel(125, 25);
    AstralLabel shipLabel = new AstralLabel(125, 25);
    AstralLabel velLabel = new AstralLabel(200, 25);
    AstralLabel throttleLabel = new AstralLabel(125, 25);
    AstralLabel engineLabel = new AstralLabel(200, 25);
    OverviewCanvas radar = new OverviewCanvas();
    private Ship sensorShip;
    public Mode mode = Mode.ACTIVE;
    protected boolean showShipNames = true;
    protected boolean showStationNames = true;
    protected Color planetGrey = new Color(15, 15, 15, 200);

    public enum Mode {

        SHORT_ACTIVE, //0.5x sensor range. everything visible including secret stuff
        ACTIVE, //1x sensor range. ships, planets, stations
        PASSIVE, //10x sensor range. planets, stations
        DEEP_FIELD, //100x sensor range. planets, without names
    }

    public OverviewWindow(AssetManager assets) {
        super(assets, 300, 300);
        generate();
    }

    public void updateOverview(Ship sensorShip) {
        this.sensorShip = sensorShip;
        shipLabel.setVisible(showShipNames);
        stationLabel.setVisible(showStationNames);
        velLabel.setText("REL SPEED: " + roundTwoDecimal(sensorShip.getLinearVelocity().length()) + "u/s");
        throttleLabel.setText("THRUST: " + (int) (sensorShip.getThrottle() * 100) + "%");
        //update engine label
        if(sensorShip.getEngine() == Ship.EngineMode.NORMAL) {
            engineLabel.setText("NORMAL");
        } else if(sensorShip.getEngine() == Ship.EngineMode.NEWTON) {
            engineLabel.setText("NEWTON");
        } else {
            engineLabel.setText("");
        }
    }

    public void incrementMode() {
        if (mode == Mode.SHORT_ACTIVE) {
            mode = Mode.ACTIVE;
        } else if (mode == Mode.ACTIVE) {
            mode = Mode.PASSIVE;
        } else if (mode == Mode.PASSIVE) {
            mode = Mode.DEEP_FIELD;
        } else {
            mode = mode.SHORT_ACTIVE;
        }
    }

    private class OverviewCanvas extends AstralComponent {

        Font radarFont = new Font("Monospaced", Font.PLAIN, 9);

        public OverviewCanvas() {
            super(300, 300);
        }

        @Override
        public void render(Graphics f) {
            BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            if (sensorShip != null) {
                //store range
                double scale = 0;
                if (mode == Mode.SHORT_ACTIVE) {
                    scale = 0.5;
                    modeLabel.setText("SHORT ACTIVE");
                } else if (mode == Mode.ACTIVE) {
                    scale = 1.0;
                    modeLabel.setText("ACTIVE");
                } else if (mode == Mode.PASSIVE) {
                    scale = 10.0;
                    modeLabel.setText("PASSIVE");
                } else if (mode == Mode.DEEP_FIELD) {
                    scale = 100.0;
                    modeLabel.setText("DEEP FIELD");
                }
                rangeLabel.setText("RANGE: " + sensorShip.getSensor() * scale);
                //get graphics
                Graphics2D gfx = (Graphics2D) frame.getGraphics();
                //draw stuff
                fillRadar(gfx);
                //draw circle
                gfx.setColor(Color.BLUE);
                gfx.drawOval(0, 0, getWidth(), getHeight());
            }
            f.drawImage(frame, getX(), getY(), getWidth(), getHeight(), null);
        }

        private void fillRadar(Graphics2D gfx) {
            //get entity list
            ArrayList<Entity> entities = sensorShip.getCurrentSystem().getCelestials();
            drawVectorLines(gfx);
            for (int a = 0; a < entities.size(); a++) {
                //get sensor strength
                double range = sensorShip.getSensor();
                //scale range
                if (mode == Mode.SHORT_ACTIVE) {
                    range *= 0.5;
                } else if (mode == Mode.ACTIVE) {
                    range *= 1.0;
                } else if (mode == Mode.PASSIVE) {
                    range *= 10.0;
                } else if (mode == Mode.DEEP_FIELD) {
                    range *= 100.0;
                }
                //get coordinates
                Vector3f loc = entities.get(a).getPhysicsLocation();
                double ex = loc.x;
                double ez = loc.z;
                //adjust for player loc
                ex -= sensorShip.getPhysicsLocation().getX();
                ez -= sensorShip.getPhysicsLocation().getZ();
                //calculate distance
                double dist = magnitude(ex, ez);
                if (dist <= range || entities.get(a) instanceof Planet) {
                    //adjust for size
                    ex /= range;
                    ez /= range;
                    ex *= getWidth() / 2;
                    ez *= getHeight() / 2;
                    /*
                     * Does the final drawing based on what exactly the object is
                     */
                    if (entities.get(a) instanceof Planet) {
                        doPlanet(entities, a, range, gfx, ex, ez);
                    } else if (entities.get(a) == sensorShip) {
                        doSensorShip(gfx, ex, ez);
                    } else if (entities.get(a) instanceof Ship) {
                        doShip(gfx, ex, ez, entities, a);
                    }
                }
            }
        }

        protected void doPlanet(ArrayList<Entity> entities, int a, double range, Graphics2D gfx, double ex, double ey) {
            //get radius
            Planet pl = (Planet) entities.get(a);
            double diam = pl.getRadius() * 2;
            diam /= (range);
            diam *= getWidth() / 2;
            gfx.setColor(planetGrey);
            gfx.fillOval((int) ex + (getWidth() / 2) - (int) (diam / 2), (int) ey + (getHeight() / 2) - (int) (diam / 2), (int) diam, (int) diam);
            gfx.setColor(Color.DARK_GRAY);
            gfx.drawOval((int) ex + (getWidth() / 2) - (int) (diam / 2), (int) ey + (getHeight() / 2) - (int) (diam / 2), (int) diam, (int) diam);
            gfx.setColor(Color.pink);
            gfx.setFont(radarFont);
            if (mode != Mode.DEEP_FIELD) {
                gfx.drawString(pl.getName(), (int) (ex + diam / 2) + (getWidth() / 2) - 1, (int) (ey + diam / 2) + (getHeight() / 2) - 1);
            } else {
                gfx.drawString("NO AIM", (int) (ex + diam / 2) + (getWidth() / 2) - 1, (int) (ey + diam / 2) + (getHeight() / 2) - 1);
            }
        }

        protected void doSensorShip(Graphics2D gfx, double ex, double ey) {
            gfx.setColor(amber);
            gfx.drawRect((int) ex + (getWidth() / 2) - 2, (int) ey + (getHeight() / 2) - 2, 4, 4);
        }

        protected void doShip(Graphics2D gfx, double ex, double ey, ArrayList<Entity> entities, int a) {
            if (mode == Mode.SHORT_ACTIVE || mode == Mode.ACTIVE) {
                drawShipOnRadar(gfx, ex, ey, entities, a);
            }
        }

        protected void drawShipOnRadar(Graphics2D gfx, double ex, double ey, ArrayList<Entity> entities, int a) {
            gfx.setColor(Color.WHITE);
            gfx.drawRect((int) ex + (getWidth() / 2) - 1, (int) ey + (getHeight() / 2) - 1, 2, 2);
            gfx.setFont(radarFont);
            if (showShipNames) {
                gfx.setFont(radarFont);
                gfx.drawString(entities.get(a).getName(), (int)ex + (getWidth() / 2) - 4, (int) ey + (getHeight() / 2) - 4);
            }
        }

        protected void drawVectorLines(Graphics2D gfx) {
            /*
             * IDEAS
             * 
             * - show the point on the map where you could be fully stopped at your current velocity
             * - allow you to "lock" a vector in red and keep it there for your own uses.
             */
            /*
             * These two lines represent the vector of your velocity and the vector
             * of your direction. They will simplify navigation in 2D space.
             */
            gfx.setColor(Color.CYAN);
            Vector3f pointer = sensorShip.getRotationAxis();
            double dTheta = Math.atan2(pointer.z, pointer.x) - FastMath.PI;
            double dpx = Math.cos(dTheta) * getWidth() / 2;
            double dpy = Math.sin(dTheta) * getHeight() / 2;
            gfx.drawLine(getWidth() / 2, (getHeight() / 2), (int) dpx + (getWidth() / 2), (int) dpy + (getHeight() / 2));
            //calculate direction vector
            gfx.setColor(Color.yellow);
            //calculate velocity vector
            Vector3f vel = sensorShip.getLinearVelocity();
            double vTheta = Math.atan2(vel.z, vel.x);
            double vpx = Math.cos(vTheta) * getWidth() / 2;
            double vpy = Math.sin(vTheta) * getHeight() / 2;
            if (!(vel.z == 0 && vel.x == 0)) {
                gfx.drawLine(getWidth() / 2, (getHeight() / 2), (int) vpx + (getWidth() / 2), (int) vpy + (getHeight() / 2));
            }
        }
    }

    private void generate() {
        backColor = windowGrey;
        //size this window
        setWidth(300);
        setHeight(300);
        setVisible(true);
        //setup range label
        rangeLabel.setText("range");
        rangeLabel.setName("range");
        rangeLabel.setX(0);
        rangeLabel.setY(0);
        rangeLabel.setVisible(true);
        //setup mode label
        modeLabel.setText("NO MODE");
        modeLabel.setName("mode");
        modeLabel.setX(0);
        modeLabel.setY(15);
        modeLabel.setVisible(true);
        //setup station label
        stationLabel.setText("STATIONS");
        stationLabel.setName("station");
        stationLabel.setX(getWidth() - 100);
        stationLabel.setY(0);
        stationLabel.setVisible(true);
        //setup ship label
        shipLabel.setText("SHIPS");
        shipLabel.setName("ship");
        shipLabel.setX(getWidth() - 100);
        shipLabel.setY(15);
        shipLabel.setVisible(true);
        //setup radar
        radar.setName("radar");
        radar.setVisible(true);
        radar.setWidth(getWidth());
        radar.setHeight(getHeight());
        radar.setX(0);
        radar.setY(0);
        //setup vel label
        velLabel.setText("REL SPEED");
        velLabel.setName("REL SPEED");
        velLabel.setX(0);
        velLabel.setY(getHeight() - 15);
        velLabel.setVisible(true);
        //setup throttle label
        throttleLabel.setText("THROTTLE");
        throttleLabel.setName("THROTTLE");
        throttleLabel.setX(getWidth() - 100);
        throttleLabel.setY(getHeight() - 15);
        throttleLabel.setVisible(true);
        //setup engine label
        engineLabel.setText("ENIGNE");
        engineLabel.setName("ENGINE");
        engineLabel.setX(0);
        engineLabel.setY(getHeight() - 30);
        engineLabel.setVisible(true);
        //pack
        addComponent(radar);
        addComponent(rangeLabel);
        addComponent(modeLabel);
        addComponent(shipLabel);
        addComponent(stationLabel);
        addComponent(velLabel);
        addComponent(throttleLabel);
        addComponent(engineLabel);
    }

    private synchronized double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private double roundTwoDecimal(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.parseDouble(twoDForm.format(d));
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isShowShipNames() {
        return showShipNames;
    }

    public void setShowShipNames(boolean showShipNames) {
        this.showShipNames = showShipNames;
    }

    public boolean isShowStationNames() {
        return showStationNames;
    }

    public void setShowStationNames(boolean showStationNames) {
        this.showStationNames = showStationNames;
    }

    @Override
    public void handleKeyReleasedEvent(String ke) {
        /*
         * navmap keys
         */ if (ke.matches("KEY_END")) {
            incrementMode();
        } else if (ke.matches("KEY_PGDN")) {
            setShowShipNames(!isShowShipNames());
        } else if (ke.matches("KEY_PGUP")) {
            setShowStationNames(!isShowStationNames());
        }
    }
}
