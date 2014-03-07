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

/*
 * Defines a planet. Planets in this simulation have infinite mass so they will
 * stay put.
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import lib.astral.Parser.Term;
import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.ProceduralPlanet;
import org.ankh.unfall.planet.texgen.ContinentalGenerator;
import org.ankh.unfall.planet.texgen.PlanetGenerator;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.planet.texgen.palette.ranges.EarthPalette;
import org.ankh.unfall.planet.texgen.palette.ranges.HospitablePalette;
import org.ankh.unfall.planet.texgen.palette.ranges.MarsPalette;
import org.ankh.unfall.planet.texgen.palette.ranges.StrangePalette;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Planet extends Celestial {

    transient private Texture2D tex;
    private Term type;
    private int seed = 0;
    protected float radius;

    public Planet(Universe universe, String name, Term type, float radius) {
        super(Float.POSITIVE_INFINITY, universe);
        setName(name);
        this.type = type;
        this.radius = radius;
    }

    public void construct(AssetManager assets) {
        generateProceduralPlanet(assets);
        //initializes the physics as a sphere
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        //setup dynamic physics
        physics = new RigidBodyControl(sphereShape, getMass());
        //add physics to mesh
        spatial.addControl(physics);
        //store physics name control
        nameControl.setParent(this);
        spatial.addControl(nameControl);
    }

    public void deconstruct() {
        setTex(null);
        mat = null;
        spatial = null;
        physics = null;
    }

    private void generateProceduralPlanet(AssetManager assets) {
        Random sRand = new Random(seed);
        if (type.getValue("group").matches("rock")) {
            /*
             * The procedural planet generator in the com package gets to do
             * all the heavy lifting and we just read the output.
             */
            //create planet parameters
            PlanetInformation infos = new PlanetInformation();
            infos.setDaytime(360);
            infos.setEquatorTemperature(sRand.nextInt(40) + 10);
            infos.setPoleTemperature(sRand.nextInt(infos.getEquatorTemperature()) - 50);
            infos.setRadius(radius);
            infos.setWaterInPercent(sRand.nextFloat());
            infos.setHeightFactor(0.2f);
            infos.setSeed((int) seed);
            infos.setHumidity(sRand.nextFloat());
            infos.setSmoothness(sRand.nextInt(3) + 7);
            //setup palette
            TerrainPalette palette = null;
            String pal = type.getValue("palette");
            if (pal.matches("Earth")) {
                palette = new EarthPalette(infos);
            } else if (pal.matches("Mars")) {
                palette = new MarsPalette(infos);
            } else if (pal.matches("Hospitable")) {
                palette = new HospitablePalette(infos);
            } else if (pal.matches("Strange")) {
                palette = new StrangePalette(infos);
            }
            //create generator
            PlanetGenerator generator = new ContinentalGenerator(4096, 2048, infos, palette);
            ProceduralPlanet p = new ProceduralPlanet(infos, generator, assets);
            //store planet spatial
            spatial = p.getPlanetGeometry();
        } else if (type.getValue("group").matches("singlegas")) {
            //create a canvas
            BufferedImage buff = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = (Graphics2D) buff.getGraphics();
            //draw debug texture
            gfx.setColor(new Color(0, 0, 0, 0));
            gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
            /*
             * Setup the sphere since we aren't using the procedural planet generator
             * supplied in the com package.
             */
            //create geometry
            Sphere objectSphere = new Sphere(256, 256, radius);
            objectSphere.setTextureMode(Sphere.TextureMode.Projected);
            spatial = new Geometry("Planet", objectSphere);
            //retrieve texture
            mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            mat.setFloat("Shininess", 0.35f);
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
            gfx.setColor(new Color(Color.HSBtoRGB(hue, sat, value)));
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
            spatial.setMaterial(mat);
        } else if (type.getValue("group").matches("doublegas")) {
            //create a canvas
            BufferedImage buff = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = (Graphics2D) buff.getGraphics();
            //draw debug texture
            gfx.setColor(new Color(0, 0, 0, 0));
            gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
            /*
             * Setup the sphere since we aren't using the procedural planet generator
             * supplied in the com package.
             */
            //create geometry
            Sphere objectSphere = new Sphere(256, 256, radius);
            objectSphere.setTextureMode(Sphere.TextureMode.Projected);
            spatial = new Geometry("Planet", objectSphere);
            //retrieve texture
            mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            mat.setFloat("Shininess", 0.35f);
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
            gfx.setColor(new Color(Color.HSBtoRGB(hue, sat, value)));
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
            //pick a hue
            hue = sRand.nextFloat();
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
            spatial.setMaterial(mat);
        } else {
            /*
             * Debugging, this really should be unreachable
             */
            //create a canvas
            BufferedImage buff = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = (Graphics2D) buff.getGraphics();
            //draw debug texture
            gfx.setColor(Color.CYAN);
            gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
            /*
             * Setup the sphere since we aren't using the procedural planet generator
             * supplied in the com package.
             */
            //create geometry
            Sphere objectSphere = new Sphere(256, 256, radius);
            objectSphere.setTextureMode(Sphere.TextureMode.Projected);
            spatial = new Geometry("Planet", objectSphere);
            //retrieve texture
            mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            mat.setFloat("Shininess", 0.5f);
            mat.setBoolean("UseMaterialColors", false);
            mat.setColor("Ambient", ColorRGBA.Black);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setColor("Diffuse", ColorRGBA.White);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Off);
        }
        //rotate
        setRotation(getRotation().fromAngles(FastMath.PI / 2, 0, 0));
        //setup shadow
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }

    protected void alive() {
        if (physics != null) {
            if (spatial != null) {
                //planets do not move
                physics.setPhysicsLocation(getLocation());
                physics.setPhysicsRotation(getRotation());
                spatial.setLocalRotation(getRotation());
            }
        }
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
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
}
