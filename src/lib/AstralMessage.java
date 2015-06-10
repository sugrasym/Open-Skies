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
 * Message frame for sending queries to the player. It is up to the sender
 * to monitor for replies.
 */
package lib;

import celestial.Ship.Ship;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author nwiehoff
 */
public class AstralMessage implements Serializable {

    private final Ship sender;
    private String message;
    private String subject;
    private String name = "";
    private final ArrayList<Binling> choices;
    private boolean repliedTo = false;
    private boolean wasSent = false;

    public AstralMessage(Ship sender, String subject, String message, ArrayList<Binling> choices) {
        this.sender = sender;
        this.message = message;
        this.choices = choices;
        this.subject = subject;
    }

    public Ship getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<Binling> getChoices() {
        return choices;
    }

    public boolean isRepliedTo() {
        return repliedTo;
    }

    public void setRepliedTo(boolean repliedTo) {
        this.repliedTo = repliedTo;
    }

    public void reply(Binling choice) {
        if (sender != null) {
            sender.recieveReply(choice);
        }
        repliedTo = true;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean wasSent() {
        return wasSent;
    }

    public void setWasSent(boolean wasSent) {
        this.wasSent = wasSent;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
