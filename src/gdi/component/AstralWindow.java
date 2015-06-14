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
 * This is a window. It stores components.
 */
package gdi.component;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralWindow extends AstralComponent {

    protected Color backColor = Color.PINK;
    protected int order = 0;
    ArrayList<AstralComponent> components = new ArrayList<>();
    BufferedImage buffer;
    AssetManager assets;
    //quad
    Quad qd_background;
    Geometry geo_background;
    Material mat_background;
    Texture2D myTex;
    AWTLoader awtLoader;
    protected boolean flat = false;
    private final boolean alpha;

    public AstralWindow(AssetManager assets, int width, int height, boolean alpha) {
        super(width, height);
        this.assets = assets;
        this.alpha = alpha;
        createQuad();
        render(null);
    }

    private void createQuad() {
        qd_background = new Quad(getWidth(), getHeight());
        geo_background = new Geometry("Background", qd_background);
        mat_background = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        myTex = new Texture2D();
        awtLoader = new AWTLoader();
        if (alpha) {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        } else {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        }
        myTex.setImage(awtLoader.load(buffer, false));
        mat_background.setTexture("ColorMap", myTex);
        geo_background.setMaterial(mat_background);
        mat_background.getAdditionalRenderState().setAlphaTest(true);
        mat_background.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    }

    public void addComponent(AstralComponent component) {
        components.add(component);
    }

    public void removeComponent(AstralComponent component) {
        components.remove(component);
    }

    @Override
    public void periodicUpdate() {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                components.get(a).periodicUpdate();
            }
        } else {
            //
        }
    }

    public void add(Node guiNode) {
        guiNode.attachChild(geo_background);
    }

    public void remove(Node guiNode) {
        guiNode.detachChild(geo_background);
    }

    @Override
    public final void render(Graphics f) {
        try {
            if (visible) {
                //get graphics
                Graphics2D s = (Graphics2D) buffer.getGraphics();
                //render the backdrop
                s.setColor(backColor);
                s.fillRect(0, 0, getWidth(), getHeight());
                //render components
                for (int a = 0; a < components.size(); a++) {
                    components.get(a).render(s);
                }
                //draw focus borders
                if (focused) {
                    s.setColor(getFocusColor());
                    s.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
                if (!flat) {
                    //flip
                    /*AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                     tx.translate(0, -buffer.getHeight(null));
                     AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                     buffer = op.filter(buffer, null);*/
                    //push frame to quad
                    myTex.setImage(awtLoader.load(buffer, true));
                    mat_background.setTexture("ColorMap", myTex);
                    //geo_background.setMaterial(mat_background);
                } else {
                    f.drawImage(buffer, x, y, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void collect() {
        //set position
        if (visible) {
            geo_background.setLocalTranslation(x, y, 0);
        } else {
            //move it off the frame
            geo_background.setLocalTranslation(-1000000000, -1000000000, 0);
        }
        //set texture
        geo_background.setMaterial(mat_background);
    }

    @Override
    public void handleKeyPressedEvent(String ke, boolean shiftDown) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyPressedEvent(ke, shiftDown);
                }
            }
        }
    }

    @Override
    public void handleKeyReleasedEvent(String ke, boolean shiftDown) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyReleasedEvent(ke, shiftDown);
                }
            }
        }
    }

    @Override
    public void handleMousePressedEvent(String me, Vector3f mouseLoc) {
        if (visible) {
            if (flat) {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, mouseLoc.y - y, 0);
                Rectangle mRect = new Rectangle((int) adjLoc.x, (int) adjLoc.y, 1, 1);
                for (int a = 0; a < components.size(); a++) {
                    if (components.get(a).intersects(mRect)) {
                        components.get(a).handleMousePressedEvent(me, adjLoc);
                    }
                }
            } else {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, (int) (mouseLoc.y - (mouseLoc.z - getHeight()) + y), 0);
                Rectangle mRect = new Rectangle((int) adjLoc.x, (int) adjLoc.y, 1, 1);
                for (int a = 0; a < components.size(); a++) {
                    if (components.get(a).intersects(mRect)) {
                        components.get(a).handleMousePressedEvent(me, adjLoc);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        if (visible) {
            if (flat) {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, mouseLoc.y - y, 0);
                for (int a = 0; a < components.size(); a++) {
                    if (components.get(a).isFocused()) {
                        components.get(a).handleMouseReleasedEvent(me, adjLoc);
                    }
                }
            } else {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, (int) (mouseLoc.y - (mouseLoc.z - getHeight()) + y), 0);
                for (int a = 0; a < components.size(); a++) {
                    if (components.get(a).isFocused()) {
                        components.get(a).handleMouseReleasedEvent(me, adjLoc);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseMovedEvent(String me, Vector3f mouseLoc) {
        if (visible) {
            boolean foundOne = false;
            if (flat) {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, mouseLoc.y - y, 0);
                Rectangle mRect = new Rectangle((int) adjLoc.x, (int) adjLoc.y, 1, 1);
                for (int a = components.size() - 1; a >= 0; a--) {
                    if (components.get(a).isVisible()) {
                        if (components.get(a).intersects(mRect) && !foundOne) {
                            components.get(a).setFocused(true);
                            foundOne = true;
                            components.get(a).handleMouseMovedEvent(me, adjLoc);
                        } else {
                            components.get(a).setFocused(false);
                        }
                    } else {
                        components.get(a).setVisible(false);
                    }
                }
            } else {
                Vector3f adjLoc = new Vector3f(mouseLoc.x - x, (int) (mouseLoc.y - (mouseLoc.z - getHeight()) + y), 0);
                Rectangle mRect = new Rectangle((int) adjLoc.x, (int) adjLoc.y, 1, 1);
                for (int a = components.size() - 1; a >= 0; a--) {
                    if (components.get(a).isVisible()) {
                        if (components.get(a).intersects(mRect) && !foundOne) {
                            components.get(a).setFocused(true);
                            foundOne = true;
                            components.get(a).handleMouseMovedEvent(me, adjLoc);
                        } else {
                            components.get(a).setFocused(false);
                        }
                    } else {
                        components.get(a).setVisible(false);
                    }
                }
            }
        }
    }

    public Color getBackColor() {
        return backColor;
    }

    public void setBackColor(Color backColor) {
        this.backColor = backColor;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isFlat() {
        return flat;
    }

    public void setFlat(boolean flat) {
        this.flat = flat;
    }
}
