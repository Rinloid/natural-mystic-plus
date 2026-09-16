$input v_color0, wPos, cloudColor, camDist, clearWeather

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-config.h"
#include "natural-mystic-cloud.h"

uniform vec4 ViewPositionAndTime;

void main() {

#if defined(ENABLE_FBM_CLOUDS)
    vec4 shadedCloud = generateShadedClouds(6, ViewPositionAndTime.w, wPos, cloudColor, v_color0, clearWeather);

    /* Clouds near the horizon should be blended back to the sky
     * color, or otherwise the planar nature of the sky plane will be
     * even more apparent. */
    gl_FragColor = mix(shadedCloud, v_color0, smoothstep(0.9, 1.0, camDist));

#else
    gl_FragColor = v_color0;
#endif /* defined(ENABLE_FBM_CLOUDS) */
}
