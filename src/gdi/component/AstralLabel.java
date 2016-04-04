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
 * This is a label. It displays text.
 */
package gdi.component;

import gdi.Utility;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralLabel extends AstralComponent {
    //very basic label stuff

    protected String text;
    protected Font font = new Font("Monospaced", Font.PLAIN, 12);
    protected Color fontColor = whiteForeground;
    protected Color backColor = transparent;
    BufferedImage buffer;

    public AstralLabel(int width, int height) {
        super(width, height);
    }

    public AstralLabel() {

    }

    @Override
    public void render(Graphics f) {
        if (visible) {
            if (buffer == null) {
                buffer = Utility.CreateCompatibleImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D s = (Graphics2D) buffer.getGraphics();
            //draw the background
            s.setColor(backColor);
            s.fillRect(0, 0, getWidth(), getHeight());
            //draw the text
            s.setFont(font);
            s.setColor(fontColor);
            drawString(s, text, 1, 0);
            //push
            f.drawImage(buffer, x, y, null);
            //clear
            s.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            s.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawString(Graphics g, String text, int x, int y) {
        for (String line : text.split("\n")) {
            g.drawString(line, x, y += font.getSize());
        }
    }

    public Color getFontColor() {
        return fontColor;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Color getBackColor() {
        return backColor;
    }

    public void setBackColor(Color backColor) {
        this.backColor = backColor;
    }
}
