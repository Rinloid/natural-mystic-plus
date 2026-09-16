// -*- glsl -*-
#if !defined(NATURAL_MYSTIC_CLOUD_H_INCLUDED)
#define NATURAL_MYSTIC_CLOUD_H_INCLUDED 1

#include "natural-mystic-noise.h"

/* Generate a pattern of clouds based on a world position. */
float cloudMap(int octaves, float lowerBound, float upperBound, float time, vec3 pos) {
    const vec2 resolution = vec2(1.4, 1.4);

    vec2 st = pos.xz / resolution;
    /* The inverse of the speed (512) should be a power of two in
     * order to avoid a precision loss.
     */
    st.y += time / 512.0;

    /* We intentionally throw away some
     * of the precision so we get somewhat sparse noise.
     */
    return fBM(octaves, lowerBound, upperBound, st * 3.0);
}

vec4 generateShadedClouds(const int octaves, float time, vec3 pos, vec4 baseCloudColor, vec4 baseSkyColor, float isClearWeather) {
    /* We are going to perform a (sort of) volumetric ray marching to
     * compute self-casting shadows of clouds, but with only a
     * few steps. This is because ray marching is terribly expensive
     * as we cannot precompute noises in a texture and instead we have
     * to generate them on the fly. See also
     * http://www.iquilezles.org/www/articles/dynclouds/dynclouds.htm */
    float density = cloudMap(octaves, mix(0.0, 0.5, isClearWeather), mix(1.0, 0.85, isClearWeather), time, pos);
    vec4 shadedCloud = mix(vec4(baseCloudColor.rgb, 0.0), baseCloudColor, density);

    #if defined(ENABLE_CLOUD_SHADE)
    /* Optimization: Don't bother to do it when there are no clouds at
     * the current position. */
    if (density > 0.0) {
        /* The game doesn't tell us where the sun or the moon is,
         * which is so unfortunate. We have to assume they are always
         * at a fixed point. */
        const vec3 sunMoonPos = vec3(-0.3, 4.0, 0);

        const int   numSteps = 1; /* Yes, it has to be this few, or
                                   * we'll get a lag. */
        const float stepSize = 0.2;
        vec3        rayStep  = normalize(sunMoonPos - pos) * stepSize;
        vec3        rayPos   = pos;
        float       inside   = 0.0;
        for (int i = 0; i < numSteps; i++) {
            rayPos += rayStep;
            float height = cloudMap(octaves / 2, mix(0.0, 0.4, isClearWeather), 1.0, time, rayPos);
            inside += max(0.0, height - (rayPos.y - pos.y));
        }
        /* Average of height differences. This isn't a distance of ray
         * traveled inside clouds in a normal sense, but if we do it
         * strictly we get severe banding artifacts (because of the
         * number of steps being too few). */
        inside /= float(numSteps);

        float brightness = baseCloudColor.r;
        shadedCloud.rgb = mix(
            shadedCloud.rgb + 0.1 * brightness, // highlight
            max(vec3_splat(0.0), shadedCloud.rgb - 0.2 * brightness), // shade
            inside);
    }
    #endif /* defined(ENABLE_CLOUD_SHADE) */
    shadedCloud.rgb = mix(baseSkyColor.rgb, shadedCloud.rgb, shadedCloud.a);

    return shadedCloud;
}

#endif /* !defined(NATURAL_MYSTIC_CLOUD_H_INCLUDED) */