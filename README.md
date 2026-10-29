# Natural Mystic PLUS

**Natural Mystic Plus** is an updated version of [PHO](https://github.com/depressed-pho)’s [Natural Mystic Shaders](https://github.com/depressed-pho/natural-mystic-shaders) for Minecraft bedrock edition.

## Changes from the original project:

* Updated for the latest version (1.26.60+)
* Added cloud reflection for water
* Better detection hacks

## Supported platforms
* Windows (Direct3D Shader Model 4.0+)
* Android (OpenGL ES 3.1+)
* iOS (Untested & Unrecommended)

## Screenshots

![Screenshot of a hill](./img/hill.jpeg)
![Screenshot of a lake](./img/lake.jpeg)
![Screenshot of a village at night](./img/night.jpeg)

## Installations
Install these tools depending on your device:

* Windows: [BetterRenderDragon](https://github.com/QYCottage/BetterRenderDragon) or [Wyvern Loader](https://github.com/mcbegamerxx954/wyvern_releases)
* Android: [MB Loader](https://play.google.com/store/apps/details?id=io.github.bambosan.mbloader)
* iOS: [Hynis](https://github.com/mcpebd/ipa-wo-hynis-jutsu)

And now, your game is able to load shaders from resource packs.
See [releases](https://github.com/Rinloid/natural-mystic-plus/releases) to download the .mcpack and apply it!

## Building
Install [Lazurite](https://github.com/veka0/lazurite) and follow its documentation to build.
You also need uniform overrides for the base vanilla materials which is also described on the documentation: add `BUILTIN_MinecraftCameraPosition` as `MinecraftCameraPosition` and `BUILTIN_MinecraftTimeOfDay` as `MinecraftTimeOfDay`.

## License
[CC0](https://creativecommons.org/share-your-work/public-domain/cc0/)
“No Rights Reserved”