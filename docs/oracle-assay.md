# Argentum Assay

A proposed first-party Oracle-text parser: Scryfall JSON in, `mtg-sdk` models out, with a grammar
where every rule is written in both directions so it proves itself against the whole corpus without
a human reading the output.

**Status: Phase 1 shipped, and the differential gate is live.** The `Phrase` kernel, invertible
normalization, the touchstone, a grammar covering vanilla cards and keyword-only abilities, and
`assay differential` against the hand-written corpus all live in
[`:oracle-assay`](../oracle-assay/README.md). The MVP the work is aimed at — *Assay reads a whole
card and proves the reading against the card we already wrote* — and the numbers each phase landed
on are in [`docs/plans/oracle-assay.md`](plans/oracle-assay.md). Phases 2 and 4–6 remain proposals.
Assay has since superseded the `:mtgish-tooling` pipeline, which is deprecated; the comparisons to
mtgish below record why.

## The name

An *assay* is the metallurgical test that determines what a sample of ore actually contains. You
assay silver; *argentum* is silver. Three things make the name carry weight rather than just rhyme:

- An assay never reports "looks fine". It reports **fineness** — purity in parts per thousand.
  Coverage is a number or it is nothing.
- "Assay" also means *to attempt*. An inconclusive assay is a valid, reportable result, which is
  exactly how a parser should treat text it cannot read.
- It reads well in use: `:oracle-assay`, `assay parse`, `assay gate`, `just assay-report`.

The vocabulary carries into the design without getting precious: **fineness** is the coverage
metric, a **decline** is a card the grammar refuses, and the round-trip check is the **touchstone**.
Everything else keeps its ordinary name.

## Why not keep extending `:mtgish-tooling`

Not a criticism of the module — it produces real cards and the coverage dashboard built on it is
genuinely useful. But four costs are structural rather than incidental.

| Cost | Evidence |
|---|---|
| **A third ontology in the middle** | mtgish's IR is shaped for mtgish. `emitter/TargetRecovery.kt` is 1,423 lines reconstructing target and filter structure that the IR's shape discarded. |
| **Two dictionaries to keep in sync** | `bridge/` answers "can Argentum express this tag?", `emitter/` answers "what Kotlin does it emit?". They can disagree — that disagreement *is* the SCAFFOLD tier. |
| **Text out, so text in is all you can check** | The emitter produces Kotlin *source strings*. Correctness can only be tested by compiling and diffing gameplay trees; there is no structural inverse to check against. |
| **An external grammar we don't own** | The corpus is downloaded and gitignored. Its ~1,000 nonterminals grew a variant per phrasing, and its choices are not ours to change. |

Mapping that IR in detail produced the finding that motivates this document: mtgish spends **933
variants** naming sets of objects, **383** moving them between zones, and **499** referring to an
earlier step's result — roughly 1,800 enum cases for what is underneath one noun, one verb, and a
named value.

**And Argentum's runtime already speaks the factored version.** `mtg-sdk/dsl/PipelineBuilder.kt`
is `gather` → `chooseExactly`/`chooseUpTo`/`chooseAnyNumber`/`selectAll` → `move`/`destroy`/`exile`
over typed slots (`CollectionSlot`, `NumberSlot`, `ChosenSlot`). The ~21.7k lines of tooling exist
to translate *into* that from a vocabulary that doesn't match it.

## The one idea: the grammar runs backwards

Assay's rules are bidirectional. A rule knows how to turn a span of Oracle English into an SDK model
node, *and* how to turn that node back into English. Both halves register together, so a rule cannot
exist with only one.

That buys the gate everything else rests on:

```
for every oracle text t in the Scryfall bulk:
    print(parse(t)) == normalize(t)      // or: declined, and counted
```

Roughly thirty thousand assertions that need no human to read them. A rule that quietly drops the
word "other", or collapses "up to three" into "three", cannot survive it — the next reprint fails.
This is the discipline that made the mtgish v2 refactor safe, moved from the schema boundary to the
parser boundary where it is worth considerably more.

