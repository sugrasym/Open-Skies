/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Program for generating universes. This allows decent looking, arbitrarily
 * large worlds to be made automatically, and then manaully tuned.
 */
package lib.astral;

import lib.astral.AstralIO;
import lib.astral.Parser;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Random;
import lib.astral.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class WorldMaker {

    public WorldMaker() {
        //generate universe
        String out = generate(1, 10, 80, 100, 100, 60000, 180000, 500, 6000, 1, 3, 25000, 200000);
        //save
        AstralIO tmp = new AstralIO();
        tmp.writeFile("new-UNIVERSE.txt", out);
        System.out.println(out);
    }

    public static void main(String[] args) {
        new WorldMaker();
    }

    public final String generate(int minPlanetsPerSystem, int maxPlanetsPerSystem, int minSystems, int maxSystems,
            int worldSize, int minSystemSize, int maxSystemSize, int minPlanetSize,
            int maxPlanetSize, int minNebulaPerSystem, int maxNebulaPerSystem, int minNebulaSize, int maxNebulaSize) {
        String ret = "";
        {
            //precache parsers
            Parser sky = new Parser("SKY.txt");
            ArrayList<Term> skyTypes = sky.getTermsOfType("Skybox");
            Parser stars = new Parser("PLANET.txt");
            ArrayList<Term> starTypes = stars.getTermsOfType("Star");
            Parser particle = new Parser("PARTICLE.txt");
            ArrayList<Term> nebTypes = particle.getTermsOfType("Nebula");
            Parser planets = new Parser("PLANET.txt");
            ArrayList<Term> planetTypes = planets.getTermsOfType("Planet");
            //start rng
            Random rnd = new Random();
            //determine the number of systems to make
            int numSystems = rnd.nextInt(maxSystems);
            if (numSystems < minSystems) {
                numSystems = minSystems;
            }
            //generate each system
            for (int a = 0; a < numSystems; a++) {
                System.out.println("Generating Universe - " + (a + 1) + "/" + numSystems);
                ArrayList<Simpling> objects = new ArrayList<>();
                String thisSystem = "";
                {
                    //pick name
                    String systemName = "System " + a;
                    //pick size
                    int size = rnd.nextInt(maxSystemSize);
                    if (size < minSystemSize) {
                        size = minSystemSize;
                    }
                    //determine skybox
                    int pick = rnd.nextInt(skyTypes.size());
                    //determine map location
                    int x = rnd.nextInt(worldSize * 2) - worldSize;
                    int y = 0;
                    int z = rnd.nextInt(worldSize * 2) - worldSize;
                    //create the system entry
                    thisSystem +=
                            "[System]\n"
                            + "name=" + systemName + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "z=" + z + "\n"
                            + "sky=" + skyTypes.get(pick).getValue("name") + "\n"
                            + "[/System]\n\n";
                    //get star types
                    pick = rnd.nextInt(starTypes.size());
                    //create a star in the relative center of the system
                    x = rnd.nextInt(size / 4) - size / 8;
                    y = 0;
                    z = rnd.nextInt(size / 4) - size / 8;
                    int r = rnd.nextInt(2 * maxPlanetSize);
                    if (r < maxPlanetSize) {
                        r = maxPlanetSize;
                    }
                    int seed = rnd.nextInt();
                    thisSystem +=
                            "[Star]\n"
                            + "name=" + systemName + "\n"
                            + "system=" + systemName + "\n"
                            + "texture=" + starTypes.get(pick).getValue("name") + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "z=" + z + "\n"
                            + "r=" + r + "\n"
                            + "seed=" + seed + "\n"
                            + "[/Star]\n\n";
                    //add a simpling for testing
                    objects.add(new Simpling(new Vector3f(x, y, z), 4 * r));
                    /*
                     * CREATE NEBULA
                     * 
                     */
                    /*int numNebula = rnd.nextInt(maxNebulaPerSystem);
                     if (numNebula < minNebulaPerSystem) {
                     numNebula = minNebulaPerSystem;
                     }
                     for (int b = 0; b < numNebula; b++) {
                     //pick texture
                     pick = rnd.nextInt(nebTypes.size());
                     Term type = nebTypes.get(pick);
                     //pick name
                     String name = "Nebula " + b;
                     //pick position
                     x = rnd.nextInt(size * 4) - size * 2;
                     y = 0;
                     z = rnd.nextInt(size * 4) - size * 2;
                     //pick dimension
                     r = rnd.nextInt(Math.min(2 * size, maxNebulaSize));
                     if (r < minNebulaSize) {
                     r = minNebulaSize;
                     }
                     int hScale = rnd.nextInt(3) + 2;
                     int h = r / hScale;
                     //pick color
                     float red = rnd.nextFloat() / 2;
                     float green = rnd.nextFloat() / 2;
                     float blue = rnd.nextFloat() / 2;
                     //seed
                     seed = rnd.nextInt();
                     //generate entry
                     thisSystem +=
                     "[Nebula]\n"
                     + "name=" + name + "\n"
                     + "system=" + systemName + "\n"
                     + "type=" + type.getValue("name") + "\n"
                     + "color=" + red + "," + green + "," + blue + "," + 1 + "\n"
                     + "x=" + x + "\n"
                     + "y=" + y + "\n"
                     + "z=" + z + "\n"
                     + "l=" + r + "\n"
                     + "w=" + h + "\n"
                     + "h=" + r + "\n"
                     + "seed=" + seed + "\n"
                     + "[/Nebula]\n\n";
                     }*/
                    /*
                     * CREATE PLANETS
                     */
                    //get list of planet assets
                    int numPlanets = rnd.nextInt(maxPlanetsPerSystem);
                    if (numPlanets < minPlanetsPerSystem) {
                        numPlanets = minPlanetsPerSystem;
                    }
                    for (int b = 0; b < numPlanets; b++) {
                        //pick texture
                        String texture = pickPlanetTexture(planetTypes);
                        //pick name
                        String name = "Planet " + b;
                        //pick seed
                        seed = rnd.nextInt();
                        //generate position
                        x = rnd.nextInt(size * 2) - size;
                        y = 0;
                        z = rnd.nextInt(size * 2) - size;
                        //generate the radius
                        r = rnd.nextInt(maxPlanetSize);
                        if (r < minPlanetSize) {
                            r = minPlanetSize;
                        }
                        //create a simpling for testing
                        Simpling test = new Simpling(new Vector3f(x, y, z), r);
                        boolean safe = true;
                        for (int c = 0; c < objects.size(); c++) {
                            if (objects.get(c).collideWith(test)) {
                                safe = false;
                                break;
                            }
                        }
                        //if it is safe add it
                        if (safe) {
                            thisSystem +=
                                    "[Planet]\n"
                                    + "name=" + name + "\n"
                                    + "system=" + systemName + "\n"
                                    + "texture=" + texture + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "r=" + r + "\n"
                                    + "seed=" + seed + "\n"
                                    + "[/Planet]\n\n";
                        }
                    }
                }
                ret += thisSystem;
            }
        }
        return ret;
    }

    private String pickPlanetTexture(ArrayList<Term> planetTypes) {
        //make a list of planet probabilities
        float[] prob = new float[planetTypes.size()];
        for (int a = 0; a < planetTypes.size(); a++) {
            prob[a] = Float.parseFloat(planetTypes.get(a).getValue("probability"));
        }
        /*
         * We know these probabilities add up to 1. The probability values therefore
         * define the stops at which that planet ends and the next one begins within
         * the spectrum.
         * 
         * That means it's basically a histogram.
         */
        //generate histogram
        float[] stops = new float[planetTypes.size()];
        for (int a = 0; a < stops.length; a++) {
            stops[a] = sumAtIndex(a, prob);
        }
        //pick a random point on the spectrum
        float val = new Random().nextFloat();
        //figure out where this lies on the array
        int index = 0;
        {
            for (int a = 1; a < stops.length; a++) {
                //determine if we can pass this stop
                //System.out.println(val + " " + stops[a-1]);
                if (val >= stops[a-1]) {
                    index = a;
                }
            }
        }
        //return the chosen one
        Term type = planetTypes.get(index);
        System.out.println(index + " :: " + type.getValue("name"));
        return type.getValue("name");
    }

    private float sumAtIndex(int index, float[] arr) {
        float sum = 0;
        for (int a = 0; a <= index; a++) {
            sum += arr[a];
        }
        return sum;
    }

    public class Simpling {
        /*
         * Class used for storing the location and radius of an object for
         * the sole purpose of avoiding collisions.
         */

        private Vector3f loc;
        private float rad;

        public Simpling(Vector3f loc, float rad) {
            this.loc = loc;
            this.rad = rad;
        }

        public boolean collideWith(Simpling test) {
            Vector3f testLoc = test.getLoc().clone();
            float testRad = test.getRad();
            //add the radii to get the min separation
            float minRad = testRad + rad;
            //multiply this rad by 1.5 so they can't be kissing each other
            minRad *= 1.5f;
            //determine if they are colliding using distance
            float dist = loc.distance(testLoc);
            if (dist < minRad) {
                return true;
            }
            return false;
        }

        public Vector3f getLoc() {
            return loc;
        }

        public float getRad() {
            return rad;
        }
    }
}
