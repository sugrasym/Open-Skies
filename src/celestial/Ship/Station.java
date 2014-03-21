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
 * Space station!
 */
package celestial.Ship;

import cargo.DockingPort;
import cargo.Item;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import java.util.ArrayList;
import java.util.Random;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Station extends Ship {
    //market

    protected ArrayList<Item> stationSelling = new ArrayList<>();
    protected ArrayList<Item> stationBuying = new ArrayList<>();
    //docking
    private ArrayList<DockingPort> ports = new ArrayList<>();

    public Station(Universe universe, Term type, String faction) {
        super(universe, type, faction);
    }

    protected void alive() {
        //update parent
        super.alive();
        //update station stuff
    }

    /*
     * Docking code
     */
    public ArrayList<DockingPort> getPorts() {
        return ports;
    }

    public DockingPort requestDockingPort(Ship client) {
        return null;
    }

    /*
     * Market code
     */
    public ArrayList<Item> getStationSelling() {
        return stationSelling;
    }

    public ArrayList<Item> getStationBuying() {
        return stationBuying;
    }

    public void buy(Ship ship, Item item, int quantity) {
        //get current offer
        int price = getPrice(item);
        Item tmp = new Item(item.getName());
        //repeat buy procedure
        for (int lx = 0; lx < quantity; lx++) {
            Item rel = null;
            //validate the item is available
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().equals(item.getName())) {
                    //make sure there is something available
                    if (stationSelling.get(a).getQuantity() > 0) {
                        rel = stationSelling.get(a);
                    }
                    break;
                }
            }
            if (rel != null) {
                //validate the player can cover the charge
                if (ship.getCash() - price >= 0) {
                    //branch based on regular item or ship
                    if (rel.getType().equals("ship")) {
                        /*
                         * This one is a little more complicated.
                         */
                        /*//make a ship
                         Ship newShip = new Ship("Your " + rel.getName(), rel.getName());
                         //initialize it to the correct faction
                         newShip.setFaction(ship.getFaction());
                         newShip.init(false);
                         //find an open hanger
                         PortContainer pick = null;
                         for (int a = 0; a < docks.size(); a++) {
                         if (docks.get(a).canFit(newShip) && docks.get(a).isAvailable(newShip)) {
                         //got one
                         pick = docks.get(a);
                         break;
                         }
                         }
                         if (pick != null) {
                         //decrement stocks
                         rel.setQuantity(rel.getQuantity() - 1);
                         //drop it in the current solar system
                         newShip.setCurrentSystem(currentSystem);
                         currentSystem.putEntityInSystem(newShip);
                         //drop it in that port
                         pick.setClient(newShip);
                         newShip.setPort(pick);
                         //allow the port to naturally pick it up when it collides
                         newShip.setX(pick.getPortX());
                         newShip.setY(pick.getPortY());
                         newShip.setAutopilot(Autopilot.DOCK_STAGE3);
                         //transfer funds
                         ship.setCash(ship.getCash() - price);
                         setCash(getCash() + price);
                         //make sure it doesn't have funds
                         newShip.setCash(0);
                         }*/
                    } else {
                        /*
                         * This is pretty simple
                         */
                        //attempt transfer of item
                        if (ship.addToCargoBay(tmp)) {
                            //decrement stocks
                            rel.setQuantity(rel.getQuantity() - 1);
                            //transfer funds
                            ship.setCash(ship.getCash() - price);
                            setCash(getCash() + price);
                        }
                    }
                }
            }
        }
    }

    public void sell(Ship ship, Item item, int quantity) {
        //get current offer
        int price = getPrice(item);
        //repeat sell procedure
        for (int lx = 0; lx < quantity; lx++) {
            Item rel = null;
            //validate the item is in the cargo bay
            for (int a = 0; a < ship.getCargoBay().size(); a++) {
                if (ship.getCargoBay().get(a).getName().equals(item.getName())) {
                    rel = ship.getCargoBay().get(a);
                    break;
                }
            }
            if (rel != null) {
                //send to station
                for (int a = 0; a < getStationBuying().size(); a++) {
                    //make sure station can cover it
                    if (getCash() - price >= 0) {
                        if (rel.getName().equals(getStationBuying().get(a).getName())) {
                            getStationBuying().get(a).setQuantity(getStationBuying().get(a).getQuantity() + 1);
                            //remove from cargo
                            ship.removeFromCargoBay(rel);
                            //pay the ship
                            ship.setCash(ship.getCash() + price);
                            //remove funds from station wallet
                            setCash(getCash() - price);
                            break;
                        }
                    }
                }
            }
        }
    }

    public int getPrice(Item item) {
        int max = 0;
        int min = 0;
        int q = 0;
        int s = 1;
        //get the right commodity
        boolean found = false;
        for (int a = 0; a < stationBuying.size(); a++) {
            if (stationBuying.get(a).getName().equals(item.getName())) {
                max = stationBuying.get(a).getMaxPrice();
                min = stationBuying.get(a).getMinPrice();
                q = stationBuying.get(a).getQuantity();
                s = stationBuying.get(a).getStore();
                found = true;
                break;
            }
        }
        if (!found) {
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().equals(item.getName())) {
                    max = stationSelling.get(a).getMaxPrice();
                    min = stationSelling.get(a).getMinPrice();
                    q = stationSelling.get(a).getQuantity();
                    s = stationSelling.get(a).getStore();
                    found = true;
                    break;
                }
            }
        }
        //calculate price
        int d = max - min;
        float per = (float) q / (float) s;
        int x = (int) (d * (1 - per));
        int price = min + x;
        if (price < min) {
            price = min;
        } else if (price > max) {
            price = max;
        }
        return price;
    }

    protected void randomizeInitialGoods(int seed) {
        Random rnd = new Random(seed);
        if (stationSelling.size() > 0) {
            for (int a = 0; a < stationSelling.size(); a++) {
                stationSelling.get(a).setQuantity(rnd.nextInt(stationSelling.get(a).getStore()));
            }
        }
        if (stationBuying.size() > 0) {
            for (int a = 0; a < stationBuying.size(); a++) {
                stationBuying.get(a).setQuantity(rnd.nextInt(stationBuying.get(a).getStore()));
            }
        }
    }
}
