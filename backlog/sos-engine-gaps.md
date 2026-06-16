# Secrets of Strixhaven — Engine Gap Analysis

Cross-reference of the **271 unique SOS cards** against the engine's actual capabilities (SDK reference +
source verification, June 2026). Generated to scope what must be built before the set can be completed.

**Status:** 0 / 271 implemented (0%). The set has no `definitions/sos/` folder yet — this is a forward-looking
scope of the engine work required.
Card list pulled from Scryfall (`set:sos`, expansion, 368 printings → 271 unique cards). Oracle text quoted
from Scryfall. Sibling sets (`soa` Mystical Archive, `soc` Commander, `ysos` Alchemy) are out of scope here.

## Bottom line

SOS is a **spellslinger / "Strixhaven mage school"** set built around **seven new mechanics**, all of which
need engine work. None are supported today. The mechanic with by far the most leverage is **Prepared** (38 cards
— a brand-new double-faced layout). After that the set is mostly standard spellslinger material (instants/sorceries,
small creatures with spell-cast triggers, dual lands, planeswalkers) that is buildable once the shared primitives
land.

The genuine gaps cluster into three buckets:

1. **One new DFC layout + permanent status** (Prepared) — the headline lift, touches casting, projection, server DTO, client.
2. **Three new spell-cast trigger / dynamic-amount primitives** (Repartee target-predicate, Opus mana-spent payoff,
   Converge color-count) — small SDK additions, each unlocking ~10 cards.
3. **A scatter of one-offs** (Paradigm recurring free-cast, Grandeur keyword, multi-turn skip, Lesson subtype).

### Already supported — no new engine work

Verified present in source; these compose the bulk of the set's non-mechanic text:

- **DFC infrastructure** — `CardLayout` already models `ADVENTURE`, `OMEN`, `MODAL_DFC`, `SPLIT`; `CastSpell.faceIndex`
  routes per-face casting; `cardFaces: List<CardFace>` holds spell-side script. Prepared is a *new sibling layout*,
  not a from-scratch DFC system. (`CardDefinition.kt:64–110`, `GameAction.kt:60–90`, `CastSpellHandler`/`StackResolver`)
- **Total mana spent to cast a spell** — fully tracked. `DynamicAmount.TotalManaSpent` (`DynamicAmount.kt:271`),
  surfaced on `SpellCastEvent.totalManaSpent` and per-color buckets on `SpellOnStackComponent` /
  `CastRecordComponent`. Powers `Expend` already.
- **"You gained life this turn" + amount** — `Conditions.YouGainedLifeThisTurn` (`Conditions.kt:565`) and
  `DynamicAmount.TurnTracking(player, LIFE_GAINED)`. This means **Infusion is essentially free** (see Tier 2 §8).
- **Copy-a-card-and-cast-it-for-free pipeline** — `CopyCardIntoCollectionEffect` + `CastFromCollectionWithoutPayingCostEffect`
  (+ the any-number loop variant), with the Rule 707.10a phantom-copy SBA. Built for Shiko/Kotis; the spine that
  Prepared and Paradigm both reuse.
- **Enters-with-dynamic-counters** — `ReplacementEffect.EntersWithDynamicCounters(count: DynamicAmount, …)`
  (`ReplacementEffect.kt:314`), proven by Stag Beetle. Converge needs only a new `DynamicAmount` to feed it.
- **Coin flip** — `FlipCoinEffect` (`CompositeEffects.kt`). (Ral Zarek ultimate.)
- **Spell-cast triggers** — `Triggers.YouCastInstantOrSorcery` / `YouCastSpell` / `NthSpellCast` / the generic
  `youCastSpell(filter, requires)` with the `SpellCastPredicate` sealed interface. Opus/Repartee/Increment all
  *extend* this, they don't invent it.
- **Conditional / composite resolution** (`ConditionalEffect`, `CompositeEffect`), **emblems**
  (`CreatePermanentEmblemEffect` — Professor Dellian Fel ult), **planeswalker loyalty framework** (both PWs need
  only their printed abilities), **Flashback, Surveil, Treasure, Cascade, Crew, Affinity, Mill, keyword counters,
  delayed triggers, dual/utility lands** — all present.

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline mechanics (highest leverage)

### 1. Prepared (38 cards) — ✅ **DONE** (was: the big lift)

> **Implemented.** `CardLayout.PREPARE` + `Keyword.PREPARED` + `prepare(name) { }` DSL; ETB marks the
> permanent (`PreparedComponent`) and exiles a castable copy of the prepare spell (`PreparedSpellCopyComponent`
> + a `MayPlayPermission`, exempt from the 707.10a phantom-copy SBA); casting the copy (face 0, from exile)
> unprepares the creature and the copy ceases to exist; the copy is cleaned up if the source leaves.
> Surfaced to the client as `ClientCard.isPrepared` (a "Prepared" badge) plus the castable exile copy.
> Tests: `PrepareMechanicScenarioTest`. Cards so far: Adventurous Eater, Landscape Painter.

