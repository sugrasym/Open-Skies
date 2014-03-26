/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Allows the management of a player's property.
 * Nathan Wiehoff
 */
package gdi;

import cargo.Hardpoint;
import cargo.Item;
import celestial.Celestial;
import celestial.Ship.Ship;
import celestial.Ship.Ship.Autopilot;
import celestial.Ship.Ship.Behavior;
import celestial.Ship.Station;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import entity.Entity;
import entity.Entity.State;
import gdi.component.AstralInput;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import universe.SolarSystem;

public class PropertyWindow extends AstralWindow {

    private enum Mode {

        NONE, //idle
        WAITING_FOR_STATION, //waiting for a station to dock at
        WAITING_FOR_CREDITS, //waiting for credits to be specified
        WAITING_FOR_NAME, //waiting for a new name
        WAITING_FOR_ATTACK, //waiting for a ship to attack
        WAITING_FOR_CELESTIAL, //waiting for a celestial to fly to
        WAITING_FOR_CELESTIAL_RANGE, //waiting for range to fly to celestial to
        WAITING_FOR_FOLLOW, //waiting for a ship to follow
        WAITING_FOR_FOLLOW_RANGE, //waiting for a range to follow at
        WAITING_FOR_TRADE, //waiting for trading window input
        WAITING_FOR_CARGO, //waiting for cargo window input
        WAITING_FOR_JUMP, //waiting for a target system to jump to
        WAITING_FOR_BASE, //waiting for a home base to assign
    };
    private Mode mode = Mode.NONE;
    public static final String CMD_SWITCH = "Switch Ship";
    public static final String CMD_PATROL = "Start Patrol";
    public static final String CMD_TRADE = "Start Local Trading";
    public static final String CMD_UTRADE = "Start Wide Trading";
    public static final String CMD_NONE = "End Program";
    public static final String CMD_MOVEFUNDS = "Credit Transfer";
    public static final String CMD_RENAME = "Rename";
    public static final String CMD_UNDOCK = "Undock";
    public static final String CMD_DOCK = "Dock";
    public static final String CMD_FLYTO = "Move To Position";
    public static final String CMD_FOLLOW = "Follow";
    public static final String CMD_ATTACK = "Attack";
    public static final String CMD_DESTRUCT = "Self Destruct";
    public static final String CMD_ALLSTOP = "All Stop";
    public static final String CMD_TRADEWITH = "Trade With Station";
    public static final String CMD_REMOTECARGO = "Manage Cargo";
    public static final String CMD_JUMP = "Jump";
    public static final String CMD_SETHOME = "Set Homebase";
    public static final String CMD_CLEARHOME = "Clear Homebase";
    public static final String CMD_SUPPLYHOME = "Supply Homebase";
    public static final String CMD_REPRESENTHOME = "Represent Homebase";
    AstralInput input = new AstralInput();
    AstralList propertyList = new AstralList(this);
    AstralList infoList = new AstralList(this);
    AstralList optionList = new AstralList(this);
    AstralList inputList = new AstralList(this);
    protected Ship ship;
    //remote operation
    TradeWindow trader;
    CargoWindow cargo;
    protected Ship tmp;

    public PropertyWindow(AssetManager assets) {
        super(assets, 500, 400);
        cargo = new CargoWindow(assets, 460, 360);
        trader = new TradeWindow(assets, 460, 360);
        generate();
    }

