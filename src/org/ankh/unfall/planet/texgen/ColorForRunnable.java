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
import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.texgen.palette.ColorMixer;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette.Colors;
import org.ankh.unfall.system.thread.ForRunnable;

/**
 * {@link ForRunnable} used to compute color and specular map of a Planet
 * @author Yacine Petitprez
 */
public class ColorForRunnable implements ForRunnable
{
    private ColorMixer mixer = new ColorMixer();
    private TerrainPalette palette;
    private int[] heightMap;
    private int[] specMap;
    private int[] colMap;
    private int shift;
    private PlanetInformation infos;
    private int height;
    private int halfheight;
    private int heightm1;
    private int width;
    private int widthm1;
    private int halfwidth;

    protected float getTemperature(int y, int height)
    {

        float latitude = FastMath.cos((Math.abs(y - halfheight) / ((float) this.height)) * FastMath.PI);

        return 257 + latitude * (infos.getEquatorTemperature() - infos.getPoleTemperature())
                + infos.getPoleTemperature() - infos.getHeightFactor() * Math.max(0, height - infos.getWaterLevel());
    }

    private ColorForRunnable()
    {
    }

    public ColorForRunnable(TerrainPalette palette, PlanetInformation infos, int height, int width, int[] heightMap, int[] specMap, int[] colMap)
    {
        this.infos = infos;

        this.palette = palette;
        this.heightMap = heightMap;
        this.specMap = specMap;
        this.colMap = colMap;

        this.width = width;
        this.widthm1 = width - 1;
        this.halfwidth = width >> 1;

        this.height = height;
        this.heightm1 = height - 1;
        this.halfheight = height >> 1;


        while (width != 1)
        {
            width >>= 1;
            ++shift;
        }
    }

    //@Override
    public ForRunnable copy()
    {
        ColorForRunnable copie = new ColorForRunnable();

        copie.infos = this.infos;
        copie.palette = this.palette;
        copie.shift = this.shift;
        copie.heightMap = this.heightMap;
        copie.specMap = this.specMap;
        copie.colMap = this.colMap;

        copie.height = this.height;
        copie.heightm1 = this.heightm1;
        copie.halfheight = this.halfheight;

        copie.width = this.width;
        copie.widthm1 = this.widthm1;
        copie.halfwidth = this.halfwidth;

        return copie;
    }

    //@Override
    public void run(int index)
    {
        int x = index & widthm1;
        int y = index >> shift;

        int height = heightMap[index];
        int slope = getSlope(x, y);
        float temp = getTemperature(y, height);

        Colors colors = palette.getPointColor(mixer, x, y, height, slope, temp);

        specMap[index] = colors.getSpecular().getRGB() | 0xFF000000;
        colMap[index] = colors.getTerrain().getRGB() | 0xFF000000;
    }

    protected int getSlope(int x, int y)
    {
        int s_a = Math.abs(heightMap[at(x, y)] - heightMap[at(x - 1, y)]);
        int s_b = Math.abs(heightMap[at(x, y)] - heightMap[at(x + 1, y)]);
        int s_c = Math.abs(heightMap[at(x, y)] - heightMap[at(x, y - 1)]);
        int s_d = Math.abs(heightMap[at(x, y)] - heightMap[at(x, y + 1)]);

        return (int) MathUtil.max(s_a, s_b, s_c, s_d);
    }

    protected int at(int x, int y)
    {
        while (x < 0)
        {
            x += width;
        }

        y = y & ((height << 1) - 1);

        if (y > heightm1)
        {
            y = (heightm1 << 1) - y;
            x += halfwidth;
        }

        if (y < 0)
        {
            y = -y;
            x += width >> 1;
        }

        x = x & widthm1;


        return (y * width) + x;
    }
}
