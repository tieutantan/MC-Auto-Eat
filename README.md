
# AutoEat (NeoForge 1.21.1)

AutoEat is an automatic eating mod for Minecraft 1.21.1 (NeoForge). It does not require holding food in hand and does not require right-clicking.

## Features

- Auto-eats only when hunger is below 50% (< 10/20).
- No separate stop-threshold logic; each check only decides based on current hunger.
- Eats exactly 1 item every 10 ticks while hunger is below 50%.
- Prioritizes inventory order from top to bottom (lower slot index first).
- Correctly decreases consumed food item count.
- After each eat action, sends 2 chat lines:
- Line 1: remaining count in the consumed stack.
- Line 2: one random message from `src/main/resources/autoeat_messages.txt` (500 messages).

## How It Works

- The mod runs on server tick (`PlayerTickEvent.Post`).
- Every 10 ticks, if hunger is below 50%, the mod scans inventory and consumes 1 food item.
- Chat messages are loaded once at class initialization and cached in memory (no file reads on each eat).
- If the message file is missing or empty, the mod falls back to a default message to avoid crashes.

## Installation

1. Install NeoForge for Minecraft `1.21.1`.
2. Build mod:

```bash
./gradlew build
```

3. Lay file jar trong thu muc `build/libs/`.
4. Copy the generated JAR into your Minecraft `mods` folder (NeoForge 1.21.1).

## Development Run

```bash
./gradlew runClient
```

## Configuration

The current version does not use a config file. All behavior is fixed and automatic by default.

## Relevant Files

- Auto-eat logic: `src/main/java/com/tantn/autoeat/AutoEatEvents.java`
- Random messages: `src/main/resources/autoeat_messages.txt`

## Notes

- Some template example assets (example block/item) may produce model warnings in dev runs, but they do not affect AutoEat features.
