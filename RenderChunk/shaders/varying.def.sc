vec4 a_color0    : COLOR0;
vec3 a_position  : POSITION;
vec2 a_texcoord0 : TEXCOORD0;
vec2 a_texcoord1 : TEXCOORD1;

vec4 i_data0 : TEXCOORD7;
vec4 i_data1 : TEXCOORD6;
vec4 i_data2 : TEXCOORD5;

vec4 v_color        : COLOR0;
vec4 v_fog          : COLOR2;
centroid vec2 v_uv0 : TEXCOORD0;
vec2 v_uv1          : TEXCOORD1;
vec3 wPos           : wPos;
vec3 cPos           : cPos;
float cameraDist    : cameraDist;
vec3 vNormal        : vNormal;
float flickerFactor : flickerFactor;
float desatFactor   : desatFactor;
float clearWeather  : clearWeather;
float waterFlag     : waterFlag;
float waterPlane    : waterPlane;
vec4 cloudColor     : cloudColor;