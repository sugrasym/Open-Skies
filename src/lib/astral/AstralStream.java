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
package lib.astral;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author nwiehoff
 */
public class AstralStream extends OutputStream {

    private final ArrayList<OutputStream> streams;

    public AstralStream() {
        streams = new ArrayList<>();
    }

    public void add(OutputStream os) {
        streams.add(os);
    }

    public void remove(OutputStream os) {
        streams.remove(os);
    }

    public void clear() {
        streams.clear();
    }

    public ArrayList<OutputStream> getStreams() {
        return streams;
    }

    @Override
    public void write(final int data) throws IOException {
        for (Iterator<OutputStream> it = streams.iterator(); it.hasNext();) {
            OutputStream stream = it.next();
            stream.write(data);
        }
    }

    @Override
    public void write(final byte[] data) throws IOException {
        for (Iterator<OutputStream> it = streams.iterator(); it.hasNext();) {
            OutputStream stream = it.next();
            stream.write(data);
        }
    }

    @Override
    public void write(final byte[] data, final int offset, final int length) throws IOException {
        for (Iterator<OutputStream> it = streams.iterator(); it.hasNext();) {
            OutputStream stream = it.next();
            stream.write(data, offset, length);
        }
    }

    @Override
    public void flush() throws IOException {
        for (Iterator<OutputStream> it = streams.iterator(); it.hasNext();) {
            OutputStream stream = it.next();
            stream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (Iterator<OutputStream> it = streams.iterator(); it.hasNext();) {
            OutputStream stream = it.next();
            stream.close();
        }
    }
}
