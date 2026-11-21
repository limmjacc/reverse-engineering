# Sylvania SP294 Bluetooth speaker reverse engineering

Scratch project to see whether firmware can be read off a Sylvania Bluetooth
speaker board.

## Device

- Sylvania Bluetooth Wireless Speaker, model `SP294`.
- FCC ID `SSMMET1210`. Imported by Curtis International Ltd. Made in China.
- Simple On/Off/Pair slide switch, no other external controls visible on
  the housing.
- Photos: [photos/](photos/) (converted from HEIC originals).

## Board -- identified components

**Bluetooth module** (castellated SMD module soldered to the main board,
silkscreen `CMD2026_V05`):
- Main chip marked `CW6639M / 14230-501 / ASTH414005.1`, stylized "X" logo.
- 26.000MHz crystal.
- Small 5-pin chip marked `DE=A1D` (likely a regulator or support IC,
  unconfirmed).

**Main carrier board:**
- **U9** -- chip with the same stylized "X" logo as the module's main chip,
  marked `B67HK05821 1411TRZ`. Relationship to the module chip (same
  vendor/family vs. coincidence) not yet confirmed.
- **U10** -- `BergMicro 25Q40ASTCG`, SOIC-8, external SPI NOR flash
  (4Mbit-class part, GD25Q40-style naming). This is a separate, standalone
  flash chip -- not embedded in a locked MCU -- which makes it a much more
  tractable firmware-extraction target than the [earbuds
  project](../earbuds-rda5836)'s on-die flash. See
  [docs/flash-chip-research.md](docs/flash-chip-research.md) for pinout/
  voltage research before clipping anything on.
- **AX8302** (marked `AX8302 BRG1Y`) -- likely a class-D audio amplifier
  driving the speaker.
- `DP`/`DM` labeled traces near U9 -- standard USB D+/D- signal names,
  confirming USB is routed into this chip (charging and/or firmware
  update/mass-storage path, unconfirmed).
- `SP+`/`SP-` labeled pads -- speaker output, as expected.

## Tooling

- CH341A USB programmer kit (SOIC-8 test clip + cable, SOP8 ZIF adapter,
  3.3V stock setup + separate 1.8V adapter board). Used via a BeagleBone
  Black's USB host port rather than directly on the Mac -- modern macOS's
  USB-access entitlement lockdown blocks `flashrom`'s libusb access even
  as root, which is not an issue on Linux.
- `ch341prog` (built from source on the BBB) -- used instead of `flashrom`,
  which refused to read this chip over an unrecognized JEDEC manufacturer
  ID. See [docs/dump-analysis.md](docs/dump-analysis.md) for the full
  story.

## Status (2026-11-11): U10 fully mapped -- it's data, not code

**Read the full 512KB contents of U10 non-destructively, clip-on, chip
still soldered to the board.** Dump file:
[dumps/sylvania_u10_dump.bin](dumps/sylvania_u10_dump.bin) (MD5
`5b7cb9de763e63687ab3ece7b5f22d4a`, verified byte-identical across two
independent checksum computations).

Fully reverse engineered the dump's structure -- it contains **no
executable code**. It's a data partition: 23 compressed voice-prompt
clips + their index table (codec unidentified -- see below), one
raw-PCM tone/chime region (extracted, listenable, confirmed ~4000Hz),
a settings area with named sub-records and standard flash
wear-leveling, and a 4-slot Bluetooth pairing table (all 4 slots your
own devices). U9 (the chip that actually reads this flash) most likely
runs its own Bluetooth stack from internal ROM and only uses U10 for
this kind of data. Ghidra decompilation was attempted first and came
up empty under both candidate architectures tried, which is what
prompted the deeper structural dig that explains why.

Full writeup, including the bring-up process, the structural map, and a
dead-end codec investigation (ruled out: fixed-header ADPCM-style
framing, CVSD, short-key XOR), is in
[docs/dump-analysis.md](docs/dump-analysis.md) and
[docs/ghidra-analysis.md](docs/ghidra-analysis.md). Extracted
audio/clips are in [extracted/](extracted/README.md).

**Open next steps:**
- U9 identification remains unconfirmed -- marking `B67HK05821
  1411TRZ`, same stylized "X" logo as the BT module's main chip
  (`CW6639M`, likely Conwise Technology Corporation family). Settling
  this would also unblock the audio codec question above.
- USB enumeration via U9's DP/DM traces -- in progress now that the
  board's micro-USB data lines (previously split/disconnected) have
  been re-soldered and connected to the BeagleBone Black.
