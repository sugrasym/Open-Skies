/*
 Copyright (c) 2012 Aaron Perkins

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

/*
 * Nathan Wiehoff
 * I have made some modifications to this class so that it can double as the
 * tracker for the player's ship in this game. Originally, I had another appstate
 * that did this however they conflicted a little bit and it made sense to just
 * combine them.
 */
package jmeplanet;

import celestial.Ship.Ship;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
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
import engine.AstralCamera;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PlanetAppState
 *
 */
public class PlanetAppState extends AbstractAppState implements Control {

    protected Application app;
    protected List<Planet> planets;
    protected Planet nearestPlanet;
    protected FilterPostProcessor nearFilter;
    protected FilterPostProcessor farFilter;
    protected FogFilter nearFog;
    protected FogFilter farFog;
    protected BloomFilter farBloom;
    protected Spatial scene;
    protected DirectionalLight sun;
    protected ViewPort nearViewPort;
    protected ViewPort farViewPort;
    protected Camera nearCam;
    protected Camera farCam;
    protected boolean shadowsEnabled;
    protected DirectionalLightShadowRenderer dlsr;
    private Ship cameraShip; //added to let this appstate double as the player camera
    private AstralCamera astralCamera;

    public PlanetAppState(Spatial scene, DirectionalLight sun) {
        this.scene = scene;
        this.sun = sun;
        this.planets = new ArrayList<Planet>();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = app;
        farViewPort = app.getViewPort();
        farCam = app.getCamera();

        nearCam = this.farCam.clone();
        nearCam.setFrustumPerspective(45f, (float) nearCam.getWidth() / nearCam.getHeight(), 1f, 310f);
        farCam.setFrustumPerspective(45f, (float) farCam.getWidth() / farCam.getHeight(), 300f, 1e7f);

        nearCam.setViewPort(0f, 1f, 0.0f, 1f);
        nearViewPort = this.app.getRenderManager().createMainView("NearView", nearCam);
        nearViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        nearViewPort.setClearFlags(false, true, true);
        nearViewPort.attachScene(this.scene);

        nearFilter = new FilterPostProcessor(app.getAssetManager());
        nearViewPort.addProcessor(nearFilter);

        nearFog = new FogFilter();
        //nearFilter.addFilter(nearFog);

        farFilter = new FilterPostProcessor(app.getAssetManager());
        farViewPort.addProcessor(farFilter);

        farFog = new FogFilter();
        farFilter.addFilter(farFog);

        farBloom = new BloomFilter();
        farBloom.setDownSamplingFactor(2);
        farBloom.setBlurScale(1.37f);
        farBloom.setExposurePower(3.30f);
        farBloom.setExposureCutOff(0.1f);
        farBloom.setBloomIntensity(1.45f);
        //farFilter.addFilter(farBloom);

        if (sun != null) {

            dlsr = new DirectionalLightShadowRenderer(app.getAssetManager(), 1024, 3);
            dlsr.setLight(sun);
            dlsr.setLambda(0.55f);
            dlsr.setShadowIntensity(0.6f);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
            dlsr.setShadowCompareMode(CompareMode.Hardware);
            dlsr.setShadowZExtend(100f);
            if (shadowsEnabled) {
                nearViewPort.addProcessor(dlsr);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void update(float tpf) {

        //if we have something to look at, center at it
        if (cameraShip != null) {
            if (astralCamera != null) {
                astralCamera.periodicUpdate(tpf);
            } else {
                //setup astral camera
                astralCamera = new AstralCamera(farCam);
                astralCamera.setTarget(cameraShip);
            }
        }
        //update short camera
        nearCam.setLocation(farCam.getLocation());
        nearCam.setRotation(farCam.getRotation());
        if (planets.size() > 0) {
            this.nearestPlanet = findNearestPlanet();

            for (Planet planet : this.planets) {
                planet.setCameraPosition(this.app.getCamera().getLocation());
            }
            updateFogAndBloom();
        } else {
            stopFogAndBloom();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    public void addPlanet(Planet planet) {
        this.planets.add(planet);
    }

    public List<Planet> getPlanets() {
        return this.planets;
    }

    public Planet getNearestPlanet() {
        return this.nearestPlanet;
    }

    public Vector3f getGravity() {
        Planet planet = getNearestPlanet();
        if (planet != null && planet.getPlanetToCamera() != null) {
            return planet.getPlanetToCamera().normalize().mult(-9.81f);
        }
        return Vector3f.ZERO;
    }

    public void setShadowsEnabled(boolean enabled) {
        this.shadowsEnabled = enabled;

        if (dlsr != null) {
            if (enabled) {
                nearViewPort.addProcessor(dlsr);
            } else {
                nearViewPort.removeProcessor(dlsr);
            }
        }
    }

    protected Planet findNearestPlanet() {
        Planet cPlanet = null;
        for (Planet planet : this.planets) {
            if (cPlanet == null || cPlanet.getDistanceToCamera() > planet.getDistanceToCamera()) {
                cPlanet = planet;
            }
        }
        return cPlanet;
    }

    protected void stopFogAndBloom() {
        nearFog.setEnabled(false);
        farFog.setEnabled(false);
        farBloom.setEnabled(true);
    }

    protected void updateFogAndBloom() {
        if (this.nearestPlanet == null) {
            return;
        }
        Planet planet = this.nearestPlanet;
        if (planet.getIsInOcean()) {
            // turn on underwater fogging
            nearFog.setFogColor(planet.getUnderwaterFogColor());
            nearFog.setFogDistance(planet.getUnderwaterFogDistance());
            nearFog.setFogDensity(planet.getUnderwaterFogDensity());
            nearFog.setEnabled(true);
            farFog.setFogColor(planet.getUnderwaterFogColor());
            farFog.setFogDistance(planet.getUnderwaterFogDistance());
            farFog.setFogDensity(planet.getUnderwaterFogDensity());
            farFog.setEnabled(true);
            farBloom.setEnabled(true);
        } else {
            if (planet.getIsInAtmosphere()) {
                // turn on atomosphere fogging
                nearFog.setFogColor(planet.getAtmosphereFogColor());
                nearFog.setFogDistance(planet.getAtmosphereFogDistance());
                nearFog.setFogDensity(planet.getAtmosphereFogDensity());
                nearFog.setEnabled(true);
                farFog.setFogColor(planet.getAtmosphereFogColor());
                farFog.setFogDistance(planet.getAtmosphereFogDistance());
                farFog.setFogDensity(planet.getAtmosphereFogDensity());
                farFog.setEnabled(true);
                farBloom.setEnabled(false);
            } else {
                // in space
                nearFog.setEnabled(false);
                farFog.setEnabled(false);
                farBloom.setEnabled(true);
            }
        }
    }

    public void removePlanet(Planet planet) {
        planets.remove(planet);
    }

    /*
     * I have added these to allow this appstate to also track a spatial since
     * it is already mucking around with the camera.
     */
    public Ship getCameraCenter() {
        return cameraShip;
    }

    public void setCameraShip(Ship cameraShip) {
        this.cameraShip = cameraShip;
        if (cameraShip != null) {
            cameraShip.getSpatial().addControl(this);
        }
    }

    public void freeCamera() {
        if (cameraShip != null) {
            if (cameraShip.getSpatial() != null) {
                cameraShip.getSpatial().removeControl(this);
            }
        }
        cameraShip = null;
        astralCamera = null;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return this;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        //
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        //
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        //
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        //
    }
}
