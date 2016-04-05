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
 * A manufacturing process that takes an input and returns an output
 * on a schedule.
 */

package cargo;

import celestial.Ship.Station;
import java.io.Serializable;
import java.util.ArrayList;
import lib.astral.Parser;
import universe.Universe;

public class Job implements Serializable {
    //for holding information about needed resources and final products

    private ArrayList<Item> products = new ArrayList<>();
    private ArrayList<Item> resources = new ArrayList<>();
    //for delivering products and deducting resources
    private ArrayList<Item> stationSelling;
    private ArrayList<Item> stationBuying;
    Station host;
    protected String name;
    //timing
    double cycleTime;
    double timer = 0;

    public Job(Station host, String processName, ArrayList<Item> stationSelling, ArrayList<Item> stationBuying) {
        this.name = processName;
        this.host = host;
        this.stationSelling = stationSelling;
        this.stationBuying = stationBuying;
        //create process
        Parser parse = Universe.getCache().getProcessCache();
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Process");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.equals(processName)) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            cycleTime = Double.parseDouble(relevant.getValue("cycle"));
            //create products and products table
            String product = relevant.getValue("product");
            if (product != null) {
                String[] pArr = product.split("/");
                for (int a = 0; a < pArr.length; a++) {
                    Item p = new Item(pArr[a].split(",")[0]);
                    p.setQuantity(Integer.parseInt(pArr[a].split(",")[1]));
                    products.add(p);
                    //see if the station has this registered as a product
                    boolean needed = true;
                    for (int b = 0; b < stationSelling.size(); b++) {
                        if (stationSelling.get(b).getName().equals(p.getName())) {
                            //yep it does
                            needed = false;
                            break;
                        }
                    }
                    if (needed) {
                        //add it to the product table so it can be bought
                        Item p2 = new Item(pArr[a].split(",")[0]);
                        p2.setQuantity(0);
                        stationSelling.add(p2);
                    }
                }
            }
            //create resources and resource table
            String resource = relevant.getValue("resource");
            if (resource != null) {
                String[] rArr = resource.split("/");
                for (int a = 0; a < rArr.length; a++) {
                    Item p = new Item(rArr[a].split(",")[0]);
                    p.setQuantity(Integer.parseInt(rArr[a].split(",")[1]));
                    resources.add(p);
                    //see if the station has this registered as a resource
                    boolean needed = true;
                    for (int b = 0; b < stationBuying.size(); b++) {
                        if (stationBuying.get(b).getName().equals(p.getName())) {
                            //yep it does
                            needed = false;
                            break;
                        }
                    }
                    if (needed) {
                        //add it to the resource table so it can be bought
                        Item p2 = new Item(rArr[a].split(",")[0]);
                        p2.setQuantity(0);
                        stationBuying.add(p2);
                    }
                }
            }
        } else {
            System.out.println("The item " + getName() + " does not exist in PROCESSES.txt");
        }
    }

    public void periodicUpdate(double tpf) {
        /*for(int a = 0; a < stationBuying.size(); a++) {
         if(stationBuying.get(a).getQuantity() > stationBuying.get(a).getStore()) {
         System.out.println(host.getName()+" overflowed on "+stationBuying.get(a)+" by "+ (stationBuying.get(a).getQuantity() - stationBuying.get(a).getStore()));
         }
         }*/
        if (timer == 0) {
            //collect resources and start
            boolean hasResources = true;
            for (int a = 0; a < resources.size(); a++) {
                for (int b = 0; b < stationBuying.size(); b++) {
                    if (resources.get(a).getName().equals(stationBuying.get(b).getName())) {
                        if (stationBuying.get(b).getQuantity() >= resources.get(a).getQuantity()) {
                            //ok
                        } else {
                            hasResources = false;
                            break;
                        }
                    }
                }
            }
            if (hasResources) {
                for (int a = 0; a < resources.size(); a++) {
                    for (int b = 0; b < stationBuying.size(); b++) {
                        if (resources.get(a).getName().equals(stationBuying.get(b).getName())) {
                            stationBuying.get(b).setQuantity(stationBuying.get(b).getQuantity() - resources.get(a).getQuantity());
                            break;
                        }
                    }
                }
                timer += tpf;
            }
        } else if (timer < cycleTime) {

            //increment timer
            timer += tpf;
        } else {
            //process complete, deliver products and reset
            if (canDeliver()) {
                for (int a = 0; a < products.size(); a++) {
                    for (int b = 0; b < stationSelling.size(); b++) {
                        if (products.get(a).getName().equals(stationSelling.get(b).getName())) {
                            //deliver
                            stationSelling.get(b).setQuantity(stationSelling.get(b).getQuantity() + products.get(a).getQuantity());
                            break;
                        }
                    }
                }
                timer = 0;
            } else {
                //no room for product delivery, stalled
            }
        }
    }

    public boolean canDeliver() {
        try {
            //iterate each product
            for (int a = 0; a < products.size(); a++) {
                //check each station's selling list
                for (int b = 0; b < stationSelling.size(); b++) {
                    if (products.get(a).getName().equals(stationSelling.get(b).getName())) {
                        //determine if there is room for delivery
                        int stored = stationSelling.get(b).getQuantity();
                        int max = stationSelling.get(b).getStore();
                        int delivering = products.get(a).getQuantity();
                        if (stored + delivering > max) {
                            //no room
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        String ret = getName() + " ";
        {
            int percent = (int) (100.0 * (timer / cycleTime));
            ret += "(" + percent + "%)";
        }
        return ret;
    }
}
