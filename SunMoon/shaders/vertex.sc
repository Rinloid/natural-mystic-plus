$input a_position, a_texcoord0

#ifdef INSTANCING__ON
    $input i_data0, i_data1, i_data2
#endif

$output v_uv0, localPos, duskOrDawn, night, phase

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-config.h"
#include "natural-mystic-hacks.h"

uniform vec4 FogColor;
uniform vec4 SunMoonColor;

void main() {
    #ifdef INSTANCING__ON
        mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
    #else
        mat4 model = u_model[0];
    #endif
    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

	localPos = vec2_splat(0.0);
	duskOrDawn = 0.0;
	night = 0.0;
	phase = 0.0;

	#if defined(ENABLE_SHADER_SUN_MOON)

		/* Enlarge the sun and the moon as the default size is too small
		* for us. We are going to draw actual shape of the sun and the
		* moon inside these vertices.
		*/
		vec3 p = a_position * vec3(10.0, 1.0, 10.0);
		gl_Position = mul(u_modelViewProj, vec4(p, 1.0));

		/* Apply some rotation to the moon. */
		localPos = mul(mat2(0.8, 0.6, -0.6, 0.8), p.xz);

		/* Hacky time detections. */
		duskOrDawn = isDuskOrDawn(FogColor);
		night      = isNight(FogColor);
		phase      = moonPhase(a_texcoord0);

	#else
		// Copied from the vanilla vertex
		gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));
	#endif /* defined(ENABLE_SHADER_SUN_MOON) */

	v_uv0 = a_texcoord0;
}