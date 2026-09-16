$input a_color0, a_position
#ifdef INSTANCING__ON
    $input i_data0, i_data1, i_data2
#endif

$output v_color0, wPos, cloudColor, camDist, clearWeather

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-color.h"
#include "natural-mystic-hacks.h"

uniform vec4 SkyColor;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;

void main() {
    #ifdef INSTANCING__ON
        mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
    #else
        mat4 model = u_model[0];
    #endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

    wPos = a_position;
    v_color0 = mix(SkyColor, FogColor, a_color0.x);
    camDist = length(a_position);

    /* The color of clouds resembles that of the sky, but is more
     * close to that of the fog. This way we will have something like
     * a sunlight-reflected-by-clouds effect (but more
     * lightgray-ish). */
    float brightness = clamp(desaturate(FogColor.rgb, 1.0).r + 0.05, 0.105, 1.0);
    cloudColor       = mix(v_color0, FogColor, 0.9);
    cloudColor       = mix(cloudColor, vec4_splat(1.0), brightness);
    cloudColor.rgb   = desaturate(cloudColor.rgb, 0.4);

    clearWeather = 1.0 - isRainy(FogAndDistanceControl.xy);
}