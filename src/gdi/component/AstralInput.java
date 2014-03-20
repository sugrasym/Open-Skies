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
 * Input box for asking the player things.
 */
package gdi.component;

import com.jme3.asset.AssetManager;
import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 *
 * @author nwiehoff
 */
public class AstralInput extends AstralLabel {

    private boolean canReturn = false;

    public AstralInput() {
        setFocusColor(Color.PINK);
        setBackColor(windowGrey);
    }

    @Override
    public void handleKeyReleasedEvent(String ke) {
        if (ke.equals("KEY_BACKSPACE")) {
            if (getText().length() > 0) {
                setText(getText().substring(0, getText().length() - 1));
            }
        } else if (ke.equals("KEY_RETURN")) {
            //return
            setVisible(false);
            setCanReturn(true);
        } else {
            setText(getText() + ke.split("_")[1]);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        canReturn = false;
    }

    public boolean canReturn() {
        return canReturn;
    }

    public void setCanReturn(boolean canReturn) {
        this.canReturn = canReturn;
    }
}
