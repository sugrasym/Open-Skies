/*
 * Copyright (c) 2016 SUGRA-SYM LLC (Nathan Wiehoff, Geoffrey Hibbert)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * Program for generating universes. This allows decent looking, arbitrarily
 * large worlds to be made automatically, and then manaully tuned.
 */
package lib.astral;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Random;
import lib.Faction;
import lib.SuperFaction;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class WorldMaker {
    //sample

    private final char[] generic = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
        'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2',
        '3', '4', '5', '6', '7', '8', '9', '0'};
    private final String[] greek = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota",
        "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi",
        "Psi", "Omega"};
    private final String[] namePrefixModifiers = {"New", "Cape", "Port"};
    private final String[] nameSuffixModifiers = {"Minor", "Major"};
    //used names
    private final ArrayList<String> usedSystemNames = new ArrayList<>();
    private final ArrayList<String> usedPlanetNames = new ArrayList<>();
    //rng
    Random rnd;

    public WorldMaker() {
        //generate universe
        String out = generate(31337, 0, 8, 80, 100, 1000, 64000,
                200000, 800, 6500, 0, 0.40f, 0.40f);
        //save
        AstralIO.writeFile("new-UNIVERSE.txt", out);
        System.out.println(out);
    }

    public static void main(String[] args) {
        new WorldMaker();
    }

    public final String generate(int masterSeed, int minPlanetsPerSystem, int maxPlanetsPerSystem, int minSystems, int maxSystems,
            int worldSize, int minSystemSize, int maxSystemSize, int minPlanetSize,
            int maxPlanetSize, int minNebulaPerSystem, float nebulaProbability, float fieldProbability) {
        rnd = new Random(masterSeed);
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
            Parser fields = new Parser("FIELD.txt");
            ArrayList<Term> fieldTypes = fields.getTermsOfType("Field");
            //determine the number of systems to make
            int numSystems = rnd.nextInt(maxSystems);
            if (numSystems < minSystems) {
                numSystems = minSystems;
            }
            //generate syslings
            ArrayList<Sysling> syslings = makeSyslings(numSystems, rnd, worldSize);
            //sort syslings
            for (int a = 0; a < syslings.size(); a++) {
                syslings.get(a).sortBuddy(syslings);
            }
            //drop sov in syslings
            dropSov(syslings);
            //generate each system
            for (int a = 0; a < syslings.size(); a++) {
                System.out.println("Generating Universe - " + (a + 1) + "/" + syslings.size());
                ArrayList<Simpling> objects = new ArrayList<>();
                String thisSystem = "";
                {
                    //pick size
                    int size = rnd.nextInt(maxSystemSize);
                    if (size < minSystemSize) {
                        size = minSystemSize;
                    }
                    //determine skybox
                    int pick = rnd.nextInt(skyTypes.size());
                    //get sysling
                    Sysling sys = syslings.get(a);
                    int x = (int) sys.getLoc().x;
                    int y = (int) sys.getLoc().y;
                    int z = (int) sys.getLoc().z;
                    String systemName = sys.getName();
                    String owner = sys.getOwner();
                    //create the system entry
                    thisSystem
                            += "[System]\n"
                            + "name=" + systemName + "\n"
                            + "owner=" + owner + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "z=" + z + "\n"
                            + "sky=" + skyTypes.get(pick).getValue("name") + "\n"
                            + "ambient=" + sys.getAmbientMusic() + "\n"
                            + "danger=" + sys.getDangerMusic() + "\n"
                            + "[/System]\n\n";
                    //get star types
                    pick = rnd.nextInt(starTypes.size());
                    //create a star in the relative center of the system
                    x = rnd.nextInt(size / 4) - size / 8;
                    y = 0;
                    z = rnd.nextInt(size / 4) - size / 8;
                    int r = rnd.nextInt(2 * maxPlanetSize);
                    if (r < minPlanetSize) {
                        r = minPlanetSize;
                    }
                    int seed = rnd.nextInt();

                    //stars can have a 50% variation from white on each axis
                    float colR = 0.5f + (rnd.nextFloat() * 0.5f);
                    float colG = 0.5f + (rnd.nextFloat() * 0.5f);
                    float colB = 0.5f + (rnd.nextFloat() * 0.5f);

                    //bring the highest axis to 1.0 and raise the others proportionately
                    float[] axes = new float[]{colR, colG, colB};

                    float diff = Float.POSITIVE_INFINITY;
                    for (int n = 0; n < 3; n++) {
                        if ((1.0f - axes[n]) < diff) {
                            diff = 1.0f - axes[n];
                        }
                    }

                    float[] scaledAxes = new float[]{
                        axes[0] + diff,
                        axes[1] + diff,
                        axes[2] + diff
                    };

                    thisSystem
                            += "[Star]\n"
                            + "name=" + systemName + "\n"
                            + "system=" + systemName + "\n"
                            + "texture=" + starTypes.get(pick).getValue("name") + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "z=" + z + "\n"
                            + "r=" + r + "\n"
                            + "color=" + scaledAxes[0] + "," + scaledAxes[1] + "," + scaledAxes[2] + "\n"
                            + "seed=" + seed + "\n"
                            + "[/Star]\n\n";
                    //add a simpling for testing
                    objects.add(new Simpling(new Vector3f(x, y, z), 4 * r));
                    /*
                     * CREATE NEBULA
                     * 
                     */
                    if (rnd.nextFloat() < nebulaProbability) {
                        //pick texture
                        pick = rnd.nextInt(nebTypes.size());
                        Term type = nebTypes.get(pick);
                        //pick name
                        String name = systemName + " Cloud";
                        //set for center and span of system
                        x = 0;
                        y = 0;
                        z = 0;
                        r = size * 3;
                        //int hScale = rnd.nextInt(3) + 2;
                        int h = 32000;
                        //pick color
                        float red = rnd.nextFloat() / 50;
                        float green = rnd.nextFloat() / 50;
                        float blue = rnd.nextFloat() / 50;
                        //seed
                        seed = rnd.nextInt();
                        //generate entry
                        thisSystem
                                += "[Nebula]\n"
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
                    }
                    /*
                     * CREATE FIELDS
                     */
                    if (rnd.nextFloat() < fieldProbability) {
                        //pick texture
                        pick = rnd.nextInt(fieldTypes.size());
                        Term type = fieldTypes.get(pick);
                        //pick name
                        String name = systemName + " Field";
                        //set for center and span of system
                        x = 0;
                        y = 0;
                        z = 0;
                        r = size * 3;
                        //int hScale = rnd.nextInt(3) + 2;
                        int h = 32000;
                        //seed
                        seed = rnd.nextInt();
                        //generate entry
                        thisSystem
                                += "[Field]\n"
                                + "name=" + name + "\n"
                                + "system=" + systemName + "\n"
                                + "type=" + type.getValue("name") + "\n"
                                + "x=" + x + "\n"
                                + "y=" + y + "\n"
                                + "z=" + z + "\n"
                                + "l=" + r + "\n"
                                + "w=" + h + "\n"
                                + "h=" + r + "\n"
                                + "seed=" + seed + "\n"
                                + "[/Field]\n\n";
                    }
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
                        String name = "TMP";
                        while (true) {
                            name = randomPlanetName();
                            for (int v = 0; v < usedPlanetNames.size(); v++) {
                                if (usedPlanetNames.get(a).equals(name)) {
                                    name = "TMP";
                                }
                            }

                            if (!name.equals("TMP")) {
                                usedPlanetNames.add(name);
                                break;
                            }
                        }
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
                        //generate tilt
                        float tiltX = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                        float tiltY = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                        float tiltZ = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
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
                            thisSystem
                                    += "[Planet]\n"
                                    + "name=" + name + "\n"
                                    + "system=" + systemName + "\n"
                                    + "texture=" + texture + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "r=" + r + "\n"
                                    + "tiltX=" + tiltX + "\n"
                                    + "tiltY=" + tiltY + "\n"
                                    + "tiltZ=" + tiltZ + "\n"
                                    + "seed=" + seed + "\n"
                                    + "[/Planet]\n\n";
                        }
                    }
                    /*
                     * CREATE JUMPHOLES
                     */
                    //calculate the number of connections to make
                    int density = rnd.nextInt(4) + 1;
                    for (int v = 0; v < density; v++) {
                        //get the sysling to connect to
                        Sysling in = sys;
                        Sysling out = sys.findBuddy(null, v + 1);
                        if (!in.connectedTo(out) && !out.connectedTo(in)) {
                            //name holes
                            String inName = out.getName() + " Jumphole";
                            String outName = sys.getName() + " Jumphole";
                            //build in gate
                            x = rnd.nextInt(size * 2) - size;
                            y = 0;
                            z = rnd.nextInt(size * 2) - size;
                            thisSystem += "[Jumphole]\n"
                                    + "name=" + inName + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "system=" + in.getName() + "\n"
                                    + "out=" + out.getName() + "/" + outName + "\n"
                                    + "[/Jumphole]\n\n";
                            //build out gate
                            x = rnd.nextInt(size * 2) - size;
                            y = 0;
                            z = rnd.nextInt(size * 2) - size;
                            thisSystem += "[Jumphole]\n"
                                    + "name=" + outName + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "system=" + out.getName() + "\n"
                                    + "out=" + in.getName() + "/" + inName + "\n"
                                    + "[/Jumphole]\n\n";
                            //inform simplings
                            sys.addConnection(out);
                            out.addConnection(sys);
                        }
                    }
                    /*
                     * CREATE INITIAL STATIONS
                     */
                    ArrayList<Statling> stations = sys.getStations();
                    for (int b = 0; b < stations.size(); b++) {
                        //pick a random planet
                        int pa = rnd.nextInt(objects.size());
                        //add owner stations near planets
                        Statling tmp = stations.get(b);
                        if (tmp.getOwner().matches(sys.getOwner())) {
                            //drop it near it
                            Simpling host = objects.get(pa);
                            //get coordinates
                            Vector3f loc = pointNearSimpling(host);
                            x = (int) loc.getX();
                            y = (int) loc.getY();
                            z = (int) loc.getZ();
                            //get initial rotation
                            float tiltX = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            float tiltY = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            float tiltZ = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            //drop
                            thisSystem += "[Station]\n"
                                    + "name=" + tmp.getName() + "\n"
                                    + "system=" + systemName + "\n"
                                    + "station=" + tmp.getType() + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "tiltX=" + tiltX + "\n"
                                    + "tiltY=" + tiltY + "\n"
                                    + "tiltZ=" + tiltZ + "\n"
                                    + "faction=" + tmp.getOwner() + "\n"
                                    + "[/Station]\n\n";
                        } else {
                            //it is probably a pirate base drop it somewhere
                            x = rnd.nextInt(2 * size) - size;
                            y = 0; // keep it near the ecliptic for now
                            z = rnd.nextInt(2 * size) - size;
                            //get initial rotation
                            float tiltX = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            float tiltY = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            float tiltZ = (rnd.nextFloat() - 0.5f) * FastMath.TWO_PI;
                            //drop
                            thisSystem += "[Station]\n"
                                    + "name=" + tmp.getName() + "\n"
                                    + "system=" + systemName + "\n"
                                    + "station=" + tmp.getType() + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "z=" + z + "\n"
                                    + "tiltX=" + tiltX + "\n"
                                    + "tiltY=" + tiltY + "\n"
                                    + "tiltZ=" + tiltZ + "\n"
                                    + "faction=" + tmp.getOwner() + "\n"
                                    + "[/Station]\n\n";
                        }
                    }
                }
                ret += thisSystem;
            }
        }
        return ret;
    }

    private Vector3f pointNearSimpling(Simpling host) {
        //pick a point outside of the planet
        float r = host.getRad() * 2;
        float dx = (rnd.nextInt(10000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
        float dy = (rnd.nextInt(2000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
        float dz = (rnd.nextInt(10000) + r) * Math.signum(rnd.nextFloat() - 0.5f);
        //store
        float x = host.getLoc().getX() + dx;
        float y = host.getLoc().getY() + dy;
        float z = host.getLoc().getZ() + dz;
        //make a point
        Vector3f pnt = new Vector3f(x, y, z);
        return pnt;
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
                if (val >= stops[a - 1]) {
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

    private ArrayList<Sysling> makeSyslings(int numSystems, Random rnd, int worldSize) {
        //generate syslings
        ArrayList<Sysling> syslings = new ArrayList<>();
        for (int a = 0; a < numSystems; a++) {
            //determine map location
            int x = rnd.nextInt(worldSize * 2) - worldSize;
            int y = rnd.nextInt(worldSize * 2) - worldSize;
            int z = rnd.nextInt(worldSize * 2) - worldSize;
            //pick name
            String name = null;
            while (name == null) {
                name = randomSystemName(generic);
            }
            //make sysling
            Sysling test = new Sysling(name, new Vector3f(x, y, z));
            //check for collission
            boolean safe = true;
            for (int v = 0; v < syslings.size(); v++) {
                if (syslings.get(v).collideWith(test)) {
                    safe = false;
                    break;
                }
            }
            //add if safe
            if (safe) {
                syslings.add(test);
            }
        }
        return syslings;
    }

    private String randomName() {
        /*
         * Generates a random name for a planet
         */
        ArrayList<Term> fg = Universe.getCache().getNameCache().getTermsOfType("First");
        ArrayList<Term> lg = Universe.getCache().getNameCache().getTermsOfType("Last");
        String first = "";
        String last = "";
        {
            for (int a = 0; a < fg.size(); a++) {
                if (fg.get(a).getValue("name").equals("Generic")) {
                    Parser.Param pick = fg.get(a).getParams().get(rnd.nextInt(fg.get(a).getParams().size() - 1) + 1);
                    first = pick.getValue();
                    break;
                }
            }

            for (int a = 0; a < lg.size(); a++) {
                if (lg.get(a).getValue("name").equals("Generic")) {
                    Parser.Param pick = lg.get(a).getParams().get(rnd.nextInt(lg.get(a).getParams().size() - 1) + 1);
                    last = pick.getValue();
                    break;
                }
            }
        }
        if (rnd.nextFloat() > 0.5) {
            return first;
        } else {
            return last;
        }
    }

    public String randomPlanetName() {
        String ret = "";
        {
            //pick a type
            if (rnd.nextBoolean()) {
                ArrayList<Term> city = Universe.getCache().getNameCache().getTermsOfType("City");
                for (int a = 0; a < city.size(); a++) {
                    if (city.get(a).getValue("name").equals("Generic")) {
                        Parser.Param pick = city.get(a).getParams().get(rnd.nextInt(city.get(a).getParams().size() - 1) + 1);
                        ret = pick.getValue();
                        break;
                    }
                }
            } else {
                //name based
                ret = randomName();
            }

            //wrap the base string
            if (rnd.nextFloat() > 0.65) {
                String prefix = namePrefixModifiers[rnd.nextInt(namePrefixModifiers.length)];
                ret = prefix + " " + ret;
            }
            if (rnd.nextFloat() > 0.85) {
                String suffix = nameSuffixModifiers[rnd.nextInt(nameSuffixModifiers.length)];
                ret += " " + suffix;
            }
        }
        return "'" + ret + "'";
    }

    public String randomSystemName(char[] sample) {
        String ret = "";
        {
            //prefix
            String prefix = greek[rnd.nextInt(greek.length)];
            ret += prefix + " ";
            int l1 = rnd.nextInt(4) + 1;
            //pass 1
            for (int a = 0; a < l1; a++) {
                char pick = sample[rnd.nextInt(sample.length)];
                ret += pick;
            }
            //add tack
            ret += "-";
            int l2 = rnd.nextInt(4) + 1;
            //pass 2
            for (int a = 0; a < l2; a++) {
                char pick = sample[rnd.nextInt(sample.length)];
                ret += pick;
            }
            //safety check
            for (int v = 0; v < usedSystemNames.size(); v++) {
                if (usedSystemNames.get(v).equals(ret)) {
                    return null;
                }
            }
        }
        //add
        usedSystemNames.add(ret);
        //return
        return ret;
    }

    private void dropSov(ArrayList<Sysling> syslings) {
        /*
         * Seeds factions
         */
        //make a list of all factions
        ArrayList<SuperFaction> factions = new ArrayList<>();
        Parser fParse = new Parser("FACTIONS.txt");
        ArrayList<Term> terms = fParse.getTermsOfType("Faction");
        for (int a = 0; a < terms.size(); a++) {
            factions.add(new SuperFaction(null, terms.get(a).getValue("name")));
        }
        //for each sov holding faction pick a capital
        for (int a = 0; a < factions.size(); a++) {
            if (factions.get(a).isEmpire()) {
                //pick a random system as the capital
                Sysling pick = null;
                while (pick == null) {
                    Sysling tmp = syslings.get(rnd.nextInt(syslings.size()));
                    if (tmp.getOwner().equals("Neutral")) {
                        pick = tmp;
                    }
                }
                //mark it
                pick.setOwner(factions.get(a).getName());
                //pick music
                ArrayList<String> ambientMusic = factions.get(a).getAmbientMusic();
                if (ambientMusic.size() > 0) {
                    pick.setAmbientMusic(ambientMusic.get(rnd.nextInt(ambientMusic.size())));
                }
                ArrayList<String> dangerMusic = factions.get(a).getDangerMusic();
                if (dangerMusic.size() > 0) {
                    pick.setDangerMusic(dangerMusic.get(rnd.nextInt(dangerMusic.size())));
                }
                //determine system count
                int numSystems = (int) (factions.get(a).getSpread() * syslings.size());
                int offset = 0;
                //pick unclaimed systems by proximity
                for (int x = 0; x < numSystems + offset; x++) {
                    if ((x + offset) < syslings.size()) {
                        Sysling tmp = pick.findBuddy(syslings, x + offset);
                        if (tmp.getOwner().equals("Neutral")) {
                            tmp.setOwner(factions.get(a).getName());
                            //pick music
                            if (ambientMusic.size() > 0) {
                                pick.setAmbientMusic(ambientMusic.get(rnd.nextInt(ambientMusic.size())));
                            }
                            if (dangerMusic.size() > 0) {
                                pick.setDangerMusic(dangerMusic.get(rnd.nextInt(dangerMusic.size())));
                            }
                        } else {
                            offset += 1;
                        }
                    } else {
                        System.out.println(factions.get(a).getName() + " ran out of space to claim sov!");
                        //no room left
                    }
                }
            } else {
                //these will be handled in the next pass
            }
        }
        //build stations
        for (int a = 0; a < factions.size(); a++) {
            dropStations(syslings, factions.get(a));
        }
        //report
        for (int a = 0; a < syslings.size(); a++) {
            if (!syslings.get(a).getOwner().equals("Neutral")) {
                System.out.println("Sysling " + a + " claimed by " + syslings.get(a).getOwner());
            }
        }
    }

    private void dropStations(ArrayList<Sysling> syslings, Faction faction) {
        ArrayList<Sysling> simp = new ArrayList<>();
        if (faction.isEmpire()) {
            //make a list of this faction's systems
            for (int a = 0; a < syslings.size(); a++) {
                if (syslings.get(a).getOwner().equals(faction.getName())) {
                    simp.add(syslings.get(a));
                }
            }
        } else {
            //figure out who hosts this faction
            ArrayList<String> hosts = faction.getHosts();
            for (int a = 0; a < hosts.size(); a++) {
                //make a list of the host's systems
                for (int v = 0; v < syslings.size(); v++) {
                    if (syslings.get(v).getOwner().equals(hosts.get(a))) {
                        simp.add(syslings.get(v));
                    }
                }
            }
        }
        //get a list of stations for this faction
        Parser sParse = new Parser("FACTIONS.txt");
        ArrayList<Term> terms = sParse.getTermsOfType("Stations");
        Term stat = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").equals(faction.getName())) {
                stat = terms.get(a);
            }
        }
        if (stat != null) {
            //get types of stations
            int a = 0;
            String type;
            while ((type = stat.getValue("station" + a)) != null) {
                //get station info
                String ty = type.split(",")[0];
                double spread = Float.parseFloat(type.split(",")[1]);
                //calculate the number of stations (guaranteeing at least 1!)
                int count = (int) (spread * simp.size()) + 1;
                //place them
                for (int v = 0; v < count; v++) {
                    String name = ty + " " + (v + 1);
                    Statling tr = new Statling(name, ty, faction.getName());
                    //put it in a random system owned by this faction
                    int pick = rnd.nextInt(simp.size());
                    simp.get(pick).getStations().add(tr);
                }
                //iterate
                a++;
            }
        } else {
            System.out.println(faction.getName() + " doesn't have any stations!");
        }
    }

    public class Sysling {
        /*
         * Solar system template used for jump hole mapping.
         */

        private final Vector3f loc;
        private Sysling[] neighbors;
        private final ArrayList<Sysling> connections = new ArrayList<>();
        private final ArrayList<Statling> stations = new ArrayList<>();
        private final String name;
        private String owner = "Neutral";
        //todo: update audio when doing sound system (these paths will fail)
        private String ambientMusic = "Audio/Music/Undefined.wav";
        private String dangerMusic = "Audio/Music/Committing.wav";
        private final SuperFaction neutralFaction = new SuperFaction(null, "Neutral");

        public Sysling(String name, Vector3f loc) {
            this.loc = loc;
            this.name = name;
            //pick neutral ambient music
            ArrayList<String> am = neutralFaction.getAmbientMusic();
            if (am.size() > 0) {
                setAmbientMusic(am.get(rnd.nextInt(am.size())));
            }
            //pick danger music
            ArrayList<String> dm = neutralFaction.getDangerMusic();
            if (dm.size() > 0) {
                setDangerMusic(dm.get(rnd.nextInt(dm.size())));
            }
        }

        public void addConnection(Sysling sys) {
            connections.add(sys);
        }

        public boolean connectedTo(Sysling test) {
            return connections.contains(test);
        }

        public void sortBuddy(ArrayList<Sysling> verse) {
            /*
             * Sorts the syslings by distance ascending.
             */
            Sysling[] arr = (Sysling[]) verse.toArray(new Sysling[verse.size()]);
            for (int a = 0; a < arr.length; a++) {
                for (int b = 1; b < arr.length - a; b++) {
                    if (distance(arr[b - 1]) > distance(arr[b])) {
                        Sysling tmp = arr[b];
                        arr[b] = arr[b - 1];
                        arr[b - 1] = tmp;
                    }
                }
            }
            neighbors = arr;
        }

        public Sysling findBuddy(ArrayList<Sysling> verse, int place) {
            /*
             * Finds Nth closest sysling to this one. Nth = place.
             */
            if (neighbors == null) {
                sortBuddy(verse);
            }
            return neighbors[place];
        }

        public boolean collideWith(Sysling test) {
            return test.getName().equals(name);
        }

        public double distance(Sysling comp) {
            return loc.distance(comp.getLoc());
        }

        public Vector3f getLoc() {
            return loc;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public ArrayList<Statling> getStations() {
            return stations;
        }

        public String getAmbientMusic() {
            return ambientMusic;
        }

        public final void setAmbientMusic(String ambientMusic) {
            this.ambientMusic = ambientMusic;
        }

        public String getDangerMusic() {
            return dangerMusic;
        }

        public final void setDangerMusic(String dangerMusic) {
            this.dangerMusic = dangerMusic;
        }
    }

    public class Statling {
        /*
         * Simple structure for storing a station template
         */

        private String name;
        private String type;
        private String owner;

        public Statling(String name, String type, String owner) {
            this.name = name;
            this.type = type;
            this.owner = owner;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }
    }

    public class Simpling {
        /*
         * Class used for storing the location and radius of an object for
         * the sole purpose of avoiding collisions.
         */

        private final Vector3f loc;
        private final float rad;

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
            return dist < minRad;
        }

        public Vector3f getLoc() {
            return loc;
        }

        public float getRad() {
            return rad;
        }
    }
}
