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
package org.ankh.unfall.planet.texgen.palette.ranges;

import java.awt.Color;
import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.texgen.palette.GaussianTerrainRange;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.planet.texgen.palette.TerrainRange;

public class MarsPalette extends TerrainPalette
{
    public MarsPalette(PlanetInformation informations)
    {
        super(informations);
    }

    @Override
    public void initPalette()
    {

        /*TerrainRange tundra = new GaussianTerrainRange(
        getInformations().getWater_level(), -1, getInformations().getWater_level(), 4,
        -1, -1, 0, 2.5f,
        -258,80,5, 1.0f,
        new Color(0x413a28), Color.black
        );*/

        TerrainRange upstairs = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel(), 4,
                -1, -1, 0, 0.8f,
                -258, -258, 50, 5.5f,
                new Color(0xac5e3e), Color.black);

        TerrainRange upupstairs = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.1f, 3,
                -1, -1, 5, 6,
                -15, 50, 25, 2,
                new Color(0x8f4d36), Color.black);

        TerrainRange very_deep_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), 0, 50,
                -1, -1, 0, 5,
                -258, 100, 50, 5,
                new Color(0x152430), Color.white);

        TerrainRange deep_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), getInformations().getWaterLevel() * .5f, 50,
                -1, -1, 0, 5,
                -258, 100, 50, 5,
                new Color(0x203849), Color.white);

        TerrainRange light_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), getInformations().getWaterLevel(), 5,
                -1, -1, 0, 1.0f,
                -258, 100, 50, 5,
                new Color(0x4FAAA8), Color.white);


        TerrainRange arctic = new GaussianTerrainRange(
                -1, -1, 0, -1f,
                -1, -1, 0, -1f,
                -258, 5, -100, 3.0f,
                new Color(0xFFFFFF), Color.white);

        TerrainRange desert = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.2f, 4,
                -1, -1, 0, 2f,
                -258, 100, 0, 3.0f,
                new Color(0x3b2e36), Color.black);

        TerrainRange desert_dune = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.3f, 4,
                -1, -1, 0, 2f,
                -1, 100, 50, 3.0f,
                new Color(0x42333b), Color.black);

        TerrainRange moutain = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 2f, 4,
                -1, -1, 10, 1.5f,
                -258, -258, 5, 3,
                new Color(0x4a2e31), Color.black);

        //attachTerrainRange(tundra);
        attachTerrainRange(upstairs);
        attachTerrainRange(upupstairs);
        attachTerrainRange(deep_water);
        attachTerrainRange(light_water);
        attachTerrainRange(arctic);
        attachTerrainRange(very_deep_water);
        attachTerrainRange(desert);
        attachTerrainRange(desert_dune);
        attachTerrainRange(moutain);
    }
}
