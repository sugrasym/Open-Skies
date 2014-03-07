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

/**
 * Compute a perlin noise map.
 * @author Yacine Petitprez
 */
public class PerlinNoise
{

    private int m_layerCount;
    private int m_width, m_height;
    private Randomizer3D2 randomizer;

    private float getLayers(int x, int y, int layer)
    {
        int heightm1 = (m_height >> layer) - 1;
        int widthm1 = (m_width >> layer) - 1;

        int xlayer = x >> layer;
        int ylayer = y >> layer;

        int xa = xlayer;
        int ya = ylayer;

        int xb = xlayer;
        int yb = (ylayer + 1) & (heightm1);

        int xc = (xlayer + 1) & (widthm1);
        int yc = ylayer;

        int xd = (xlayer + 1) & (widthm1);
        int yd = (ylayer + 1) & (heightm1);


        float random_a = randomizer.getPointRandom(xa, ya, layer);
        float random_b = randomizer.getPointRandom(xb, yb, layer);
        float random_c = randomizer.getPointRandom(xc, yc, layer);
        float random_d = randomizer.getPointRandom(xd, yd, layer);

        float freq = 1.f / (1 << layer);

        float a1 = (x * freq) - xa;
        float a2 = (y * freq) - ya;

        float ret =
                (MathUtil.cosMix(
                MathUtil.cosMix(random_a, random_c, a1),
                MathUtil.cosMix(random_b, random_d, a1),
                a2));

        return ret;
    }

    public float[] getPerlin()
    {
        float[] ret = new float[m_width * m_height];

        for (int i = 0; i < m_width; ++i)
        {
            for (int j = 0; j < m_height; ++j)
            {
                float f = 0;

                for (int l = 0, div = (1 << m_layerCount); l < m_layerCount; ++l, div >>= 1)
                {
                    f += getLayers(i, j, l) / div;
                }

                ret[i + j * m_width] = f;
            }
        }

        return ret;
    }

    public float getPerlinPointAt(int x, int y)
    {
        float f = 0;

        float divMul = 1.f / (1 << m_layerCount);

        for (int l = 0; l < m_layerCount; ++l)
        {
            f += getLayers(x, y, l) * divMul;
            divMul *= 2; //Pas de division en virgule flottante!
        }

        return f;
    }

    public PerlinNoise(int layerCount, int width, int height, int seed)
    {
        m_layerCount = layerCount;
        m_width = width;
        m_height = height;

        randomizer = new Randomizer3D2(seed);
    }
}
