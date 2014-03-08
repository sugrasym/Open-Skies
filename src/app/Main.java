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
import com.jme3.post.filters.BloomFilter;
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
        core = new Core(rootNode, guiNode, bulletAppState, assetManager, planetAppState);
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
    }

    @Override
    public void simpleUpdate(float tpf) {
        core.periodicUpdate(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        core.render(rm);
    }
}
