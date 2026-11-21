# UART capture findings (2026-11-09)

## Setup

- Host: BeagleBone Black, UART4 (`P9_11` = RXD, `P9_13` = TXD), `/dev/ttyS4`,
  115200 8N1. Native 3.3V logic, matching the chip's measured pad voltage --
  no level shifting.
- Wiring: chip TX pad -> `P9_11`, chip RX pad -> `P9_13` (confirmed crossed
  correctly after an earlier attempt with TX/RX reversed produced nothing
  but sparse `0x00` bytes).
- Board powered via microUSB. Continuously cycling its red/blue LED
  pattern on its own during capture, with no button presses.

## Raw captures

- [../captures/uart4_capture_20261109_1.bin](../captures/uart4_capture_20261109_1.bin)
  -- 62 bytes, one frame.
- [../captures/uart4_capture_20261109_2.bin](../captures/uart4_capture_20261109_2.bin)
  -- 124 bytes, two back-to-back frames.

## Frame structure (preliminary -- only 3 samples so far)

Each frame ends with the same 3-byte sequence `94 25 fe`, followed by
zero padding until the next frame (or end of capture). Working hypothesis:
`94 25 fe` is a fixed trailer/terminator, possibly including a checksum
byte, though with only 3 samples this hasn't been verified against a
candidate checksum algorithm yet.

Frame 1 (from `uart4_capture_20261109_1.bin`):
```
00 b5 00 b5 05 15 a1 81 a0 81 a0 81 a0 a0 81 81
a0 a0 a0 81 81 81 81 a0 81 81 a0 a0 a0 81 a0 a0
a0 81 a0 81 a0 a0 a0 81 a0 81 a0 a0 a0 81 81 81
a0 a0 a0 94 25 fe
```

Frame 2 and 3 (from `uart4_capture_20261109_2.bin`, back-to-back):
```
# Frame 2
00 b5 00 b5 05 05 a0 81 a0 81 a0 81 a0 a0 a0 81
81 a1 a0 a0 81 81 81 81 a0 81 81 a0 a0 a0 81 a0
a0 a0 81 a0 a0 81 a0 a0 a0 81 a0 81 a0 a0 a0 81
81 81 a0 a0 a0 94 25 fe

# Frame 3 (immediately follows, same capture)
00 c9 05 15 81 05 05 05 05 15 81 05 05 15 81 a0
a0 a0 81 81 81 81 a0 81 81 a0 a0 a0 81 a0 a0 a0
81 a0 81 a0 a1 a0 81 a0 81 a0 a0 a0 81 81 81 81
a0 a0 a0 94 25 fe
```

Observations:
- All three frames are the same length (54 bytes before the `94 25 fe`
  trailer) and end identically.
- The body is almost entirely two byte values, `0x81` and `0xa0`
  (occasionally `0xa1`) -- looks like it could be a bit-per-byte encoding
  of a much shorter underlying payload (e.g. each bit of a real message
  represented as one of two byte values), common in some simple
  serial/IR-style protocols, but this is a guess, not confirmed.
- Frames 1 and 2 are byte-for-byte identical (both start
  `00 b5 00 b5 05 ...`). Frame 3 differs starting at byte 2
  (`00 c9 05 15 ...` instead of `00 b5 05 05 ...`), while still matching
  frames 1/2 for most of the body and the full trailer.
- This is consistent with either: (a) a changing field very early in the
  frame (sequence number, timestamp, status flags) with the rest of the
  frame being constant/templated content, or (b) frame 3 being a
  genuinely different message type that happens to share most of its
  body with frames 1/2. Not enough samples yet to tell apart.

## Next steps

- Capture many more frames (a few minutes continuous) to build a real
  sample set before guessing at field boundaries.
- Try to correlate frame content with physical triggers (button presses,
  specific points in the red/blue LED cycle) by timestamping captures
  against observed LED state.
- Consider writing a small parser that splits a raw capture stream into
  frames on the `94 25 fe` trailer and diffs them byte-by-byte, rather
  than doing this by hand each time.
- Still unverified: whether `0x94 0x25 0xfe` is a real fixed trailer or
  partially a checksum that happens to repeat due to constant frame
  content in these first 3 samples.
