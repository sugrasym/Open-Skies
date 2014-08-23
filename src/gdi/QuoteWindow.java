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
    private static final float MAX_LIFE = 5;

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
