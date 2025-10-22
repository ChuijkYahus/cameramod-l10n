#version 150

#moj_import <fog.glsl>
#moj_import <vista:crt_vignette.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;


uniform float GameTime;

uniform float NoiseSpeed;
uniform float NoiseScale;
uniform float NoiseIntensity;

// Vignette strength: 0.0 = off, 1.0 = full
uniform float VignetteIntensity;
/* SpriteDimensions = (minU, minV, sizeU, sizeV) in normalized UVs */
uniform vec4 SpriteDimensions;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

const float PHI = 1.61803398874989484820459; // Φ = Golden Ratio

float gold_noise(in vec2 xy, in float seed){
    return fract(tan(distance(xy * PHI, xy) * seed) * xy.x);
}

void main() {
    float seed = fract(GameTime * NoiseSpeed);
    vec2  uv   = texCoord0 * NoiseScale;

    // Procedural noise color
    vec4 noise = vec4(
        gold_noise(uv, seed + 0.1),
        gold_noise(uv, seed + 0.2),
        gold_noise(uv, seed + 0.3),
        gold_noise(uv, seed + 0.4)
    );

    // Base texture
    vec4 tex = texture(Sampler0, texCoord0);

    // Mix texture ↔ noise
    float k = clamp(NoiseIntensity, 0.0, 1.0);
    vec3 base = mix(tex.rgb, noise.rgb, k);

    // Apply material/lighting tints
    vec4 tint = vertexColor * ColorModulator * lightMapColor;
    vec4 shaded = vec4(base, tex.a) * tint;

    // Fog is usually last scene-space op
    vec4 fogged = linear_fog(shaded, vertexDistance, FogStart, FogEnd, FogColor);

    // Vignette is a post effect: last
    float vignette = mix(1.0, crtVignette(SpriteDimensions, texCoord0), clamp(VignetteIntensity, 0.0, 1.0));
    fogged.rgb *= vignette;

    fragColor = fogged;
}


