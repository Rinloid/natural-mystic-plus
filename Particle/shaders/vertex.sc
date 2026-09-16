$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING__ON
    $input i_data0, i_data1, i_data2
#endif

$output v_color0, v_fog, v_texcoord0, clearWeather

#include "bgfx_shader.sh"
#include "FogUtil.dragonh"
#include "natural-mystic-hacks.h"

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;

void main() {
    #ifdef INSTANCING__ON
        mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
    #else
        mat4 model = u_model[0];
    #endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

    float fogIntensity = calculateFogIntensity(gl_Position.z, FogAndDistanceControl.z, FogAndDistanceControl.x, FogAndDistanceControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);
    clearWeather = 1.0 - isRainy(FogAndDistanceControl.xy);

    v_texcoord0 = a_texcoord0;
    v_color0 = a_color0;
    v_fog = fog;
}