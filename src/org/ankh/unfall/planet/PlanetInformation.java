/*    
This file is part of jME Planet Demo.

jME Planet Demo is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation.

jME Planet Demo is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU General Public License
along with jME Planet Demo.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ankh.unfall.planet;

import com.jme3.math.ColorRGBA;

/**
 * List of params used to generate planet
 * @author Yacine Petitprez
 */
public class PlanetInformation {

    /** Equator temperatures */
    private int equatorTemperature;
    /** Poles temperatures */
    private int poleTemperature;
    /** Height of water ([0..255] */
    private int waterLevel;
    /** Percentage of water on planet */
    private float waterInPercent;
    /** Height factor */
    private float heightFactor;
    /** Random seed for this planet */
    private int seed;
    /** Planet radius (in km) */
    private float radius;
    /** day/night cycle time (in sec, Earth  ~= 88000sec) */
    private int daytime;
    /** XXX Humidity factor is NOT USED ACTUALLY */
    private float humidity;
    /** This planet has cloud? */
    private boolean hasCloud = true;
    /** Atmosphere density */
    private float atmosphereDensity = 1.f;
    /** Atmosphere color */
    private ColorRGBA atmosphereColor = new ColorRGBA(.7f, .8f, 1f, 1f);
    /** Atmosphere absorption power */
    private float atmosphereAbsorptionPower = 2f;
    /** Atmosphere glow power */
    private float atmosphereGlowPower = 20f;
    /** Set the cloud height int percentage of the radius size */
    private float cloudHeight = 0.004f;
    /** Smoothness */
    private int smoothness = 7;

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public int getDaytime() {
        return daytime;
    }

    public void setDaytime(int daytime) {
        this.daytime = daytime;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getPoleTemperature() {
        return poleTemperature;
    }

    public void setPoleTemperature(int pole_temp) {
        this.poleTemperature = pole_temp;
    }

    public int getEquatorTemperature() {
        return equatorTemperature;
    }

    public void setEquatorTemperature(int equator_temp) {
        this.equatorTemperature = equator_temp;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterInPercent(float percent) {
        this.waterInPercent = percent;
    }

    public float getWaterInPercent() {
        return waterInPercent;
    }

    /* Niveau en % */
    public void setWaterLevel(int water_level) {
        this.waterLevel = water_level;
    }

    public float getHeightFactor() {
        return heightFactor;
    }

    public void setHeightFactor(float height_factor) {
        this.heightFactor = height_factor;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public boolean hasCloud() {
        return hasCloud;
    }

    public void setHasCloud(boolean hasCloud) {
        this.hasCloud = hasCloud;
    }

    public float getAtmosphereDensity() {
        return atmosphereDensity;
    }

    public void setAtmosphereDensity(float atmosphereDensity) {
        this.atmosphereDensity = atmosphereDensity;
    }

    public ColorRGBA getAtmosphereColor() {
        return atmosphereColor;
    }

    public void setAtmosphereColor(ColorRGBA atmosphereColor) {
        this.atmosphereColor = atmosphereColor;
    }

    public float getAtmosphereAbsorptionPower() {
        return atmosphereAbsorptionPower;
    }

    public void setAtmosphereAbsorptionPower(float atmosphereAbsorptionPower) {
        this.atmosphereAbsorptionPower = atmosphereAbsorptionPower;
    }

    public float getAtmosphereGlowPower() {
        return atmosphereGlowPower;
    }

    public void setAtmosphereGlowPower(float atmosphereGlowPower) {
        this.atmosphereGlowPower = atmosphereGlowPower;
    }

    public float getCloudHeight() {
        return cloudHeight;
    }

    /**
     * Set the cloud height factor. To compute real cloud height, you must make this:<br>
     * <code>realCloudHeight = (1.f+cloudHeight) * radius</code><br>
     * The end of atmosphere is compute like this:<br>
     * <code>endAtmosphere = (1.f+(10*cloudHeight)) * radius</code>
     * @param cloudHeight
     */
    public void setCloudHeight(float cloudHeight) {
        this.cloudHeight = cloudHeight;
    }

    /**
     * Compute the radius of atmosphere to the <code>cloudHeight</code> factor 
     * @param atmoSize The size of atmosphere, in 3D unity. For example, if your Planet has a radius of 7000 u3D, 
     * and you call {@link #setAtmosphereSizeInUnity(500)}, the atmosphere pass will have a size of 7500 u3D.
     */
    public void setAtmosphereSizeInUnity(float atmoSize) {
        cloudHeight = 0.1f * ((getRadius() / (atmoSize + getRadius())) - 1.f);
    }

    public int getSmoothness() {
        return smoothness;
    }

    public void setSmoothness(int smoothness) {
        this.smoothness = smoothness;
    }
}
