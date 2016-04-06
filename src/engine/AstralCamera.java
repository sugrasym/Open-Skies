/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
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
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
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
    private Camera nearCam;
    private final Camera farCam;
    private final RenderManager renderManager;
    private final AssetManager AssetManager;

    //effects
    protected FilterPostProcessor nearFilter;
    protected FilterPostProcessor farFilter;
    protected BloomFilter bloom;
    protected DirectionalLightShadowRenderer dlsr;
    protected DirectionalLightShadowFilter dlsf;

    //viewports
    protected ViewPort farViewPort;
    protected ViewPort nearViewPort;

    //object being tracked
    private Celestial target;

    //properties
    protected boolean shadowsEnabled = false;
    protected boolean enabled = true;
    protected boolean inertia = true;

    protected Vector3f upVector;
    private Vector3f lastTargetVelocity = new Vector3f();

    enum Mode {

        COCKPIT,
        NORMAL,
        RTS
    }
    Mode mode = Mode.NORMAL;

    private ArrayList<TargetPlacement> cachedTargetPlacements;
    private int trailingCount = 0;
    private int trailingFactor = 0;
    public static int TRAILING_FACTOR = 20;

    public AstralCamera(Application app) {
        this.farCam = app.getCamera();
        this.renderManager = app.getRenderManager();
        this.farViewPort = app.getViewPort();
        this.AssetManager = app.getAssetManager();
        init();
    }

    public AstralCamera(Camera appCamera, RenderManager rm, ViewPort vp, AssetManager am) {
        this.farCam = appCamera;
        this.renderManager = rm;
        this.farViewPort = vp;
        this.AssetManager = am;
        init();
    }

    private void init() {
        this.upVector = farCam.getUp().clone();
        this.nearFilter = new FilterPostProcessor(AssetManager);
        this.farFilter = new FilterPostProcessor(AssetManager);

        farCam.setViewPort(0f, 1f, 0.0f, 1f);
        farViewPort = renderManager.createMainView("FarView", farCam);
        farViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);

        nearCam = this.farCam.clone();
        nearCam.setViewPort(0f, 1f, 0.0f, 1f);
        nearViewPort = renderManager.createMainView("NearView", nearCam);
        nearViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        nearViewPort.setClearFlags(false, true, true);

        farCam.setFrustumPerspective(45f, (float) farCam.getWidth() / farCam.getHeight(), 300f, 1e7f);
        nearCam.setFrustumPerspective(45f, (float) nearCam.getWidth() / nearCam.getHeight(), 1f, 310f);

        //near effects
        nearViewPort.addProcessor(nearFilter);

        //far effects
        bloom = new BloomFilter();
        bloom.setDownSamplingFactor(2);
        bloom.setBlurScale(1.37f);
        bloom.setExposurePower(3.30f);
        bloom.setExposureCutOff(0.1f);
        bloom.setBloomIntensity(1.45f);
        //farFilter.addFilter(bloom);
        farViewPort.addProcessor(farFilter);

        cachedTargetPlacements = new ArrayList<TargetPlacement>();
    }

    @Override
    public void update(float f) {
        if (target != null) {
            if (mode == Mode.COCKPIT) {
                //TODO: Handle using a different cam for cockpit
            } else if (mode == Mode.NORMAL) {
                Quaternion rotation = target.getPhysicsRotation().clone();
                Vector3f lookAtUpVector = rotation.mult(Vector3f.UNIT_Y).clone();
                Vector3f currentVelocity = target.getLinearVelocity().clone();

                //record position for camera to follow
                TargetPlacement newPlacement = new TargetPlacement(target.getCameraRestPoint().clone(), rotation.clone());
                cachedTargetPlacements.add(newPlacement);
                trailingCount++;

                if (trailingCount >= trailingFactor) {
                    //check if we are no longer accelerating
                    if ((lastTargetVelocity.subtract(currentVelocity).length()) == 0
                            && target.getAngularVelocity().length() < 0.2) {
                        if (trailingFactor > 1) {
                            cachedTargetPlacements.remove(0);
                            trailingFactor--;
                            trailingCount--;
                        }
                    } else {
                        //increase the "rope"
                        if (trailingFactor < TRAILING_FACTOR) {
                            trailingFactor++;
                        }
                    }

                    //we've moved enough to matter
                    //get the oldest placement in buffer
                    TargetPlacement placementToLookAt = cachedTargetPlacements.remove(0);
                    trailingCount--;

                    nearCam.setLocation(placementToLookAt.location.clone());
                    farCam.setLocation(placementToLookAt.location.clone());
                    lookAtUpVector = rotation.mult(Vector3f.UNIT_Y);
                }

                lastTargetVelocity = currentVelocity;
                farCam.lookAt(target.getLineOfSightPoint(), lookAtUpVector.clone());
                nearCam.lookAt(target.getLineOfSightPoint(), lookAtUpVector.clone());
            } else if (mode == Mode.RTS) {
                //TODO: Handle rotations to camera and distance to make viewport look right
            }
        }
    }

    @Override
    public void setSpatial(Spatial sptl) {
        Vector3f currentPosition = new Vector3f(target.getSpatial().getWorldTranslation());
        nearCam.setLocation(currentPosition);
        farCam.setLocation(currentPosition);
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

    public Celestial getTarget() {
        return target;
    }

    public void setTarget(Celestial target) {
        freeCamera();
        this.target = target;
        this.target.getSpatial().addControl(this);
    }

    public void setSun(DirectionalLight sun, AssetManager assetManager) {
        if (shadowsEnabled) {
            if (dlsr != null) {
                nearViewPort.removeProcessor(dlsr);
                farViewPort.removeProcessor(dlsr);
            }
            dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 3);

            dlsr.setLight(sun);
            dlsr.setLambda(0.55f);
            dlsr.setShadowIntensity(0.6f);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
            dlsr.setShadowCompareMode(CompareMode.Hardware);
            dlsr.setShadowZExtend(100f);

            nearViewPort.addProcessor(dlsr);

            if (dlsf != null) {
                nearFilter.removeFilter(dlsf);
            }
            dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 3);
            dlsf.setLight(sun);
            dlsf.setEnabled(true);

            nearFilter.addFilter(dlsf);
            farFilter.addFilter(dlsf);
        }
    }

    public void updateFogAndBloom(Planet planet) {
        if (planet.getIsInOcean()) {
            bloom.setEnabled(true);
        } else {
            if (planet.getIsInAtmosphere()) {
                bloom.setEnabled(false);
            } else {
                // in space
                bloom.setEnabled(true);
            }
        }
    }

    public void stopFogAndBloom() {
        bloom.setEnabled(true);
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
        if (target != null && target.getSpatial() != null) {
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
        farViewPort.attachScene(scene);
        nearViewPort.attachScene(scene);
    }

    /**
     * Return the enabled/disabled state of the camera
     *
     * @return true if the camera is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the camera
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * clone this camera for a spatial
     *
     * @param spatial
     * @return
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        AstralCamera ac = new AstralCamera(this.farCam, this.renderManager, this.farViewPort, this.AssetManager);
        return ac;
    }

    /**
     * Sets the up vector of the camera used for the lookAt on the target
     *
     * @param up
     */
    public void setUpVector(Vector3f up) {
        upVector = up;
    }

    /**
     * Returns the up vector of the camera used for the lookAt on the target
     *
     * @return
     */
    public Vector3f getUpVector() {
        return upVector;
    }
}
