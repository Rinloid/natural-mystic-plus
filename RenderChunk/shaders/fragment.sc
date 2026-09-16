$input v_color, v_fog, v_uv0, v_uv1, wPos, cameraDist, vNormal, flickerFactor, desatFactor, clearWeather, waterFlag, waterPlane, cloudColor

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
#include "natural-mystic-rain.h"
#include "natural-mystic-water.h"
#include "natural-mystic-cloud.h"

uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

void main() {
    vec4 diffuse = vec4_splat(0.0);
	float occlusion = 0.0;

	#if defined(DEPTH_ONLY_OPAQUE_PASS) || defined(DEPTH_ONLY_PASS)
		diffuse.rgb = vec3_splat(1.0);
	#else
		diffuse = texture2D(s_MatTexture, v_uv0);

		#if defined(ALPHA_TEST_PASS)
			if (diffuse.a < 0.5) {
				discard;
			}
		#endif

		#if defined(SEASONS__ON) && (defined(OPAQUE_PASS) || defined(ALPHA_TEST_PASS))
			diffuse.rgb *= mix(vec3_splat(1.0), texture2D(s_SeasonsTexture, v_color.xy).rgb * 2.0, v_color.b);
			diffuse.rgb *= v_color.aaa;
			occlusion = 1.0; /* Assume it's not occluded at all. */
		#else
			diffuse *= v_color;
			/* The game appears to encode an ambient occlusion in the vertex
			* color in this case. Abuse it to create more shadows. */
			occlusion = occlusionFactor(v_color.rgb);
		#endif
	#endif

	#ifndef TRANSPARENT_PASS
		diffuse.a = 1.0;
	#endif

	/* Fetch the level of daylight (i.e. the one which darkens at
	 * night) from the light map passed by the upstream. Note that we
	 * intentionally reduce the dynamic range because the upstream
	 * daylight level doesn't drop to zero at night. */
	float daylight = texture2D(s_LightMapTexture, vec2(0.0, 1.0)).r;
	daylight = smoothstep(0.4, 1.0, daylight);

	/* Fetch the level of ambient light from the light map passed by
	 * the upstream. The constant multiplifier is determined so the
	 * intensity will be 6 on the Overworld and 26 in the
	 * Nether/End. */
	float ambientBrightness = texture2D(s_LightMapTexture, vec2(0.0, 0.0)).r * 44.797;

	/* Calculate the color of the ambient light based on the fog
	 * color. We'll use it at several places. Note that the .a
	 * component denotes the intensity. */
	vec3 ambientColor;

	bool isUnderwater = isInWater(FogAndDistanceControl.xy);

	if (v_fog.x > 0.0) {
		if (isRenderDistanceFog(FogAndDistanceControl.xy)) {
			/* Don't use the fog color in this case, as it would be
			* slightly different from the color of near terrain. */
			ambientColor = ambientLightColor(v_uv1.y, daylight);
		}
		else {
			/* The existence of bad weather fog (and also underwater fog)
			* should increase the intensity of ambient light. But
			* at night it should work the other way.
			*/
			if (isUnderwater) {
				ambientColor = ambientLightColor(v_fog);
				ambientBrightness *= mix(0.9, 1.4, daylight);
			}
			else {
				ambientColor = mix(
					ambientLightColor(v_fog),
					ambientLightColor(v_uv1.y, daylight),
					clearWeather);
				ambientBrightness *= mix(mix(0.9, 1.4, daylight), 1.0, clearWeather);
			}
		}
	}
	else {
		ambientColor = ambientLightColor(v_uv1.y, daylight);
	}

	/* Save the diffused color here as the color of the material. We
	 * are going to redo all the lightings with our own HDR method. */
	vec3 pigment = diffuse.rgb;

	/* Accumulate all the light to one linear RGB vector. We are going
	 * to use it for diffuse lighting, and also specular lighting. */
	vec3 dirLight   = vec3_splat(0.0);
	vec3 undirLight = vec3_splat(0.0);

	undirLight += ambientLight(ambientColor, ambientBrightness);

	if (v_fog.x > 0.0) {
		/* When it's raining the sunlight shouldn't affect the
		* scene, but we cannot treat the rain as a boolean switch as that
		* would cause #40. */
		if (isUnderwater) {
			dirLight += sunlight(v_uv1.y, daylight);
			dirLight += moonlight(v_uv1.y, daylight);
		}
		else {
			dirLight += sunlight(v_uv1.y, daylight) * clearWeather;
			dirLight += moonlight(v_uv1.y, daylight) * clearWeather;
		}
	}
	else {
		dirLight += sunlight(v_uv1.y, daylight);
		dirLight += moonlight(v_uv1.y, daylight);
	}
		undirLight += skylight(v_uv1.y, daylight);
		/* Torchlight is directional, but since we don't actually know
		* their directions we have to consider it as undirectional. */
		undirLight += torchLight(v_uv1.x, v_uv1.y, daylight, flickerFactor);

	/* Light sources should be significantly brighter than regular
	 * objects. */
	if (v_uv1.x == 1.0) {
		undirLight += emissiveLight(flickerFactor);
	}

	/* Now we finished accumulating light. Compute the diffuse light
	 * and the specular light here. We assume the color of specular
	 * light is always the same as the color of accumulated light. */

	vec3  sNormal = normalize(cross(dFdx(wPos), dFdy(wPos)));
	float wet     = wetness(clearWeather, v_uv1.y);
	if (waterFlag > 0.5) {
		/* Compute the specular light and the opacity of water. It is
		* tempting to do this only when defined(BLEND), but if we do
		* that water in far terrain will have different colors.
		*
		* We need a per-fragment normal here, which is an
		* interpolated vertex normal passed by our vertex
		* shader. However, we can't compute it correctly for anything
		* other than flat top surfaces because the game doesn't tell
		* us anything about normals. In the fragment shader we can
		* compute the surface normal with dFdx/dFdy but it's only a
		* normal of flat triangles. Luckily for us as the water
		* volume decreases the perturbance also decreases and thus
		* the surface becomes more and more flat. So to disguise the
		* problem we mix the flat surface normal with the smooth,
		* analytical but the incorrect one, using the volume. */
		vec3 fNormal = normalize(mix(sNormal, vNormal, waterPlane));

	#if defined(ENABLE_WAVES)
		/* Perturb the normal even more, but this time with much higher
		* frequencies. This is a kind of bump mapping. */
		const float distThreshold = 0.6;
		const float distFadeStart = distThreshold * 0.8;
		if (cameraDist < distThreshold) {
			/* But perturbing the normal on far geometry doesn't
			* contribute to the overall quality, and it may even
			* cause aliasing. Also reduce the perturbance depending
			* on the sunlight level. */
			vec3 perturbed = waterWaveNormal(wPos, ViewPositionAndTime.w, fNormal);
			perturbed = mix(perturbed, fNormal,
							smoothstep(distFadeStart, distThreshold, cameraDist));
			perturbed = mix(sNormal, perturbed, smoothstep(0.5, 1.0, v_uv1.y));
			fNormal = normalize(perturbed);
		}
	#endif /* defined(ENABLE_WAVES) */

	pigment.rgb *= 0.5; // Darken the base water color.

	#if defined(ENABLE_CLOUD_REFLECTION)
		vec3 skyPos = reflect(normalize(wPos), fNormal);

		/* The view-space positions in the skyplane are
		 * 10 times bigger than the terrain one. */
		skyPos.xz *= 0.1;

		float cloudDist = smoothstep(0.0, 1.0, length(skyPos.xz / (skyPos.y)));

		if (cloudDist < 1.0 && !bool(step(skyPos.y, 0.0))) {
			/* Generate the same clouds as ones in the skyplane
			 * but in less octaves to reduce lag.
			 */
			vec4 shadedCloud = generateShadedClouds(4, ViewPositionAndTime.w, skyPos / skyPos.y, cloudColor, vec4(pigment, 1.0), clearWeather);

			/* Calculate a fresnel effect for reflection. */
			const float fresnel = 0.06;
			vec3  viewDir   = -normalize(wPos);
			float incident  = max(0.0, dot(viewDir, fNormal));
			float reflCoeff = fresnel + (1.0 - fresnel) * pow(1.0 - incident, 5.0);

			pigment = mix(pigment, shadedCloud.rgb, shadedCloud.a * smoothstep(0.7, 1.0, v_uv1.y) * reflCoeff);
		}
	#endif

	diffuse.rgb  = pigment * (dirLight + undirLight);

	#if defined(ENABLE_SPECULAR)
		vec4 specular = waterSpecularLight(diffuse.a, dirLight, undirLight, wPos, ViewPositionAndTime.w, fNormal);

		diffuse.rgb += specular.rgb;
		diffuse.a    = specular.a;
	#endif /* defined(ENABLE_SPECULAR) */

	#if defined(ENABLE_RIPPLES)
		if (wet > 0.0) {
			diffuse.rgb += wet * ripples(dirLight + undirLight, wPos, cameraDist, ViewPositionAndTime.w, fNormal);
		}
	#endif /* defined(ENABLE_RIPPLES) */
	}
	else {
	#if defined(ENABLE_OCCLUSION_SHADOWS)
		/* The intensity of directional light should be affected by the
		* occlusion factor. */
		const float occlShadow = 0.35;
		dirLight *= mix(occlShadow, 1.0, occlusion);
	#endif /* defined(ENABLE_OCCLUSION_SHADOWS) */

		/* Wet ground should have reduced diffuse light if it's made
		* of a rough material. But for now it's a constant value
		* because we don't know what a material it's made of. */
		diffuse.rgb = pigment * (dirLight + undirLight);
		diffuse.rgb *= mix(1.0, 0.5, wet);

	#if defined(ENABLE_SPECULAR)
		const float fresnel   = 0.04;
		const float shininess = 2.0;
		vec3 specular = specularLight(fresnel, shininess, dirLight, undirLight, wPos, sNormal);

		diffuse.rgb *= 1.0 - fresnel;
		diffuse.rgb += specular * mix(1.0, 5.0, wet);
	#endif /* defined(ENABLE_SPECULAR) */

	#if defined(ENABLE_RIPPLES)
		if (wet > 0.0) {
			diffuse.rgb += wet * ripples(dirLight + undirLight, wPos, cameraDist, ViewPositionAndTime.w, sNormal);
		}
	#endif /* defined(ENABLE_RIPPLES) */
	}


	diffuse.rgb = uncharted2ToneMap(diffuse.rgb, 112.0, 1.0);
	diffuse.rgb = contrastFilter(diffuse.rgb, 1.25);

	/* Reduce the contrast of far objects. The overall color
	 * should lean towards the ambient. And we also apply a permanent
	 * fog. It should ideally be biome-specific but we can't do
	 * that currently, so we always use the ambient color. */
	#if defined(ENABLE_BASE_FOG)
		const float contrast   = 0.45;
		const float fogDensity = 0.4;
		diffuse.rgb = mix(
			diffuse.rgb,
			mix(contrastFilter(diffuse.rgb, contrast) * ambientColor,
				fogBrightness(v_uv1.x, v_uv1.y, daylight) * ambientColor,
				fogDensity),
			desatFactor);
	#endif

	/* We can't apply fogs before doing tone mapping, because that
	 * changes the fog color and breaks "render distance fog". */
	if (v_fog.a > 0.0) {
		if (isUnderwater || isRenderDistanceFog(FogAndDistanceControl.xy)) {
			diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);
		}
		else {
			/* During a bad weather everything should look dull. We do a
			* desaturation for that. The reason for the mix() is so
			* fogs not only reduce saturation but also contrast.
			*/
			diffuse.rgb = mix(desaturate(diffuse.rgb, v_fog.a) * ambientColor, v_fog.rgb, v_fog.a);
		}
	}

	#if defined(DEBUG_SHOW_VERTEX_COLOR)
		diffuse = color;

	#elif defined(DEBUG_SHOW_SUNLIGHT_LEVEL)
		diffuse.rgb = vec3_splat(v_uv1.y);

	#elif defined(DEBUG_SHOW_OCCLUSION_FACTOR)
		diffuse.rgb = vec3_splat(occlusion);

	#elif defined(DEBUG_SHOW_FOG_COLOR)
		diffuse.rgb = FogColor.rgb;

	#elif defined(DEBUG_SHOW_FOG_CONTROL)
		diffuse.rgb = vec3_splat(cameraDist < 0.5 ? FogAndDistanceControl.x : FogAndDistanceControl.y);
	#endif /* DEBUG */

    gl_FragColor = diffuse;
}