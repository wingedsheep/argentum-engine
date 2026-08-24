# The Dark — engine gaps

Divergences found while verifying DRK (2026-08-23, `verify-set`) that are **not** card-shaped. Each
survived an adversarial verification pass whose brief was to refute it. None of them is a reason to
hold the set back — every one is either invisible in a two-player game of DRK-only cards, or is a
pre-existing engine behaviour that DRK merely happens to expose.

Ordered by blast radius. **Two were fixed at the card level** after this file was first written —
Worms of the Earth (§5) and Sorrow's Path (§9) — and one (§7, Reflecting Mirror) turned out on closer
reading not to be a divergence at all. Each says so inline rather than being deleted, because the
engine-level gap behind the two fixes is still open.

---

## 1. Landwalk reads base type lines, not projected subtypes — CORPUS-WIDE

`LandwalkRule.playerControlsLandWithSubtype`
(`rules-engine/.../mechanics/combat/rules/BlockEvasionRules.kt:148-157`) takes the **base**
`CardComponent` off `ctx.state.getEntity(entityId)` and tests
`cardComponent.typeLine.hasSubtype(landSubtype)`. `TypeLine` is inert printed SDK data; land-type
changes are written only into `ProjectedValues` (`EffectApplicator.kt:229-238`), and
`GameState.projectedState` is a `by lazy` derived object with no write-back. So a continuous effect
that changes a land's subtypes is invisible to landwalk evasion.

The tell: the *same two lambdas* already read the projection for control
(`ctx.projected.getController(entityId)`, lines 141/153). Only the type read was left on base state.
This is precisely the hazard `AGENTS.md` names under "Projected state for battlefield filters".

Corroboration that the correct spelling is known: `ai/.../engine/CombatMath.kt:97-101` does the same
landwalk check the right way (`projected.hasSubtype(it, "Forest")`). **The AI's combat math and the
authoritative combat rule disagree under any land-type-changing effect.**

- **DRK contains both halves of the bug in one set**: Blood Moon (`SetLandTypesForGroup`) and nine
  landwalk cards (Scarwood Hag, Hidden Path, Scarwood Bandits, Merfolk Assassin, Marsh Goblins,
  Goblins of the Flarg, War Barge, Wormwood Treefolk, Cave People).
- Corpus-wide: 64 card files across ~20 sets carry landwalk; the other half is every set-land-type
  effect — `SetEnchantedLandType` (Sea's Claim), `SetEnchantedLandTypeFromChosen` (Phantasmal
  Terrain), `SetBasicLandTypesFromChosen` (Dream Thrush), `AddLandTypeByCounter`, `SetLandTypeExecutor`.
