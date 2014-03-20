/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import java.io.Serializable;
import java.util.ArrayList;

public class Binling implements Serializable {
    /*
     * Class for storing strings paired with a double. It is capable
     * of storing as many strings as you like. For compatibility
     * with the original binling the getStr() and setStr() methods work
     * the same as before.
     */

    private final ArrayList<String> str = new ArrayList<>();
    private double num;

    public Binling(String str, double num) {
        this.str.add(str);
        this.num = num;
    }

    public String getString() {
        return getStr().get(0);
    }

    public void setString(String str) {
        this.getStr().set(0, str);
    }

    public double getDouble() {
        return num;
    }

    public void setDouble(double num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return getStr().get(0);
    }

    public ArrayList<String> getStr() {
        return str;
    }
}
