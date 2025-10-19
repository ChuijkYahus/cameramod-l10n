#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/* ===================== KNOBS ===================== */
// Make them uniforms in your host if you want live tweaks.
uniform float TriadsPerPixel;// 1.0 = ~one triad per texture pixel; 2.0 = denser; 0.5 = larger triads
uniform float Smear;// Beam/spot width in TRIAD units; >1 = more smear, <1 = crisper
uniform float EnableEnergyNormalize;// 1.0 = keep brightness consistent across settings

// Triad-kernel radius in MASK (triad) space: ±K triad cells around the current one.
// Small is enough because decay() is applied in mask units.
const int TriadKernelRadius = 1;// 1 => 3x3 triad neighborhood; try 0 for fastest/sharpest

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

/* ===================== Profiles / Helpers ===================== */

// Phosphor/beam falloff in TRIAD (mask) space.
float decay(float d) {
    // Same flavor as before; adjust constants to taste.
    return mix(exp2(-d*d*2.5 - 0.3), 0.05 / (d*d*d*0.45 + 0.055), 0.65) * 0.99;
}

// Distance metric in TRIAD (mask) space — smear acts here (independent of texture grid).
float sqd(vec2 a, vec2 b) {
    a -= b;
    // Anisotropy preserved; Smear scales the spot size ONLY in triad units.
    a *= vec2(1.25, 1.8) * 0.905 * Smear;
    float d = max(abs(a.x), abs(a.y));// Chebyshev-ish core
    d = mix(d, length(a * vec2(1.05, 1.0)) * 0.85, 0.3);// blend in Euclidean
    return d;
}

/* ===================== Phosphor pass =====================

Spaces:
- Texture UV space:    texCoord0 in [0,1].
- Texture pixel space: pPix = texCoord0 * texSize    (units = texels). Used ONCE to sample color.
- Mask / triad space:  pMask = pPix * TriadsPerPixel (units = triads). All beam/smear math here.

We do NOT sample neighboring texels. We convolve in TRIAD space instead.
*/
vec3 phosphors(vec2 pPix, sampler2D tex, vec2 texSize) {
    // Sample the source texture ONCE (center of the texel to stabilize)
    vec2 basePix   = floor(pPix) + 0.5;
    vec2 baseUV    = clamp(basePix / texSize, vec2(0.0), vec2(1.0));
    vec3 baseColor = texture(tex, baseUV).rgb;
    baseColor = pow(baseColor, vec3(1.18)) * 1.08;// same gentle tweak

    // Current position in TRIAD (mask) space
    vec2 pMask = pPix * TriadsPerPixel - 0.25;

    // Scanline/beam jitter locked to MASK space (stable relative to the texture because pMask derives from pPix)
    vec2 jitteredPMask = pMask;
    jitteredPMask.y += (mod(pMask.x, 2.0) < 1.0) ?  0.03 : -0.03;
    jitteredPMask.y += (mod(pMask.x, 4.0) < 2.0) ?  0.02 : -0.02;

    // Accumulate purely in TRIAD space
    vec3 col = vec3(0.0);
    float wR = 0.0, wG = 0.0, wB = 0.0;

    // Iterate neighboring TRIAD CELLS (NOT texture pixels!)
    // This simulates beam spread across adjacent phosphor triads without pulling different texel colors.
    for (int j = -TriadKernelRadius; j <= TriadKernelRadius; ++j) {
        for (int i = -TriadKernelRadius; i <= TriadKernelRadius; ++i) {
            // Center of this triad cell in MASK space
            vec2 triadCenter = floor(pMask) + 0.5 + vec2(i, j);

            // RGB subpixel centers within the triad cell
            const float xoff = 0.25;
            float rd = sqd(triadCenter, jitteredPMask + vec2(0.0, 0.2));
            float gd = sqd(triadCenter, jitteredPMask + vec2(xoff, 0.0));
            float bd = sqd(triadCenter, jitteredPMask + vec2(-xoff, 0.0));

            float wr = decay(rd);
            float wg = decay(gd);
            float wb = decay(bd);

            // Since the source color is the same for all triads (no texel blending),
            // we just weight that same color by the triad-space beam distribution.
            col.r += baseColor.r * wr;  wR += wr;
            col.g += baseColor.g * wg;  wG += wg;
            col.b += baseColor.b * wb;  wB += wb;
        }
    }

    // Normalize energy so smear/density don't change average brightness
    if (EnableEnergyNormalize > 0.5) {
        if (wR > 1e-6) col.r /= wR;
        if (wG > 1e-6) col.g /= wG;
        if (wB > 1e-6) col.b /= wB;
    }

    return clamp(col, 0.0, 1.0);
}

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));// (width, height)
    vec2 pPix    = texCoord0 * texSize;// texture pixel space (texels)
    vec3 crtRGB  = phosphors(pPix, Sampler0, texSize);

    vec4 color = vec4(crtRGB, 1.0) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
