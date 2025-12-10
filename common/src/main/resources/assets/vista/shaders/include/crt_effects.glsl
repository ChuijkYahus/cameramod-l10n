#version 150


const float PHI = 1.61803398874989484820459;
const float NOISE_SPEED = 1000;
const float NOISE_SCALE = 10000;

/* ===================== CRT Vignette (pure, no randomness) ===================== */
// UV must be in [0,1] within the SPRITE (not the whole atlas).
float crt_vignette(in vec4 spriteDimensions,in vec2 texCoord) {
    // ===================== Apply CRT Vignette =====================
    // Compute sprite-local UV (0..1 across the sprite rect)
    vec2 minUV  = spriteDimensions.xy;
    vec2 sizeUV = max(spriteDimensions.zw, vec2(1e-6));
    vec2 uvLocal = clamp((texCoord - minUV) / sizeUV, 0.0, 1.0);

    float v = 44.0 * (uvLocal.x * (1.0 - uvLocal.x) * uvLocal.y * (1.0 - uvLocal.y));
    // Base/gain chosen to match the referenced style: 0.6 + 0.4 * v
    return 0.6 + 0.4 * v;
}

// ------------------------------------------------------------------
// Golden-ratio procedural noise
// ------------------------------------------------------------------
float gold_noise(in vec2 xy, in float seed) {
    return fract(tan(distance(xy * PHI, xy) * seed) * xy.x);
}

// ------------------------------------------------------------------
// Combined noise generation (RGB + A channel if needed)
// ------------------------------------------------------------------
vec4 crt_noise(in vec2 uv, in float timeSeed) {
    uv = uv * NOISE_SCALE;
    timeSeed = fract(timeSeed * NOISE_SPEED);
    return vec4(
    gold_noise(uv, timeSeed + 0.1),
    gold_noise(uv, timeSeed + 0.2),
    gold_noise(uv, timeSeed + 0.3),
    gold_noise(uv, timeSeed + 0.4)
    );
}


