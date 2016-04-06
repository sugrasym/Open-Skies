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
 * Allows communication between NPCs and the player.
 * Nathan Wiehoff
 */
package gdi;

import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import entity.Entity.State;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.util.ArrayList;
import lib.AstralMessage;
import lib.Binling;

public class CommWindow extends AstralWindow {

    AstralList messageLog = new AstralList(this);
    AstralList replyList = new AstralList(this);
    AstralList messageDisplay = new AstralList(this);
    AstralMessage working;
    protected Ship ship;

    public CommWindow(AssetManager assets) {
        super(assets, 300, 300, false);
        generate();
    }

    private void generate() {
        backColor = windowBlue;
        //size this window
        width = 300;
        height = 300;
        setVisible(false);
        //setup the message list
        messageLog.setX(0);
        messageLog.setY(height / 2);
        messageLog.setWidth(width / 2 - 1);
        messageLog.setHeight((height / 2) - 1);
        messageLog.setVisible(true);
        //setup the display for the message text
        messageDisplay.setX(0);
        messageDisplay.setY(0);
        messageDisplay.setWidth(width - 1);
        messageDisplay.setHeight((height / 2) - 1);
        messageDisplay.setVisible(true);
        //setup the list of replies
        replyList.setX(width / 2);
        replyList.setY(height / 2);
        replyList.setWidth((int) (width / 2) - 1);
        replyList.setHeight((height / 2) - 1);
        replyList.setVisible(true);
        //pack
        addComponent(messageLog);
        addComponent(replyList);
        addComponent(messageDisplay);
    }

    public void update(Ship ship) {
        setShip(ship);
        if (ship != null) {
            messageLog.clearList();
            //get ship's message que
            ArrayList<AstralMessage> que = ship.getMessages();
            //add each one
            for (int a = que.size() - 1; a >= 0; a--) {
                messageLog.addToList(que.get(a));
                if (!(que.get(a).getSender().getState() == State.ALIVE)) {
                    que.get(a).setRepliedTo(true);
                }
            }
        }
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillMessageLines() {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        if (working != null) {
            messageDisplay.addToList(" ");
            messageDisplay.addToList("--Sender--");
            messageDisplay.addToList(" ");
            messageDisplay.addToList("From:         " + working.getSender().getName());
            messageDisplay.addToList("              " + working.getSender().getPilot());
            messageDisplay.addToList("On Behalf Of: " + working.getSender().getFaction().getName());
            messageDisplay.addToList("Subject:      " + working.getSubject());
            messageDisplay.addToList(" ");
            messageDisplay.addToList(" ");
            messageDisplay.addToList("--Message--");
            messageDisplay.addToList(" ");
            //
            String description = working.getMessage();
            int lineWidth = (((messageDisplay.getWidth() - 1) / (messageDisplay.getFont().getSize())));
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
                            messageDisplay.addToList(tmp);
                            tmp = "";
                            cursor = 0;
                            a--;
                        } else {
                            tmp += "[LEN!]";
                        }
                    }
                } else {
                    messageDisplay.addToList(tmp);
                    tmp = "";
                    cursor = 0;
                    if (!words[a].equals("/br/")) {
                        a--;
                    }
                }
            }
            messageDisplay.addToList(tmp);
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        if (messageLog.isFocused()) {
            //clear
            messageDisplay.clearList();
            replyList.clearList();
            //get the faction
            int index = messageLog.getIndex();
            AstralMessage selected = (AstralMessage) messageLog.getItemAtIndex(index);
            working = selected;
            //push message
            fillMessageLines();
            //push options
            if (selected != null) {
                if (!selected.isRepliedTo()) {
                    replyList.addToList("--Reply--");
                    replyList.addToList(" ");
                    if (selected.getChoices() != null) {
                        if (selected.getChoices().size() > 0) {
                            for (int a = 0; a < selected.getChoices().size(); a++) {
                                replyList.addToList(selected.getChoices().get(a));
                            }
                        } else {
                            selected.setRepliedTo(true);
                        }
                    } else {
                        selected.setRepliedTo(true);
                    }
                } else {
                    selected.setRepliedTo(true);
                }
            }
        }
        if (replyList.isFocused()) {
            if (working != null) {
                int index = replyList.getIndex();
                if (replyList.getItemAtIndex(index) instanceof Binling) {
                    Binling pick = (Binling) replyList.getItemAtIndex(index);
                    if (pick != null) {
                        working.reply(pick);
                    }
                }
            }
        }
    }
}
