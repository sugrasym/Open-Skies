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
 * Reads/writes file data. Nuff said. Nathan Wiehoff, masternerdguy@yahoo.com
 */
package lib.astral;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import universe.Universe;

public class AstralIO implements Serializable {

    public static final String RESOURCE_DIR = "/resource/";
    public static final String HOME_DIR = "/.outlier/";
    public static final String SAVE_GAME_DIR = "/saves/";
    public static final String PAYLOAD_FILE = "/payload.txt";

    public static String readFile(String target, boolean local) {
        String ret = "";
        //Attemps to load an external file (local = false) or a file from within the archive
        if (local) {
            try {
                ret = readTextFromJar(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                try (BufferedReader in = new BufferedReader(new FileReader(target))) {
                    String str;
                    while ((str = in.readLine()) != null) {
                        ret += str + "\n";
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static void setupGameDir() {
        deployRootDir();
        deploySaveDir();
        deployControlPayload();
    }

    /*
     * Deploys the saved game directory
     */
    public static void deploySaveDir() {
        //create the subfolder
        String saves = getSaveDir();
        File saveFolder = new File(saves);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }
    }

    /*
     * Deploys the root of the game directory structure
     */
    public static void deployRootDir() {
        //create the main folder
        String home = getHomeDir();
        File homeFolder = new File(home);
        if (!homeFolder.exists()) {
            homeFolder.mkdir();
        }
    }

    /*
     * Deploys initial control mappings
     */
    public static void deployControlPayload() {
        //deploy initial control mapping
        File file = new File(getPayloadFile());
        if (!file.exists()) {
            String payload = readTextFromJar("PAYLOAD.txt"); //I actually typed "string" instead of "String", filfthy C#
            writeFile(getPayloadFile(), payload);
        }
    }

    public static String getHomeDir() {
        return System.getProperty("user.home") + "/" + HOME_DIR;
    }

    public static String getSaveDir() {
        return System.getProperty("user.home") + HOME_DIR + SAVE_GAME_DIR;
    }

    public static String getPayloadFile() {
        return System.getProperty("user.home") + HOME_DIR + PAYLOAD_FILE;
    }

    public static void writeFile(String target, String text) {
        try {
            FileWriter fstream = new FileWriter(target);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(text);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Image loadImage(String target) {
        Image tmp = null;
        try {
            System.out.println("Loading image resource " + RESOURCE_DIR + target);
            tmp = Toolkit.getDefaultToolkit().getImage(getClass().getResource(RESOURCE_DIR + target));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public static String readTextFromJar(String target) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        String ret = "";

        try {
            is = AstralIO.class.getResourceAsStream(RESOURCE_DIR + target);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                ret = ret + line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public void saveGame(Universe universe, String gameName) throws Exception {
        //generate serializable universe
        Everything everything = new Everything(universe);
        //serialize universe
        FileOutputStream fos = new FileOutputStream(getSaveDir() + gameName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(everything);
    }

    public class Everything implements Serializable {
        /*
         * This class contains everything in the universe in a temporary container
         * useful for serialization.
         */

        protected Universe universe;

        public Everything(Universe universe) {
            this.universe = universe;
        }

        public Universe getUniverse() {
            return universe;
        }

        public void setUniverse(Universe universe) {
            this.universe = universe;
        }
    }
}
