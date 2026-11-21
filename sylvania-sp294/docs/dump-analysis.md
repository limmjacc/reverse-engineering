# U10 flash dump: bring-up and analysis (2026-11-11)

## Result

Successfully read the full contents of U10 (`BergMicro 25Q40ASTCG`) via a
CH341A + SOIC-8 clip, non-destructively, with the chip still soldered to
the board.

- File: [../dumps/sylvania_u10_dump.bin](../dumps/sylvania_u10_dump.bin)
- Size: 524,288 bytes (512KB / 4Mbit), matching the chip's expected capacity
  exactly.
- MD5: `5b7cb9de763e63687ab3ece7b5f22d4a`
- Verified byte-identical in two independent ways: (1) md5sum computed
  separately on the BeagleBone Black (where the read was performed) and on
  the Mac (after transfer) matched exactly; (2) the zmodem transfer
  protocol used for the transfer itself has its own CRC-based integrity
  checking.

## How it was actually read

`flashrom`'s `ch341a_spi` programmer reported "unknown SPI chip (RDID)"
and refused to read -- its chip database has no entry for the JEDEC ID
this chip actually returns (see below). Forcing a specific chip profile
(`-c "GD25Q40(B)" -f`) did not bypass this: `-c` only narrows which single
chip's probe routine is attempted, it does not disable the ID-match
requirement, and `-f` only affects write/erase confirmation, not probing.

