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
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
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

    protected Spatial scene;
    protected DirectionalLight sun;
    protected Ship playerShip;

    private AstralCamera astralCamera;

    public PlanetAppState(Spatial scene, DirectionalLight sun) {
        this.scene = scene;
        this.sun = sun;
        this.planets = new ArrayList<>();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = app;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void update(float tpf) {

        if (astralCamera != null) {
            if (sun != null) {
                astralCamera.setSun(sun, app.getAssetManager());
            }
        } else {
            //setup astral camera
            astralCamera = new AstralCamera(this.app);
            astralCamera.attachScene(scene);
        }
        
        if (planets.size() > 0) {
            this.nearestPlanet = findNearestPlanet();

            for (Planet planet : this.planets) {
                planet.setCameraPosition(astralCamera.getLocation());
            }

            if (this.nearestPlanet != null) {
                astralCamera.updateFogAndBloom(this.nearestPlanet);
            } else {
                astralCamera.stopFogAndBloom();
            }
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

    protected Planet findNearestPlanet() {
        Planet cPlanet = null;
        for (Planet planet : this.planets) {
            if (cPlanet == null || cPlanet.getDistanceToCamera() > planet.getDistanceToCamera()) {
                cPlanet = planet;
            }
        }
        return cPlanet;
    }

    public void removePlanet(Planet planet) {
        planets.remove(planet);
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

    public AstralCamera getAstralCamera() {
        return astralCamera;
    }

    public void setAstralCamera(AstralCamera astralCamera) {
        this.astralCamera = astralCamera;
    }
}
