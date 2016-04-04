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
 * A health bar, progress bar, or any other kind of variable color bar you need.
 */
package gdi.component;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import gdi.Utility;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralBar extends AstralComponent {

    protected Color barColor = whiteForeground;
    protected double percentage = 100;
    BufferedImage buffer;

    public AstralBar(int width, int height) {
        super(width, height);
    }

    @Override
    public void render(Graphics f) {
        if (visible) {
            if (buffer == null) {
                buffer = Utility.CreateCompatibleImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D s = (Graphics2D)buffer.getGraphics();
            //draw the background
            s.setColor(getBarColor());
            s.fillRect(0, 0, (int) (percentage * getWidth()), getHeight());
            //push
            f.drawImage(buffer, x, y, null);
            //clear
            s.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            s.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public Color getBarColor() {
        return barColor;
    }

    public void setBarColor(Color barColor) {
        this.barColor = barColor;
    }
}
