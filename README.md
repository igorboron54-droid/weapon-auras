# Weapon Auras - Fabric, Minecraft 26.3 (Java 25, Mojang names)

Client-side mod: particle auras on enchanted weapons (held + dropped). Press K for the aura menu.

## Build without installing anything (GitHub)
1. Create a free GitHub repository and upload every file in this folder
   (the hidden `.github/workflows/build.yml` file must be included - see below).
2. Open the repo's **Actions** tab; the "Build mod" run starts automatically.
3. When it turns green, open the run and download **weapon-auras-jar** under Artifacts. Unzip it -> the .jar.

If `.github` didn't upload: in the repo click Add file > Create new file, type
`.github/workflows/build.yml` as the name, and paste the contents of that file.

## Install
Put the jar plus Fabric API 0.165.0+26.3 in your mods folder, with Fabric Loader 0.19.5+ and Java 25.

## Build locally
Needs JDK 25 + Gradle 9.6.0:  gradle build   ->  build/libs/weapon-auras-1.0.0.jar
