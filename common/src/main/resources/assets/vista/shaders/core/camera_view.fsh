#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/* SpriteDimensions = (minU, minV, sizeU, sizeV) in normalized UVs */
uniform vec4 SpriteDimensions;

/* ===================== KNOBS ===================== */
// 1.0 ≈ one triad per texel; >1 = denser triads; <1 = larger triads
uniform float TriadsPerPixel;
// Beam/spot width in TRIAD units; >1 = softer/smeared, <1 = sharper
uniform float Smear;
// 1.0 keeps average brightness stable across settings
uniform float EnableEnergyNormalize;

// Vignette strength: 0.0 = off, 1.0 = full
uniform float VignetteIntensity;

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
vec2 triadToUV(vec2 triadP, vec2 atlasSizePx) {
    vec2 pix = triadP / max(TriadsPerPixel, 1e-6);
    return pix / atlasSizePx;
}

/* ---- Precise, no-bleed clamp: clamp to edge texel centers ----
   We clamp in texel units relative to the sprite, to [0.5 .. width-0.5]. */
vec2 clampToSpriteTexelCenters(vec2 uv, vec2 atlasSizePx) {
    vec2 minUV = SpriteDimensions.xy;
    vec2 sizeUV = SpriteDimensions.zw;
    vec2 spritePx = sizeUV * atlasSizePx;

    // Convert to texel coords relative to sprite:
    vec2 p = (uv - minUV) * atlasSizePx;

    // Safe range is from center of first texel to center of last:
    vec2 lo = vec2(0.5);
    vec2 hi = max(spritePx - vec2(0.5), lo); // avoid inversion for tiny sprites

    p = clamp(p, lo, hi);
    return minUV + p / atlasSizePx;
}

/* ===================== CRT Vignette (pure, no randomness) ===================== */
// UV must be in [0,1] within the SPRITE (not the whole atlas).
float crtVignette() {
    // ===================== Apply CRT Vignette =====================
    // Compute sprite-local UV (0..1 across the sprite rect)
    vec2 minUV  = SpriteDimensions.xy;
    vec2 sizeUV = max(SpriteDimensions.zw, vec2(1e-6));
    vec2 uvLocal = clamp((texCoord0 - minUV) / sizeUV, 0.0, 1.0);

    float v = 44.0 * (uvLocal.x * (1.0 - uvLocal.x) * uvLocal.y * (1.0 - uvLocal.y));
    // Base/gain chosen to match the referenced style: 0.6 + 0.4 * v
    return 0.6 + 0.4 * v;
}

