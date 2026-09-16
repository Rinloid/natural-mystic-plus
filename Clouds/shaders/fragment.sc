$input v_color0

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-config.h"

void main() {
    #if defined(ENABLE_FBM_CLOUDS)
    /* We completely disable the vanilla clouds. It's impossible to
     * improve it. Instead we render clouds with sky shaders. */
    gl_FragColor = vec4_splat(0.0);
#else
    gl_FragColor = v_color0;
#endif /* ENABLE_FBM_CLOUDS */
}
