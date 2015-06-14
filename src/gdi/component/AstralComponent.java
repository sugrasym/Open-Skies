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
 * This class is the base for any sort of component.
 */
package gdi.component;

import com.jme3.math.Vector3f;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

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
    protected Color windowBlue = new Color(20, 30, 45);
    protected Color whiteForeground = new Color(250, 250, 250);
    protected Color transparent = new Color(0, 0, 0, 0);
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

    public void handleKeyPressedEvent(String ke, boolean shiftDown) {
    }

    public void handleKeyReleasedEvent(String ke, boolean shiftDown) {
    }

    public void handleMouseMovedEvent(String me, Vector3f mouseLoc) {
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
