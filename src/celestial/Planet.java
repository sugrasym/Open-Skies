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
 * Defines a planet. Planets in this simulation have infinite mass so they will
 * stay put.
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import jmeplanet.FractalDataSource;
import jmeplanet.PlanetAppState;
import jmeplanet.PlanetCollisionShape;
import jmeplanet.Utility;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Planet extends Celestial {
    public static final float MIN_ATMOSPHERE_DAMAGE_VELOCITY = 10f;
    public static final float ATMOSPHERE_DAMAGE_SCALER = 8f;

    private transient Texture2D tex;
    transient jmeplanet.Planet fractalPlanet;
    transient jmeplanet.Planet atmosphereShell;
    protected transient RigidBodyControl atmospherePhysics;
    private Term type;
    private int seed = 0;
    protected final float radius;
    private float atmosphereScaler;
    private final Vector3f tilt;

    public Planet(Universe universe, String name, Term type, float radius,
            Vector3f tilt) {
        super(Float.POSITIVE_INFINITY, universe);
        setName(name);
        this.type = type;
        this.radius = radius;
        this.tilt = tilt;
        //planets are automatically discovered
        discover();
    }

    @Override
    public void construct(AssetManager assets) {
        generateProceduralPlanet(assets);
        if (getSpatial() != null) {
            CollisionShape hullShape;
            //initializes the physics as a sphere
            String group = type.getValue("group");
            if (!group.equals("rock")) {
                hullShape = new SphereCollisionShape(radius);
            } else {
                hullShape = new PlanetCollisionShape(getLocation(), radius,
                        fractalPlanet.getDataSource());
            }
            //setup dynamic physics
            physics = new RigidBodyControl(hullShape, getMass());
            //add physics to mesh
            getSpatial().addControl(physics);
            if (atmosphereShell != null) {
                atmospherePhysics = new RigidBodyControl(hullShape, getMass());
                atmosphereShell.addControl(atmospherePhysics);
            }
            //store physics name control
            nameControl.setParent(this);
            getSpatial().addControl(nameControl);
        }
    }

    @Override
    public void deconstruct() {
        setTex(null);
        mat = null;
        setSpatial(null);
        physics = null;
        atmosphereShell = null;
        atmospherePhysics = null;
    }

    private void generateProceduralPlanet(AssetManager assets) {
        //setup seeded rng
        Random sRand = new Random(seed);
        //get group and palette
        String group = type.getValue("group");
        String palette = type.getValue("palette");
        //split based on planet group
        switch (group) {
            case "rock":
                //determine height scale
                float heightScale = (sRand.nextFloat() * 0.02f) + 0.01f; //1% to 3%
                switch (palette) {
                    case "Earth": {
                        // Add planet
                        FractalDataSource planetDataSource = new FractalDataSource(seed);
                        planetDataSource.setHeightScale(heightScale * radius);
                        fractalPlanet = Utility.createEarthLikePlanet(assets, radius, null, planetDataSource);
                        setSpatial(fractalPlanet);
                        setAtmosphereScaler(Utility.ATMOSPHERE_MULTIPLIER);
                        break;
                    }
                    case "Barren":
                        FractalDataSource moonDataSource = new FractalDataSource(seed);
                        moonDataSource.setHeightScale(heightScale * radius);
                        fractalPlanet = Utility.createMoonLikePlanet(assets, radius, moonDataSource);
                        setSpatial(fractalPlanet);
                        setAtmosphereScaler(0);
                        break;
                    case "Ice": {
                        // Add planet
                        FractalDataSource planetDataSource = new FractalDataSource(seed);
                        planetDataSource.setHeightScale(heightScale * radius);
                        fractalPlanet = Utility.createIcePlanet(assets, radius, null, planetDataSource, seed);
                        setAtmosphereScaler(Utility.ATMOSPHERE_MULTIPLIER);
                        setSpatial(fractalPlanet);
                        break;
                    }
                    case "Mars": {
                        //determine water presence
                        boolean hasWater = Boolean.parseBoolean(type.getValue("hasWater"));
                        // Add planet
                        FractalDataSource planetDataSource = new FractalDataSource(seed);
                        planetDataSource.setHeightScale(heightScale * radius);
                        fractalPlanet = Utility.createMarsLikePlanet(assets, radius, null, planetDataSource, hasWater, seed);
                        setAtmosphereScaler(Utility.ATMOSPHERE_MULTIPLIER);
                        setSpatial(fractalPlanet);
                        break;
                    }
                }
                break;
            case "gas":
                Color airColor = Color.WHITE;
                if (palette.equals("BandedGas")) {
                    //create a canvas
                    BufferedImage buff = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gfx = (Graphics2D) buff.getGraphics();
                    //draw debug texture
                    gfx.setColor(new Color(0, 0, 0, 0));
                    gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
                    /*
                     * Setup the sphere since we aren't using the procedural planet generator
                     * supplied in the jmeplanet package
                     */
                    //create geometry
                    Sphere objectSphere = new Sphere(256, 256, radius);
                    objectSphere.setTextureMode(Sphere.TextureMode.Projected);
                    setSpatial(new Geometry("Planet", objectSphere));
                    //retrieve texture
                    mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setFloat("Shininess", 0.32f);
                    mat.setBoolean("UseMaterialColors", false);
                    mat.setColor("Ambient", ColorRGBA.Black);
                    mat.setColor("Specular", ColorRGBA.White);
                    mat.setColor("Diffuse", ColorRGBA.White);
                    mat.getAdditionalRenderState().setBlendMode(BlendMode.Off);
                    /*
                     * My gas giants are conservative. They have a color and brightness
                     * which is held constant while bands are drawn varying the saturation.
                     *
                     * Two passes are made. The first draws primary bands, which define the
                     * overall look. The second does secondary bands which help de-alias
                     * the planet.
                     */
                    //determine band count
                    int bands = sRand.nextInt(75) + 25;
                    int height = (buff.getHeight() / bands);
                    //pick sat and val
                    float sat = sRand.nextFloat();
                    float value = sRand.nextFloat();
                    if (value < 0.45f) {
                        value = 0.45f;
                    }
                    //pick a hue
                    float hue = sRand.nextFloat();
                    //draw a baseplate
                    airColor = new Color(Color.HSBtoRGB(hue, sat, value));
                    gfx.setColor(airColor);
                    gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
                    //pass 1, big bands
                    for (int a = 0; a < bands / 2; a++) {
                        //vary saturation
                        sat = sRand.nextFloat();
                        //draw a band
                        Color raw = new Color(Color.HSBtoRGB(hue, sat, value));
                        Color col = new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 64);
                        gfx.setColor(col);
                        gfx.fillRect(0, height / 2 * (a), buff.getWidth(), height);
                    }
                    //pass 2, small secondary bands
                    for (int a = 0; a < bands * 4; a++) {
                        //vary saturation
                        sat = sRand.nextFloat();
                        //draw a band
                        Color raw = new Color(Color.HSBtoRGB(hue, sat, value));
                        Color col = new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 16);
                        gfx.setColor(col);
                        gfx.fillRect(0, height / 4 * (a), buff.getWidth(), height);
                    }
                    //map to material
                    Image load = new AWTLoader().load(buff, true);
                    setTex(new Texture2D(load));
                    mat.setTexture("DiffuseMap", getTex());
                    getSpatial().setMaterial(mat);
                }   //rotate
                setRotation(getRotation().fromAngles(FastMath.PI / 2, 0, 0));
                //add an atmosphere
                FractalDataSource planetDataSource = new FractalDataSource(seed);
                planetDataSource.setHeightScale(0.015f * radius);
                //generate color
                float colR = (float) airColor.getRed() / 255.0f;
                float colG = (float) airColor.getGreen() / 255.0f;
                float colB = (float) airColor.getBlue() / 255.0f;
                ColorRGBA atmoColor = new ColorRGBA(colR, colG, colB, 0.5f);
                //generate shell
                setAtmosphereScaler(0.01f);
                atmosphereShell = Utility.createAtmosphereShell(assets,
                        radius + (radius * getAtmosphereScaler()),
                        planetDataSource, atmoColor);
                break;
        }
        //apply tilt
        Quaternion dTilt = new Quaternion().fromAngles(tilt.x, tilt.y, tilt.z);
        setRotation(getRotation().add(dTilt));
    }

    @Override
    protected void alive() {
        if (physics != null) {
            if (getSpatial() != null) {
                //planets do not move
                physics.setPhysicsLocation(getLocation());
                physics.setPhysicsRotation(getRotation());
                getSpatial().setLocalRotation(getRotation());
                if (atmosphereShell != null) {
                    atmospherePhysics.setPhysicsLocation(getLocation());
                    atmospherePhysics.setPhysicsRotation(getRotation());
                    atmosphereShell.setLocalRotation(getRotation());
                }
            }
        }
    }

    @Override
    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        if (getSpatial() != null) {
            node.attachChild(getSpatial());
            physics.getPhysicsSpace().add(getSpatial());
            if (fractalPlanet != null) {
                planetAppState.addPlanet(fractalPlanet);
            }
            if (atmosphereShell != null) {
                node.attachChild(atmosphereShell);
                physics.getPhysicsSpace().add(atmospherePhysics);
                planetAppState.addPlanet(atmosphereShell);
            }
        }
    }

    @Override
    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.detachChild(getSpatial());
        physics.getPhysicsSpace().remove(getSpatial());
        if (fractalPlanet != null) {
            planetAppState.removePlanet(fractalPlanet);
        }
        if (atmosphereShell != null) {
            node.detachChild(atmosphereShell);
            physics.getPhysicsSpace().remove(atmospherePhysics);
            planetAppState.removePlanet(atmosphereShell);
        }
    }

    public float getRadius() {
        return radius;
    }

    public Term getType() {
        return type;
    }

    public void setType(Term type) {
        this.type = type;
    }

    public Texture2D getTex() {
        return tex;
    }

    public void setTex(Texture2D tex) {
        this.tex = tex;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public String toString() {
        return getName();
    }

    public float getAtmosphereScaler() {
        return atmosphereScaler;
    }

    public void setAtmosphereScaler(float atmosphereScaler) {
        this.atmosphereScaler = atmosphereScaler;
    }

    public float getSafetyZone(float caution) {
        return getAtmosphereRadius() * caution;
    }

    public float getAtmosphereRadius() {
        return getRadius() + (this.getRadius() * getAtmosphereScaler());
    }

    public Vector3f getTilt() {
        return tilt;
    }
}
