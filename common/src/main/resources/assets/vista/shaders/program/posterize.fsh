#version 150

uniform sampler2D DiffuseSampler;

uniform float PostMode;      // 0 = OKLab, 1 = OKLCh
uniform vec3 PostLevels;
uniform float DitherScale;
uniform float DitherStrength;

const float MAX_CHROMA = 0.4;

in vec2 texCoord0;
in vec2 oneTexel;

out vec4 fragColor;


vec3 srgb_to_linear(vec3 c) { return pow(c, vec3(2.2)); }
vec3 linear_to_srgb(vec3 c) { return pow(max(c, 0.0), vec3(1.0 / 2.2)); }
float cbrt(float x) { return sign(x) * pow(abs(x), 1.0 / 3.0); }

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


ivec2 pxFromUV(vec2 uv, vec2 texelSize) {
    return ivec2(floor(uv / texelSize));
}

float bayer_scaled(vec2 uv, vec2 texelSize) {
    ivec2 p = ivec2(floor(uv / texelSize));

    if (DitherScale <= 1) {
        // 2x2
        const int M2[4] = int[4](0, 2, 3, 1);
        int idx = ((p.y & 1) << 1) | (p.x & 1);
        return (float(M2[idx]) + 0.5) / 4.0;
    } else if (DitherScale <= 3) {
        // 4x4
        const int M4[16] = int[16](
        0,  8,  2, 10,
        12,  4, 14,  6,
        3, 11,  1,  9,
        15,  7, 13,  5
        );
        int idx = (p.y & 3) * 4 + (p.x & 3);
        return (float(M4[idx]) + 0.5) / 16.0;
    } else {
        // 8x8
        const int M8[64] = int[64](
        0, 32,  8, 40,  2, 34, 10, 42,
        48, 16, 56, 24, 50, 18, 58, 26,
        12, 44,  4, 36, 14, 46,  6, 38,
        60, 28, 52, 20, 62, 30, 54, 22,
        3, 35, 11, 43,  1, 33,  9, 41,
        51, 19, 59, 27, 49, 17, 57, 25,
        15, 47,  7, 39, 13, 45,  5, 37,
        63, 31, 55, 23, 61, 29, 53, 21
        );
        int idx = (p.y & 7) * 8 + (p.x & 7);
        return (float(M8[idx]) + 0.5) / 64.0;
    }
}

float quantize_dither(float v, float levels, float dither01) {
    float jitter = (dither01 - 0.5) * DitherStrength / max(levels, 1.0);
    return floor((v + jitter) * levels + 0.5) / max(levels, 1.0);
}

float quantizeCentered_dither(float v, float range, float levels, float dither01) {
    float norm = clamp((v + range) / (2.0 * range), 0.0, 1.0);
    float jitter = (dither01 - 0.5) * DitherStrength / max(levels, 1.0);
    norm = floor((norm + jitter) * levels + 0.5) / max(levels, 1.0);
    return norm * (2.0 * range) - range;
}

vec3 posterize_oklab(vec3 srgb, vec2 texelSize) {
    ivec2 px = pxFromUV(texCoord0, texelSize);
    float dA = bayer_scaled(texCoord0, texelSize);
    float dB = bayer_scaled(texCoord0 + vec2(1.7, 3.1) * texelSize, texelSize);
    float dC = bayer_scaled(texCoord0 + vec2(2.3, 1.9) * texelSize, texelSize);

    vec3 lin = srgb_to_linear(srgb);
    vec3 lab = linear_rgb_to_oklab(lin);

    if (PostMode == 0.0) {
        // OKLab
        lab.x = quantize_dither( clamp(lab.x, 0.0, 1.0), PostLevels.x, dA);
        lab.y = quantizeCentered_dither( lab.y, 0.5,      PostLevels.y, dB);
        lab.z = quantizeCentered_dither( lab.z, 0.5,      PostLevels.z, dC);
    } else {
        // OKLCh
        float L = quantize_dither( clamp(lab.x, 0.0, 1.0), PostLevels.x, dA);

        float C = length(lab.yz);
        float H = (C > 1e-6) ? atan(lab.z, lab.y) : 0.0;

        float Cn = clamp(C / MAX_CHROMA, 0.0, 1.0);
        float Cq = quantize_dither(Cn, PostLevels.y, dB) * MAX_CHROMA;

        float Hn = (H + 3.14159265) * (0.5 / 3.14159265);
        Hn = fract(Hn); // ensure [0,1)
        float Hq = quantize_dither(Hn, PostLevels.z, dC);
        Hq = Hq * (2.0 * 3.14159265) - 3.14159265;

        lab = vec3(L, Cq * cos(Hq), Cq * sin(Hq));
    }

    vec3 linOut = oklab_to_linear_rgb(lab);
    return linear_to_srgb(clamp(linOut, 0.0, 1.0));
}

void main() {
    vec3 sampled = texture(DiffuseSampler, texCoord0).rgb;
    sampled = posterize_oklab(sampled, oneTexel);

    fragColor = vec4(sampled, 1.0);
}
