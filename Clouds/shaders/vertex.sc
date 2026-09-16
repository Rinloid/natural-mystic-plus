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

uniform vec4 CloudColor;
uniform vec4 DistanceControl;

CONST(float fogNear = 0.9);

void main() {
    #ifdef INSTANCING__ON
        mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
    #else
        mat4 model = u_model[0];
    #endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

#if defined(ENABLE_FBM_CLOUDS)
	/* We completely disable the vanilla clouds. It's impossible to
	 * improve it. Instead we render clouds with sky shaders. */
	v_color0 = vec4_splat(0.0);
#else
	v_color0 = a_color0 * CloudColor;

 	float depth = length(a_position) / DistanceControl.x;
 	float fog = max(depth - fogNear, 0.0);

 	v_color0.a *= clamp(1.0 - fog, 0.0, 1.0);
#endif
}