    private void generate() {
        backColor = windowGrey;
        //size this window
        width = 500;
        height = 400;
        setVisible(false);
        //setup the cargo list
        propertyList.setX(0);
        propertyList.setY(0);
        propertyList.setWidth(width);
        propertyList.setHeight((height / 2) - 1);
        propertyList.setVisible(true);
        //setup the property list
        infoList.setX(0);
        infoList.setY(height / 2);
        infoList.setWidth((int) (width / 1.5));
        infoList.setHeight((height / 2) - 1);
        infoList.setVisible(true);
        //setup the command list
        optionList.setX((int) (width / 1.5) + 1);
        optionList.setY(height / 2);
        optionList.setWidth((int) (width / 3));
        optionList.setHeight((height / 2) - 1);
        optionList.setVisible(true);
        //setup input method
        input.setName("Input");
        input.setText("|");
        input.setVisible(false);
        input.setWidth(width / 3);
        input.setX((getWidth() / 2) - input.getWidth() / 2);
        input.setHeight((input.getFont().getSize() + 2));
        input.setY((getHeight() / 2) - input.getHeight() / 2);
        //setup input list
        inputList.setWidth((int) (width / 1.5));
        inputList.setX((getWidth() / 2) - inputList.getWidth() / 2);
        inputList.setHeight((height / 2) - 1);
        inputList.setVisible(false);
        inputList.setY((getHeight() / 2) - inputList.getHeight() / 2);
        //setup private trade window
        /*trader.setX(20);
         trader.setY(20);
         trader.setWidth(width - 40);
         trader.setHeight(height - 40);*/
        //setup private cargo window
        cargo.setX(20);
        cargo.setY(20);
        cargo.setWidth(460);
        cargo.setHeight(360);
        cargo.setFlat(true);
        //setup private trader window
        trader.setX(20);
        trader.setY(20);
        trader.setWidth(460);
        trader.setHeight(360);
        trader.setFlat(true);
        //pack
        addComponent(propertyList);
        addComponent(infoList);
        addComponent(optionList);
        //do last
        addComponent(inputList);
        addComponent(input);
        addComponent(trader);
        addComponent(cargo);
    }

    @Override
    public void setVisible(boolean visible) {
        //trader.setVisible(false);
        cargo.setVisible(false);
        super.setVisible(visible);
        mode = Mode.NONE;
        input.setVisible(false);
        inputList.setVisible(false);
    }

    private void showInput(String text) {
        input.setText(text);
        input.setVisible(true);
        input.setFocused(true);
    }

    private void showInputList(ArrayList<Object> options) {
        inputList.clearList();
        for (int a = 0; a < options.size(); a++) {
            inputList.addToList(options.get(a));
        }
        inputList.setVisible(true);
        inputList.setFocused(true);
    }

    private void hideInputList() {
        inputList.setVisible(false);
        inputList.setIndex(0);
    }

