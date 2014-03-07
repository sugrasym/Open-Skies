/*    
 This file is part of jME Planet Demo.

 jME Planet Demo is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation.

 jME Planet Demo is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with jME Planet Demo.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ankh.unfall.planet;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.nio.ByteBuffer;
import org.ankh.unfall.planet.texgen.PlanetGenerator;

/**
 * Telluric planet object
 *
 * @author Yacine Petitprez (anykeyh)
 */
public class ProceduralPlanet {

    transient private Geometry planetGeometry;
    transient private PlanetInformation informations;
    transient private PlanetGenerator generator;
    transient private int textureHeight, textureWidth;

    private Image getBaseMapData() {
        ByteBuffer baseMapData = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        int[] colorMap = generator.getColorMap();

        // For each color int in the colormap:
        for (int c : colorMap) {
            //ARGB -> RGBA
            //= (ARGB & 0x00FFFFFF) << 8 | 0xFF;
            int newc = ((c & 0x00FFFFFF) << 8) | 0xFF;
            baseMapData.putInt(newc);
        }

        Image baseMap = new Image(Image.Format.RGBA8, textureWidth, textureHeight, baseMapData);

        return baseMap;
    }

    private Image getHeightMapData() {
        ByteBuffer heightMapData = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        int[] heightMap = generator.getHeightMap();

        // For each color int in the colormap:
        for (int c : heightMap) {
            //ARGB -> RGBA
            //= (ARGB & 0x00FFFFFF) << 8 | 0xFF;
            int newc = ((c & 0x00FFFFFF) << 8) | 0xFF;
            heightMapData.putInt(newc);
        }

        Image outputMap = new Image(Image.Format.RGBA8, textureWidth, textureHeight, heightMapData);

        return outputMap;
    }

    private Image getSpecularMapData() {
        ByteBuffer baseMapData = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        int[] colorMap = generator.getSpecularMap();

        for (int c : colorMap) {
            int newc = ((c & 0x00FFFFFF) << 8) | 0xFF;
            baseMapData.putInt(newc);
        }

        Image baseMap = new Image(Image.Format.RGBA8, textureWidth, textureHeight, baseMapData);

        return baseMap;
    }

    private Image getNormalMapData() {
        ByteBuffer baseMapData = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        int[] colorMap = generator.getNormalMap();

        for (int c : colorMap) {
            int newc = ((c & 0x00FFFFFF) << 8) | 0xFF;
            baseMapData.putInt(newc);
        }

        Image baseMap = new Image(Image.Format.RGBA8, textureWidth, textureHeight, baseMapData);

        return baseMap;
    }

    /**
     * Construit l'objet 3D d'une planete
     *
     * @param informations Les informations sur la planete.
     * @param generator Le générateur de texture utilisé. Si les texture ne sont
     * pas encore généré, le constructeur s'occupera de le faire.
     * @param renderer Couche de rendue employée
     */
    public ProceduralPlanet(PlanetInformation informations, PlanetGenerator generator, AssetManager assetManager) {

        this.informations = informations;
        this.generator = generator;

        textureHeight = generator.getHeight();
        textureWidth = generator.getWidth();

        /* Recuperation des textures */
        Texture baseMap = new Texture2D();
        baseMap.setImage(getBaseMapData());

        Texture normalMap = new Texture2D();
        normalMap.setImage(getNormalMapData());

        Texture heightMap = new Texture2D();
        heightMap.setImage(getHeightMapData());

        Sphere planetSphere = new Sphere(256, 256, informations.getRadius());
        planetSphere.setTextureMode(Sphere.TextureMode.Projected);
        planetGeometry = new Geometry("PlanetSphere", planetSphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", baseMap);
        mat.setTexture("NormalMap", normalMap);
        mat.setBoolean("UseMaterialColors", false);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setFloat("Shininess", 0.35f);

        planetGeometry.setMaterial(mat);

    }

    public Geometry getPlanetGeometry() {
        return planetGeometry;
    }

    public void setPlanetGeometry(Geometry planetGeometry) {
        this.planetGeometry = planetGeometry;
    }

    public PlanetInformation getInformations() {
        return informations;
    }

    public void setInformations(PlanetInformation informations) {
        this.informations = informations;
    }
}
