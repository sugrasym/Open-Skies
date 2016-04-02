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

import celestial.Celestial;
import celestial.Ship.Ship;
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
import java.util.ArrayList;
import java.util.List;
import jmeplanet.Planet;

/**
 *
 * @author nwiehoff, Geoff Hibbert
 */
public class AstralCamera {

    //engine resources
    private final Camera appCam;
    protected FilterPostProcessor chaseFilter;
    protected FogFilter chaseFog;
    protected BloomFilter chaseBloom;
    protected ViewPort chaseViewPort;
    protected boolean shadowsEnabled = true;
    protected DirectionalLightShadowRenderer dlsr;
    private Celestial target;
    private ArrayList<TargetPlacement> cachedTargetPlacements;
    private RenderManager renderManager;
    private int trailingCount = 0;
    public static int TRAILING_FACTOR = 15;
    public static float DISTANCE_CHANGE_THRESHOLD = 1f;

    enum Mode {

        COCKPIT,
        NORMAL,
        RTS
    }
    Mode mode = Mode.NORMAL;

    public AstralCamera(Application app) {
        this.appCam = app.getCamera();
        this.renderManager = app.getRenderManager();

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

        cachedTargetPlacements = new ArrayList<>();
    }

    public void periodicUpdate(float tpf) {
        if (target != null) {
            if (mode == Mode.COCKPIT) {
                //TODO: Handle using a different cam for cockpit
            } else if (mode == Mode.NORMAL) {
                //continue to fill buffer if needed
                if (trailingCount < TRAILING_FACTOR) {
                    
                }

                Quaternion rotation = target.getPhysicsRotation();
                Vector3f lookAtUpVector = rotation.mult(Vector3f.UNIT_Y);

                //record position for camera to follow
                TargetPlacement newPlacement = new TargetPlacement(target.getLocation(), rotation);

                boolean increaseBuffer = false;
                if (cachedTargetPlacements.isEmpty()) {
                    increaseBuffer = true;
                } else {
                    float distanceFromLastPlacement = newPlacement.location.distance(cachedTargetPlacements.get(cachedTargetPlacements.size() - 1).location);
                    if (distanceFromLastPlacement > DISTANCE_CHANGE_THRESHOLD) {
                        increaseBuffer = true;
                    }
                }
                if (increaseBuffer){
                    cachedTargetPlacements.add(newPlacement);
                    trailingCount++;
                }
                
                if (trailingCount == TRAILING_FACTOR) {
                    //we've moved enough to matter
                    //get the oldest placement in buffer
                    TargetPlacement placementToLookAt = cachedTargetPlacements.remove(0);

                    //remove points of little change so after trailing we can start moving to behind the ship
                    boolean continueCulling = cachedTargetPlacements.size() > 1;
                    while (continueCulling && !cachedTargetPlacements.isEmpty()) {
                        if (placementToLookAt.location.distance(cachedTargetPlacements.get(0).location) < DISTANCE_CHANGE_THRESHOLD) {
                            cachedTargetPlacements.remove(0);
                        } else {
                            continueCulling = false;
                        }
                    }
                    trailingCount = cachedTargetPlacements.size();
                    appCam.setLocation(placementToLookAt.location);
                    lookAtUpVector = placementToLookAt.rotation.mult(Vector3f.UNIT_Y);

                } else {
                    //try to move to behind celestial
                    appCam.setLocation(target.getCameraRestPoint().interpolate(appCam.getLocation(), .5f));
                }
                appCam.lookAt(target.getLocation(), lookAtUpVector);
            } else if (mode == Mode.RTS) {
                //TODO: Handle rotations to camera and distance to make viewport look right
            }
        }
    }

    public Celestial getTarget() {
        return target;
    }

    public void setTarget(Celestial target) {
        freeCamera();
        this.target = target;
        trailingCount = 0;
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
