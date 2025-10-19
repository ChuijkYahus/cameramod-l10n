#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/* ===================== KNOBS ===================== */
// Make them uniforms in your host if you want live tweaks.
uniform float TriadsPerPixel;// 1.0 ≈ one triad per texel; >1 = denser triads; <1 = larger triads
uniform float Smear;// Beam/spot width in TRIAD units; >1 = softer/smeared, <1 = sharper
uniform float EnableEnergyNormalize;// 1.0 keeps average brightness stable across settings

// Triad-kernel radius in MASK (triad) space: ±K triad cells around the current one.
// Small is enough because beamFalloff() is applied in mask units.
const int TriadKernelRadius = 1;// 1 => 3x3 triad neighborhood; 0 => fastest/sharpest (no neighbors)

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

/* ===================== Profiles / Helpers ===================== */

/**
 * Phosphor/beam intensity falloff in TRIAD (mask) space.
 * Input d is a distance in triad units (after anisotropic scaling + smear).
 * Shape blends an exponential-like spot with a soft 1/r^3 tail.
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

/* ===================== Phosphor pass =====================

Spaces:
- Texture UV space:    texCoord0 in [0,1].
- Texture pixel space: pixelPos = texCoord0 * textureSizePx   (units = texels). Used ONCE to sample color.
- Mask / triad space:  triadPos = pixelPos * TriadsPerPixel   (units = triads). All beam/smear math here.

Key idea:
We do NOT sample neighboring texels. We sample the source once (stabilized at texel center),
then spread that same color across nearby triad cells using a beam kernel in TRIAD space.
This simulates CRT-like subpixel triads/beam spread without cross-texel blending.
*/
vec3 accumulateTriadResponse(vec2 pixelPos, sampler2D srcTexture, vec2 textureSizePx) {
    // --- 1) Sample the source texture ONCE at the center of the covering texel (stabilizes against jitter) ---
    vec2 centerPix  = floor(pixelPos) + 0.5;
    vec2 sampleUV   = clamp(centerPix / textureSizePx, vec2(0.0), vec2(1.0));
    vec3 srcColor   = texture(srcTexture, sampleUV).rgb;

    // Gentle tonal tweak to taste (kept from original)
    srcColor = pow(srcColor, vec3(1.18)) * 1.08;

    // --- 2) Convert to TRIAD (mask) space: where the phosphor/beam math happens ---
    vec2 triadPos = pixelPos * TriadsPerPixel - 0.25;

    // --- 3) Add small scanline/beam jitter in TRIAD space (stable vs texture because derived from pixelPos) ---
    vec2 jitteredTriadPos = triadPos;
    // amplitude scales inversely with triad density
    float amp = 0.03 * clamp(1.0 / TriadsPerPixel, 0.25, 1.0);
    // use primes to get long repeat periods (~15 triads here)
    float offset = 0.0;
    offset += (mod(floor(triadPos.x), 3.0) < 1.5 ? 1.0 : -1.0) * amp;
    offset += (mod(floor(triadPos.x), 5.0) < 2.5 ? 1.0 : -1.0) * (amp * 0.6);
    offset += (mod(floor(triadPos.x), 7.0) < 3.5 ? 1.0 : -1.0) * (amp * 0.4);

    jitteredTriadPos.y += offset;


    // --- 4) Accumulate contributions from neighboring TRIAD CELLS (NOT texture pixels) ---
    vec3 accumulatedRGB = vec3(0.0);
    float totalWeightR = 0.0;
    float totalWeightG = 0.0;
    float totalWeightB = 0.0;

    // Neighborhood in triad units
    for (int dy = -TriadKernelRadius; dy <= TriadKernelRadius; ++dy) {
        for (int dx = -TriadKernelRadius; dx <= TriadKernelRadius; ++dx) {
            // Center of this triad cell in mask space
            vec2 cellCenter = floor(triadPos) + 0.5 + vec2(dx, dy);

            // RGB subpixel centers within the triad cell (stripe-like layout)
            const float lrPixelOffset = 0.25;
            const float upPixelOffset   = 0.2;

            // Distances from beam center (jitteredTriadPos) to each subpixel center
            float distR = triadDistance(cellCenter, jitteredTriadPos + vec2(0.0, upPixelOffset));
            float distG = triadDistance(cellCenter, jitteredTriadPos + vec2(lrPixelOffset, 0.0));
            float distB = triadDistance(cellCenter, jitteredTriadPos + vec2(-lrPixelOffset, 0.0));

            // Beam falloff weights per subpixel
            float wR = beamFalloff(distR);
            float wG = beamFalloff(distG);
            float wB = beamFalloff(distB);

            // Since source color is identical for all triads (no cross-texel sampling),
            // just weight that same color by the triad-space beam distribution.
            accumulatedRGB.r += srcColor.r * wR;  totalWeightR += wR;
            accumulatedRGB.g += srcColor.g * wG;  totalWeightG += wG;
            accumulatedRGB.b += srcColor.b * wB;  totalWeightB += wB;
        }
    }

    // --- 5) Optional energy normalization so smear/density changes don't alter average brightness ---
    if (EnableEnergyNormalize > 0.5) {
        float avgW = (totalWeightR + totalWeightG + totalWeightB) / 3.0;
        accumulatedRGB /= avgW;
    }

    return clamp(accumulatedRGB, 0.0, 1.0);
}

void main() {
    // Texture size in texels (width, height)
    vec2 textureSizePx = vec2(textureSize(Sampler0, 0));

    // Current fragment position in texture-pixel (texel) space
    vec2 pixelPos = texCoord0 * textureSizePx;

    // Simulate CRT-like phosphor triads/beam spread in triad space
    vec3 triadRGB = accumulateTriadResponse(pixelPos, Sampler0, textureSizePx);

    // Standard Minecraft pipeline bits (coloring, lights, fog)
    vec4 color = vec4(triadRGB, 1.0) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
