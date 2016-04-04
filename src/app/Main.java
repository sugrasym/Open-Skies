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

package app;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import com.jme3.system.AppSettings;
import engine.Core;
import jmeplanet.PlanetAppState;
import lib.astral.AstralIO;

/**
 *  Hour by hour, one more change 
 *  I'm sewing them together, take great pains 
 *      - Rarity
 */
public class Main extends SimpleApplication {
    //fpp

    FilterPostProcessor fpp;
    PlanetAppState planetAppState;
    //engine
    private BulletAppState bulletAppState;
    Core core;

    public static void main(String[] args) {
        Main app = new Main();
        //override settings for joystick
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        settings.setVSync(true);
        settings.setWidth(1440);
        settings.setHeight(900);
        settings.setTitle("Outlier: Open Skies");
        settings.setSettingsDialogImage("splash.png");
        settings.setMinResolution(800, 600);
        app.setSettings(settings);
        
        //set renderer properties
        String osName = System.getProperty("os.name");
        String renderMode = "DirectX";
        System.setProperty("sun.java2d.transaccel", "True");
        System.setProperty("sun.java2d.ddforcevram", "True");
        if (!osName.contains("Windows")) {
            System.setProperty("sun.java2d.opengl", "True");
            renderMode = "OpenGL";
        }
        System.out.println("Running on " + osName + " using " + renderMode);
        
        //start
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //setup game directory
        AstralIO.setupGameDir();
        
        //register models
        assetManager.registerLoader(BlenderModelLoader.class, "blend");
        
        //init physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(Vector3f.ZERO);

        //setup camera
        flyCam.setEnabled(false);
        
        //setup planet generator
        planetAppState = new PlanetAppState(rootNode, null);
        stateManager.attach(planetAppState);
        
        //start engine
        core = new Core(rootNode, guiNode, bulletAppState, assetManager, planetAppState, inputManager, settings, listener);
        
        //setup post processing
        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        //remove cruft
        setDisplayFps(false);
        setDisplayStatView(false);

        //prevent application from pausing when out of focus (I hate how X3TC does that!)
        setPauseOnLostFocus(false);
    }

    @Override
    public void simpleUpdate(float tpf) {
        core.periodicUpdate(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        core.render(rm);
    }
    
    @Override
    public void gainFocus() {
        core.setFocus(true);
    }
    
    @Override
    public void loseFocus() {
        core.setFocus(false);
    }
}
