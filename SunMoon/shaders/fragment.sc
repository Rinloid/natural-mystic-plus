$input v_uv0, localPos, duskOrDawn, night, phase

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-config.h"
#include "natural-mystic-hacks.h"
#include "natural-mystic-noise.h"
#include "natural-mystic-sunmoon.h"

uniform vec4 SunMoonColor;
uniform vec4 FogColor;

SAMPLER2D_AUTOREG(s_SunMoonTexture);

void main() {
	#if defined(ENABLE_SHADER_SUN_MOON)

		if (texture2D(s_SunMoonTexture, vec2_splat(0.5)).r > 0.1) {
			gl_FragColor = renderSun(localPos, duskOrDawn, FogColor, SunMoonColor);
		}
		else {
			gl_FragColor = renderMoon(localPos, phase, SunMoonColor);
		}

	#else

		// Copied from the vanilla texture_ccolor.fragment
	vec4 diffuse = SunMoonColor * texture2D(s_SunMoonTexture, v_uv0);
		gl_FragColor = diffuse;

	#endif /* defined(ENABLE_SHADER_SUN_MOON) */
}
