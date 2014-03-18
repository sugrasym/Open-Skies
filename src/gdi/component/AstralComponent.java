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
 * This class is the base for any sort of component.
 */
package gdi.component;

import com.jme3.math.Vector3f;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralComponent {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean focused;
    protected boolean visible;
    protected String name;
    //colors
    protected Color windowGrey = new Color(25, 25, 25);
    protected Color amber = new Color(255, 126, 0);
    protected Color transparent = new Color(0, 0, 0, 1);
    protected Color focusColor = Color.PINK;

    public AstralComponent(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public AstralComponent() {
        
    }

    /*
     * Rendering and updating
     */
    public void render(Graphics f) {
        if (isVisible()) {
            f.setColor(Color.PINK);
            f.fillRect(getX(), getY(), getWidth(), getHeight());
        }
    }

    public void periodicUpdate() {
    }

    /*
     * Event handling
     */

    public void handleKeyPressedEvent(String ke) {
    }

    public void handleKeyReleasedEvent(String ke) {
    }

    public void handleMouseMovedEvent(MouseEvent me) {
    }

    public void handleMousePressedEvent(String me, Vector3f mouseLoc) {
    }

    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
    }

    /*
     * Access and mutation
     */
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getFocusColor() {
        return focusColor;
    }

    public void setFocusColor(Color focusColor) {
        this.focusColor = focusColor;
    }

    public boolean intersects(Rectangle rect) {
        return new Rectangle(x, y, getWidth(), getHeight()).intersects(rect);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
