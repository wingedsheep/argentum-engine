# Plan: Argentum Assay — a first-party Oracle-text parser

Design: [`docs/oracle-assay.md`](../oracle-assay.md). This is the build order.

> **Status:** Assay has superseded `:mtgish-tooling`, which is now deprecated. The cutover phases below
> are kept as the historical plan.

## Goal

Replace `Scryfall → mtgish (Go) → mtgish.lines.json → emitter → Kotlin source` with
`Scryfall → Assay → mtg-sdk model → Kotlin source`, using a bidirectional grammar whose
correctness is proved by a corpus-wide round trip rather than by review.

Decisions (locked):

- **Bidirectional or it doesn't ship.** A rule registers `build` (text→model) and `match`
  (model→text) together. No parse-only rules, ever — the gate is the whole point.
- **No intermediate representation.** The grammar targets `mtg-sdk` types directly.
- **Declining is success.** Unparseable text is counted and ranked, never approximated.
- **Per-set cutover, no flag day.** A set moves to Assay only when Assay's whole-render rate on
  that set beats `:mtgish-tooling`'s. `:mtgish-tooling` stays authoritative until Phase 6.
- **New module `:oracle-assay`, depending on `:mtg-sdk` only.** Not on `:rules-engine`, not on
  `:mtgish-tooling`.

## Core insight: most of the substrate already exists

- **The target vocabulary is already factored.** `mtg-sdk/dsl/PipelineBuilder.kt` is
  `gather` → `chooseExactly`/`chooseUpTo`/`chooseAnyNumber`/`selectAll`/`chooseRandom` →
  `move`/`destroy`/`sacrifice`/`exile`/`toHand`/`toGraveyard`, over typed slots. The parser's output
  shape is a solved problem; we are not designing an IR.
- **Scryfall ingestion exists.** `mtgish-tooling/.../coverage/Scryfall.kt` already fetches and caches
  set data. Assay needs the bulk `oracle_text`, which is the same source.
- **The semantic oracle exists.** 8,728 canonical `cardDef`s and 2,528 scenario tests are ground
  truth for gates 2 and 3, and they are already in the repo.
- **The compile gate exists.** `mtg-sets:verifyGeneratedCards` already compiles generated drafts and
  serializes them; it needs re-pointing at a model comparison, not rebuilding.

**What genuinely does not exist:** the bidirectional `Phrase` kernel, invertible normalization, and
the grammar itself. The grammar is the long pole and always will be.

---

## The MVP

Decided 2026-08-15, after Phase 1 shipped. The phases below are ordered by *layer*; the MVP is the
vertical slice through them that is worth shipping on its own:

> **Assay reads a whole card and proves the reading against the card we already wrote.**

Not: emit Kotlin, beat `:mtgish-tooling` on a set, retire the bridge. Those are Phases 4–6, and none
of them is where the risk is. Phase 1 proved the machinery is *reversible*; it did not prove it is
*right*, and every rule added before there is a semantic gate is grammar built on unverified
semantics. A renderer on that footing only generates wrong Kotlin faster.

Three parts, in this order:

1. **The differential gate** — done for the class the grammar reads whole; see Phase 3 below.
2. **One vertical grammar band** — the narrowest rule set that makes simple *whole* cards parse:
   cardinals → a small filter/target vocabulary → a handful of pipeline steps → the trigger prefix.
   The decline table ranks this for us: `Whenever` (6,450 cards) and `When` (6,054) are the top two
   families by a wide margin, so triggers are neither deferrable nor a guess.
   *Done.* `Cardinals` (number words), `Targets` (the requirement/reference pair), `Filters` (the
   noun phrase, with its controller clause), `Steps` (draw, destroy, exile, tap, untap, return to
   hand — every one-verb spell over a targeted permanent) and `Triggers` (the ETB / dies / attacks /
   blocks / combat-damage prefixes) are in. Whole-card coverage 1,744 → **2,014**; the differential's
   compared population 449 → **537**.

   The trigger prefix answered its three open questions this way. A triggered ability keeps its
   target on the *ability* (`TriggeredAbility.targetRequirement`), so the rule slots `Steps.step`
   whole and lifts the resulting `CardScript` onto it — which means every step rule enriches every
   trigger rule for free. `AbilityId` is arbitrary in exactly the way a slot name is (the DSL
   generates it from a counter), so the grammar mints one constant and the differential normalizes by
   position. And `descriptionOverride` is presentation by the SDK's own definition — "overrides the
   auto-generated one" — so it is stripped on both sides rather than compared.

   It also needed a normalization pass, not a grammar rule: modern templating writes a card's
   self-reference as "this creature" rather than as its name, and the *noun* is a function of the
   type line that the model has nowhere to put. Both spellings now abstract to `~`, restored
   positionally — the same treatment the name pass already gave. That alone pulled 219 cards out of
   the `Oracle text differs from golden` bucket, because goldens and Scryfall disagreed on which
   spelling they carried.

