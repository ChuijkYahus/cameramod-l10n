#version 150
/* ===================== CRT Vignette (pure, no randomness) ===================== */
// UV must be in [0,1] within the SPRITE (not the whole atlas).
float crtVignette(vec4 spriteDimensions, vec2 texCoord) {
    // ===================== Apply CRT Vignette =====================
    // Compute sprite-local UV (0..1 across the sprite rect)
    vec2 minUV  = spriteDimensions.xy;
    vec2 sizeUV = max(spriteDimensions.zw, vec2(1e-6));
    vec2 uvLocal = clamp((texCoord - minUV) / sizeUV, 0.0, 1.0);

    float v = 44.0 * (uvLocal.x * (1.0 - uvLocal.x) * uvLocal.y * (1.0 - uvLocal.y));
    // Base/gain chosen to match the referenced style: 0.6 + 0.4 * v
    return 0.6 + 0.4 * v;
}




