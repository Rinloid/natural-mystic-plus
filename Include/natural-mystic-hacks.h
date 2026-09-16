// -*- glsl -*-
#if !defined(NATURAL_MYSTIC_HACKS_H_INCLUDED)
#define NATURAL_MYSTIC_HACKS_H_INCLUDED 1

/* Detect foliage based on its biome colors and hardcoded height. 
 * The idea of crop detection is from Eringi, See:
 * https://github.com/McbeEringi/esbe-3g/blob/master/ESBE_3G/shaders/glsl/terrain.vsh
*/
bool isFoliage(vec4 color, vec3 pos) {
    vec3 fractPos = fract(pos.xyz);
    #if defined(ALPHA_TEST_PASS)
        return (color.g != color.b && color.r < color.g + color.b) || (fractPos.y == 0.9375 && (fractPos.z == 0.0 || fractPos.x == 0.0));
    #else
        return false;
    #endif
}

/* Detect water based on its transparency. */
bool isWater(vec4 color) {
    return (color.a < 0.95 && color.a > 0.05);
}

/* Detect a water plane based on the world position of a vertex. */
bool isWaterPlane(vec4 worldPos) {
    float y = fract(worldPos.y);
    return y >= 0.7 && y <= 0.9;
}

/* Detect if the player is underwater. */
bool isInWater(vec2 fogControl) {
    return fogControl.x == 0.0;
}

/* Detect the render distance fog. NOTE: It is tempting to use
 * material variants "*_far" instead of this hack (see
 * terrain.material) but no, that's actually not possible because the
 * fog they can have is not always the render distance fog. It can
 * also be the bad weather fog.
 */
bool isRenderDistanceFog(vec2 fogControl) {
    return fogControl.x > 0.6;
}

/* Detect rainy weather. */
float isRainy(vec2 fogControl) {
    return mix(smoothstep(0.5, 0.3, fogControl.x), 0.0, step(fogControl.x, 0.0));
}

/* When it's near dusk or dawn, the B component of the fog color
 * gradually decreases. We exploit this fact to detect dusk and
 * dawn. Terrain shaders don't need this function because they can
 * directly fetch the daylight level. This function returns 1.0 when
 * it's dusk or dawn, or 0.0 otherwise.
 */
float isDuskOrDawn(vec4 fogColor) {
    return pow(clamp(1.0 - fogColor.b * 1.7, 0.0, 1.0), 0.3);
}

/* When it's night, the R component of the fog color significantly
 * decreases. We exploit this fact to detect night. Terrain shaders
 * don't need this function because they can directry fetch the
 * daylight level. This function returns 1.0 when it's night, or 0.0
 * otherwise.
*/
float isNight(vec4 fogColor) {
    return pow(clamp(1.0 - fogColor.r * 1.5, 0.0, 1.0), 2.0);
}

/* Translate the texture UV into the phase of moon. This function
 * returns [0, 2π) where 0 is the new moon and π is the full
 * moon. This is a hack.
 */
float moonPhase(vec2 texUV) {
    float x = floor(texUV.x * 4.0) * 0.25; // {0, 0.25, 0.5, 0.75}
    float y = step(0.5, texUV.y);          // {0, 1}
    return (x + y) * 3.14159;
}

/* Compute an occlusion factor [0, 1] based on the vertex color. 0.0
 * means completely occluded, and 1.0 means not occluded at all. This
 * works because the game appears to encode an ambient occlusion in
 * the vertex color unless SEASONS is defined.
 */
float occlusionFactor(vec3 color) {
    const float shadowBorder = 0.87;
    const float shadowBlurLo = 0.05;
    const float shadowBlurHi = 0.01;

    /* Hackish adjustment for grass blocks! */
    float luminance = color.g * 2.0 - (color.r < color.b ? color.r : color.b);

    return smoothstep(shadowBorder - shadowBlurLo, shadowBorder + shadowBlurHi, luminance);
}

#endif /* NATURAL_MYSTIC_HACKS_H_INCLUDED */