3. **A third number in the fineness report** — beside "round-trips byte-exact" and "declined", a
   *confirmed* row: whole cards whose model matches the hand-written definition. *Done*, as the
   differential report's own summary.

**Done when:** several hundred implemented cards parse whole *and* differentially confirm; every
divergence is classified with none unexplained; at least one genuine bug has been found in a
hand-written card; `MISMATCH` and `AMBIGUOUS` are still 0.

**✅ Met, 2026-08-15.** 537 cards compared and 536 confirmed; one standing divergence, classified
(`TargetCreatureOrPlaneswalker` versus the general filtered target — two fully-wired parallel engine
paths, deliberately not folded); `MISMATCH` and `AMBIGUOUS` still 0 across 66,793 ability lines. The
bug in a hand-written card is **Meteor Golem**, whose printed "an opponent controls" was missing from
its target filter, so it could destroy its own controller's permanents — fixed, with a scenario test
that asserts the negative and fails without the fix.

Explicitly out: the renderer, per-set cutover, and closing the ~40 missing `Keyword` constants —
that last one is ranked content work with no risk attached, and it inflates Phase 1's number without
teaching anything.

---

## Phase 0 — Decide to start (no code)

Read the design doc, confirm the premise, and pick a first-milestone fineness target to be judged
against. Kill criteria for the whole project, agreed up front:

- Phase 1 ships and the touchstone cannot get vanilla + keyword-only cards past ~95% ‰. If the
  round trip can't hold on the easy quarter of the corpus, it won't hold anywhere.
- Phase 2 stalls below the incumbent's whole-render rate on any calibrated set (POR is the usual
  canary).

Neither costs anything to check, and both are cheap exits.

---

## Phase 1 — Kernel, normalization, gate harness ✅ SHIPPED

The riskiest phase, because it decides whether the round trip is achievable at all. Deliberately
paired with a trivial grammar so the *machinery* is what's under test.

**Outcome.** The round trip holds. Over the whole Scryfall Oracle bulk — 34,882 cards, 35,776 faces,
66,793 ability lines — the gate reports **0 ambiguities, 0 print mismatches, and 0 non-invertible
normalizations**. Module docs and the command list: [`oracle-assay/README.md`](../../oracle-assay/README.md).

```
Round-trips byte-exact           12646   189.3‰ (18.9%)   (whole corpus; mostly Phase 2+ text)
Alternate spelling normalized    30
Declined                         54117
Vanilla + keyword-only cards     1439 / 1712   840.5‰ (84.1%)   <- this phase's own target
```

Fineness is parts per thousand, so the target row reads **84.1%** — not 84.05%, and not 840%. The
kill criterion above is written "~95% ‰", which is ambiguous by a factor of ten; the phase lands
below it on either reading.

The shortfall is not the round trip faltering. Every remaining line in that class declines for one
reason: the SDK has no vocabulary for the keyword. `just assay-report --scope` ranks them — Exalted, Infect, Echo,
Soulshift, Bloodthirst, Scavenge, Backup, Megamorph, Unleash, Extort, Evolve, Myriad, Unearth,
Champion, Eternalize, Skulk, Melee, Battle cry, Reinforce, Devoid, Dethrone, Phasing, Cumulative
upkeep, and ~40 more, none of which has a `Keyword` enum constant. Closing that list is content
work with a ranked backlog attached, not a risk to the approach; the machinery it would run on is
proved.

Three findings the phase produced on the way, written up in the module README: `Enchant` and `Equip`
are keyword abilities modelled as an aura restriction and a `CardDefinition` field respectively (the
two largest keyword-only decline families, 1,289 and 621 cards); `PROTECTION_FROM_EACH_OPPONENT` and
`ProtectionScope.EachOpponent` are two spellings of one thing; and printed reminder text is a
function of the ability *and* the card's types, which a `KeywordAbility` alone cannot produce.

