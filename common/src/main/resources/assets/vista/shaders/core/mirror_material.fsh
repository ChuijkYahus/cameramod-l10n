#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0; // reflection
uniform sampler2D Sampler1; // base underlay
uniform sampler2D Sampler3; // overlay decal

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// connected mirror grid size in blocks
uniform vec2 Tiles;
// silvering fade-in [0,1]
uniform float Fade;


in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

uniform float Distortion;
const float EDGE_WEAR = 0.2;
const float SCRATCH = 0.02;
const float ROUGHNESS = 0.5;
const float REFLECTIVITY = 0.9;
const float SHADOW_BLEND = 0.2;
const vec3 SHADOW_COLOR = vec3(35.0, 44.0, 40.0) / 255.0;

// tiles base texture one copy per block, trimming outer edge to match framebuffer
vec2 tiledUV(vec2 uv) {
    return fract((1.0 + uv * (16.0 * Tiles - 2.0)) / 16.0);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x)
         + (c - a) * u.y * (1.0 - u.x)
         + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;

    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

float edgeMask(vec2 uv) {
    vec2 d = abs(uv - 0.5) * 2.0;
    float e = max(d.x, d.y);
    return smoothstep(1.0 - EDGE_WEAR, 1.0, e);
}

float scratchField(vec2 uv) {
    float n = fbm(uv * 200.0);
    float lines = sin(uv.y * 312.0 + n * 3.0);
    return smoothstep(0.52, 0.5, abs(lines));
}

void main() {
    vec2 uv = texCoord0;
    // per-block noise coord
    vec2 p = uv * Tiles;


    vec2 warp;
    warp.x = fbm(p * 3.0);
    warp.y = fbm(p * 3.0 + 17.3);
    vec2 duv = uv + (warp - 0.5) * Distortion * 0.05;

    vec3 refl = vec3(0.0);
    float total = 0.0;
    for (int i = 0; i < 5; i++) {
        float a = float(i) / 5.0 * 6.2831853;
        vec2 offset = vec2(cos(a), sin(a)) * ROUGHNESS * 0.002;
        refl += texture(Sampler0, duv + offset).rgb;
        total += 1.0;
    }
    refl /= total;

    vec3 base = (texture(Sampler1, tiledUV(uv)) * vertexColor).rgb;

    float edge = edgeMask(uv);
    float scratch = scratchField(p) * SCRATCH;

    float reflectivity = REFLECTIVITY * (1.0 - edge * EDGE_WEAR) * (1.0 - scratch * 0.6) * Fade;

    vec3 color = mix(base, refl, reflectivity);
    color = mix(color, base, edge * EDGE_WEAR);
    vec4 outColor = vec4(color, 1.0) * lightMapColor * ColorModulator;

    vec4 ovl = texture(Sampler3, tiledUV(uv));
    outColor.rgb = mix(outColor.rgb, ovl.rgb, ovl.a);

    // 1px drop shadow on bottom and right edges
    vec2 surfacePx = 16.0 * Tiles - 2.0;
    bool bottomEdge = floor(uv.y * surfacePx.y) > surfacePx.y - 2.0;
    bool rightEdge = floor(uv.x * surfacePx.x) > surfacePx.x - 2.0;
    if (bottomEdge || rightEdge) {
        outColor.rgb = mix(outColor.rgb, SHADOW_COLOR, SHADOW_BLEND);
    }

    fragColor = linear_fog(outColor, vertexDistance, FogStart, FogEnd, FogColor);
}