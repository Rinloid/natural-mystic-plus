$input v_color0, v_fog, v_texcoord0, clearWeather

#include "bgfx_shader.sh"
#include "FogUtil.dragonh"
#include "natural-mystic-color.h"

SAMPLER2D_AUTOREG(s_ParticleTexture);

void main() {
    vec4 diffuse = texture2D(s_ParticleTexture, v_texcoord0);
    #if ALPHA_TEST_PASS
        if (diffuse.w < 0.5) {
            discard;
        }
    #endif
    
    diffuse *= v_color0;
    diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);
    /* When it's raining The color of particles should be highly
	 * desaturated because it looks ugly when the rain drops look
	 * blue. */
	diffuse.rgb = desaturate(diffuse.rgb, mix(0.95, 0.0, clearWeather));

    #if ALPHA_TEST_PASS
        diffuse.a = 1.0;
    #endif

    gl_FragColor = diffuse;
}
