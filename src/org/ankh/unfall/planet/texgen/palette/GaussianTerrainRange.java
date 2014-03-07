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

import com.jme3.math.FastMath;
import java.awt.Color;

public class GaussianTerrainRange implements TerrainRange
{
    //Height
    private int m_minh, m_maxh; //Maximum height
    private float m_mediumh, m_varianceh;
    //Slope
    private int m_mins, m_maxs;
    private float m_mediums, m_variances;
    //Temp in Kelvin!!!
    private int m_mint, m_maxt;
    private float m_mediumt, m_variancet;
    private Color m_color_terrain;
    private Color m_color_specular;

    public GaussianTerrainRange(int m_minh, int m_maxh, float m_mediumh,
            float m_varianceh, int m_mins, int m_maxs, float m_mediums,
            float m_variances, int m_mint, int m_maxt, float m_mediumt,
            float m_variancet, Color m_color_terrain, Color m_color_specular)
    {
        super();
        this.m_minh = m_minh;
        this.m_maxh = m_maxh;
        this.m_mediumh = m_mediumh;
        this.m_varianceh = m_varianceh;

        this.m_mins = m_mins;
        this.m_maxs = m_maxs;
        this.m_mediums = m_mediums;
        this.m_variances = m_variances;

        this.m_mint = m_mint + 257;
        this.m_maxt = m_maxt + 257;
        this.m_mediumt = m_mediumt + 257;
        this.m_variancet = m_variancet;

        this.m_color_terrain = m_color_terrain;
        this.m_color_specular = m_color_specular;
    }

    public float getFactor(int x, int y, int height, int slope, float temperature)
    {

        if (m_maxh != -1 && height > m_maxh)
        {
            return 0;
        }
        if (m_minh != -1 && height < m_minh)
        {
            return 0;
        }

        if (m_maxs != -1 && slope > m_maxs)
        {
            return 0;
        }
        if (m_mins != -1 && slope < m_mins)
        {
            return 0;
        }

        if (m_maxt != -1 && temperature > m_maxt)
        {
            return 0;
        }
        if (m_mint != -1 && temperature < m_mint)
        {
            return 0;
        }


        /* Min/Max value clipping */
        float mhsqr = 0.0f, mssqr = 0.0f, mtsqr = 0.0f;

        if (m_varianceh != -1.0)
        {
            mhsqr = (m_mediumh - height) / m_varianceh;
            mhsqr = Math.abs(mhsqr);
        }


        if (m_variances != -1.0)
        {
            mssqr = (m_mediums - slope) / m_variances;
            mssqr = Math.abs(mssqr);
        }

        if (m_variancet != -1.0)
        {
            mtsqr = (m_mediumt - temperature) / m_variancet;
            mtsqr = Math.abs(mtsqr);
        }
        /* e^-dh * e^-ds * e^-dt function */
        return (FastMath.exp(-(mhsqr + mssqr + mtsqr)));
    }

    //@Override
    public Color getTerrainColor()
    {
        return m_color_terrain;
    }

    //@Override
    public Color getTerrainSpecular()
    {
        return m_color_specular;
    }
}
