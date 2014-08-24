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
 * Allows the cargo bay of a ship to be viewed.
 * Nathan Wiehoff
 */
package gdi;

import cargo.Equipment;
import cargo.Hardpoint;
import cargo.Item;
import cargo.Weapon;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.util.ArrayList;
import lib.astral.Parser;
import universe.Universe;

public class CargoWindow extends AstralWindow {

    public static final String CMD_TRASH = "Trash";
    public static final String CMD_EJECT = "Eject";
    public static final String CMD_UNMOUNT = "Unmount";
    public static final String CMD_MOUNT = "Mount";
    public static final String CMD_STACK = "Stack";
    public static final String CMD_SPLIT = "Split";
    public static final String CMD_SPLITALL = "Split All";
    public static final String CMD_ASSEMBLE = "Assemble";
    public static final String CMD_PACKAGE = "Package";
    public static final String CMD_DEPLOY = "Deploy";
    public static final String CMD_CLAIMSOV = "Claim System";
    public static final String CMD_USEPASTE = "Use Repair Paste";
    AstralList cargoList = new AstralList(this);
    AstralList propertyList = new AstralList(this);
    AstralList optionList = new AstralList(this);
    protected Ship ship;

    public CargoWindow(AssetManager assets) {
        super(assets, 500, 400, false);
        generate();
    }

    public CargoWindow(AssetManager assets, int width, int height) {
        super(assets, width, height, false);
        generate();
    }

    private void generate() {
        backColor = windowGrey;
        setVisible(false);
        //setup the cargo list
        cargoList.setX(0);
        cargoList.setY(0);
        cargoList.setWidth(width);
        cargoList.setHeight((height / 2) - 1);
        cargoList.setVisible(true);
        //setup the property list
        propertyList.setX(0);
        propertyList.setY(height / 2);
        propertyList.setWidth((int) (width / 1.5));
        propertyList.setHeight((height / 2) - 1);
        propertyList.setVisible(true);
        //setup the fitting label
        optionList.setX((int) (width / 1.5) + 1);
        optionList.setY(height / 2);
        optionList.setWidth((int) (width / 3));
        optionList.setHeight((height / 2) - 1);
        optionList.setVisible(true);
        //pack
        addComponent(cargoList);
        addComponent(propertyList);
        addComponent(optionList);
    }

