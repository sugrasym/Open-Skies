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
package app;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import engine.Core;
import jmeplanet.PlanetAppState;

/**
 * - What do you say to Goons? - Die.
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
        //set properties
        System.setProperty("sun.java2d.transaccel", "True");
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.setProperty("sun.java2d.d3d", "True");
            System.out.println("Running on " + System.getProperty("os.name") + " using DirectX");
        } else {
            System.setProperty("sun.java2d.opengl", "True");
            System.out.println("Running on " + System.getProperty("os.name") + " using OpenGL");
        }
        System.setProperty("sun.java2d.ddforcevram", "True");
        //start
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //register models
        assetManager.registerLoader(BlenderModelLoader.class, "blend");
        //init physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(Vector3f.ZERO);
        //setup planet generator
        planetAppState = new PlanetAppState(rootNode, null);
        stateManager.attach(planetAppState);
        //start engine
        core = new Core(rootNode, guiNode, bulletAppState, assetManager, planetAppState, inputManager, settings);
        //setup post processing
        fpp = new FilterPostProcessor(assetManager);
        /*bloom.setDownSamplingFactor(4.0f);
         bloom.setBloomIntensity(2.0f);
         bloom.setBlurScale(1.0f);*/
        viewPort.addProcessor(fpp);
        //remove cruft
        setDisplayFps(false);
        setDisplayStatView(false);
        flyCam.setMoveSpeed(40000);
        flyCam.setEnabled(false);
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