New double-faced layout: **front = creature, back = instant/sorcery** (the backs are the Mystical-Archive
all-stars — Ancestral Recall, Brainstorm, Lightning Bolt, Demonic Tutor, Reanimate, Swords to Plowshares, …).
Oracle: *"This creature enters prepared. (While it's prepared, you may cast a **copy** of its spell. Doing so
unprepares it.)"* A few cards (Emeritus of Truce, …) *conditionally* become prepared via an ETB clause.

The card is cast/plays as the creature. The spell side is **never the card itself** — while the permanent carries
a "prepared" status its controller may cast a *copy* of the back face (the original card stays on the battlefield),
and resolving/casting that copy clears the status. This is structurally unlike Adventure (card moves to exile),
Omen (card shuffles), and MDFC (choose a face at cast time).

**What exists to compose:** the `CardLayout`/`cardFaces`/`faceIndex` machinery; the copy-and-cast-for-free pipeline
(`CopyCardIntoCollectionEffect` + `CastFromCollectionWithoutPayingCostEffect`); the activated-ability enumerator.

**Genuine gaps:**
- **`CardLayout.PREPARED`** + a `prepare`-layout loader (Scryfall layout is literally `prepare`), so the back face's
  script/cost/type line are parsed onto `cardFaces[0]` like Adventure/Omen.
- **A `prepared` status marker** on the permanent — a boolean component (pattern exists: `RingBearerComponent`,
  `RoomComponent`, face-down flag). Set true on ETB (printed `Keyword.PREPARED`) or via the conditional ETB clause.
- **An optional "cast a copy of my spell face" action** surfaced as a legal action *while the status is set* — most
  cleanly modeled as a free-cost activated ability gated on the marker, so the existing `ActivatedAbilityEnumerator`
  surfaces it; resolving it synthesizes a copy of the back face on the stack (graveyard on resolution — not a token,
  not an Adventure re-cast) and clears the marker.
- **Cross-layer plumbing** (per the add-feature rule): projection of the prepared flag, server DTO + client badge so
  the player sees the prepared state and the "cast spell" option, and the keyword reminder text.

This is the one item that warrants the full add-feature treatment (SDK type designed for the *next* prepared card,
not just one).

### 2. Repartee (12 cards) — ❌ spell-cast trigger gated on "targets a creature"

*"Whenever you cast an instant or sorcery spell **that targets a creature**, …"* The engine has the
`YouCastInstantOrSorcery` trigger and the `SpellCastPredicate` sealed interface, but **no predicate inspects the
cast spell's targets** — `SpellCastEvent` carries only `targetNames` (display strings), not target entities/types.

**Gap:** add a `SpellCastPredicate.TargetsMatch(GameObjectFilter)` (matcher reads the spell's `TargetsComponent`
and evaluates the filter against the targeted entities), then `flurry`-style `repartee { … }` builder that wires
`youCastSpell(instantOrSorcery, requires = TargetsMatch(Creature))` + the reminder-text prefix.
→ Graduation Day, Informed Inkwright, Inkshape Demonstrator, Rehearsed Debater, Stirring Hopesinger, …

### 3. Opus (10 cards) — ✅ **DONE** (mana-spent payoff tier on a spell-cast trigger)

> **Implemented.** The shared §8 primitive shipped, so Opus composes directly:
> `ConditionalEffect(Compare(ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL), GTE, 5), big, small)`. An
> `opus { }` ability-word builder exists (ability word — adds no keyword). Cards: Expressive Firedancer.

*"Whenever you cast an instant or sorcery spell, **<effect>. If five or more mana was spent to cast that spell,
<bigger effect> instead.**"* The trigger exists and `DynamicAmount.TotalManaSpent` exists — but it reads the mana
spent on **this** (the source's own) spell, not on the **triggering** spell. There is no
`Condition.Compare` input that reads the *triggering spell's* total mana spent at trigger-resolution time.

**Gap:** expose the triggering spell's `totalManaSpent` to the resolving ability (e.g.
`DynamicAmount.ContextProperty(TRIGGER_SPELL_MANA_SPENT)` mirroring the existing trigger-context properties), then
the payoff is plain `ConditionalEffect(Compare(…, GTE, 5), big, small)`. Add an `opus { … }` builder + reminder text.
→ Deluge Virtuoso, Exhibition Tidecaller, Muse Seeker, Expressive Firedancer, Molten-Core Maestro, …

### 4. Converge (9 cards) — ❌ count of distinct colors of mana spent

