# OpponentArmor

Client-side Fabric mod for Minecraft 1.21.11. Hit a player and their armor durability
appears in a corner of your screen for a few seconds.

- Percent, durability, or durability/max display
- Pick which pieces to show (helmet / chestplate / leggings / boots)
- How long it stays after your last hit (1–30 s), optionally also while aiming at a player
- Size, column or row layout, corner, offsets, corner radius, background opacity, text shadow
- Durability bars and red→green coloring
- Settings through Mod Menu (with a live preview), or bind *Open settings* under Controls

Note: servers that hide item durability of other players (e.g. Paper's item obfuscation)
will make every piece read 100%.

## Build
```
./gradlew build   # → build/libs/opponentarmor-<version>.jar
```
