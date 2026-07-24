# `:mtgish-tooling`

Guidance for coding agents working in this module. Read [`README.md`](README.md) first — it's the
authoritative reference for the commands, the two dictionaries (`bridge/` capability + `emitter/`
rendering), the fidelity policy, and the write/relocate paths.

## What this module is

A **predictive, non-authoritative** analyzer + draft generator that maps the external
[mtgish](https://github.com/i5jb/mtgish) oracle IR onto our SDK capabilities. It is `scripts/`-style
tooling, **never a card loader** — ground truth stays a human-authored `cardDef` whose scenario test
passes. Generated `.kt` are DRAFTS in a staging dir; they must compile, get a scenario test, and be
human-reviewed before moving into a set's `cards/` package.

## ⚠ Creator's note before extending the bridge/emitter

The engine underneath this tooling is still **sloppy around extra costs and value selection** —
additional costs, choosing values at cast/activation time (X, creature type, color), and *inheriting*
a cast-time choice into later effects (e.g. "When ~ enters, draw X cards" using the cast-time `X`).
Forge handles this with a `declare` directive + hidden X bookkeeping; we have no clean equivalent.

Consequences when working here:

- The emitter must keep returning `null` (→ `SCAFFOLD`) for these shapes rather than guessing.
- Even a "complete render" that touches them needs a scenario test before you trust it.
- This is an **open design area the creator wants to fix and welcomes suggestions on** — see the full
  write-up under *"⚠ Creator's note: extra costs & chosen / inherited values"* in [`README.md`](README.md).
