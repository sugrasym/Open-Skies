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
 * A list is a, well, list of objects. You can select from the list, scroll
 * the list, and all kinds of neat stuff.
 */
package gdi.component;

import com.jme3.math.Vector3f;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralList extends AstralComponent {

    ArrayList<Object> listContents = new ArrayList<>();
    //appearance
    protected Font font = new Font("Monospaced", Font.PLAIN, 12);
    protected Color fontColor = whiteForeground;
    protected Color backColor = windowBlue;
    protected Color selectColor = Color.DARK_GRAY;
    BufferedImage buffer;
    //index and scrolling
    protected int index = 0;
    protected int scrollPosition = 0;
    private int scrollDirection = 0;
    //for getting info about my location
    private AstralWindow parent;
    //for the scroll bar
    private int oldMx;
    private int oldMy;
    boolean dragging = false;

    public AstralList(AstralWindow parent, int width, int height) {
        super(width, height);
        this.parent = parent;
    }

    public AstralList(AstralWindow parent) {
        super();
        this.parent = parent;
    }

    @Override
    public void render(Graphics f) {
        if (visible) {
            try {
                if (buffer == null) {
                    buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                }
                Graphics2D s = (Graphics2D) buffer.getGraphics();
                //draw the background
                s.setColor(backColor);
                s.fillRect(0, 0, getWidth(), getHeight());
                //draw over the selected
                s.setColor(selectColor);
                s.fillRect(0, (index - scrollPosition) * getFont().getSize(), getWidth() - 10, getFont().getSize());
                //draw the text
                s.setFont(getFont());
                s.setColor(fontColor);
                for (int a = scrollPosition; a < listContents.size(); a++) {
                    s.drawString(listContents.get(a).toString(), 1, ((a + 1) - scrollPosition) * getFont().getSize());
                }
                //draw indicator that there is more or less to the list
                int displayableLines = getHeight() / getFont().getSize();
                //draw scroll bar
                s.setColor(fontColor);
                if (displayableLines >= listContents.size()) {
                    s.fillRect(getWidth() - 10, 0, 9, getHeight());
                } else {
                    double scale = (double) displayableLines / (double) listContents.size();
                    double listPercent = (double) scrollPosition / (double) listContents.size();
                    s.fillRect(getWidth() - 10, (int) (listPercent * getHeight()), 9, (int) (scale * getHeight()) + 2);
                }
                //draw the edges
                s.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                //push
                f.drawImage(buffer, x, y, null);
            } catch (Exception e) {
                scrollPosition = 0;
                index = 0;
            }
        }
    }

    @Override
    public void periodicUpdate() {
        if(!isFocused() || !isVisible()) {
            scrollDirection = 0;
            dragging = false;
        }
        if (scrollDirection == 0) {
        } else if (scrollDirection == -1) {
            scrollUp();
        } else if (scrollDirection == 1) {
            scrollDown();
        }
        if (listContents.size() < scrollPosition) {
            scrollPosition = listContents.size();
        }
    }

    public void addToList(Object item) {
        listContents.add(item);
    }

    public void removeFromList(Object item) {
        listContents.remove(item);
    }

    public void clearList() {
        listContents.clear();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        if (index < listContents.size()) {
            this.index = index;
        }
    }

    public Object getItemAtIndex(int index) {
        if (index < listContents.size()) {
            return listContents.get(index);
        } else {
            index = 0;
            return null;
        }
    }

    public Color getSelectColor() {
        return selectColor;
    }

    public void setSelectColor(Color selectColor) {
        this.selectColor = selectColor;
    }

    public void scrollDown() {
        scrollPosition++;
        if (scrollPosition > listContents.size() - (int) (getHeight() / getFont().getSize())) {
            scrollPosition = listContents.size() - (int) (getHeight() / getFont().getSize());
        }
    }

    public void scrollUp() {
        scrollPosition--;
        if (scrollPosition < 0) {
            scrollPosition = 0;
        }
    }

    @Override
    public void handleKeyPressedEvent(String ke, boolean shiftDown) {
        if (ke.equals("KEY_UP")) {
            scrollDirection = -1;
        } else if (ke.equals("KEY_DOWN")) {
            scrollDirection = 1;
        }
    }

    @Override
    public void handleKeyReleasedEvent(String ke, boolean shiftDown) {
        if (ke.equals("KEY_UP")) {
            scrollDirection = 0;
        } else if (ke.equals("KEY_DOWN")) {
            scrollDirection = 0;
        }
    }

    @Override
    public void handleMousePressedEvent(String me, Vector3f mouseLoc) {
        //determine the relative mouse location
        int rx = oldMx = (int) mouseLoc.x - x;
        int ry = oldMy = (int) mouseLoc.y - y;
        //create the local mouse rect
        Rectangle mouseRect = new Rectangle(rx, ry, 1, 1);
        //create the scrollbar rect
        Rectangle scrollRect = new Rectangle(getWidth() - 9, 0, 9, getHeight());
        int displayableLines = getHeight() / getFont().getSize();
        if (scrollRect.intersects(mouseRect) && (displayableLines < listContents.size())) {
            dragging = true;
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        if (dragging) {
            //scroll based on how far the mouse moved
            int dy = ((int) (mouseLoc.getY() - y) - oldMy);
            double change = dy / (double) getHeight();
            scrollPosition += (int) (listContents.size() * change);
            //check bounds
            if (scrollPosition < 0) {
                scrollPosition = 0;
            }
            if (scrollPosition > listContents.size()) {
                scrollPosition = listContents.size() - 1;
            }
            dragging = false;
        } else {
            //determine the relative mouse location
            int rx = oldMx = (int) mouseLoc.x - x;
            int ry = oldMy = (int) mouseLoc.y - y;
            //create the local mouse rect
            Rectangle mouseRect = new Rectangle(rx, ry, 1, 1);
            //check for list intersections to update selection
            checkForListIntersection(mouseRect);
        }
    }

    private void checkForListIntersection(Rectangle mouseRect) {
        int displayableLines = getHeight() / getFont().getSize();
        for (int a = 0; a < displayableLines; a++) {
            Rectangle lRect = new Rectangle(0, a * getFont().getSize(), getWidth() - 9, getFont().getSize());
            if (lRect.intersects(mouseRect)) {
                int newSelection = a + scrollPosition;
                setIndex(newSelection);
            }
        }
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }
}
