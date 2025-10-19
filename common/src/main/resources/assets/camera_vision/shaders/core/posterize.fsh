
#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform float FxaaEdge;
uniform float FxaaBlend;
uniform float FxaaSpread;
uniform float FxaaDiagonal;

uniform vec2 TexelSize;
uniform float PostMode;
uniform vec3 PostLevels;


const float MAX_CHROMA = 0.4;

in vec2 texCoord0;

out vec4 fragColor;

// =============================================================
//                      FXAA SAMPLER FUNCTION
// =============================================================

float luma(vec3 c) {
    // Perceptual luma, works fine for sRGB inputs
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 fxaaSample(sampler2D tex, vec2 uv, vec2 invSz) {

    // --- Fetch center pixel ---
    vec3  cM = texture(tex, uv).rgb;
    float lM = luma(cM);

    // --- Fetch 4-neighbor luma values ---
    float lN = luma(texture(tex, uv + vec2(0.0, -invSz.y)).rgb);
    float lS = luma(texture(tex, uv + vec2(0.0,  invSz.y)).rgb);
    float lW = luma(texture(tex, uv + vec2(-invSz.x, 0.0)).rgb);
    float lE = luma(texture(tex, uv + vec2( invSz.x, 0.0)).rgb);

    // --- Compute local contrast ---
    float lMin = min(lM, min(min(lN, lS), min(lW, lE)));
    float lMax = max(lM, max(max(lN, lS), max(lW, lE)));
    float contrast = lMax - lMin;

    // --- Adaptive threshold (relative + absolute floor) ---
    float threshold = max(0.0312, FxaaEdge * lMax);
    if (contrast < threshold)
    return cM; // No strong edge → keep original

    // --- Estimate edge direction ---
    float gx = lW - lE;  // horizontal gradient
    float gy = lN - lS;  // vertical gradient

    // Add a small diagonal bias (helps detect stair-step edges)
    vec2 diagBias = vec2(sign(gx), sign(gy)) * FxaaDiagonal * 0.05;
    vec2 dir = normalize(vec2(gx, gy) + diagBias + 1e-6);

    // --- Adjust step length based on gradient magnitude ---
    float dirReduce = max((lW + lE + lN + lS) * 0.125, 1e-4);
    float invMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);

    // Step direction scaled by texture size
    vec2 stepUV = clamp(dir * invMin, -2.0, 2.0) * invSz * FxaaSpread;

    // --- Sample two points along the edge direction ---
    vec3 cA = texture(tex, uv + stepUV * (1.0/3.0 - 0.5)).rgb;
    vec3 cB = texture(tex, uv + stepUV * (2.0/3.0 - 0.5)).rgb;
    vec3 cEdge = 0.5 * (cA + cB);

    // --- Compute subpixel aliasing amount (how uneven the luma is) ---
    float subpix = clamp(
    ((lN + lS + lW + lE) * 0.25 - lMin) / (contrast + 1e-5),
    0.0, 1.0
    );

    // --- Blend amount: stronger on high aliasing areas ---
    float blend = FxaaBlend * subpix;

    // --- Final mix ---
    return mix(cM, cEdge, blend);
}




// =============================================================
//                 OKLab Posterization Settings
// =============================================================

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

    if (PostMode == 0) {
        lab.x = quantize(clamp(lab.x, 0.0, 1.0), PostLevels.x);
        lab.y = quantizeCentered(lab.y, 0.5, PostLevels.y);
        lab.z = quantizeCentered(lab.z, 0.5, PostLevels.z);
    } else {
        float L = quantize(clamp(lab.x, 0.0, 1.0), PostLevels.x);
        float C = min(length(lab.yz), MAX_CHROMA);
        float H = atan(lab.z, lab.y);

        float Cq = quantize(C / MAX_CHROMA, PostLevels.y) * MAX_CHROMA;
        float Hq = floor((H + 3.14159265) / (2.0 * 3.14159265) * PostLevels.z) / PostLevels.z;
        Hq = Hq * (2.0 * 3.14159265) - 3.14159265;

        lab = vec3(L, Cq * cos(Hq), Cq * sin(Hq));
    }

    vec3 linOut = oklab_to_linear_rgb(lab);
    return linear_to_srgb(clamp(linOut, 0.0, 1.0));
}


void main() {
    vec3 sampled = fxaaSample(Sampler0, texCoord0, vec2(1.0/60, 1.0/60.0));
    //vec3 sampled = texture(Sampler0, texCoord0).rgb;
    sampled = posterize_oklab(sampled);

    fragColor = vec4(sampled, 1);
}