    public void update(Ship ship) {
        ArrayList<Item> logicalCargoList = new ArrayList<>();
        if (ship != null) {
            setShip(ship);
            cargoList.clearList();
            propertyList.clearList();
            optionList.clearList();
            //add equipment
            for (int a = 0; a < ship.getHardpoints().size(); a++) {
                logicalCargoList.add(ship.getHardpoints().get(a).getMounted());
            }
            //add cargo goods
            ArrayList<Item> cargo = ship.getCargoBay();
            for (int a = 0; a < cargo.size(); a++) {
                logicalCargoList.add(cargo.get(a));
            }
            //add to display
            for (int a = 0; a < logicalCargoList.size(); a++) {
                cargoList.addToList(logicalCargoList.get(a));
            }
            //display detailed information about the selected item
            int index = cargoList.getIndex();
            if (index < logicalCargoList.size()) {
                Item selected = (Item) cargoList.getItemAtIndex(index);
                //fill
                propertyList.addToList("--GLOBAL--");
                propertyList.addToList(" ");
                propertyList.addToList("Credits:      " + ship.getCash());
                propertyList.addToList("Bay Volume:   " + ship.getCargo());
                propertyList.addToList("Volume Used:  " + ship.getBayUsed());
                propertyList.addToList("Percent Used: " + ship.getBayUsed() / ship.getCargo() * 100.0 + "%");
                propertyList.addToList("Vessel Mass:  " + ship.getMass());
                propertyList.addToList(" ");
                propertyList.addToList("--BASIC--");
                propertyList.addToList(" ");
                propertyList.addToList("Name:         " + selected.getName());
                propertyList.addToList("Type:         " + selected.getType());
                propertyList.addToList("Mass:         " + selected.getMass());
                propertyList.addToList("Volume:       " + selected.getVolume());
                propertyList.addToList(" ");
                propertyList.addToList("--MARKET--");
                propertyList.addToList(" ");
                propertyList.addToList("Min Price:    " + selected.getMinPrice());
                propertyList.addToList("Max Price:    " + selected.getMaxPrice());
                propertyList.addToList(" ");
                propertyList.addToList("--DETAIL--");
                fillDescriptionLines(selected);
                fillCommandLines(selected);
            }
        }
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillDescriptionLines(Item selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        String description = selected.getDescription();
        int lineWidth = (((propertyList.getWidth() - 10) / (propertyList.getFont().getSize())));
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
        propertyList.addToList(tmp.toString());
    }

    private void fillCommandLines(Item selected) {
        boolean canEject = true;
        if (selected instanceof Equipment) {
            optionList.addToList("--Fitting--");
            Equipment tmp = (Equipment) selected;
            Hardpoint socket = tmp.getSocket();
            if (socket != null) {
                //it is mounted
                optionList.addToList(CMD_UNMOUNT);
                canEject = false;
            } else {
                //it is not mounted
                optionList.addToList(CMD_MOUNT);
                optionList.addToList(CMD_PACKAGE);
            }
            optionList.addToList(" ");
        } else {
            /*
             * Options for sov papers
             */
            /*if (selected.getGroup().equals("sovtransfer")) {
             if (!ship.isDocked()) {
             optionList.addToList("--Setup--");
             optionList.addToList(CMD_CLAIMSOV);
             optionList.addToList(" ");
             }
             }*/
            /*
             * Options for stations
             */
            //determine if this is a station
            if (selected.getGroup().equals("constructionkit")) {
                if (!ship.isDocked()) {
                    optionList.addToList("--Setup--");
                    optionList.addToList(CMD_DEPLOY);
                    optionList.addToList(" ");
                }
            }
            /*
             * Options for repair kits
             */
            if (selected.getGroup().equals("repairkit")) {
                optionList.addToList("--Setup--");
                optionList.addToList(CMD_USEPASTE);
                optionList.addToList(" ");
            }
            /*
             * Options for cannons
             */
            if (selected.getType().equals(Item.TYPE_CANNON)) {
                optionList.addToList("--Setup--");
                optionList.addToList(CMD_ASSEMBLE);
                optionList.addToList(" ");
            }
            /*
             * Options for missiles
             */
            if (selected.getType().equals(Item.TYPE_MISSILE)) {
                optionList.addToList("--Setup--");
                optionList.addToList(CMD_ASSEMBLE);
                optionList.addToList(" ");
            }
            /*
             * Options for turret
             */
            if (selected.getType().equals(Item.TYPE_TURRET)) {
                optionList.addToList("--Setup--");
                optionList.addToList(CMD_ASSEMBLE);
                optionList.addToList(" ");
            }
            /*
             * Options for battery
             */
            if (selected.getType().equals(Item.TYPE_BATTERY)) {
                optionList.addToList("--Setup--");
                optionList.addToList(CMD_ASSEMBLE);
                optionList.addToList(" ");
            }
        }
        //for packaging and repackaging
        optionList.addToList("--Packaging--");
        optionList.addToList(CMD_STACK);
        optionList.addToList(CMD_SPLIT);
        optionList.addToList(CMD_SPLITALL);
        //doing these last for safety.
        optionList.addToList(" ");
        optionList.addToList("--Dangerous--");
        optionList.addToList(CMD_TRASH);
        if (!ship.isDocked() && canEject) {
            optionList.addToList(CMD_EJECT);
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        //get the module and toggle its enabled status
        if (optionList.isFocused()) {
            String command = (String) optionList.getItemAtIndex(optionList.getIndex());
            parseCommand(command);
        }
    }

    private void parseCommand(String command) {
        if (command != null) {
            if (command.equals(CMD_TRASH)) {
                /*
                 * This command simply destroys an item.
                 */
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                ship.removeFromCargoBay(selected);
            } else if (command.equals(CMD_EJECT)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                ship.ejectFromCargoBay(selected);
            } else if (command.equals(CMD_UNMOUNT)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                Equipment tmp = (Equipment) selected;
                ship.unfit(tmp);
            } else if (command.equals(CMD_MOUNT)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                Equipment tmp = (Equipment) selected;
                ship.fit(tmp);
            } else if (command.equals(CMD_STACK)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                ArrayList<Item> cargoBay = ship.getCargoBay();
                if (cargoBay.contains(selected)) {
                    stackItem(cargoBay, selected);
                }
            } else if (command.equals(CMD_SPLIT)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                ArrayList<Item> cargoBay = ship.getCargoBay();
                if (cargoBay.contains(selected)) {
                    if (selected.getQuantity() > 1) {
                        Item tmp = new Item(selected.getName());
                        cargoBay.add(tmp);
                        selected.setQuantity(selected.getQuantity() - 1);
                    }
                }
            } else if (command.equals(CMD_SPLITALL)) {
                ArrayList<Item> cargoBay = ship.getCargoBay();
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                if (ship.hasInCargo(selected)) {
                    if (selected.getQuantity() > 1) {
                        for (int a = 0; a < cargoBay.size(); a++) {
                            Item tmp = cargoBay.get(a);
                            if (tmp.getName().equals(selected.getName())) {
                                if (tmp.getType().equals(selected.getType())) {
                                    if (tmp.getGroup().equals(selected.getGroup())) {
                                        cargoBay.remove(tmp);
                                    }
                                }
                            }
                        }
                        for (int a = 0; a < selected.getQuantity(); a++) {
                            Item tmp = new Item(selected.getName());
                            ship.addToCargoBay(tmp);
                        }
                    }
                }
            } else if (command.equals(CMD_ASSEMBLE)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                if (selected.getQuantity() == 1) {
                    Weapon tmp = new Weapon(selected.getName());
                    ship.removeFromCargoBay(selected);
                    if (ship.addToCargoBay(tmp)) {
                        //success
                    } else {
                        //failure, add the old one back
                        ship.addToCargoBay(selected);
                    }
                }
            } else if (command.equals(CMD_PACKAGE)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                if (selected.getQuantity() == 1) {
                    Equipment tmp = (Equipment) selected;
                    ship.removeFromCargoBay(selected);
                    Item nTmp = new Item(tmp.getName());
                    if (ship.addToCargoBay(nTmp)) {
                        //success!
                    } else {
                        //failure
                        ship.addToCargoBay(selected);
                    }
                }
            } else if (command.equals(CMD_DEPLOY)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                if (selected.getQuantity() == 1) {
                    //deploy the station
                    Parser tmp = Universe.getCache().getStationCache();
                    ArrayList<Parser.Term> list = tmp.getTermsOfType("Station");
                    Parser.Term hull = null;
                    for (int a = 0; a < list.size(); a++) {
                        if (list.get(a).getValue("type").equals(selected.getName())) {
                            hull = list.get(a);
                            break;
                        }
                    }
                    Station ret = new Station(ship.getCurrentSystem().getUniverse(),
                            hull, ship.getFaction().getName());
                    ret.setName("Your " + selected.getName());
                    //
                    boolean safe = false;
                    double sx = 0;
                    double sz = 0;
                    safe = true; //todo: make asteroid mines only buildable on asteroids
                    //configure coordinates
                    double dx = 500;
                    double dy = 500;
                    sx = (ship.getLocation().getX()) + dx;
                    sz = (ship.getLocation().getZ()) + dy;
                    if (safe) {
                        ret.setLocation(new Vector3f((float) sx, ship.getLocation().getY(), (float) sz));
                        //finalize
                        ret.setCurrentSystem(ship.getCurrentSystem());
                        ship.getCurrentSystem().putEntityInSystem(ret);
                        //remove item from cargo
                        selected.setQuantity(0);
                        ship.removeFromCargoBay(selected);
                        //since it's not NPC make sure it has no start cash
                        ret.clearWares();
                    }
                }
            } else if (command.equals(CMD_CLAIMSOV)) {/*
                 Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                 if (selected.getQuantity() == 1) {
                 if (!ship.getCurrentSystem().getOwner().equals("Neutral")) {
                 //count stations
                 int count = 0;
                 ArrayList<Entity> stationList = ship.getCurrentSystem().getStationList();
                 for (int a = 0; a < stationList.size(); a++) {
                 Station test = (Station) stationList.get(a);
                 if (test.getFaction().equals("Player")) {
                 count++;
                 }
                 }
                 if (count >= 4) {
                 //standings hit
                 Faction tmp = new Faction(ship.getCurrentSystem().getOwner());
                 ship.getUniverse().getPlayerShip().getMyFaction().derivedModification(tmp, -8.0);
                 //transfer
                 ship.getCurrentSystem().setOwner("Player");
                 //notify player
                 ship.composeMessage(ship, ship.getCurrentSystem().getName() + " claimed", "Congratulations on your "
                 + "recent acquisition! If you fail to maintain at least 1 station in this system "
                 + "you will lose control, and it will return to a new owner. Fly safe. /br/ /br/ "
                 + "Paralegal: Beyond the Law", null);
                 //remove papers
                 ship.removeFromCargoBay(selected);
                 } else {
                 ship.composeMessage(ship, "Terms of Service", "You need at least 4 stations here for us to transfer ownership. /br/ /br/ "
                 + "Paralegal: Beyond the Law", null);
                 }
                 } else {
                 ship.composeMessage(ship, "Terms of Service", "We can't work with neutral space, sorry /br/ /br/ "
                 + "Paralegal: Beyond the Law", null);
                 }
                 }*/

            } else if (command.equals(CMD_USEPASTE)) {
                Item selected = (Item) cargoList.getItemAtIndex(cargoList.getIndex());
                for (int a = 0; a < selected.getQuantity(); a++) {
                    //if the hp of the ship is less than the max, use a unit of paste
                    if (ship.getHull() < ship.getMaxHull()) {
                        ship.setHull(ship.getHull() + (float) selected.getHP());
                        selected.setQuantity(selected.getQuantity() - 1);
                    }
                    //limit to max hull
                    if (ship.getHull() > ship.getMaxHull()) {
                        ship.setHull(ship.getMaxHull());
                    }
                }
                //remove if needed
                if (selected.getQuantity() <= 0) {
                    ship.removeFromCargoBay(selected);
                }

            }
        }
    }

    private void stackItem(ArrayList<Item> cargoBay, Item selected) {
        if (cargoBay.contains(selected)) {
            for (int a = 0; a < cargoBay.size(); a++) {
                Item tmp = cargoBay.get(a);
                if (tmp != selected) {
                    if (selected.getName().equals(tmp.getName())) {
                        if (selected.getGroup().equals(tmp.getGroup())) {
                            if (selected.getType().equals(tmp.getType())) {
                                tmp.setQuantity(selected.getQuantity() + tmp.getQuantity());
                                cargoBay.remove(selected);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
