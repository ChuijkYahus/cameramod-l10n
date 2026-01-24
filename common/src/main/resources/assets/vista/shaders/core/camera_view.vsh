#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
uniform sampler2D Sampler0;


uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;// pass full UV0 to fragment shader
out vec2 atlasSizePx;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // distance for fog
    vertexDistance = fog_distance(Position, FogShape);

    // base vertex color with lighting
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);

    atlasSizePx = vec2(textureSize(Sampler0, 0));// atlas size in texels

    // light map color
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    // pass UV0 unchanged; fragment shader will compute frame-local coords
    texCoord0 = UV0;
}
