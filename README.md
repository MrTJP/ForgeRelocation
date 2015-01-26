Forge Relocation
==========
Forge Relocation is a mod created to handle the movement of blocks.  The aim is to allow developers to use the API toimplement ways to move their own blocks instead of having one mod handle them all in a very hacky and non-reliable manner.
- [![Build Status](https://travis-ci.org/MrTJP/ForgeRelocation.svg)](https://travis-ci.org/MrTJP/ForgeRelocation)
- [Minecraft Forum Thread](http://www.minecraftforum.net/topic/1885652-)
- [Website](http://projectredwiki.com)

Note:
----------
Forge Relocation is currently in alpha stages of development.  Although it is fairly stable, the safety of your world is not guaranteed.

*This mod is not affiliated with Minecraft Forge.*


Developing:
----------
Setup is slightly different depending on what system and IDE you use.
This assumes you know how to run gradle commands on your system.
The base command, `./gradlew` being used below is for Linux or Unix based systems. For windows, this would simply change to `gradlew`.
Of course, if you dont need to use the wrapper (as in, you have gradle installed on your system), you can simply go right to `gradle`.


1. Clone repository to empty folder.
2. Cd to the repository (folder where `src` and `resources` are located).
3. Run `./gradlew setupDecompWorkspace` to set up an environment.
4. Run `./gradlew eclipse` or `./gradlew idea` appropriately.
5. Open your IDE using the generated files (i.e., for IDEA, a ProjectRed.ipr is generated in `./`)
6. Edit, run, and debug your new code.
7. Once its bug free and working, you may submit it as a PR to the main repo.