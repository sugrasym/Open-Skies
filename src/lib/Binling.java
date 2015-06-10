/*
 * Copyright (c) 2015 Nathan Wiehoff
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
