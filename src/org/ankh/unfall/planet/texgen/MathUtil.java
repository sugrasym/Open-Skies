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

/**
 * This class add some functions like multi max/min.
 * @author Yacine Petitprez
 */
public final class MathUtil
{
    private MathUtil()
    {
    }

    public final static float min(float... fs)
    {
        float ret = Float.MAX_VALUE;
        for (float f : fs)
        {
            ret = Math.min(ret, f);
        }
        return ret;
    }

    public final static float max(float... fs)
    {
        float ret = Float.MIN_VALUE;
        for (float f : fs)
        {
            ret = Math.max(ret, f);
        }
        return ret;
    }

    /**
     * Sinusoidal interpolation between two scalar a and b
     * @param a
     * @param b
     * @param alpha
     * @return the interpolated value of input floats.
     */
    public final static float cosMix(float a, float b, float alpha)
    {
        alpha = (1 - FastMath.cos(alpha * FastMath.PI)) * 0.5f;

        return a * (1 - alpha) + b * alpha;
    }

    /**
     * Linear interpolation between to scalar a and b
     * @param a
     * @param b
     * @param alpha
     * @return the interpolated value of input floats.
     */
    public final static float linMix(float a, float b, float alpha)
    {

        return a * (1 - alpha) + b * alpha;

    }

    /**
     * Clamp a value between min and max bounds.
     * @param val
     * @param min
     * @param max
     * @return val if val>=min and val<=max. min if val<min and max if val>max
     */
    public final static float clamp(float val, float min, float max)
    {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Clamp a value between min and max bounds.
     * @param val
     * @param min
     * @param max
     * @return val if val>=min and val<=max. min if val<min and max if val>max
     */
    public final static int clamp(int val, int min, int max)
    {
        return Math.max(min, Math.min(max, val));
    }
}
