#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

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

/* ===================== Profiles / Helpers ===================== */

/**
 * Phosphor/beam intensity falloff in TRIAD (mask) space.
 * Input d is a distance in triad units (after anisotropic scaling + smear).
 */
float beamFalloff(float d) {
    float expLike   = exp2(-d * d * 2.5 - 0.3);
    float softTail  = 0.05 / (d * d * d * 0.45 + 0.055);
    return mix(expLike, softTail, 0.65) * 0.99;
}

/**
 * Distance metric in TRIAD (mask) space (independent of texture grid).
 * - Applies anisotropy (triads are not isotropic dots)
 * - Scales by Smear (beam size control) in triad units
 * - Blends a Chebyshev-ish core with a Euclidean ring for nice spot shape
 */
float triadDistance(vec2 triadA, vec2 triadB) {
    vec2 delta = triadA - triadB;

    // Anisotropy preserved; Smear scales spot size ONLY in triad units.
    delta *= vec2(1.25, 1.8) * 0.905 * Smear;

    float chebyshevCore = max(abs(delta.x), abs(delta.y));
    float euclidBlend   = length(delta * vec2(1.05, 1.0)) * 0.85;

    // Blend: mostly Chebyshev core with a touch of Euclidean rounding
    return mix(chebyshevCore, euclidBlend, 0.3);
}

/** TRIAD space -> UV helper */
vec2 triadToUV(vec2 triadP, vec2 textureSizePx) {
    vec2 pix = triadP / max(TriadsPerPixel, 1e-6);
    return clamp(pix / textureSizePx, vec2(0.0), vec2(1.0));
}

/* ===================== Phosphor pass (true gather) =====================

Spaces:
- Texture UV space:    texCoord0 in [0,1].
- Texture pixel space: pixelPos = texCoord0 * textureSizePx   (units = texels).
- Mask / triad space:  triadPos = pixelPos * TriadsPerPixel   (units = triads).

Key change from your original:
We DO NOT reuse one texel sample. We gather from the source texture at the
R/G/B beam centers (in triad space), weighted by the beam kernel.
*/
vec3 accumulateTriadResponse(vec2 pixelPos, sampler2D srcTexture, vec2 textureSizePx) {
    // Convert to TRIAD (mask) space: where the phosphor/beam math happens
    vec2 triadPos = pixelPos * TriadsPerPixel - 0.25;

    // Small scanline/beam jitter in TRIAD space (stable vs texture)
    vec2 jitteredTriadPos = triadPos;
    float amp = 0.03 * clamp(1.0 / max(TriadsPerPixel, 1e-6), 0.25, 1.0);
    float offset = 0.0;
    offset += (mod(floor(triadPos.x), 3.0) < 1.5 ? 1.0 : -1.0) * amp;
    offset += (mod(floor(triadPos.x), 5.0) < 2.5 ? 1.0 : -1.0) * (amp * 0.6);
    offset += (mod(floor(triadPos.x), 7.0) < 3.5 ? 1.0 : -1.0) * (amp * 0.4);
    jitteredTriadPos.y += offset;

    // Accumulators
    vec3 accumulatedRGB = vec3(0.0);
    float totalWeightR = 0.0;
    float totalWeightG = 0.0;
    float totalWeightB = 0.0;

    // Dynamic neighborhood radius in triad units (grow with smear)
    int dynRadius = max(TriadKernelRadius, int(ceil(2.5 * Smear)));

    // Predefine subpixel layout (stripe-like)
    const float lrPixelOffset = 0.25;
    const float upPixelOffset = 0.2;

    for (int dy = -dynRadius; dy <= dynRadius; ++dy) {
        for (int dx = -dynRadius; dx <= dynRadius; ++dx) {
            // Center of this triad cell in mask space
            vec2 cellCenter = floor(triadPos) + 0.5 + vec2(dx, dy);

            // R/G/B beam centers (in TRIAD space)
            vec2 rTriadP = jitteredTriadPos + vec2(0.0,  upPixelOffset);
            vec2 gTriadP = jitteredTriadPos + vec2( lrPixelOffset, 0.0);
            vec2 bTriadP = jitteredTriadPos + vec2(-lrPixelOffset, 0.0);

            // Distances from beam centers to this triad cell center
            float distR = triadDistance(cellCenter, rTriadP);
            float distG = triadDistance(cellCenter, gTriadP);
            float distB = triadDistance(cellCenter, bTriadP);

            // Beam falloff weights per subpixel
            float wR = beamFalloff(distR);
            float wG = beamFalloff(distG);
            float wB = beamFalloff(distB);

            // Sample the source at the *beam centers* (true gather)
            vec3 sampleR = texture(srcTexture, triadToUV(rTriadP, textureSizePx)).rgb;
            vec3 sampleG = texture(srcTexture, triadToUV(gTriadP, textureSizePx)).rgb;
            vec3 sampleB = texture(srcTexture, triadToUV(bTriadP, textureSizePx)).rgb;

            // Optional gentle tonal tweak (comment out while debugging)
            sampleR = pow(sampleR, vec3(1.18)) * 1.08;
            sampleG = pow(sampleG, vec3(1.18)) * 1.08;
            sampleB = pow(sampleB, vec3(1.18)) * 1.08;

            // Per-channel accumulation: each channel draws from the texture *at that position*
            accumulatedRGB.r += sampleR.r * wR;  totalWeightR += wR;
            accumulatedRGB.g += sampleG.g * wG;  totalWeightG += wG;
            accumulatedRGB.b += sampleB.b * wB;  totalWeightB += wB;
        }
    }

    // Optional energy normalization so smear/density changes don't alter average brightness
    if (EnableEnergyNormalize > 0.5) {
        accumulatedRGB.r /= max(totalWeightR, 1e-6);
        accumulatedRGB.g /= max(totalWeightG, 1e-6);
        accumulatedRGB.b /= max(totalWeightB, 1e-6);
    }

    return clamp(accumulatedRGB, 0.0, 1.0);
}

void main() {
    // Texture size in texels (width, height)
    vec2 textureSizePx = vec2(textureSize(Sampler0, 0));

    // Current fragment position in texture-pixel (texel) space
    vec2 pixelPos = texCoord0 * textureSizePx;

    // Simulate CRT-like phosphor triads/beam spread in triad space (true gather)
    vec3 triadRGB = accumulateTriadResponse(pixelPos, Sampler0, textureSizePx);

    // Standard Minecraft pipeline bits (coloring, lights, fog)
    vec4 color = vec4(triadRGB, 1.0) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
