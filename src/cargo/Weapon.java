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
 * Represents a weapon (turret, cannon, battery, missile)
 */
package cargo;

import celestial.Celestial;
import celestial.Projectile;
import celestial.Ship.Ship;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import engine.Core;
import java.util.ArrayList;
import lib.astral.Parser;

/**
 *
 * @author Nathan Wiehoff
 */
public class Weapon extends Equipment {

    int width;
    int height;
    //weapon properties
    private float shieldDamage;
    private float hullDamage;
    private float speed;
    //weapon graphics
    private float size = 1;
    private ColorRGBA startColor = ColorRGBA.Red;
    private ColorRGBA endColor = ColorRGBA.Blue;
    private Vector3f pVel = Vector3f.UNIT_XYZ;
    private float highLife = 1;
    private float lowLife = 0.1f;
    private int numParticles = 10;
    private float variation = 0.75f;
    private String texture = "Effects/Trail/point.png";
    private float emitterRate = 10;
    private boolean guided = false;
    private float thrust;
    private float turning;
    private float shotMass;
    private float delay;
    private Item ammo;
    private float maxLife;
    private float proximityFuse;
    private String soundPath;
    private transient AudioNode sound;

    public Weapon(String name) {
        super(name);
        init();
    }

    private void init() {
        //get weapon stuff now
        Parser parse = new Parser("WEAPONS.txt");
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Weapon");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.equals(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            setName(relevant.getValue("name"));
            setType(relevant.getValue("type"));
            setMass(Float.parseFloat(relevant.getValue("mass")));
            setShieldDamage(Float.parseFloat(relevant.getValue("shieldDamage")));
            setHullDamage(Float.parseFloat(relevant.getValue("hullDamage")));
            setRange(Float.parseFloat(relevant.getValue("range")));
            setSpeed(Float.parseFloat(relevant.getValue("speed")));
            setCoolDown(Float.parseFloat(relevant.getValue("refire")));
            setSize(Float.parseFloat(relevant.getValue("size")));
            {
                //start color
                String rawStart = relevant.getValue("startColor");
                String[] arr = rawStart.split(",");
                float r = Float.parseFloat(arr[0]);
                float g = Float.parseFloat(arr[1]);
                float b = Float.parseFloat(arr[2]);
                float a = Float.parseFloat(arr[3]);
                ColorRGBA col = new ColorRGBA(r, g, b, a);
                startColor = col;
            }
            {
                //end color
                String rawStart = relevant.getValue("endColor");
                String[] arr = rawStart.split(",");
                float r = Float.parseFloat(arr[0]);
                float g = Float.parseFloat(arr[1]);
                float b = Float.parseFloat(arr[2]);
                float a = Float.parseFloat(arr[3]);
                ColorRGBA col = new ColorRGBA(r, g, b, a);
                endColor = col;
            }
            {
                //pVel : The velocity of the particles in their local space
                String rawVel = relevant.getValue("pVel");
                String[] arr = rawVel.split(",");
                float x = Float.parseFloat(arr[0]);
                float y = Float.parseFloat(arr[1]);
                float z = Float.parseFloat(arr[2]);
                Vector3f v = new Vector3f(x, y, z);
                pVel = v;
            }
            setHighLife(Float.parseFloat(relevant.getValue("highLife")));
            setLowLife(Float.parseFloat(relevant.getValue("lowLife")));
            setNumParticles(Integer.parseInt(relevant.getValue("numParticles")));
            setVariation(Float.parseFloat(relevant.getValue("variation")));
            setTexture(relevant.getValue("texture"));
            setEmitterRate(Float.parseFloat(relevant.getValue("emitterRate")));

            String rawGuided = relevant.getValue("guided");
            if (rawGuided != null) {
                guided = Boolean.parseBoolean(rawGuided);
            } else {
                guided = false;
            }

            String rawThrust = relevant.getValue("thrust");
            if (rawThrust != null) {
                thrust = Float.parseFloat(rawThrust);
            } else {
                thrust = 0;
            }

            String rawTurning = relevant.getValue("turning");
            if (rawTurning != null) {
                turning = Float.parseFloat(rawTurning);
            } else {
                turning = 0;
            }

            String rawShotMass = relevant.getValue("shotMass");
            if (rawShotMass != null) {
                shotMass = Float.parseFloat(rawShotMass);
            } else {
                shotMass = 0.00000001f;
            }

            String rawDelay = relevant.getValue("delay");
            if (rawDelay != null) {
                delay = Float.parseFloat(rawDelay);
            } else {
                delay = 0;
            }

            String rawAmmo = relevant.getValue("ammo");
            if (rawAmmo != null) {
                ammo = new Item(rawAmmo);
            }

            String rawSound = relevant.getValue("sound");
            if (rawSound != null) {
                soundPath = rawSound;
            }

            String rawMaxLife = relevant.getValue("maxLife");
            if (rawMaxLife != null) {
                maxLife = Float.parseFloat(rawMaxLife);
            } else {
                maxLife = 0;
            }

            String rawFuse = relevant.getValue("proximityFuse");
            if (rawFuse != null) {
                proximityFuse = Float.parseFloat(rawFuse);
            } else {
                proximityFuse = 0;
            }
        } else {
            System.out.println("Error: The item " + getName() + " does not exist in WEAPONS.txt");
        }
    }

    @Override
    public void construct(AssetManager assets) {
        if (soundPath != null) {
            sound = new AudioNode(assets, soundPath);
        }
    }

    @Override
    public void deconstruct() {
        killSound();
    }

    @Override
    public void killSound() {
        if (sound != null) {
            sound.stop();
            sound = null;
        }
    }

    @Override
    public void activate(Celestial target) {
        if (getCoolDown() <= getActivationTimer() && enabled && hasAmmo()) {
            if (canActivate(target)) {
                setActivationTimer(0); //restart cooldown
                //determine if OOS or not
                if (host.getCurrentSystem() == host.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem()) {
                    fire(target);
                    playSound();
                } else {
                    oosFire(target);
                }
            }
        }
    }

    private boolean canActivate(Celestial target) {
        return ((!guided
                && !isTurret()
                && !isBattery()) || (target != null))
                && inFiringCone(target);
    }

    public boolean inFiringCone(Celestial target) {

        //turrets always hit oos
        if(host.getCurrentSystem() != host.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem()) {
            return true;
        }
        
        if (isCannon() || isMissile()) {
            return true;
        } else if(isTurret() || isBattery()) {
            Vector3f targetLoc = adjustedTargetLocation(target);
            Vector3f turretUp = adjustedTurretLocation();
            float t = turretUp.angleBetween(targetLoc);
            
            if( t <= getSocket().getGimbal()) {
                return true;
            }
        }

        return false;
    }

    private Vector3f adjustedTurretLocation() {
        Vector3f turretUp = getSocket().getUpNode().getWorldTranslation()
                .subtract(getSocket().getNode().getWorldTranslation())
                .normalize();
        return turretUp;
    }

    private Vector3f adjustedTargetLocation(Celestial target) {
        Vector3f targetLoc = (leadTargetLocation(target)
                .subtract(getSocket().getNode().getWorldTranslation()))
                .normalize();
        return targetLoc;
    }
    
    public boolean isCannon() {
        return getType().equals(Item.TYPE_CANNON);
    }
    
    public boolean isMissile() {
        return getType().equals(Item.TYPE_MISSILE);
    }
    
    public boolean isBattery() {
        return getType().equals(Item.TYPE_BATTERY);
    }

    public boolean isTurret() {
        return getType().equals(Item.TYPE_TURRET);
    }

    private void playSound() {
        if (sound != null) {
            if (!host.getSoundQue().contains(sound)) {
                sound.setLocalTranslation(host.getLocation().clone());
                host.getSoundQue().add(sound);
            }
        }
    }

    public boolean hasAmmo() {
        if (ammo != null) {
            if (host.getNumInCargoBay(ammo) > 0) {
                //and return true;
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    public void useAmmo() {
        if (ammo != null) {
            ArrayList<Item> cargo = host.getCargoBay();
            for (int a = 0; a < cargo.size(); a++) {
                Item tmp = cargo.get(a);
                if (tmp.getName().equals(ammo.getName())) {
                    if (tmp.getGroup().equals(ammo.getGroup())) {
                        if (tmp.getType().equals(ammo.getType())) {
                            if (tmp.getQuantity() > 1) {
                                tmp.setQuantity(tmp.getQuantity() - 1);
                            } else {
                                cargo.remove(tmp);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void fire(Celestial target) {
        /*
         * This is the one called in system. It uses the physics system and is
         * under the assumption that the host has been fully constructed. It
         * generates a projectile using precached values, assigns those values,
         * and finally drops the projectile into space.
         */
        if (enabled) {
            //generate projectile
            Projectile pro = new Projectile(host.getCurrentSystem().getUniverse(),
                    target, getName(), shotMass);
            //store stats
            pro.setShieldDamage(shieldDamage);
            pro.setHullDamage(hullDamage);
            pro.setSpeed(speed);
            pro.setRange(range);
            //store graphics
            pro.setSize(size);
            pro.setHighLife(highLife);
            pro.setLowLife(lowLife);
            pro.setNumParticles(numParticles);
            pro.setVariation(variation);
            pro.setTexture(texture);
            pro.setEmitterRate(emitterRate);
            pro.setStartColor(startColor);
            pro.setEndColor(endColor);
            pro.setpVel(pVel);

            /*
             * Missiles and cannons fire straight forward out of their hard points.
             * Turrets and batteries need to simulate a swivel.
             */
            Quaternion rot;
            Vector3f vel;

            //determine world rotation
            if (isCannon() || isMissile()) {
                rot = getSocket().getNode().getWorldRotation();
                vel = getSocket().getUp().mult(speed);
                rot.multLocal(vel);
            } else {
                Vector3f targetLoc = leadTargetLocation(target);
                rot = new Quaternion();
                rot.lookAt(targetLoc, Vector3f.UNIT_Y);

                Vector3f u = targetLoc
                        .subtract(getSocket().getNode().getWorldTranslation());
                u = u.normalize();

                vel = u.mult(speed);
            }

            vel = vel.add(host.getLinearVelocity());
            
            Vector3f loc = getSocket().getNode().getWorldTranslation()
                    .add(vel.mult(Math.min(tpf, Core.DEFAULT_TICK)));

            //store physics
            pro.setLocation(loc);
            pro.setRotation(rot);
            pro.setVelocity(vel);
            pro.setDelay(delay);
            //store host
            pro.setHost(host);
            pro.setOrigin(socket);
            //store guidance
            pro.setThrust(thrust);
            pro.setTurning(turning);
            pro.setGuided(guided);
            pro.setMaxLife(maxLife);
            pro.setProximityFuse(proximityFuse);
            //put projectile in system
            host.getCurrentSystem().putEntityInSystem(pro);
            //use ammo
            useAmmo();
        }
    }

    private Vector3f leadTargetLocation(Celestial target) {
        Vector3f targetLoc = target.getPhysicsLocation()
                .add((target.getLinearVelocity()
                        .subtract(host.getLinearVelocity()))
                        .mult((distanceTo(target.getLocation()) / speed)));
        return targetLoc;
    }

    private void oosFire(Celestial target) {
        if (enabled) {
            if (target instanceof Ship) {
                //directly damage the ship
                Ship tmp = (Ship) target;
                tmp.applyDamage(shieldDamage, hullDamage);
                tmp.setLastBlow(host);
            }
        }
    }

    public float getShieldDamage() {
        return shieldDamage;
    }

    public void setShieldDamage(float shieldDamage) {
        this.shieldDamage = shieldDamage;
    }

    public float getHullDamage() {
        return hullDamage;
    }

    public void setHullDamage(float hullDamage) {
        this.hullDamage = hullDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public ColorRGBA getStartColor() {
        return startColor;
    }

    public void setStartColor(ColorRGBA startColor) {
        this.startColor = startColor;
    }

    public ColorRGBA getEndColor() {
        return endColor;
    }

    public void setEndColor(ColorRGBA endColor) {
        this.endColor = endColor;
    }

    public Vector3f getpVel() {
        return pVel;
    }

    public void setpVel(Vector3f pVel) {
        this.pVel = pVel;
    }

    public float getHighLife() {
        return highLife;
    }

    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    public float getLowLife() {
        return lowLife;
    }

    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }

    public int getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(int numParticles) {
        this.numParticles = numParticles;
    }

    public float getVariation() {
        return variation;
    }

    public void setVariation(float variation) {
        this.variation = variation;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public float getEmitterRate() {
        return emitterRate;
    }

    public void setEmitterRate(float emitterRate) {
        this.emitterRate = emitterRate;
    }

    @Override
    public String toString() {
        String ret = "";
        if (ammo == null) {
            ret = super.toString();
        } else if (host != null) {
            ret = super.toString();
            ret += " <" + host.getNumInCargoBay(ammo) + ">";
        } else {
            ret = super.toString();
        }
        return ret;
    }
    
    public float distanceTo(Vector3f loc) {
        return loc.distance(getSocket().getNode().getWorldTranslation());
    }
}
