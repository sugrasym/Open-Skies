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

import java.util.Random;

/**
 * Simple randomizer in three dimension.
 * No use of multiplication and floating point operations
 * @author Yacine Petitprez
 */
public class Randomizer3D2
{
    private final static int TABSIZE = 4096;
    private final static int TABSIZE_MINUS_1 = TABSIZE - 1;
    private static int[] tab = new int[TABSIZE];
    private static float[] tab2 = new float[TABSIZE];
    private int seed;

    public Randomizer3D2(int seed)
    {
        this.seed = seed;
    }

    static
    {
        Random r = new Random(12345);

        for (int i = 0; i < TABSIZE; i++)
        {
            tab[i] = r.nextInt(TABSIZE);
            tab2[i] = r.nextFloat();
        }
    }

    //private final static float INVERSE_4095 = 1.f/4095.f;
    public float getPointRandom(int x, int y)
    {
        return getPointRandom(x, y, 0);
    }

    public float getPointRandom(int x, int y, int z)
    {
        return tab2[  tab[(seed ^ x) & TABSIZE_MINUS_1] ^ tab[y & TABSIZE_MINUS_1] ^ tab[z & TABSIZE_MINUS_1]];
    }
}
