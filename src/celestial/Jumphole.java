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
 * This is the method by which ships can move between solar systems.
 */
package celestial;

import entity.Entity;
import universe.SolarSystem;
import universe.Universe;

public class Jumphole extends Planet {

    private Jumphole outGate;
    private Universe universe;
    protected String out = "n/n";

    public Jumphole(String name, Universe universe) {
        super(universe, name, null, 200); /*null type is why you're getting an error - you need to define a logical texture for jumpgates.*/
        this.universe = universe;
    }

    public void createLink(String out) {
        /*
         * Locates this gate's partner in the target solar system.
         */
        String outSysTmp = out.split("/")[0];
        String outGateTmp = out.split("/")[1];
        //find the out link
        for (int a = 0; a < universe.getSystems().size(); a++) {
            SolarSystem curr = universe.getSystems().get(a);
            if (curr.getName().matches(outSysTmp)) {
                for (int b = 0; b < curr.getCelestials().size(); b++) {
                    Entity entity = curr.getCelestials().get(b);
                    if (entity instanceof Jumphole) {
                        if (entity.getName().matches(outGateTmp)) {
                            outGate = (Jumphole) entity;
                            outGate.linkWithPartner(this);
                        }
                    }
                }
            }
        }
    }

    public void linkWithPartner(Jumphole gate) {
        outGate = gate;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }
}
