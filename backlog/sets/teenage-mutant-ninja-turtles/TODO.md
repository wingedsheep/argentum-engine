# Teenage Mutant Ninja Turtles (TMT) — Implementation Plan

> **Every card must be implemented perfectly — exactly as stated in the rules.** No
> approximations, no "close enough", no silently dropped clauses. Each card's behavior must
> match its oracle text (from `tmt_full.json` in the repo root) and the Comprehensive Rules
> (`MagicCompRules_20260417.pdf`) in full, including edge cases, timing, and interactions.
> A card is not done until its scenario test proves the rules-correct behavior.

Verify status anytime with: `scripts/card-status --set TMT` (and `--list --set TMT`).

## Status

179 / 190 implemented (basics excluded — handled by `basicLandsFallback`). See
`cards.md` for the full checklist (the authoritative status); the per-card
commits all carry `flavorText` in metadata.

> **2026-06-25 second sweep — eight more "feature-gated" cards were composable.**
> Debunked: **Leatherhead** (Slumbering Walker's reflexive remove-a-counter +
> Dawning Purist's combat target; HEXPROOF counter already exists), **Lita**
> (`ModalEffect.chooseOneNotYetChosen`), **Donatello, Mutant Mechanic**
> (`MoveAllLastKnownCounters` + `TriggeringEntityHadCounters` + `BecomeCreature`),
> **Mondo Gecko** (`ChooseColorThen` feeding both `ChangeColorToChosen` and
> `GrantHexproofFromChosenColor` + `colorsAmongPermanents`), **Tokka & Rahzar**
> (`CompareAmounts(ManaSpent < ManaValue)` over `AnyPlayerCastsSpell` — Pyrostatic
> Pillar idiom), **Raphael, Ninja Destroyer** (persistent mana is the engine default —
> pools empty only at end of turn — so Enrage is a plain dynamic `AddMana`),
> **Turtles in Time** (`ForEachPlayer(Player.Each)` per-player shuffle+draw + `selfExile`),
> and **Krang & Shredder** (`GatherUntilMatch(Nonland)` → linked exile + Disappear
> `GrantMayPlayFromExile`/`GrantPlayWithoutPayingCost`).
>
> **The genuinely-remaining 11 each need a NEW engine feature** (all re-verified
> 2026-06-25 — none is composable):
> - **The Last Ronin** — delayed *attack* trigger must bind the attacker (root-caused
>   above: `fromEvent(AttackersDeclaredEvent)` is empty + no per-attacker fan-out). Card
>   .kt + combat test ready to restore once fixed.
> - **Leonardo, Sewer Samurai** — needs an engine fix (verified). `MayCastFromGraveyard` +
>   double strike + sneak all compose, BUT "creatures you cast from your graveyard enter with a
>   finality counter" does not: a `selfOnly = false` `EntersWithCounters(FINALITY, condition =
>   WasCastFromGraveyard)` (and the `TriggeringEntityEnteredOrWasCastFromGraveyard` variant) on
>   Leonardo leaves the cast creature with 0 counters — the condition is evaluated against the
>   replacement's source (Leonardo), not the entering creature, so the finality idiom only works
>   `selfOnly = true` (a card's own replacement, Hundred-Battle Veteran). Fix: make non-self
>   `EntersWithCounters` evaluate its `condition` against the affected/entering entity, OR add an
>   `entersWithCounter` field to `MayCastFromGraveyard` wired through the graveyard-cast path.
> - **Ninja Teen L3** — grant Sneak to graveyard creature cards + cast them from GY via Sneak.
> - **Mikey & Don** — cast Mutant/Ninja/Turtle from top of library; creatures cast this way
>   enter with a +1/+1 counter (no cast-from-top counter rider).
> - **Party Dude L3** — "whenever one or more of your opponents are attacked" trigger
>   (L1 each-player Food is now composable via `Player.Each`; L3 is the blocker).
> - **Rat King** — return target creature card + all same-named cards from your graveyard.
> - **The Cloning of Shredder** — create a token that's a copy of a card in this Saga's
>   linked exile (no token-copy-of-linked-exile effect).
> - **Don & Raph** — grant the next noncreature spell you cast affinity for artifacts.
> - **Purple Dragon Punks** — "spend only to cast an artifact spell or activate **any**
>   ability" mana (`CardTypeSpellsOrAbilitiesOnly` ties abilities to the card type).
> - **North Wind Avatar** + **Turtles Forever** — wishboard (cards from outside the game).

> **2026-06-25 sweep — six more "feature-gated" cards were actually composable.**
> Re-verifying each supposed gap against real primitives debunked: **Turtle Van**
> (`crewedOrSaddledSourceThisTurn()` filter + `DoubleCounters`), **Michelangelo's
> Technique** (`SelectionRestriction.TotalManaValueAtMost`), **Fugitive Droid**
> (`CardPredicate.TargetsMatching` — counter target spell that targets your stuff,
> the Teferi's Response idiom), **Northampton Farm** + **Raphael, Most Attitude** +
> **Koya, Death from Above** (the `CardSource.FromLinkedExile` / `MoveCollection(linkToSource)`
> / `GrantMayPlayFromExile` / `PayOrSuffer` linked-exile family — all already built).
>
> **The genuinely-remaining 19 each need a real new engine primitive** (oracle text
> re-read 2026-06-25; none is "just authoring"):
> - **Leatherhead, Swamp Stalker** — remove *one counter of any kind* (chooser picks) +
>   reflexive "when you do" (HEXPROOF counter + entersWith already exist).
> - **Purple Dragon Punks** — *any*-ability mana ("artifact spell or activate an ability";
>   `CardTypeSpellsOrAbilitiesOnly` ties abilities to the card type, too narrow).
> - **The Last Ronin** — needs a small **engine fix** (add-feature), root-caused: chapters I
>   (`DestroyAll`) and II (`mill` + `ReflexiveTrigger` return) compose cleanly, and chapter III's
>   spec is correct (`CreateDelayedTriggerEffect(trigger = attacks(filter=youControl,
>   requires=Alone, binding=ANY), …)` + `EffectTarget.TriggeringEntity` — the proven Thoughtweft
>   Imbuer / Doran "a creature you control attacks → do X to it" shape). But a combat scenario
>   test left the lone attacker with 0 counters because **`TriggerContext.fromEvent` maps
>   `AttackersDeclaredEvent -> TriggerContext()` (empty)** and `TriggerDetector.detectEventBasedDelayedTriggers`
>   doesn't iterate the declared attackers — so a *delayed* attack trigger never binds the
>   attacking creature to `TriggeringEntity` (regular attack triggers bind it per-attacker in the
>   non-delayed path). Fix: in `detectEventBasedDelayedTriggers`, when the event is
>   `AttackersDeclaredEvent`, fan out one PendingTrigger per attacker matching the spec's filter,
>   each with `triggeringEntityId = that attacker`. Reusable for any "this turn, whenever a
>   creature attacks, do X to it" delayed trigger. The card .kt and a combat scenario test are
>   ready to restore once the engine binds the attacker.
> - **Tokka & Rahzar** — "mana spent to cast it was less than its mana value" trigger condition.
> - **Lita, Little Orphan Amphibian** — modal where each mode is once-per-turn ("hasn't been chosen this turn").
> - **Mondo Gecko** — become-chosen-color + "draw a card for each color among permanents you control" dynamic.
> - **Donatello, Mutant Mechanic** — move a dying artifact's last-known counters to a target.
> - **Krang & Shredder** — each opponent exiles from top until a nonland (reveal-until).
> - **Party Dude** — "your opponents are attacked" trigger (+ each-player Food).
> - **Raphael, Ninja Destroyer** — Enrage mana that persists past step/phase cleanup.
> - **Rat King** — return target creature card + all same-named cards from your graveyard.
> - **Leonardo, Sewer Samurai** — cast creatures from GY (static), entering with a finality counter.
> - **Ninja Teen L3** — grant Sneak to graveyard creature cards + cast them from GY via Sneak.
> - **Mikey & Don** — cast from top of library; creatures cast this way enter with a +1/+1 counter.
> - **The Cloning of Shredder** — token copy of a card in this Saga's linked exile.
> - **Don & Raph** — grant the next noncreature spell you cast affinity for artifacts.
> - **North Wind Avatar** + **Turtles Forever** — wishboard (cards from outside the game).
> - **Turtles in Time** — each-player-MAY Timetwister (per-player shuffle-hand+gy / draw 7).

> **2026-06-24 run — the gap list below was badly overestimated.** A sweep added
> 38 cards by discovering that most "blocking gaps" were *already supported* and
> the cards just needed authoring. Additional debunked gaps from the continued
> sweep: **Gap P** (sacrifice-unless-discard = `PayOrSufferEffect`, Bebop &
> Rocksteady), **Gap R** (mass gain-control = `ForEachInGroup(GainControl)`,
> Insurrection shape, Broadcast Takeover), **Gap FF** (becomes-artifact-creature =
> `BecomeCreature` + `AddCardTypeEffect("ARTIFACT")`, Mind Transfer Protocol),
> **Gap CC** (tap-land-for-mana = `Triggers.landTappedForMana`, Groundchuck &
> Dirtbag), **Gap JJ** (multi-subtype mana = `SubtypeSpellsOnly(set)`, Turtle Lair),
> plus the Mutagen cost-reduction static (`ReduceActivatedAbilityCost`), `+1`
> counter rider (`ModifyCounterPlacement`), Chrome Dome
> (`CreateTokenCopyOfTarget(addedKeywords, sacrificeAtStep)`), and the three Class
> cards (Does Machines, Cool but Rude, Leader's Talent). Final sweep also shipped:
> **Gap II** (tap-or-untap choice = `MayEffect(ModalEffect(tap, untap))`, Sewer-veillance
> Cam), Retro-Mutation (`TransformPermanent`+`SetBasePowerToughnessStatic`+`CantAttack`+
> `LoseAllAbilities` aura), Kitsune (`ExchangeControl`), Venus (`TakesDamage` +
> `dealsDamage` factory + `MayPayMana`), and Everything Pizza (multi-target composite).
>
> Also debunked after that note: **Grounded for Life** (`ModifySpellCost` +
> `CostReductionSource.FixedIfAnyTargetMatches(Creature.tapped())`, Quicksand
> Whirlpool shape) and **Ray Fillet** (`Costs.RemovePlusOnePlusOneCounters` — the
> counter-typed cost already existed). 27 cards remain.
>
> The remaining 27 cards each need a genuinely new engine feature (each verified
> individually this run — none is "just authoring"): dynamic pay-life (Madame Null),
> card-types-cast-this-turn amount
> (April O'Neil Hacktivist), counter-spell-that-targets-a-filter (Fugitive Droid —
> its can't-be-blocked half is `ConditionalStaticAbility` +
> `PermanentTypeEnteredBattlefieldThisTurn`),
> grant-affinity-to-next-spell (Don & Raph), any-ability mana restriction (Purple
> Dragon Punks), crewed-this-turn filter (Turtle Van),
> remove-one-counter-of-any-kind action (Leatherhead), last-known
> counter transfer (Donatello Mutant Mechanic), cast-from-GY/top *with a counter rider*
> (Leonardo Sewer Samurai, Mikey & Don, Ninja Teen L3), total-mana-value-budget select
> (Michelangelo's Technique), "opponents are attacked" trigger (Party Dude), UEOT grant
> of play-from-linked-exile (Raphael Most Attitude), persistent mana / Enrage display
> (Raphael Ninja Destroyer), mode-not-chosen-this-turn (Lita), same-name reanimate (Rat
> King), reveal-until + cast-from-exile (Krang & Shredder), linked-exile copy across
> Saga chapters (The Cloning of Shredder), attacks-alone + mill-then-do (The Last Ronin),
> mana-spent-< -MV trigger (Tokka & Rahzar), linked-exile land (Northampton Farm),
> delayed-target ransom return (Koya), become-chosen-color + colors-among-permanents
> (Mondo Gecko), and wishboard (North Wind Avatar, Turtles Forever), plus the
> mass-bounce/shuffle/draw-7 Turtles in Time.
>
> Debunked gaps (now shipped composably):
> - **Gap B (Disappear)** — `Conditions.YouHadPermanentLeaveBattlefieldThisTurn`
>   + the per-controller all-permanent leave tracker already existed (LTR Shortcut
>   to Mushrooms). NO engine change. Shipped Foot Mystic, Insectoid Exterminator,
>   Michelangelo Game Master, Putrid Pals, Lord Dregg, Pizza Face, West Wind Avatar.
> - **Gap U (Class)** — the `classLevel(N, cost){}` DSL + the "becomes level N"
>   = `EntersBattlefield`-trigger-in-level-block idiom (Caretaker's Talent) already
>   existed. Shipped Does Machines, Cool but Rude, Leader's Talent.
> - **Gap AA (when-you-do reflexive trigger)** — `ReflexiveTriggerEffect` /
>   `MayEffect(IfYouDoEffect(...))` already existed. Shipped General Traag,
>   Paramecia Coloniex, Novel Nunchaku.
> - **Gap BB (greatest power among)** — `AggregateBattlefield(MAX, POWER)`. Go Ninja Go.
> - **Gap HH (can't be blocked by greater power)** — `powerGreaterThanEntity(Source)`.
>   Prehistoric Pet.
> - **Gap V (search-or-fail token)** — `IfYouDoEffect(search, ifYouDont=token)`.
>   Courier of Comestibles.
> - **Gap X (paired flicker)** — sequential `Move` over two targets. Don & Leo.
> - **"No multi-target zone selector"** — wrong: `TargetObject(count=2)` +
>   `ForEachTargetEffect` (INV Restock). Shipped Does Machines L2, Leonardo's Technique.
> - **Gap O-on-self (Technodrome)** — `CantAttack/BlockUnless(Compare(source power))`.
> - **Mutagen token (the one real feature built)** — unlocked the 7 pure cards +
>   Mutant Chain Reaction, Mutagen Man, The Ooze, Return to the Sewers,
>   Michelangelo Mutant BFF/Weirdness to 11. The Weirdness "+1 counter" rider is
>   `ModifyCounterPlacement(modifier=1)`; the cost-reduction static is
>   `ReduceActivatedAbilityCost`; owner-top-or-bottom is `PutOnTopOrBottomOfLibrary`.
> - **Flicker tapped-and-attacking (The Neutrinos)** — `ZonePlacement.TappedAndAttacking`.
> - **Chrome Dome** — `CreateTokenCopyOfTarget(addedKeywords=HASTE, sacrificeAtStep=END)`.
>
> The genuinely-remaining 38 cards each need a *real* feature (verify before
> assuming the gap is real — this list has cried wolf): dynamic pay-life (Gap GG,
> Madame Null), BecomeCreature-adds-Artifact (Gap FF, Mind Transfer Protocol),
> target-state cost reduction (Gap DD), tap-or-untap choice (Gap II), card-types-
> cast dynamic (Gap N), sacrifice-unless-discard (Gap P), counter-typed remove
> cost (Ray Fillet), cast-from-GY/top with a counter rider (Leonardo Sewer Samurai,
> Mikey & Don, Ninja Teen L3), counter-transfer-on-death (Donatello Mutant
> Mechanic), total-MV-budget select (Michelangelo's Technique), linked-exile copy
> across Saga chapters (The Cloning of Shredder), wishboard (North Wind, Turtles
> Forever), and assorted bespoke one-offs.

The remaining 71 cards mostly cluster on a handful of unresolved gaps:
- **Sneak** — RESOLVED (Gap A). The keyword, alt-cost plumbing, and
  "sneak cost was paid" flag are fully wired; 23 of 26 Sneak cards have
  shipped. Of the 9 that were previously called "pure authoring", **6 were
  genuinely composable and shipped** (`Dark Leo & Shredder`,
  `Karai, Future of the Foot`, `Kitsune's Technique`, `Michelangelo, Improviser`,
  `Raphael's Technique`, `Shark Shredder, Killer Clone`). The remaining **3 carry
  a SECOND engine gap beyond Sneak** — the earlier "no other blocker" note was
  wrong for them: `Leonardo's Technique` (up-to-two *target* graveyard reanimate
  with a per-target MV≤3 filter — no multi-target zone-card selector exists),
  `Leonardo, Sewer Samurai` (cast-creatures-from-graveyard *static permission* —
  missing), `Michelangelo's Technique` (look-at-8, put up to 2 with *total* MV≤6
  — aggregate-MV-budget selection; note `SelectCardsDecision.maxTotalManaValue`
  already exists at the decision layer, so this may be closer to composable than
  first thought). Each is its own `add-feature` PR.