    private void behave(Ship selected) {
        if (mode == Mode.NONE) {
            //do nothing
        } else if (mode == Mode.WAITING_FOR_CREDITS) {
            if (input.canReturn()) {
                Ship player = ship.getCurrentSystem().getUniverse().getPlayerShip();
                try {
                    int val = Integer.parseInt(input.getText());
                    if (val > 0) {
                        //we are pushing
                        long source = player.getCash();
                        if (source >= val) {
                            selected.setCash(selected.getCash() + val);
                            player.setCash(player.getCash() - val);
                        } else {
                            //insufficient credits
                        }
                    } else {
                        //we are pulling
                        long source = selected.getCash();
                        long tfr = -val;
                        if (source >= tfr) {
                            player.setCash(player.getCash() + tfr);
                            selected.setCash(selected.getCash() - tfr);
                        } else {
                            //insufficient credits
                        }
                    }
                    //hide it
                    input.setVisible(false);
                    //normal mode
                    mode = Mode.NONE;
                } catch (Exception e) {
                    System.out.println("Malformed input");
                    //normal mode
                    mode = Mode.NONE;
                }
            }
        } else if (mode == Mode.WAITING_FOR_NAME) {
            try {
                if (input.canReturn()) {
                    //get name
                    String nm = input.getText();
                    //push
                    selected.setName(nm);
                    //normal mode
                    mode = Mode.NONE;
                }
            } catch (Exception e) {
                System.out.println("Malformed input");
                //normal mode
                mode = Mode.NONE;
            }
        } else if (mode == Mode.WAITING_FOR_STATION) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof Station) {
                //grab it
                Station pick = (Station) raw;
                //order docking
                selected.cmdAbortDock();
                selected.cmdDock(pick);
                //hide it
                hideInputList();
                //normal mode
                mode = Mode.NONE;
            } else {
                //probably selected some info text
            }
        } else if (mode == Mode.WAITING_FOR_ATTACK) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof Ship) {
                //grab it
                Ship pick = (Ship) raw;
                //order attack
                selected.cmdFightTarget(pick);
                //hide it
                hideInputList();
                //normal mode
                mode = Mode.NONE;
            } else {
                //probably selected some info text
            }
        } else if (mode == Mode.WAITING_FOR_CELESTIAL) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof Celestial) {
                //grab it
                Celestial pick = (Celestial) raw;
                //store celestial
                selected.setFlyToTarget(pick);
                //hide it
                hideInputList();
                //show the next step
                showInput("1000");
                //get range
                mode = Mode.WAITING_FOR_CELESTIAL_RANGE;
            } else {
                //probably selected some info text
            }
        } else if (mode == Mode.WAITING_FOR_CELESTIAL_RANGE) {
            try {
                if (input.canReturn()) {
                    //get input
                    String nm = input.getText();
                    Float range = Float.parseFloat(nm);
                    //start command
                    selected.cmdFlyToCelestial(selected.getFlyToTarget(), range);
                    //normal mode
                    mode = Mode.NONE;
                }
            } catch (Exception e) {
                System.out.println("Malformed input");
                //normal mode
                mode = Mode.NONE;
            }
        } else if (mode == Mode.WAITING_FOR_FOLLOW) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof Ship) {
                //grab it
                Ship pick = (Ship) raw;
                //store celestial
                selected.setFlyToTarget(pick);
                //hide it
                hideInputList();
                //show the next step
                showInput("100");
                //get range
                mode = Mode.WAITING_FOR_FOLLOW_RANGE;
            } else {
                //probably selected some info text
            }
        } else if (mode == Mode.WAITING_FOR_FOLLOW_RANGE) {
            try {
                if (input.canReturn()) {
                    //get input
                    String nm = input.getText();
                    Float range = Float.parseFloat(nm);
                    //start command
                    selected.cmdFollowShip((Ship) selected.getFlyToTarget(), range);
                    //normal mode
                    mode = Mode.NONE;
                }
            } catch (Exception e) {
                System.out.println("Malformed input");
                //normal mode
                mode = Mode.NONE;
            }
        } else if (mode == Mode.WAITING_FOR_TRADE) {
            if (!visible) {
                mode = Mode.NONE;
            } else {
                trader.update(tmp);
            }
        } else if (mode == Mode.WAITING_FOR_CARGO) {
            if (!visible) {
                mode = Mode.NONE;
            } else {
                cargo.update(tmp);
            }
        } else if (mode == Mode.WAITING_FOR_JUMP) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof SolarSystem) {
                //grab it
                SolarSystem pick = (SolarSystem) raw;
                //store celestial
                selected.cmdJump(pick);
                //hide it
                hideInputList();
                //normal mode
                mode = Mode.NONE;
            } else {
                //probably selected some info text
            }
        } else if (mode == Mode.WAITING_FOR_BASE) {
            Object raw = inputList.getItemAtIndex(inputList.getIndex());
            if (raw instanceof Station) {
                //grab it
                Station pick = (Station) raw;
                //set base
                selected.setHomeBase(pick);
                //hide it
                hideInputList();
                //normal mode
                mode = Mode.NONE;
            }
        }
    }

    public void update(Ship ship) {
        setShip(ship);
        propertyList.clearList();
        infoList.clearList();
        optionList.clearList();
        ArrayList<Ship> logicalPropertyList = new ArrayList<>();
        if (ship != null) {
            //get global list
            ArrayList<Entity> prop = ship.getCurrentSystem().getUniverse().getPlayerProperty();
            //make sub lists
            ArrayList<Ship> pShips = new ArrayList<>();
            ArrayList<Ship> pStats = new ArrayList<>();
            for (int a = 0; a < prop.size(); a++) {
                //add to correct sub list
                //if (!(prop.get(a) instanceof Explosion)) {
                if (prop.get(a) instanceof Station) {
                    pStats.add((Station) prop.get(a));
                } else if (prop.get(a) instanceof Ship) {
                    pShips.add((Ship) prop.get(a));
                }
                //}
            }
            //add to logical list
            /*
             * Ships go first.
             * Then stations.
             */
            //sort
            Collections.sort(pShips, new Comparator<Ship>() {
                @Override
                public int compare(Ship left, Ship right) {
                    return left.getName().compareTo(right.getName());
                }
            });
            //add
            for (int a = 0; a < pShips.size(); a++) {
                logicalPropertyList.add(pShips.get(a));
            }
            //sort
            Collections.sort(pStats, new Comparator<Ship>() {
                @Override
                public int compare(Ship left, Ship right) {
                    return left.getName().compareTo(right.getName());
                }
            });
            //add
            for (int a = 0; a < pStats.size(); a++) {
                logicalPropertyList.add(pStats.get(a));
            }
            //push list to window
            for (int a = 0; a < logicalPropertyList.size(); a++) {
                propertyList.addToList(logicalPropertyList.get(a));
            }
            //display detailed information about the selected item
            int index = propertyList.getIndex();
            if (index < logicalPropertyList.size()) {
                Ship selected = (Ship) propertyList.getItemAtIndex(index);
                behave(selected);
                infoList.addToList("--Basic--");
                infoList.addToList(" ");
                infoList.addToList("Credits:      " + selected.getCash());
                infoList.addToList("Behavior:     " + selected.getBehavior());
                infoList.addToList("Autopilot:    " + selected.getAutopilot());
                /*
                 * Specifics
                 */
                infoList.addToList(" ");
                infoList.addToList("--Advanced--");
                infoList.addToList(" ");
                if (selected.getHomeBase() != null) {
                    infoList.addToList("Homebase:     " + selected.getHomeBase().getName());
                    infoList.addToList("              " + selected.getHomeBase().getCurrentSystem());
                    infoList.addToList(" ");
                }
                fillSpecifics(selected);
                infoList.addToList(" ");
                infoList.addToList("--Integrity--");
                infoList.addToList(" ");
                infoList.addToList("Shield:       " + roundTwoDecimal(100.0 * (selected.getShield() / selected.getMaxShield())) + "%");
                infoList.addToList("Hull:         " + roundTwoDecimal(100.0 * (selected.getHull() / selected.getMaxHull())) + "%");
                infoList.addToList("Fuel:         " + roundTwoDecimal(100.0 * (selected.getFuel() / selected.getMaxFuel())) + "%");
                infoList.addToList(" ");
                infoList.addToList("--Fitting--");
                infoList.addToList(" ");
                ArrayList<Hardpoint> fit = selected.getHardpoints();
                for (int a = 0; a < fit.size(); a++) {
                    infoList.addToList(fit.get(a));
                }
                infoList.addToList(" ");
                infoList.addToList("--Cargo--");
                infoList.addToList(" ");
                ArrayList<Item> cargo = selected.getCargoBay();
                for (int a = 0; a < cargo.size(); a++) {
                    infoList.addToList(cargo.get(a));
                }
                infoList.addToList(" ");
                //more
                fillDescriptionLines(selected);
                fillCommandLines(selected);
            }
        }
    }

    private void fillSpecifics(Ship selected) {
        if (selected != null) {
            boolean isStation = false;
            if (selected.getCurrentSystem().getStationList().contains(selected)) {
                isStation = true;
            }
            if (isStation) {
                //fill info on process status
                Station test = (Station) selected;
                for (int a = 0; a < test.getJobs().size(); a++) {
                    infoList.addToList("Job:          " + test.getJobs().get(a));
                }
                //fill info on resource and product quantities
                infoList.addToList(" ");
                for (int a = 0; a < test.getStationBuying().size(); a++) {
                    int q = test.getStationBuying().get(a).getQuantity();
                    int m = test.getStationBuying().get(a).getStore();
                    String n = test.getStationBuying().get(a).getName();
                    infoList.addToList("Resource:     " + n + " [" + q + " / " + m + "]");
                }
                infoList.addToList(" ");
                for (int a = 0; a < test.getStationSelling().size(); a++) {
                    int q = test.getStationSelling().get(a).getQuantity();
                    int m = test.getStationSelling().get(a).getStore();
                    String n = test.getStationSelling().get(a).getName();
                    infoList.addToList("Product:      " + n + " [" + q + " / " + m + "]");
                }
                infoList.addToList(" ");
            }
            /*
             * More autopilot info
             */
            if (selected.getAutopilot() == Autopilot.FLY_TO_CELESTIAL) {
                infoList.addToList("Waypoint:     " + selected.getFlyToTarget().getName());
            }
            if (selected.getPort() != null) {
                if (selected.getAutopilot() == Autopilot.DOCK_STAGE1) {
                    infoList.addToList("Docking At:   " + selected.getPort().getParent().getName());
                } else if (selected.getAutopilot() == Autopilot.DOCK_STAGE2) {
                    infoList.addToList("Docking At:   " + selected.getPort().getParent().getName());
                }
            }
            /*
             * More behavior info
             */
            /*if (selected.getBehavior() == Behavior.PATROL) {
             //what are we flying to?
             if (selected.getTarget() != null) {
             infoList.addToList("Attacking:    " + selected.getTarget().getName());
             } else {
             infoList.addToList("NO AIM");
             }
             } else if (selected.getBehavior() == Behavior.SECTOR_TRADE) {
             Station start = selected.getBuyFromStation();
             Station end = selected.getSellToStation();
             Item ware = selected.getWorkingWare();
             if (start != null && end != null && ware != null) {
             infoList.addToList("Ware:         " + selected.getWorkingWare().getName());
             infoList.addToList("From:         " + start.getName());
             infoList.addToList("To:           " + end.getName());
             }
             } else if (selected.getBehavior() == Behavior.UNIVERSE_TRADE) {
             Station start = selected.getBuyFromStation();
             Station end = selected.getSellToStation();
             Item ware = selected.getWorkingWare();
             if (start != null && end != null && ware != null) {
             infoList.addToList("Ware:         " + selected.getWorkingWare().getName());
             infoList.addToList("From:         " + start.getName());
             infoList.addToList("              " + start.getCurrentSystem());
             infoList.addToList("To:           " + end.getName());
             infoList.addToList("              " + end.getCurrentSystem());
             }
             } else if (selected.getBehavior() == Behavior.SUPPLY_HOMEBASE) {
             Station start = selected.getBuyFromStation();
             Station end = selected.getSellToStation();
             Item ware = selected.getWorkingWare();
             if (start != null && end != null && ware != null) {
             infoList.addToList("Ware:         " + selected.getWorkingWare().getName());
             infoList.addToList("From:         " + start.getName());
             infoList.addToList("              " + start.getCurrentSystem());
             infoList.addToList("To:           " + end.getName());
             infoList.addToList("              " + end.getCurrentSystem());
             }
             } else if (selected.getBehavior() == Behavior.REPRESENT_HOMEBASE) {
             Station start = selected.getBuyFromStation();
             Station end = selected.getSellToStation();
             Item ware = selected.getWorkingWare();
             if (start != null && end != null && ware != null) {
             infoList.addToList("Ware:         " + selected.getWorkingWare().getName());
             infoList.addToList("From:         " + start.getName());
             infoList.addToList("              " + start.getCurrentSystem());
             infoList.addToList("To:           " + end.getName());
             infoList.addToList("              " + end.getCurrentSystem());
             }
             }*/
        }
    }

    private double roundTwoDecimal(double d) {
        try {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return Double.parseDouble(twoDForm.format(d));
        } catch (Exception e) {
            System.out.println("Not a Number");
            return 0;
        }
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillDescriptionLines(Ship selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        if (selected != null) {
            Item shipItem = new Item(selected.getType().getValue("type"));
            String description = shipItem.getDescription();
            //fill
            int lineWidth = (((infoList.getWidth() - 10) / (infoList.getFont().getSize())));
            int cursor = 0;
            String tmp = "";
            String[] words = description.split(" ");
            for (int a = 0; a < words.length; a++) {
                if (a < 0) {
                    a = 0;
                }
                int len = words[a].length();
                if (cursor < lineWidth && !words[a].equals("/br/")) {
                    if (cursor + len <= lineWidth) {
                        tmp += " " + words[a];
                        cursor += len;
                    } else {
                        if (lineWidth > len) {
                            infoList.addToList(tmp);
                            tmp = "";
                            cursor = 0;
                            a--;
                        } else {
                            tmp += "[LEN!]";
                        }
                    }
                } else {
                    infoList.addToList(tmp);
                    tmp = "";
                    cursor = 0;
                    if (!words[a].equals("/br/")) {
                        a--;
                    }
                }
            }
            infoList.addToList(tmp.toString());
        }
    }

    private void fillCommandLines(Ship selected) {
        if (selected != null) {
            boolean isStation = false;
            if (selected.getCurrentSystem().getStationList().contains(selected)) {
                isStation = true;
            }
            /*
             * Funds transfer can happen no matter where the ships are located.
             */
            optionList.addToList("--Transfer--");
            optionList.addToList(" ");
            optionList.addToList(CMD_MOVEFUNDS);
            /*
             * Some actions are only possible while both ships are docked in the same
             * station. This is the block for those.
             */
            /*if (selected.isDocked() && selected.getUniverse().getPlayerShip().isDocked()) {
             if (selected.getPort() != null) {
             Station a = selected.getPort().getParent();
             Station b = selected.getUniverse().getPlayerShip().getPort().getParent();
             if (a == b) {
             optionList.addToList(CMD_SWITCH);
             }
             }
             }*/
            optionList.addToList(" ");
            /*
             * These activate behaviors on a ship
             */
            optionList.addToList("--Console--");
            optionList.addToList(" ");
            optionList.addToList(CMD_RENAME);
            optionList.addToList(CMD_REMOTECARGO);
            optionList.addToList(" ");
            optionList.addToList(CMD_NONE);
            if (!isStation) {
                optionList.addToList(" ");
                optionList.addToList(CMD_TRADE);
                if (selected.hasGroupInCargo("jumpdrive")) {
                    optionList.addToList(CMD_UTRADE);
                    if (selected.getHomeBase() != null) {
                        optionList.addToList(" ");
                        optionList.addToList(CMD_CLEARHOME);
                        optionList.addToList(CMD_SUPPLYHOME);
                        optionList.addToList(CMD_REPRESENTHOME);
                        optionList.addToList(" ");
                    } else {
                        optionList.addToList(" ");
                        optionList.addToList(CMD_SETHOME);
                        optionList.addToList(" ");
                    }
                }
                optionList.addToList(CMD_PATROL);
            }
            optionList.addToList(" ");
            /*
             * Some things can't be done while docked.
             */
            if (selected.isDocked()) {
                optionList.addToList(CMD_UNDOCK);
                optionList.addToList(CMD_TRADEWITH);
            } else {
                if (!isStation) {
                    optionList.addToList(CMD_DOCK);
                    optionList.addToList(CMD_FLYTO);
                    optionList.addToList(CMD_FOLLOW);
                    optionList.addToList(CMD_ALLSTOP);
                    if (ship.hasGroupInCargo("jumpdrive")) {
                        optionList.addToList(" ");
                        optionList.addToList(CMD_JUMP);
                    }
                    optionList.addToList(" ");
                    optionList.addToList("--Combat--");
                    optionList.addToList(" ");
                    optionList.addToList(CMD_ATTACK);
                    optionList.addToList(" ");
                }
                optionList.addToList("--Red Zone--");
                optionList.addToList(" ");
                optionList.addToList(CMD_DESTRUCT);
            }
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        if (cargo.isVisible() || trader.isVisible()) {
            //make sure nothing else has focus so only the child gets it
            propertyList.setFocused(false);
            infoList.setFocused(false);
            optionList.setFocused(false);
            inputList.setFocused(false);
        }
        super.handleMouseReleasedEvent(me, mouseLoc);
        if (optionList.isFocused()) {
            String command = (String) optionList.getItemAtIndex(optionList.getIndex());
            parseCommand(command);
        }
    }

    private void parseCommand(String command) {
        if (command != null && mode == Mode.NONE) {
            Ship selected = (Ship) propertyList.getItemAtIndex(propertyList.getIndex());
            if (command.equals(CMD_SWITCH)) {
                /*
                 * Switch to another ship.
                 */ ship.getCurrentSystem().getUniverse().setPlayerShip(selected);
            } else if (command.equals(CMD_NONE)) {
                //abort current behavior
                selected.setBehavior(Behavior.NONE);
                selected.setAutopilot(Autopilot.NONE);
                selected.cmdAbortDock();
            } else if (command.equals(CMD_TRADE)) {
                //selected.setBehavior(Behavior.SECTOR_TRADE);
            } else if (command.equals(CMD_UTRADE)) {
                //selected.setBehavior(Behavior.UNIVERSE_TRADE);
            } else if (command.equals(CMD_PATROL)) {
                //selected.setBehavior(Behavior.PATROL);
            } else if (command.equals(CMD_MOVEFUNDS)) {
                mode = Mode.WAITING_FOR_CREDITS;
                showInput("0");
            } else if (command.equals(CMD_RENAME)) {
                mode = Mode.WAITING_FOR_NAME;
                showInput(selected.getName());
            } else if (command.equals(CMD_UNDOCK)) {
                selected.cmdUndock();
            } else if (command.equals(CMD_TRADEWITH)) {
                mode = Mode.WAITING_FOR_TRADE;
                trader.setVisible(true);
                tmp = selected;
            } else if (command.equals(CMD_DOCK)) {
                ArrayList<Object> choice = new ArrayList<>();
                choice.add("--Select Station To Dock At--");
                choice.add(" ");
                ArrayList<Station> st = selected.getDockableStationsInSystem();
                for (int a = 0; a < st.size(); a++) {
                    choice.add(st.get(a));
                }
                if (st.size() > 0) {
                    showInputList(choice);
                    mode = Mode.WAITING_FOR_STATION;
                } else {
                    mode = Mode.NONE;
                }
            } else if (command.equals(CMD_REMOTECARGO)) {
                mode = Mode.WAITING_FOR_CARGO;
                cargo.setVisible(true);
                tmp = selected;
            } else if (command.equals(CMD_ATTACK)) {
                /*mode = Mode.WAITING_FOR_ATTACK;
                 ArrayList<Object> choice = new ArrayList<>();
                 choice.add("--Select Target To Attack--");
                 choice.add(" ");
                 ArrayList<Ship> sh = selected.getShipsInSensorRange();
                 for (int a = 0; a < sh.size(); a++) {
                 choice.add(sh.get(a));
                 }
                 if (sh.size() > 0) {
                 showInputList(choice);
                 mode = Mode.WAITING_FOR_ATTACK;
                 } else {
                 mode = Mode.NONE;
                 }*/
            } else if (command.equals(CMD_DESTRUCT)) {
                selected.setState(State.DYING);
            } else if (command.equals(CMD_FLYTO)) {
                ArrayList<Object> choice = new ArrayList<>();
                choice.add("--Select Target To Fly To--");
                choice.add(" ");
                ArrayList<Entity> cel = selected.getCurrentSystem().getCelestials();
                for (int a = 0; a < cel.size(); a++) {
                    choice.add(cel.get(a));
                }
                if (cel.size() > 0) {
                    showInputList(choice);
                    mode = Mode.WAITING_FOR_CELESTIAL;
                } else {
                    mode = Mode.NONE;
                }
            } else if (command.equals(CMD_FOLLOW)) {
                /*ArrayList<Object> choice = new ArrayList<>();
                 choice.add("--Select Target To Follow--");
                 choice.add(" ");
                 ArrayList<Ship> sh = selected.getShipsInSensorRange();
                 for (int a = 0; a < sh.size(); a++) {
                 choice.add(sh.get(a));
                 }
                 if (sh.size() > 0) {
                 showInputList(choice);
                 mode = Mode.WAITING_FOR_FOLLOW;
                 } else {
                 mode = Mode.NONE;
                 }*/
            } else if (command.equals(CMD_ALLSTOP)) {
                //selected.cmdAllStop();
            } else if (command.equals(CMD_JUMP)) {
                /*ArrayList<Object> choice = new ArrayList<>();
                 choice.add("--Select Target System--");
                 choice.add(" ");
                 ArrayList<SolarSystem> sh = ship.getUniverse().getDiscoveredSpace();
                 for (int a = 0; a < sh.size(); a++) {
                 if (ship.canJump(sh.get(a))) {
                 choice.add(sh.get(a));
                 }
                 }
                 if (sh.size() > 0) {
                 showInputList(choice);
                 mode = Mode.WAITING_FOR_JUMP;
                 } else {
                 mode = Mode.NONE;
                 }*/
            } else if (command.equals(CMD_CLEARHOME)) {
                selected.clearHomeBase();
            } else if (command.equals(CMD_SETHOME)) {
                ArrayList<Object> choice = new ArrayList<>();
                choice.add("--Select Home Base In System--");
                choice.add(" ");
                ArrayList<Entity> stat = selected.getCurrentSystem().getStationList();
                for (int a = 0; a < stat.size(); a++) {
                    if (selected.getCurrentSystem().getUniverse().getPlayerProperty().contains(stat.get(a))) {
                        choice.add(stat.get(a));
                    }
                }
                if (stat.size() > 0) {
                    showInputList(choice);
                    mode = Mode.WAITING_FOR_BASE;
                } else {
                    mode = Mode.NONE;
                }
            } else if (command.equals(CMD_SUPPLYHOME)) {
                //selected.setBehavior(Behavior.SUPPLY_HOMEBASE);
            } else if (command.equals(CMD_REPRESENTHOME)) {
                //selected.setBehavior(Behavior.REPRESENT_HOMEBASE);
            }
        }
    }
}
