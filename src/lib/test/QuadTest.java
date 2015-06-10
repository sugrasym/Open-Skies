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

package lib.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import engine.Core;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 * - What do you say to Goons? - Die.
 */
public class QuadTest extends SimpleApplication {

    public static void main(String[] args) {
        QuadTest app = new QuadTest();
        //setup joystick
        AppSettings set = new AppSettings(true);
        set.setUseJoysticks(true);
        set.setResolution(1024, 768);
        set.setVSync(true);
        app.setShowSettings(true);
        app.setSettings(set);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //create a canvas
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        //apply transform
        //draw crap
        g.setColor(Color.gray);
        g.setFont(new Font("System", Font.PLAIN, 12));
        g.fillRect(0, 0, 400, 300);
        g.setColor(Color.WHITE);
        g.drawString("Hello Computer", 100, 100);
        //create quad
        Quad qd_background = new Quad(400, 300);
        Geometry geo_background = new Geometry("Background", qd_background);
        //position quad
        geo_background.setLocalTranslation(100, 100, 10);
        //flip
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -img.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        img = op.filter(img, null);
        //map texture to quad
        Material mat_background = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture2D myTex = new Texture2D();
        AWTLoader awtLoader = new AWTLoader();
        myTex.setImage(awtLoader.load(img, false));
        mat_background.setTexture("ColorMap", myTex);
        geo_background.setMaterial(mat_background);
        //add quad to scene
        guiNode.attachChild(geo_background);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }
}
