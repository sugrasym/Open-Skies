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
 * Extensible framework for in-game items.
 * Nathan Wiehoff, masternerdguy@yahoo.com
 */
package cargo;

import java.io.Serializable;
import java.util.ArrayList;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.Universe;

public class Item implements Serializable {
    //parameters

    public static final String TYPE_CANNON = "cannon";
    public static final String TYPE_MISSILE = "missile";
    public static final String TYPE_TURRET = "turret";
    public static final String TYPE_BATTERY = "battery";
    public static final String TYPE_COMMODITY = "commodity";
    public static final String TYPE_SHIP = "ship";
    public static final String TYPE_STATION = "station";
    private int volume;
    private int store;
    private double mass;
    private double HP;
    private String name;
    private String description;
    private String type;
    protected String group;
    protected int quantity = 1;
    //flags
    private boolean alive;
    //economic parameters
    private int minPrice;
    private int maxPrice;

    public Item(String name) {
        this.name = name;
        init();
    }

    private void init() {
        Parser parse = Universe.getCache().getItemCache();
        ArrayList<Term> terms = parse.getTermsOfType("Item");
        Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.equals(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            //extract
            type = relevant.getValue("type");
            volume = Integer.parseInt(relevant.getValue("volume"));
            mass = Double.parseDouble(relevant.getValue("mass"));
            HP = Double.parseDouble(relevant.getValue("HP"));
            minPrice = Integer.parseInt(relevant.getValue("minPrice"));
            maxPrice = Integer.parseInt(relevant.getValue("maxPrice"));
            description = relevant.getValue("description");
            group = relevant.getValue("group");
            String st = relevant.getValue("store");
            if (st != null) {
                store = Integer.parseInt(relevant.getValue("store"));
            } else {
                store = 1000;
            }
        } else {
            System.out.println("The item " + getName() + " does not exist in ITEM.txt");
        }
    }

    public void periodicUpdate(double tpf) {
    }

    //Utility
    public void damageItem(double damage) {
        HP -= damage;
        if (HP <= 0) {
            alive = false;
            HP = 0;
        }
    }

    //Reusable Parsing
    public String removeLeftSpaces(String s) {
        return s.replaceAll("^\\s+", "");
    }

    //Accesors + Mutators
    public double getVolume() {
        return volume * (double)quantity;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public double getMass() {
        return mass * (double)quantity;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getHP() {
        return HP;
    }

    public void setHP(double HP) {
        this.HP = HP;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name + "[" + quantity + "]";
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getStore() {
        return store;
    }

    public void setStore(int store) {
        this.store = store;
    }
}