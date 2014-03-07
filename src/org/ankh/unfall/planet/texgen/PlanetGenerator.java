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
package org.ankh.unfall.planet.texgen;

import com.jme3.math.FastMath;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;

import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.system.thread.ForRunnable;
import org.ankh.unfall.system.thread.MultiThreadUtil;

public abstract class PlanetGenerator
{
    protected int[] m_heightMap;
    protected int[] m_specularMap;
    protected int[] m_colorMap;
    protected int[] m_cloudMap;
    protected int[] m_normalMap;
    private PlanetInformation m_informations;
    private TerrainPalette m_palette;
    private int m_width, m_height;
    /* Constants to generate every map at once. */
    public final static int MAP_COLOR = 0x1;
    public final static int MAP_SPECULAR = 0x2;
    public final static int MAP_CLOUD = 0x4;
    public final static int MAP_NIGHT = 0x8;
    public final static int MAP_HEIGHT = 0x10;
    public final static int MAP_NORMAL = 0x20;
    
    public int getWidth()
    {
        return m_width;
    }
    
    public int getHeight()
    {
        return m_height;
    }
    
    public PlanetInformation getInformations()
    {
        return m_informations;
    }
    
    public PlanetGenerator(int texture_width, int texture_height, PlanetInformation info, TerrainPalette color_palette)
    {

        /* The textures sizes need to be powers of two, since we used the binary AND operator 
         * to do the modulo function, which only works on 2^n numbers */
        if (!FastMath.isPowerOfTwo(texture_width) || !FastMath.isPowerOfTwo(texture_height))
        {
            throw new RuntimeException("[ERROR] Texture height and width must be power of two...");
        }
        m_palette = color_palette;
        m_width = texture_width;
        m_height = texture_height;
        m_informations = info;

        /* Precalculated variables */
        m_halfheight = m_height >> 1;
        m_halfwidth = m_width >> 1;
        m_widthm1 = m_width - 1;
        m_heightm1 = m_height - 1;
        
        while (texture_width != 1)
        {
            texture_width >>= 1;
            m_shiftwidth++;
        }
        
        while (texture_height != 1)
        {
            texture_height >>= 1;
            m_shiftheight++;
        }
        
    }
    
    protected abstract int[] generateHeightmap();

    /**
     * Allows to generate several textures at the same time.
     * @param activeMap the textures to generate. 
     * Can be <code>MAP_COLOR</code>, <code>MAP_SPECULAR</code>, <code>MAP_CLOUD</code>,
     * <code>MAP_NIGHT</code>, or a combinaison of those values (using the binary OR "|" ) ).
     *
     * Example: 
     * <code>
     * 	 generateAllMap(MAP_COLOR | MAP_SPECULAR); // Generates the color and specular maps.
     * </code>
     */
    public final void generateAllMap(int activeMap)
    {
        getHeightMap();
        
        if ((activeMap & MAP_COLOR) != 0)
        {
            getColorMap();
        }
        
        if ((activeMap & MAP_SPECULAR) != 0)
        {
            getSpecularMap();
        }
    }
    // Variables to hold some commonly-used values
    //height << 1
    protected int m_halfheight;
    //width << 1
    protected int m_halfwidth;
    //width-1
    protected int m_widthm1;
    //height-1
    protected int m_heightm1;
    protected int m_shiftheight;
    protected int m_shiftwidth;

    /**
     * @param x the X-coordinate of the poin to get
     * @param y the Y-coordinate of the poin to get
     * @return the offset to use in the table to get the point data.
     */
    protected int at(int x, int y)
    {
        while (x < 0)
        {
            x += m_width;
        }
        
        y = y & ((m_height << 1) - 1);
        
        if (y > m_heightm1)
        {
            y = (m_heightm1 << 1) - y;
            x += m_halfwidth;
        }
        
        if (y < 0)
        {
            y = -y;
            x += m_width >> 1;
        }
        
        x = x & m_widthm1;
        
        
        return (y * m_width) + x;
        
    }

    /**
     * Specular map generation
     * @return the specular map data formatted as A8 (gray scale)
     */
    protected int[] generateSpecularMap()
    {
        return null;
    }
    
    protected int getSlope(int x, int y)
    {
        int[] heightMap = getHeightMap();
        int s_a = Math.abs(heightMap[at(x, y)] - heightMap[at(x - 1, y)]);
        int s_b = Math.abs(heightMap[at(x, y)] - heightMap[at(x + 1, y)]);
        int s_c = Math.abs(heightMap[at(x, y)] - heightMap[at(x, y - 1)]);
        int s_d = Math.abs(heightMap[at(x, y)] - heightMap[at(x, y + 1)]);
        
        return (int) MathUtil.max(s_a, s_b, s_c, s_d);
    }
    
    protected float getTemperature(int y, int height)
    {
        
        float latitude = FastMath.cos((Math.abs(y - m_halfheight) / ((float) m_height)) * FastMath.PI);
        
        return 257 + latitude * (m_informations.getEquatorTemperature() - m_informations.getPoleTemperature())
                + m_informations.getPoleTemperature() - m_informations.getHeightFactor() * MathUtil.max(0, height - m_informations.getWaterLevel());
        
    }

