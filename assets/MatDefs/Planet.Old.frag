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
varying vec3 vNormal;

varying vec3 vLightDirectionInWorldSpace;
varying vec3 vViewDirectionInWorldSpace;

varying vec3 vLightDirectionInTangentSpace;
varying vec3 vViewDirectionInTangentSpace;

varying vec2 vTexCoords;

uniform sampler2D baseMap;
uniform sampler2D specMap;
uniform sampler2D normalMap;
uniform sampler2D cloudsMap;

uniform float fSpecularPower;
uniform float fCloudHeight;
uniform float fCloudRotation; // The clouds move slowly

uniform vec4 fvDiffuse; // Diffuse sun color
uniform vec4 fvSpecular; // Specular sun color

void main(void)
{
   // Retrieve the normal from the normal map
   vec3 normal           = normalize( ( texture2D( normalMap, vTexCoords ).xyz * 2.0 ) - 1.0 );
   
   // Retrieve the normal without considering the relief to add some clouds
   vec3 simpleNormal     = normalize(vNormal);
   
   vec3 lightDirection   = normalize(vLightDirectionInTangentSpace);
   vec3 viewDirection    = normalize(vViewDirectionInTangentSpace);
   
   vec4 diffuseColor = texture2D(baseMap, vTexCoords); 
   
   // Angle between light and the normal
   float NdotL = dot(normal, lightDirection);
   
   // Same with the simple normal; but since this one is expressd in absolute space, we use those vectors.
   vec3 lightDirectionWorld = normalize(vLightDirectionInWorldSpace);
   vec3 viewDirectionWorld = normalize(vViewDirectionInWorldSpace);
   
   float NdotLCloud = dot(simpleNormal, lightDirectionWorld);
   
   vec3 RCloud = reflect(-lightDirectionWorld, simpleNormal);
   // Clouds don't reflect much; divide max specular value by 5.
   float SpecularCloud = pow(max(0.0, dot(viewDirectionWorld, RCloud)), fSpecularPower) * 0.80;
   
   // Retrieve the reflected ray to compute the specular value
   vec3 R = reflect(-lightDirection, normal);
   float NdotR = max(0.0, dot(viewDirection, R));
   
   // Compute specular factor
   float specularFactor = pow(NdotR, fSpecularPower);
   
   vec4 specularColor = texture2D(specMap, vTexCoords);
   
   vec4 specular = specularFactor * specularColor;
   
   vec4 diffuseFinal = diffuseColor * NdotL * fvDiffuse;
   
   vec2 cloudTexCoord = vTexCoords;
   cloudTexCoord.x += fCloudRotation;
   
   vec4 cloudColor = texture2D(cloudsMap, cloudTexCoord);
   
   // Compute clouds shadows: Equal to the inverse of the cloud's opacity  Egale à l'inverse de l'opacité du nuage + displacement according to the light.
   float zFactor = min(0.01, abs(fCloudHeight/lightDirection.z));
   vec2 displacement = -zFactor * lightDirection.yx;
   
   float shadowModule = 1.0 - texture2D(cloudsMap, cloudTexCoord + displacement).w;
   
   // The color right before adding the clouds
   vec4 beforeCloud = diffuseFinal + specular * fvSpecular;
   
   // Multiply the ground color with the module of the cloud's shadow
   beforeCloud *= shadowModule;
   
   vec4 finalCloudColor;
   finalCloudColor.xyz = cloudColor.xyz * fvDiffuse.xyz * 
   												NdotLCloud + fvSpecular.xyz*SpecularCloud;
   finalCloudColor.w = cloudColor.w;
   // Interpolate according to the alpha channel of the cloud layer
   //gl_FragColor.xyz = -viewDirection.xyz;
   
   //gl_FragColor = vec4(NdotLCloud);
   //gl_FragColor = beforeCloud;
   gl_FragColor = mix(beforeCloud, finalCloudColor, finalCloudColor.w);
   //vec3 light = normalize(vLightDirectionInWorldSpace);
   //gl_FragColor = vec4(light.x, light.y, light.z, 1.0);
}