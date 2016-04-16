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
 * Displays the contents of a station your ship is currently docked at and allows
 * you to buy and sell to the station.
 */
package gdi;

import cargo.DockingPort;
import cargo.Item;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import gdi.component.AstralInput;
import gdi.component.AstralLabel;
import gdi.component.AstralList;
import gdi.component.AstralListItem;
import gdi.component.AstralWindow;
import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.Universe;

public class TradeWindow extends AstralWindow {

    private Ship ship;
    private Station docked;
    AstralLabel buyLabel = new AstralLabel();
    AstralLabel sellLabel = new AstralLabel();
    AstralList productList = new AstralList(this);
    AstralList resourceList = new AstralList(this);
    AstralList cargoList = new AstralList(this);
    AstralList propertyList = new AstralList(this);
    AstralList optionList = new AstralList(this);
    AstralInput input = new AstralInput();
    //quick lookup
    Parser shipParser = Universe.getCache().getShipCache();
    Parser weaponParser = Universe.getCache().getWeaponCache();
    //logical
    AstralList lastFocus = cargoList;
    //behavior

    private enum Behavior {

        WAITING_TO_BUY,
        WAITING_TO_SELL,
        NONE
    };
    private Behavior action = Behavior.NONE;

    public TradeWindow(AssetManager assets) {
        super(assets, 500, 400, false);
        generate();
    }

    public TradeWindow(AssetManager assets, int width, int height) {
        super(assets, width, height, false);
        generate();
    }

    private void generate() {
        backColor = windowBlue;
        setVisible(false);
        //setup input method
        input.setName("Input");
        input.setText("|");
        input.setY(getHeight() / 2);
        input.setVisible(false);
        input.setWidth(150);
        input.setX((getWidth() / 2) - input.getWidth() / 2);
        input.setHeight((input.getFont().getSize() + 2));
        //setup buying label
        buyLabel.setName("buy");
        buyLabel.setText("Buying");
        buyLabel.setX(0);
        buyLabel.setY(0);
        buyLabel.setWidth(width / 2);
        buyLabel.setHeight(buyLabel.getFont().getSize() + 2);
        buyLabel.setVisible(true);
        //setup selling label
        sellLabel.setName("sell");
        sellLabel.setText("Selling");
        sellLabel.setX(width / 2);
        sellLabel.setY(0);
        sellLabel.setWidth(width / 2);
        sellLabel.setHeight(sellLabel.getFont().getSize() + 2);
        sellLabel.setVisible(true);
        //setup the buying list
        resourceList.setX(0);
        resourceList.setY(buyLabel.getFont().getSize() + 2);
        resourceList.setWidth((width / 2) - 1);
        resourceList.setHeight((height / 2) - 3);
        resourceList.setVisible(true);
        //setup the selling list
        productList.setX(width / 2);
        productList.setY(sellLabel.getFont().getSize() + 2);
        productList.setWidth((width / 2) - 1);
        productList.setHeight((height / 2) - 3);
        productList.setVisible(true);
        //setup cargo list
        cargoList.setX(0);
        cargoList.setY((height / 2) + sellLabel.getFont().getSize());
        cargoList.setWidth((int) (width / 1.5));
        cargoList.setHeight((height / 4) - (buyLabel.getFont().getSize() + 2));
        cargoList.setVisible(true);
        //setup property list
        propertyList.setX(0);
        propertyList.setY((height / 2) + (height / 4));
        propertyList.setWidth((int) (width / 1.5));
        propertyList.setHeight((height / 4) - 1);
        propertyList.setVisible(true);
        //setup options list
        optionList.setX((int) (width / 1.5) + 1);
        optionList.setY((height / 2) + sellLabel.getFont().getSize());
        optionList.setWidth((int) (width / 3) - 1);
        optionList.setHeight((height / 2) - (sellLabel.getFont().getSize() + 1));
        optionList.setVisible(true);
        //pack
        addComponent(buyLabel);
        addComponent(sellLabel);
        addComponent(resourceList);
        addComponent(productList);
        addComponent(cargoList);
        addComponent(propertyList);
        addComponent(optionList);
        addComponent(input);
    }

