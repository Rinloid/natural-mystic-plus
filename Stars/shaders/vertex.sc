$input a_color0, a_position
#ifdef INSTANCING__ON
    $input i_data0, i_data1, i_data2
#endif

$output v_color0

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-config.h"
#include "natural-mystic-noise.h"

/* Generate a pattern of brightness of stars based on a world
 * position. */
float stars(vec3 pos) {
    const vec2 resolution = vec2(0.3, 0.3);

    vec2 st = pos.xz / resolution;
    return (simplexNoise(st) + 1.0) * 0.5;
}


void main() {
    #ifdef INSTANCING__ON
        mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
    #else
        mat4 model = u_model[0];
    #endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

    v_color0 = a_color0;

    #if defined(ENABLE_RANDOM_STARS)
        v_color0.rgb *= vec3_splat(stars(a_position.xyz));
    #endif
}