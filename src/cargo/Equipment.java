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
 * Equipment is a framwork class for things that can be mounted to hard points.
 */
package cargo;

import celestial.Celestial;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import java.io.Serializable;

/**
 *
 * @author nwiehoff
 */
public class Equipment extends Item implements Serializable {

    protected boolean active = false;
    protected boolean enabled = true;
    protected double activationTimer;
    protected double coolDown;
    protected Ship host;
    protected Hardpoint socket;
    protected float range;
    protected float tpf;

    public Equipment(String name) {
        super(name);
    }

    @Override
    public void periodicUpdate(double tpf) {
        //update timer
        if (getActivationTimer() <= getCoolDown()) {
            setActivationTimer(getActivationTimer() + tpf);
        }
        this.tpf = (float) tpf;
    }

    public void activate(Celestial target) {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getActivationTimer() {
        return activationTimer;
    }

    public void setActivationTimer(double activationTimer) {
        this.activationTimer = activationTimer;
    }

    public double getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(double coolDown) {
        this.coolDown = coolDown;
    }

    public void mount(Ship host, Hardpoint socket) {
        this.host = host;
        this.setSocket(socket);
    }

    @Override
    public String toString() {
        String ret;
        if (enabled) {
            //return cooldown status
            double percentCooled = activationTimer / coolDown;
            int percent = (int) (100.0 * percentCooled);
            if (percent > 100) {
                percent = 100;
            }
            ret = "(" + percent + "%) " + getName();
        } else {
            ret = (getName()+ " [OFFLINE]");
        }
        if(quantity != 1) {
            ret += " ["+quantity+"]";
        }
        return ret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public Hardpoint getSocket() {
        return socket;
    }

    public void setSocket(Hardpoint socket) {
        this.socket = socket;
    }
    
    public void construct(AssetManager assets) {
        //TODO
    }
    
    public void deconstruct() {
        killSound();
    }
    
    public void killSound() {
        //TODO
    }
}
