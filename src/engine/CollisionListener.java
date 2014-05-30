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
 * Listens for and handles collisions between spatials. Contains all custom
 * behaviors for the environment that are applied when a collision occurs.
 */
package engine;

import celestial.Planet;
import celestial.Projectile;
import celestial.Ship.Ship;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import entity.Entity;
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
                    handlePlanetCollision((Ship) objB.getParent());
                }
            } else if (objB.getParent() instanceof Planet) {
                if (objA.getParent() instanceof Ship) {
                    handlePlanetCollision((Ship) objA.getParent());
                }
            } else if (objA.getParent() instanceof Projectile) {
                if (objB.getParent() instanceof Ship) {
                    handleProjectileCollision((Ship) objB.getParent(), (Projectile) objA.getParent());
                }
            } else if (objB.getParent() instanceof Projectile) {
                if (objA.getParent() instanceof Ship) {
                    handleProjectileCollision((Ship) objA.getParent(), (Projectile) objB.getParent());
                }
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
        //just use the worst possible thing
        float damageA = (float) (25 * b.getMass() * impulse);
        float damageB = (float) (25 * a.getMass() * impulse);
        float worstCase = Math.max(damageA, damageB);
        //apply damage
        a.applyDamage(worstCase);
        b.applyDamage(worstCase);
    }

    private void handlePlanetCollision(Ship a) {
        //I am pretty sure this will kill you
        a.applyDamage(Float.MAX_VALUE);
    }
}