It also gives ambiguity a definition instead of a feeling. The parser returns *every* parse:

| Parses | Meaning | Action |
|---|---|---|
| 0 | The grammar doesn't cover this text | Decline. Record the longest partial match and the token it died on. |
| 1 | Unambiguous | Accept, subject to the touchstone. |
| >1, same model | Two rules spell one meaning | Fine. Grammar redundancy; report it so it can be simplified. |
| >1, different models | Genuine ambiguity | **Hard error**, naming the card and both readings. Never pick one. |

### What a rule looks like

```kotlin
val drawCards = phrase<Effect>("draw {n} cards") {
    slot("n", cardinal)
    build { Effects.DrawCards(it.amount("n")) }                    // text -> model
    match { e -> (e as? DrawCardsEffect)?.let { bind("n" to it.count) } }  // model -> text
}

val destroyTarget = phrase<Effect>("destroy target {obj}") {
    slot("obj", permanentFilter)
    build {
        Effects.Pipeline {
            val t = selectTarget(TargetPermanent(it.filter("obj")))
            destroy(t)
        }
    }
    match { e -> e.asSingleTargetedDestroy()?.let { bind("obj" to it.filter) } }
}
```

The template string supplies the surface form for both directions, so the printer cannot drift from
the parser. Slots are themselves phrases, recursively — `cardinal` and `permanentFilter` are
ordinary rules with the same two halves.

Where two English phrasings mean the same thing, the alternate is marked `canonical = false`: it
parses but never prints. That single flag — lifted from the mtgish v2 rule table, where it resolved
`DrawACard` versus `DrawNumberCards(1)` — is what keeps printing deterministic when the language
isn't.

## The second idea: there is no Assay IR

Assay parses **directly into `mtg-sdk` types** — `Effect`, `GameObjectFilter`, `TargetRequirement`,
`DynamicAmount`, the pipeline steps. No intermediate representation, no bridge, no capability
dictionary. Three things follow, and together they are the argument for a new module rather than a
refactor of the old one:

- **"Can Argentum express this?" becomes "did it parse?"** The two dictionaries collapse into one,
  and a decline names a missing *SDK capability* in Argentum's own words — a strictly better backlog
  signal than a missing mtgish tag.
- **Nothing to recover.** Targets and filters are parsed into final form, so `TargetRecovery.kt` has
  no job.
- **Kotlin emission becomes a pretty-printer** over a typed model, and gets its own round trip.

That last point fixes something quietly. Today `emitter/SpellShortcuts.kt` recognises whole-card
shapes and emits a named `Patterns.*` composition, correct by careful hand-reading. Under Assay a
shortcut is a *fold* on the model, admissible only if the compiled result expands back to an
identical model. Shortcuts stop being trusted and start being checked, which makes them safe to add
aggressively.

## What this says about `mtg-sdk`

If the SDK is the IR, the SDK's factoring is the parser's ceiling — so it is worth auditing against
the failure modes mtgish demonstrates. Counts below are from the working tree at time of writing.

**The headline is good.** `mtg-sdk` does not have mtgish's core disease. There is no
`DestroyEachPermanent`/`DestroyAPermanent`/`DestroyNumberPermanents` family; cardinality lives in
`DynamicAmount` and `SelectionMode` where it belongs. Of 446 effect `@SerialName`s only 13 contain
"Each", nearly all genuine semantics ("each player") rather than a quantifier welded to a verb.

Three rot patterns have nonetheless started.

### 1. The reach split — the serious one

`CardSource` is a good shared noun with 17 variants. It is consumed by **one file**.
`PipelineEffects.kt` is 68 of the 446 effects; the other ~378 — removal, library, player, combat,
token, damage, counters, tap — take a bespoke `GameObjectFilter`, a target, or nothing.

