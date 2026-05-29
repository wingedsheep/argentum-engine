# Invasion — Engine Gap Analysis

Cross-reference of the **275 remaining (unimplemented) Invasion cards** against the engine's
actual capabilities (SDK reference + source verification, May 2026). Generated to scope what
must be built before the set can be completed.

**Status at time of writing:** 59 / 335 implemented (18%). Card list comes from
`scripts/card-status --list --set INV`.

## Bottom line

The **vast majority** of remaining cards are buildable today. Invasion's defining mechanics are
all supported:

- **Kicker / Kicker {X} / Kicker {cost}** — `KeywordAbility.kicker(...)` (Agonizing Demise, Kavu Titan, Urza's Rage, Verdeloth, …)
- **Domain** — `DynamicAmounts.domain()` / `Conditions.BasicLandTypesAtLeast` (Tribal Flames, Kavu Scout, Wandering Stream, Ordered Migration, Worldly Counsel, Strength of Unity, Exotic Curse, Power Armor, Collapsing Borders, Wayfaring Giant)
- **Split cards** — `CardLayout.SPLIT` (Assault//Battery, Pain//Suffering, Spite//Malice, Stand//Deliver, Wax//Wane)
- **Coin flips** — `FlipCoinExecutor` (Chaotic Strike)
- **Cast-only-during-combat / after-blockers** — `CastRestriction.OnlyDuringPhase/Step` (Cauldron Dance, Spinal Embrace, Chaotic Strike)
- **"Doesn't untap" static** — `DOESNT_UNTAP` keyword static (Shackles, Juntu Stakes, Tsabo's Web; Temporal Distortion via counter)
- **Dynamic +X/+X statics** — `GrantDynamicStatsEffect` (Crusading Knight, Marauding Knight, Sparring Golem, Kavu Scout, Wayfaring Giant, Power Armor)
- **CDA P/T** — `dynamicPower`/`dynamicToughness` (Molimo, Yavimaya Kavu)
- **Protection from a subtype** — `ProtectionScope.Subtype` (Shoreline Raider)
- **Multicolored predicate** — `CardPredicate.IsMulticolored` (Urza's Filter, Rewards of Diversity filter)
- **Permanent gain-control / exchange-control** — `Duration.Permanent` / `ExchangeControlEffect` (Empress Galina, Phyrexian Infiltrator, Spinal Embrace)
- **Color-change permanents** — `ChangeColor` / `BecomeAllColors` (Tidal Visionary, Rainbow Crow, Kavu Chameleon, Metathran Transport, Sway of Illusion, Ancient Kavu, Defiling Tears, Alloy Golem)
- Counter spells/abilities, reanimation + mass reanimation (Bringer shape), prevent-damage shields, choose-a-number, search/reveal/mill/scry pipelines, mana rocks/taplands/sac-lands/cameos.

What follows are the **genuine gaps** — elements no current SDK primitive expresses. ~21 distinct
elements, concentrated in ~35 of the 275 cards. The other ~240 are implementable now.

---

## Gaps by theme

### Conditions / state checks

1. **"[Specific color] is the most common color among all permanents (or tied)" as a self-static gate.**
   `Conditions.TargetSharesMostCommonColor` exists but is *target*-relative. Needs a self-gating
   `ColorIsMostCommon(color)` condition.
   → **Goham, Halam, Ruham, Sulam, Zanam Djinn** (5 cards — one shared primitive unlocks all)

2. **"Control a permanent/creature of each color" (five-color condition).**
   → **Coalition Victory**, **Spirit of Resistance**

### Triggers

3. **"Whenever a player taps a land for mana" trigger + mana-production replacement.** No
   tapped-for-mana trigger, no "produces a different color instead" replacement.
   → **Fertile Ground**, **Overabundance**, **Pulse of Llanowar** (one shared primitive)

4. **Opponent / any-player cast triggers.** `SpellCastEvent` has a `player` field, but no DSL sugar
   exposes it and detector support for `Player.Each/Opponent` on casts is unverified (only
   `Player.You` constants/factory exist).
   → **Pure Reflection** ("whenever a player casts a creature spell"), **Rewards of Diversity**
   ("whenever an opponent casts a multicolored spell")

5. **"Shares a color with the triggering creature" filter** for ETB group effects.
   → **Spreading Plague**

6. **Triggered/activated abilities that function from the graveyard.**
   → **Pyre Zombie** ("at the beginning of your upkeep, if this card is in your graveyard…")

### Damage prevention / replacement

7. **Damage-amount-modifying / conditional replacements** not currently expressible:
   - **Divine Presence** — cap any 4+ damage to 3
   - **Callous Giant** — prevent damage only when amount ≤ 3 (threshold all-or-nothing)
   - **Well-Laid Plans** — prevent damage between two creatures if they share a color
   - **Harsh Judgment** — redirect chosen-color instant/sorcery damage to its controller
   - **Protective Sphere** — prevent damage from a source sharing a color with the *mana spent on the activation cost*
   - **Spirit of Resistance** — prevent all damage to you (also needs the five-color condition, #2)

### Costs / X-spend

8. **"Spend only [color] mana on X" + tracking how much of a color was spent.**
   → **Soul Burn** (spend only B/R; life gain capped by {B} spent), **Atalya, Samite Master**
   (spend only white on X)

9. **Discard as an activation cost** (no `Costs.Discard`).
   → **Meteor Storm** ("Discard two cards at random:")

### Choices / targeting

10. **"Name a card" choice + name-matching search/reveal filter.**
    → **Desperate Research**, **Lobotomy**

11. **Color-change applied to a spell on the stack** (current color-change is permanent-only).
    → **Blind Seer**, **Crystal Spray**

12. **Damage by/to "the controller of a target permanent"** (no `EffectTarget` for a target's controller).
    → **Backlash**, **Agonizing Demise** (kicked)

13. **Protection from a supertype + targeting by supertype (legendary).** `ProtectionScope` covers
    color/cardtype/subtype but not supertype; no legendary target predicate.
    → **Tsabo Tavoc** (protection from legendary creatures; destroy target legendary creature)

14. **Chosen-type landwalk grant** (`EntersWithChoice(BASIC_LAND_TYPE)` exists, but no "grant
    landwalk of the chosen type" modification).
    → **Traveler's Cloak**

15. **Dynamic multi-color protection from a board-computed color set.**
    → **Pledge of Loyalty** ("protection from the colors of permanents you control")

### Bespoke / one-off engines

16. **Life-bidding / auction.** → **Mages' Contest**
17. **Text-changing effects** (replace color word / land type). → **Crystal Spray**
18. **Color-relational cast restriction.** → **Mana Maze** ("can't cast spells sharing a color with the most recently cast spell")
19. **Target-changing-on-reveal engine.** → **Psychic Battle**
20. **"Play with the top card of your library revealed"** passive static. → **Goblin Spy** (minor)

### Partially supported (primitive exists, payoff control-flow is new)

21. **Pile-separation cycle.** `SeparatePermanentsIntoPilesEffect` / `factOrFiction` cover the
    Fact-or-Fiction shape (reveal → opponent splits → you choose). These cards need "*each player*
    separates their *own* permanents → an opponent chooses → only the chosen pile can attack/block
    this turn" (a continuous restriction on a chosen set):
    → **Bend or Break**, **Fight or Flight**, **Stand or Fall**, **Death or Glory**, **Global Ruin**

---

## Implementation plans

Each plan below was written after verifying the actual SDK against source (May 2026). The single
most important correction from that verification: **several "gaps" are already buildable today** — the
primitives exist, they just weren't found during the first pass. Those are called out explicitly so we
don't build anything twice.

The guiding principle throughout (per `docs/architecture-principles.md` §1.5 and the team's standing
feedback): **extend the composable vocabulary — filters, conditions, dynamic amounts, effect-targets,
replacement variants — never add a card-specific executor.** A good gap fix unlocks a *family* of
cards, not one.

### Already buildable — no engine work (close these first)

| # | Card(s) | Why it already works |
|---|---------|----------------------|
| #6 | **Pyre Zombie** | ✅ **Implemented** (`inv/cards/PyreZombie.kt`). Graveyard-functional upkeep trigger via `triggerZone = Zone.GRAVEYARD` + `MayPayManaEffect({1}{B}{B}, ReturnToHand(Self))` (same shape as Onslaught's Gigapede); sac ability = `Costs.Composite(Mana({1}{R}{R}), SacrificeSelf)` → `DealDamage(2, Targets.Any)`. The `triggeredAbility { }` builder already surfaces `triggerZone`; `TriggerDetector` scans graveyard cards (`TriggerDetector.kt:359-405`). No engine work. |
| #12 | **Backlash**, **Agonizing Demise** (kicked) | `EffectTarget.TargetController` already exists (`EffectTarget.kt:57-62`), plus `ControllerOfTriggeringEntity`. Backlash = `DealDamage(amount = DynamicAmounts.targetPower(0), target = EffectTarget.TargetController)`; Agonizing Demise riders on `Conditions.WasKicked`. |
| #20 | **Goblin Spy** | `MiscStaticAbilities.LookAtTopOfLibrary` / `PlayFromTopOfLibrary` exist (`MiscStaticAbilities.kt:162-201`). One nuance: Goblin Spy reveals to **all** players (public), `LookAtTopOfLibrary` is private. Add a sibling `RevealTopOfLibrary` data object (public reveal, no play permission) mirroring the existing ones — ~5 lines, not a new system. |
| #2 (half) | **Coalition Victory** | ✅ **Implemented** (`inv/cards/CoalitionVictory.kt`). "A creature of each color" = `Compare(DynamicAmounts.colorsAmongPermanents(Player.You, GameObjectFilter.Creature), GTE, Fixed(5))` (a single 5-color creature satisfies it — matches the official ruling, and `Aggregation.DISTINCT_COLORS` caps at 5). "A land of each basic land type" = `Conditions.BasicLandTypesAtLeast(5)`. Combined with `Conditions.All(...)` inside a `ConditionalEffect` → `Effects.WinGame()`. No new primitive. |
| #4 (runtime) | **Rewards of Diversity**, **Pure Reflection** (trigger half) | `SpellCastEvent(player = Player.Opponent / Player.Each)` is already matched at runtime (`TriggerMatcher.matchesPlayer`, lines 668-675). Rewards of Diversity = trigger on `Player.Opponent` + multicolored filter, payoff to `Player.TriggeringPlayer`. Only ergonomic gap: facade constants (see #4 below). |

---

### #1 — `ColorIsMostCommon(color)` self-condition · 5 djinns ✅ DONE

> **Implemented (primitive + all 5 cards).** `Condition.ColorIsMostCommon(color)` +
> `Conditions.ColorIsMostCommon(color)` facade; `ConditionEvaluator` shares a
> `mostCommonColors(state, projected)` helper with `TargetSharesMostCommonColor` and evaluates the
> new condition dual-mode (resolution + projection). Covered by `ColorIsMostCommonTest`. All five
> djinns authored in `definitions/inv/cards/` (Goham/Halam/Ruham/Sulam/Zanam), each as a
> `ConditionalStaticAbility(ModifyStats(-2,-2, source()), Conditions.ColorIsMostCommon(<color>))`.


**What exists.** `ConditionEvaluator.evaluateTargetSharesMostCommonColor()`
(`ConditionEvaluator.kt:647-674`) already tallies every color across every battlefield permanent
(via projected colors), finds the max tally, and builds the tied "most common" set. It's just
*target*-relative.

**Plan.**
1. Extract the tally→maxCount→`mostCommonColors: Set<Color>` computation into a private helper
   `mostCommonColors(state, projected): Set<Color>` in `ConditionEvaluator`.
2. Add `Condition.ColorIsMostCommon(val color: Color)` in `BattlefieldConditions.kt`. Evaluate as
   `color in mostCommonColors(...)`. It is board-derived only (no targets / no kicker / no triggering
   entity), so it works unchanged in **both** the `Resolution` and `Projection`
   `ConditionEvaluationContext` paths — which is required, since the djinns use it as a
   `ConditionalStaticAbility`.
3. `Conditions.ColorIsMostCommon(color)` facade method.

**Composition.** Each djinn → `ConditionalStaticAbility(condition = Conditions.ColorIsMostCommon(<color>), ability = <its bonus>)`.

**Leverage.** One condition unlocks all 5 djinns (Goham/Halam/Ruham/Sulam/Zanam). Reusable for any
"as long as [color] is the most common color" card.

---

### #2 — Five-color "of each color" condition · Coalition Victory, Spirit of Resistance

**What exists.** `DynamicAmounts.colorsAmongPermanents(player, filter)` (DISTINCT_COLORS, caps at 5)
and `DynamicAmounts.domain(player)` (DISTINCT_BASIC_LAND_SUBTYPES). Both already do the counting.

**Status.** ✅ **Coalition Victory implemented** (`inv/cards/CoalitionVictory.kt`, scenario test
`CoalitionVictoryScenarioTest`) — pure composition, no engine change. **Spirit of Resistance still
blocked on #7** (it needs a continuous "prevent all damage to you" static; the five-color condition
half is ready).

**Plan.** No new primitive — **compose**.
- Coalition Victory: see the "already buildable" table above.
- Spirit of Resistance: condition = `Compare(colorsAmongPermanents(Player.You), GTE, Fixed(5))`,
  gating a `PreventDamage` static (the prevention itself is covered under #7). Build as
  `ConditionalStaticAbility(condition, ability = <prevent all damage to you>)`.

Optional ergonomics: add `Conditions.ControlPermanentOfEachColor(filter = Any)` and
`Conditions.ControlLandOfEachBasicType()` as thin facade wrappers over the two `Compare`s, since the
shape will recur (Crystalline Crawler, Cromat, Dega/Ana-type cards). Pure sugar, no engine change.

---

### #3 — Tapped-for-mana event · Fertile Ground, Overabundance, Pulse of Llanowar ✅ DONE

> **Implemented.** All three cards authored + scenario-tested (`InvasionTappedForManaTest`).
> SDK: `GameEvent.LandTappedForMana` + `Triggers.AnyPlayerTapsLandForMana`/`landTappedForMana(...)`;
> `AdditionalManaOnTap.anyColor` (Fertile Ground's choose-any-color bonus); `AdditionalManaOnSourceTap.rider`
> (Overabundance's "deals 1 damage" inline rider); new `ReplaceLandManaColor(filter)` static (Pulse of
> Llanowar). Engine: emits `LandTappedForManaEvent` on the manual mana-ability path + `TriggerMatcher`
> case; `TappedForManaBonusResolver` + `ChooseAnyColorTapBonusContinuation` drive Fertile Ground's per-tap
> color choice (resolution-time pause); `ReplaceLandManaColor` swaps a matched land's base mana effect for
> "add one mana of any color" in the handler and treats it as a five-color source in `ManaSolver`; the
> any-color tap bonus is modeled as a flexible `BonusManaEntry` in the solver. **Known limitation
> (intentional, matches existing engine behavior for City of Brass etc.):** automatic cost payment adds the
> mirror/replacement/bonus *mana* via the solver but does **not** fire the `LandTappedForMana` event or
> non-mana riders (Overabundance's damage) — those only resolve on a manual tap.

**What exists.** `MiscStaticAbilities.TappedForManaGrant` / `TappedForManaGrantFromFilter` already
intercept mana-ability resolution and **add** extra mana inline (no stack), which is exactly the
shape of a triggered mana ability (CR 605). Runtime emits `ManaAddedEvent` but there is no
*tapped-a-land-for-mana* SDK trigger and no "produce different mana instead" replacement.

Card-by-card:
- **Fertile Ground** ("…adds an additional one mana of any color") → **already `TappedForManaGrant`**.
  Buildable today as an Aura with that static. Verify and close.
- **Overabundance** ("…adds one mana of any type that land produced. Overabundance deals 1 damage to
  that player") → additive grant **plus a non-mana rider** (the damage). Still a triggered mana
  ability, so it must resolve inline during mana production.
- **Pulse of Llanowar** ("…adds one mana of any color **instead**") → mana-production **replacement**,
  not additive.

**Plan.** Introduce one canonical event and reuse the existing inline intercept:
1. Add SDK `GameEvent.LandTappedForMana(player: Player = Player.Each, landFilter: GameObjectFilter)`
   and emit a `LandTappedForManaEvent(tapperId, landId)` from the mana-ability resolution path
   (where `ManaAddedEvent` is produced).
2. For the **rider** case (Overabundance), extend the tapped-for-mana intercept so the static can
   carry an optional inline `rider: Effect` resolved during mana production (deal 1 damage to the
   tapper). Keep it inline — it is not a stack ability.
3. For the **replacement** case (Pulse), add a `ManaProductionReplacement` variant: "when a land
   matching `filter` is tapped for mana, its controller adds one mana of any color it could produce
   instead." This slots beside the existing `LandTappedForTwoOrMoreMana` (Damping Sphere) intercept,
   which is the precedent for modifying mana-ability output.

**Leverage.** The `LandTappedForMana` event + inline-rider + mana-replacement trio is broadly reusable
(Mana Flare, Heartbeat of Spring, Power Surge-likes). Medium effort, concentrated in the mana path.

---

### #4 — Opponent / any-player cast-trigger sugar · Rewards of Diversity, Pure Reflection ✅ DONE

> **Implemented (facade + both cards).** `Triggers.AnyPlayerCastsSpell`, `Triggers.OpponentCastsSpell`,
> `Triggers.anyPlayerCasts(spellFilter)`, `Triggers.opponentCasts(spellFilter)` added to `Triggers.kt`.
> **Rewards of Diversity** (`opponentCasts(Multicolored)`) and **Pure Reflection**
> (`anyPlayerCasts(Creature)` → destroy existing Reflections, create X/X token under the caster's
> control) authored in `definitions/inv/cards/` and auto-registered. Covered by
> `RewardsOfDiversityScenarioTest` (incl. the `Player.Opponent`-scoping negative case) and
> `PureReflectionScenarioTest`.

**What exists.** Already wired at runtime (`TriggerMatcher.matchesPlayer`). Only the `Triggers` facade
lacks constants; cards can construct `SpellCastEvent(player = …)` directly today.

**Plan.** Pure ergonomics — add to `Triggers.kt`:
- `Triggers.AnyPlayerCastsSpell`, `Triggers.OpponentCastsSpell`
- `fun anyPlayerCasts(spellFilter)`, `fun opponentCasts(spellFilter)`

Then **Rewards of Diversity** = `opponentCasts(multicolored)` → `GainLife(2, Player.You)` +
`LoseLife(2, Player.TriggeringPlayer)`. **Pure Reflection's** trigger = `anyPlayerCasts(creature)`
(its self-token-copy payoff is a separate, existing token-copy effect). Low effort, high reuse.

---

### #5 — `SharesColorWith` filter · Spreading Plague ✅ DONE

> **Implemented (primitive + card).** `CardPredicate.SharesColorWith(entity)` +
> `GameObjectFilter.sharingColorWith(entity)`, evaluated via projected colors in `PredicateEvaluator`
> (colorless → no match). Plus a reusable `excludeTriggering` flag on `CardSource.BattlefieldMatching`
> threaded through `destroyAllPipeline` / `Effects.DestroyAll(…, excludeTriggering = true)` so "all
> *other* … with it" triggers spare the triggering creature. Spreading Plague authored in
> `definitions/inv/cards/SpreadingPlague.kt`; covered by `SpreadingPlagueScenarioTest`.

**What exists.** `CardPredicate.SharesCreatureTypeWith(entity)` +
`GameObjectFilter.sharingCreatureTypeWith(entity)` (`ObjectFilter.kt:335`). No color analogue.

**Plan.** Mirror the creature-type pair exactly:
1. `CardPredicate.SharesColorWith(val entity: EntityReference)`.
2. `GameObjectFilter.sharingColorWith(entity)` builder.
3. Evaluate in `PredicateEvaluator` using **projected** colors
   (`projected.getColors(candidate)` ∩ `projected.getColors(entity)` non-empty) — projection is
   mandatory here (color-changing effects).

**Composition.** Spreading Plague = ETB trigger (binding `ANY`, any creature) →
`Destroy(GroupRef(GameObjectFilter.Creature.sharingColorWith(EntityReference.Triggering).excludeSelf))`.

**Leverage.** Reusable for every "shares a color with" card (Standardize, Circle of Solace targeting,
Dega-style payoffs).

---

### #6 — Graveyard-functional ability · Pyre Zombie ✅ DONE

> **Implemented** (`inv/cards/PyreZombie.kt`, pure card authoring — no engine change). The
> `triggeredAbility { }` builder already exposes `triggerZone` (→ `TriggeredAbility.activeZone`), and
> `TriggerDetector` scans graveyard cards for step triggers with `activeZone == GRAVEYARD`
> (`TriggerDetector.kt:359-405`, controller = card owner). Upkeep recursion =
> `triggerZone = Zone.GRAVEYARD` + `MayPayManaEffect(ManaCost.parse("{1}{B}{B}"),
> Effects.ReturnToHand(EffectTarget.Self))`. Sac ability =
> `Costs.Composite(Costs.Mana("{1}{R}{R}"), Costs.SacrificeSelf)` → `Effects.DealDamage(2, Targets.Any)`.

`activeZone = Zone.GRAVEYARD` + intervening-`if` is fully wired (`TriggerDetector.kt:359-405`;
controller resolves to the card's **owner** for graveyard cards). The `MayPayManaEffect`
("you may pay {1}{B}{B}. If you do, return it to your hand") and graveyard `triggerZone` setter both
already existed (Gigapede precedent), so no engine work was required.

---

### #7 — Conditional / amount-relational damage replacements · 6 cards ✅ MOSTLY DONE

> **Implemented (5 of 6 cards; Protective Sphere deferred to #8).** New SDK vocabulary, no per-card
> executors: `AmountFilter` (`Any`/`AtMost`/`AtLeast`/`Exactly`) on `GameEvent.DamageEvent`; `CapDamage`
> replacement; `PreventDamage.restrictions: List<Condition>` (condition-gated prevention, mirrors
> `ModifyLifeLoss.restrictions`); relational `CardPredicate.SharesColorWithRecipient` +
> `GameObjectFilter.sharingColorWithRecipient()`; `CardPredicate.SharesChosenColorWithSource` +
> `sharingChosenColorWithSource()`; `EffectTarget.ControllerOfDamageSource`. Engine: `AmountFilter`,
> the relational source predicate, and `restrictions` are honored in `DamageUtils.applyStaticDamageReduction`;
> `CapDamage` in `applyStaticDamageAmplification`; the previously-dead `RedirectDamage` replacement is now
> applied as a continuous static via a new `findStaticDamageRedirect` scan in `dealDamageToTarget`
> (each source applies once per event, loop-guarded by `appliedRedirects`). All paths shared by combat
> and noncombat damage. Cards authored in `definitions/inv/cards/`: **Callous Giant** (AmountFilter),
> **Divine Presence** (CapDamage), **Well-Laid Plans** (SharesColorWithRecipient), **Spirit of
> Resistance** (#2 five-color condition + restrictions), **Harsh Judgment** (RedirectDamage +
> ControllerOfDamageSource + chosen-color predicate). Covered by per-card scenario tests
> (`CallousGiantTest`, `DivinePresenceTest`, `WellLaidPlansTest`, `SpiritOfResistanceTest`,
> `HarshJudgmentTest`).
>
> **Protective Sphere remains blocked on #8** — it prevents damage from a source sharing a color with
> *the mana spent on the activation cost*, which needs per-color mana-spent tracking (gap #8). The
> reusable hook is ready: store the spent color as the prevention's chosen color and reuse the
> chosen-color source predicate.

This is the largest gap and the place where it's most tempting to write six bespoke executors. The
elegant path is to **enrich the existing `appliesTo` filter vocabulary** so the existing
`PreventDamage` / `RedirectDamage` replacements cover most cases, then add exactly one new "cap"
variant.

**What exists.** `ReplacementEffect.PreventDamage(amount?)`, `RedirectDamage(target)`, `DoubleDamage`,
`ModifyDamageAmount(modifier)`, all filtered by `GameEvent.DamageEvent(recipient, source, damageType)`
with `RecipientFilter` / `SourceFilter (HasColor, Matching, …)` / `DamageType`. The 7-stage
`dealDamageToTarget()` pipeline (`DamageUtils.kt:72-260`) already evaluates these. Floating
`PreventAllDamageTo` etc. also exist.

**Plan — three additions, no per-card executors:**

1. **Amount filter on `DamageEvent`.** Add `amount: AmountFilter = Any` with
   `AmountAtMost(n) / AmountAtLeast(n) / Exactly(n)` (`EventFilters.kt`). The pipeline checks it before
   applying a matching prevention.
   - **Callous Giant** ("prevent damage if it's 3 or less") → `PreventDamage(amount = null,
     appliesTo = DamageEvent(recipient = Self, amount = AmountAtMost(3)))`. Pure reuse.

2. **`CapDamage(maxAmount, appliesTo)`** replacement (the one genuinely new variant — capping isn't
   prevent or modify). Mirrors `PreventDamage`'s structure.
   - **Divine Presence** ("4+ damage to a permanent or player → 3 instead") →
     `CapDamage(maxAmount = 3, appliesTo = DamageEvent(recipient = AnyPlayerOrPermanent))`.

3. **Relational + chosen-color source filters** (`SourceFilter`):
   - `SourceFilter.SharesColorWithRecipient` → **Well-Laid Plans** ("prevent damage a creature would
     deal to another creature if they share a color"): `PreventDamage(null, DamageEvent(
     recipient = AnyCreature, source = Creature + SharesColorWithRecipient))`.
   - `SourceFilter.HasChosenColor` (reads the replacement source's `ChosenColorComponent`) →
     **Harsh Judgment** ("the next time a source of your choice of the chosen color would deal
     damage…redirect to its controller"): `RedirectDamage(target = ControllerOfDamageSource,
     appliesTo = DamageEvent(source = HasChosenColor + instant/sorcery `Matching`))`. Requires one
     new `EffectTarget`: **`ControllerOfDamageSource`** (the controller of the current damage's
     source) — a natural sibling of the existing `TargetController` family.
   - **Protective Sphere** ("prevent damage from a source of the color of mana spent to activate")
     reuses `SourceFilter.HasChosenColor` — the activation stores the spent mana color as the chosen
     color on the created prevention effect (ties into #8's per-color tracking).

4. **Spirit of Resistance** ("prevent all damage to you") = `PreventDamage(amount = null,
   appliesTo = DamageEvent(recipient = You))` gated by the five-color `ConditionalStaticAbility` from
   #2. Already expressible once it's wrapped in the condition.

**Leverage.** `AmountFilter` and the relational `SourceFilter`s are reused by triggers too (they share
the `GameEvent` pattern system per §2.7), so this enriches the whole event vocabulary, not just damage
prevention.

---

### #8 — Color-restricted X-spend + per-color mana tracking · Soul Burn, Atalya ✅ DONE

> **Implemented (primitive + both cards).** `xManaRestriction: Set<Color>` on `CardScript` (spell) and
> `ActivatedAbility`, surfaced through the `spell { }` / `activatedAbility { }` DSL. The mana solver
> grew an `xManaRestriction` parameter + dedicated restricted-X pass (`ManaSolution.xRestrictedManaSpent`
> reports the per-color X allocation); `CastPaymentProcessor` and `ActivateAbilityHandler` restrict the
> floating-mana X loops to the allowed colors (colorless disallowed) and `canPay` only counts allowed-color
> pool mana toward X. Per-color mana spent on X is stored on `SpellOnStackComponent.manaSpentOnXByColor`,
> plumbed into `EffectContext`, and read by the new `DynamicAmount.ManaSpentOnX(color)`. **Soul Burn**
> ({X}{2}{B}, `xManaRestriction = {BLACK, RED}`, life = `Effects.GainLife(DynamicAmount.ManaSpentOnX(BLACK))`)
> and **Atalya, Samite Master** (modal `{X},{T}` ability, `xManaRestriction = {WHITE}`) authored in
> `definitions/inv/cards/`. Covered by `SoulBurnAndAtalyaXManaTest`. **Scoping note:** Soul Burn implements
> the original Invasion life-gain wording (life = black spent on X); the modern Oracle's secondary caps
> (damage dealt / target's life / loyalty / toughness) are omitted as edge-case-only refinements.

**What exists.** Per-color spent buckets (`manaSpentWhite…`) live on `SpellOnStackComponent`
(`StackComponents.kt:47-52`) but are **not** exposed to `EffectContext` (only the
`totalManaSpent` sum is). There is no color restriction on X payment.

**Plan — two pieces:**
1. **Expose per-color spent.** Plumb the six buckets into `EffectContext` (as
   `manaSpentByColor: Map<Color, Int>`) and add `DynamicAmount.ManaSpentOfColor(color)` reading it
   (sibling of the existing `TotalManaSpent`).
   - **Soul Burn**: damage = `XValue`; "gain life equal to the black mana spent" =
     `GainLife(DynamicAmount.ManaSpentOfColor(Color.BLACK))`.
2. **Restrict which colors pay X.** Add `Spell.xManaRestriction: Set<Color>` honored by the
   `ManaSolver` / `CastPaymentProcessor` when paying the X (generic) portion only.
   - **Atalya** activated ability and **Soul Burn** ("spend only B and/or R on X") set the restriction.

**Leverage.** `ManaSpentOfColor` is reusable for any "for each [color] spent" payoff; the X-restriction
covers the small family of "spend only [color] on X" spells.

---

### #9 — Discard (incl. at random) as a cost · Meteor Storm

**What exists.** `AbilityCost.Discard(filter)` (single, chosen) and `AdditionalCost.DiscardCards(count,
filter)`. No "at random" and `AbilityCost.Discard` has no count.

**Plan.** Add `count: Int = 1` and `atRandom: Boolean = false` to `AbilityCost.Discard` (and `atRandom`
to `AdditionalCost.DiscardCards` for symmetry). The `CostHandler` selects randomly when `atRandom`.
Cost is a distinct family from Effects, so widening it (rather than composing) is the right call here.
- **Meteor Storm** = activated ability, cost `Composite(Mana("{R}"), Discard(count = 2, atRandom =
  true))`, effect `DealDamage(3, AnyTarget)`.

**Leverage.** Random discard recurs (Browbeat-likes, Wheel-of-Fortune riders). Small, contained.

---

### #10 — "Name a card" choice + name-matching filter · Desperate Research, Lobotomy

**What exists.** Only `CardPredicate.NameEquals(static)`. No `CARD_NAME` option type, no choose-a-name
effect.

**Plan.**
1. Add `OptionType.CARD_NAME` + `ChooseCardNameEffect(storeAs)` storing the chosen name in
   `EffectContext` (`chosenCardName: String?` plus a named pipeline variable). The client gets a
   name-entry/autocomplete decision.
2. Add `CardPredicate.NameEqualsChosen` matching the stored chosen name.

**Composition** (both reuse the existing gather/search/move pipeline):
- **Lobotomy** = `ChooseCardName` → search target player's hand+graveyard+library with
  `NameEqualsChosen` → exile all matches.
- **Desperate Research** = `ChooseCardName` → gather library → partition by `NameEqualsChosen` →
  matches to hand, remainder to graveyard.

**Leverage.** Unlocks the whole "name a card" family (Pithing Needle is a different axis, but Cranial
Extraction, Memoricide, Sadistic Sacrament all follow this exact shape).

---

### #11 — Color-change applied to a spell on the stack · Blind Seer, Crystal Spray

**What exists.** `ChangeColorEffect` exists but `ChangeColorExecutor` (`:28-29`) silently fizzles if the
target isn't on the battlefield, and color projection runs only over the battlefield.

**Plan.**
1. Relax the battlefield guard in `ChangeColorExecutor` to also accept stack entities (spells have a
   `CardComponent`).
2. Extend the Layer-5 color projection to also project floating color effects onto **stack** objects,
   so a recolored spell reads its new color during resolution and for color-matching checks.
3. Target requirement = `TargetObject` whose filter admits stack spells (Blind Seer: "target spell or
   permanent").

**Leverage.** Stack-object color projection is the prerequisite for any "target spell becomes [color]"
card; pairs with #17 for Crystal Spray. Medium effort (projection scope change is the real work).

---

### #12 — Damage to a target's controller · Backlash, Agonizing Demise

**Already buildable** (see table). Use `EffectTarget.TargetController`. No engine work.

---

### #13 — Protection from a supertype + legendary targeting · Tsabo Tavoc

**What exists.** `ProtectionScope` covers Color/Colors/CardType/Subtype/Everything/EachOpponent but not
supertype. `CardPredicate.IsLegendary` exists (filter only).

**Plan.**
1. **Targeting** "destroy target legendary creature" is already buildable:
   `TargetObject(GameObjectFilter.Creature.legendary())`.
2. Add `ProtectionScope.Supertype(val supertype: String)`. Wire it exactly like the existing scopes:
   `KeywordAbility.Protection` text + `StateProjector` synthesizes
   `PROTECTION_FROM_SUPERTYPE_LEGENDARY`, and the combat/damage/target protection checks consult the
   projected supertype.

**Leverage.** "Protection from legendary creatures" is rare but the scope addition is trivial and
mirrors the established pattern.

---

### #14 — Landwalk of a chosen basic land type · Traveler's Cloak

**What exists.** Landwalk is a fixed 5-keyword enum; `EntersWithChoice(BASIC_LAND_TYPE)` writes a
chosen-type component.

**Plan.** Because basic-land-type → landwalk keyword is a fixed 5-way mapping
(Forest→Forestwalk, …), add a static ability `GrantLandwalkOfChosenType` that, at projection, reads
the source's chosen-basic-land-type component, maps it to the matching existing `Keyword`, and grants
it to the enchanted creature. Reuses the existing landwalk keywords and chosen-type machinery — no new
"parameterized landwalk" keyword needed.

**Composition.** Traveler's Cloak = Aura with `EntersWithChoice(BASIC_LAND_TYPE)` +
`staticAbility { GrantLandwalkOfChosenType(target = EnchantedCreature) }`.

---

### #15 — Dynamic multi-color protection from a board-computed set · Pledge of Loyalty

**What exists.** `ProtectionScope.Colors(Set<Color>)` is a fixed set; protection is synthesized as
per-color keywords at projection (`StateProjector.kt:100-101`).

**Plan.** Add `ProtectionScope.DynamicColors(val filter: GroupFilter)`. At projection, compute the
color set from permanents matching `filter` (for Pledge: "permanents you control"), then synthesize
the same per-color protection keywords already used by `Colors`. The only new logic is computing the
set at projection time; the downstream protection checks are unchanged.

**Leverage.** Reusable for any "protection from the colors of [group]" (Voidmage Apprentice-style,
Akroma variants).

---

### #16 — Life-bidding / auction · Mages' Contest

Genuinely bespoke; lowest priority.

**Plan.** Add a `LifeAuctionContinuation` + `BidLifeDecision`. The effect iterates players in APNAP
order presenting "bid higher or pass," tracks `{currentBid, highBidder, playersStillIn}`, loops until
all but one pass, then the winner loses `currentBid` life and a stored `payoffEffect` (controlled by
the winner) runs. Mages' Contest's payoff = the existing counter-spell effect bound to the winner. The
continuation/decision are reusable for other bidding cards, but there's only one in this set — build it
when nothing higher-leverage remains.

---

### #17 — Text-changing (color word / land-type word) · Crystal Spray

**What exists.** Only `ChangeCreatureTypeTextEffect` (Layer 3, via `TextReplacementComponent`).

**Plan.** Add `ChangeWordInTextEffect(target, category: WordCategory { COLOR, BASIC_LAND_TYPE }, from,
to, duration)`, mirroring the creature-type text effect. The genuine difficulty: the engine's abilities
are **structured data**, not literal text, so a true text-change must reinterpret color/land-type
references inside serialized abilities (mana production, color conditions). Scope this carefully —
deliver the common cases (color words in printed colors/mana, land-type words in mana abilities) and
flag the long tail. Couples with #11 (Crystal Spray targets spells too). **Bespoke / lower priority.**

---

### #18 — Color-relational cast restriction · Mana Maze

**What exists.** `CastRestriction` is timing/phase only and is *per-spell-definition*;
`spellsCastThisTurnByPlayer` records casts but there's no "most recently cast spell" color check, and
Mana Maze restricts **all** spells globally.

**Plan.**
1. Track the most recent cast: add `lastCastSpellColors: Set<Color>?` to `GameState`, updated on every
   `SpellCastEvent`.
2. Model Mana Maze as a `StaticAbility.CantCastSpellsSharingColorWithLastCast` consulted by
   `CastPermissionUtils.canCastSpell()` — reject a candidate that shares a color with
   `lastCastSpellColors`. This is a global continuous restriction (correct for Mana Maze), not a
   per-card cast restriction.

**Leverage.** Narrow (one card), but `lastCastSpellColors` tracking is cheap and the static-restriction
hook is reusable for other "players can't cast…" effects. Medium effort.

---

### #19 — Reveal-and-compare target swap · Psychic Battle

**What exists.** `ChangeTargetEffect` / `ChangeSpellTargetEffect` / `ReselectTargetRandomlyEffect` all
exist (`StackEffects.kt:355-410`). Reveal/gather + `DynamicAmount.StoredCardManaValue` +
`DynamicAmount.Conditional` all exist.

**Plan.** Compose a continuation that: gathers (reveals) the top card of each relevant player's
library, stores their mana values, then runs a `Conditional` effect comparing them (the
control/redirect payoff). Most pieces exist; the new control-flow is "reveal both, compare, branch,"
which is a thin continuation over existing primitives. **Medium / bespoke.**

---

### #20 — Play with top card revealed · Goblin Spy

**Already buildable** modulo a public-reveal variant — see the table. Add `RevealTopOfLibrary` data
object (public) beside the existing private `LookAtTopOfLibrary`. ~5 lines.

---

### #21 — "Each player separates; an opponent chooses a pile" + chosen-pile restriction · 5 cards

**What exists.** `ChoosePileEffect` (binary chooser, `Chooser.Opponent` supported) +
`SelectFromCollectionEffect` partition + the `factOrFiction` pattern. `CantAttackGroupEffect` /
`CantBlockGroupEffect` apply turn-duration restrictions via a `dynamicGroupFilter`.
`SeparatePermanentsIntoPilesEffect` is self-separate-**self**-choose only.

The five cards (Bend or Break, Fight or Flight, Stand or Fall, Death or Glory, Global Ruin) share one
shape: *one player separates a set into two piles → a chooser picks a pile → a directed effect hits the
chosen vs. other pile.* The existing pile primitives already cover the **partition + choose** step
(set `Chooser.Opponent`). Three additions close the rest:

1. **Gather-controlled-permanents `CardSource`.** A source that collects "permanents `<player>`
   controls matching `<filter>`" into a named collection, feeding the partition. (Bend or Break: lands;
   Fight or Flight: defender's creatures; Death or Glory: exiled creatures.)
2. **`ForEachPlayer` combinator** over the pile pipeline — for the "*each* player separates their own,
   an opponent chooses" cards (Bend or Break, Global Ruin). Reuses `Player.Each`/APNAP iteration.
3. **`GroupFilter.InStoredCollection(name)`** so a continuous restriction can target the *chosen pile*
   specifically (snapshotting entity IDs at resolution). Fight or Flight = `CantBlockGroupEffect(filter
   = creatures NOT InStoredCollection("chosenPile"))`.

Sacrifice/return variants (Bend or Break, Stand or Fall, Death or Glory, Global Ruin) then compose as
`gather → SelectFromCollection(chooser = separator) → ChoosePile(chooser = Opponent) →
Sacrifice/MoveCollection(chosenPile)`. No new pile *engine*, just the source + iterator + stored-group
filter.

**Leverage.** These three primitives also unlock Tempt-with-Discovery-style and Varchild-style
"separate / choose a pile" cards across other sets.

---

## Suggested build order (highest leverage first)

0. **Close the already-buildable cards** (#6, #12, #20, Coalition Victory, Rewards of Diversity) — pure
   card authoring plus ≤2 trivial surfacing tweaks (graveyard `activeZone` setter, public
   `RevealTopOfLibrary`, opponent-cast facade constants).
1. **`ColorIsMostCommon` self-condition** (#1) — 5 djinns from one condition.
2. **Opponent/any-player cast facade** (#4) ✅ — runtime-wired; facade added, Rewards of Diversity + Pure Reflection done.
3. **`SharesColorWith` filter** (#5) — one predicate, reusable family.
4. **Per-color mana tracking + X restriction** (#8) — unlocks Soul Burn / Atalya and feeds #7's
   Protective Sphere.
5. **Damage-replacement vocabulary** (#7) ✅ — `AmountFilter`, relational/chosen-color predicates,
   `CapDamage`, `PreventDamage.restrictions`, `RedirectDamage` (now wired), `ControllerOfDamageSource`;
   unlocked 5 cards and enriched the shared event system. Protective Sphere still waits on #8.
6. **Tapped-for-mana event + rider + replacement** (#3) — 3 cards, reusable mana hooks.
7. **Name-a-card choice + filter** (#10) — 2 cards, large external family.
8. **Pile-separation trio** (#21) — 5 cards from three composable additions.
9. Scope additions: protection supertype (#13), dynamic-color protection (#15), chosen-type landwalk
   (#14), discard-at-random cost (#9) — small, independent.
10. Bespoke / heavier: stack color-change (#11), Mana Maze restriction (#18), reveal-and-compare
    (#19), text-changing (#17), life auction (#16).
