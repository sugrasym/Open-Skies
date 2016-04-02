/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engine;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author Geoff Hibbert
 */
public class TargetPlacement {
    public final Vector3f location;
    public final Quaternion rotation;

    public TargetPlacement(Vector3f location, Quaternion rotation) {
        this.location = location;
        this.rotation = rotation;
    }
    
}