    /**
     * Color map generation
     * @return the color map data formatted as A8R8G8B8
     */
    protected void generateColorAndSpecularMap()
    {
        
        if (m_colorMap == null)
        {
            m_colorMap = new int[m_height * m_width];
            m_specularMap = new int[m_height * m_width];
            
            int[] heightMap = getHeightMap();
            
            m_palette.initPalette();
            
            if (MultiThreadUtil.PROCESSORS_COUNT > 1)
            {
                MultiThreadUtil.multiFor(0, m_width * m_height, new ColorForRunnable(
                        m_palette, m_informations, m_height, m_width, heightMap, m_specularMap, m_colorMap));
            } else
            {
                for (int x = 0; x < m_width; ++x)
                {
                    for (int y = 0; y < m_height; ++y)
                    {
                        int offset = at(x, y);
                        int height = heightMap[offset];
                        int slope = getSlope(x, y);
                        
                        float temp = getTemperature(y, height);
                        
                        TerrainPalette.Colors colors = m_palette.getPointColor(x, y, height, slope, temp);
                        m_specularMap[offset] = colors.getSpecular().getRGB() | 0xFF000000;
                        m_colorMap[offset] = colors.getTerrain().getRGB() | 0xFF000000;
                        
                    }
                }
            }
        }
        
    }

    /**
     * Not implemented yet, will return <code>null</code> for now ;)
     * @deprecated
     * @return
     */
    @Deprecated
    protected int[] generateCloudMap()
    {
        return null;
    }
    
    public final synchronized int[] getSpecularMap()
    {
        if (m_specularMap == null)
        {
            generateColorAndSpecularMap();
        }
        
        return m_specularMap;
    }
    
    public final synchronized int[] getColorMap()
    {
        if (m_colorMap == null)
        {
            generateColorAndSpecularMap();
        }
        
        return m_colorMap;
    }

    /**
     * Generates or retrieves if possible the height map
     * @return the height map data formatted as A8 (gray scale)
     */
    public final synchronized int[] getHeightMap()
    {
        if (m_heightMap == null)
        {
            m_heightMap = generateHeightmap();
        }
        
        return m_heightMap;
    }
    
    public final synchronized int[] getNormalMap()
    {
        if (m_normalMap == null)
        {
            m_normalMap = new int[m_width * m_height];
            
            final int factor = 150;
            
            
            if (MultiThreadUtil.PROCESSORS_COUNT > 1)
            {
                long time = System.currentTimeMillis();
                
                MultiThreadUtil.multiFor(0, m_normalMap.length, new ForRunnable()
                {
                    int[] heightMap = getHeightMap();

                    //@Override
                    public void run(int index)
                    {
                        int x = index & m_widthm1;
                        int y = index >> m_shiftwidth;
                        
                        int xy = Math.max(getInformations().getWaterLevel(), heightMap[at(x, y)]);
                        int xyp1 = Math.max(getInformations().getWaterLevel(), heightMap[at(x, y + 1)]);
                        int xp1y = Math.max(getInformations().getWaterLevel(), heightMap[at(x + 1, y)]);
                        
                        int z1 = xyp1 - xy;
                        int z2 = xp1y - xy;
                        
                        float v_x = (z1 / 255.f) * factor;
                        float v_y = (z2 / 255.f) * factor;
                        
                        float l = FastMath.sqrt(v_x * v_x + v_y * v_y + factor);
                        
                        v_x /= l;
                        v_y /= l;
                        
                        int r = (int) (v_x * 127 + 128);
                        int g = (int) (v_y * 127 + 128);
                        int b = 255;
                        
                        m_normalMap[index] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }

                    //@Override
                    public ForRunnable copy()
                    {
                        return this;
                    }
                });
                
                time = System.currentTimeMillis() - time;
                
                System.out.println("Temps: " + time);
                
            } else
            {
                int[] heightMap = getHeightMap();
                
                for (int x = 0; x < m_width; ++x)
                {
                    for (int y = 0; y < m_height; ++y)
                    {
                        
                        int xy = Math.max(getInformations().getWaterLevel(), heightMap[at(x, y)]);
                        int xyp1 = Math.max(getInformations().getWaterLevel(), heightMap[at(x, y + 1)]);
                        int xp1y = Math.max(getInformations().getWaterLevel(), heightMap[at(x + 1, y)]);

                        //System.out.println(xy +", " + xyp1 + ", " + xp1y);

                        int z1 = xyp1 - xy;
                        int z2 = xp1y - xy;
                        
                        float v_x = (z1 / 255.f) * factor;
                        float v_y = (z2 / 255.f) * factor;
                        //float v_z = factor;

                        float l = FastMath.sqrt(v_x * v_x + v_y * v_y + factor);
                        
                        v_x /= l;
                        v_y /= l;
                        
                        int r = (int) (v_x * 127 + 128);
                        int g = (int) (v_y * 127 + 128);
                        int b = 255;
                        
                        m_normalMap[at(x, y)] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }
            }
            
            
        }
        
        return m_normalMap;
    }
    
    public BufferedImage getDebugImageMap(int map)
    {
        
        int[] x;
        
        switch (map)
        {
            case MAP_COLOR:
                x = getColorMap().clone();
                break;
            case MAP_HEIGHT:
                x = getHeightMap().clone();
                break;
            case MAP_SPECULAR:
                x = getSpecularMap().clone();
                break;
            case MAP_NORMAL:
                x = getNormalMap().clone();
                break;
            default:
                return null;
        }
        
        if (map == MAP_HEIGHT)
        {
            for (int i = 0; i < x.length; i++)
            {
                x[i] *= 0x010101;
            }
        }
        
        Image temp = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(m_width, m_height, x, 0, m_width));
        
        BufferedImage ret = new BufferedImage(m_width, m_height, BufferedImage.TYPE_INT_RGB);
        
        ret.getGraphics().drawImage(temp, 0, 0, null);
        
        return ret;
    }
}
