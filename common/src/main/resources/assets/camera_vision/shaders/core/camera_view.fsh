
#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec2 TexelSize;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

// =============================================================
//                 FXAA CONFIGURATION CONSTANTS
// =============================================================

// Edge detection sensitivity: lower = more aggressive smoothing
const float FXAA_EDGE_THRESHOLD = 0.035;

// Blend factor between original and smoothed color (0 → none, 1 → full blur)
const float FXAA_BLEND_FACTOR = 0.50;

// Spread of sampling neighborhood (1.0 = 1 pixel; increase slightly to widen)
const float FXAA_SPREAD = 0.75;

// Weight of diagonal samples (0 disables diagonals, 1 uses them equally)
const float FXAA_DIAGONAL_WEIGHT = 0.75;

// =============================================================
//                     FXAA SAMPLER
// =============================================================
vec3 fxaaSample(sampler2D tex, vec2 uv, vec2 invSz) {
    // Center and 4-neighbor luma (perceptual grayscale)
    vec3 cM = texture(tex, uv).rgb;
    float lM = dot(cM, vec3(0.299, 0.587, 0.114));
    float lN = dot(texture(tex, uv + vec2(0.0, -invSz.y * FXAA_SPREAD)).rgb, vec3(0.299, 0.587, 0.114));
    float lS = dot(texture(tex, uv + vec2(0.0,  invSz.y * FXAA_SPREAD)).rgb, vec3(0.299, 0.587, 0.114));
    float lW = dot(texture(tex, uv + vec2(-invSz.x * FXAA_SPREAD, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float lE = dot(texture(tex, uv + vec2( invSz.x * FXAA_SPREAD, 0.0)).rgb, vec3(0.299, 0.587, 0.114));

    float lMin = min(lM, min(min(lN, lS), min(lW, lE)));
    float lMax = max(lM, max(max(lN, lS), max(lW, lE)));
    float contrast = lMax - lMin;

    // Early exit: no strong edge → keep original
    if (contrast < FXAA_EDGE_THRESHOLD) return cM;

    // Diagonal taps
    vec3 cNW = texture(tex, uv + vec2(-invSz.x, -invSz.y) * FXAA_SPREAD).rgb;
    vec3 cNE = texture(tex, uv + vec2( invSz.x, -invSz.y) * FXAA_SPREAD).rgb;
    vec3 cSW = texture(tex, uv + vec2(-invSz.x,  invSz.y) * FXAA_SPREAD).rgb;
    vec3 cSE = texture(tex, uv + vec2( invSz.x,  invSz.y) * FXAA_SPREAD).rgb;

    vec3 avg4 = (cNW + cNE + cSW + cSE) * (0.25 * FXAA_DIAGONAL_WEIGHT);
    vec3 avg5 = (avg4 + cM) / (1.0 + FXAA_DIAGONAL_WEIGHT);

    // Blend between original and averaged
    return mix(cM, avg5, FXAA_BLEND_FACTOR);
}




// =============================================================
//                 OKLab Posterization Settings
// =============================================================

// Posterization mode: 0 = L,a,b;  1 = LCh (lightness/chroma/hue)
const int POSTERIZE_MODE = 0;

// Quantization steps
const float L_LEVELS = 10.0;    // more = smoother gradients
const float A_LEVELS = 10.0;
const float B_LEVELS = 10.0;

// For LCh mode
const float C_LEVELS = 10.0;
const float H_LEVELS = 16.0;

// Clip chroma range to prevent RGB out-of-gamut conversion
const float MAX_CHROMA = 0.4;

// =============================================================
//                   Utility: sRGB <-> Linear
// =============================================================
vec3 srgb_to_linear(vec3 c) {
    return pow(c, vec3(2.2));
}
vec3 linear_to_srgb(vec3 c) {
    return pow(max(c, 0.0), vec3(1.0 / 2.2));
}

// =============================================================
//                  RGB (linear) <-> OKLab
// =============================================================
float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

vec3 linear_rgb_to_oklab(vec3 c) {
    float l = 0.4122214708*c.r + 0.5363325363*c.g + 0.0514459929*c.b;
    float m = 0.2119034982*c.r + 0.6806995451*c.g + 0.1073969566*c.b;
    float s = 0.0883024619*c.r + 0.2817188376*c.g + 0.6299787005*c.b;

    l = cbrt(max(l, 0.0));
    m = cbrt(max(m, 0.0));
    s = cbrt(max(s, 0.0));

    return vec3(
    0.2104542553*l + 0.7936177850*m - 0.0040720468*s,
    1.9779984951*l - 2.4285922050*m + 0.4505937099*s,
    0.0259040371*l + 0.7827717662*m - 0.8086757660*s
    );
}

vec3 oklab_to_linear_rgb(vec3 lab) {
    float L = lab.x, a = lab.y, b = lab.z;
    float l = pow(L + 0.3963377774*a + 0.2158037573*b, 3.0);
    float m = pow(L - 0.1055613458*a - 0.0638541728*b, 3.0);
    float s = pow(L - 0.0894841775*a - 1.2914855480*b, 3.0);

    return vec3(
    4.0767416621*l - 3.3077115913*m + 0.2309699292*s,
    -1.2684380046*l + 2.6097574011*m - 0.3413193965*s,
    -0.0041960863*l - 0.7034186147*m + 1.7076147010*s
    );
}

// =============================================================
//               Posterization Helpers (Fixed OKLab)
// =============================================================
float quantize(float v, float levels) {
    return floor(v * levels) / levels;
}

float quantizeCentered(float v, float range, float levels) {
    // Map [-range, range] -> [0, 1] → quantize → back
    float norm = (v + range) / (2.0 * range);
    norm = clamp(norm, 0.0, 1.0);
    norm = floor(norm * levels) / levels;
    return norm * (2.0 * range) - range;
}

// =============================================================
//              Posterize in OKLab or OKLCh
// =============================================================
vec3 posterize_oklab(vec3 srgb) {
    vec3 lin = srgb_to_linear(srgb);
    vec3 lab = linear_rgb_to_oklab(lin);

    if (POSTERIZE_MODE == 0) {
        lab.x = quantize(clamp(lab.x, 0.0, 1.0), L_LEVELS);
        lab.y = quantizeCentered(lab.y, 0.5, A_LEVELS);
        lab.z = quantizeCentered(lab.z, 0.5, B_LEVELS);
    } else {
        float L = quantize(clamp(lab.x, 0.0, 1.0), L_LEVELS);
        float C = min(length(lab.yz), MAX_CHROMA);
        float H = atan(lab.z, lab.y);

        float Cq = quantize(C / MAX_CHROMA, C_LEVELS) * MAX_CHROMA;
        float Hq = floor((H + 3.14159265) / (2.0 * 3.14159265) * H_LEVELS) / H_LEVELS;
        Hq = Hq * (2.0 * 3.14159265) - 3.14159265;

        lab = vec3(L, Cq * cos(Hq), Cq * sin(Hq));
    }

    vec3 linOut = oklab_to_linear_rgb(lab);
    return linear_to_srgb(clamp(linOut, 0.0, 1.0));
}


void main() {
    vec3 sampled = fxaaSample(Sampler0, texCoord0, TexelSize);
    vec3 posterized = posterize_oklab(sampled.rgb);

    vec4 color = vec4(posterized, 1) * vertexColor * ColorModulator;
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
