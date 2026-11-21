# Earbuds RDA5836Q firmware reverse engineering

Scratch project for reading the firmware off a Bluetooth earbuds board.

## Board

- Marking: `LHB019 E01-B V3.0R8`, date code `201803023`.
- Single IC, marked `5836Q40` above `DBC2134748` -- likely RDA Microelectronics
  (now Unisoc) RDA58xx-family Bluetooth audio SoC.
- Micro-USB connector, crystal (X1), battery pads `B+`/`B-` (red/black wires
  attached), speaker pads `R+`/`R-` (and a possible `L+`), three buttons
  (`V-`, `P/P`, `V+`) and two LEDs on the underside, PCB trace antenna.
- Pads labelled `TX`, `RX`, `G1`, `G2`. Traced (not yet multimeter-confirmed)
  as: `G1`/`G2` = USB D+/D- (not ground) -- so no adapter ground should be
  tied to them.
- USB does not currently enumerate on macOS (no new entry in `ioreg` /
  `system_profiler`, no serial port, no disk) when connected via the dock.
  Possible causes: charge-only cable, a data path only active in a boot/
  download mode, or firmware with no USB stack.

## Host setup

Mac (macOS) with a Kensington TB4 dock; behind it: Keychron Ultra-Link 8K,
Logitech C920, Realtek USB Ethernet, hubs, Chrontel USB-C to DP adapter.

Debug host: Arduino Nano (5V/ATmega328P logic), same 1k series / 2k-to-ground
resistor divider used for the shaver-board SWD lines in
[../braun-shaver](../braun-shaver) (5V ->
~3.33V), reserved for the Nano-TX -> chip-RX direction once bidirectional
work starts.

## Status (updated 2026-11-09)

- USB still does not enumerate on macOS in any configuration tried so far.
- Idle voltage on the `TX`/`RX` pads measured ~3.3V (pulses, not sustained --
  a DMM can't resolve real UART bit timing, so this only confirms logic
  level, not traffic). Confirmed 3.3V logic, consistent with the chip's
  pad voltage matching 3.3V I/O.
- Board was powered via microUSB; battery pads `B+`/`B-` read ~4.3V in that
  state, confirming an active onboard LiPo charge circuit drives that rail
  -- **do not connect an external supply (Nano/BBB 3V3) to `B+` while
  microUSB is also connected**, to avoid two sources fighting over that net.
- Switched the debug host from the Arduino Nano to the BeagleBone Black
  (see [../braun-shaver/beaglebone](../braun-shaver/beaglebone)
  for the board itself) specifically because its UART pins are native
  3.3V logic, matching the chip exactly -- no resistor divider/level
  shifting needed in either direction. Wired via its UART4 (`P9_11`=RXD,
  `P9_13`=TXD), device-tree overlay `BB-UART4-00A0.dtbo` enabled through
  `/boot/uEnv.txt`'s `uboot_overlay_addr4=`.
- **First real captured data** (2026-11-09): with wiring crossed correctly
  (chip TX -> BBB RX, chip RX -> BBB TX) and `/dev/ttyS4` at 115200 8N1,
  captured two back-to-back ~60-byte binary frames while the board was
  continuously cycling its red/blue LED pattern on its own (no button
  presses). See [captures/](captures/) for the raw files and
  [docs/uart-findings.md](docs/uart-findings.md) for the frame breakdown.
  This is binary protocol data, not ASCII text/logging.
- Earlier capture attempts (DMM-era wiring, and the first BBB attempts
  before the TX/RX cross was corrected) produced only sparse `0x00` bytes,
  consistent with intermittent/bouncy contact rather than real traffic --
  a useful negative data point, not a dead end.

## Arduino (superseded by BeagleBone for UART work, kept for reference)

[arduino/uart_sniff/uart_sniff.ino](arduino/uart_sniff/uart_sniff.ino) --
passive UART sniff sketch. Uses `SoftwareSerial` on D2/D3 (RX only wired for
now) so it doesn't collide with the Nano's own USB-serial bridge on D0/D1.
Echoes received bytes to the Mac over USB serial, printable ASCII as-is and
everything else as `[XX]` hex so no data is silently lost to the terminal.
Not actually used for the UART capture below -- superseded by the BBB once
the 5V/3.3V level-shifting question came up -- kept here in case the Nano
is needed again (e.g. as a second simultaneous listener).
