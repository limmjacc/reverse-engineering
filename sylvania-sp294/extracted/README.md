# Extracted data from U10 (2026-11-11)

Everything here was pulled out of [`../dumps/sylvania_u10_dump.bin`](../dumps/sylvania_u10_dump.bin).
Full writeup and reasoning: [`../docs/dump-analysis.md`](../docs/dump-analysis.md).
This file just describes what's in each output.

## `audio/`

The one part of U10 that's raw, uncompressed 16-bit signed PCM rather
than the proprietary codec used everywhere else -- no decoding needed,
just a WAV header.

- `voice_prompt_tone_0x049000-0x05B000_4000hz.wav` -- confirmed by ear
  (2026-11-11) to sound roughly correct at 4000Hz: a tone/beep sequence
  with a muffled, cartoon-ish voice underneath, possibly counting.
  4000Hz is a very plausible native rate for this class of chip but
  the slight muffle suggests it may not be exact -- worth nudging a
  little either direction if you want to sharpen it up further.
- `osc_region_0x05A000-0x05B000_4000hz.wav` -- a shorter, separate
  oscillating region right after the tone above, same rate for
  consistency.

## `clips_raw/`

The 23 voice-prompt clips pointed to by the index table at `0x030000` in
the dump, extracted by their true byte range (offset to the next clip's
offset -- not the table's own "length" field, which turned out to be a
256-byte block count rather than a byte count; see dump-analysis.md).

Named `clip_NN_off0x<offset>_gap<bytes>b_sr<rate>.bin`.

**These do not play as audio.** Every clip measures 7.95-7.99 bits/byte
entropy -- genuinely compressed, not raw/lightly-encoded PCM. Codec
identification was attempted and came up empty (see dump-analysis.md's
"codec dig" section for exactly what was ruled out: no fixed-position
header, CVSD decode produces unbounded noise rather than a smooth
waveform, no short repeating-key XOR obfuscation). They're kept here,
isolated by exact byte range, in case a real codec lead turns up later.
