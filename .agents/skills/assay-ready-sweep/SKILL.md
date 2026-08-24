---
name: assay-ready-sweep
description: Implement every "Assay-ready" card for a Magic set — the cards Argentum Assay reads whole that the set hasn't authored yet — placing each canonical in its earliest real printing and giving the sweep's own set a `Printing` row when the card is a reprint. Computes the four-way split first, authors to `assay compile`'s JSON rather than to the oracle text, and proves the result with the differential gate. Use when asked to "implement all the Assay-ready cards for set X", "sweep set X", "do the free cards in set X", "add the cards Assay can read for X", or when the Set Completion view shows a set with a large ⚡ Assay-ready count.
argument-hint: <SET name or code>
---

# The Assay-ready sweep

A set's ⚡ Assay-ready count is the cheapest work in the corpus: cards whose every printed line Argentum
Assay reads *whole*, meaning the SDK already has a spelling for all of it. This skill turns that count
into merged cards.

It is one job with two halves that look identical in the badge and are completely different in the repo:
**authoring a canonical** and **adding a `Printing` row**. `SetCoverageService.assayReady` counts a card
as done when its name appears in the set's `cards` **+ `basicLands` + `printings`**, so a card whose
canonical has existed elsewhere for years is badged exactly like one nobody has ever written. A sweep
that authors canonicals and stops looks finished and isn't — PR #1946 reported Jumpstart 101 → 0, and the
same filter returned **79** the next day, every one a missing reprint row.

So: compute the split before planning anything, and never infer the post-change count — re-run the
filter on the branch.

## Stage 0 — the split

```bash
just assay-bake                              # only if the ledger is stale; it is a deliberate commit
just assay-ready <CODE> --names --tail
```

Five buckets, five different jobs:

| Bucket | What it is | The work |
|---|---|---|
| `original` | no canonical anywhere, and this set **is** the earliest real printing | author the `card(...)` here |
| `elsewhere` | no canonical anywhere, earliest real printing is a **different scaffolded set** | author the canonical **there**, then a `Printing(...)` row here |
| `row-only` | canonical already in the corpus | a `Printing(...)` row here, nothing else |
| `basic` | a basic land | a per-set `basicLand(...)` val + the `basicLands` override — **never** a `Printing` row |
| `unscaffolded` | earliest printing's set has no `definitions/<code>/` | **blocked**: scaffold that set first (a minimal `MtgSet` object; `MtgSetCatalog` discovers it) |

`--tail` is the bucket nobody budgets for. Authoring a canonical is what makes `check-card-printing` able
to see that the card's *other* scaffolded printings have no rows either, so those rows appear the moment
you land the canonical and belong in the same PR. M20's 57 new canonicals owe 36 rows across 18 other
sets; CLB's owed 20.

**Read the shape before planning the work.** The count varies by two orders of magnitude and the *kind*
of work varies with the set type:

- **A reprint or Commander product** is a *placement* job. BLC: 7 row-only / 21 elsewhere / 2 originals /
  0 basics — almost none of the tail is the set's own design. Expect to author in a dozen other sets, and
  expect to collide with any agent working those sets.
- **A premier set whose identity is its new mechanics** is the basic-land job and nothing else. MKM: 5
  Assay-ready, all five basic lands, because cases/disguise/cloak/collect evidence/suspect put a declining
  word on nearly every missing card. Say so before proposing a fan-out.
- **A Horizons set** is the keyword-tax shape: a large Assay-ready tail that skews toward returning
  mechanics. MH2 landed modular, cascade and bushido in one batch — and bushido had *zero* corpus
  implementations.

## Stage 1 — the capability pre-flight

Before authoring anything, trace every mechanic the list touches: one table of **SDK type → its
`rules-engine` consumer → a card to copy from**. One `Explore` agent does this in a single pass.

