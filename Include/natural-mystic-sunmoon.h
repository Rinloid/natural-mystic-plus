// -*- glsl -*-
#if !defined(NATURAL_MYSTIC_SUNMOON_H_INCLUDED)
#define NATURAL_MYSTIC_SUNMOON_H_INCLUDED 1

vec4 renderSun(vec2 pos, float isDuskOrDawn, vec4 fogColor, vec4 sunMoonColor) {
    const float radius     = 0.08;
    const float sharpness  = 0.2;  // The smaller the sharper the edge will be.
    const float colorScale = 2.5;
    const vec3  baseColor  = vec3(1.0, 0.55, 0.15);

    /* The fog color is white during the daytime and is red at dusk
     * and dawn. We use it as the color of the sun. We also hide the
     * sun when it's raining, hence the SunMoonColor.a.
     */
    vec4 sunColor = vec4(fogColor.rgb * baseColor * colorScale, sunMoonColor.a);

    /* The body of the sun in a Signed Distance Field. */
    float sdf        = length(pos) - radius;
    float brightness = 1.0 - smoothstep(0.0, radius * sharpness, sdf);

    /* Render halo, or sort of. When it's daytime and is clear, quite
     * a large area around the sun should be rendered extremely
     * brightly. We do this by using the distance between the current
     * pixel and the border of the sun as the factor of the
     * brightness, i.e. the closer the brighter. */
    float halo = (1.0 - isDuskOrDawn) * 0.7;
    brightness = max(brightness, (1.0 - sdf) * halo);

    return clamp(sunColor * brightness, 0.0, 1.0);
}

/* Based on https://www.shadertoy.com/view/XsdGzX
 * and http://learnwebgl.brown37.net/09_lights/lights_diffuse.html
 */
float diffuseSphere(vec2 pos, float radius, vec3 light) {
    float sq = radius*radius - pos.x*pos.x - pos.y*pos.y;

    if (sq < 0.0) {
        return 0.0;
    }
    else {
        float z      = sqrt(sq);
        vec3  normal = normalize(vec3(pos, z));
        return max(0.0, dot(normal, light));
    }
}

/* Huge thanks for ESBE-2G shaders. I took its moon renderer as a
 * reference (although I didn't use it directly).
 */
vec4 renderMoon(vec2 pos, float phase, vec4 sunMoonColor) {
    const float radius    = 0.11;
    const float sharpness = 0.15; // The smaller the sharper the edge will be.
    const vec3  baseColor = vec3(1.0, 0.95, 0.81);
    const vec2  resolution = vec2(0.06, 0.06);

    /* We hide the moon when it's raining, hence the
     * SunMoonColor.a.
     */
    vec4 moonColor = vec4(baseColor, sunMoonColor.a);

    /* The body of the moon in a Signed Distance Field. */
    float pDistance  = length(pos);
    float sdf        = pDistance - radius;

    /* Compute the light vector based on the phase of moon. */
    vec3 light = vec3(sin(phase), 0.0, -cos(phase));

    /* Perform a diffuse lighting to render the phase of moon. */
    float diffuse = diffuseSphere(pos, radius, light);
    /* Hacky adjustment of the diffuse light. */
    float brightness = smoothstep(0.2, 0.8, min(diffuse + 0.3, 1.0));

    /* Generate the texture of the moon with 2D simplex noise. */
    float tex = simplexNoise(pos / resolution);
    brightness *= 1.0 - clamp(tex, 0.0, 1.0) * 0.05;

    /* Apply the sharpness to the surface. */
    brightness *= 1.0 - smoothstep(radius - radius * sharpness, radius, pDistance);

    return clamp(moonColor * brightness, 0.0, 1.0);
}

#endif /* !defined(NATURAL_MYSTIC_SUNMOON_H_INCLUDED) */