That is mtgish's `Exilable`-knows-21-shapes / `Millable`-knows-2 problem one level up, and it is
already visible as two sacrifice mechanisms with different reach:

```kotlin
// pipeline: 17 sources, dynamic quantifier, composable filter
sacrifice(from: CollectionSlot)

// standalone: a filter, a literal Int, and two ad-hoc booleans
data class SacrificeEffect(
    val filter: GameObjectFilter,
    val count: Int = 1,                  // cannot say "sacrifice X creatures"
    val any: Boolean = false,            // a second quantifier axis, bolted on
    val excludeSource: Boolean = false,  // a filter special-case, outside the filter
)
```

**A shared noun only pays off if every verb takes it.** One that half the verbs ignore is not a
factoring, it is a second dialect — and which dialect a card gets is decided by which verb happened
to be written first.

### 2. Cardinality is half-migrated

39 effect parameters are a literal `Int` count against 36 that are `DynamicAmount`. Roughly half the
SDK cannot say "X" or "equal to the number of…". This is not found by design review; it is found one
card at a time, years apart, and each find is a signature change.

### 3. Boolean knobs are variant explosion in disguise

113 boolean parameters across the effect types, worst offenders carrying six to eight:

| Effect | Booleans | Combinations |
|---|---:|---:|
| `GrantMayPlayFromExileEffect` | 8 | 256 |
| `MoveCollectionEffect` | 7 | 128 |
| `ModalEffect` | 7 | 128 |
| `CreateTokenEffect` | 6 | 64 |

mtgish minted `DestroyEachPermanent`; we add `val each: Boolean = false`. Same information, same
combinatorial surface, now invisible to a name search and untested in all but a handful of the 2ⁿ
settings. Where the flags encode a genuine axis — and in `MoveCollectionEffect` they do — that axis
wants to be a named enum, exactly the `cause` field the v2 refactor introduced for the same reason.

### 4. The `…ThisWay` curve, at its beginning

Nine of the seventeen `CardSource` variants are not sources. They are bespoke names for *a value
something earlier bound*:

```
TappedAsCost      ChosenTargets       FromLinkedExile
CraftedMaterials  TriggeringEntity    EnteredViaThisResolution
LastKnownCombatPairedWithSource       CreaturesThatSaddledSource
LastKnownEquipmentAttachedToSource

// ...against exactly one general mechanism:
FromVariable
```

These are our `TheCardsExiledThisWay` and `TheTokensCreatedThisWay`. Every new mechanic adds one —
saddle added `CreaturesThatSaddledSource`, craft added `CraftedMaterials` — because minting an enum
case is a two-line change and generalising is not.

**mtgish finished that curve at 382 bespoke variants and no general mechanism**, across 25 owning
types; unifying them required declining 265 outright. It got there one locally-reasonable two-line
change at a time. We are at 9-to-1, and the pipeline already has the right answer (typed slots,
`FromVariable`, `storeNumber`, `captureControllers`). The fix is a policy, not a refactor:

> A new mechanic may not mint a `CardSource` variant for a value an earlier step could have bound.

That costs nothing today. It is proposed as an addition to
[`sdk-design-principles.md`](sdk-design-principles.md), not adopted by this document.

### What Assay does about all four

It surfaces them, ranked, with nobody auditing anything. Because the grammar parses into SDK types, a
card that cannot be expressed *does not parse*, and the decline names the missing capability in
Argentum's vocabulary. "Sacrifice X creatures" declines against `SacrificeEffect(count: Int)`.

So the fineness report is two documents at once: parser coverage, and a continuously-updated SDK gap
report ordered by how many real cards each gap blocks.

## The asset nobody else had

The round trip proves a parse is *reversible*. It does not prove it is *right*. Concretely: in the
mtgish v2 slot work, `ACardWasntDrawnThisWay` — a negative condition — mapped to the positive "a card
was drawn" and round-tripped perfectly, because a bijection only has to be reversible. Four such
inversions existed, and every one was found by reading names, not by running the corpus.

