/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gdi;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 *
 * @author Geoff
 */
public class Utility {
    public static BufferedImage CreateCompatibleImage(int width, int height, int TYPE_INT_ARGB) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        GraphicsConfiguration config = device.getDefaultConfiguration();
        return config.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }
    
}