*"…with a +1/+1 counter on it for each **color of mana spent to cast it**"* and *"…mana value less than or equal
to the **number of colors of mana spent** to cast this creature."* All colors actually spent are tracked
(`SpellOnStackComponent.manaSpent{W/U/B/R/G/C}` → `CastRecordComponent`), and `EntersWithDynamicCounters` already
takes a `DynamicAmount`. **What's missing is the count primitive** (Sunburst, classically).

**Gap:** add `DynamicAmount.DistinctColorsManaSpent` (count of the five color buckets that are non-zero), evaluated
in `DynamicAmountEvaluator`. Feeds `EntersWithDynamicCounters` directly. For the exile-by-color-count variant, add a
sibling card predicate `ManaValueAtMostColorsSpent` (the existing `ManaValueAtMostEntityManaSpent` compares total
mana, not color count). Then a `converge { … }` builder.
→ Rancorous Archaic, Sundering Archaic, Transcendent Archaic, Magmablood Archaic, Together as One, Arcane Omens, …

### 5. Increment (9 cards) — ✅ **DONE** (self-growing-by-mana-spent keyword)

> **Implemented.** `Keyword.INCREMENT` + the `increment()` `CardBuilder` DSL (`dsl/mechanics/IncrementDsl.kt`):
> a `YouCastSpell` trigger whose intervening-if (CR 603.4) compares the triggering spell's mana spent
> (`DynamicAmount.ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL)`, the §8 primitive) against
> `Min(EntityProperty(Source, Power), EntityProperty(Source, Toughness))` with `GT`, then puts a +1/+1 counter
> on the source. "greater than power or toughness" = greater than the *smaller* stat, hence `Min`; the bar is
> read from projected P/T so it rises as the creature grows. Composition only — no new engine type. Tests:
> `IncrementMechanicScenarioTest`. Cards so far: Cuboid Colony, Hungry Graffalon. mtgish bridge + emitter wired.

*"Increment (Whenever you cast a spell, if the amount of mana you spent is greater than this creature's power or
toughness, put a +1/+1 counter on this creature.)"* The trigger (`YouCastSpell`) and the comparison inputs both
exist — `DynamicAmount.TotalManaSpent` vs `EntityProperty(Source, Power/Toughness)`. The composition (compare the
*triggering* spell's mana spent against the source's projected P/T as an intervening-if) is **not** expressible until
the triggering-spell mana-spent value from §3 is available; once it is, Increment is a small keyword builder
(`increment { }`) forcing `youCastSpell` + a `Compare(triggerSpellManaSpent, GT, min(power, toughness))`
trigger-condition + `AddCounters(+1/+1)`.
→ Pensive Professor, Tester of the Tangential, Textbook Tabulator, Ambitious Augmenter, Berta Wise Extrapolator, …

