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

package lib;

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
