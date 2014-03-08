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
 * Parses game world data from files. It is up to individual classes how to
 * interpret the data they get.
 * 
 * For an explanation, see src/resource/Markup
 */
package lib;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class Parser implements Serializable {

    String text;
    private ArrayList<Term> terms = new ArrayList<>();

    public Parser(String file) {
        //read the text
        text = AstralIO.readFile(file, true);
        //call the parser
        parse();
    }

    /*
     * Parser
     */
    private void parse() {
        /*
         * Parses the loaded text into Terms.
         */
        //split into seperate lines
        String[] arr = text.split("\n");
        //loop through each element
        boolean grabbing = false;
        String tmp = "";
        for (int a = 0; a < arr.length; a++) {
            /*
             * This segment is kind of nasty looking but actually simple.
             */
            char[] line = arr[a].toCharArray();
            if (line.length >= 3) {
                if (line[0] == '[' && line[line.length - 1] == ']') {
                    if (line[1] == '/') {
                        grabbing = false;
                        try {
                            terms.add(extractTerm(tmp));
                            tmp = "";
                        } catch (Exception e) {
                        }
                    } else {
                        grabbing = true;
                    }
                }
            }
            if (grabbing) {
                tmp += arr[a] + "\n";
            }
        }
    }

    private Term extractTerm(String raw) throws Exception {
        /*
         * Assuming that raw is a properly isolated term, this method will
         * generate a Term object.
         */
        String[] arr = raw.split("\n");
        Term term = null;
        {
            //the first line should contain the term's name
            String name = arr[0].replace("[", "").replace("]", "");
            term = new Term(name);
            //the other lines are terms
            for (int a = 1; a < arr.length; a++) {
                try {
                    String[] tmp = arr[a].split("=");
                    Param param = new Param(tmp[0], tmp[1]);
                    term.addParam(param);
                } catch (Exception e) {
                }
            }
        }
        return term;
    }

    /*
     * Accessors
     */
    public ArrayList<Term> getTerms() {
        return terms;
    }

    public ArrayList<Term> getTermsOfType(String type) {
        /*
         * Returns all the terms of a specific type
         */
        ArrayList<Term> tmp = new ArrayList<>();
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getName().matches(type)) {
                tmp.add(terms.get(a));
            }
        }
        return tmp;
    }

    /*
     * Container classes
     */
    public class Term implements Serializable {
        /*
         * Stores a term, which is a collection of params with a name.
         */

        private ArrayList<Param> params = new ArrayList<>();
        private final String name;

        public Term(String name) {
            this.name = name;
        }

        /*
         * Access and limited mutation
         */
        public void addParam(String name, String value) {
            Param tmp = new Param(name, value);
            params.add(tmp);
        }

        public void addParam(Param param) {
            params.add(param);
        }

        public ArrayList<Param> getParams() {
            return params;
        }

        public String getName() {
            return name;
        }

        public void setValue(String paramName, String value) {
            for (int a = 0; a < params.size(); a++) {
                if (params.get(a).getName().matches(paramName)) {
                    params.get(a).setValue(value);
                }
            }
        }

        /*
         * Search function
         */
        public String getValue(String paramName) {
            if (params != null && paramName != null) {
                for (int a = 0; a < params.size(); a++) {
                    if (params.get(a).getName().matches(paramName)) {
                        return params.get(a).getValue();
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        /*
         * Utility and debugging
         */
        @Override
        public String toString() {
            String ret = "[" + name + "]";
            for (int a = 0; a < params.size(); a++) {
                ret += ("\n" + params.get(a).toString());
            }
            ret += ("\n[/" + name + "]");
            return ret;
        }
    }

    public class Param implements Serializable {
        /*
         * Stores a param, which is a parameter and its associated value.
         * They should be immutable. Why would you want to change the 
         * parameters you are currently loading?
         */

        private final String name;
        private String value;

        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /*
         * Access and mutation
         */
        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        /*
         * Utility and Debugging
         */
        @Override
        public String toString() {
            return (name + "=" + value);
        }
    }
}