This is not optional diligence, it is the step that keeps cards from silently doing nothing. A
`Keyword.X` existing is **not** the engine reading it: vanishing, fading, modular, annihilator and cascade
all render the keyword line while `rules-engine` ignores them entirely. Declaring one ships a card that
lies. The fix is to spell the rule out of primitives that *are* live (Deep Forest Hermit spells CR 702.62
as `EntersWithCounters(TIME, 3)` + an intervening-`if` upkeep trigger + `countersRemovedFrom(lastRemoved
= true)` → `SacrificeTarget(Self)`), which needs no new vocabulary and stays in the batch PR.

ONE's sweep ran this probe and came back **35 compared / 35 confirmed / 0 divergent** — the first with no
delta at all. Budget the probe; it is cheaper than a dropped card.

## Stage 2 — the differential baseline

Take it **with the binary you will use afterwards**, or the A/B is meaningless:

```bash
./gradlew -q :oracle-assay:installDist
oracle-assay/build/install/oracle-assay/bin/oracle-assay differential | tail -5
```

MH2's first baseline read "33 goldens would not decode / 36 divergent" against 0/35 after — which looks
like the change fixed 33 unrelated cards. It didn't: the baseline used a **stale installed binary** in the
main checkout. Run the freshly-built binary with cwd = the checkout holding the goldens (it walks up for
`settings.gradle.kts`); that is the clean A/B and it needs no revert.

Record `compared / confirmed / divergent` now. At the end you classify **only the delta**.

## Stage 3 — author to Assay's JSON, not to the oracle text

This is the single biggest quality lever in the whole job.

```bash
oracle-assay/build/install/oracle-assay/bin/oracle-assay compile "<Card Name>"
```

It prints the whole `CardDefinition` as JSON — the exact SDK model Assay's grammar builds. Authoring *to
that model* makes the differential a formality: Dominaria United's 50 cards produced **0** divergences,
against 6 real defects in MH3/J22's 166 cards written from the oracle text.

Four mechanics that matter:

- **Use the installed binary in a loop**, not `just assay compile` per card. It is faster (~50 cards in a
  couple of minutes) and it is the *only* form that works for a name with an apostrophe — `just`'s
  `*ARGS` re-enters `sh -c`, so `just assay compile "Urza's Rebuff"` dies on `unexpected EOF` no matter
  how you quote it.
- **Length-check the batch.** `compile` looks a bare name up in the Oracle bulk and can hit a non-card
  sharing that name: `compile "Savage Lands"` returned an FMSC art card, empty type line, 359 bytes
  against ~1700 for its siblings.
- **Take metadata from the set's own Scryfall payload, not from the compile.** Its `setCode` and
  `metadata.imageUri` are whatever printing the Oracle bulk happened to carry.
- **When the JSON shows a field the corpus rarely writes** (`prompt`, `showAllCards`, `restOrder`, a
  nested `Composite`), find the `Patterns.*` facade whose defaults produce it — grep the assay `grammar/`
  sources for `Patterns.<X>.<fn>` and it names the function directly.

Then place each card by its bucket. `elsewhere` cards are authored in the earlier set with **that set's**
metadata (re-fetch with `&set=<earliest>`), and this set gets the row. For row shapes and the registry,
see [`add-card/reprints.md`](../add-card/reprints.md).

Fanning out over more than a handful of cards: [`fan-out.md`](fan-out.md).

## Stage 4 — the placement tools

The reprint rows are generated, not hand-written:

```bash
python3 - <<'PY'          # refresh stale printing caches FIRST — see the trap below
import importlib.machinery, importlib.util, sys
sys.path.insert(0, "scripts")
loader = importlib.machinery.SourceFileLoader("mr", "scripts/missing-reprints.py")
spec = importlib.util.spec_from_loader("mr", loader); mr = importlib.util.module_from_spec(spec)
sys.modules["mr"] = mr; loader.exec_module(mr)
for name in ["<Card One>", "<Card Two>"]:
    mr.fetch_printings(name)
PY
scripts/generate-reprints.py --set <CODE> --dry-run
scripts/generate-reprints.py --set <CODE>
```

