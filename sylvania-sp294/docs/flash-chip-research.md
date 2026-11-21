# U10 flash chip research: BergMicro 25Q40ASTCG

**Update (2026-11-11): chip was successfully read.** Actual JEDEC RDID
response confirmed by two independent tools: `E0 40 13` (manufacturer
`0xE0`, memory type `0x40`, capacity `0x13` -- the latter two matching the
GD25Q40-family prediction below exactly; the manufacturer byte does not
match GigaDevice's `0xC8` or any ID in either tool's database, consistent
with "BergMicro" being a genuinely obscure, unlisted vendor rather than a
simple GigaDevice relabel). See
[dump-analysis.md](dump-analysis.md) for the full read process and result.
The voltage/pinout predictions below held up -- 3.3V stock clip setup was
correct, no damage, full capacity read successfully.

## Identity

"BergMicro" (also written "Berg Micro") is a real, if obscure, brand --
confirmed present in the Elnec device-programmer database as "BG25Q40A"
(SOP8-150mil / SOP8-208mil). No public datasheet or company site was
found for BergMicro specifically. The part number `25Q40ASTCG` follows the
industry-wide "25Q40" naming convention shared by GigaDevice (GD25Q40),
Winbond (W25Q40), Boya/BOYAMICRO (BY25Q40), ESMT (EN25Q40A), and others --
all pin-compatible, command-compatible clones of the original Winbond
W25Q-series SPI NOR design. BergMicro's own silicon origin is not
confirmed, but electrically this should behave as a member of this family.

## SOIC-8 pinout

Industry-standard SPI NOR pinout, consistent across every vendor in this
family. Top view, **pin 1 marked by the dot/notch at the package corner**,
numbered counter-clockwise from the dot:

| Pin | Function              |
|-----|------------------------|
| 1   | CS# (Chip Select, in)  |
| 2   | SO / DO (also IO1)     |
| 3   | WP# (also IO2)         |
| 4   | VSS / GND              |
| 5   | SI / DI (also IO0)     |
| 6   | SCLK                   |
| 7   | HOLD# (also IO3)       |
| 8   | VCC                    |

## Voltage

**2.7V-3.6V, nominal 3.3V.** Use the CH341A kit's stock 3.3V SOIC-8 clip
setup, **not** the 1.8V adapter. Every pin-compatible part in this family
(Winbond/GigaDevice/Boya/ESMT) is 3.3V-class; a low-voltage variant would
carry a distinct part-number suffix, which this one does not have.
Recommended: verify with a multimeter that the board's own rail feeding
this chip reads ~3.3V before clipping, as final confirmation.

## Capacity and expected JEDEC ID

4Mbit (512KB). For a GD25Q40C-class part, RDID (`9Fh`) should return:
- Manufacturer ID: `C8h` (GigaDevice)
- Memory Type: `40h`
- Capacity: `13h`

If the chip reads back `C8 40 13`, it's electrically a GD25Q40C-compatible
part regardless of the BergMicro branding. A genuine BergMicro-specific ID
byte was not confirmed from public sources -- if the programmer software
reports something other than `C8h`, that's expected and not necessarily a
fault; check it against the software's own chip database rather than
assuming an error.

## Tool compatibility

Universally supported by `flashrom` and CH341A-based tools (ASProgrammer,
NeoProgrammer, flashrom's `ch341a_spi` programmer). Known quirks:

- Status register Block-Protect bits (BP0-BP4) may be set from the
  factory/OEM, which would block erase/write -- irrelevant for a
  **read-only** dump, which is the only operation planned here.
- WP# and HOLD# must not be left floating. Most SOIC-8 clip adapter boards
  tie these high by design -- worth a quick continuity check on the clip
  adapter if available, rather than assuming.
- If software doesn't auto-detect under a "BergMicro" profile (unlikely to
  exist), manually select the GD25Q40 / W25Q40 profile -- same pinout,
  same command set.

## Sources

- [GigaDevice GD25Q40C datasheet (DS-18076)](https://cdn.sparkfun.com/assets/9/c/2/2/d/DS-18076-4Mb_Flash_memory_-_GD25Q40CTIGR.pdf)
- [Elnec device list: Berg Micro BG25Q40A](https://www.elnec.com/en/search/device-list/27/berg%20micro)
- [DatasheetCafe 25Q80ASSIG page listing "Berg Micro, Eon Silicon Solution, Winbond"](https://www.datasheetcafe.com/25q80assig-datasheet-flash-memory/)

## Plan before first read

1. Confirm board's own 3.3V rail at U10 with a multimeter (final sanity
   check beyond the family-wide voltage convention above).
2. Disconnect the speaker's own power (no battery, not plugged in) so the
   CH341A is the only thing powering the flash chip during the read --
   avoids bus contention with U9 or anything else sharing the SPI lines.
3. Clip onto U10 with the dot/notch (pin 1 = CS#) aligned correctly against
   the clip's own pin-1 marking.
4. Read the JEDEC ID first (no erase/write) and confirm it against the
   `C8 40 13` expectation before attempting a full dump.
5. Full read-only dump once ID is confirmed.
