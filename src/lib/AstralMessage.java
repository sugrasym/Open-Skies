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
