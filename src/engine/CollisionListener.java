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
 * Listens for and handles collisions between spatials. Contains all custom
 * behaviors for the environment that are applied when a collision occurs.
 */
package engine;

import cargo.Item;
import celestial.Field;
import celestial.Jumphole;
import celestial.Planet;
import celestial.Projectile;
import celestial.Ship.CargoContainer;
import celestial.Ship.Ship;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import entity.Entity;
import entity.Entity.State;
import entity.PhysicsEntity.PhysicsNameControl;

/**
 *
 * @author nwiehoff
 */
public class CollisionListener implements PhysicsCollisionListener {

    @Override
    public void collision(PhysicsCollisionEvent event) {
        //get the objects responsible
        PhysicsNameControl objA = event.getNodeA().getControl(PhysicsNameControl.class);
        PhysicsNameControl objB = event.getNodeB().getControl(PhysicsNameControl.class);
        //get the impulse applied
        float impulse = event.getAppliedImpulse();
        //make sure this is valid
        if (objA != null && objB != null) {
            //branch based on type of collision
            if (objA.getParent() instanceof Ship && objB.getParent() instanceof Ship) {
                handleShipCollision((Ship) objA.getParent(), (Ship) objB.getParent(), impulse);
            } else if (objA.getParent() instanceof Planet) {
                if (objB.getParent() instanceof Ship) {
                    if (!(objA.getParent() instanceof Jumphole)) {
                        handlePlanetCollision((Ship) objB.getParent());
                    } else {
                        //jumpholes do their own tests
                    }
                } else if (objB.getParent() instanceof Projectile) {
                    Projectile pro = (Projectile) objB.getParent();
                    pro.setState(State.DYING);
                }
            } else if (objB.getParent() instanceof Planet) {
                if (objA.getParent() instanceof Ship) {
                    if (!(objB.getParent() instanceof Jumphole)) {
                        handlePlanetCollision((Ship) objA.getParent());
                    } else {
                        //jumpholes do their own tests
                    }
                } else if (objA.getParent() instanceof Projectile) {
                    Projectile pro = (Projectile) objA.getParent();
                    pro.setState(State.DYING);
                }
            } else if (objA.getParent() instanceof Projectile) {
                if (objB.getParent() instanceof Ship) {
                    handleProjectileCollision((Ship) objB.getParent(), (Projectile) objA.getParent());
                } else if(objB.getParent() instanceof Field) {
                    handleProjectileFieldCollision((Projectile) objA.getParent(), (Field) objB.getParent());
                }
            } else if (objB.getParent() instanceof Projectile) {
                if (objA.getParent() instanceof Ship) {
                    handleProjectileCollision((Ship) objA.getParent(), (Projectile) objB.getParent());
                } else if(objA.getParent() instanceof Field) {
                    handleProjectileFieldCollision((Projectile) objB.getParent(), (Field) objA.getParent());
                }
            }
        }
    }

    private void handleProjectileFieldCollision(Projectile pro, Field field) {
        if(pro.getState() == State.ALIVE) {
            pro.setState(State.DYING);
            if(field.isMineable()) {
                //chip off a piece of the asteroid
                Item i = new Item(field.getResource());
                i.setQuantity(1);
                CargoContainer can = new CargoContainer(
                        field.getCurrentSystem().getUniverse(), i);

                can.setLocation(pro.getLocation());
                can.setVelocity(pro.getVelocity());

                field.getCurrentSystem().putEntityInSystem(can);
            }
        }
    }
    
    private void handleProjectileCollision(Ship a, Projectile pro) {
        if (a != pro.getHost()) {
            //get damage
            float shieldDamage = pro.getShieldDamage();
            float hullDamage = pro.getHullDamage();
            //apply damage
            a.applyDamage(shieldDamage, hullDamage);
            a.setLastBlow(pro.getHost());
            //remove projectile
            pro.setState(Entity.State.DYING);
        }
    }

    private void handleShipCollision(Ship a, Ship b, float impulse) {
        //this is the case where the first ship is a cargo container
        if (a instanceof CargoContainer) {
            b.addAllToCargoBay(a.getCargoBay());
            a.removeAllFromCargoBay();
            a.setState(State.DYING);
        } //this is the case where the second ship is a cargo container
        if (b instanceof CargoContainer) {
            a.addAllToCargoBay(b.getCargoBay());
            b.removeAllFromCargoBay();
            b.setState(State.DYING);
        } else {
            //just use the worst possible thing
            float damageA = (float) (25 * b.getMass() * impulse);
            float damageB = (float) (25 * a.getMass() * impulse);
            float worstCase = Math.max(damageA, damageB);
            //apply damage
            a.applyDamage(worstCase);
            b.applyDamage(worstCase);
        }
    }

    private void handlePlanetCollision(Ship a) {
        try {
            a.applyDamage((float) (25 * a.getMass() * a.getLinearVelocity().length()));
        } catch (Exception e) {
            System.out.println("Error handling planet - ship collission");
        }
    }
}
