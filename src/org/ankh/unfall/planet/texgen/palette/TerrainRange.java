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

public interface TerrainRange
{
    /**
     * @param x X-coordinate on this map
     * @param y Y-coordinate on this map
     * @param temperature This point's temperature
     * @param slope This point's slope
     * @param height This point's height
     * @return the coefficient used for mixing the various colors
     */
    public float getFactor(int x, int y, int height, int slope, float temperature);

    /**
     * @return the color for this type of terrain.
     */
    public Color getTerrainColor();

    /**
     * @return the specular value for this type of terrain.
     */
    public Color getTerrainSpecular();
}
