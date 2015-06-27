ForgeRelocation
==========
An API for handling the movement of blocks.
- [![Build Status](https://travis-ci.org/MrTJP/ForgeRelocation.svg)](https://travis-ci.org/MrTJP/ForgeRelocation)
- [Minecraft Forum Thread](http://www.minecraftforum.net/topic/1885652-)
- [Website](http://projectredwiki.com)

Info
-
The ForgeRelocation API contains utility classes that can easily help move blocks.  Operation
of the API is very simple.  Just specify the blocks, the direction, and the speed.  The
rest will be taken care of.


The MCFrames mod (included) contains a robust and highly configurable implementation of
frames.  It also contains an API that can quickly resolve frame sticks.
An example implementation of a motor block is included as well.

Usage
-
The best way to use the API is to simply link the dev jar as a dependency in your IDE.

However, both ForgeRelocation and MCFrames contains a self-contained API package.  If needed, they can be used,
but it is recommended that they are not included. They should be used as a soft dependency.


Development
-
If you would like to contribute to the API, a simple `./gradlew setupDecompWorkspace` followed by a `./gradlew idea` or
`./gradlew eclipse` should set up the entire dev environment.


*This mod is not affiliated with Minecraft Forge.*