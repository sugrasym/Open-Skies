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
uniform vec4 fvAtmoColor;
uniform vec4 fvDiffuse;

uniform float fAtmoDensity;
uniform float fAbsPower;
uniform float fGlowPower;

varying vec3 fvNormal;
varying vec3 fvViewDirection;
varying vec3 fvLightDirection;

/* Returns the biggest value in the vector */
vec3 maxVal(vec3 rgb) {
   return rgb/max(rgb.x, max(rgb.y, rgb.z));
}

void main(void)
{
    vec3 n = normalize(fvNormal);
    vec3 v = normalize(fvViewDirection);
    vec3 l = normalize(fvLightDirection);
    
    // dot product: (n.l) . (n.v)
    float NdotL = dot(n, l);
    float NdotV = dot(n, v);
    
    // Damping for the current pixel
    float abs_power = max(0.0, pow( -dot(v, l), 1.0/fAbsPower));
    
    float glow =  1.0 - pow(cos(abs(NdotV) * 1.57079), fGlowPower);  
    
    vec4 color = fvDiffuse * fvAtmoColor;
    vec4 invColor = 1.0 - color;
    invColor.w = color.w;
    
    vec4 diffuseColor = mix(color, invColor, abs_power);
    
    // Compute the atmosphere transparency    
    float falloff;
    // Adding 0.35 to NdotL allows to light the atmosphere during a sun eclipse!
	// Stophe: I guess he meant when the sun is behind the planet.
    falloff = max(0.0,(1.0 - NdotV) *  min(1.0, NdotL+0.35) * glow );
    falloff = pow(falloff, 1.0/fAtmoDensity);

    gl_FragColor.xyz = maxVal(diffuseColor.xyz * falloff);
    gl_FragColor.w = diffuseColor.w * falloff;
    //gl_FragColor = vec4(glow);    
}