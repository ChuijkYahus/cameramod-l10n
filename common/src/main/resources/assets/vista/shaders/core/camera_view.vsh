#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;  // texture uv
in ivec2 UV1; // overlay uv
in ivec2 UV2; // lightmap uv
in vec3 Normal;

uniform sampler2D Sampler2;
uniform sampler2D Sampler0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

/* Sprite frame dimensions in normalized UVs */
uniform vec2 SpriteDimensions;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;        // original texture coord
out vec2 texCoord1;        // overlay coord

/* Precomputed frame origin / size for fragment shader */
out vec2 vFrameOriginUV;
out vec2 spriteSizePx;
out vec2 atlasSizePx;      // texture size in pixels

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // distance for fog
    vertexDistance = fog_distance(Position, FogShape);

    // base vertex color with lighting
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);

    // atlas size in pixels
    atlasSizePx = vec2(textureSize(Sampler0, 0));

    // sprite resolution in pixels (frame size × atlas size)
    spriteSizePx = SpriteDimensions * atlasSizePx;

    // frame origin inside atlas (floor once per vertex)
    vFrameOriginUV = floor(UV0 / SpriteDimensions) * SpriteDimensions;

    // light map color
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);

    // pass-through UVs
    texCoord0 = UV0;
    texCoord1 = UV1;
}
