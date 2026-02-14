#version 150
#moj_import <fog.glsl>
#moj_import <vista:crt_effects.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform float GameTime;

uniform float NoiseIntensity;
uniform float FadeAnimation;// 1 = off, 0 = on
uniform int OverlayIndex;

uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;


/* SpriteDimensions = (sizeU, sizeV) in normalized UVs. First 2 are unused */
uniform vec2 SpriteDimensions;

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
const int TriadKernelRadius = 1;// 0 => fastest/sharpest (no neighbors)

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
flat in vec2 atlasSizePx;
flat in vec2 spriteSizePx;

in vec2 texCoord0;
in vec2 texCoord1;

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

vec2 normalizedTriadPerPixel() {
    //it works i guess
    vec2 invFrame = 96.0 / spriteSizePx;
    //remove 1 multiplication to have triads per pixels. 2 to be triads per uv
    return TriadsPerPixel * invFrame;
}

/* TRIAD space -> UV (no global clamp here; we clamp to sprite rect later) */
vec2 triadToUV(vec2 triadP, vec2 frameOriginUV) {
    vec2 tpp = normalizedTriadPerPixel();

    // convert triad position to frame-local pixels
    vec2 pix = triadP / max(tpp, 1e-6);

    // frame-local UV (0..1 inside frame)
    vec2 localUV = pix / spriteSizePx;

    // return absolute UV inside the texture
    return frameOriginUV + localUV * SpriteDimensions;
}


/* ---- Precise, no-bleed clamp: clamp to edge texel centers ----
   We clamp in texel units relative to the current frame, to [0.5 .. width-0.5]. */
vec2 clampToSpriteTexelCenters(vec2 uv, vec2 frameOriginUV) {
    // frame size in UVs

    // Convert to texel coords relative to frame
    vec2 p = (uv - frameOriginUV) * atlasSizePx;

    // Safe range: center of first texel → center of last texel
    vec2 lo = vec2(0.5);
    vec2 hi = max(spriteSizePx - vec2(0.5), lo);

    p = clamp(p, lo, hi);
    return frameOriginUV + p / atlasSizePx;
}

vec3 sampleImage(vec2 uv, vec2 frameOriginUV)
{
    float colorAdd = 0.0;
    if (OverlayIndex == 1) {
        vec3 uvAndCol = vcr_pause(uv, frameOriginUV, SpriteDimensions, atlasSizePx, GameTime, normalizedTriadPerPixel());
        uv = uvAndCol.xy;
        colorAdd = uvAndCol.z;
    }

    vec3 color = texture(Sampler0, uv).rgb;

    if (OverlayIndex > 0){
        vec2 overlayUV = (uv - frameOriginUV) / SpriteDimensions;
        vec4 overlay = texture(Sampler1, overlayUV);
        color += overlay.rgb * overlay.a;
    }

    color += colorAdd;

    return color;
}

/* ===================== Phosphor pass (gather) ===================== */
vec3 accumulateTriadResponse(vec2 pixelPos, vec2 localUV, vec2 frameOriginUV) {

    vec2 tpp = normalizedTriadPerPixel();

    vec2 triadPos = pixelPos * tpp - 0.25;

    // -------- Derivative-based aliasing detection (cycles per pixel) --------
    // Estimate how many triad cycles a single screen pixel spans.
    vec2 dx = dFdx(triadPos);
    vec2 dy = dFdy(triadPos);
    float cpp = max(length(dx), length(dy));// cycles-per-pixel in triad units
    // Nyquist ~ 0.5 cpp. Build a soft contrast scale in [0..1].
    float t = clamp(0.5 / max(cpp, 1e-5), 0.0, 1.0);
    float contrast = t * t;// smoother rolloff

    // Small scanline/beam jitter in TRIAD space (stable vs texture)
    vec2 jittered = triadPos;
    float amp = 0.03 * clamp(1.0 / max(tpp.x, 1e-6), 0.25, 1.0);//tpp.x is incorrect
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
            vec2 gP = jittered + vec2(lr, 0.0);
            vec2 bP = jittered + vec2(-lr, 0.0);

            float dR = triadDistance(cellCenter, rP);
            float dG = triadDistance(cellCenter, gP);
            float dB = triadDistance(cellCenter, bP);

            float wR = beamFalloff(dR);
            float wG = beamFalloff(dG);
            float wB = beamFalloff(dB);

            // Beam center → atlas UV, then clamp to sprite edge texel centers
            vec2 uvR = clampToSpriteTexelCenters(triadToUV(rP, frameOriginUV), frameOriginUV);
            vec2 uvG = clampToSpriteTexelCenters(triadToUV(gP, frameOriginUV), frameOriginUV);
            vec2 uvB = clampToSpriteTexelCenters(triadToUV(bP, frameOriginUV), frameOriginUV);

            vec3 sR = sampleImage(uvR, frameOriginUV);
            vec3 sG = sampleImage(uvG, frameOriginUV);
            vec3 sB = sampleImage(uvB, frameOriginUV);

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
    // compute frame-local UVs for fallback
    vec2 fallbackUV = frameOriginUV + localUV * SpriteDimensions;

    vec3 fallback = sampleImage(clampToSpriteTexelCenters(fallbackUV, frameOriginUV), frameOriginUV);


    // Blend based on aliasing risk: high cpp -> contrast→0 -> prefer fallback
    return mix(fallback, triadOut, contrast);
}


void main() {
    // infer frame origin from texCoord0
    vec2 frameOriginUV = floor(texCoord0 / SpriteDimensions) * SpriteDimensions;;
    // frame-local UVs
    vec2 localUV = (texCoord0 - frameOriginUV) / SpriteDimensions;
    // frame-local pixel position for triad math
    vec2 pixelPos = localUV * spriteSizePx;

    vec3 triadRGB = NoiseIntensity >=1 ? vec3(1, 1, 1) :  accumulateTriadResponse(pixelPos, localUV, frameOriginUV);

    vec4 color = vec4(triadRGB, 1.0) * vertexColor;

    // ===================== NOISE =====================
    // Get procedural noise
    if (NoiseIntensity >=0){
        vec4 noise = crt_noise(texCoord0, GameTime);
        color.rgb =  color.rgb * (1-NoiseIntensity) + noise.rgb * NoiseIntensity;
    }

    // ===================== AC BEAT =====================
    float acBeat = vhs_wave(localUV, frameOriginUV, SpriteDimensions, GameTime);
    color.rgb += (acBeat-0.5)*(0.03 + (NoiseIntensity*0.4));
    // =============================================================

    // ===================== VIGNETTE =====================
    // Compute vignette using frame-local UV
    float vFactor = crt_vignette(localUV);
    float vignette = mix(1.0, vFactor, clamp(VignetteIntensity, 0.0, 1.0));
    color.rgb *= vignette;
    // =============================================================



    if (FadeAnimation != 0){
        color = crt_turn_on(
        color,
        pixelPos,
        spriteSizePx,
        FadeAnimation
        );
    }

    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
