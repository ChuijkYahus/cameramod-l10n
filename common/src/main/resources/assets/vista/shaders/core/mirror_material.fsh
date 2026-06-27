#version 150

#moj_import <fog.glsl>

// Sampler0 = dynamic reflection texture (off-axis frustum render of the world)
// Sampler1 = mirror underlay base material (block/mirror/underlay) — shows through where the
//           silvering has worn or scratched.
// Sampler3 = mirror overlay (block/mirror/overlay) — alpha-blended decal stamped one full
//           copy per block on top of everything (Sampler2 is reserved for the lightmap).
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler3;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// (W, H) block count of the connected mirror grid. Used to scale procedural noise so its
// frequency stays world-consistent across mirror sizes, and to tile the base material one
// copy per block (matches what the previous overlay-blit pass did).
uniform vec2 Tiles;

// Silvering fade-in factor in [0,1]. 0 = no reflection (just the base material), 1 =
// full reflection. Driven by MirrorReflectionTexture#getFadeProgress so the surface
// fades in over a few hundred ms after the first valid frame.
uniform float Fade;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

// ---------- PARAMETERS ----------
const float DISTORTION   = 0.17;
const float EDGE_WEAR    = 0.2;
const float SCRATCH      = 0.02;
const float ROUGHNESS    = 0.5;
const float REFLECTIVITY = 0.9;

// ---------- HASH / NOISE ----------
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

// ---------- EDGE MASK ----------
float edgeMask(vec2 uv) {
    vec2 d = abs(uv - 0.5) * 2.0;
    float e = max(d.x, d.y);
    return smoothstep(1.0 - EDGE_WEAR, 1.0, e);
}

// ---------- SCRATCHES ----------
float scratchField(vec2 uv) {
    float n = fbm(uv * 200.0);
    float lines = sin(uv.y * 312.0 + n * 3.0);
    return smoothstep(0.52, 0.5, abs(lines));
}

void main() {
    vec2 uv = texCoord0;

    // World-space noise coord: keep frequency per-block instead of per-quad so a 1x1 and a
    // 4x4 mirror have the same scratch density.
    vec2 p = uv * Tiles;

    // ---------- DISTORTION ----------
    vec2 warp;
    warp.x = fbm(p * 3.0);
    warp.y = fbm(p * 3.0 + 17.3);

    vec2 duv = uv + (warp - 0.5) * DISTORTION * 0.05;

    // ---------- REFLECTION BLUR ----------
    vec3 refl = vec3(0.0);
    float total = 0.0;

    for (int i = 0; i < 5; i++) {
        float a = float(i) / 5.0 * 6.2831853;
        vec2 offset = vec2(cos(a), sin(a)) * ROUGHNESS * 0.002;

        refl += texture(Sampler0, duv + offset).rgb;
        total += 1.0;
    }
    refl /= total;

    // ---------- BASE ----------
    // Tile the base material one full copy per block — same pattern as the old overlay blit.
    vec3 base = (texture(Sampler1, fract(uv * Tiles)) * vertexColor).rgb;

    // ---------- EFFECTS ----------
    float edge = edgeMask(uv);
    float scratch = scratchField(p) * SCRATCH;

    float reflectivity = REFLECTIVITY;
    reflectivity *= (1.0 - edge * EDGE_WEAR);
    reflectivity *= (1.0 - scratch * 0.6);
    // Fade the silvering in from 0 → full. Multiplying here means the wear/scratch
    // masking still attenuates the partial reflection correctly during the ramp.
    reflectivity *= Fade;

    // ---------- COMPOSITION ----------
    vec3 color = mix(base, refl, reflectivity);
    color = mix(color, base, edge * EDGE_WEAR);

    vec4 outColor = vec4(color, 1.0) * lightMapColor * ColorModulator;

    // ---------- OVERLAY ----------
    // One full overlay copy per block, alpha-composited on top — matches the old per-tile blit.
    vec4 ovl = texture(Sampler3, fract(uv * Tiles));
    outColor.rgb = mix(outColor.rgb, ovl.rgb, ovl.a);

    fragColor = linear_fog(outColor, vertexDistance, FogStart, FogEnd, FogColor);
}