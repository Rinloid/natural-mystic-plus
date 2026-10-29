$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
    $input i_data0, i_data1, i_data2
#endif

$output v_color, v_fog, v_uv0, v_uv1, wPos, cPos, cameraDist, vNormal, flickerFactor, desatFactor, clearWeather, waterFlag, waterPlane, cloudColor

#include "bgfx_shader.sh"

#if BGFX_SHADER_LANGUAGE_GLSL
	precision highp float;
	precision highp int;
#endif

#include "natural-mystic-color.h"
#include "natural-mystic-config.h"
#include "natural-mystic-fog.h"
#include "natural-mystic-hacks.h"
#include "natural-mystic-light.h"
#include "natural-mystic-water.h"

uniform vec4 RenderChunkFogAlpha;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;
uniform vec4 MinecraftCameraPosition;

void main() {
	#ifdef INSTANCING__ON
		mat4 model = mtxFromCols(i_data0, i_data1, i_data2, vec4(vec3_splat(0.0), 1.0));
	#else
		mat4 model = u_model[0];
	#endif

    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

	#ifdef RENDER_AS_BILLBOARDS__ON
		worldPos += 0.5;
		vec3 viewDir = normalize(worldPos - ViewPositionAndTime.xyz);
		vec3 boardPlane = normalize(vec3(viewDir.z, 0.0, -viewDir.x));
		worldPos = (worldPos -
			((((viewDir.yzx * boardPlane.zxy) - (viewDir.zxy * boardPlane.yzx)) *
			(a_color0.z - 0.5)) +
			(boardPlane * (a_color0.x - 0.5))));
	#endif
	gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

	/* Completely insane, but somehow these messy ofsets
	 * are needed to work on the latest version.
	 */
	uvec2 uv0Temp = uvec2(round(a_texcoord0 * 65535.0));
    v_uv0 = vec2((uv0Temp & 32767u) << 1u) * 1.5259021893143653e-05;
    v_uv0 += 3.0517578125e-05 * (2.0 * vec2((uv0Temp & 32768u) >> 15u) - 1.0);
    v_uv1 = fract(a_texcoord1.y * vec2(256.0, 4096.0));

	#ifdef RENDER_AS_BILLBOARDS__ON
		v_color = vec4_splat(1.0);
	#else
		v_color = a_color0;
	#endif

	cPos = worldPos.xyz;
	wPos = cPos + MinecraftCameraPosition.xyz;

	vNormal = vec3_splat(0.0);
	flickerFactor = 1.0;
	#if defined(ENABLE_TORCH_FLICKER)
		if (v_uv1.x > 0.0) {
			flickerFactor = torchLightFlicker(wPos.xyz, ViewPositionAndTime.w);
		}
	#endif

    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);

	/* Reduce the contrast of far objects. The overall color
	 * should lean towards the ambient. Note that cameraDist is a
	 * normalized camera distance being 1.0 at the point where the far
	 * terrain fog ends. */
	desatFactor = exponentialFog(vec2(0.0, 4.0), camDis / FogAndDistanceControl.z);

	/* Detect the weather on the Overworld. */
	clearWeather = 1.0 - isRainy(FogAndDistanceControl.xy);

	float len = camDis / FogAndDistanceControl.z;
	cameraDist = len;

    v_fog.rgb = FogColor.rgb;
	#if defined(FOG_TYPE)
		#if FOG_TYPE == FOG_TYPE_LINEAR
			v_fog.a = linearFog(FogAndDistanceControl.xy, len);
		#elif FOG_TYPE == FOG_TYPE_EXP
			v_fog.a = exponentialFog(FogAndDistanceControl.xy, len);
		#elif FOG_TYPE == FOG_TYPE_EXP2
			v_fog.a = exponentialSquaredFog(FogAndDistanceControl.xy, len);
		#endif
	#endif

	#if !defined(SEASONS__ON) && defined(TRANSPARENT_PASS)
		waterFlag = isWater(a_color0) ? 1.0 : 0.0;
	#else
		waterFlag = 0.0;
	#endif
	waterPlane = 0.0;

	cloudColor = vec4_splat(0.0);
	if (waterFlag > 0.5) {
		float brightness = clamp(desaturate(FogColor.rgb, 1.0).r + 0.05, 0.105, 1.0);
		cloudColor     = mix(a_color0, FogColor, 0.9);
		cloudColor     = mix(cloudColor, vec4_splat(1.0), brightness);
		cloudColor.rgb = desaturate(cloudColor.rgb, 0.4);
	}

	/* Waves */
	#if defined(ENABLE_WAVES)
		bool grassFlag = isFoliage(a_color0, a_position.xyz);

	if (grassFlag) {
		vec3 posw = wPos;
		float wave = sin(ViewPositionAndTime.w * 3.5 + 2.0 * posw.x + 2.0 * posw.z + posw.y);
		float amplitude = 0.015;
		/* Reduce the amplitude if it's indoor, i.e. the sunlight
		 * level is low. */
		gl_Position.x += wave * amplitude * smoothstep(0.7, 1.0, v_uv1.y);
	}
	else if (waterFlag > 0.5) {
		/* We want water to swell in proportion to its volume. The more
		 * the vertex is close to the ground (i.e. integral points in the
		 * world position), the less the vertex should swell. Without this
		 * tweak the water will leave the ground. */
		float volume = fract(a_position.y);
		if (volume > 0.0) {
			vec3 wPos1     = waterWaveGeometric(wPos, ViewPositionAndTime.w, vNormal);
			vec3 wPosDelta = (wPos1 - wPos) * volume;
			/* Also reduce the amplitude depending on the sunlight
			 * level. We do the same for wave normal in the fragment
			 * shader. */
			wPos         += wPosDelta * smoothstep(0.5, 1.0, v_uv1.y);
			gl_Position  = mul(u_viewProj, vec4(worldPos, 1.0));
		}

		/* When we know the surface normal we can do something
		 * advanced. */
		if (isWaterPlane(vec4(a_position, 1.0))) {
			waterPlane = 1.0;
		}
		// The default opacity of water is way too high. Reduce it.
		v_color.a *= 0.5;
	}
	#endif

	#ifdef TRANSPARENT_PASS
		if (a_color0.a < 0.95) {
			v_color.a = mix(v_color.a, 1.0, clamp((camDis / FogAndDistanceControl.w), 0.0, 1.0));
		}
	#endif
}