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
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import java.io.IOException;
import java.util.ArrayList;
import jmeplanet.Planet;

/**
 *
 * @author nwiehoff, Geoff Hibbert
 */
public class AstralCamera implements Control {

    //engine resources
    private final Camera farCam;
    private final Camera nearCam;
    protected FilterPostProcessor chaseFilter;
    protected FogFilter chaseFog;
    protected BloomFilter chaseBloom;
    protected ViewPort farViewPort;
    protected ViewPort nearViewPort;
    protected boolean shadowsEnabled = true;
    protected DirectionalLightShadowRenderer dlsr;
    private Celestial target;
    private final ArrayList<TargetPlacement> cachedTargetPlacements;
    private final RenderManager renderManager;
    private int trailingCount = 0;
    private int trailingFactor = 0;
    private Vector3f lastTargetVelocity = new Vector3f();
    public static int TRAILING_FACTOR = 10;

    @Override
    public void update(float f) {
        if (target != null) {
            if (mode == Mode.COCKPIT) {
                //TODO: Handle using a different cam for cockpit
            } else if (mode == Mode.NORMAL) {
                Quaternion rotation = target.getPhysicsRotation();
                Vector3f lookAtUpVector = rotation.mult(Vector3f.UNIT_Y);
                Vector3f cameraLocation = farCam.getLocation();
                Vector3f restPointLocation = target.getCameraRestPoint();
                Vector3f currentVelocity = target.getLinearVelocity().clone();

                //record position for camera to follow
                TargetPlacement newPlacement = new TargetPlacement(target.getCameraRestPoint().clone(), rotation.clone());
                cachedTargetPlacements.add(newPlacement);

                if (trailingCount == trailingFactor) {
                    //check if we are no longer accelerating
                    if (lastTargetVelocity.subtract(currentVelocity).length() == 0) {
                        if (trailingFactor > 1) {
                            cachedTargetPlacements.remove(0);
                            trailingFactor--;
                            trailingCount--;
                        }
                    } else {
                        //increase the "rope"
                        if(trailingFactor < TRAILING_FACTOR) trailingFactor++;
                    }

                    //we've moved enough to matter
                    //get the oldest placement in buffer
                    TargetPlacement placementToLookAt = cachedTargetPlacements.remove(0);

                    trailingCount = cachedTargetPlacements.size();
                    nearCam.setLocation(nearCam.getLocation().interpolate(placementToLookAt.location, f));
                    farCam.setLocation(farCam.getLocation().interpolate(placementToLookAt.location, f));
                    lookAtUpVector = rotation.mult(Vector3f.UNIT_Y);

                } else {
                    //try to move to behind celestial
                    nearCam.setLocation(target.getCameraRestPoint().interpolate(nearCam.getLocation(), .5f));
                    farCam.setLocation(target.getCameraRestPoint().interpolate(farCam.getLocation(), .5f));
                    trailingCount++;
                }

                lastTargetVelocity = currentVelocity;
                farCam.lookAt(target.getLineOfSightPoint(), lookAtUpVector);
                nearCam.lookAt(target.getLineOfSightPoint(), lookAtUpVector);
            } else if (mode == Mode.RTS) {
                //TODO: Handle rotations to camera and distance to make viewport look right
            }
        }
    }

    @Override
    public Control cloneForSpatial(Spatial sptl) {
        return this;
    }

    @Override
    public void setSpatial(Spatial sptl) {
        //
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        //
    }

    @Override
    public void write(JmeExporter je) throws IOException {
        //
    }

    @Override
    public void read(JmeImporter ji) throws IOException {
        //
    }

    private float getShiftAmount(float inputAmount) {
        float shift;
        float sign = Math.signum(inputAmount) * -1;
        float amount = Math.abs(inputAmount);
        if (amount > 20000) {
            shift = 10000 * sign;
        } else if (amount > 10000) {
            shift = 2000 * sign;
        } else if (amount > 5000) {
            shift = 500 * sign;
        } else if (amount > 2000) {
            shift = 200 * sign;
        } else if (amount > 1000) {
            shift = 100 * sign;
        } else if (amount > 100) {
            shift = 20 * sign;
        } else if (amount > 10) {
            shift = 5 * sign;
        } else {
            shift = 1 * sign;
        }

        return shift;
    }

    enum Mode {

        COCKPIT,
        NORMAL,
        RTS
    }
    Mode mode = Mode.NORMAL;

    public AstralCamera(Application app) {
        this.farCam = app.getCamera();
        this.renderManager = app.getRenderManager();

        farViewPort = app.getViewPort();

        farCam.setViewPort(0f, 1f, 0f, 1f);
        nearCam = this.farCam.clone();

        farCam.setFrustumPerspective(45f, (float) farCam.getWidth() / farCam.getHeight(), 300f, 1e7f);
        nearCam.setFrustumPerspective(45f, (float) nearCam.getWidth() / nearCam.getHeight(), 1f, 310f);

        chaseFilter = new FilterPostProcessor(app.getAssetManager());
        farViewPort.addProcessor(chaseFilter);

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

    public Celestial getTarget() {
        return target;
    }

    public void setTarget(Celestial target) {
        freeCamera();
        this.target = target;
        this.target.getSpatial().addControl(this);
        trailingCount = 0;
        trailingFactor = TRAILING_FACTOR;
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
            farViewPort.addProcessor(dlsr);
            nearViewPort.addProcessor(dlsr);
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
                farViewPort.addProcessor(dlsr);
                nearViewPort.addProcessor(dlsr);
            } else {
                farViewPort.removeProcessor(dlsr);
                nearViewPort.removeProcessor(dlsr);
            }
        }
    }

    public void freeCamera() {
        if (target != null) {
            target.getSpatial().removeControl(this);
            target = null;
        }
    }

    public Vector3f getScreenCoordinates(Vector3f position) {
        return farCam.getScreenCoordinates(position);
    }

    public Vector3f getLocation() {
        return farCam.getLocation();
    }

    public Vector3f getDirection() {
        return farCam.getDirection();
    }

    public void attachScene(Spatial scene) {
        farCam.setViewPort(0f, 1f, 0.0f, 1f);
        farViewPort = renderManager.createMainView("FarView", farCam);
        farViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        farViewPort.attachScene(scene);

        nearCam.setViewPort(0f, 1f, 0.0f, 1f);
        nearViewPort = renderManager.createMainView("NearView", nearCam);
        nearViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        nearViewPort.setClearFlags(false, true, true);
        nearViewPort.attachScene(scene);
    }
}
