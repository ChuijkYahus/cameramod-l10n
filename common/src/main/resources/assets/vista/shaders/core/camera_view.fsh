#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/* SpriteDimensions = (minU, minV, sizeU, sizeV) */
uniform vec4 SpriteDimensions;

/* ===================== KNOBS ===================== */
// 1.0 ≈ one triad per texel; >1 = denser triads; <1 = larger triads
uniform float TriadsPerPixel;
// Beam/spot width in TRIAD units; >1 = softer/smeared, <1 = sharper
uniform float Smear;
// 1.0 keeps average brightness stable across settings
uniform float EnableEnergyNormalize;

// Base kernel radius used if the dynamic one computes smaller.
// (Dynamic radius grows with Smear; this is a floor.)
const int TriadKernelRadius = 1; // 0 => fastest/sharpest (no neighbors)

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

/* ===================== Helpers ===================== */

float beamFalloff(float d) {
    float expLike   = exp2(-d * d * 2.5 - 0.3);
    float softTail  = 0.05 / (d * d * d * 0.45 + 0.055);
    return mix(expLike, softTail, 0.65) * 0.99;
}

float triadDistance(vec2 triadA, vec2 triadB) {
    vec2 delta = triadA - triadB;
    delta *= vec2(1.25, 1.8) * 0.905 * Smear;
    float chebyshevCore = max(abs(delta.x), abs(delta.y));
    float euclidBlend   = length(delta * vec2(1.05, 1.0)) * 0.85;
    return mix(chebyshevCore, euclidBlend, 0.3);
}

/* TRIAD space -> UV (no global clamp here; we clamp to sprite rect later) */
vec2 triadToUV(vec2 triadP, vec2 textureSizePx) {
    vec2 pix = triadP / max(TriadsPerPixel, 1e-6);
    return pix / textureSizePx;
}

/* Kernel footprint (in texels) to compute padding for clamping */
float kernelFootprintPx(int dynRadius) {
    return (float(dynRadius) + 1.0) / max(TriadsPerPixel, 1e-6);
}

/* Clamp atlas UV to sprite rect with padding */
vec2 clampToSprite(vec2 uv, float footprintPx, vec2 atlasPx) {
    vec2 minUV = SpriteDimensions.xy;
    vec2 sizeUV = SpriteDimensions.zw;
    vec2 maxUV = minUV + sizeUV;

    // +0.5 half-texel safety so we never touch gutters
    vec2 padUV = (vec2(footprintPx) + 0.5) / atlasPx;
    return clamp(uv, minUV + padUV, maxUV - padUV);
}

/* ===================== Phosphor pass (true gather) ===================== */
vec3 accumulateTriadResponse(vec2 pixelPos, sampler2D srcTexture, vec2 textureSizePx) {
    vec2 triadPos = pixelPos * TriadsPerPixel - 0.25;

    // Small scanline/beam jitter in TRIAD space (stable vs texture)
    vec2 jitteredTriadPos = triadPos;
    float amp = 0.03 * clamp(1.0 / max(TriadsPerPixel, 1e-6), 0.25, 1.0);
    float offset = 0.0;
    offset += (mod(floor(triadPos.x), 3.0) < 1.5 ? 1.0 : -1.0) * amp;
    offset += (mod(floor(triadPos.x), 5.0) < 2.5 ? 1.0 : -1.0) * (amp * 0.6);
    offset += (mod(floor(triadPos.x), 7.0) < 3.5 ? 1.0 : -1.0) * (amp * 0.4);
    jitteredTriadPos.y += offset;

    vec3 accumulatedRGB = vec3(0.0);
    float totalWeightR = 0.0;
    float totalWeightG = 0.0;
    float totalWeightB = 0.0;

    int dynRadius = max(TriadKernelRadius, int(ceil(2.5 * Smear)));
    float fpPx = kernelFootprintPx(dynRadius);
    vec2 atlasPx = textureSizePx;

    // Subpixel layout (stripe-like)
    const float lrPixelOffset = 0.25;
    const float upPixelOffset = 0.2;

    for (int dy = -dynRadius; dy <= dynRadius; ++dy) {
        for (int dx = -dynRadius; dx <= dynRadius; ++dx) {
            vec2 cellCenter = floor(triadPos) + 0.5 + vec2(dx, dy);

            vec2 rTriadP = jitteredTriadPos + vec2(0.0,  upPixelOffset);
            vec2 gTriadP = jitteredTriadPos + vec2( lrPixelOffset, 0.0);
            vec2 bTriadP = jitteredTriadPos + vec2(-lrPixelOffset, 0.0);

            float distR = triadDistance(cellCenter, rTriadP);
            float distG = triadDistance(cellCenter, gTriadP);
            float distB = triadDistance(cellCenter, bTriadP);

            float wR = beamFalloff(distR);
            float wG = beamFalloff(distG);
            float wB = beamFalloff(distB);

            // Beam center → atlas UV, then clamp to sprite rect (no mipmaps)
            vec2 uvR = clampToSprite(triadToUV(rTriadP, textureSizePx), fpPx, atlasPx);
            vec2 uvG = clampToSprite(triadToUV(gTriadP, textureSizePx), fpPx, atlasPx);
            vec2 uvB = clampToSprite(triadToUV(bTriadP, textureSizePx), fpPx, atlasPx);

            vec3 sampleR = texture(srcTexture, uvR).rgb;
            vec3 sampleG = texture(srcTexture, uvG).rgb;
            vec3 sampleB = texture(srcTexture, uvB).rgb;

            // Optional gentle tonal tweak
            sampleR = pow(sampleR, vec3(1.18)) * 1.08;
            sampleG = pow(sampleG, vec3(1.18)) * 1.08;
            sampleB = pow(sampleB, vec3(1.18)) * 1.08;

            accumulatedRGB.r += sampleR.r * wR;  totalWeightR += wR;
            accumulatedRGB.g += sampleG.g * wG;  totalWeightG += wG;
            accumulatedRGB.b += sampleB.b * wB;  totalWeightB += wB;
        }
    }

    if (EnableEnergyNormalize > 0.5) {
        accumulatedRGB.r /= max(totalWeightR, 1e-6);
        accumulatedRGB.g /= max(totalWeightG, 1e-6);
        accumulatedRGB.b /= max(totalWeightB, 1e-6);
    }

    return clamp(accumulatedRGB, 0.0, 1.0);
}

void main() {
    vec2 textureSizePx = vec2(textureSize(Sampler0, 0)); // atlas size in texels
    vec2 pixelPos = texCoord0 * textureSizePx;

    vec3 triadRGB = accumulateTriadResponse(pixelPos, Sampler0, textureSizePx);

    vec4 color = vec4(triadRGB, 1.0) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
