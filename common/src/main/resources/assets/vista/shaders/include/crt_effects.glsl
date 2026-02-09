// ===================== CRT Vignette (pure, no randomness) =====================
// UV must be in [0,1] within the current sprite frame. Needs to be normalized first.
float crt_vignette(in vec2 texCoord) {
    // ===================== Apply CRT Vignette =====================
    // Compute sprite-local UV (0..1 across the sprite rect)
    vec2 minUV  = vec2(0,0);
    vec2 sizeUV = vec2(1,1);
    vec2 uvLocal = clamp((texCoord - minUV) / sizeUV, 0.0, 1.0);

    float v = 44.0 * (uvLocal.x * (1.0 - uvLocal.x) * uvLocal.y * (1.0 - uvLocal.y));
    // Base/gain chosen to match the referenced style: 0.6 + 0.4 * v
    return 0.6 + 0.4 * v;
}


// ------------------------------------------------------------------
// Golden-ratio procedural noise
// ------------------------------------------------------------------
const float PHI = 1.61803398874989484820459;
const float NOISE_SPEED = 1000.0;
const float NOISE_SCALE = 10000.0;

float gold_noise(in vec2 xy, in float seed) {
    return fract(tan(distance(xy * PHI, xy) * seed) * xy.x);
}

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


// ==========================================================
// Helper: returns 0->1 smoothly between startTime and duration
// ==========================================================
float animate(float t, float startTime, float duration) {
    if (duration == 0.0) return 1.0;               // avoid division by zero
    float a = (t - startTime) / duration;         // normalized offset
    return clamp(a, 0.0, 1.0);                    // works whether a positive or negative
}

// ==========================================================
// Ellipse + dot effect with background replacement
// ==========================================================
vec4 crt_turn_on(vec4 inColor, vec2 fragPx, vec2 resolutionPx, float t) {
    // --------------------------------------------------
    // PIXEL LOCK (this is the important bit)
    // --------------------------------------------------
    vec2 uv = ((floor(fragPx) + 0.5) / resolutionPx) - 0.5;   // center origin, same space as before

    //non pixelataed loook:
    //vec2 uv = (fragPx - 0.5 * resolutionPx) / resolutionPx;

    // --- PARAMETERS ---
    float fadeStart = 0.0;
    float fadeDuration = 0.20;

    float ryAnimStart = 0.1;
    float ryAnimDuration = 0.4;
    float ryStart = 0.25;
    float ryEnd = 0.003;

    float rxAnimStart = 0.38;
    float rxAnimDuration = 0.62;
    float rxStart = 0.25;
    float rxEnd = 0.0;

    float dotStart = 0.5;
    float dotDuration = 0.5;
    float dotRadiusMax = 0.1;

    // --- RADII ---
    float r_y = mix(ryStart, ryEnd, animate(t, ryAnimStart, ryAnimDuration));
    float r_x = mix(rxStart, rxEnd, animate(t, rxAnimStart, rxAnimDuration));

    vec2 r_in = vec2(r_x, r_y);
    vec2 r_out = r_in * 10.0;

    // --- ELLIPSE MASK (now pixelated) ---
    vec2 norm = uv / r_out;
    float d = length(norm);

    vec2 norm_in = uv / r_in;
    float inside = length(norm_in) < 1.0 ? 1.0 : 0.0;
    float ellipse = max(inside, smoothstep(1.0, r_in.x / r_out.x, d));

    vec4 glow = vec4(vec3(ellipse), ellipse);

    // --- DOT (also pixelated automatically) ---
    float dotT = animate(t, dotStart, dotDuration);
    if (dotT > 0.0) {
        float s = sin(dotT * 3.1415926);
        float radius = dotRadiusMax * s;

        float dist = length(uv);
        float dot = smoothstep(radius, 0.0, dist);

        glow.rgb += dot;
        glow.a = max(glow.a, dot);
    }

    float fade = animate(t, fadeStart, fadeDuration);
    vec4 color = mix(inColor, glow, fade);

    return color;
}

// ===================== VHS PAUSE EFFECT =====================

// Knobs
#define VHS_LINE_JITTER_AMPLITUDE    0.006
#define VHS_FRAME_JITTER_AMPLITUDE   0.003
#define VHS_COLOR_NOISE_STRENGTH     0.10
#define VHS_SCANLINE_COUNT          480.0   // logical tape lines

// RNG
float vhs_rand(vec2 co)
{
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

// --------------------
// Single self-contained function
// uv: normalized 0–1
// tex: sampler2D (iChannel0 style)
// --------------------
vec3 vhs_pause(in vec2 uv, sampler2D tex, float iTime)
{
    // Reconstruct scanline
    float scanline = floor(uv.y * VHS_SCANLINE_COUNT);

    // Horizontal per-line jitter
    float lineNoise = vhs_rand(vec2(iTime, scanline));
    uv.x += (lineNoise - 0.5) * VHS_LINE_JITTER_AMPLITUDE;

    // Vertical frame jitter
    float frameNoise = vhs_rand(vec2(iTime, 0.0));
    uv.y += (frameNoise - 0.5) * VHS_FRAME_JITTER_AMPLITUDE;

    // Clamp UVs to 0..1 (last valid sample)
    uv = clamp(uv, 0.0, 1.0);

    // Sample texture (Shadertoy Y-flip)
    vec3 color = texture(tex, vec2(uv.x, 1.0 - uv.y)).rgb;

    // Apply per-scanline chromatic noise
    vec3 noise;
    noise.r = vhs_rand(vec2(scanline, iTime));
    noise.g = vhs_rand(vec2(scanline, iTime + 1.0));
    noise.b = vhs_rand(vec2(scanline, iTime + 2.0));

    color += (noise - 0.5) * VHS_COLOR_NOISE_STRENGTH;

    return color;
}