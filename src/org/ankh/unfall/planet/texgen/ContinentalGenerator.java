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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.system.thread.ForRunnable;
import org.ankh.unfall.system.thread.MultiThreadUtil;

/**
 * Provide Perlin noise modulated by Voronoi diagram for make Continental-like
 * heighmap.
 *
 * @author Yacine Petitprez
 */
public class ContinentalGenerator extends PlanetGenerator {

    private Random m_random;
    private final static float[] m_voronoi_vectors = new float[]{
        8.5f,
        -3,
        1
    /*1.1680948f,
     -0.2082833f,
     0.40441322f,
     -0.878508f,
     0.7664678f*/
    };

    /**
     * Class used for multi-threading heightmap generation task
     */
    private final class ForHeightMap implements ForRunnable {

        private PerlinNoise perlin;
        private int shift;
        private VoronoiDiagram diagram;
        private int[] heightmap;

        private ForHeightMap() {
        }

        public ForHeightMap(PerlinNoise perlin, int shift,
                VoronoiDiagram diagram, int[] heightmap) {
            this.perlin = perlin;
            this.shift = shift;
            this.diagram = diagram.clone();
            this.heightmap = heightmap;
        }

        //@Override
        public ForRunnable copy() {
            ForHeightMap copie = new ForHeightMap();

            copie.perlin = perlin;
            copie.shift = shift;
            copie.diagram = diagram.clone();
            copie.heightmap = heightmap;

            return copie;
        }

        @Override
        public void run(int index) {

            int x = index & m_widthm1;
            int y = index >> shift;

            int result = (int) (perlin.getPerlinPointAt(x, y) * 255 * (1.0f - 0.8f * diagram.getValueAt(x, y)));

            heightmap[index] = result;
        }
    }

    @Override
    protected int[] generateHeightmap() {

        final int[] heightmap = new int[getWidth() * getHeight()];

        /* Preparation du bruit de perlin */
        final PerlinNoise perlin = new PerlinNoise(getInformations().getSmoothness(), getWidth(), getHeight(), getInformations().getSeed());

        final VoronoiDiagram diagram = new VoronoiDiagram(getWidth(), getHeight(), 2f);

        for (int i = 0; i < 7; i++) {
            int x = m_random.nextInt(getWidth());
            //calculate equitorial point
            int equator = getHeight()/2;
            int range = getHeight()/12;
            int dR = m_random.nextInt(2*range)-range;
            //
            int y = equator + dR;

            diagram.addPoint(x, y);
        }

        for (float vector : m_voronoi_vectors) {
            diagram.addVector(vector);
        }

        /* decide where the water stops */
        int[] tab = new int[256];
        float seuil = heightmap.length * getInformations().getWaterInPercent();

        //double angle = Math.PI/2 - 0.001;

        //System.out.println( 0.5 * Math.log((1 + Math.sin(angle) )/(1 - Math.sin(angle))));


        //Utilisation de la methode multithread:
        if (MultiThreadUtil.PROCESSORS_COUNT > 1) {

            int loopCount = getWidth() * getHeight();

            int shift = 0;
            int width = getWidth();

            while (width != 1) {
                shift++;
                width >>= 1;
            }

            MultiThreadUtil.multiFor(0, loopCount,
                    new ForHeightMap(perlin, shift, diagram, heightmap));

            for (int i = 0; i < loopCount; i++) {
                tab[heightmap[i]]++;
            }

        } else {
            for (int x = 0; x < getWidth(); ++x) {
                for (int y = 0; y < getHeight(); ++y) {
                    int result = (int) (perlin.getPerlinPointAt(x, y) * 255 * (1.0f - 0.8f * diagram.getValueAt(x, y)));
                    //result += m_random.nextInt(2); //Ajout d'un pixel 1 � la valeur du pixel 1 fois sur 2
                    tab[result]++;
                    heightmap[y * getWidth() + x] = result;

                }
            }
        }

        int sum = 0;

        int x = 0;
        while (sum < seuil) {
            sum += tab[x++];
        }

        /* On lie le pixel seuillant l'eau au pourcentage de terre immerg�es */
        getInformations().setWaterLevel(x);

        m_heightMap = heightmap;

        for (int i = 0; i < (getHeight() / 5); i++) {
            addSource(m_random.nextInt(getWidth()), m_random.nextInt(getHeight()), null, 128);
        }

        return heightmap;
    }

    /**
     * Add source water at position x/y in the heightmap graphic
     *
     * @param x
     * @param y
     * @param visited
     * @param maxiteration
     */
    private void addSource(int x, int y, Set<Integer> visited, int maxiteration) {

        if (visited == null) {
            visited = new HashSet<Integer>();
        }

        int xm1 = x - 1;
        int xp1 = (x + 1) & m_widthm1;

        int ym1 = y - 1;
        int yp1 = (y + 1) & m_widthm1;

        if (x == 0) {
            xm1 = m_widthm1;
        }
        if (y == 0) {
            ym1 = m_widthm1;
        }

        int current = at(x, y);
        int a = at(xm1, ym1);
        int b = at(x, ym1);
        int c = at(xp1, ym1);
        int d = at(xm1, y);
        int e = at(xp1, y);
        int f = at(xm1, yp1);
        int g = at(x, yp1);
        int h = at(xp1, yp1);

        /* We should not get a negative result!! */
        if (m_heightMap[current] == 0) {
            return;
        }

        if ((!visited.contains(current) && m_heightMap[current] <= (getInformations().getWaterLevel() - 1))
                || --maxiteration <= 0) {

            m_heightMap[current]--;

            return;
        }


        boolean allvisited = visited.contains(a) & visited.contains(b) & visited.contains(c)
                & visited.contains(d) & visited.contains(e) & visited.contains(f) & visited.contains(g);



        int best = a;
        if (!allvisited) {
            if ((!visited.contains(b) && m_heightMap[b] < m_heightMap[best])) {
                best = b;
            }

            if ((!visited.contains(c) && m_heightMap[c] < m_heightMap[best])) {
                best = c;
            }

            if ((!visited.contains(d) && m_heightMap[d] < m_heightMap[best])) {
                best = d;
            }

            if ((!visited.contains(e) && m_heightMap[e] < m_heightMap[best])) {
                best = e;
            }

            if ((!visited.contains(f) && m_heightMap[f] < m_heightMap[best])) {
                best = f;
            }

            if ((!visited.contains(g) && m_heightMap[g] < m_heightMap[best])) {
                best = g;
            }

            if ((!visited.contains(h) && m_heightMap[h] < m_heightMap[best])) {
                best = h;
            }
        }

        visited.add(current);

        m_heightMap[current] -= 2;

        int x_best = best & m_widthm1;
        int y_best = best >> m_shiftwidth;

        addSource(x_best, y_best, visited, maxiteration);
    }

    public ContinentalGenerator(int texture_width, int texture_height,
            PlanetInformation info, TerrainPalette color_palette) {
        super(texture_width, texture_height, info, color_palette);

        /* Create a random source */
        m_random = new Random(info.getSeed());
    }
}