Switched to `ch341prog` (https://github.com/setarcos/ch341prog), a
lighter-weight CH341A tool that doesn't gate reads behind a chip-ID
database match. Not available via apt; built from source on the
BeagleBone Black (`gcc -o ch341prog main.c ch341a.c -I/usr/include/libusb-1.0
-lusb-1.0`, after installing `libusb-1.0-0-dev`). Source files were
transferred from the Mac to the BBB manually over the serial console
(base64, chunked, each file's md5sum verified byte-identical before use) --
GitHub itself was unreachable through the macOS Internet Sharing NAT setup
used for this project's network access (a recurring, host-specific
blocking pattern also seen with a couple of other non-Debian-mirror hosts
earlier in this project; `deb.debian.org`/`security.debian.org` worked
throughout).

Read command: `sudo ./ch341prog -l 524288 -r sylvania_u10_dump.bin`
(capacity specified manually since auto-detection from the unrecognized
JEDEC ID failed).

The finished 512KB dump file was transferred from the BBB back to the Mac
via `zmodem` (`lrzsz`'s `sz`/`rz`) over the same serial console link, since
it was too large for the chunked-base64-over-serial method used for the
small source files, and direct network transfer (scp/ssh) was not
available (the BBB could reach the public internet through the Mac's
Internet Sharing NAT, but the Mac could not reach the BBB's own IP in the
reverse direction over that same link, for reasons not fully diagnosed).

## The JEDEC ID mismatch, resolved

Two independent tools (`flashrom` and `ch341prog`) converged on the same
raw 3-byte RDID response, just parsed at different byte offsets by each
tool's own code: **`E0 40 13`**.

- `0x40` / `0x13` (memory type / capacity) match the GD25Q40-family
  expectation from [flash-chip-research.md](flash-chip-research.md)
  exactly.
- `0xE0` (manufacturer ID) does not match GigaDevice's `0xC8`, and isn't
  one of the manufacturer IDs either tool's database recognizes.

Two tools independently reading the identical, stable byte sequence --
reproducible across a physical reseat of the clip and a full
disconnect/reconnect of the whole programmer -- rules out a flaky
connection or wiring fault. The straightforward read: "BergMicro" is
genuinely an obscure-enough vendor that its real JEDEC manufacturer ID
isn't in either tool's (dated) chip database, despite the chip being
otherwise fully pin/command-compatible with the GD25Q40 family (as
`-l 524288` plus a successful full-capacity read confirms).

## Content analysis (preliminary)

- `binwalk` found zero recognized file signatures (no filesystem headers,
  no known compression magic, no recognized executable headers).
- No meaningful ASCII strings (`strings -n 8`) -- consistent with
  minimal embedded text, not with a text-containing filesystem image.
- Shannon entropy: **6.32 bits/byte** (max 8.0) -- noticeably below what
  genuinely encrypted or well-compressed data would show (typically
  7.9-8.0, flat distribution). Combined with a heavy skew toward `0xFF`
  (~25% of all bytes) and `0x00` (~11%), this is much more consistent with
  **real firmware containing padding/erased regions** than with an
  encrypted or corrupted blob.
- Layout: real (non-0xFF) content occupies roughly the first 374KB
  (`0x000000`-`0x05d800`), followed by a 108KB contiguous erased block
  (`0x064800`-`0x07f100`, 108,544 bytes), a handful of smaller erased gaps,
  and 3,840 bytes of trailing erased padding to the end of the chip. This
  is the expected shape of a firmware image that doesn't use the chip's
  full capacity, not noise.
- No file signature match plus structured-but-unidentified content is
  consistent with U9 (the "X"-logo chip, marking `B67HK05821 1411TRZ`,
  identity not yet researched) using a proprietary, undocumented
  firmware/data format -- not surprising given U9's own identity is still
  unconfirmed.

## Resolved: U10 is a data partition, not code (2026-11-11)

Ghidra ([ghidra-analysis.md](ghidra-analysis.md)) couldn't find convincing
code structure under either 8051 or ARM Cortex-M, and the standard 8051
interrupt-vector jump opcodes were absent. Block-wise entropy analysis
(4KB windows across the whole file, rather than one whole-file number)
confirmed why, and mapped out exactly what is actually stored:

- **`0x000000`-`~0x02E000` (~184KB): compressed audio.** Entropy is
  consistently ~7.95 bits/byte (max 8.0) across this entire span --
  essentially flat, maximal-entropy data, the signature of compressed
  audio rather than code (code has strong structural repetition that
  keeps entropy well below this) or plain config values.
- **`0x030000`-`0x0302C0`: an index table.** 23 `(offset, length)` pairs
  of little-endian uint32s (e.g. `0x000800`/`0x2e`, `0x003600`/`0x26`,
  `0x005c00`/`0x2e`, ...), every offset pointing back into the audio blob
  above. This is a directory of voice-prompt clips -- 23 is a very
  ordinary number of prompts for a BT speaker (power on/off, pairing,
  connected, battery low, volume steps, etc).
- **`0x031000`-`~0x048000`: more of the same high-entropy audio-like
  data**, likely the remainder of the prompt set.
- **`0x049000`-`0x05A000`: structured numeric tables**, including a
  literal repeating `7F FF` / `00 80` pattern -- the little-endian
  encoding of `0x7FFF`/`0x8000`, the max-positive/max-negative values of
  16-bit signed PCM. Consistent with an audio gain/limiter table, not
  instructions.
- **`0x05B000`-`0x064FFF`: small repeating records inside otherwise
  zero-filled sectors.** Classic flash wear-leveling: a small settings
  struct gets written into a fresh page each time rather than
  erase-and-rewrite in place, leaving stale copies behind until the
  sector is reclaimed.
- **`0x07F000`: a literal plaintext header, `BTPAIREDINFOHEAD`**,
  followed by the Bluetooth pairing/bond table. Extracting printable
  strings from this region recovers real device names from past
  pairings (binary link-key data sits between each name).
- **`0x065000` onward (minus the pairing table at `0x07F000`): erased
  flash (`0xFF`)** -- unused capacity, matching the earlier whole-file
  padding finding.

This is a complete, non-code account for the dump: compressed voice
prompts + their index, a PCM-related table, wear-leveled settings, and
the pairing table, with the remainder erased. It also explains why
Ghidra came up empty under every architecture tried -- there's no
machine code on this chip to find. U9 (the chip that actually drives
U10) most likely runs its Bluetooth stack from internal ROM, consistent
with the Conwise on-chip-ROM pattern noted in
[ghidra-analysis.md](ghidra-analysis.md), and only reaches out to U10 for
data.

## Full structural map (exhaustive pass, 2026-11-11)

Went back over every region in detail. Extracted artifacts live in
[`../extracted/`](../extracted/README.md). Corrections and additions to
the picture above:

### Audio index table -- corrected format

The table at `0x030000` is **not** uniform `(u32 offset, u32 length)` --
that was a wrong first parse. The real per-entry layout is 8 bytes:

- bytes `[0:3]`: 24-bit little-endian offset into the audio region
- byte `[3]`: reserved, always `0x00`
- bytes `[4:6]`: 16-bit "length" field
- bytes `[6:8]`: 16-bit field -- `0` for entries 0-7, `8000` (i.e.
  8000 Hz) for entries 8-22. This is a sample-rate tag added partway
  through the table.

The "length" field is **not** a byte count -- it's a **256-byte block
count**. Multiplying it out: `declared_length x 256` lands exactly on the
byte span between one clip's offset and the next, for every one of the
23 entries (entries 0-7: `gap == length*256` exactly; entries 8-22:
`gap == (length+1)*256`, i.e. one extra 256-byte block, presumably a
per-clip header/setup block that goes with the added sample-rate tag).
This is a clean, exact relationship across all 23 entries, not a
coincidence -- the clips are built from fixed 256-byte frames, a useful
anchor for identifying the actual codec later.

Per-clip entropy (measured over the true gap-based byte range) is
7.95-7.99 bits/byte for **every single clip**, confirming these are
genuinely compressed (not raw or lightly-encoded PCM) -- so they won't
play back correctly just by wrapping them in a WAV header. All 23 clips
are extracted by true byte range in
[`../extracted/clips_raw/`](../extracted/clips_raw).

### The one region that *is* raw audio

`0x049000`-`~0x05B000` is the exception to all the above: entropy there
drops to the 4-6 bit/byte range with a narrow byte-value range, and the
raw bytes decode as smooth, slowly-varying 16-bit signed PCM samples
(including the `0x7FFF`/`0x8000` max-amplitude values noted earlier).
This was written out directly as WAV files (no decoding needed, since
it's already PCM) at several candidate sample rates. **Confirmed by ear
(2026-11-11): 4000Hz is roughly correct** -- a tone/beep sequence with a
muffled, cartoon-ish voice underneath, possibly counting. The slight
muffle suggests 4000Hz may not be the exact native rate, just close to
it. Kept as [`../extracted/audio/`](../extracted/README.md).

### Codec dig on the compressed clips (2026-11-11) -- dead end, documented

Tried to actually identify the codec behind `clips_raw/`'s 23 clips,
rather than leave it as an open guess. Three tests, all negative:

- **Per-byte-position entropy across all 760 frames (all 23 clips).**
  Real block-based codecs (IMA/DVI ADPCM, etc.) have a low-entropy
  header at a fixed offset within each frame (predictor seed, step
  index, a reserved byte that's almost always `0x00`). Computed entropy
  separately for each of the 256 byte-positions within the frame --
  every position sits in the same narrow 7.5-7.8 bit/byte band. No
  position stands out as a header field.
- **CVSD decode attempt.** CVSD (a 1-bit-per-sample delta codec, common
  in cheap embedded voice-prompt chips, and the best fit for the
  near-maximal entropy observed since real CVSD streams look close to
  random at the byte level) was decoded with a standard adaptive
  step-size integrator, both bit orders tried. Result: unbounded, noisy
  output -- 1.8% of samples slamming into the clamp ceiling, huge
  sample-to-sample jumps -- not the smooth, bounded waveform a real CVSD
  decode of speech produces. Rejected.
- **Repeating-key XOR test.** Computed entropy of `byte[i] XOR
  byte[i+k]` for every key length `k` from 1 to 32 bytes, across all 23
  clips concatenated. Flat at 7.999 for every single `k` -- no
  periodicity at any short key length, ruling out simple byte-wise
  obfuscation.

**Conclusion:** no fixed header, no CVSD structure, no short XOR key, and
entropy essentially at the theoretical maximum (8.0) everywhere tested.
That combination rules out the usual cheap embedded-codec suspects and
is actually more consistent with either real encryption or a properly
entropy-coded (Huffman/arithmetic) compressed bitstream than with a
simple voice-prompt codec. Making further progress here would need the
actual U9/vendor codec spec or a known-plaintext sample to work
backward from, not more blind guessing -- parking this as a dead end
unless new information about U9's identity turns up.

### Settings sectors, decoded

The wear-leveled region `0x05B000`-`0x064FFF` breaks down into distinct
named sub-blocks, not one uniform thing:

- `0x05C300`: a record literally tagged `"WAV\0"`.
- `0x05D000`: a record literally tagged `"BTINF\0"` (Bluetooth info).
- `0x05E000`-`0x05F000`: fully erased (`0xFF`) -- reserved, unused
  wear-leveling slots.
- `0x060000`-`0x064FFF`: a small (~36-byte) settings record containing a
  single changeable byte (seen values: `0x0F`, then `0x0D`, most
  recently `0x0C` -- likely a volume level or EQ preset index) plus what
  looks like a 4-byte checksum, rewritten into a fresh 256-byte slot
  every time the setting changes rather than erasing in place -- the
  textbook flash-wear-leveling pattern. Slot period confirmed exactly
  256 bytes by locating repeated checksum markers.
- A distinctive repeating `0x3F 0xBF 0xBF 0x3F` fill pattern appears at
  the tail of the `0x05C000` sector (after the `WAV` record) -- neither
  `0x00` nor `0xFF`, so likely a deliberate "slot state" sentinel used by
  the wear-leveling logic itself, not meaningful payload data.

### Pairing table, decoded

Fixed 56-byte (`0x38`) records starting at `0x07F010`, immediately after
the 16-byte `"BTPAIREDINFOHEAD"` header:

- bytes `[0:16]`: Bluetooth link key (16 bytes, standard BT link-key
  length)
- bytes `[16:19]`: 3 bytes, likely class-of-device or similar flags
- bytes `[19:~52]`: device name, null-padded
- trailing bytes: a `0x01` "slot valid" flag followed by what's
  consistent with a per-entry sequence number/checksum

4 of these records are populated (confirmed as your own previously-paired
phones), occupying `0x07F010`-`0x07F0EF`; everything from `0x07F0F0`
onward is erased (`0xFF`), i.e. up to 4 pairing slots total, all
currently used.

### Bottom line

Every non-erased byte in the dump is now accounted for: compressed
voice-prompt audio (23 clips, 256-byte-frame storage, codec identity
unresolved -- see the codec dig above) + its index table, one genuine
raw-PCM region (tone/chime, extracted, listenable, rate confirmed close
to 4000Hz), a settings area with named sub-records and standard
wear-leveling, and a 4-slot Bluetooth pairing table (all 4 slots your
own devices). Nothing in the dump is executable code, and U9 (the chip
that actually drives it) most likely runs its own Bluetooth stack from
internal ROM rather than from anything stored here.

This closes out the "what's on this flash chip" investigation -- it's
fully mapped, and the one unresolved piece (the audio codec) is a
documented dead end rather than an open guess.

## Open next steps

- Identify the actual audio codec used by the 256-byte-frame clips in
  `clips_raw/`, if a real lead on U9's exact part number or a vendor
  codec spec ever turns up -- not worth more blind guessing without one.
- U9's exact identity is still unconfirmed (inferred only from a shared
  package logo with the BT module's `CW6639M`); not required to explain
  U10's contents anymore, but would settle the "on-chip ROM" claim
  directly rather than by inference, and would unblock the codec
  question above.
- Consider whether the chip's manufacturer being genuinely unlisted
  anywhere public (not just under "BergMicro") is itself worth a targeted
  search, now that a confirmed real JEDEC ID (`E0 40 13`) is in hand
  rather than just the package marking.
