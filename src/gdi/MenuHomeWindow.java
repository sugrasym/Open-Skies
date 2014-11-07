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
 * The home window used in the main menu
 */
package gdi;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import engine.Core;
import gdi.component.AstralLabel;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.Font;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.astral.AstralIO;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class MenuHomeWindow extends AstralWindow {

    private enum InternalState {

        NORMAL,
        PRENEW,
        PRELOAD,
    }

    AstralLabel logoLabel = new AstralLabel();
    AstralLabel versionLabel = new AstralLabel();
    AstralList mainList = new AstralList(this);
    AstralList gameList = new AstralList(this);
    AstralList saveList = new AstralList(this);
    private final Core engine;
    private InternalState state = InternalState.NORMAL;
    private int stateCounter = 0;
    private String loadGameName;

    public MenuHomeWindow(AssetManager assets, Core engine) {
        super(assets, 800, 600, false);
        this.engine = engine;
        generate();
    }

    private void generate() {
        setFocused(true);
        //a nice color
        backColor = windowBlue;
        //setup dimensions
        setWidth(800);
        setHeight(600);
        //setup logo label
        logoLabel.setText("Outlier: Open Skies");
        logoLabel.setFont(new Font("Monospaced", Font.PLAIN, 36));
        logoLabel.setX(0);
        logoLabel.setY(0);
        logoLabel.setWidth(getWidth());
        logoLabel.setHeight(50);
        logoLabel.setVisible(true);
        //setup version label
        versionLabel.setText("Alpha 3.1");
        versionLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        versionLabel.setX(0);
        versionLabel.setY(height - 17);
        versionLabel.setWidth(getWidth());
        versionLabel.setHeight(50);
        versionLabel.setVisible(true);
        //setup menu list
        mainList.setX(getWidth() / 2 - 200);
        mainList.setY(getHeight() / 2 - 200);
        mainList.setWidth(400);
        mainList.setHeight(400);
        mainList.setVisible(true);
        mainList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        populateMainMenuList();
        //setup save game list
        gameList.setX(getWidth() / 2 - 200);
        gameList.setY(getHeight() / 2 - 200);
        gameList.setWidth(400);
        gameList.setHeight(400);
        gameList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gameList.setVisible(false);
        //setup load game list
        saveList.setX(getWidth() / 2 - 200);
        saveList.setY(getHeight() / 2 - 200);
        saveList.setWidth(400);
        saveList.setHeight(400);
        saveList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        saveList.setVisible(false);
        //add components
        addComponent(logoLabel);
        addComponent(versionLabel);
        addComponent(mainList);
        addComponent(gameList);
        addComponent(saveList);
        //make visible
        setVisible(true);
    }

    public void update(float tpf) {
        if (getState() != InternalState.NORMAL) {
            if (getStateCounter() > 2) {
                if (getState() == InternalState.PRENEW) {
                    engine.newGame("Default");
                } else if(getState() == InternalState.PRELOAD) {
                    engine.load(getLoadGameName());
                }
                resetState();
            }
            
            //increment state counter
            setStateCounter(getStateCounter() + 1);
        }
    }

    private void resetState() {
        //reset state
        setState(InternalState.NORMAL);
        setStateCounter(0);
        setLoadGameName(null);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (mainList.isVisible()) {
            populateMainMenuList();
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        super.handleMouseReleasedEvent(me, mouseLoc);
        String command = "";
        if (mainList.isVisible()) {
            command = (String) mainList.getItemAtIndex(mainList.getIndex());
        } else if (gameList.isVisible()) {
            command = (String) gameList.getItemAtIndex(gameList.getIndex());
        }
        parseCommand(command);
    }

    private void parseCommand(String command) {
        if (mainList.isVisible()) {
            if (command.matches("New Game")) {
                setVisible(false);
                setState(InternalState.PRENEW);
            } else if (command.matches("Load Quicksave")) {
                setVisible(false);
                setLoadGameName("Quick");
                setState(InternalState.PRELOAD);
            } else if (command.matches("Load Game")) {
                mainList.setVisible(false);
                saveList.setVisible(false);
                populateLoadGameList();
                gameList.setVisible(true);
            } else if (command.matches("Save Game")) {
                mainList.setVisible(false);
                gameList.setVisible(false);
                populateSaveGameList();
                saveList.setVisible(true);
            } else if (command.matches("Exit")) {
                //I don't hate you
                System.exit(0);
            }
        } else if (gameList.isVisible()) {
            int index = gameList.getIndex();
            if (index > 2) {
                setVisible(false);
                setLoadGameName((String) gameList.getItemAtIndex(index));
                setState(InternalState.PRELOAD);
            } else if (index == 1) {
                mainList.setVisible(true);
                gameList.setVisible(false);
                saveList.setVisible(false);
                populateMainMenuList();
            }
        } else if (saveList.isVisible()) {
            int index = saveList.getIndex();
            if (index > 2) {
                try {
                    new AstralIO().saveGame(engine.getUniverse(), (String) saveList.getItemAtIndex(index));
                    populateSaveGameList();
                } catch (Exception ex) {
                    Logger.getLogger(MenuHomeWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (index == 1) {
                mainList.setVisible(true);
                gameList.setVisible(false);
                saveList.setVisible(false);
                populateMainMenuList();
            }
        }
    }

    private void populateMainMenuList() {
        mainList.clearList();
        //store text
        mainList.addToList("Select an Option");
        mainList.addToList("");
        mainList.addToList("New Game");
        mainList.addToList("");
        mainList.addToList("Load Quicksave");
        mainList.addToList("Load Game");
        if (engine.getUniverse() != null) {
            mainList.addToList("");
            mainList.addToList("Save Game");
        }
        mainList.addToList("");
        mainList.addToList("Exit");
    }

    private void populateSaveGameList() {
        saveList.clearList();
        //add menu cruft
        saveList.addToList("Select a Game to Save");
        saveList.addToList("Return to Main Menu");
        saveList.addToList("");
        addSaves(saveList);
        saveList.addToList("Game " + countSaves());
    }

    private void populateLoadGameList() {
        gameList.clearList();
        //add menu cruft
        gameList.addToList("Select a Game to Load");
        gameList.addToList("Return to Main Menu");
        gameList.addToList("");
        addSaves(gameList);
    }

    private Universe getUniverse() {
        return engine.getUniverse();
    }

    private int countSaves() {
        String path = AstralIO.getSaveDir();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        return listOfFiles.length;
    }

    private void addSaves(AstralList list) {
        //add all the files
        String path = AstralIO.getSaveDir();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                list.addToList(listOfFiles[i].getName());
            }
        }
    }

    private InternalState getState() {
        return state;
    }

    private void setState(InternalState state) {
        this.state = state;
    }

    private int getStateCounter() {
        return stateCounter;
    }

    private void setStateCounter(int stateCounter) {
        this.stateCounter = stateCounter;
    }

    private String getLoadGameName() {
        return loadGameName;
    }

    private void setLoadGameName(String loadGameName) {
        this.loadGameName = loadGameName;
    }
}
