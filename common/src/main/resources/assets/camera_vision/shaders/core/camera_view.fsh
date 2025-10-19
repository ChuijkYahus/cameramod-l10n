#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
// NEW uniform
uniform float ScanDensity; // 1.0 default; >1 = denser & smaller, <1 = coarser & larger


in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

// ========================= CRT / Phosphor helpers =========================

// Phosphor decay profile (your original)
float decay(float d) {
    // mix of gaussian-ish and inverse-cubic tails, lightly damped
    return mix(exp2(-d*d*2.5 - 0.3), 0.05 / (d*d*d*0.45 + 0.055), 0.65) * 0.99;
}

// Pixel “distance” shaping (your original, slightly commented)
float sqd(vec2 a, vec2 b) {
    a -= b;
    a *= vec2(1.25, 1.8) * 0.905 * ScanDensity;  // <-- Density shrinks spot size
    float d = max(abs(a.x), abs(a.y));                            // Chebyshev-ish
    d = mix(d, length(a * vec2(1.05, 1.0)) * 0.85, 0.3);          // blend in Euclidean
    return d;
}

// Sample the phosphor triads around a pixel position in *texture pixel space*.
// texSize = (texture width, texture height)
vec3 phosphors(vec2 pPix, sampler2D tex, vec2 texSize) {
    vec3 col = vec3(0.0);

    // Align with your original offset logic
    // Triad/mask space: scale by ScanDensity to make grid denser/sparser
    vec2 p = pPix * ScanDensity - 0.25;

    // **Important change**: use texture pixel X (p.x) for scanline jitter
    // so the mask is locked to the texture (stable in-world), not screen.
    p.y += (mod(p.x, 2.0) < 1.0) ?  0.03 : -0.03;
    p.y += (mod(p.x, 4.0) < 2.0) ?  0.02 : -0.02;

    // 5x5 kernel over neighboring *texture pixels* (tap in pixel space)
    for (int i = -2; i <= 2; ++i) {
        for (int j = -2; j <= 2; ++j) {
            // Taps in mask space
            vec2 tapMask = floor(p) + 0.5 + vec2(i, j);

            // Map mask tap back to SOURCE pixel space before sampling
            vec2 tapSrc = tapMask / ScanDensity;   // undo mask scaling
            vec2 uv     = tapSrc / texSize;

            vec3 rez = texture(Sampler0, uv).rgb;

            // Triad centers in mask space (xoff relative to triad width)
            const float xoff = 0.25;
            float rd = sqd(tapMask, p + vec2( 0.0, 0.2));
            float gd = sqd(tapMask, p + vec2( xoff, 0.0));
            float bd = sqd(tapMask, p + vec2(-xoff, 0.0));

            rez = pow(rez, vec3(1.18)) * 1.08;

            rez.r *= decay(rd);
            rez.g *= decay(gd);
            rez.b *= decay(bd);

            col += rez;
        }
    }

    // mild safety clamp; triad accumulation can spike on bright sources
    return clamp(col, 0.0, 1.0);
}

void main() {
    // Convert UV to *texture pixel coordinates* for the CRT pass
    //vec2 texSize = 1.0 / TexelSize;          // (width, height)
    vec2 texSize = vec2(textureSize(Sampler0, 0));  // width, height of the atlas
    vec3 crtRGB  = phosphors(texCoord0 * texSize, Sampler0, texSize);
    //crtRGB = texture(Sampler0, texCoord0).rgb;

    // Apply Minecraft’s usual tinting & fog pipeline
    vec4 color = vec4(crtRGB, 1.0) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
