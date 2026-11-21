# Reverse engineering

Learning projects. I pull the circuit boards out of old, dead or half-broken devices and try to read the firmware off their chips with an Arduino, a BeagleBone or a cheap flash programmer. Each board gets its own folder with its own README, docs, photos, code and captures.

## Projects

| Folder | Device | Chip | Debug host | Status |
|---|---|---|---|---|
| [braun-shaver](braun-shaver) | Braun shaver charger PCB | Infineon PSoC 4125 | Arduino Nano clone, BeagleBone Black | SWD acquire works; memory reads fault because the chip is protected |
| [earbuds-rda5836](earbuds-rda5836) | Bluetooth earbuds board | RDA5836Q (RDA58xx family) | Arduino Nano, then BeagleBone Black | UART traffic captured; firmware not read yet |
| [sylvania-sp294](sylvania-sp294) | Sylvania SP294 Bluetooth speaker | CW6639M module, BergMicro 25Q40 SPI flash (U10) | CH341A programmer on a BeagleBone Black | U10 fully dumped and mapped (data, not code); voice prompts extracted |

Start with each project's README. Detailed notes, research and logs are in its `docs/` folder.

## Layout

Every project follows roughly the same shape:

- `README.md` covers the board, wiring, current status and what to try next.
- `docs/` holds research notes, datasheets, diagrams and photos.
- `arduino/`, `beaglebone/` or `ghidra/` holds sketches, firmware and helper scripts for that host.
- `captures/`, `dumps/` and `extracted/` hold raw data read off the board and anything decoded from it.
