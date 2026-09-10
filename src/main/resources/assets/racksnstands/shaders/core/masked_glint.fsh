#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GlintAlpha;
in float vertexDistance;
in vec2 glintUv;
in vec2 spriteUv;
out vec4 fragColor;
void main() {
    // Depth equality alone cannot identify which coplanar item wrote a pixel.
    // Test this item's own sprite alpha before applying its enchantment sheen.
    float mask = texture(Sampler1, spriteUv).a;
    if (mask < 0.1) discard;
    vec4 color = texture(Sampler0, glintUv) * ColorModulator;
    if (color.a < 0.1) discard;
    float fade = linear_fog_fade(vertexDistance, FogStart, FogEnd) * GlintAlpha * mask;
    fragColor = vec4(color.rgb * fade, color.a);
}