    public void update(Ship ship) {
        this.ship = ship;
        if (ship.isDocked()) {
            docked = ship.getPort().getParent();
            productList.clearList();
            resourceList.clearList();
            cargoList.clearList();
            propertyList.clearList();
            optionList.clearList();
            {
                ArrayList<Item> logicalCargoList = new ArrayList<>();
                //add cargo goods
                ArrayList<Item> cargo = ship.getCargoBay();
                for (int a = 0; a < cargo.size(); a++) {
                    logicalCargoList.add(cargo.get(a));
                }
                //add to display
                for (int a = 0; a < logicalCargoList.size(); a++) {
                    cargoList.addToList(logicalCargoList.get(a));
                }
                //display products and resources
                Station station = ship.getPort().getParent();
                ArrayList<Item> selling = station.getStationSelling();
                for (int a = 0; a < selling.size(); a++) {
                    Item tmp = selling.get(a);
                    if (tmp.getType().equals("ship")) {
                        //make sure there is a port available to sell at
                        ArrayList<DockingPort> ports = station.getPorts();
                        for (int n = 0; n < ports.size(); n++) {
                            if (ports.get(n).isEmpty()) {
                                productList.addToList(tmp);
                                break;
                            }
                        }
                    } else {
                        productList.addToList(tmp);
                    }
                }
                ArrayList<Item> buying = station.getStationBuying();
                for (int a = 0; a < buying.size(); a++) {
                    resourceList.addToList(buying.get(a));
                }
                //display info on selected
                int index = lastFocus.getIndex();
                if (lastFocus.getItemAtIndex(index) != null) {
                    Item selected = (Item) lastFocus.getItemAtIndex(index);
                    propertyList.addToList("--GLOBAL--");
                    propertyList.addToList(" ");
                    propertyList.addToList(new AstralListItem("Credits:      " + ship.getCash(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Bay Volume:   " + ship.getCargo(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Volume Used:  " + ship.getBayUsed(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Percent Used: " + ship.getBayUsed() / ship.getCargo() * 100.0 + "%", "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Vessel Mass:  " + ship.getMass(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(" ");
                    propertyList.addToList("--BASIC--");
                    propertyList.addToList(" ");
                    propertyList.addToList(new AstralListItem("Name:         " + selected.getName(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Type:         " + selected.getType(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Mass:         " + selected.getMass() / selected.getQuantity(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Volume:       " + selected.getVolume() / selected.getQuantity(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(" ");
                    propertyList.addToList("--MARKET--");
                    propertyList.addToList(" ");
                    propertyList.addToList(new AstralListItem("Min Price:    " + selected.getMinPrice(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(new AstralListItem("Max Price:    " + selected.getMaxPrice(), "TOOLTIPPLACEHOLDER"));
                    propertyList.addToList(" ");
                    propertyList.addToList("--DETAIL--");
                    propertyList.addToList(" ");
                    fillDescriptionLines(selected);
                    fillCommandLines(selected);
                }
            }
            //behave
            behave();
        } else {
            //you can't trade when you're not docked
            setVisible(false);
            docked = null;
        }
    }

    private void behave() {
        if (action == Behavior.NONE) {
            //do nothing
        } else if (action == Behavior.WAITING_TO_BUY) {
            //check if a value was returned
            try {
                if (input.canReturn()) {
                    int val = Integer.parseInt(input.getText());
                    if (val > 0) {
                        //perform trade
                        int index = lastFocus.getIndex();
                        Item selected = (Item) lastFocus.getItemAtIndex(index);
                        docked.buy(ship, selected, val);
                    }
                    //hide it
                    input.setVisible(false);
                    //normal mode
                    action = Behavior.NONE;
                }
            } catch (Exception e) {
                System.out.println("Malformed input");
                //normal mode
                action = Behavior.NONE;
            }
        } else if (action == Behavior.WAITING_TO_SELL) {
            //check if a value was returned
            try {
                if (input.canReturn()) {
                    int val = Integer.parseInt(input.getText());
                    if (val > 0) {
                        //perform trade
                        int index = lastFocus.getIndex();
                        Item selected = (Item) lastFocus.getItemAtIndex(index);
                        docked.sell(ship, selected, val);
                    }
                    //hide it
                    input.setVisible(false);
                    //normal mode
                    action = Behavior.NONE;
                }
            } catch (Exception e) {
                System.out.println("Malformed input");
                //normal mode
                action = Behavior.NONE;
            }

        } else {
            //unreachable
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        if (action == Behavior.NONE) {
            super.handleMouseReleasedEvent(me, mouseLoc);
            //get the module and toggle its enabled status
            if (productList.isFocused()) {
                lastFocus = productList;
            } else if (resourceList.isFocused()) {
                lastFocus = resourceList;
            } else if (cargoList.isFocused()) {
                lastFocus = cargoList;
            }
            //handle trade commands
            if (optionList.isFocused()) {
                String command = (String) optionList.getItemAtIndex(optionList.getIndex());
                parseCommand(command);
            }
        }
    }

    private void parseCommand(String command) {
        if (command != null) {
            switch (command) {
                case "Sell":
                    showInput();
                    action = Behavior.WAITING_TO_SELL;
                    break;
                case "Buy":
                    showInput();
                    action = Behavior.WAITING_TO_BUY;
                    break;
            }
        }
    }

    private void showInput() {
        input.setText("1");
        input.setVisible(true);
        input.setFocused(true);
    }

    private void fillCommandLines(Item selected) {
        optionList.addToList("--Market--");
        optionList.addToList("Price: " + docked.getPrice(selected));
        optionList.addToList(" ");
        optionList.addToList("--Trade--");
        if (lastFocus == cargoList) {
            optionList.addToList("Sell");
        } else if (lastFocus == productList) {
            optionList.addToList("Buy");
        } else if (lastFocus == resourceList) {
            optionList.addToList("Resources Not Sold.");
            optionList.addToList("Check Cargo Bay.");
        }
    }

    private void fillDescriptionLines(Item selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        String description = selected.getDescription();
        int lineWidth = (((propertyList.getWidth() - 10) / (propertyList.getFont().getSize())));
        int cursor = 0;
        String tmp = "";
        String[] words;
        if (description != null) {
            words = description.split(" ");
        } else {
            words = "Missing Item: No information available".split(" ");
        }
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
                        propertyList.addToList(tmp);
                        tmp = "";
                        cursor = 0;
                        a--;
                    } else {
                        tmp += "[LEN!]";
                    }
                }
            } else {
                propertyList.addToList(tmp);
                tmp = "";
                cursor = 0;
                if (!words[a].equals("/br/")) {
                    a--;
                }
            }
        }
        propertyList.addToList(tmp);
        appendShipDetails(selected);
        appendWeaponDetails(selected);
    }

    private void appendShipDetails(Item selected) {
        //add ship info
        ArrayList<Term> types = shipParser.getTermsOfType("Ship");
        Term test = null;
        for (int a = 0; a < types.size(); a++) {
            if (types.get(a).getValue("type").equals(selected.getName())) {
                test = types.get(a);
                break;
            }
        }
        if (test != null) {
            propertyList.addToList(" ");
            propertyList.addToList("--Ship Details--");
            propertyList.addToList(" ");
            //it's a ship! time to provide important information about it
            String hpRaw = test.getValue("hardpoint");
            if (hpRaw != null) {
                //show info on loadout possibilities
                String[] arr = hpRaw.split("/");
                for (int a = 0; a < arr.length; a++) {
                    String[] det = arr[a].split(",");
                    String hpType = det[0];
                    String hpSize = det[1];
                    String txt = hpType + " size " + hpSize;
                    propertyList.addToList(new AstralListItem("Hardpoint:    " + txt, "TOOLTIPPLACEHOLDER"));
                }
            }
            propertyList.addToList(" ");
            String mShield = test.getValue("shield");
            String rShield = test.getValue("shieldRecharge");
            String mHull = test.getValue("hull");
            String thrust = test.getValue("thrust");
            String torque = test.getValue("torque");
            String sensor = test.getValue("sensor");
            String fuel = test.getValue("fuel");
            String mass = test.getValue("mass");
            String cargo = test.getValue("cargo");
            //add
            propertyList.addToList(new AstralListItem("Shield:       " + mShield, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Shield Regen: " + rShield, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Hull:         " + mHull, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Fuel:         " + fuel, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Sensor:       " + sensor, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Cargo:        " + cargo, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Mass:         " + mass, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Thrust:       " + thrust, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Torque:       " + torque, "TOOLTIPPLACEHOLDER"));
        }
    }

    private void appendWeaponDetails(Item selected) {
        //add ship info
        ArrayList<Term> types = weaponParser.getTermsOfType("Weapon");
        Term test = null;
        for (int a = 0; a < types.size(); a++) {
            if (types.get(a).getValue("name").equals(selected.getName())) {
                test = types.get(a);
                break;
            }
        }
        if (test != null) {
            propertyList.addToList(" ");
            propertyList.addToList("--Weapon Details--");
            propertyList.addToList(" ");
            //it's a weapon! time to provide important information about it
            propertyList.addToList(" ");
            String type = test.getValue("type");
            String shieldDamage = test.getValue("shieldDamage");
            String hullDamage = test.getValue("hullDamage");
            String range = test.getValue("range");
            String speed = test.getValue("speed");
            String refire = test.getValue("refire");
            String guided = test.getValue("guided");
            String ammo = test.getValue("ammo");
            String accel = test.getValue("accel");
            String turning = test.getValue("turning");
            //add
            propertyList.addToList(new AstralListItem("Type:         " + type, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Damage:       " + shieldDamage + " / "
                    + hullDamage, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Range:        " + range, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Muzzle Vel:   " + speed, "TOOLTIPPLACEHOLDER"));
            propertyList.addToList(new AstralListItem("Cooldown:     " + refire, "TOOLTIPPLACEHOLDER"));
            if (guided != null) {
                propertyList.addToList(new AstralListItem("Guided:       " + guided, "TOOLTIPPLACEHOLDER"));
            }
            if (ammo != null) {
                propertyList.addToList(new AstralListItem("Ammo:         " + ammo, "TOOLTIPPLACEHOLDER"));
            }
            if (accel != null) {
                propertyList.addToList(new AstralListItem("Accel:        " + accel, "TOOLTIPPLACEHOLDER"));
            }
            if (turning != null) {
                propertyList.addToList(new AstralListItem("Turning:      " + turning, "TOOLTIPPLACEHOLDER"));
            }
        }
    }
}