*(§3 + §5 share one new primitive: the triggering spell's total mana spent. Build it once.)*

### 6. Paradigm (5 cards — Lessons) — ❌ recurring free-cast from exile, keyed to spell name

*"Then exile this spell. After you first resolve a spell with this name, you may cast a **copy** of it from exile
without paying its mana cost at the beginning of each of your **first main phases**."* A recurring per-turn impulse
of a self-exiled spell. The copy-and-cast-for-free pipeline exists; the gaps are the recurrence + the name gate:

**Gaps:**
- **Self-exile-on-resolution** that installs a persistent per-player permission (composable, but no existing
  "exile this spell + remember it" Paradigm shape).
- **A recurring (non-one-shot) "at the beginning of your first main phase each turn" trigger.** `DelayedTriggerTiming`
  has `NEXT_END_STEP` / `NEXT_TURN` but **no recurring first-main-phase timing**, and no "precombat-only" distinction.
- **"After you first resolve a spell with this name" gate** — there is no per-name spell tracking
  (`spellsCastThisTurn` is a global int). Needs a name-keyed "have I resolved a Paradigm spell with this name"
  marker so the recurring permission only switches on after the first resolution and the copy is cast per turn.
→ Restoration Seminar, Echocasting Symposium, Decorum Dissertation, Improvisation Capstone, Germination Practicum.
*(Also: add `Subtype.LESSON` — see Tier 2 §10. No Learn cards in the set, so Lesson is a plain subtype.)*

### 7. Infusion (12 cards) — ✅ **buildable today** (no new engine work)

*"Infusion — If you gained life this turn, <extra effect>"* — sometimes a spell rider, sometimes an end-step
triggered ability with X = life gained this turn. Every piece exists: `Conditions.YouGainedLifeThisTurn`,
`DynamicAmount.TurnTracking(You, LIFE_GAINED)`, `ConditionalEffect`/`CompositeEffect`, "beginning of your end step"
triggers. **No gap** — listed here only because it's a named mechanic. Add an `infusion { }` reminder-text builder
for consistency, but it composes existing primitives.
→ Foolish Fate, Moseo Vein's New Dean, Poisoner's Apprentice, Tragedy Feaster, Withering Curse, Efflorescence, …

---

## Tier 2 — Small recurring primitives

8. **Triggering-spell mana-spent context value.** ✅ **DONE.** Shared dependency of **Opus (§3)** and
   **Increment (§5)**. `DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL)` is
   populated in `TriggerContext` from `SpellCastEvent.totalManaSpent`, threaded through the triggered-ability
   component → `EffectContext.triggerManaSpentOnTriggeringSpell`, and read by `DynamicAmountEvaluator`. It also
   resolves inside an intervening-if (`TriggerMatcher.filterByTriggerCondition` populates it), so it works in both
   the resolving-effect and trigger-condition paths. One primitive, two mechanics.

9. **Distinct-colors-of-mana-spent dynamic amount.** ❌ For **Converge (§4)**. `DynamicAmount.DistinctColorsManaSpent`
   + evaluator + (optional) `ManaValueAtMostColorsSpent` card predicate. (Sunburst-shaped; the per-permanent
   `Aggregation.DISTINCT_COLORS` analog for spell-cast mana.)

10. **`Subtype.LESSON`.** ❌ The 5 Paradigm cards are `Sorcery — Lesson`. No `LESSON` subtype constant exists. Trivial
    add (no Learn mechanic in this set, so it's a plain, non-functional subtype — but the type line must parse).

11. **Multi-turn skip ("skips their next X turns").** ❌ `SkipNextTurnEffect` exists but is a singleton boolean
    component — no count, no `DynamicAmount`. Needs `SkipNextTurnComponent.turnsToSkip: Int` + a `turns: DynamicAmount`
    parameter so Ral Zarek's ultimate ("skips their next X turns, X = heads") works. (One card, but no other path.)

---

## Tier 3 — One-off complex cards

12. **Grandeur keyword.** ❌ Page, Loose Leaf — *"Grandeur — Discard another card named Page, Loose Leaf: <effect>.
    Activate only as a sorcery."* No `GRANDEUR` keyword and no "discard another card **with this card's name**" ability
    cost. `AdditionalCost.DiscardCards` discards by *filter*, but there's no "same-name-as-source" card filter.
    Needs a `CardFilter.SameNameAsSource` (or `NamedLike(self)`) + the keyword's sorcery-timing activated-ability shape.

13. **Ral Zarek, Guest Lecturer** ❌ (depends on §11) + ✅ coin-flip exists. *"Flip five coins. Target opponent skips
    their next X turns, X = heads."* Coin flip and "count heads" compose today; the blocker is the multi-turn skip (§11).
    His other abilities (Surveil, discard, reanimate MV≤3) are standard.

14. **Professor Dellian Fel** ✅ buildable. +2 gain life / 0 draw-and-lose-1 / −3 destroy / −6 emblem
    ("Whenever you gain life, target opponent loses that much life" — `CreatePermanentEmblemEffect` + life-gain trigger
    + `DynamicAmount` for "that much"). No new framework; listed to confirm the second PW is not a gap.

15. **Wisdom of Ages** ✅ "no maximum hand size" — confirm `NoMaximumHandSize` static exists (common; verify during build).

16. **Quill-Blade Laureate // Twofold Intent**, **Expressive Firedancer**, **Growth Curve**, **Practiced Offense** —
    "double"-text cards (double counters / double damage / doubling). Existing one-shot counter-doubling and
    double-damage primitives likely cover these; spot-check each against the relevant existing effect during build,
    not flagged as gaps yet.

---

## Recommended build order

1. **Infusion** (✅ today) + **Lesson subtype** (§10) — warm-up; unlocks 12+ cards with zero or trivial engine work.
2. ✅ **Triggering-spell mana-spent value (§8)** → unlocks **Opus (§3)** and **Increment (§5)** together (~19 cards). **Done.**
3. **Distinct-colors-spent (§9)** → **Converge (§4)** (~9 cards). **Repartee target-predicate (§2)** (~12 cards) in
   parallel — both are isolated SDK additions.
4. ✅ **Prepared (§1)** — the big cross-layer feature (38 cards). **Done** (SDK→engine→DTO→client + tests).
5. **Paradigm (§6)** — recurring free-cast + name gate (5 Lessons).
6. **Tier-3 one-offs** (multi-turn skip §11, Grandeur §12) as Ral / Page come up.

Prepared alone is ~14% of the set; the four spell-cast/mana primitives (§2, §8/§3, §5, §9/§4) cover another ~40 cards.
Once those land, the remaining ~150 cards are standard spellslinger material buildable with today's SDK.
