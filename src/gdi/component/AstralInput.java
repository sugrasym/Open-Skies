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
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

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
        } else if (ke.equals("KEY_SPACE")) {
            setText(getText() + " ");
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
    
    @Override
    public void render(Graphics f) {
        if (visible) {
            if (buffer == null) {
                buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D s = (Graphics2D) buffer.getGraphics();
            //draw the background
            s.setColor(backColor);
            s.fillRect(0, 0, getWidth(), getHeight());
            //draw focus color
            if(focused) {
                s.setColor(focusColor);
                s.drawRect(0, 0, width-1, height-1);
            }
            //draw the text
            s.setFont(font);
            s.setColor(fontColor);
            s.drawString(text, 1, font.getSize());
            //push
            f.drawImage(buffer, x, y, null);
            //clear
            s.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            s.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
