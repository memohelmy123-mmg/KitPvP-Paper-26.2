# KitPvP for Paper 26.2

Targets **Paper 26.2** (`paper-api:26.2.build.124-stable`) and compiles with **Java 25**.
The plugin descriptor declares `api-version: '26.2'`.

## Build the jar

### Option A - on your PC
1. Install JDK 25 (e.g. Temurin 25) and Gradle 9.1.0 or newer.
2. In this folder run:  `gradle build`
3. Your plugin is `build/libs/KitPvP-1.0.0.jar` -> put it in the server's `plugins/` folder.

### Option B - no install (GitHub)
1. Create a free GitHub repo and upload this whole folder (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab -> "Build KitPvP jar" -> run it.
3. Download the `KitPvP-jar` artifact -> it contains the .jar.

### Option C - IntelliJ IDEA
Open the folder as a Gradle project, set the Project SDK to 25, run the `build` task.

## First setup
1. Start the server once, then stand at your spawn and run `/kitadmin setspawn`.
2. Edit `plugins/KitPvP/kits.yml` and `config.yml`, then `/kitadmin reload`.

## Commands
/kit [name], /spawn, /stats [player], /top [kills|deaths|streak|coins]
/kitadmin reload | setspawn | givecoins <player> <amt> | unlock <player> <kit>

## Permissions
kitpvp.use (default true), kitpvp.admin (op), kitpvp.build (op), kitpvp.kit.<id> (grant a kit), kitpvp.kit.vip (VIP kit)