**`generate-reprints.py` silently skips any card whose `~/.cache/scryfall/printings/<slug>.json` is past
its 30-day TTL** — `load_card_printings` returns `None` and `continue`s, and the skip does not appear in
the summary. 40 of JMP's 79 were stale and would have been dropped without a word. Refresh first, then
diff the generated file count against the bucket count.

`just coverage-relocate <CODE>` exists and does the `elsewhere` move mechanically, but it emits *mtgish*
drafts rather than Assay's reading — treat its output as a starting skeleton, not as authored cards.

## Stage 5 — prove it

Run the gates once for the whole batch, via the **`verify`** skill:

1. `just build` (or `just test` if Stage 1 found a mechanic needing engine work) — compiles only.
2. `just rebless-cards`, and confirm **only your cards** moved in the golden.
3. `just assay-differential` — the correctness net. This is where the bugs are: 6 real defects in 166
   cards, all in the `staticAbilities`/filter layer no other gate looks at. Classify only the delta from
   your Stage 2 baseline, as parser bug / card bug / known fold.
4. `just check-card-printing "<Card>"` per authored canonical.
5. **`just assay-ready <CODE>` again, on the branch.** It must read 0 Assay-ready. Never infer this
   number — that inference is exactly what made #1946 wrong by 79.

Adding behaviour Assay does *not* model is not a divergence — those abilities land in the "script slot
not modelled yet" bucket. Check which bucket a card moved into before deciding to drop it.

## Traps

- **A basic land is not a `Printing` row** — it is a per-set `basicLand(...)` val plus `override val
  basicLands by lazy { CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code) }` (mirror
  `DominariaBasicLands.kt`). Generating `PlainsReprint.kt` is wrong.
- **A Snow-Covered basic cannot use `basicLand()`** — it hardcodes `Basic Land — <type>`. Hand-write it
  with `card()` (mirror `ice/cards/SnowCoveredIsland.kt`) and set `inBooster = false`, or
  `BoosterGenerator.getBasicLands` will offer it as a freely-addable basic, which its own ruling forbids.
- **`check-card-printing` flags every basic land in every set as drift**, because it wants a `Printing`
  row where the repo uses `basicLand(...)`. Pre-existing script limitation — ignore that class rather than
  "fixing" it.
- **Promo-first printings**: `assay-ready` filters them (it reuses `expected_canonical`), but if you take
  `prints[0]` by hand you will file Ant Queen's canonical in `pm10`.
- **A card absent from the ledger is a third state**, not a decline: multi-face cards filed under a bare
  name never get a verdict. `assay-ready` reports them separately; they are not free work.
- **A one-mechanic cluster is a metadata job.** When the list clusters on one keyword (MOM: 12 of 43 were
  convoke), check that keyword against `rules-engine` once and the rest of the sweep is metadata.
- **First-in-corpus mechanics earn a scenario test even with no new SDK vocabulary.** Bushido existed as
  `KeywordAbility.bushido(n)` and no card had ever used it; Jade Avenger is the first lowering and got a
  test. (Use `EffectTarget.Self` there — `Triggers.Blocks` fires off a `BlockEvent` that does not bind the
  source.)
- **`ScenarioTestBase.passUntilPhase` hangs a multi-turn test.** A scenario board has no library, the game
  ends mid-loop, and `passUntilPhase` breaks *without erroring* — the outer loop spins until the watchdog
  halts the JVM (exit 13, which reads like a crash). Use `GameTestDriver` with a real `Deck.of(...)` for
  anything spanning turns; the two bases mix fine in one file.
- **Sweeping a reprint-heavy set collides with other agents**, because the canonicals scatter across a
  dozen old sets. Check for live worktrees on those sets before starting.

## The PR

One PR for the sweep. Title in house style (`Add 79 Assay-ready cards to Core Set 2020`). The body carries
the four-way split with counts, the before/after differential triple, the before/after `assay-ready`
count, and any card that left the batch with the reason. Findings the sweep surfaced but did not fix
(a parser bug, a wrong CR citation in someone else's KDoc) go in the body too, named — they are the
sweep's other product.
