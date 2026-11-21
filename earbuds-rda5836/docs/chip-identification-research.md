# Chip identification research (2026-11-09)

## Question

Is there a public datasheet or debug-interface (SWD/JTAG) pinout for the
`5836Q40` / `DBC2134748`-marked chip on this board, so a pinout document
similar to the Infineon-datasheet-backed one in
[../braun-shaver](../braun-shaver) could be
built?

## Findings

**No public datasheet exists for RDA5836 specifically.** Direct searches
return nothing from RDA Microelectronics or Unisoc -- no datasheet,
distributor listing, or teardown. The only near-match is the unrelated
Qorvo/RFMD "RF5836" Wi-Fi front-end module. Treat `5836Q40` as a
marking/date-code convention on an obscure or relabeled die -- common on
cheap earbuds boards -- not a confirmed, documented catalog part.

**Closest documented siblings (do not assume uniformity across the family):**

- **RDA5876 / RDA5850 / RDA5851S** (Bluetooth 2.1+EDR + FM tuner single-chip
  audio SoCs, 32-pin QFN): datasheet mirrors describe an **ARM7-based** core.
  - https://www.scribd.com/document/448347465/RDA5876-RDA
  - https://static6.arrow.com/aropdfconversion/b522d5086a8a08e5de839b7fc645080260a814d1/132011120709025216.pdf
- **RDA5981** (Wi-Fi IoT SoC, different market segment, now marketed by
  Unisoc): confirmed **ARM Cortex-M4**, up to 160 MHz.
  - https://www.cnx-software.com/2018/01/11/1-rda5981-wifi-iot-soc-is-designed-for-smart-home-devices-smart-speakers/
  - https://www.electrodragon.com/w/images/c/cf/Rda5981_datasheet.pdf
- A happytrees.org chip-database entry for "RDA5851SX" lists an "XCPU RISC"
  core at 312 MHz in TFBGA-203 -- inconsistent with the RDA5851S audio chip
  and likely a database mix-up with an unrelated part. Not trusted.

Given this chip is on an earbuds board (not Wi-Fi/IoT), ARM7 is the more
plausible core for an RDA58xx *audio* part by analogy to RDA5876/5850/5851S
-- but this is inference from siblings, not a confirmed fact about RDA5836
itself.

**No documented debug/programming interface for any RDA58xx part.** No
datasheet mirror found mentions JTAG, SWD, or ICE pins. A related chip
(RDA5869, BT headset) only documents UART and I2C as general interfaces,
suggesting a UART-based serial bootloader/ISP protocol is plausible for
firmware loading on this family (a common pattern for Chinese BT audio
SoCs: hold a pin low at reset to enter a UART download mode) -- but no
confirmed pinout for this was found on RDA5836, RDA5876, or any sibling.

**No community reverse-engineering work found** specifically targeting
RDA5836, RDA5876, or RDA58xx earbuds firmware extraction, across forums,
GitHub, and teardown sites searched.

## Implication for this project

Unlike the PSoC 4100S in the shaver project (audited against an official
Infineon datasheet pin table), there is nothing to audit a pinout against
here. Any pinout for this chip would have to come from physical inspection
and inference instead of documentation:

- Package pin count and pitch (physical inspection/measurement)
- Continuity tracing from the known external pads (`TX`, `RX`, `G1`, `G2`,
  `B+`, `B-`, `R+`, `R-`, possible `L+`) back to specific package pins
- Comparison against the *loosely similar* documented siblings above, with
  the ARM7-vs-Cortex-M4 core uncertainty explicitly unresolved

Any such pinout would need to be clearly labeled as inferred/unverified,
not datasheet-sourced, before using it to decide where to solder a debug
probe. Given there is also no confirmed debug-pin precedent anywhere in
this chip family, a from-scratch physical probing approach (package
inspection + continuity map + reset-strap behavior testing) carries real
risk to a one-off board with no fallback reference to check against.

## Open decision

Whether to invest effort in physical pin-probing/inference work given this
lack of precedent, versus focusing further effort on the UART protocol
already being explored (see [uart-findings.md](uart-findings.md)) or
revisiting the still-unresolved USB enumeration question.
