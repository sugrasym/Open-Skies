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
 * Formerly a camera controller, now provides calculations to support
 * PlanetAppState as a camera controller.
 */
package engine;

import celestial.Ship.Ship;
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.input.ChaseCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import jmeplanet.Planet;

/**
 *
 * @author nwiehoff
 */
public class AstralCamera {

    //engine resources
    private final Camera appCam;
    protected FilterPostProcessor chaseFilter;
    protected FogFilter chaseFog;
    protected BloomFilter chaseBloom;
    protected ViewPort chaseViewPort;
    protected ChaseCamera chaseCam;
    protected boolean shadowsEnabled;
    protected DirectionalLightShadowRenderer dlsr;
    private Spatial target;
    private RenderManager renderManager;

    enum Mode {

        COCKPIT,
        NORMAL,
        RTS
    }
    Mode mode = Mode.NORMAL;

    public AstralCamera(Application app) {
        this.appCam = app.getCamera();
        this.renderManager = app.getRenderManager();
        
        chaseCam = new ChaseCamera(appCam, app.getInputManager());
        chaseCam.setSmoothMotion(true);
        chaseCam.setChasingSensitivity(5f);
        chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(4));
        chaseCam.setDefaultHorizontalRotation(FastMath.DEG_TO_RAD * 90);
        chaseCam.setRotationSensitivity(.08f);
        chaseCam.setMinVerticalRotation(.5f);
        chaseCam.setMaxVerticalRotation(1f);
        chaseViewPort = app.getViewPort();
        
        appCam.setViewPort(0f, 1f, 0f, 1f);
        appCam.setFrustumPerspective(45f, (float) appCam.getWidth() / appCam.getHeight(), 1f, 1e7f);
        chaseFilter = new FilterPostProcessor(app.getAssetManager());
        chaseViewPort.addProcessor(chaseFilter);

        chaseFog = new FogFilter();
        chaseFilter.addFilter(chaseFog);

        chaseBloom = new BloomFilter();
        chaseBloom.setDownSamplingFactor(2);
        chaseBloom.setBlurScale(1.37f);
        chaseBloom.setExposurePower(3.30f);
        chaseBloom.setExposureCutOff(0.1f);
        chaseBloom.setBloomIntensity(1.45f);
        //chaseFilter.addFilter(chaseBloom);
    }

    public void periodicUpdate(float tpf) {
        if (target != null) {
            if (mode == Mode.COCKPIT) {
                //TODO: Handle using a different cam for cockpit
            }
            else if (mode==Mode.NORMAL){
                
            }
            else if (mode == Mode.RTS) {
                //TODO: Handle rotations to camera and distance to make viewport look right
            }
        }
    }

    public Spatial getTarget() {
        return target;
    }

    public void setTarget(Spatial target) {
        freeCamera();
        target.addControl(chaseCam);
        this.target = target;
    }

    public void setTargetShip(Ship target) {
        setTarget(target.getSpatial());
    }

    public void addLight(DirectionalLight light, AssetManager assetManager) {
        dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 3);
        dlsr.setLight(light);
        dlsr.setLambda(0.55f);
        dlsr.setShadowIntensity(0.6f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        dlsr.setShadowCompareMode(CompareMode.Hardware);
        dlsr.setShadowZExtend(100f);
        if (shadowsEnabled) {
            chaseViewPort.addProcessor(dlsr);
        }
    }

    public void updateFogAndBloom(Planet planet) {
        if (planet.getIsInOcean()) {
            // turn on underwater fogging
            chaseFog.setFogColor(planet.getUnderwaterFogColor());
            chaseFog.setFogDistance(planet.getUnderwaterFogDistance());
            chaseFog.setFogDensity(planet.getUnderwaterFogDensity());
            chaseFog.setEnabled(true);
            chaseBloom.setEnabled(true);
        } else {
            if (planet.getIsInAtmosphere()) {
                // turn on atomosphere fogging
                chaseFog.setFogColor(planet.getAtmosphereFogColor());
                chaseFog.setFogDistance(planet.getAtmosphereFogDistance());
                chaseFog.setFogDensity(planet.getAtmosphereFogDensity());
                chaseFog.setEnabled(true);
                chaseBloom.setEnabled(false);
            } else {
                // in space
                chaseFog.setEnabled(false);
                chaseBloom.setEnabled(true);
            }
        }
    }

    public void stopFogAndBloom() {
        chaseFog.setEnabled(false);
        chaseBloom.setEnabled(true);
    }

    public void setShadowsEnabled(boolean enabled) {
        this.shadowsEnabled = enabled;

        if (dlsr != null) {
            if (enabled) {
                chaseViewPort.addProcessor(dlsr);
            } else {
                chaseViewPort.removeProcessor(dlsr);
            }
        }
    }

    public void freeCamera() {
        if (target != null) {
            target.removeControl(chaseCam);
            target = null;
        }
    }

    public Vector3f getScreenCoordinates(Vector3f position) {
        return appCam.getScreenCoordinates(position);
    }

    public Vector3f getLocation() {
        return appCam.getLocation();
    }

    public Vector3f getDirection() {
        return appCam.getDirection();
    }

    public Camera getCamera() {
        return appCam;
    }

    public void attachScene(Spatial scene) {
        appCam.setViewPort(0f, 1f, 0.0f, 1f);
        chaseViewPort = renderManager.createMainView("ChaseCam", appCam);
        chaseViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        chaseViewPort.setClearFlags(false, true, true);
        chaseViewPort.attachScene(scene);
    }
}
