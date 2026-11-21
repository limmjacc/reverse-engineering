# Ghidra analysis attempt (2026-11-11)

## Setup

Ghidra 12.1.4 (Homebrew), driven via `analyzeHeadless` (not the `rea` MCP
tool, which requires its own setup + session restart that wasn't worth
the interruption given Ghidra itself was already installed). Needed
`openjdk@25` installed separately (Ghidra's declared dependency; the
system only had `openjdk@21`).

Attempted `pyghidra` (pip package) for easier scripting, but its `Jpype1`
dependency failed to build from source (missing `ant`, then a C++ stdlib
header error after installing `ant`) -- abandoned in favor of classic
Ghidra Java scripts (`GhidraScript` subclasses), which Ghidra compiles
on the fly with no extra toolchain needed.

## Architecture: unconfirmed, had to guess

U9 (the chip presumably reading this flash) has no public datasheet --
see the main project docs. Best lead: its package marking's logo visually
matches the BT module's `CW6639M` chip (user-confirmed side-by-side), and
research strongly suggests that chip is from **Conwise Technology
Corporation**, whose documented sibling parts (CW6626, CW6638M) all use an
embedded **8051** core. This is an inference from a shared logo, not a
confirmed part-number match for U9 itself.

Tried two candidates: `8051:BE:16:default` and `ARM:LE:32:Cortex`.

## Result: inconclusive-to-negative for both

- Raw binary import clips to the architecture's native address space --
  8051's 16-bit addressing meant only the first 64KB of the 512KB dump
  was even reachable for this test.
- Neither candidate's full auto-analysis found any code structure on its
  own (0 instructions) -- Ghidra's analyzers need a seed/entry point for
  a headerless raw binary.
- Manually seeding disassembly at address 0 (the 8051 reset-vector
  convention) produced a short, syntactically plausible 18-instruction
  run under 8051 before hitting a `RET` -- but an **identical** result
  came from re-running full auto-analysis afterward, meaning nothing else
  in the entire 64KB was discoverable as code. A real 8051 firmware image
  would be expected to show many more reachable subroutines.
- The standard 8051 interrupt vector offsets (`0x0003`, `0x000B`,
  `0x0013`, `0x001B`, `0x0023`, `0x002B`) do not contain the `LJMP`/`AJMP`
  opcodes (`0x02`/`0x01`) that essentially all real-world 8051 firmware
  uses at those fixed locations to dispatch to handler code. None of the
  6 checked offsets matched.
- ARM Cortex-M (Thumb) produced a similarly short, locally-plausible
  9-instruction run before a function epilogue (`pop {..,pc}`) -- same
  caveat: short decoded runs are not very diagnostic on their own, since
  most byte patterns decode as *some* valid instruction in most ISAs.

**Conclusion so far: neither architecture is confirmed, and the evidence
leans against straightforward 8051 machine code specifically** (the
missing vector-table jumps are the most concrete negative signal).

## Alternative hypothesis worth taking seriously

Combined with [dump-analysis.md](dump-analysis.md)'s findings (no file
signatures, no meaningful strings, moderate entropy with heavy
padding rather than a flat encrypted-looking distribution): **this flash
may not contain CPU machine code at all.** Conwise CW66xx-family chips
are documented as having on-chip ROM for their Bluetooth baseband stack
specifically so they don't need external program flash. If U9 is
similarly self-contained, U10 is a more likely candidate for pure *data*
storage -- a pairing/bond table, audio DSP coefficients, EQ presets,
voice-prompt audio samples, or similar -- rather than executable code.

This isn't confirmed either, but it's consistent with every negative
result gathered so far, and would mean continuing to guess CPU
architectures for Ghidra is the wrong next step.

## Next steps worth considering

- Look at the ~374KB "real content" region (see dump-analysis.md) for
  patterns consistent with structured data records (fixed-size repeating
  blocks, a table of offsets/lengths, audio codec framing) rather than
  code.
- If audio data is suspected, try interpreting chunks as raw/compressed
  PCM at plausible sample rates and listening to the result.
- Revisit U9 identification with fresh eyes -- confirming whether it
  really is Conwise-family (and which specific part) would settle the
  on-chip-ROM-vs-needs-external-code question directly, rather than
  inferring it from a dead end in Ghidra.
