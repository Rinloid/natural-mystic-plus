$input v_color0

#include "bgfx_shader.sh"

uniform vec4 StarsColor;

void main() {
    vec4 starColor = v_color0;
    starColor.rgb *= StarsColor.rgb * v_color0.a;

    gl_FragColor = starColor;
}