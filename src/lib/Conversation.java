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
 * System for managing "conversations" between the player and NPCs. It is not
 * a messaging system for use by NPCs to talk to other NPCs.
 */
package lib;

import celestial.Ship.Ship;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.Mission;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Conversation implements Serializable {

    private final ArrayList<AstralMessage> nodes = new ArrayList<>();
    private final String name;
    private AstralMessage currentNode;
    private final Ship owner;
    //random
    Random rnd = new Random();
    //mission
    private Mission tmpMission;

    public Conversation(Ship owner, String name, String startNode) {
        this.name = name;
        this.owner = owner;
        init(owner);
        findStart(startNode);
    }

    public void periodicUpdate(double tpf) {
        try {
            if (currentNode != null) {
                if (!currentNode.wasSent()) {
                    owner.getUniverse().getPlayerShip().receiveMessage(currentNode);
                } else {
                    //wait
                }
            } else {
                //done
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findStart(String startNode) {
        currentNode = findNode(startNode);
    }

    private void init(Ship owner) {
        Parser tmp = Universe.getCache().getConversationCache();
        ArrayList<Term> list = tmp.getTermsOfType("Node");
        for (int a = 0; a < list.size(); a++) {
            String nme = list.get(a).getValue("name");
            String sub = list.get(a).getValue("subject");
            String bod = list.get(a).getValue("body");
            //generate binlings
            ArrayList<Binling> cho = new ArrayList<>();
            int v = 0;
            String type = "";
            while ((type = list.get(a).getValue("choice" + v)) != null) {
                //get station info
                String msg = type.split("/")[0];
                String next = type.split("/")[1];
                Binling tv = new Binling(msg, v);
                tv.getStr().add(next);
                cho.add(tv);
                //iterate
                v++;
            }
            //make message
            AstralMessage node = new AstralMessage(owner, sub, bod, cho);
            node.setName(nme);
            //store
            nodes.add(node);
        }
    }

    public void reply(Binling choice) {
        if (choice != null) {
            //advance
            currentNode = findNode(choice.getStr().get(1));
            //handle
            handleBinling(choice);
        } else {
            currentNode = findNode("END");
        }
    }

    private void handleBinling(Binling choice) {
        //does this bindling have additional params?
        if (choice.getStr().size() > 1) {
            //is this a mission offer?
            switch (choice.getStr().get(1)) {
                case "MISSION":
                    //generate a mission
                    tmpMission = new Mission(owner);
                    if (tmpMission.getBriefing() != null) {
                        //append mission body
                        String body = makeMissionDescription(tmpMission);
                        currentNode.setMessage(currentNode.getMessage().replace("<#MISSION>", body));
                    } else {
                        currentNode = findNode("END");
                    }   break;
                case "START_MISSION":
                    //assign generated mission
                    if (tmpMission != null) {
                        owner.getUniverse().getPlayerMissions().add(tmpMission);
                }   break;
                case "RUMOR":
                    //get a rumor
                    if (owner.getFaction().getRumorList().size() > 0) {
                        String pick = owner.getFaction().getRumorList().get(rnd.nextInt(owner.getFaction().getRumorList().size()));
                        currentNode = findNode(pick);
                    } else {
                        currentNode = findNode("END");
                }   break;
            //nope
                default:
                    break;
            }
        } else {
            //nope
        }
    }

    private String makeMissionDescription(Mission mission) {
        String ret = "";
        {
            ret += mission.getBriefing();
            //append reward info
            ret += " /br/ /br/ Cash Reward:     " + mission.getReward();
            ret += " /br/ /br/ Standings Bonus: " + mission.getDeltaStanding();
        }
        return ret;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        if (currentNode.getName().equals("END")) {
            return true;
        } else {
            return currentNode.getChoices().isEmpty();
        }
    }

    private AstralMessage findNode(String startNode) {
        for (int a = 0; a < nodes.size(); a++) {
            if (nodes.get(a).getName().equals(startNode)) {
                return nodes.get(a);
            }
        }
        return findNode("END");
    }
}