**Risk that did not materialize.** Printing turned out to be underdetermined in exactly two places,
and `canonical = false` resolved both cleanly: the semicolon separator ("Flying; banding", ~31 cards)
and line grouping. Both are properties of the printed text that a flat model has no room for, so
normalization owns the second and the first reports as a `VARIANT` — parsed correctly, printed
canonically, model provably unchanged. That verdict is the one addition to the design's vocabulary
this phase made, and it exists so that an alternate spelling is neither counted as a byte-exact
round trip nor as a failure.

New module `:oracle-assay` (`settings.gradle.kts`, `build.gradle.kts` modelled on
`mtgish-tooling/build.gradle.kts` but with an `implementation(project(":mtg-sdk"))`).

1. **`syntax/Phrase.kt`** — the kernel:
   - `phrase<T>(template) { slot(...); build { }; match { } }`
   - template parsing (`"destroy target {obj}"` → literal/slot sequence)
   - `parse(text): List<Parse<T>>` — *all* parses; `print(value: T): String?`
   - `canonical = false` for alternates that parse but never print
   - memoization keyed on (rule, offset); a per-span parse cap that degrades to a decline
2. **`normalize/`** — Scryfall JSON → canonical ability lines, **each pass with its inverse**:
   name→`~`, reminder-text strip/regenerate, line split, face split, symbol lexing.
3. **`gate/Touchstone.kt`** — run `print(parse(normalize(t))) == normalize(t)` over the bulk;
   emit the fineness report with declines ranked by cards blocked.
4. **`grammar/`** — vanilla cards and keyword-only abilities only. Nothing else.
5. **`cli/`** — `assay parse <card>`, `assay gate --touchstone`, `assay report`.
   `just assay-gate` / `just assay-report` recipes.

**Acceptance:** fineness reported for the whole corpus; ambiguity count is 0; every normalization
pass round-trips; `assay explain <card>` prints the token a decline died on. — **All met.**

---

## Phase 2 — The pipeline family 🚧 FIRST BAND SHIPPED

The largest and most mechanical share of the corpus, and the part where the SDK vocabulary already
lines up. This is where the fineness curve should climb steeply.

Grammar, roughly in dependency order — the first band of each is in, and the remainder of each line
is the next work:

1. `Cardinals.kt` ✅ number words. Still to come: "X", "that many", "equal to the number of…"
2. `Filters.kt` ✅ type and controller. Still to come: subtype, colour, power/toughness, "other"
3. `Zones.kt` — "your library", "the top three cards of", "your graveyard" → `CardSource`
4. `Targets.kt` ✅ "target creature" over a filter. Still to come: "any target", "up to two target…"
5. `Steps.kt` ✅ draw / destroy / exile / tap / untap / return to hand, one verb over one target;
   ✅ the counted verbs — life gain and loss, scry, surveil, damage (to any target, to a player, to a
   filtered permanent) and the pump spell with its `until end of turn`; ✅ the *sequence*; ✅ **the
   counter sentences** — "Put a +1/+1 counter on target creature." and the same clause aimed at the
   source and at the target an earlier clause chose, in both grammatical numbers.
   Still to come: sacrifice, mill, discard, counter (the verb), removing and moving counters, tokens