- **Disappear** — 9 cards (Gap B — unresolved)
- **Alliance** — 4 cards left (Gap C — partly cleared; 6 shipped composably,
  the remaining 4 each have an *additional* unresolved blocker beyond Alliance
  itself: `Lita` → Gap E modal-not-yet-chosen, `Raphael, Most Attitude` → exile-
  and-may-cast chain, `The Neutrinos` → flicker-tapped-and-attacking, `The
  Neutrinos` listed once). Pure Alliance is no longer a blocker on its own.
- **Class** — 3 cards (Gap U — unresolved)
- **Mutagen token consumers** — ~5 cards (Gap W — unresolved)
- **Vehicles / Crew** — 1 card left (Gap J — partly cleared; `Turtle Blimp`
  shipped composably, `Turtle Van` still waits on Gap LL "creature that
  crewed this vehicle this turn" tracking)
- **Sagas** — 2 cards (Gap K — Saga primitive is wired, but each TMT Saga
  has its own separate blocker: `The Cloning of Shredder` → Gap MM
  `addedSubtypes` on `CreateTokenCopyOfTarget`; `The Last Ronin` → Gap AA
  "Mill X. When you do, …" sub-trigger + Gap NN "attacks alone this turn"
  trigger condition)
- Plus various one-offs from Gaps M and N–NN

Gaps **resolved across the runs so far**:
- **Gap A — Sneak — RESOLVED**: the full alternative-cost pipeline shipped.
  SDK: `Keyword.SNEAK`, `KeywordAbility.Sneak(cost)`, the `sneak("{cost}")`
  DSL helper on `CardBuilder`, the `ChoiceSlot.SNEAK` flag, and the
  `Conditions.SneakCostWasPaid` predicate. Engine: `SneakWindow` (the
  declare-blockers payment window that bounces an unblocked attacker),
  `SneakCastEnumerator` (surfaces "cast for Sneak" only at the right step),
  plus wiring in `CastSpellHandler`, `StackResolver`, and `ConditionEvaluator`
  so a permanent cast for Sneak enters tapped and attacking and carries the
  "sneak cost was paid" fact durably. Proven by `SneakTest` and per-card
  scenario tests. 23 of 26 Sneak cards have shipped (incl. all four
  sneak-was-paid riders: `Leonardo, Leader in Blue`, `Turncoat Kunoichi`,
  `The Last Ronin's Technique`, and the Karai rider). The remaining 3 each
  carry a second engine gap beyond Sneak (see the Status section above).
- **Gap C — Alliance (partly cleared)**: 6 of the 10 Alliance cards (`East
  Wind Avatar`, `EPF Point Squad`, `Mighty Mutanimals`, `Mutant Town
  Musicians`, `Raphael, Tough Turtle`, `Slash, Reptile Rampager`) shipped by
  composing `Triggers.OtherCreatureEnters` directly. The `Keyword.ALLIANCE`
  display marker is still absent, so the rendered card text doesn't get the
  italic "Alliance —" prefix yet, but the trigger semantics are faithful.
  Wiring the marker + `alliance { }` DSL helper is now purely a cosmetic /
  ergonomics improvement.
- **Gap D — Channel**: `Action News Crew` shipped via
  `activatedAbility { activateFromZone = Zone.HAND }` with
  `Costs.Composite(Mana("{6}"), DiscardSelf)`. The `Keyword.CHANNEL` display
  marker is still absent — same caveat as Alliance: behavior is right, the
  italic "Channel —" prefix doesn't render.
- **Gap H — Affinity for artifacts**: `Krang, Master Mind` shipped via
  `KeywordAbility.Affinity(CardType.ARTIFACT)`. No engine change required.
- **Gap L (partial)** — Landfall (`Weather Maker`) and all five basic-land-
  cycling variants (`Jennika` Plainscycling, `Stockman` Islandcycling, `Bebop
  Warthog Warrior` Swampcycling, `Zog` Mountaincycling, `Rocksteady Crash
  Courser` Forestcycling) all shipped composably via `KeywordAbility.
  typecycling("<Basic>", "{N}")`. Fight (`Novel Nunchaku`) and the four Mill-
  keyword cards remain unchecked but are believed composable.
- **Gap M — choose-one-of-N-keywords (sub-shape resolved)**: `Wingnut, Bat on
  the Belfry` shipped via `ModalEffect(countsAsModalSpell = false)` carrying
  three single-keyword `Mode.noTarget` entries. The general "choose a kind
  from among <list>" pattern in Gap M is closed for keywords by this idiom.
- **Gap S — delayed "at the beginning of your NEXT upkeep"**: `Casey Jones,
  Vigilante` shipped via `CreateDelayedTriggerEffect(step = Step.UPKEEP,
  fireOnlyOnControllersTurn = true, effect = …)`. The same pattern handles
  any other "next upkeep" rider that surfaces later.
- **Gap EE — bottom-of-library-then-draw**: `Manhole Missile` shipped by
  composing `GatherCards → SelectFromCollection(ChooseUpTo 1) → MoveCollection
  to Library bottom → ConditionalOnCollection(DrawCards)`. No new SDK shape;
  a future `EffectPatterns.bottomThenDraw(n)` would only be ergonomic sugar.
- **Gap O — "can't be blocked by creatures with power N or greater" —
  RESOLVED**: `April O'Neil, Kunoichi Trainee` shipped via the existing
  `CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(N))` static — same
  shape BLB Azure Beastbinder uses. Fixed-N variant closed. The relative-
  comparison sibling **Gap HH** ("with greater power" than source) remains
  unresolved.
- **Gap Q — graveyard reanimate as a typed-override token — RESOLVED**:
  `Brilliance Unleashed` (Mode 2) shipped by composing `MoveToZoneEffect(...
  GRAVEYARD → BATTLEFIELD)` with `ConditionalEffect(Not(TargetMatchesFilter
  (Creature)), Effects.BecomeCreature(3, 3, keywords = {FLYING},
  creatureTypes = {"Robot"}, duration = Duration.Permanent))`. Same idea as
  the EOE Xu-Ifit reanimate-with-permanent-rider, but the rider only fires
  when the original card wasn't already an artifact creature card.
- **Splinter-style "triggers an additional time" filter** — `Splinter,
  Radical Rat` shipped via the existing `AdditionalSourceTriggers` static
  (same shape ECL Twinflame Travelers uses), parameterised on
  `GameObjectFilter.Creature.withSubtype("Ninja").youControl()` with
  `excludeSelf = false` so Splinter's own triggers count (printed "a Ninja
  you control", no "another").
- **Delayed destroy of a created token (Old Hob)** — `Old Hob, Alleycat
  Blues` ships via the EOE Systems Override idiom: `CreateTokenEffect`
  publishes the new token into `ContextTarget(0)`, then
  `CreateDelayedTriggerEffect(step = Step.END, effect = MoveToZoneEffect
  (ContextTarget(0), GRAVEYARD, byDestruction = true))` schedules a
  faithful *destroy* at the next end step — so the second ability's UEOT
  indestructible grant legitimately saves the token (a `sacrificeAtStep`
  shortcut would have silently bypassed indestructible).
- **Additional combat phase rider** — `Raph & Leo, Sibling Rivals` ships
  via the existing `AddCombatPhaseEffect` (same shape as LTR Éomer, Marshal
  of Rohan), with the printed *"if it's the first combat phase of the
  turn"* intervening-if approximated by `oncePerTurn = true`. The
  approximation is documented in the file's docstring; swap for
  `Conditions.IsFirstCombatPhase` when that primitive lands.
- **Gap I — double the number of +1/+1 counters — RESOLVED at the primitive
  level**: `Effects.DoubleCounters(Counters.PLUS_ONE_PLUS_ONE, target)`
  already exists (Sage of the Fang shape). Turtle Van's "double the
  counters" rider composes today; the only outstanding piece is Gap LL
  below (crewed-this-turn filter).
- **Gap J — Crew / Vehicle — partly cleared**: `Turtle Blimp` shipped via
  `typeLine = "Artifact — Vehicle"` + `KeywordAbility.Numeric(Keyword.CREW,
  N)` — same shape DOM Weatherlight / BLC Rolling Hamsphere. The Vehicle
  pipeline (artifact-becomes-artifact-creature-UEOT + Crew activation) is
  fully wired. `Turtle Van` waits on Gap LL, not on Vehicle itself.
- **Gap K — Sagas — RESOLVED at the primitive level**: the
  `sagaChapter(N) { … }` DSL on `CardBuilder` already wires lore counters,
  per-chapter effects, and the after-last-chapter sacrifice. Confirmed via
  SPM Origin of Spider-Man. No TMT Saga has shipped because each carries
  an additional unresolved primitive: `The Cloning of Shredder` →
  `addedSubtypes` on `CreateTokenCopyOfTarget` (Gap MM); `The Last Ronin`
  → Gap AA's "When you do" sub-trigger and a new "attacks alone this
  turn" trigger condition (Gap NN).

## Data sources — do NOT hit the network

- **Card data** (name, mana cost, type line, oracle text, P/T, rarity, collector number,
  artist, flavor, image URI): read from `/workspace/tmt_full.json`, **not** Scryfall. It is
  a full Scryfall dump of all 195 cards (`.data[]`, keyed by the usual field names). When
  running `add-card`, feed it the matching entry from this file instead of doing a Scryfall
  lookup.
- **Rules**: cite and verify against `MagicCompRules_20260417.pdf` (repo root), **not**
  yawgatog or any web source. Read pages with the `Read` tool's `pages` parameter.

## Workflow

Each card is implemented with the **`add-card` skill** (oracle errata, set registration,
scenario test) — but source its card data from `tmt_full.json` and its rule references from
`MagicCompRules_20260417.pdf` per the section above. Run one card per Claude invocation:

```
/add-card <Card Name>   # from set TMT; use tmt_full.json for data, the CompRules PDF for rules
```

The skill is the source of truth on whether a card needs an engine change.

### Git strategy

**One PR per engine-change feature**, each off `main`. When several cards share one new
engine feature, that feature's PR can land all of them together — note it in the PR.
Composable cards (no engine change needed) can land directly on `main`, one commit per card.

### Per-card procedure

1. `/add-card <name>` — implement via the DSL, no class inheritance.
2. If it composes from existing primitives → commit directly on `main` (`Add <Card>`).
3. If `add-card` finds it needs a new `Effect`/keyword/replacement/SDK change →
   stop, branch off `main`, build the engine feature + the card + tests, open its own PR.
   Update `docs/card-sdk-language-reference.md` in the same PR (required for any SDK change).
4. Check the box in `cards.md` and update the `Implemented:` count.

## Notes

- Implement every card faithfully, reproducing oracle text as printed (use the Scryfall
  oracle text in `tmt_full.json`).
- Verify any MTG rule number against `MagicCompRules_20260417.pdf` before citing it.
- Battlefield filtering must use projected state (`matchesWithProjection`).
- Basic lands are covered by `basicLandsFallback`; add TMT-art variants only if you want
  the distinct printings.

---

## Engine gaps blocking the remaining cards

Each gap is its own PR. A card with more than one gap is listed under its dominant one
with the secondary noted inline. Quotes are from the card's oracle text in `tmt_full.json`.

### Gap A — Sneak (alternative cost) — RESOLVED — 23 of 26 cards shipped
**Status:** the keyword + alternative-cost pipeline is fully implemented and tested.
All four required pieces below landed; the 3 unimplemented Sneak cards each carry a
*second* engine gap beyond Sneak itself (see the Status section: `Leonardo's Technique`,
`Leonardo, Sewer Samurai`, `Michelangelo's Technique`).

> Sneak {cost} (You may cast this spell for {cost} if you also return an unblocked
> attacker you control to hand during the declare blockers step.
> [He/She/It enters tapped and attacking.])

Delivered pieces:
1. **Alt-cost gate at declare-blockers — DONE.** `SneakCastEnumerator`
   (`rules-engine/.../legalactions/enumerators/`) surfaces "cast for Sneak" only when
   it's the declare-blockers step of your combat, you control an unblocked attacker, and
   the spell is castable. `SneakWindow` (`rules-engine/.../mechanics/`) drives the
   bounce-an-unblocked-attacker payment.
2. **Permanent spells enter tapped and attacking — DONE.** Cast permanents that paid
   their sneak cost reuse the `attackingState` plumbing (wired through `CastSpellHandler`
   and `StackResolver`).
3. **Per-spell "sneak-was-paid" flag — DONE.** The fact rides the spell-cast context and
   the resulting permanent via `ChoiceSlot.SNEAK`, and is read back through
   `Conditions.SneakCostWasPaid` (`SourceConditions.kt`). Exercised by the four rider
   cards: `Leonardo, Leader in Blue`, `Turncoat Kunoichi`, `The Last Ronin's Technique`,
   and the Karai rider.
4. **DSL helper — DONE.** `sneak("{1}{W}")` on `CardBuilder`, mirroring `harmonize` /
   `impending`.

Tests: `SneakTest` plus per-card scenario tests (`JennikasTechniqueTest`,
`DonatellosTechniqueTest`, `ShreddersTechniqueTest`, `SplintersTechniqueTest`,
`TurncoatKunoichiTest`, `LeonardoLeaderInBlueTest`, `TheLastRoninsTechniqueTest`,
`KaraisTechniqueTest`, `NewGenerationsTechniqueTest`, `FootNinjasTest`).

Shipped (23): `Donatello's Technique`, `Donatello, Gadget Master`, `Foot Ninjas`,
`Jennika's Technique`, `Karai's Technique`, `Leonardo, Big Brother`,
`Leonardo, Cutting Edge`, `Leonardo, Leader in Blue`, `New Generation's Technique`,
`Oroku Saki, Shredder Rising`, `Raphael, the Nightwatcher`, `Shredder's Technique`,
`Shredder, Unrelenting`, `Splinter's Technique`, `Splinter, Hamato Yoshi`,
`The Last Ronin's Technique`, `Turncoat Kunoichi`, plus the 6 most recent:
`Dark Leo & Shredder`, `Karai, Future of the Foot`, `Kitsune's Technique`,
`Michelangelo, Improviser`, `Raphael's Technique`, `Shark Shredder, Killer Clone`.

Still to author (3 — each blocked on a SECOND gap, NOT pure authoring):
`Leonardo's Technique` (multi-target GY reanimate, per-target MV≤3),
`Leonardo, Sewer Samurai` (cast-from-graveyard static permission),
`Michelangelo's Technique` (aggregate total-MV-≤6 selection). See the corrected
Sneak bullet in the Status section above; each needs its own `add-feature` PR.

### Gap B — Disappear — RESOLVED (no engine change needed; the premise below was wrong)
**Engine change: NONE.** The earlier premise — that only a *global, nonland-only*
flag existed — was incorrect. A **per-controller, all-permanent** tracker already
ships: `PermanentLeftBattlefieldThisTurnComponent(count)` on each player, set in
`ZoneTransitionService` for every permanent (lands and tokens included) that leaves
that player's battlefield, credited to the last-known controller, and cleared at
cleanup. The matching Condition (`PermanentLeftBattlefieldThisTurn(Player.You)`,
DSL `Conditions.YouHadPermanentLeaveBattlefieldThisTurn`) already reads it for the
ability's controller in both resolution and projection — it is exactly "if a
permanent left the battlefield under your control this turn" and is what LTR
Shortcut to Mushrooms uses. Wire it as `triggerCondition` (the intervening-if, CR
603.4) on the existing `Triggers.EntersBattlefield` / `Triggers.YourEndStep`.

Shipped composably (4 pure): `Foot Mystic` (ETB → 1/1 black Ninja token),
`Insectoid Exterminator` (end step → scry 1), `Michelangelo, Game Master`
(end step → +1/+1 counter on self), `Putrid Pals` (enters-with-two-counters via
the Benalish Lancer conditional-ETB-counters idiom).

Still blocked, but on a *second* gap (Disappear itself is no longer the holdup):
- End-step Disappear + secondary gap: `Lord Dregg, Insect Invader` (Sacrifice-a-
  token cost — Gap M), `Rat King, Verminister` (same-name reanimate — Gap M),
  `West Wind Avatar` (token-or-land sac filter — Gap M), `Krang & Shredder`
  (reveal-until + cast-from-exile chain — Gap M), `Pizza Face, Gastromancer`
  (type-grant on a non-creature — Gap M).

### Gap C — Alliance (ability-word trigger) — partly RESOLVED
**Engine change:** still none required for behavior — `Triggers.OtherCreatureEnters`
already does the right thing. What remains is purely cosmetic: add a display-only
`Keyword.ALLIANCE` to the SDK enum + an `alliance { ... }` DSL helper (mirror
`flurry`, `eerie`, `vivid`, `fatefulBite`) so the rendered card text gets the
italic ability-word prefix.

Shipped composably (6/10): `East Wind Avatar`, `EPF Point Squad`,
`Mighty Mutanimals`, `Mutant Town Musicians`, `Raphael, Tough Turtle`,
`Slash, Reptile Rampager`, plus `Wingnut, Bat on the Belfry` (which also
needed the Gap M choose-one-of-keywords idiom — see below).

Still blocked on something *else*: `Lita, Little Orphan Amphibian` (Gap E
modal-not-yet-chosen), `Raphael, Most Attitude` (exile-and-may-cast chain),
`The Neutrinos` (flicker-tapped-and-attacking).

### Gap D — Channel — RESOLVED (behavior); display marker still missing
**Engine change:** none required for behavior. `Action News Crew` ships via
`activatedAbility { activateFromZone = Zone.HAND }` paired with
`Costs.Composite(Mana("{N}"), DiscardSelf)`. Cosmetic follow-up: add a display-
only `Keyword.CHANNEL` + `channel { }` DSL helper so the rendered text gets
the italic "Channel —" prefix. Same shape as the residual Alliance / Disappear
cosmetic work.

### Gap E — modal "choose one that hasn't been chosen this turn" — 1 card
**Engine change:** per-source memory of which modes have been chosen this turn,
threaded into modal-spell mode selection. (LTR's Gandalf the Grey gap mentions the
same shape — coordinate with that PR.)
- **Lita, Little Orphan Amphibian** — Alliance trigger picks a mode "that hasn't been
  chosen this turn."

### Gap F — Enrage (ability word) — 1 card
**Engine change:** display-only `Keyword.ENRAGE` + `enrage { ... }` DSL helper that
wires the existing "Whenever this creature is dealt damage" trigger (already exists per
Ixalan-era cards in older sets). Verify the trigger primitive is present; if not, add it.
- **Raphael, Ninja Destroyer** — "Enrage — Whenever Raphael is dealt damage, add that
  much {R}. Until end of turn, you don't lose this mana as steps and phases end."
  Also needs the **"don't lose this mana as steps and phases end"** mana modifier
  (CR 106.4 duration override) — see Gap G.

### Gap G — mana that doesn't empty at end of step/phase — 1 card
**Engine change:** mana pool entry tagged "persists across steps/phases for the rest of
the turn" (CR 106.4 exception). Likely a flag on the mana pool entry that the empty-mana
hooks skip.
- **Raphael, Ninja Destroyer** — see Gap F.

### Gap H — Affinity for artifacts (cost reduction) — RESOLVED
**Engine change:** none — the existing `KeywordAbility.Affinity(CardType.ARTIFACT)`
already works. Shipped as `Krang, Master Mind`.

### Gap I — "double the number of +1/+1 counters" — RESOLVED at the primitive level
**Engine change:** none — `Effects.DoubleCounters(counterType = Counters
.PLUS_ONE_PLUS_ONE, target)` already exists (Sage of the Fang shape). The
only card that needed this in TMT was `Turtle Van`, whose ship still waits
on the separate Gap LL "crewed-this-turn" filter (see below).

### Gap J — Crew / Vehicle — partly RESOLVED
**Engine change:** none for the baseline mechanic. `typeLine = "Artifact —
Vehicle"` + `keywordAbility(KeywordAbility.Numeric(Keyword.CREW, N))` already
wires the full pipeline (artifact-becomes-artifact-creature-UEOT, Crew
activation tapping a combined-power-N pile of creatures). Same shape DOM
Weatherlight and BLC Rolling Hamsphere ship.
- **Turtle Blimp** — shipped composably.
- **Turtle Van** — still blocked, but on Gap LL ("creature that crewed this
  Vehicle this turn" filter), *not* on Vehicle itself.

### Gap K — Sagas — RESOLVED at the primitive level
**Engine change:** none. The `sagaChapter(N) { … }` DSL on `CardBuilder`
already wires the full Saga pipeline — lore counters on enter and after
your draw step, per-chapter effect groups, sacrifice after the last
chapter. Confirmed via SPM Origin of Spider-Man.

Both TMT Sagas remain unchecked because each carries a *separate* blocker
beyond Saga itself:
- **The Cloning of Shredder** — Chapter I needs an *added* subtype on
  `CreateTokenCopyOfTarget` ("is a Mutant in addition to its other types").
  Today the API has `overrideSubtypes` (replaces wholesale) and
  `addedKeywords` / `addedSupertypes` / `removedSupertypes` but no
  `addedSubtypes`. Chapters II/III also need a "card exiled with this Saga"
  target reading from `CardSource.FromLinkedExile`.
- **The Last Ronin** — Chapter II needs the "Mill four. When you do,
  return target creature card" sub-trigger (Gap AA shape). Chapter III
  needs an "attacks alone this turn" trigger condition plus a delayed
  UEOT counters+trample+lifelink+indestructible rider. (Earlier skip-log
  entries listed this card as also Sneak — wrong; its only keyword is
  `Mill`, and Mill keyword itself is already supported.)

### Gap L — Landfall, Fight, Mill keyword, basic-land-cycling — partly RESOLVED
**Engine change:** none for Landfall + basic-land-cycling (both shipped). Fight
and Mill-keyword cards remain unchecked; expected composable.
- Landfall: `Weather Maker` — **DONE**.
- Basic-land-cycling family: `Jennika` (Plainscycling), `Stockman, Mad Fly-
  entist` (Islandcycling), `Bebop, Warthog Warrior` (Swampcycling),
  `Zog, Triceraton Castaway` (Mountaincycling), `Rocksteady, Crash Courser`
  (Forestcycling) — all **DONE** via `KeywordAbility.typecycling`.
- Fight: `Novel Nunchaku` — unchecked. ETB has an attach-then-"when you do,
  fight" sub-trigger which is the actual blocker (see Gap AA shape), not Fight
  itself.
- Mill keyword: `Does Machines` (Class — Gap U), `Kitsune's Technique` (Sneak
  — Gap A), `Paramecia Coloniex` (dies → "may exile … when you do, …" sub-
  trigger — Gap AA shape), `The Last Ronin` (Saga + Sneak). Each of these is
  blocked on a different gap; Mill itself is not the holdup.

### Gap M — one-off bespoke cards
Each is its own PR; they don't share a clean reusable gap with others.

- **Pizza Face, Gastromancer** — "If it isn't a creature, it becomes a 0/0 Mutant
  creature in addition to its other types" (type-grant + base P/T on a non-creature).
- **Rat King, Verminister** — "Return target creature card and all other cards with the
  same name as that card from your graveyard to the battlefield tapped." (Name-matching
  multi-target reanimate.)
- **Krang & Shredder** — "Whenever Krang & Shredder enter or attack, each opponent
  exiles cards from the top of their library until they exile a nonland card."
  (Reveal-until per-opponent + cast-from-exile-without-paying via the Disappear rider.)
- **West Wind Avatar** — "you may sacrifice a token or a land" (sacrifice filter:
  "token or land" — composes existing filters, just verify the disjunction works).
- **~~Wingnut, Bat on the Belfry~~ — RESOLVED**: "choose flying / menace / haste UEOT"
  ships via `ModalEffect(countsAsModalSpell = false)` with three single-keyword
  `Mode.noTarget` entries. The "choose-one-of-N-keywords UEOT grant" sub-shape
  is closed for keywords by this idiom; the same composition generalises to any
  "choose a kind from among <list of statics>" wording.
- **Lord Dregg, Insect Invader** — "Sacrifice a token: Draw a card." (Token-filtered
  sacrifice cost on an activated ability; verify `AbilityCost.SacrificeFiltered`
  supports the token predicate.)
- **The Last Ronin's Technique** — Sneak rider: "If this spell's sneak cost was paid,
  they enter tapped and attacking." (Gap A) → token-creator already supports
  tapped-and-attacking; condition is the new piece.

(Continue listing as `add-card` discovers more during implementation.)

---

## New engine gaps discovered during implementation

These were found while walking the alphabetical card list. Each is one card today
unless noted; group with Gap M if they stay solo, lift to their own gap if more
cards turn up wanting the same primitive.

### Gap N — distinct-card-types-among-spells-cast-this-turn dynamic amount
**Engine change:** add `DynamicAmount.DistinctCardTypesAmongSpellsCastByYouThisTurn`
(or a parameterized aggregator over the existing spell-cast-this-turn log).
- **April O'Neil, Hacktivist** — end step: "draw a card for each card type among
  spells you've cast this turn."

### Gap O — "can't be blocked by creatures with power N or greater" — RESOLVED
**Engine change:** none. `CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(N))`
already covers the fixed-N variant — shipped via April O'Neil, Kunoichi
Trainee. The relative-comparison sibling is **Gap HH** below.

### Gap P — "unless you discard a card" cost-mitigation rider
**Engine change:** a triggered-ability shape that says "sacrifice X unless you
[pay alternate cost]," where the player chooses on resolution.
- **Bebop & Rocksteady** — "Whenever Bebop & Rocksteady attack or block, sacrifice
  a permanent unless you discard a card."

### Gap Q — graveyard reanimate as a typed-override token — RESOLVED
**Engine change:** none. The `MoveToZoneEffect(GRAVEYARD → BATTLEFIELD) +
ConditionalEffect(Not(TargetMatchesFilter(Creature)),
Effects.BecomeCreature(... duration = Duration.Permanent))` chain models
Brilliance Unleashed's second mode faithfully. The "becomes" rider only
fires on the non-creature branch, matching the printed text. `BecomeCreature`
with `duration = Duration.Permanent` mirrors EOE Xu-Ifit's reanimate-with-
permanent-rider shape.

### Gap R — gain control of all matching permanents UEOT + untap + grant haste
**Engine change:** a bulk gain-control effect over a filter (all artifacts an
opponent controls), composed with Untap and GrantKeyword(HASTE, UEOT). Existing
gain-control primitives are single-target (LTR Gap 37 covers duration-based).
- **Broadcast Takeover** — "Gain control of all artifacts your opponents control
  until end of turn. Untap them. They gain haste until end of turn."

### Gap S — delayed "at the beginning of your NEXT upkeep" — RESOLVED
**Engine change:** none required. `CreateDelayedTriggerEffect(step = Step.UPKEEP,
fireOnlyOnControllersTurn = true, effect = …)` already covers this — `Casey
Jones, Vigilante` shipped via that combinator paired with the Urgoros random-
discard pipeline. The same composition handles any future "at your next
upkeep" rider.

### Gap T — copy-an-artifact-token with sacrifice at the next end step
**Engine change:** activated "create a token that's a copy of target artifact you
control" + "sacrifice it at the beginning of the next end step." Existing
token-copy effects are creature-typed and don't carry a self-sac timer.
- **Chrome Dome** — "{5}: Create a token that's a copy of another target artifact
  you control. That token gains haste. Sacrifice it at the beginning of the next
  end step."

### Gap U — Class enchantments (level-up subtype) — 2 cards
**Engine change:** the Class card type (Strixhaven CR 716) — sorcery-speed
level-up costs, per-level static/triggered abilities, "becomes level N" trigger.
Likely a sizable build; coordinates with future Strixhaven / Adventures in the
Forgotten Realms work.
- **Cool but Rude** — Class with 3 levels.
- **Does Machines** — Class with 2 levels.

### Gap V — search-or-fail conditional ETB effect
**Engine change:** an "ETB: search library for X; if no X is put into hand this
way, create a Y token" composite. Composes existing primitives but needs the
post-search "did anything land in hand" condition exposed.
- **Courier of Comestibles** — "search your library for a Food card …; if you
  don't put a card into your hand this way, create a Food token."

### Gap W — Mutagen token (artifact token with an activated counter ability)
**Engine change:** generic "create a Mutagen token" facade — an artifact token
with "{1}, {T}, Sacrifice: put a +1/+1 counter on target creature. Activate only
as a sorcery." Several cards reference it; once defined, they're all composable.
- **Crustacean Commando** — "create a Mutagen token."
- **Genghis Frog** — "Whenever … another Mutant you control enters, create a
  Mutagen token."

### Gap X — flicker a pair (artifact + creature) and return together
**Engine change:** an exile-and-return effect that takes two targets and returns
both at the same time (so synchronous ETB triggers see each other).
- **Don & Leo, Problem Solvers** — "exile up to one target artifact you control
  and up to one target creature you control. Then return them to the battlefield
  under their owners' control."

### Gap Y — grant a keyword (Affinity) to the next spell of a kind you cast
**Engine change:** a delayed continuous effect that watches the next noncreature
spell cast by you this turn and grants it Affinity-for-artifacts at cast time.
Different from cost-reduction-this-turn auras because the granted ability fires
at the *next-cast* moment with a specific filter.
- **Don & Raph, Hard Science** — "the next noncreature spell you cast this turn
  has affinity for artifacts."

### Gap Z — "an artifact entered the battlefield under your control this turn"
**Engine change:** per-controller "has X entered this turn" tracking that
includes artifacts (and ideally lands/creatures), and a `Condition` reading it
for static unblockability. Adjacent to the `nonlandPermanentLeftBattlefield`
plumbing but on the *entered* side.
- **Fugitive Droid** — "This creature can't be blocked if an artifact entered
  the battlefield under your control this turn." Second ability ("counter target
  spell that targets an artifact or creature you control") also needs a target-
  spell-targets-filter check that the engine doesn't expose generically yet.

### Gap AA — "when you do" sub-trigger off an optional sacrifice
**Engine change:** the "may [cost]. When you do, [effect]" two-step pattern,
where the inner trigger only fires if the optional cost was paid.
- **General Traag, Heart of Stone** — "you may sacrifice another artifact. When
  you do, General Traag deals 4 damage to target creature."

### Gap BB — "damage equal to the greatest power among creatures you control"
**Engine change:** `DynamicAmount.GreatestPowerAmong(filter)` (and the
single-creature filter for "the creature with the greatest power"). Read via
projected state. Pairs with several existing damage effects via
`Effects.DealDamage(dynamicAmount, target)`.
- **Go Ninja Go** — Mode 2: "deals damage equal to the greatest power among
  creatures you control to target creature an opponent controls."

### Gap CC — "Whenever you tap a land for mana, add X" trigger
**Engine change:** a triggered ability on mana abilities — the trigger fires
when the player taps a land specifically *to add mana*, not every time it
becomes tapped.
- **Groundchuck & Dirtbag** — "Whenever you tap a land for mana, add {G}."

### Gap DD — cost reduction conditioned on target's state
**Engine change:** spell-cost discount evaluated against the spell's intended
target (a tapped creature here). Different from "if you control X" reductions
because the predicate reads the chosen target during cast.
- **Grounded for Life** — "This spell costs {3} less to cast if it targets a
  tapped creature."

### Gap EE — "may put a card from your hand on the bottom of your library. If you do, draw a card." — RESOLVED
**Engine change:** none required. Composes inline as
`GatherCards → SelectFromCollection(ChooseUpTo 1) → MoveCollection-to-
Library-bottom → ConditionalOnCollection(DrawCards)`. `Manhole Missile`
ships with this idiom. A future `EffectPatterns.bottomThenDraw(n)` would
be ergonomic sugar but isn't required.

### Gap FF — "becomes an artifact creature with base P/T N/M" until end of turn
**Engine change:** extend `Effects.BecomeCreature` (or add a sibling) so it can
*add* card types instead of only granting Creature. Today it sets Creature +
overrides base P/T, which is correct for an artifact target but loses the
"becomes an artifact" wording when the target is a non-artifact creature.
- **Mind Transfer Protocol** — "target artifact or creature becomes an artifact
  creature with base power and toughness 4/5 until end of turn."

### Gap GG — "may pay X life where X is a dynamic amount" as an optional cost
**Engine change:** `Costs.PayLife` currently takes `Int`; need a
`Costs.PayLife(DynamicAmount)` overload threaded through the trigger-cost
pipeline so it can resolve at trigger time (e.g. equal to the triggering
creature's power).
- **Madame Null, Power Broker** — "Whenever another creature you control enters,
  you may pay life equal to its power. If you do, put that many +1/+1 counters
  on it."

### Gap HH — "can't be blocked by creatures with power N or greater"
**Engine change:** mirror of the existing
`CantBeBlockedByCreaturesWithLessPower` static. Either add
`CantBeBlockedByCreaturesWithGreaterPower` or parameterize the existing one
with a `ComparisonOperator`.
- **Prehistoric Pet** — "This creature can't be blocked by creatures with
  greater power." (Also has an activated bounce that's already composable.)

### Gap II — "you may tap or untap target creature" (controller chooses on resolution)
**Engine change:** an effect (or a target-modal helper) that asks the
controller to pick between tapping or untapping the chosen target. Today
`TapUntapEffect` carries a fixed `tap: Boolean` chosen at script-write time.
- **Sewer-veillance Cam** — Flash artifact whose ETB/LTB trigger says "you may
  tap or untap target creature." Composable apart from the player choice.

### Gap JJ — multi-subtype restricted mana
**Engine change:** today `ManaRestriction.SubtypeSpellsOrAbilitiesOnly` takes a
single subtype. Need a "this mana may be spent on a spell of subtype X *or* Y."
- **Turtle Lair** — "Add one mana of any color. Spend this mana only to cast a
  Ninja or Turtle spell." (The colorless `{T}: Add {C}` half plus the "Target
  Ninja or Turtle can't be blocked this turn" activated ability are composable.)

### Gap KK — "Spend this mana only to cast a [type] spell or to activate an ability"
**Engine change:** today `ManaRestriction.CardTypeSpellsOrAbilitiesOnly`'s
`allowAbilities` restricts to abilities **of [cardType] sources**. Need a
variant that permits *any* activated ability (not just artifact-source).
- **Purple Dragon Punks** — "{T}: Add {R}. Spend this mana only to cast an
  artifact spell or to activate an ability."

### Gap LL — "creature that crewed this Vehicle this turn" filter
**Engine change:** per-Vehicle tracking of which creatures tapped to crew it
each turn, plus a `TargetFilter` (or `GameObjectFilter` predicate) that reads
the tracking. Today's `Keyword.CREW` pipeline taps the chosen creatures and
flips the Vehicle to an artifact-creature, but it doesn't surface "the list
of creatures that crewed it this turn" anywhere; without that, "target
creature that crewed it this turn" can't be expressed. The natural shape is
a `CrewersThisTurnComponent` on the Vehicle (cleared at cleanup with the
existing per-turn flags), plus a `TargetFilter.CreatureThatCrewedSelfThisTurn`
helper.
- **Turtle Van** — "Whenever this Vehicle attacks, put a +1/+1 counter on
  target creature that crewed it this turn. Then if that creature is a
  Mutant, Ninja, or Turtle, double the number of +1/+1 counters on it."
  (The "double the counters" rider already composes via `Effects.DoubleCounters`
  paired with a `ConditionalEffect` on a subtype filter — only the crewers-
  this-turn target is missing.)

### Gap MM — `addedSubtypes` on `CreateTokenCopyOfTarget` (and friends)
**Engine change:** add an `addedSubtypes: Set<Subtype>` parameter to
`CreateTokenCopyOfTargetEffect` (and the equivalent on
`CreateTokenCopyOfSourceEffect`). Today the API has `overrideSubtypes`
(replaces wholesale) and `addedKeywords` / `addedSupertypes` /
`removedSupertypes` but no symmetrical *adder* for subtypes — so wordings
like "in addition to its other types" against a copied permanent can't be
expressed faithfully on the token's printed type line.
- **The Cloning of Shredder** — "Create a token that's a copy of it, except
  it isn't legendary and is a Mutant in addition to its other types."
  Chapters II/III repeat the same rider against a card exiled with the
  Saga (also needs `CardSource.FromLinkedExile` plumbing into the chapter's
  target / pipeline).

### Gap NN — "attacks alone this turn" trigger condition + delayed UEOT bundle
**Engine change:** a triggered ability that fires when a single attacker is
declared *and no other creature is attacking at the same time*, plus the
"this turn" persistence (the trigger needs to keep firing for the rest of
the turn, not just on a single declare-attackers step). Or model it as a
turn-long delayed-trigger registered on the Saga's chapter resolution that
listens for attacks-alone events until end of turn.
- **The Last Ronin** — Chapter III: "Whenever a creature you control
  attacks alone this turn, put three +1/+1 counters on it. It gains
  trample, lifelink, and indestructible until end of turn." The counter +
  triple-keyword rider already composes via AddCounters + a chain of
  GrantKeyword(EndOfTurn) calls — only the trigger shape is missing.

---

## Composable — deferred for time

All four of the previously deferred composable cards (`Baxter Stockman`, `Bebop,
Warthog Warrior`, `Dimensional Exile`, `Foot Elite`) have now landed on
`tmt-scaffolding`. This section will be repopulated if the next pass uncovers
more "composable but skipped" entries.

---

## Skip log — cards inspected and skipped (alphabetical)

Each row is a card encountered during an alphabetical pass that was not
implemented, with the underlying blocker. Gaps reference the sections above
(existing Gap A–M or the new Gap N–KK). Rows previously marked
"Composable — deferred" have all landed and been removed. Likewise, cards
that subsequently landed in later runs (the six Alliance composables,
Action News Crew, Casey Jones Vigilante, Escape Tunnel, Manhole Missile,
Wingnut, and the 17 shipped Sneak cards once Gap A resolved) have been removed
from the table — see the "Gaps resolved" list at the top of the Status section
for what closed those.

| Card                                  | Blocker / Gap                               |
|---------------------------------------|---------------------------------------------|
| April O'Neil, Hacktivist              | Gap N (card-types-cast amount)              |
| Bebop & Rocksteady                    | Gap P (unless-you-discard rider)            |
| Broadcast Takeover                    | Gap R (mass gain-control UEOT)              |
| Chrome Dome                           | Gap T (copy-artifact + next-end-step sac)   |
| Cool but Rude                         | Gap U (Class)                               |
| Courier of Comestibles                | Gap V (search-or-fail-then-token)           |
| Crustacean Commando                   | Gap W (Mutagen token)                       |
| Does Machines                         | Gap U (Class)                               |
| Don & Leo, Problem Solvers            | Gap X (paired flicker)                      |
| Don & Raph, Hard Science              | Gap Y (grant Affinity to next spell)        |
| Donatello, Mutant Mechanic            | Gap M (Pizza-Face-style type-grant)         |
| Everything Pizza                      | Pentacolored sac activation that also draws + makes each opponent discard — bespoke |
| Foot Mystic                           | Gap B (Disappear)                           |
| Fugitive Droid                        | Gap Z (artifact-ETB-this-turn + sac-counter)|
| General Traag, Heart of Stone         | Gap AA (when-you-do sub-trigger)            |
| Genghis Frog                          | Gap W (Mutagen token)                       |
| Go Ninja Go                           | Gap BB (greatest-power-among amount)        |
| Groundchuck & Dirtbag                 | Gap CC (tap-land-for-mana trigger)          |
| Grounded for Life                     | Gap DD (cost reduction if target tapped)    |
| Insectoid Exterminator                | Gap B (Disappear)                           |
| Kitsune, Dragon's Daughter            | "Exchange control of two creatures controlled by different players" — bespoke |
| Koya, Death from Above                | Delayed end-step "you may pay; if not, return that card" conditional — bespoke |
| Krang & Shredder                      | Gap B (Disappear) + Gap M (cast-from-exile-without-paying chain) |
| Leader's Talent                       | Gap U (Class)                               |
| Leatherhead, Swamp Stalker            | Hexproof counter + "may remove a counter; when you do, …" (Gap AA shape) |
| Leonardo's Technique                  | Sneak OK; needs up-to-2 *target* GY reanimate, per-target MV≤3 (new gap) |
| Leonardo, Sewer Samurai               | Sneak OK; needs cast-creatures-from-graveyard static permission (new gap) |
| Lita, Little Orphan Amphibian         | Gap C (Alliance) + Gap E (mode-not-yet-chosen) |
| Lord Dregg, Insect Invader            | Gap B (Disappear) + Gap M (Sacrifice-a-token cost) |
| Madame Null, Power Broker             | Gap GG (may-pay-dynamic-life)               |
| Michelangelo's Technique              | Sneak OK; needs aggregate total-MV-≤6 selection over looked-at cards (new gap; `SelectCardsDecision.maxTotalManaValue` may already cover it) |
| Michelangelo, Game Master             | Gap B (Disappear)                           |
| Michelangelo, Mutant BFF              | Gap W (Mutagen) + counter-doubling replacement |
| Michelangelo, Weirdness to 11         | Gap W (Mutagen) + counter-doubling replacement |
| Mikey & Don, Party Planners           | Play-from-top + cast-Mutant/Ninja/Turtle-from-top — bespoke |
| Mind Transfer Protocol                | Gap FF (BecomeCreature that adds Artifact)  |
| Mondo Gecko                           | Discard-to-grant-color + hexproof-from-color combat trigger — bespoke |
| Mutagen Man, Living Ooze              | Gap W (Mutagen) + activated-ability-cost-reduction static |
| Mutant Chain Reaction                 | Gap W (Mutagen)                             |
| Ninja Teen                            | Gap U (Class)                               |
| North Wind Avatar                     | "Put a card you own from outside the game into your hand" (wishboard) |
| Northampton Farm                      | Custom land with linked-exile mechanic — bespoke |
| Novel Nunchaku                        | "When you do, equipped creature fights" sub-trigger (Gap AA shape) |
| Ooze Spill                            | Gap W (Mutagen)                             |
| Paramecia Coloniex                    | Dies → "may exile; when you do …" sub-trigger (Gap AA shape) |
| Party Dude                            | Gap U (Class)                               |
| Pizza Face, Gastromancer              | Gap B (Disappear) + Gap M (type-grant on non-creature) |
| Prehistoric Pet                       | Gap HH (can't-be-blocked-by-greater-power)  |
| Purple Dragon Punks                   | Gap KK (artifact-spell-or-any-ability mana restriction) |
| Putrid Pals                           | Gap B (Disappear)                           |
| Raphael, Most Attitude                | Gap C (Alliance)                            |
| Raphael, Ninja Destroyer              | Gap F (Enrage) + Gap G (persistent mana)    |
| Rat King, Verminister                 | Gap B (Disappear) + Gap M (same-name reanimate) |
| Ray Fillet, Man Ray                   | Gap W (Mutagen)                             |
| Retro-Mutation                        | Type-overriding Aura + "loses all abilities" UEOT |
| Return to the Sewers                  | Owner-chooses-top-or-bottom + Gap W (Mutagen) |
| Sewer-veillance Cam                   | Gap II (tap-or-untap controller-chooses)    |
| Slithering Cryptid                    | Gap W (Mutagen)                             |
| Technodrome                           | "Can't attack or block unless its power is 6 or greater" — Gap O-shape on self |
| The Cloning of Shredder               | Gap MM (addedSubtypes on CreateTokenCopy)   |
| The Last Ronin                        | Gap AA (Mill-X-then-when-you-do) + Gap NN (attacks-alone trigger) |
| The Neutrinos                         | Gap C (Alliance)                            |
| The Ooze                              | Gap W (Mutagen) + dies-with-counter trigger |
| Tokka & Rahzar, Terrible Twos         | "Can't be countered" + mana-spent-less-than-MV trigger — bespoke |
| Turtle Lair                           | Gap JJ (multi-subtype mana restriction)     |
| Turtle Van                            | Gap LL (crewed-this-turn filter)            |
| Turtles Forever                       | Wishboard tutor (outside-the-game search)   |
| Turtles in Time                       | Mass bounce + shuffle-hand+gy-and-draw-7 + exile-self |
| Venus, Torn Between Worlds            | "Damage dealt + survives" trigger condition + counter-bearer combat trigger |
| West Wind Avatar                      | Gap B (Disappear) + Gap M (token-or-land sac filter) |
| Zoo Escapees                          | Gap W (Mutagen)                             |
