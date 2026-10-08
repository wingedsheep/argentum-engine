# SDK consolidation — what the type graph, card usage and code say

**Snapshot:** 2026-10-08, `main` @ `fa3672e42a`. Point-in-time report: counts go stale as cards and
SDK types land. Re-run the method rather than editing the numbers.
**Status:** P1 done (2026-10-08); P2–P6 proposed, not started. **Owner:** TBD.
**Related:** [`engine-sdk-architecture-review.md`](engine-sdk-architecture-review.md). Its §5
("one spelling per concept", largely shipped in #2348) removed *parallel spellings of one concept*.
This doc targets *groups of types that should be one concept*, and the process that keeps creating
them. §6 there (generate the boilerplate) is still open and is complementary.

## Method

**Quantitative pass.**
- Every public declaration in `mtg-sdk/src/main` (1,904 types) was parsed for its kind, sealed
  family, constructor fields, KDoc and `@SerialName`.
- Each type was measured against the 15,002 card definitions in
  `mtg-sets/src/test/resources/snapshots/cards/*.json`. The walk follows declared field types, so a
  shared discriminator like `"Fixed"` is credited to the right family.
- "Alike" means identical `name: Type` fields, shared name words, and the same family or file.
- The result is an interactive graph (the "Argentum SDK Atlas" artifact, private to the author).

**Qualitative pass.** Five read-only investigations, one per proposal area. Each one read:
- the engine executors and handlers behind the types;
- the card files that use them;
- the facades that build them;
- the `git log -S` history of when and why each type or field was added;
- the authoring guidance (`docs/sdk-design-principles.md` and the `add-feature` skill).

That pass changed several proposals. Changes from the first draft are marked *(revised)*. Rules
cited were checked against the 2026-09-25 Comprehensive Rules text.

**Limits.**
- Card usage only sees what a card's JSON contains. The 206 types outside any sealed family can't
  be measured, and "used by no card" has false positives: `ManaSymbol` (mana costs are strings) and
  `ManaRestriction.AllOf` (built by the engine at runtime, `BorrowedManaAbilities.kt:79`).
- Field likeness finds candidates. The qualitative pass is what separates real duplicates from
  look-alikes.

## Findings

### F1. A small core carries the corpus

| Family | Types | Types covering 90% of card uses | Types used by 1–2 cards |
|---|---|---|---|
| `Effect` | 379 | 34 | 148 |
| `StaticAbility` | 190 | 40 | 105 |
| `CardPredicate` | 117 | 28 | 46 |
| `Condition` | 115 | 21 | 54 |
| `EventPattern` | 101 | 13 | 36 |

- The most-used types are the composition primitives. `CompositeEffect` is in 5,096 cards,
  `GatedEffect` in 1,635 and `ForEachEffect` in 1,218. Each pipeline step (gather, select, move) is
  in about 2,000.
- Across all sealed families, 666 of about 1,700 serialized types are used by one or two cards.

### F2. Why the long tail exists *(revised)*

A sample of 41 types used by one or two cards was classified by reading each type, its handler and
its card:

| Class | Count | Meaning | Examples |
|---|---|---|---|
| A: genuinely its own type | 8 | One-of-a-kind rules | `RestartGameEffect` (CR 727), `WinCoinFlips`, `AddCountersWithLimitEffect`, `ExploitedEvent` |
| B: composable today | 12 | Expressible with existing primitives | `PowerAtLeastX` (`CompareNumericProperty`), `AnOpponentLifeAtMost` (`Compare(LeastAmongPlayers…)`), `DrawUpToEffect` (`ChooseNumberThen` + draw), `FlipTwoCoinsEffect` |
| C: an existing type plus an axis | 19 | A missing parameter or sibling family | `SkipNextDrawStep` / `SkipNextUntapStep` (one `SkipNext(TurnPart)`), `GrantHexproofFromMonocolored` / `…Multicolored`, five "grant X of the chosen Y" effects, about ten "cards in zone Z have keyword K" statics, three set-base-P/T statics |
| D: card-shaped | 2 | Named and parameterised for one card | `EachPlayerDiscardsOrLoseLifeEffect` (Strongarm Tactics) |

The first draft blamed card-specific types. The data says otherwise:

- **Missing axes (C) are the biggest group.** Card-specific types (D) are rare, and names are
  almost always mechanic names.
- **11 of the 12 composable types (B) are fossils.** Each was reasonable when added. The general
  primitive came later and nobody migrated the old type:
  - `PowerAtLeastX` (Jul 30) predates `CompareNumericProperty` (Oct 1);
  - `AnOpponentLifeAtMost` (Aug 1) predates `LeastAmongPlayers` (Oct 7);
  - `DrawUpToEffect` (Feb) predates `ChooseNumberThen` (May).
- **The guidance makes a new type the cheapest change.**
  - `docs/sdk-design-principles.md:27` and `add-feature/SKILL.md:231` say *"Don't extend an
    existing effect with a new optional parameter to cover a variation. Compose instead."* When
    composition fails, that leaves only a new type, which forbids class C outright.
  - `docs/sdk-design-principles.md:25` says "inline with explicit flags otherwise", which invites
    Booleans.
  - `add-feature/SKILL.md:209`: changing a type owes Assay grammar work and the gate, but adding
    one owes "nothing".
- **The catalog has stopped working for discovery.** `docs/card-sdk-language-reference.md` is
  1.6 MB (16,910 lines, the longest a single 10,690-character line). `CompareNumericProperty`,
  which subsumes the X-predicates, has no entry; `PowerAtLeastX` does.
- **Who adds types.** Nearly all introducing commits are agent-authored.
  - Since 2026-09-20, autonomous loops are the main source of new SDK types: 24 of the 36 added in
    October came from loop commits.
  - Commits whose subject says "collapse", "unify" or "consolidate" fell from 26–29 a month (Feb,
    May) to 3–5 a month since July.
- **The fixed cost of a type is about six files:** the SDK type, facade, docs row, executor,
  registry line and serialization registration. Real rules work comes on top of that.

### F3. Wide types grew one card at a time *(revised)*

Field history from `git log -S` shows one card, one new flag, almost without exception. For
example, `GrantMayPlayFromExileEffect` gained 13 fields between April and October:
- `withAnyManaType` came with Taster of Wares;
- `nonLandOnly` with Ragavan;
- `singleUse` with Chandra, Hope's Beacon;
- `colorlessAsAnyColor` with Abstruse Appropriation.

Snapshot analysis of which fields each card actually sets:

| Type | Objects / cards | Distinct field combinations | Fields used by ≤2 cards |
|---|---|---|---|
| `GrantMayPlayFromExileEffect` | 191 / 180 | 22 | 9 of 18 |
| `PreventDamageEffect` | 150 / 139 | 29 | 6 of 15 |
| `CreateDelayedTriggerEffect` | 185 / 163 | 22 | 3 of 13 |
| `CreateTokenCopyOfTargetEffect` | 100 / 88 | 36 | 9 of 27 |
| `CreateTokenEffect` | 1,163 / 1,077 | 101 | 5 of 26 |
| `MoveCollectionEffect` | 3,037 / 2,248 | 32 | 3 of 17 |

**Width is not the problem; fields that depend on each other are.** The co-occurrence data shows
hidden structure:

- **`CreateDelayedTriggerEffect`:** `step` (113 cards) and `trigger` (50) never co-occur and
  together cover every object, so it is a sealed choice. The executor drops `fireOnce` and `expiry`
  on the step path, and Stone Giant sets both.
- **`PreventDamageEffect`:**
  - three spellings of "react to prevented damage" never co-occur: `onPrevented`,
    `gainLifeFromPrevented` and `gainLifeFromColors`;
  - `halvePreventedDamage` only appears with `nextInstanceOnly`;
  - `toPlayersOnly` only appears with `direction = FromTarget`;
  - `target = Controller` is overloaded to mean "no recipient", a global Fog.

  The executor is a 12-branch `when` whose result depends on branch order
  (`PreventDamageExecutor.kt:257–388`). Several branches silently drop `scope`, `amount` or
  `recipientGroup`.
- **`GrantMayPlayFromExileEffect`:** three pairs of fields never co-occur, because each pair
  spells one choice two ways:
  - how mana may be spent: `withAnyManaType` / `colorlessAsAnyColor`;
  - who gets the permission: `ownerControls` / `recipient`;
  - the alternative cost: `fixedAlternativeManaCost` / `fixedAlternativeCostIsManaValue`.

  The executor resolves the conflicts by precedence: `ownerControls` silently beats `recipient`.
- **`MoveCollectionEffect` is wide but healthy.** 1,420 objects are a bare `from + destination`.
- **A token split already exists but stalled.** `CopyExceptions` was designed to absorb the flat
  riders on the copy-token effects. Only 3 of 100 snapshot objects use it, and the facade still
  exposes every rider.
- **Boolean count is a weak signal.** 20 of 1,223 types with fields have four or more Booleans. Some
  are healthy and orthogonal (`ActivatedAbility`, `TargetObject`), and the count misses
  `PreventDamageEffect`'s real problem: nullables and enums that depend on each other. The SDK's
  own KDocs already flag the real smell, with 58 phrases like "only meaningful with", "ignored
  unless", "mutually exclusive" and "takes precedence".

### F4. Static and effect spellings of one rule change *(revised)*

**The engine already merges the two paths.** `StateProjector.kt:946–988` turns every floating
effect into the same `ContinuousEffect` that `StaticAbilityHandler` produces for statics, and
`FloatingEffectFactory.addFloatingEffect` removed the executor boilerplate in March. So the
duplication lives in the SDK types and a thin executor or handler shell. The rules logic isn't
duplicated.

**Pairs.** Besides CantBlock, CantAttack, GrantKeyword, RemoveKeyword, ModifyStats and
MustBeBlocked, the code reading found more:

| Static | Effect |
|---|---|
| `LoseAllAbilities` | `RemoveAllAbilitiesEffect` |
| `CantBeBlockedExceptBy` | `GrantCantBeBlockedExceptByEffect` |
| `SetBasePowerToughnessStatic` | `SetBaseStatsEffect` |
| `GrantColor` | `AddColorEffect` |
| `ControlEnchantedPermanent` | `GainControlEffect` |
| `MustBlock` | `MarkMustBlockThisTurnEffect` |
| `MustAttack` | `MarkMustAttackThisTurnEffect` |

**Where the pairs genuinely diverge:**
- **ModifyStats.** The static takes a fixed `Int` (732 cards) and has a separate
  `GrantDynamicStats(DynamicAmount)` (102 cards) that is re-evaluated continuously. The effect locks
  its `DynamicAmount` at resolution.
- **MustBeBlocked.** The static is not projected: `StaticAbilityHandler.kt:778` maps it to `null`,
  and `BlockPhaseManager` scans printed statics instead. The effect is a floating effect hard-coded
  to end of turn.
- **Can't be blocked is spelled three ways:**
  - the `CantBeBlocked` static, which becomes a keyword;
  - `flags(AbilityFlag.CANT_BE_BLOCKED)`;
  - `Effects.GrantKeyword(CANT_BE_BLOCKED)`.

  Because it is a keyword, `RemoveAllAbilities` clears it, but it doesn't clear `cantBlock`.
- **Must attack is stored four ways:**
  1. the projected `SetMustAttack` flag;
  2. `MustAttackThisTurnComponent`;
  3. `GoadedComponent`;
  4. granted `MustAttack` statics.

  They are merged at `AttackPhaseManager.kt:769–1001`.
- **A third path, `GrantStaticAbilityEffect` (34 card files), writes `grantedStaticAbilities`.**
  Projection only reads attack permission from it (`StateProjector.kt:499–505`), so granting a
  `CantBlock` static this way would silently do nothing.

**The shared-shape groups are smaller than the field match suggested.**
- **Target-only effects:** of the 54 with just `(target: EffectTarget)`, about 13 are
  modification-shaped. The rest are actions: shuffle, proliferate, transform, phase out, skip and
  so on.
- **Filter-only statics:** of the 28 with just `(filter: GroupFilter)`, 17 map to a projected
  `Modification`. About 10 are rules read at the point of use, and several are pairwise (blocker
  versus attacker) or about combat damage assignment.
- **Goad (CR 701.15) and Taunt are designations, not flags,** and should stay bespoke.

**The real semantic constraint.** CR 611.2c says an effect from a resolving spell that modifies
characteristics or control locks its affected set. Rules-modifying effects ("can't block") also
apply to objects that arrive later. CR 611.2d fixes X at resolution, and CR 611.3a says statics
aren't locked in.

The engine encodes this, but the author picks the shape:
- characteristic group effects go through `ForEachInGroup`, one locked effect per creature
  (`GroupPatterns.kt:184–268`);
- rules-modifying group effects use a single effect with a live filter (`CantBlockGroupExecutor`).
  That shape came from a bug fix, 59254ea0c2: "Fix CantBlockGroup not applying to creatures
  entering after resolution".

### F5. Combinators: same idea, five spellings, six semantics *(revised)*

| Family | "all" | "any" | "not" |
|---|---|---|---|
| `Condition` | `AllConditions` | `AnyCondition` | `NotCondition` |
| `CardPredicate` | `And` | `Or` | `Not` |
| `StatePredicate` | `And` (0 cards) | `Or` | `Not` |
| `ControllerPredicate` | `And` | `Or` (0 cards) | `Not` |
| `ActivationRestriction` | `All` | — | — |
| `CastRestriction` | `All` (0 cards) | — | — |
| `ManaRestriction` | `AllOf` (built at runtime) | `AnyOf` | — |
| `EventPattern` / `Recipient` / `SpellCastPredicate` | — | `AnyOf` | — |

- **The names diverged by accretion.** Different agents added them at different times. There was
  no naming decision.
- **Lists already mean AND almost everywhere:**
  - `GameObjectFilter.cardPredicates` / `statePredicates`;
  - `ActivatedAbility.restrictions`;
  - `castRestrictions`.

  So most AND nodes are redundant. All 17 uses of `ActivationRestriction.All` are
  `listOf(All(a, b))`, which is equivalent to `listOf(a, b)`.
- **Evaluation is not one semantics.** Six shapes exist in `rules-engine`:
  1. ordinary short-circuit;
  2. three-valued logic for last-known-info snapshots (`PredicateEvaluator.kt:302–360`);
  3. first-failure reason strings (`LegalityKernel`);
  4. structural walkers for target enumeration;
  5. rewrites;
  6. UI flattening.

  `CardPredicate` combinators are matched in about 8 separate evaluators. One shared helper
  already exists for one family, `ControllerPredicate.evaluateWith`.
- **Cost combinators are not boolean logic.**
  - `Composite` means "pay every part, in order", and storage flows between the steps.
  - `Choice` is a player decision, offered as separate legal actions.
  - `OrPay` folds its mana leg into the spell's cost.
- **A generic `Logic<T>` is not feasible in kotlinx.serialization.** Sealed subtypes must share a
  package, `AllOf<T> : T` can't be declared, and type safety would be lost.
- **`@SerialName`s can't take aliases.** kotlinx has no alias mechanism for class
  discriminators, and `GameState`, which embeds SDK nodes, is persisted for live games
  (`PersistentGameSession`).

### F6. Branch fields inside effects *(revised)*

**The outcome channel P4 asked for already exists.** `PipelineState` carries stored numbers and
collections across pauses (`PipelineCollectionPropagation.kt:40–57`), and several newer effects
already record outcomes there:
- `FlipCoinsEffect.storeHeadsAs`;
- `FlipCoinsUntilLossEffect.storeWinsAs`;
- `PlayerGuessesConditionEffect.storeGuessedRightAs`. Its KDoc gives the reason: Liar's Pendulum
  needs a step between the guess and the payoff.

Clash's "if you win" stores into a collection and gates on it with `Gate.DoAction`.

| Effect | Fits "record outcome + `Effects.If`"? |
|---|---|
| `FlipCoinEffect`, `FlipTwoCoinsEffect` | Yes, as `FlipCoins(n, store)` (`FlipTwoCoins`' `mixedEffect` is unused) |
| `OpponentGuessesTopCardKindEffect`, `OpenLifeBidEffect` | Yes: store won or right as 1/0. The bidding loop stays. |
| `BeholdEffect`, `MayRevealCardFromHandEffect` | Yes, but they keep their type and their reveal; only the branch fields go |
| `ConditionalOnCollectionEffect` (125 cards) | Yes. It is `If(collection condition)` and predates `CollectionContainsMatch`. Cards reach it only through `PipelineBuilder.ifNotEmpty`. |
| `SecretBidEffect` | No. Each bidder runs their own branch with their own controller and X. |
| `PayOrSufferEffect`, `AnyPlayerMayPayEffect` | No. Kept bespoke on purpose by the gated-effect migration (multi-player APNAP). |

## Bugs found during the research

These are separate from the proposals. Each deserves its own fix PR with a scenario test.

| Bug | Confidence | Status |
|---|---|---|
| **Decorated Griffin's combat-only shield also stops noncombat damage.** `Effects.PreventDamage(amount = 1, combatOnly = true)` falls through to the `amount != null` branch (`PreventDamageExecutor.kt:363`), which builds `PreventNextDamage(1)`. That modification has no combat-only field, and the shield consumption at `DamageUtils.kt:1578` never checks combat. A Shock would use up the shield. | Confirmed by reading; no test exists | [x] Fixed — #2937 |
| **`ProvokeExecutor.kt:40` checks the printed `typeLine.isCreature`** instead of projected state, against the projected-state rule. | Confirmed by reading | [x] Fixed — #2934 |
| **`CreateDelayedTriggerExecutor` drops `fireOnce` and `expiry` for step triggers** (around `:144`, `:168`). Stone Giant sets both and works by accident. | Reported, not re-checked | [x] Not a bug — step triggers are always consumed on fire (`fireOnce` is documented as ignored for them), and honouring the default `EndOfTurn` expiry on one-shot step triggers would delete "next upkeep" triggers before they fire. Stone Giant works by design. |
| **A granted `CantBlock`-style static does nothing.** Granting a projected static through `GrantStaticAbilityEffect` is ignored by projection (`StateProjector.kt:499–505`). | Reported, not re-checked | [x] Fixed — #2940 (every granted projected static kind was dropped, not just `CantBlock`) |
| **`MustBeBlocked` statics ignore lost abilities.** `BlockPhaseManager.kt:1261–1281` scans printed statics directly. | Unsure | [x] Fixed — #2941 (also face-down and granted cases, plus "can't be blocked by more than N") |
| **`RemoveAllAbilities` treats two evasion flags differently.** It clears "can't be blocked" (a keyword) but not "can't block". | Unsure whether intended | [x] Fixed — #2942 (a creature's own "can't block" is now lost with its abilities; one from another source stays) |
| **Delayed-trigger target baking only handles one gate.** It covers `GatedEffect` only for a bare `MayDecide` (`CreateDelayedTriggerExecutor.kt:535–543`), so a delayed `If(…)` over a context target may lose it. Latent today; blocks P4. | Reported, not re-checked | [x] Fixed — #2938 (bakes through every gate kind) |
| **`SecretBidEffect` branch failures.** Its per-bidder branch ignores a paused result and drops targets and pipeline state (`CardSpecificContinuationResumer.kt:192–238`). | Reported, not re-checked | [x] Fixed — #2935 (hardening: latent, Menacing Ogre can't reach it) |

## Proposals

Reordered by the research. P1 addresses the cause; P2–P6 pay down the debt. Prove each migration
by an alpha-equivalent golden re-bless, as #2348 was.

### P1. Make extending cheaper than adding *(new)* — ✅ done

- [x] **Done 2026-10-08.** What shipped, item by item:
  1. `docs/sdk-design-principles.md` § "Extend before you add" and `add-feature` Step 3. Both mirrored in
     `add-card/new-sdk-types.md` and the `review-changes` SDK lens.
  2. Delete-what-you-subsume is now a rule in both docs and an `add-feature` anti-pattern.
  3. `add-feature` Step 9 now treats widening and adding the same way. Assay work triggered by a new
     axis is stated as expected.
  4. `SdkSurfaceBaselineTest` (`:mtg-sdk`) works against
     `mtg-sdk/src/test/resources/sdk-surface-baseline.txt`. The 1,630 existing leaves are listed as
     `[grandfathered]`; new ones go under `[added]` as `name — family — first card — closest — why not`.
  5. `just sdk-tail` (`SdkTailReport`) writes per-family tail shares plus fossil candidates, by shape and
     by 1:1 lowering. It is wired into the set-loop finishing PR.
  6. The catalog is searchable through a generated `docs/sdk-index.md`: one line per sealed type with its
     fields and KDoc summary, kept current by `SdkIndexTest` and `just sdk-index`. The catalog now
     states a one-line entry style.
     - **Still open:** cutting the existing long entries in `card-sdk-language-reference.md` down to one
       line, with detail moved into KDoc. Do that as entries are touched, or as part of the review's §6
       catalog split.
  7. Retired:
     - the X predicates;
     - the life conditions: `AnOpponentLifeAtMost`, `EachPlayerLifeAtMost`, and their unlisted
       sibling `APlayerLifeAtMost`;
     - the skip trio, now `SkipNextStepOrPhaseEffect` / `SkipStepOrPhase` over `TurnPart`;
     - the hexproof-from pair, now `GrantHexproofFromToGroup(ProtectionScope)`;
     - `TriggeringEntityHad*`, now an LKI-reading `TriggeringEntityWas(filter)`;
     - two of the three name predicates, now `SharesNameWithPermanentYouControl(filter, excludeSelf)`.

     **Left on purpose:** `NameNotSharedWithControlledRoom`. It compares against the unlocked door
     names of Rooms, which isn't the same as "shares a name with a permanent matching a filter".


The long tail is mostly missing axes and unmigrated fossils, and the guidance steers toward both.
Fix the guidance first, or every later consolidation regrows.

1. **Rewrite the rule in `docs/sdk-design-principles.md` and `add-feature`.**
   - Replace "don't extend with a new optional parameter" with an order of preference: *compose
     → add an axis to the closest existing type (rename or migrate it if the name no longer fits)
     → new type*.
   - A new type must name its two closest existing types and why neither can take the axis.
2. **Make a new primitive delete what it subsumes.** "When your new general primitive subsumes
   an older narrow type, migrate its cards and delete it in the same PR." This targets the
   fossils directly.
3. **Remove the cost asymmetry in `add-feature` Step 9.** Either adding a type owes the same Assay
   note as changing one, or the skill says outright that Assay work triggered by a new axis is
   expected.
4. **Add an `SdkSurfaceBaselineTest`,** modelled on `EffectExecutorCoverageTest`.
   - A checked-in list of sealed leaves per family. Adding a leaf requires a line: *name — family —
     first card — closest existing types — why not those*.
   - On failure, the test prints the nearest neighbours: same family, identical fields, shared name
     words.
   - It is a review prompt, not a block.
5. **Add a `just sdk-tail` report.** It prints the share of types used by 1–2 cards per family and
   lists fossil candidates: types whose fields are a subset of another's, or whose handler lowers
   1:1 to another type, like `CantBeSacrificed` → `GrantKeyword(CANT_BE_SACRIFICED)`.
   - Read it in the set-loop finishing PR.
   - More than about one net new type per loop PR is a review flag. No hard threshold yet.
6. **Make the catalog searchable again.**
   - One line per entry in `card-sdk-language-reference.md`, with detail moved into KDoc.
   - Ideally a generated per-family index of names and fields. This overlaps the review's §6
     catalog split.
7. **Retire the fossil and axis groups found in F2.** Small, safe PRs, each a good first proof of
   the new rule:
   - `PowerEqualsX` / `PowerAtLeastX` / `ToughnessAtMostX` → `CompareNumericProperty`;
   - `AnOpponentLifeAtMost` / `EachPlayerLifeAtMost` → `Compare` over `Least/GreatestAmongPlayers`;
   - `SkipNextDrawStep` / `SkipNextUntapStep` / `SkipUntapStep` → one skip type over `TurnPart`;
   - the hexproof-from pair → `GrantHexproofFrom(quality)`;
   - `TriggeringEntityHadSubtype` / `…CardType` → an LKI-reading `EntityMatches`;
   - the three "name not shared with" predicates → one with a filter.

### P2. Regroup wide types by co-dependence, validated at the facade *(revised)*

Regroup fields that depend on each other into sealed options. Keep the flat facade signatures, and
have the facades `require()` valid combinations. Card files don't change: `Effects.PreventDamage`
is the only construction point for 168 card files, and `Effects.GrantMayPlayFromExile` for 120.

1. **Pilot: `PreventDamageEffect`.**
   - Write the Decorated Griffin test first.
   - Regroup into `recipients` (Target / You / Group / None), `shield` (All / NextN / NextInstance)
     and an optional `reaction` (Run effect / GainLifeFromPrevented / GainLifeFromColors).
   - That takes 15 fields to about 7, turns the global Fog into an explicit `None`, and replaces the
     order-dependent `when`.
2. **Finish `CopyExceptions`** on the copy-token effects. The design exists; remove the frozen
   riders from the facade.
3. **`GrantMayPlayFromExileEffect` → a `CastPermission` set of terms:**
   - grantee;
   - alternative cost;
   - mana spending;
   - play scope: cast-only, face, color, single use, flash timing, land enters tapped.

   Decide first whether the static may-cast types share it. They spell the same ideas differently:
   `oncePerTurn` versus `singleUse`, `exileInsteadOfGraveyard` versus `insteadOfGraveyard`. Their
   lifecycles differ, so perhaps only the terms are shared.
4. **`CreateDelayedTriggerEffect` → sealed `AtStep` / `OnEvent`.** The cleanest data, but the facade
   is a pass-through used by 151 card files, so either add new facades or keep the old one as an
   adapter.
5. **A shared `TokenCreation`** (count, controller, tapped, attacking, cleanup) for the token
   effects. Lower priority: CreateToken's width is mostly orthogonal.

**Review rule.** A field whose KDoc needs "only meaningful with", "ignored unless" or "mutually
exclusive with" belongs in a sealed option. Field counts are only a review trigger: at four or more
Booleans or twelve or more fields, the PR says why the new field is orthogonal.

### P3. One continuous-modification vocabulary, scoped to what projects *(revised)*

The direction holds: one set of modifications, applied by a static or by a resolving effect. The
research changes its shape:

- **Affected objects are a target or a group:** `Apply(modification, affected = Target(t) |
  Group(filter), duration)`. Without the group form, "creatures you control can't block this turn"
  can't be said.
- **Locking follows the rules, not the author.** Each modification declares whether it changes
  characteristics or control, or modifies rules.
  - The generic executor then locks the affected set per CR 611.2c, or keeps a live filter.
  - That replaces today's author-chosen `ForEachInGroup`-versus-group-type split.
  - `DynamicAmount` locks for effects (CR 611.2d) and stays live for statics.
    `SetBaseStatsEffect.reevaluateContinuously` remains as an opt-in.
- **Events need a per-variant hook.** Keyword grants and stat changes emit events today. Control
  keeps its own executor, because of summoning sickness and `ControlChangedEvent`.
- **Only modifications that project are in scope.**
  - Prevention, redirection, shields and replace-draw stay out: they are 34 of the 63
    `SerializableModification` variants that map to `NoOp`, are CR 614/615 effects, and are read
    directly by damage code.
  - Goad, Taunt and the pairwise combat statics also stay out.
- **Keep `GrantStaticAbility` separate.** "Gains '…'" grants an ability, which ability-loss effects
  can remove; an applied rule change is not an ability. But fix the silent no-op by routing
  granted statics through `StaticAbilityHandler`.
- **Expect card-file churn on the static half.** Card files construct statics directly:
  `ability = ModifyStats(` appears 733 times. Either add top-level factory functions with the old
  names, or accept the churn. The effect half is facade-only.

**Pilot: CantBlock, CantAttack and MustBlock, with their group forms.** Not GrantKeyword, which has
about 1,950 uses plus targets on the stack, a condition, an event and AI reads. This pilot fits
because:
- both halves already share one `Modification`;
- usage is small;
- nothing emits events;
- it exercises the live-filter path.

**Then:**
- `ModifyStats` + `GrantDynamicStats`, which proves locking and folds `modifyStatsForAll`;
- MustBeBlocked and must-attack's four storages, which carry the real bugs.

**What becomes of the old "combat rules as data" proposal.** It shrinks to the roughly 13 + 17
modification-shaped types from F4 and becomes part of this proposal's rollout.

### P4. Effects record their outcome; `Effects.If` branches *(revised)*

No new slot type. Action effects store into the existing pipeline channel. Add named conditions so
descriptions read like Oracle text: `Conditions.WonCoinFlip(slot)` reads "if you win the flip"
where `Compare(VariableReference…)` reads "that much is equal to 1".

**Prerequisites:**
1. Widen delayed-trigger target baking to every `GatedEffect`. Without it, Goblin Kites would
   silently stop sacrificing.
2. Make Krark's Thumb's resume publish the flip count for single flips.
3. Add a scenario test for `Gate.DoAction` inside an enter-the-battlefield replacement (Molten
   Sentry, Behold). That path has no existing coverage.

**Pilot:** FlipCoin + FlipTwoCoins in one PR, lowered inside the existing facades, so no card files
change.
- Tests: Goblin Kites, Molten Sentry, Two-Headed Giant and a Krark's Thumb flip.
- Then Behold / MayReveal, then `ConditionalOnCollection` via the `ifNotEmpty` lowering.
- Update Assay in the same PR (it emits `FlipCoinEffect` and `ConditionalOnCollectionEffect`).

**Out of scope:** SecretBid, PayOrSuffer and AnyPlayerMayPay stay bespoke, as do the binder effects
(`ChooseColorThen`, `ChooseNumberThen`, `Discover`).

### P5. Combinators: rename the classes, freeze the wire names *(revised)*

1. **Delete the redundant AND nodes:**
   - `ActivationRestriction.All`: unwrap its 17 uses into the list;
   - `CastRestriction.All`;
   - `StatePredicate.And`: first check `EffectDiscardDestinations.kt:35`, which matches it.

   Keep `ManaRestriction.AllOf` (built at runtime) and `ControllerPredicate.Or` (needed under
   Not/Or).
2. **Add shared interfaces for the logic families.** Plain, non-serialized SDK interfaces, for
   example `Junction<T> { val operands: List<T> }` and `Negation<T> { val operand: T }`. They
   carry shared folds generalised from `ControllerPredicate.evaluateWith`:
   - `fold2`: short-circuit;
   - `foldKleene`: three-valued, for last-known-info snapshots;
   - `firstFailure`: the first failing reason;
   - `children()`: for the walkers.

   Migrate the roughly 20 duplicated fold sites. Keep the per-site leaf hooks: the spell-or-ability
   ordering in `PredicateEvaluator.kt:650–667` depends on them.
3. **Rename the Kotlin classes to `AllOf` / `AnyOf` / `Not`, but keep every `@SerialName`.**
   Goldens and persisted games then don't move, and the refactor stays provably alpha-equivalent.
4. **Add `Predicates.*` facades and widen `FacadeBoundaryTest`,** so the codemod touches card
   files once. Today about 150 card files construct `CardPredicate.Or/And/Not` and
   `ControllerPredicate.*` directly. Assay emits them too (36 references).

**Out of scope:** cost combinators (pay-in-order and player choice are different concepts),
`DynamicAmount` arithmetic, static-ability composites, and merging `GameObjectFilter.anyOf` with
`CardPredicate.Or` (a real design question of its own).

**Pilot:** `StatePredicate`. It is small: 6 card files, about 112 golden occurrences and about 12
engine sites. Prove it with an unchanged snapshot hash.

## Not proposed

- **Merging the `KeywordAbility` `(cost: ManaCost)` group.** The members share a shape, not rules.
- **Splitting `ActivatedAbility`, `TargetObject` or `CardScript`.** They are wide, but their fields
  are orthogonal.
- **A generic `Logic<T>` type, or changing serialized discriminators,** for the reasons in F5.
- **Deleting "used by no card" types without checking `rules-engine`.** Some are built at runtime
  or only reached through string-serialized fields.