6. `Triggers.kt` ✅ enters / dies / leaves / attacks / blocks / becomes blocked / deals combat damage;
   ✅ the step triggers ("at the beginning of your upkeep", each-player's spelling as an `alternate`).
   Still to come: the other party's triggers ("whenever a creature you control dies"), the spell-cast
   triggers, "you may", and intervening-if

   **The step triggers measured smaller than their decline rank predicted, and that is the finding.**
   410 implemented cards decline on "At the beginning of…", but adding the prefixes moved whole-card
   coverage by only 23. The rest are blocked on their *effect* clause, not their prefix. Trigger
   prefixes are multiplicative with the step vocabulary rather than additive to it, so the decline
   table's rank overstates a prefix and understates a verb — the ranking is by the token a line *died
   on*, and a line dies on its first unknown token, which for a trigger is whatever follows the comma
   only once the prefix is known. Read the ranked *sentences* (below) before believing a rank.
7. `Mana.kt` ✅ "Add {G}", "Add {C}{C}", and the choice form "Add {B} or {G}" / "Add {W}, {U}, or {B}",
   which denotes *several* abilities rather than one effect with options. Still to come: "one mana of
   any color" and the rest of `ManaColorSet` (48 + 19 implemented cards), the "deals 1 damage to you"
   painland rider, and dynamic amounts.
8. `Costs.kt` + `Activated.kt` ✅ the cost-colon-effect sentence — `{T}`, `{2}, {T}` and a bare mana
   cost, with the clause after the colon slotting `Steps.step` whole. Still to come: the rest of the
   cost vocabulary (sacrifice, pay life, discard, remove counters), `ActivationRestriction`, and
   "Activate only as a sorcery".
9. `Replacements.kt` ✅ "~ enters tapped.", the shock-land form, and ✅ "~ enters with two +1/+1
   counters on it." Still to come: the check-land `unlessCondition` and the kicker entries, which
   are the same missing piece — a `condition` the rule refuses to drop rather than print without.
10. `Statics.kt` ✅ the aura band — `Enchant <filter>` (in `Targets`, since the SDK models it as a
    `TargetRequirement` and not as a keyword), "Enchanted creature gets +N/+N.", "Enchanted creature
    has <keyword>.", and the joined "gets +N/+N **and has** <keyword>." which denotes two abilities
    from one sentence. Still to come: the lord pump ("Creatures you control get +1/+1."), which
    needs a `GroupFilter` vocabulary — a *different type* from the `GameObjectFilter` `Filters`
    produces, so it is its own decision; the combat restrictions ("~ can't block.", "~ can block
    only creatures with flying.", "~ attacks each combat if able."); "doesn't untap during its
    controller's untap step"; and the control-change static ("You control enchanted creature.").

    **The win here is the slot, not the count, and it was chosen on that basis.** 225 aura cards
    decline in total and their tail is long and varied; the three sentences above are 30-odd whole
    cards. What they buy is `staticAbilities` — the largest `CardScript` slot the differential could
    not see into, and the one every later static family lands in. The greedy-cover ranking makes the
    point: the combat restrictions above are 41 sole-blocked cards between them and they are now one
    family in one file rather than a slot that did not exist.

    **Equip is not the same shape, and the band is where that stopped being one finding.** Enchant
    and Equip were reported together in Phase 1 as "two keyword abilities of identical shape modelled
    two different ways". Enchant turned out to be a plain `TargetRequirement` in a plain `CardScript`
    slot, so it reads as an ordinary filtered target and the whole of `Filters` arrives with it.
    Equip lowers at authoring time into `CardDefinition.equipCost` *and* a synthesized activated
    ability carrying its own timing, effect and target requirement — a lowering to reproduce rather
    than a sentence to read, and one that reaches past `CardScript` into a slot `CardFragment` did
    not model. Reading it was a `CardFragment` design change, not a rule. *Done*, as the equipment
    band below.

   **The land band is the first family where whole-card coverage moved with line coverage.** Lands
   are one-to-three-line cards, so 819 mana-ability lines and 319 tapped-entry lines over cards that
   already have a golden are mostly the *same* cards, and the two rule families together moved
   whole-card coverage by 497 against 2,154 lines. That ratio is what "rank sentences, not dead
   tokens" is for, and it is the opposite of the step triggers' — worth reading beside them.

   **Sentence case turned out to be a line property with more than one sentence in it.** An activated
   ability's effect clause is capitalized after the cost colon, so slotting the mid-sentence `Steps`
   templates after a colon needed the case shift to happen somewhere. It went where the line-initial
   one already lives — `syntax/SentenceCase.kt`, at the text boundary — rather than into a grammar
   combinator, because the alternative is every activated-ability rule restating its effect clause
   capitalized, which is exactly the re-spelling that stops `Steps` being slottable. The rule is
   Wizards': of 14,042 `": "` occurrences in the corpus, 32 are followed by a lowercase letter, and
   all 32 are prose enumerations on the "hero's journey" cards rather than ability costs.

**The counters band is the first one chosen by ranking the backlog rather than by picking a set, and
it settles how to read the ranking.** The token table's top row is a trigger *subject*; the step
triggers already proved that over-promises, because a line dies on its first unknown token and a
trigger's real blocker is usually after the comma. The measure that decides work is therefore **cards
whose line dies at the verb** — everything before it already read — and by that measure counters were
the largest family in the implemented population: 1,025 cards carry a counter line the grammar could
not read, and 656 decline on nothing else. A verb is also multiplicative, since `Triggers`, `Activated` and the modal rules all
slot `Steps.step` whole. Prefix versus verb now has a worked example on each side; read them
together before picking the next band.

**The equipment band is the third, and the first where the overstatement was measured *before* the
work rather than after.** `Equip {§}` was the largest sentence shape in the corpus — 563 cards, next
one 342 — and a 400-card sample of what it blocked said 248 of them declined on nothing but
equipment-shaped lines, predicting ~350 whole cards. The band delivered **65** (6,335 → 6,400), and
the gap is one word: the sample counted "Equipped creature gets +2/+2 and has trample **and**
lifelink" as an equipment shape, where the grammar's joined sentence takes one keyword and not a run.
So a per-card sample predicts which *cards* a band reaches and still says nothing about which *lines*
it finishes — the same failure mode as the token rank, one level up. What the band did buy is not in
the coverage number: `CardDefinition.equipCost` is the first field outside `CardScript` the
differential compares, and the attachment-noun normalization means every present and future `Statics`
rule reads Equipment and Auras alike without knowing there are two card classes.

The residue is measured and small, which is the other half worth recording: 385 cards decline on
nothing but an "Enchanted creature …" sentence, their largest single shape is 10 cards, and
generalizing the joined form to a keyword *run* is worth 22. There is no fourth large family behind
this one.

**Acceptance:** POR, LEA and a modern set (DFT or FDN) each report fineness; the per-set whole-render
rate is directly comparable to `:mtgish-tooling`'s `gN` figure in the coverage dashboard.

---

## Phase 3 — Differential gate against the hand-written corpus ✅ HARNESS SHIPPED

Turn the implemented corpus into the semantic oracle. This is what catches the reversible-but-wrong
class that the touchstone structurally cannot. **Brought forward ahead of Phase 2** per the MVP
above: the gate has to exist before grammar breadth, or the breadth is unverified.

**Outcome.** `just assay-differential` runs over all 8,874 committed goldens. Of those, 930 clear
every scoping guard and are compared: **924 confirmed (99.4%), 6 classified divergences**. It found
the predicted class on its first run — multi-quality protection read as one ability where CR 702.16g makes it two,
reversible and wrong — plus two "one concept, two spellings" findings in the SDK. All five opening
divergences are now fixed: the grammar reads a joined quality list as several abilities (and
generalized to subtypes, three-way Oxford lists, and hexproof per CR 702.11f), and the dead
`KeywordAbility.Flanking` object is deleted from `mtg-sdk` — it overrode no `keyword`, so a card
authored with it would have done nothing. Details in [`oracle-assay/README.md`](../../oracle-assay/README.md).

The count is a *checkpoint*, not a property, and it behaved like one immediately: the first band of
spell rules took it from 0 to 8, of which six were the gate's own slot-name normalization colliding
with a field called `target`, one was the positional-versus-named target idiom (now folded, on the
SDK's own statement that they are the same link), and one is a standing finding —
`TargetCreatureOrPlaneswalker` versus the general filtered target, two fully-wired *parallel* engine
paths, deliberately not folded.

**Five guards, most of them found by the gate lying to itself once.** Assay must read every *line*;
the golden's text must be the *same text* Scryfall serves (compared normalized, since goldens carry
reminder text inconsistently); the definition must use only *modelled slots*; and the card's lines
must *fold into one card* — two lines that both parse as the spell effect mean a sequence the grammar
cannot spell, which used to throw and now counts; and the card must carry no *unread abilities*,
since a keyword the SDK lowers at authoring time puts an ability in the script that no text line
prints. Every card failing one lands in a named bucket rather than being confirmed.

**And the land band was where it paid the MVP's outstanding clause.** Opening `activatedAbilities`
and `replacementEffects` took the compared population from 653 to 890 and found **two genuine bugs
in hand-written cards** — Voltaic Construct untapping any creature *or* artifact where the text says
"artifact creature", and Dwarven Miner destroying basic lands where the text says "nonbasic". Both
are generated renders that dropped a clause, the same shape as Meteor Golem. That closes the "at
least one genuine bug found in a hand-written card" clause, three times over.

**The aura band is the first new card class that added no divergence at all**, and that is worth
recording rather than glossing. Opening `staticAbilities` and `auraTarget` took the compared
population from 890 to 930 and every one of the 40 agreed; a by-hand sweep of every golden printing
one of the three aura sentences — comparing the printed numbers and keyword against the card's own
`staticAbilities`, independent of whether Assay covers the whole card — found no disagreement
either. The pattern that held for three classes ("a divergence appears the first time the grammar
reads a class") is not a law: an aura in this band is two lines with nothing to drop, where every bug
the gate has found was a clause lost *inside* a filter on a longer sentence. Widening the guards was
still the expensive half — `REQUIREMENT_KEYS` needed `auraTarget` or all 40 would have diverged over
an id in neither model, and `carriesUnreadAbilities` needed a `staticAbilities` count or the
hand-rolled-affinity cards would have.

1. **`gate/Differential.kt`** — done, over keyword abilities plus `spellEffect`,
   `targetRequirements`, `triggeredAbilities`, `activatedAbilities` and `replacementEffects`. The
   comparison grows with the grammar, and the guards above are what keep each addition honest. Static
   abilities follow as Phase 2 reaches them.
2. **An explicit fold list** — done, as `Folds` in the same file, currently one entry: a bare
   `Keyword` implied by a parameterized `KeywordAbility` of the same keyword, which is a
   `CardDefinition` index entry the SDK populates on purpose rather than a second ability. Reviewed,
   never grown silently.
3. **Triage every divergence.** Ongoing, and the point. The opening five are closed: three were the
   protection-join parser bug, two were the flanking spelling — the first a bug in Assay, the second
   a dead type in the SDK. The land band added six, of which two are the genuine card bugs above,
   one was the gate's own slot normalization not reaching a requirement declared inside an ability
   (fixed — `ContextTarget`'s index is per-owner, and a card-wide counter stopped at the root), and
   three are a new standing SDK finding: mana-ability-ness is carried by two fields and 24
   hand-written abilities set only one of them.

**The oracle is a file read, not a dependency.** `:oracle-assay` still depends on `:mtg-sdk` alone.
The goldens under `mtg-sets/src/test/resources/snapshots/cards/` are data, decoded by `mtg-sdk`'s own
`CardLoader` — which is why this phase cost far less than the plan assumed.

**Acceptance:** divergences enumerated and each one classified as parser bug / card bug / fold. The
count matters less than the fact that none are unexplained. — **Met for the keyword class.**

---

## Phase 4 — Renderer

Model → `cardDef` source, as a pretty-printer rather than a string-assembler.

1. **`render/CardDefPrinter.kt`** — typed model → Kotlin, deriving imports by scanning emitted
   symbols (the approach `emitter/Shells.kt` already uses and which works well).
2. **`render/Folds.kt`** — recognise model shapes and emit the idiomatic `Patterns.*` /
   `Effects.*` spelling. **A fold is admissible only if the compiled result expands back to an
   identical model**, so folds become checked rather than trusted.
3. **Re-point `mtg-sets:verifyGeneratedCards`** at a model comparison:
   `deserialize(compile(render(m))) == m`.

**Acceptance:** the fourth gate is green for every card Assay renders whole.

---

## Phase 5 — Per-set cutover

No flag day. For each set, when Assay's whole-render rate exceeds `:mtgish-tooling`'s, switch that
set's generation over and record the pair of numbers in the PR. Always reversible: both generators
remain runnable throughout.

Start with a calibrated set (POR) where the incumbent's fidelity gate is already trusted, so the
first cutover is measured against a known-good baseline rather than a guess.

---

## Phase 6 — Retire the bridge

Only once no set generates from mtgish any more:

1. Drop the mtgish corpus download and `mtgish-tooling/data/mtgish.lines.json`.
2. Delete the capability dictionary (`coverage/bridge/`) and `emitter/TargetRecovery.kt`.
3. Re-point the coverage dashboard at Assay's decline report — same TUI, better signal, because
   declines name Argentum capabilities instead of mtgish tags.

**The dashboard survives; only its data source changes.** Backlog triage ("which feature unlocks the
most cards?") is the module's real value and is unaffected.

---

## Follow-on, not in scope here

- **User-authored cards.** A JVM parser over SDK types lets Argentum accept pasted Scryfall-shaped
  JSON and return a playable definition. Product feature; needs its own design.
- **The SDK vocabulary findings.** The reach split, half-migrated cardinality, boolean-knob
  accretion, and the `…ThisWay` curve are written up in the design doc's *What this says about
  `mtg-sdk`* section. The proposed one-line policy — *a new mechanic may not mint a `CardSource`
  variant for a value an earlier step could have bound* — belongs in
  [`sdk-design-principles.md`](../sdk-design-principles.md) and should be adopted (or rejected) on
  its own merits, independently of whether Assay gets built.

## Sizing, honestly

The grammar is open-ended: `:mtgish-tooling` is ~21.7k lines and does not cover everything, and
Assay's rules cost roughly 2× a parse-only rule. Phases 1 and 2 are the ones worth committing to up
front; everything after is gated on Phase 2's fineness curve being convincing.

Phase 1 is the real decision point. It is self-contained, produces a corpus-wide number on its own,
and if the round trip doesn't hold there, the cheapest possible exit has already been taken.
