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

/*
 * The quote window at the start of the game.
 */
package gdi;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import engine.Core;
import gdi.component.AstralLabel;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import lib.astral.Parser;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class QuoteWindow extends AstralWindow {
    
    float living = 0;
    private static final float MAX_LIFE = 15;

    AstralLabel quoteLabel = new AstralLabel();
    private final Core engine;

    public QuoteWindow(AssetManager assets, Core engine) {
        super(assets, 800, 600, false);
        this.engine = engine;
        generate();
    }

    private void generate() {
        backColor = Color.BLACK;
        //setup dimensions
        setWidth(800);
        setHeight(600);
        //setup quote label
        quoteLabel.setText("Placeholder");
        quoteLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        quoteLabel.setX(0);
        quoteLabel.setY(0);
        quoteLabel.setWidth(getWidth());
        quoteLabel.setHeight(getHeight());
        quoteLabel.setVisible(true);
        quoteLabel.setFontColor(Color.WHITE);
        //get quote
        makeQuote();
        //add components
        addComponent(quoteLabel);
        //make visible
        setVisible(true);
        setFocused(false);
    }
    
    private void makeQuote() {
        Parser parse = Universe.getCache().getQuoteCache();
        Term pick = parse.getTerms().get(new Random().nextInt(parse.getTerms().size()));
        //build the quote
        String quote = pick.getValue("body").replace("/br/", "\n");
        quote += "\n\n";
        quote += pick.getValue("source").replace("/br/", "\n");
        //store the quote
        quoteLabel.setText(quote);
    }
    
    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        setFocused(false);
        living = MAX_LIFE + 1;
    }
    
    public void update(float tpf) {
        living += tpf;
    }
    
    public boolean doneShowing() {
        if(living > MAX_LIFE) {
            living = 0;
            return true;
        }
        return false;
    }
}