- **Trim on the claim:** under Blood Moon specifically, `playerControlsNonbasicLand` still answers
  correctly, because `isBasicLand` keys on the BASIC *supertype* and `SetBasicLandTypes` never touches
  supertypes. It is the *subtype* function Blood Moon breaks. (`playerControlsNonbasicLand` is still
  wrong for Imprisoned in the Moon vs Trailblazer's Boots.)
- **No test covers it.** `LandwalkEvasionTest` has 5 tests, all plain printed Forest/Island.
  `BloodMoonScenarioTest` asserts only `projectedState.hasSubtype(...)` and the granted `{R}` ability —
  it proves the projection is right, which is exactly why the base read slips through: the two never
  meet in a test.

**Fix:** ~8 lines in one file — iterate `ctx.projected.getBattlefieldControlledBy(...)` and test
`ctx.projected.hasType/hasSubtype/getSupertypes`. Every battlefield permanent is guaranteed a
projection entry, so it is safe. But it changes a shared combat rule and wants its own regression
test (a Blood-Moon-vs-mountainwalk case in `LandwalkEvasionTest`), so it belongs in its own engine
PR — `add-feature`, not a set-verification pass.

---

## 2. Granted zone-change replacements are not scoped to the granted entity — Whippoorwill

`ZoneMovementUtils.checkZoneChangeRedirect`'s granted-replacement loop
(`rules-engine/.../handlers/effects/ZoneMovementUtils.kt:845-858`) never compares `grant.entityId`
to the moving entity; it scopes only on `event.filter` and `grant.controllerId`. Whippoorwill's third
clause grants `RedirectZoneChange(filter = GameObjectFilter.Creature)`, whose `controllerPredicate`
is null — and `matchesZoneChangeFilter` returns `true` immediately when that is null (`:900`).

**Effect in play:** activating Whippoorwill exiles *every* creature that dies that turn, on both
sides, instead of only the targeted one. `effect.selfOnly` is honoured in the battlefield loop
(`:790`) but is not consulted in the granted loop, so it is not an available escape hatch today.
`GrantReplacementEffectExecutor.kt:44-46` documents this read path outright.

The existing `WhippoorwillScenarioTest` misses it because its bystander case bolts the *shielded*
creature, so no second creature ever dies while the grant is live.

**Not a gap:** Whippoorwill's second clause ("damage … can't be prevented or dealt instead to
another permanent or player") is fully implemented, both halves — `DamageUnpreventableThisTurnComponent`
read via `DamageUtils.isDamagePreventionDisabled`, plus the redirect short-circuit at
`DamageUtils.kt:168-175`, correctly per-recipient rather than the global flag.

**Blast radius: only this card.** The two other users of `GrantReplacementEffect` + `RedirectZoneChange`
— Malicious Eclipse (LCI) and Walk-In Closet // Forgotten Cellar (DSK) — are *intentionally* global
and controller-scoped, so filter+controller matching is right for them.

**Fix (2 lines):** honour `selfOnly` in the granted loop
(`if (effect.selfOnly && grant.entityId != entityId) continue`) and set `selfOnly = true` on
Whippoorwill's `RedirectZoneChange`. Defaults leave the other two cards unchanged.

---

## 3. `PayOrSufferEffect` has no `ifPaid` branch — Spitting Slug

Spitting Slug reads "you may pay {1}{G}. If you do, **this creature** gains first strike … Otherwise,
**each creature blocking or blocked by** this creature gains first strike." `PayOrSufferEffect`
(`mtg-sdk/.../effects/CompositeEffects.kt:677-698`) models only the *unpaid* branch, so the card
grants first strike to the Slug unconditionally and the unpaid branch removes it again.

An earlier review guessed the "otherwise" clause was missing entirely; **verification refuted that.**
All three printed outcomes are correct and tested: paid → Slug has first strike; unpaid → Slug does
not; unpaid → both blockers gain it (via
`Patterns.Group.grantKeywordToAll(FIRST_STRIKE, Creature.blockingOrBlockedBySource())`, covering both
directions of the CR 509 pairing).

**What genuinely diverges** is narrow: the take-back is a `Layer.ABILITY` `RemoveKeyword` floating
effect timestamped *later*, so first strike the Slug already had from an Aura or a lord is stripped
too — which the printed "otherwise" branch never authorises (it should do nothing to the Slug). No
CR 613.8 dependency saves it: neither effect changes what the other does, so plain timestamp order
applies. Untested, and unreachable with DRK cards alone.

**Fix:** add an `ifPaid` branch to `PayOrSufferEffect` so the grant is conditional rather than taken
back. SDK change — `add-feature`.

---

## 4. Enter-then-move instead of a pre-entry replacement — Frankenstein's Monster

"…put this creature into its owner's graveyard **instead of onto the battlefield**" is modelled as
enter-then-move (`OnEnterRunEffect` runs post-entry), so ETB and dies triggers elsewhere observe an
entry that never happened. Already documented in the card's own KDoc; needs a pre-entry replacement
hook. The counter distribution itself is correct — repeatable `ModalEffect` with both ceiling and
floor pinned to `DynamicAmount.XValue`, all three asymmetric counter kinds, tested at X = 0/1/2/4.

---

## 5. Modes are never feasibility-gated — Worms of the Earth

`ModalEffectExecutor.kt:122` offers every mode: `val availableIndices = effect.modes.indices.filter
{ it !in alreadyChosen }` — the only filter is the already-chosen memory. And the sacrifice silently
succeeds when it cannot be paid (`SacrificeExecutor.kt:72-75`):

```kotlin
if (validPermanents.size < effect.count) {
    // Not enough valid permanents - effect does nothing (per MTG rules)
    return EffectResult.success(state)
}
```

Because that is `success` and not `error`, `CompositeEffectExecutor.kt:56-58` walks straight on to the
next sub-effect — `WormsOfTheEarth.kt:71` `Effects.Destroy(EffectTarget.Self)`.

**Effect in play:** a player with 0 or 1 lands accepts the gate, picks "Sacrifice two lands",
sacrifices nothing, and destroys Worms of the Earth for free. Printed, that player's only escape is
the 5-damage mode. This is **the common case, not a corner case** — Worms of the Earth is precisely
the card that makes players landless, so the lock is escapable at zero cost by whoever it locks out
hardest. `WormsOfTheEarthScenarioTest.kt:55` only covers the two-lands-available path.

**FIXED at the card level.** Mode 1's composite is now wrapped in
`GatedEffect(Gate.WhenCondition(Conditions.ControlLandsAtLeast(2)), …)`, so the `Destroy` is
unreachable when the sacrifice can't be paid. `ControlLandsAtLeast` reads `Player.You`, and
`ForEachExecutor.bindIterationContext` rebinds `controllerId` to the iterated player for
`IterationSpace.Players`, so it asks about the player actually being offered the escape.
`WormsOfTheEarthScenarioTest` covers it: a player with one land picks the sacrifice mode, gets no
land-selection prompt, and the enchantment survives. Verified by mutation — removing the gate turns
that test red and leaves the other three green.

**Still open, and the better fix:** `ModalEffectExecutor` does not feasibility-filter its modes, and
`SacrificeExecutor` returning `success` below its count is what makes an unpayable mode silently
"work". Every `ModalEffect` in the corpus shares this; the card-level gate fixes one instance of it.

---

## 6. No draw-step replacement hook — Fasting

Fasting's "If you would begin your draw step, you may skip that step instead. If you do, you gain 2
life" is a replacement effect; it is modelled as a *second* optional upkeep trigger
(`Fasting.kt:57-63`) that arms `SkipNextDrawStep` and gains the life. The card's own KDoc documents
the simplification. The marker is a bare flag, not a counter — `SkipDrawStepComponent`
(`PlayerComponents.kt:542`), added unconditionally (`SkipNextDrawStepExecutor.kt:39`) and consumed
once (`DrawPhaseManager.kt:113-114`).

**Effect in play:** two Fastings — or Fasting plus any other "skip your draw step" source — accept
both upkeep triggers, gain 2 life each, and consume one idempotent marker: **4 life for one skipped
draw step**, where the printed replacement can only apply once. Likewise, Fasting leaving the
battlefield with the trigger on the stack still gains the 2 life with no draw step skipped.

**Correction to the original review:** its "turn 1 on the play" example is *not* reachable — upkeep
precedes the first main phase, so a `{W}` enchantment can never be on the battlefield for its
controller's turn-1 upkeep.

**Fix:** proper fix is a draw-step replacement primitive (medium engine work), exactly as the KDoc
says. A card-only mitigation could gate the life gain on the skip marker not already being present.

---

## 7. Reflecting Mirror — investigated and NOT a divergence

An earlier verification pass reported that Reflecting Mirror ("target **spell** with a single
target") could be announced against an activated or triggered ability, and that its X gate collapsed
there because an ability stack entity carries no `CardComponent`, making `2 × manaValue` equal 0.

**That is wrong, and acting on it would have introduced the bug it described.**

Whether a stack target requirement offers abilities at all is decided in one place —
`StackObjectTargeting.permitsAbilities` — which both readers consult (`TargetFinder`, the
authoritative target set, and `TargetEnumerationUtils`, the legal actions sent to clients and the
AI). An ability is a candidate **only when the filter explicitly names an ability predicate**.
`TargetFilter.SpellOrAbilityOnStack` is `GameObjectFilter.Any` in the stack zone and names none, so
abilities are never offered. Reflecting Mirror is spell-only in practice today, and the X = 0 path is
unreachable through legal play.

The trap: the obvious "fix" is a filter built from
`CardPredicate.Not(CardPredicate.IsActivatedOrTriggeredAbility)`. But `permitsAbilities` deliberately
does **not** invert negation — its own comment says the question is "does the requirement's text
mention abilities at all", not "must the object be an ability" — so such a filter returns `true` and
would start offering ability entities, relying on the predicate to reject them afterwards. That is
strictly worse than the status quo. No change made.

The genuinely open item here is the *opposite* one, and it is already recorded in
`docs/card-sdk-language-reference.md`: `Targets.SpellOrAbility` and
`Targets.SpellOrAbilityWithSingleTarget` name no ability predicate, so the "or ability" half of
Willbender, Bolt Bend, Return the Favor, Untimely Malfunction and Redirect Lightning does not reach
abilities today. Closing that needs an explicit `Or` naming the ability kinds, per the reference's own
caveat — and it does not involve Reflecting Mirror, which wants the narrow behaviour it already has.

Reflecting Mirror's one real divergence is unchanged and documented in its KDoc: X is *chosen* and
validated at resolution rather than *computed* at activation, because ability cost calculation can't
see the chosen targets. Underpaying is possible but wasted; the redirect can never be had below its
printed price.

---

## 8. No exact-X target floor — Word of Binding, and 11 others

`WordOfBinding.kt:25` uses `TargetCreature(optional = true, dynamicMaxCount = DynamicAmount.XValue)`.
`TargetRequirement.kt:101` — `val effectiveMinCount: Int get() = if (optional || unlimited) 0 else
minCount` — is the only floor `TargetValidator.kt:135` checks, and **`dynamicMinCount` does not exist
anywhere in the repo**. `dynamicMaxCount` is a ceiling only, and the client is told the same
(`TargetEnumerationUtils.kt:298`).

**Effect in play:** X=3 with one target declared is accepted. Usually self-harming, but not always
neutral — X inflates the spell's mana value on the stack, so a player can dodge "counter target spell
with mana value 3 or less", or satisfy a mana-value-matters trigger, while paying for only one
creature's worth of effect. Printed, CR 601.2c requires exactly X targets.

**Corpus-wide: 12 cards** share the shape — Icy Blast (KTK), Builder's Bane, Distorting Wake,
Candelabra of Tawnos, Elusive Otter, Aurelia's Vindicator, Doppelgang, Foggy Swamp Visions, Crashing
Wave, Rot-Curse Rakshasa, Divergent Equation, Word of Binding. Word of Binding is the only one of the
twelve with no scenario test.

**Fix:** add `dynamicMinCount: DynamicAmount?` to `TargetObject`, resolve it in the min path of
`TargetValidator` and `TargetEnumerationUtils.buildTargetInfos`, then flip all twelve cards off
`optional = true`. Medium engine unit plus a mechanical card sweep.

---

## 9. Cross-requirement distinctness is opt-in — Sorrow's Path

`SorrowsPath.kt:53-54` declares two independent one-target requirements. `TargetValidator.kt:147-151`
states the rule outright: *"Cross-requirement duplicates are a different 'target' instance and stay
legal by default — that distinctness is opt-in via TargetOther"*, and both the distinctness check
(`:149`) and the `sameController` check (`:166`) fire only **within** one requirement with count > 1.

**Effect in play:** the tap is a *cost*, so the `BecomesTapped` trigger (2 damage to you and each
creature you control) has already fired by the time the ability fizzles at resolution
(`SwapBlockingAssignmentsExecutor.kt:48`, `:70-72`). A player can announce the same blocker twice —
illegal per CR 601.2c, since "two target blocking creatures" is one instance of "target" — pay the
tap, eat 2 damage across their whole board, and get nothing. In multiplayer, two different opponents'
blockers do the same. Heads-up, only the duplicate-target case is reachable.
`SorrowsPathScenarioTest.kt:88` already proves the opponent-control half is rejected at announcement,
so the gap is specifically distinctness and "same".

**FIXED.** Collapsed to one requirement —
`target("blockers", TargetCreature(count = 2, sameController = true, filter = TargetFilter(opponentsBlockingCreature)))`
— which buys both CR 601.2c distinctness and the same-controller check at announcement. (Note the
activated-ability builder exposes `target(name, requirement)` but not the `targets(...)` plural form,
which lives only on `SpellBuilder`; the effect reads `ContextTarget(0)`/`(1)` off the flat positional
target list either way, so one requirement of two behaves identically for it.)
`SorrowsPathScenarioTest` covers it: naming the same blocker twice is rejected at announcement, the
land never taps, and its controller takes no damage. Verified by mutation — restoring the two
independent requirements turns that test red on the announcement assertion and leaves the other
three green.

---

## 10. Smaller, tracked

- **`Costs.ExilePermanentsFixed` is mislabelled to the client — City of Shadows.** The card script is
  exactly right, but `ActivatedAbilityEnumerator` hardcodes `costType = "ExileFromGraveyard"` and
  `pipelinePhases.ts` then sets `flags.targetZone = 'Graveyard'`, so the real UI opens a *graveyard*
  picker over battlefield creature ids. City of Shadows is the only user of that cost, and its
  scenario test bypasses the path (it passes `exiledCards` directly). Fix: make the enumerator's
  label follow `atom.zone` (as `SelectionCostPresentation` already does) and add an
  `ExileFromBattlefield` case in the client.
- **`Player.AnOpponent` asks one opponent — Scarwood Bandits.** Printed "unless an opponent pays {2}"
  lets *any* of them pay. Multiplayer-only; already noted in the card's KDoc, and blamed on the
  any-player pay executor not accepting mana costs.
- **`FlipCoinsExecutor` uses `flipperId = context.controllerId`** — so you flip the opponent's coin in
  Mana Clash. Observable only with a Krark's-Thumb-style replacement or a "whenever a player flips a
  coin" trigger, neither of which exists alongside this card.
- **`AnyPlayerMayPayExecutor`'s prompt doesn't name the permanent — Cleansing.** "For each land,
  destroy that land unless any player pays 1 life" shows five indistinguishable "Pay 1 life to
  prevent Cleansing's effect?" prompts on a five-land board.
- **Assay grammar gap (not ours): Scarecrow.** `assay-differential` flags it because
  `oracle-assay/.../grammar/Prevention.kt` has "dealt to you and {group}" and "dealt by {group}"
  frames but no "dealt to you this turn by {group}" frame, so Assay silently drops the recipient
  clause. **The hand-written card is the correct reading**; the fix belongs in Assay.

---

## Scenario-test coverage

70 of 119 DRK cards have a `<CardName>ScenarioTest.kt`. The 49 without are overwhelmingly vanilla or
single-keyword cards where the engine keyword is already covered elsewhere (Squire, Goblin Hero,
Scarwood Goblins, Carnivorous Plant, Land Leeches, Marsh Goblins, Bog Imp, Pikemen, Knights of
Thorn…). The per-card review flagged these as worth a test on their own merits, in rough priority:

- **Maze of Ith** — the script is faithful, but *no test anywhere in the repo* exercises
  `PreventCombatDamageToAndBy`, so the prevention half is unverified engine-wide.
- **Stone Calendar** — a cost-reduction static with nothing exercising it.
- **Mana Clash** — has a test, but it is vacuous: it asserts only `stack.size == 0` and `life <= 20`,
  both of which pass if the spell does nothing at all. Needs fixed-seed assertions.
- **Goblin Shrine** — its test claims to distinguish `LeavesBattlefield` from `Dies`, but killing the
  host is a dies via the CR 704.5m state-based action, so a `Dies` implementation would pass it. Needs
  a bounce/exile case.
- **Barl's Cage** — never advances to the *following* untap step, so a permanent lock would pass.
- **Giant Shark** — the middle clause (+2/+0 and trample when blocking a damaged creature) has no test.
- **War Barge** — never proves the "this turn" expiry of the drowning trigger.
- Then: Ball Lightning, Fissure, Hidden Path, Niall Silvain, Witch Hunter, Book of Rass, Standing
  Stones, Word of Binding, Skull of Orm, Grave Robbers.

**Stale `mtgish-tooling` generator banners** still sit on five files whose cards have since been
reviewed: Ball Lightning, Fountain of Youth, Inferno, Skull of Orm (Bog Imp's was removed during this
pass). Fountain of Youth also uses the raw `GainLifeEffect(1)` constructor rather than the
`Effects.GainLife(1)` facade — `FacadeBoundaryTest`'s regex list doesn't catch it.
