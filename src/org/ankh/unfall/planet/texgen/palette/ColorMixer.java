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
package org.ankh.unfall.planet.texgen.palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColorMixer
{
    // Private structure to hold both the weight and color.
    private class Pair
    {
        public float weight;
        public Color c;

        Pair(Color c, float weight)
        {
            this.weight = weight;
            this.c = c;
        }
    }
    private List<Pair> color_list = new ArrayList<Pair>();

    public void attachColor(Color c, float weight)
    {
        color_list.add(new Pair(c, weight));
    }

    public void clear()
    {
        color_list.clear();
    }

    public Color getMixedColor()
    {
        float r = 0, g = 0, b = 0;
        float sum_weight = 0;

        for (Pair p : color_list)
        {
            r += p.c.getRed() * p.weight;
            g += p.c.getGreen() * p.weight;
            b += p.c.getBlue() * p.weight;
            sum_weight += p.weight;
        }

        return new Color((int) (r / sum_weight), (int) (g / sum_weight), (int) (b / sum_weight));
    }
}