This repository has an answer mtgish never could: **8,728 canonical hand-written `cardDef`s** and
**2,528 scenario tests** asserting how they behave. That is ground truth, and it makes a semantic
gate possible:

```
just assay-differential

  Hand-written cards                 8874
    compared                          449
    not yet covered by the grammar   7630
    script slot not modelled yet       33
    multi-face (out of scope)         301
    Oracle text differs from golden   461

  Confirmed — models agree            444   988.9‰ (98.9%)
  DIVERGENT — read every one            5   <- read every one of these
```

A divergence is either a parser bug or a bug in a hand-written card. Both are worth finding, and
nothing in the mtgish pipeline can produce that list. **It found the predicted class immediately**: a
reading of multi-quality protection as one ability where CR 702.16g makes it two — reversible, and
wrong. It also caught, during construction, "Plains" de-pluralizing to `Subtype("Plain")`: the
`Wasnt` failure mode above, live, on the basic land types.

The buckets beside the comparison are the point as much as the number is. Each one is a way the gate
could have claimed a check it had not performed — a card only partly read, a golden joined to the
wrong Scryfall entry, a definition using a slot the grammar cannot produce — and naming them keeps
the denominator visible instead of flattering the numerator.

## Three gates

In increasing strength. Each catches a class the previous one cannot.

**1. Touchstone — textual round trip.** Over every unique Oracle text in the Scryfall bulk. Fully
automatic, no labels needed, runs in CI on every grammar change.
*Catches:* dropped words, collapsed quantifiers, lost modifiers, printer drift.
*Misses:* a reading that is reversible but means the wrong thing.

**2. Differential — against the 8,728.** Structural diff of the parsed model against the
hand-written `CardDefinition`, modulo an explicit fold list. Every divergence triaged; the fold list
reviewed, never grown silently.
*Catches:* the `Wasnt` class — semantically wrong, perfectly reversible.
*Misses:* cards nobody has implemented yet.

**3. Behavioural — the existing scenario suite.** Swap the parsed definition in for the hand-written
one and run that card's scenario test. The 2,528 tests already encode the rules-correct outcome.
*Catches:* models that are structurally plausible and behave wrongly.
*Cost:* slow. Nightly and on release, not per-commit.

Plus a fourth guarding the emitter rather than the grammar: `compile(render(m))` must deserialize
back to `m`. That is the existing `mtg-sets:verifyGeneratedCards` task pointed at a model comparison
instead of a text one.

## Normalization

The touchstone compares against *normalized* text, so normalization is load-bearing. Scryfall's
`oracle_text` is already errata'd to current templating, which removes the biggest obstacle — thirty
years of drifting wording — before we start.

| Pass | Rule |
|---|---|
| Self-reference | The card's own name → `~`, longest-match first so "Kenrith, the Returned King" beats bare "Kenrith". Printing restores it. |
| Reminder text | Stripped for parsing, **regenerated** from the keyword when printing. A mismatch is a finding: our keyword model and the printed gloss disagree. |
| Ability split | One ability per line. Within a line, sentences become a step sequence — which is what a pipeline is. (mtgish requires one ability per line *and* fails on two sentences; that limit goes away.) |
| Faces | Each `card_faces` entry parsed independently; the layout decides how they compose (DFC, adventure, split, room). |
| Symbols | `{T}`, `{2}{U}`, `{E}`, loyalty `[+1]` lexed as tokens, never as prose. |

**Normalization is held to the same standard as the grammar: every pass ships with its inverse and
is included in the round trip.** If a pass throws information away, the touchstone stops being a
proof and becomes a formality — you can always pass a round trip by normalizing hard enough.

## Module shape

