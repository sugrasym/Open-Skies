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

public class UniformTerrainRange implements TerrainRange
{
    private float m_factor;
    private int m_min_h, m_max_h;
    private Color m_diffuse;
    private Color m_specular;

    public UniformTerrainRange(int minHeight, int maxHeight, float factor, Color diffuse, Color specular)
    {
        m_factor = factor;
        m_diffuse = diffuse;
        m_specular = specular;
        m_min_h = minHeight;
        m_max_h = maxHeight;
    }

    //@Override
    public float getFactor(int x, int y, int height, int slope,
            float temperature)
    {
        if (height > m_min_h && height < m_max_h)
        {
            return m_factor;
        } else
        {
            return 0.f;
        }
    }

    //@Override
    public Color getTerrainColor()
    {
        return m_diffuse;
    }

    //@Override
    public Color getTerrainSpecular()
    {
        return m_specular;
    }
}
