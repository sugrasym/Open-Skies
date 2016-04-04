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
import com.jme3.math.FastMath;
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
    protected FilterPostProcessor chaseFilter;
    protected FogFilter chaseFog;
    protected BloomFilter chaseBloom;
    protected DirectionalLightShadowRenderer dlsr;
    
    //viewports
    protected ViewPort farViewPort;
    protected ViewPort nearViewPort;
    
    //object being tracked
    private Celestial target;
    
    //properties
    protected boolean shadowsEnabled = true;
    protected boolean enabled = true;
    protected boolean smoothMotion = true;
    
    protected float minDistance = 1.0f;
    protected float maxDistance = 40.0f;
    protected float offsetDistance = 0.002f;
    protected float distance = 20;
    protected float distanceLerpFactor = 0;
    
    protected boolean rotating = false;
    protected float rotation = 0;
    protected float rotationSpeed = 1.0f;
    protected float rotationSensitivity = 5f;
    protected float rotationLerpFactor = 0;
    protected boolean veryCloseRotation = true;
    protected boolean canRotate;
    
    protected boolean trailing = false;
    protected boolean trailingEnabled = true;
    protected float trailingRotationInertia = 0.05f;
    protected float trailingSensitivity = 0.5f;
    protected float trailingLerpFactor = 0;
    
    protected boolean chasing = false;
    protected float chasingSensitivity = 5f;
    
    protected Vector3f initialUpVec;
    protected Vector3f lookAtOffset = new Vector3f(0, 0, 0);
    
    protected boolean vRotating = false;
    protected float vRotation = FastMath.PI / 6;
    protected float vRotationLerpFactor = 0;
    protected float minVRotation = 0.00f;
    protected float maxVRotation = FastMath.PI / 2;
    
    protected boolean zRotating = false;
    protected float zRotationLerpFactor;
    protected float zRotation;
    protected float maxZRotation;
    protected float minZRotation;
    
    protected boolean zoomin;
    protected boolean zooming = false;
    protected float zoomSensitivity = 2f;
    
    protected Vector3f prevPos;
    protected final Vector3f pos = new Vector3f();
    protected Vector3f temp = new Vector3f(0, 0, 0);
        
    protected float targetVRotation = vRotation;
    protected float targetZRotation = zRotation;
    protected float targetRotation = rotation;
    protected float targetDistance = distance;
    protected boolean targetMoves = false;
    protected final Vector3f targetDir = new Vector3f();
    protected Vector3f targetLocation = new Vector3f(0, 0, 0);
    protected float previousTargetRotation;
        
    enum Mode {

        COCKPIT,
        NORMAL,
        RTS
    }
    Mode mode = Mode.NORMAL;

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
        this.initialUpVec = farCam.getUp().clone();
        this.chaseFilter = new FilterPostProcessor(AssetManager);
        farCam.setViewPort(0f, 1f, 0f, 1f);
        nearCam = this.farCam.clone();

        farCam.setFrustumPerspective(45f, (float) farCam.getWidth() / farCam.getHeight(), 300f, 1e7f);
        nearCam.setFrustumPerspective(45f, (float) nearCam.getWidth() / nearCam.getHeight(), 1f, 310f);

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

    }

    @Override
    public void update(float tpf) {
        if (enabled && target != null) {
            if (mode == Mode.COCKPIT) {
                //TODO: Handle using a different cam for cockpit
            } else if (mode == Mode.NORMAL) {
                targetLocation.set(target.getSpatial().getWorldTranslation()).addLocal(lookAtOffset);
                if (smoothMotion) {
                    //computation of target direction
                    targetDir.set(targetLocation).subtractLocal(prevPos);
                    float dist = targetDir.length();

                    //Low pass filtering on the target postition to avoid shaking when physics are enabled.
                    if (offsetDistance < dist) {
                        //target moves, start chasing.
                        chasing = true;
                        //target moves, start trailing if it has to.
                        if (trailingEnabled) {
                            trailing = true;
                        }
                        //target moves...
                        targetMoves = true;
                    } else {
                        //if target was moving, we compute a slight offset in rotation to avoid a rought stop of the cam
                        //We do not if the player is rotationg the cam
                        if (targetMoves && !canRotate) {
                            if (targetRotation - rotation > trailingRotationInertia) {
                                targetRotation = rotation + trailingRotationInertia;
                            } else if (targetRotation - rotation < -trailingRotationInertia) {
                                targetRotation = rotation - trailingRotationInertia;
                            }
                        }
                        //Target stops
                        targetMoves = false;
                    }

                    //the user is rotating the cam by dragging the mouse
                    if (canRotate) {
                        //reseting the trailing lerp factor
                        trailingLerpFactor = 0;
                        //stop trailing user has the control
                        trailing = false;
                    }

                    if (trailingEnabled && trailing) {
                        if (targetMoves) {
                            //computation if the inverted direction of the target
                            Vector3f a = targetDir.negate().normalizeLocal();
                            //the x unit vector
                            Vector3f b = Vector3f.UNIT_X;
                            //computation of the rotation angle between the x axis and the trail
                            if (targetDir.z > 0) {
                                targetRotation = FastMath.TWO_PI - FastMath.acos(a.dot(b));
                            } else {
                                targetRotation = FastMath.acos(a.dot(b));
                            }
                            //the z unit vector
                            Vector3f c = Vector3f.UNIT_Z;
                            //computation of the rotation angle between the z axis and the trail
                            if (targetDir.y > 0) {
                                targetRotation = FastMath.TWO_PI - FastMath.acos(a.dot(c));
                            } else {
                                targetRotation = FastMath.acos(a.dot(c));
                            }
                            
                            if (targetRotation - rotation > FastMath.PI || targetRotation - rotation < -FastMath.PI) {
                                targetRotation -= FastMath.TWO_PI;
                            }

                            //if there is an important change in the direction while trailing reset of the lerp factor to avoid jumpy movements
                            if (targetRotation != previousTargetRotation && FastMath.abs(targetRotation - previousTargetRotation) > FastMath.PI / 8) {
                                trailingLerpFactor = 0;
                            }
                            previousTargetRotation = targetRotation;
                        }
                        //computing lerp factor
                        trailingLerpFactor = Math.min(trailingLerpFactor + tpf * tpf * trailingSensitivity, 1);
                        //computing rotation by linear interpolation
                        rotation = FastMath.interpolateLinear(trailingLerpFactor, rotation, targetRotation);

                        //if the rotation is near the target rotation we're good, that's over
                        if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                            trailing = false;
                            trailingLerpFactor = 0;
                        }
                    }

                    //linear interpolation of the distance while chasing
                    if (chasing) {
                        distance = temp.set(targetLocation).subtractLocal(nearCam.getLocation()).length();
                        distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * chasingSensitivity * 0.05f), 1);
                        distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);
                        if (targetDistance + 0.01f >= distance && targetDistance - 0.01f <= distance) {
                            distanceLerpFactor = 0;
                            chasing = false;
                        }
                    }

                    //linear interpolation of the distance while zooming
                    if (zooming) {
                        distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * zoomSensitivity), 1);
                        distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);
                        if (targetDistance + 0.1f >= distance && targetDistance - 0.1f <= distance) {
                            zooming = false;
                            distanceLerpFactor = 0;
                        }
                    }

                    //linear interpolation of the rotation while rotating horizontally
                    if (rotating) {
                        rotationLerpFactor = Math.min(rotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                        rotation = FastMath.interpolateLinear(rotationLerpFactor, rotation, targetRotation);
                        if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                            rotating = false;
                            rotationLerpFactor = 0;
                        }
                    }

                    //linear interpolation of the rotation while rotating vertically
                    if (vRotating) {
                        vRotationLerpFactor = Math.min(vRotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                        vRotation = FastMath.interpolateLinear(vRotationLerpFactor, vRotation, targetVRotation);
                        if (targetVRotation + 0.01f >= vRotation && targetVRotation - 0.01f <= vRotation) {
                            vRotating = false;
                            vRotationLerpFactor = 0;
                        }
                    }

                    //linear interpolation of the rotation while rotating on z
                    if (zRotating) {
                        zRotationLerpFactor = Math.min(zRotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                        zRotation = FastMath.interpolateLinear(zRotationLerpFactor, zRotation, targetZRotation);
                        if (targetZRotation + 0.01f >= zRotation && targetZRotation - 0.01f <= zRotation) {
                            zRotating = false;
                            zRotationLerpFactor = 0;
                        }
                    }
                    //computing the position
                    computePosition();
                    //setting the position at last
                    nearCam.setLocation(pos.addLocal(lookAtOffset));
                    farCam.setLocation(pos.addLocal(lookAtOffset));
                } else {
                    //easy no smooth motion
                    vRotation = targetVRotation;
                    rotation = targetRotation;
                    distance = targetDistance;
                    computePosition();
                    nearCam.setLocation(pos.addLocal(lookAtOffset));
                    farCam.setLocation(pos.addLocal(lookAtOffset));
                }
                
                
                //keeping track on the previous position of the target
                prevPos.set(targetLocation);
                
                
                farCam.lookAt(targetLocation, initialUpVec);
                nearCam.lookAt(targetLocation, initialUpVec);
            } else if (mode == Mode.RTS) {
                //TODO: Handle rotations to camera and distance to make viewport look right
            }
        }
    }

    @Override
    public void setSpatial(Spatial sptl) {
        computePosition();
        prevPos = new Vector3f(target.getSpatial().getWorldTranslation());
        nearCam.setLocation(pos);
        farCam.setLocation(pos);
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

    public Celestial getTarget() {
        return target;
    }

    public void setTarget(Celestial target) {
        freeCamera();
        this.target = target;
        this.target.getSpatial().addControl(this);
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
            target.getCameraRestPoint().detachAllChildren();
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

    protected void computePosition() {

        float hDistance = (distance) * FastMath.sin((FastMath.PI / 2) - vRotation);
        pos.set(hDistance * FastMath.cos(rotation), (distance) * FastMath.sin(vRotation), hDistance * FastMath.sin(rotation));
        pos.addLocal(target.getSpatial().getWorldTranslation());
    }

    //rotate the camera around the target on the horizontal plane
    protected void rotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        rotating = true;
        targetRotation += value * rotationSpeed;

    }

    //move the camera toward or away the target
    protected void zoomCamera(float value) {
        if (!enabled) {
            return;
        }

        zooming = true;
        targetDistance += value * zoomSensitivity;
        if (targetDistance > maxDistance) {
            targetDistance = maxDistance;
        }
        if (targetDistance < minDistance) {
            targetDistance = minDistance;
        }
        if (veryCloseRotation) {
            if ((targetVRotation < minVRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVRotation;
            }
        }
    }

    //rotate the camera around the target on the vertical plane
    protected void vRotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        vRotating = true;
        float lastGoodRot = targetVRotation;
        targetVRotation += value * rotationSpeed;
        if (targetVRotation > maxVRotation) {
            targetVRotation = lastGoodRot;
        }
        if (veryCloseRotation) {
            if ((targetVRotation < minVRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVRotation;
            } else if (targetVRotation < -FastMath.DEG_TO_RAD * 90) {
                targetVRotation = lastGoodRot;
            }
        } else {
            if ((targetVRotation < minVRotation)) {
                targetVRotation = lastGoodRot;
            }
        }
    }

    //rotate the camera around the target on the z plane
    protected void zRotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        zRotating = true;
        float lastGoodRot = targetZRotation;
        targetZRotation += value * rotationSpeed;
        if (targetZRotation > maxZRotation) {
            targetZRotation = lastGoodRot;
        }
        if (veryCloseRotation) {
            if ((targetZRotation < minZRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetZRotation = minZRotation;
            } else if (targetZRotation < -FastMath.DEG_TO_RAD * 90) {
                targetZRotation = lastGoodRot;
            }
        } else {
            if ((targetZRotation < minZRotation)) {
                targetZRotation = lastGoodRot;
            }
        }
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
        if (!enabled) {
            canRotate = false; // reset this flag in-case it was on before
        }
    }

    /**
     * Returns the max zoom distance of the camera (default is 40)
     *
     * @return maxDistance
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the max zoom distance of the camera (default is 40)
     *
     * @param maxDistance
     */
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        if (maxDistance < distance) {
            zoomCamera(maxDistance - distance);
        }
    }

    /**
     * Returns the min zoom distance of the camera (default is 1)
     *
     * @return minDistance
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the min zoom distance of the camera (default is 1)
     */
    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
        if (minDistance > distance) {
            zoomCamera(distance - minDistance);
        }
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
        ac.setMaxDistance(getMaxDistance());
        ac.setMinDistance(getMinDistance());
        return ac;
    }

    /**
     * @return The maximal vertical rotation angle in radian of the camera
     * around the target
     */
    public float getMaxVerticalRotation() {
        return maxVRotation;
    }

    /**
     * Sets the maximal vertical rotation angle in radian of the camera around
     * the target. Default is Pi/2;
     *
     * @param maxVerticalRotation
     */
    public void setMaxVerticalRotation(float maxVerticalRotation) {
        this.maxVRotation = maxVerticalRotation;
    }

    /**
     *
     * @return The minimal vertical rotation angle in radian of the camera
     * around the target
     */
    public float getMinVerticalRotation() {
        return minVRotation;
    }

    /**
     * Sets the minimal vertical rotation angle in radian of the camera around
     * the target default is 0;
     *
     * @param minHeight
     */
    public void setMinVerticalRotation(float minHeight) {
        this.minVRotation = minHeight;
    }

    /**
     * @return True is smooth motion is enabled for this chase camera
     */
    public boolean isSmoothMotion() {
        return smoothMotion;
    }

    /**
     * Enables smooth motion for this chase camera
     *
     * @param smoothMotion
     */
    public void setSmoothMotion(boolean smoothMotion) {
        this.smoothMotion = smoothMotion;
    }

    /**
     * returns the chasing sensitivity
     *
     * @return
     */
    public float getChasingSensitivity() {
        return chasingSensitivity;
    }

    /**
     *
     * Sets the chasing sensitivity, the lower the value the slower the camera
     * will follow the target when it moves default is 5 Only has an effect if
     * smoothMotion is set to true and trailing is enabled
     *
     * @param chasingSensitivity
     */
    public void setChasingSensitivity(float chasingSensitivity) {
        this.chasingSensitivity = chasingSensitivity;
    }

    /**
     * Returns the rotation sensitivity
     *
     * @return
     */
    public float getRotationSensitivity() {
        return rotationSensitivity;
    }

    /**
     * Sets the rotation sensitivity, the lower the value the slower the camera
     * will rotates around the target when draging with the mouse default is 5,
     * values over 5 should have no effect. If you want a significant slow down
     * try values below 1. Only has an effect if smoothMotion is set to true
     *
     * @param rotationSensitivity
     */
    public void setRotationSensitivity(float rotationSensitivity) {
        this.rotationSensitivity = rotationSensitivity;
    }

    /**
     * returns true if the trailing is enabled
     *
     * @return
     */
    public boolean isTrailingEnabled() {
        return trailingEnabled;
    }

    /**
     * Enable the camera trailing : The camera smoothly go in the targets trail
     * when it moves. Only has an effect if smoothMotion is set to true
     *
     * @param trailingEnabled
     */
    public void setTrailingEnabled(boolean trailingEnabled) {
        this.trailingEnabled = trailingEnabled;
    }

    /**
     *
     * returns the trailing rotation inertia
     *
     * @return
     */
    public float getTrailingRotationInertia() {
        return trailingRotationInertia;
    }

    /**
     * Sets the trailing rotation inertia : default is 0.1. This prevent the
     * camera to roughly stop when the target stops moving before the camera
     * reached the trail position. Only has an effect if smoothMotion is set to
     * true and trailing is enabled
     *
     * @param trailingRotationInertia
     */
    public void setTrailingRotationInertia(float trailingRotationInertia) {
        this.trailingRotationInertia = trailingRotationInertia;
    }

    /**
     * returns the trailing sensitivity
     *
     * @return
     */
    public float getTrailingSensitivity() {
        return trailingSensitivity;
    }

    /**
     * Only has an effect if smoothMotion is set to true and trailing is enabled
     * Sets the trailing sensitivity, the lower the value, the slower the camera
     * will go in the target trail when it moves. default is 0.5;
     *
     * @param trailingSensitivity
     */
    public void setTrailingSensitivity(float trailingSensitivity) {
        this.trailingSensitivity = trailingSensitivity;
    }

    /**
     * returns the zoom sensitivity
     *
     * @return
     */
    public float getZoomSensitivity() {
        return zoomSensitivity;
    }

    /**
     * Sets the zoom sensitivity, the lower the value, the slower the camera
     * will zoom in and out. default is 2.
     *
     * @param zoomSensitivity
     */
    public void setZoomSensitivity(float zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    /**
     * Returns the rotation speed when the mouse is moved.
     *
     * @return the rotation speed when the mouse is moved.
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Sets the rotate amount when user moves his mouse, the lower the value,
     * the slower the camera will rotate. default is 1.
     *
     * @param rotationSpeed Rotation speed on mouse movement, default is 1.
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Sets the default distance at start of applicaiton
     *
     * @param defaultDistance
     */
    public void setDefaultDistance(float defaultDistance) {
        distance = defaultDistance;
        targetDistance = distance;
    }

    /**
     * sets the default horizontal rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    public void setDefaultHorizontalRotation(float angleInRad) {
        rotation = angleInRad;
        targetRotation = angleInRad;
    }

    /**
     * sets the default vertical rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    public void setDefaultVerticalRotation(float angleInRad) {
        vRotation = angleInRad;
        targetVRotation = angleInRad;
    }

    /**
     * @param rotateOnlyWhenClose When this flag is set to false the chase
     * camera will always rotate around its spatial independently of their
     * distance to one another. If set to true, the chase camera will only be
     * allowed to rotated below the "horizon" when the distance is smaller than
     * minDistance + 1.0f (when fully zoomed-in).
     */
    public void setDownRotateOnCloseViewOnly(boolean rotateOnlyWhenClose) {
        veryCloseRotation = rotateOnlyWhenClose;
    }

    /**
     * @return True if rotation below the vertical plane of the spatial tied to
     * the camera is allowed only when zoomed in at minDistance + 1.0f. False if
     * vertical rotation is always allowed.
     */
    public boolean getDownRotateOnCloseViewOnly() {
        return veryCloseRotation;
    }

    /**
     * return the current distance from the camera to the target
     *
     * @return
     */
    public float getDistanceToTarget() {
        return distance;
    }

    /**
     * returns the current horizontal rotation around the target in radians
     *
     * @return
     */
    public float getHorizontalRotation() {
        return rotation;
    }

    /**
     * returns the current vertical rotation around the target in radians.
     *
     * @return
     */
    public float getVerticalRotation() {
        return vRotation;
    }

    /**
     * returns the offset from the target's position where the camera looks at
     *
     * @return
     */
    public Vector3f getLookAtOffset() {
        return lookAtOffset;
    }

    /**
     * Sets the offset from the target's position where the camera looks at
     *
     * @param lookAtOffset
     */
    public void setLookAtOffset(Vector3f lookAtOffset) {
        this.lookAtOffset = lookAtOffset;
    }

    /**
     * Sets the up vector of the camera used for the lookAt on the target
     *
     * @param up
     */
    public void setUpVector(Vector3f up) {
        initialUpVec = up;
    }

    /**
     * Returns the up vector of the camera used for the lookAt on the target
     *
     * @return
     */
    public Vector3f getUpVector() {
        return initialUpVec;
    }
}