/* ===================== Phosphor pass (gather) ===================== */
vec3 accumulateTriadResponse(vec2 pixelPos, sampler2D srcTexture, vec2 atlasSizePx) {
    vec2 triadPos = pixelPos * TriadsPerPixel - 0.25;

    // -------- Derivative-based aliasing detection (cycles per pixel) --------
    // Estimate how many triad cycles a single screen pixel spans.
    vec2 dx = dFdx(triadPos);
    vec2 dy = dFdy(triadPos);
    float cpp = max(length(dx), length(dy));   // cycles-per-pixel in triad units
    // Nyquist ~ 0.5 cpp. Build a soft contrast scale in [0..1].
    float t = clamp(0.5 / max(cpp, 1e-5), 0.0, 1.0);
    float contrast = t * t; // smoother rolloff

    // Small scanline/beam jitter in TRIAD space (stable vs texture)
    vec2 jittered = triadPos;
    float amp = 0.03 * clamp(1.0 / max(TriadsPerPixel, 1e-6), 0.25, 1.0);
    float offset = 0.0;
    offset += (mod(floor(triadPos.x), 3.0) < 1.5 ? 1.0 : -1.0) * amp;
    offset += (mod(floor(triadPos.x), 5.0) < 2.5 ? 1.0 : -1.0) * (amp * 0.6);
    offset += (mod(floor(triadPos.x), 7.0) < 3.5 ? 1.0 : -1.0) * (amp * 0.4);
    jittered.y += offset;

    vec3 sum = vec3(0.0);
    float wr = 0.0, wg = 0.0, wb = 0.0;

    int dynRadius = max(TriadKernelRadius, int(ceil(2.5 * Smear)));

    // Subpixel layout (stripe-like)
    const float lr = 0.25;
    const float up = 0.2;

    for (int dy_i = -dynRadius; dy_i <= dynRadius; ++dy_i) {
        for (int dx_i = -dynRadius; dx_i <= dynRadius; ++dx_i) {
            vec2 cellCenter = floor(triadPos) + 0.5 + vec2(dx_i, dy_i);

            vec2 rP = jittered + vec2(0.0, up);
            vec2 gP = jittered + vec2( lr, 0.0);
            vec2 bP = jittered + vec2(-lr, 0.0);

            float dR = triadDistance(cellCenter, rP);
            float dG = triadDistance(cellCenter, gP);
            float dB = triadDistance(cellCenter, bP);

            float wR = beamFalloff(dR);
            float wG = beamFalloff(dG);
            float wB = beamFalloff(dB);

            // Beam center → atlas UV, then clamp to sprite edge texel centers
            vec2 uvR = clampToSpriteTexelCenters(triadToUV(rP, atlasSizePx), atlasSizePx);
            vec2 uvG = clampToSpriteTexelCenters(triadToUV(gP, atlasSizePx), atlasSizePx);
            vec2 uvB = clampToSpriteTexelCenters(triadToUV(bP, atlasSizePx), atlasSizePx);

            vec3 sR = texture(srcTexture, uvR).rgb;
            vec3 sG = texture(srcTexture, uvG).rgb;
            vec3 sB = texture(srcTexture, uvB).rgb;

            // Optional gentle tonal tweak
            sR = pow(sR, vec3(1.18)) * 1.08;
            sG = pow(sG, vec3(1.18)) * 1.08;
            sB = pow(sB, vec3(1.18)) * 1.08;

            sum.r += sR.r * wR;  wr += wR;
            sum.g += sG.g * wG;  wg += wG;
            sum.b += sB.b * wB;  wb += wB;
        }
    }

    if (EnableEnergyNormalize > 0.5) {
        sum.r /= max(wr, 1e-6);
        sum.g /= max(wg, 1e-6);
        sum.b /= max(wb, 1e-6);
    }

    vec3 triadOut = clamp(sum, 0.0, 1.0);

    // Low-frequency fallback (no triad) using the sprite-local UV
    vec3 fallback = texture(srcTexture, clampToSpriteTexelCenters(texCoord0, atlasSizePx)).rgb;

    // Blend based on aliasing risk: high cpp -> contrast→0 -> prefer fallback
    return mix(fallback, triadOut, contrast);
}


// Returns [0..1] contrast scale for the triad mask based on pixel footprint.
// 1.0 = keep full mask; 0.0 = kill mask (too high frequency).
float triadContrastScale(vec2 triadPos) {
    // How fast triad coordinates change across a screen pixel:
    vec2 dx = dFdx(triadPos);
    vec2 dy = dFdy(triadPos);
    // Approx cycles-per-pixel (CPP) ~ length of gradient (triad units per pixel)
    float cpp = max(length(dx), length(dy));

    // Nyquist is ~0.5 cycles/pixel. Add softness to avoid hard popping.
    const float nyquist = 0.5;
    // Start rolling off a bit below Nyquist to be safe:
    float t = clamp(nyquist / max(cpp, 1e-5), 0.0, 1.0);

    // Ease the rolloff for smoother transition
    return t * t; // or smoothstep(0.0, 1.0, t)
}

void main() {
    vec2 atlasSizePx = vec2(textureSize(Sampler0, 0)); // atlas size in texels
    vec2 pixelPos    = texCoord0 * atlasSizePx;

    vec3 triadRGB = accumulateTriadResponse(pixelPos, Sampler0, atlasSizePx);

    vec4 color = vec4(triadRGB, 1.0) * vertexColor * ColorModulator;



    // Pure vignette factor and blend with intensity
    float vFactor = crtVignette();
    float vignette = mix(1.0, vFactor, clamp(VignetteIntensity, 0.0, 1.0));

    color.rgb *= vignette;
    // =============================================================

    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