```
oracle-assay/            // deps: mtg-sdk only (no engine, no server)
  syntax/                // the Phrase kernel: templates, slots, both directions
  normalize/             // Scryfall JSON -> canonical ability lines (+ inverse)
  grammar/               // the rules, by topic - the part that grows forever
    Cardinals.kt  Filters.kt  Targets.kt  Zones.kt
    Keywords.kt   Triggers.kt Costs.kt    Statics.kt
  render/                // typed model -> cardDef source; Patterns folds
  gate/                  // touchstone, differential, behavioural, fineness report
  compile/               // a whole reading -> CardDefinition (the custom-card sandbox)
  cli/                   // assay parse | compile | gate | report | explain
```

It is deliberately **not a runtime loader**, and the house rule stands unchanged: a generated card is
a draft until a human reviews it and a scenario test passes. Assay makes the draft trustworthy enough
to review quickly; it does not make review optional.

One capability comes free and is a product feature rather than tooling: a parser running in the JVM
on the SDK's own types means **Argentum can accept user-authored cards** — paste Scryfall-shaped
JSON, get a playable definition, with declines reported in plain language.

**That capability has shipped, in the narrowest form that keeps the "not a loader" rule true.** The
Scenario Builder has a *Custom cards* panel (dev endpoints only): paste a card object, see each
printed line with its verdict and a caret on any token the parse died on, and — when Assay reads the
card *whole* — put the compiled card into any zone and play it. `compile/CardCompiler.kt` does the
reading; `game-server`'s `AssayCardService` registers the result in a `CardRegistry` overlay scoped
to one scenario session. A card with even one unreadable line is refused rather than approximated,
and nothing about the corpus changes: shipped cards are still hand-written `cardDef`s with scenario
tests. `just assay compile --file card.json` is the same path without a server.

## Fineness

One report, per set and for the whole corpus, from the first milestone:

| Row | |
|---|---|
| Unique Oracle texts assayed | — |
| Round-trips byte-exact | — ‰ |
| Declined, with the token it died on | — |
| Ambiguous — distinct readings | must be 0 |
| Divergent vs. hand-written | — |
| Top declines, ranked by cards unlocked | list |

The last row is what pays for the project. In the v2 work, making `lower()` *total* — unknown input
kept verbatim rather than crashing — meant coverage was measured from the first line of code instead
of estimated, and the to-do list ranked itself. Assay declines the same way: a card that doesn't
parse is counted, not lost.

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| **Bidirectional rules cost ~2× to author**, and printing is underdetermined wherever English has synonyms. | High | `canonical = false` makes the choice explicit and one line. Accept the cost; it buys the gate, which is the point. |
| **The long tail is brutal.** ~30k unique Oracle texts; the last quarter is layered templating and genuine prose. | High | Declines are first-class and ranked, so the plateau is visible rather than a surprise. Assay does not need 100% — it needs to beat the incumbent per set, which the cutover rule measures. |
| **Ambiguity explosion** on long texts. | Medium | Anchor and left-factor the grammar; memoize. Sentences are short. Cap parses per span and treat hitting the cap as a decline. |
| **Rebuilding ~21.7k working lines.** | Medium | Nothing is deleted until the last phase, and cutover is per set. If it stalls early, the incumbent is untouched and the fineness report is still a better backlog than what exists. |
| **Scryfall templating drift.** | Low | The touchstone *detects* this by construction: a wording change surfaces as a new decline on a card that used to pass. |

## Non-goals

- Not a runtime card loader. Generated cards remain drafts pending human review + a scenario test.
- Not a general English parser. An allowlist grammar of Oracle-ese; declines are first-class.
- Not attempting silver-border, `ante`, or anything needing knowledge outside the card.
- Not replacing the coverage dashboard's *purpose* — backlog triage stays, re-pointed at Assay's own
  decline report in the final phase.

## See also

- [`docs/plans/oracle-assay.md`](plans/oracle-assay.md) — the phased realization plan
- [`docs/sdk-design-principles.md`](sdk-design-principles.md) — the bar for new SDK types
- [`mtgish-tooling/README.md`](../mtgish-tooling/README.md) — the deprecated pipeline Assay replaced
