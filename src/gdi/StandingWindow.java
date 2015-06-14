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
 * Allows the standings of the player to be viewed.
 * Nathan Wiehoff
 */
package gdi;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.util.ArrayList;
import java.util.Arrays;
import lib.Binling;
import lib.Faction;

public class StandingWindow extends AstralWindow {

    public static final String PLAYER_FACTION = "Player";
    public static final int HOSTILE_STANDING = -2;
    AstralList factionList = new AstralList(this);
    AstralList infoList = new AstralList(this);
    Faction viewing = null;
    protected Ship ship;
    
    public StandingWindow(AssetManager assets) {
        super(assets, 500, 400, false);
        generate();
    }

    public StandingWindow(AssetManager assets, int width, int height) {
        super(assets, width, height, false);
        generate();
    }

    private void generate() {
        backColor = windowBlue;
        //size this window
        setVisible(false);
        //setup the cargo list
        factionList.setX(0);
        factionList.setY(0);
        factionList.setWidth(width);
        factionList.setHeight((height / 2) - 1);
        factionList.setVisible(true);
        //setup the property list
        infoList.setX(0);
        infoList.setY(height / 2);
        infoList.setWidth((int) (width));
        infoList.setHeight((height / 2) - 1);
        infoList.setVisible(true);
        //pack
        addComponent(factionList);
        addComponent(infoList);
    }

    public void update(Ship ship) {
        setShip(ship);
        factionList.clearList();
        infoList.clearList();
        ArrayList<Binling> logicalFactionList = new ArrayList<>();
        if (ship != null) {
            //add factions
            Faction fac = ship.getFaction();
            ArrayList<Binling> standings = fac.getStandings();
            for (int a = 0; a < standings.size(); a++) {
                logicalFactionList.add(standings.get(a));
            }
            //sort by standings
            logicalFactionList = sort(logicalFactionList);
            //add to display
            for (int a = 0; a < logicalFactionList.size(); a++) {
                factionList.addToList(logicalFactionList.get(a));
            }
            //display detailed information about the selected item
            int index = factionList.getIndex();
            Binling bin = (Binling) factionList.getItemAtIndex(index);
            if (index < logicalFactionList.size()) {
                //fill
                fillFactionLines(viewing, bin);
                fillDescriptionLines(viewing);
            }
        }
    }

    private ArrayList<Binling> sort(ArrayList<Binling> list) {
        ArrayList<Binling> sorted = new ArrayList<>();
        {
            Binling[] arr = (Binling[]) list.toArray(new Binling[list.size()]);
            for (int a = 0; a < arr.length; a++) {
                for (int b = 1; b < arr.length - a; b++) {
                    if (arr[b - 1].getDouble() < arr[b].getDouble()) {
                        Binling tmp = arr[b];
                        arr[b] = arr[b - 1];
                        arr[b - 1] = tmp;
                    }
                }
            }
            sorted.addAll(Arrays.asList(arr));
        }
        return sorted;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillFactionLines(Faction selected, Binling simple) {
        if (selected != null) {
            infoList.addToList("--Basic--");
            infoList.addToList(" ");
            infoList.addToList("Name:         " + selected.getName());
            infoList.addToList("Empire:       " + selected.isEmpire());
            if (selected.isEmpire()) {
                infoList.addToList("Extent:       " + (100 * (selected.getSpread())) + "%");
            }
            infoList.addToList(" ");
            infoList.addToList("--Standings--");
            infoList.addToList(" ");
            infoList.addToList("You:          "
                    + ship.getCurrentSystem().getUniverse().getPlayerShip().getStandingsToMe(simple.getString()));
            infoList.addToList(" ");
            infoList.addToList("--Likes--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() > 0) {
                    if (!selected.getStandings().get(a).getString().matches(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            infoList.addToList(" ");
            infoList.addToList("--Dislikes--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() < 0) {
                    if (!selected.getStandings().get(a).getString().matches(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            infoList.addToList(" ");
            infoList.addToList("--Will Attack--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() <= Faction.HOSTILE_STANDING) {
                    if (!selected.getStandings().get(a).getString().matches(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            if (selected.getContraband().size() > 0) {
                infoList.addToList(" ");
                infoList.addToList("--Contraband--");
                infoList.addToList(" ");
                for (int a = 0; a < selected.getContraband().size(); a++) {
                    infoList.addToList(selected.getContraband().get(a));
                }
            }
        }
    }

    private void fillDescriptionLines(Faction selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        if (selected != null) {
            infoList.addToList(" ");
            infoList.addToList("--Description--");
            infoList.addToList(" ");
            //
            String description = selected.getDescription();
            int lineWidth = (((infoList.getWidth() - 10) / (infoList.getFont().getSize())));
            int cursor = 0;
            String tmp = "";
            String[] words = description.split(" ");
            for (int a = 0; a < words.length; a++) {
                if (a < 0) {
                    a = 0;
                }
                int len = words[a].length();
                if (cursor < lineWidth && !words[a].matches("/br/")) {
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
                    if (!words[a].matches("/br/")) {
                        a--;
                    }
                }
            }
            infoList.addToList(tmp);
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        if (factionList.isFocused()) {
            //get the faction
            int index = factionList.getIndex();
            Binling tmp = (Binling) factionList.getItemAtIndex(index);
            //build the superfaction
            if (tmp.getString().matches(PLAYER_FACTION)) {
                viewing = ship.getCurrentSystem().getUniverse().getPlayerShip()
                        .getFaction();
            } else {
                viewing = new Faction(tmp.getString());
            }
        }
    }
}
