# SDK Effect-Atom Audit (2026-06-26)

A full survey of **all 284 effect types** across the 41 files in
`mtg-sdk/.../scripting/effects/`, rated against the architecture vision in
[`docs/architecture-principles.md`](../docs/architecture-principles.md) §1.5
*Atomic Effect Pipelines*: **complex behaviour composed from small reusable primitives,
so new sets ship with zero new effect code.**

Complements the existing redundancy backlog
[`sdk-reusability-consolidation.md`](sdk-reusability-consolidation.md) — that file is the
canonical migration tracker; this one is the evidence-gathering pass that re-derived the
picture bottom-up (read every definition, grepped real card reuse) and grades each cluster.

Rating scale per effect:

- **A** — exemplary atom: small, composable, reused at scale, well parameterized.
- **B** — solid primitive, minor improvements possible.
- **C** — smell: too specific, overlaps another effect, or should be a parameter/composition.
- **D** — one-off monolith that violates "compose, don't add" — bespoke for one card.

---

## Headline verdict

**The vision is real and largely working — but 284 effect types is itself the finding.**
A small spine of heavily-reused, well-parameterized atoms carries thousands of cards. Layered
on top is a long tail of **single-card monoliths** and **near-duplicate families
distinguished by one field** that the project keeps meaning to fold in. The SDK is roughly
**~75% of the way to its own ideal** — the architecture is sound and the discipline is mostly
applied; the remaining gap is cleanup of known holdouts, not any structural flaw.

The most telling signal: **the codebase's own kdoc repeatedly admits the duplication.**
`CopyTargetSpellOrAbilityEffect` says it "generalizes" two effects that still ship separately;
`ChangeTriggeringObjectTargetsEffect` calls itself "the non-random counterpart of
`ReselectTargetRandomlyEffect`"; `GrantToxicEffect` admits it just emits a `TOXIC_<n>` string
keyword; `GrantStaticAbility`'s kdoc prescribes a `ForEachInGroup`+`Self` composition that
three sibling `*Group` effects ignore. The SDK knows where its smells are; they just haven't
been swept.

---

## Cluster scorecard

| Cluster | Grade | State |
|---|---|---|
| **Control-flow combinators** (ForEach, Gated, Conditional, Modal, Composite) | **A** | Flagship. `ForEachEffect/IterationSpace` and `GatedEffect/Gate` each collapsed a whole wrapper zoo into one executor + one sealed axis; legacy names survive as thin facades. The model. |
| **Library / Draw / Reveal / Pipeline** | **A−** | Gather→Filter→Select→Move spine is exemplary (`GatherCards`+`CardSource`, `FilterCollection`+`CollectionFilter`). Marred by 3 monoliths and central atoms accreting boolean riders. |
| **Mana** | **A−** | `AddMana` + `AddManaOfChoice` plus orthogonal `ManaExpiry`/`ManaRestriction`/`ManaSpellRider` axes are textbook. 4 add-effect variants + 5 fixed `ManaRestriction` objects still owe consolidation. |
| **Damage / Combat / Life** | **B+** | `PreventDamageEffect` ("replaces all previous prevention types with one parametrized type") and `SetSuspected` are best-in-class. Drag: the `CantAttack/CantBlock` ×4 quartet and the redirect family. |
| **Keyword / Type / Transform / Permanent / Tap** | **B** | Anchored by `GrantKeyword` (704 uses), `TapUntap` (`tap:Boolean`), `BecomeCreature`. Largest cluster carries the most filter-variant + single-keyword-grant duplication. |
| **Control / Removal / Player / Protection** | **B** | Strong base atoms (`GainControl`, `Regenerate`, `Sacrifice`). Pervasive *recipient/quality fragmentation*. |
| **Counters / Stats** | **B−** | `AddCounters`/`ModifyStats`/`Proliferate` excellent; `GrantCounterPlacementModifier` is a model "general by construction" atom. ~6 single-card counter-move/distribute one-offs that cross-reference each other in their own kdocs. |
| **Token / Copy / Stack** | **B−** | Contains both the best (`CounterEffect` absorbed 7 types; `ChainCopyEffect`) and the worst (5 `CreateTokenCopy*` fragmented on copy-source; stalled spell-copy merge). |

---

## What's exemplary (proof the vision works)

Every new effect should imitate these — single executor, single sealed "axis," reused at scale:

- **`ForEachEffect` + `IterationSpace`** and **`GatedEffect` + `Gate`** — the two flagship
  unifications. The entire legacy wrapper set (`MayEffect`, `OptionalCost`, `Conditional`
  (234 uses), `IfYouDo`, `MayPayMana/X`, five `ForEach*`) survives only as facades lowering
  onto these.
- **`CounterEffect`** — kdoc proudly names the **7 effects it replaced**.
- **`PreventDamageEffect`** — one parametrized type expresses CoP, Deflecting Palm, Eye for an
  Eye, Samite Ministration.
- **`AddManaOfChoiceEffect`** — replaced 5 legacy mana effects via one `ManaColorSet` + riders.
- **`GrantKeyword`** (704), **`ModifyStats`** (911), **`DealDamage`** (~930),
  **`AddCounters`** (346), **`GatherCards`** (384), **`MoveCollection`** (370) — high-traffic
  leaf atoms.
- **`GrantSuspendEffect`** / **`SetSuspectedEffect`** — pure marker atoms that let
  `MoveToZone`+`AddCounters`+`GrantKeyword` do the real work. Gold standard for compose-don't-add.

---

## The five systemic smells

The duplication isn't random — it falls into five repeating patterns.

### 1. Recipient/quality fragmentation — *same effect, differs only in who/what receives it*

- **Control:** `GainControlByActivePlayer`, `GainControlByMost`, `GiveControlToTargetPlayer`
  → one `GainControl(recipient: ControlRecipient)` (preserve the good `PlayerRankMetric` sealed
  type as a `ControlRecipient.PlayerWithMost`).
- **Grants:** `GrantShroud`/`GrantHexproof`, `GrantHexproofFromChosenColor`/`GrantProtectionFromChosenColor`,
  `GrantFlashToSpells`/`GrantSpellsCantBeCountered` → keyword/quality parameter.
- `GrantToxic` → `GrantKeyword("TOXIC_$n")` facade (the codebase already does exactly this for
  `GrantProtectionFromColor`).

### 2. Filter/`*Group` variants re-implementing a single-target atom

The codebase's own guidance (`GrantStaticAbility` kdoc) says to use `ForEachInGroup`+`Self`.
Offenders ignoring it: `CantAttackGroup`/`CantBlockGroup`, `SetGroupCreatureSubtypes`,
`ChangeGroupColor`, `GrantActivatedAbilityToGroup`, `AddCountersToCollection`.

### 3. "Choose source" fragmentation — *same operation, differs only in how input is selected*

- **Worst single offender:** five `CreateTokenCopy{OfSource,OfTarget,OfChosenPermanent,OfEquippedCreature,Random…}`
  → one `CreateTokenCopy(source: CopySource, mods: CopyModifications)`. `…OfTarget` has grown
  ~20 modifier fields while siblings re-declare uneven subsets.
- The retarget quartet: `ChangeSpellTarget`/`ChangeTarget`/`ReselectTargetRandomly`/`ChangeTriggeringObjectTargets`
  → one `RetargetEffect(scope, chooser, constraint)`.
- The counter-move family: `MoveCounters`/`MoveChosenCountersToTarget`/`MoveCountersEachKindMissing`/`MoveAllLastKnownCounters`
  + the two `Distribute` + `RemoveAll`/`RemoveAnyNumber` → one `MoveCounters` over
  (source × destination × kind-selector × amount).

### 4. Stalled merges — *generalizing type exists, specific siblings still ship*

- Spell-copy family: `CopyTargetSpellOrAbility` already "generalizes" `CopyTargetSpell` +
  `CopyTargetTriggeredAbility`; finish into `CopyOnStackEffect(scope, riders)` (also folds
  `CopyEachTargetSpell`).
- `ChooseOptionEffect(CREATURE_TYPE)` is "the generic replacement" yet legacy
  `ChooseCreatureTypeEffect` still has ~15 uses.
- Near-duplicate pairs: `GrantHarmonize`/`GrantFlashback` (→ `GrantGraveyardCastKeyword`),
  `ChangeCreatureTypeText`/`ChangeWordInText` (→ `ChangeTextWords(category)`),
  `CopyNextSpellCast`/`CopyEachSpellCast` (→ `consumption` param),
  the three coin effects (→ `FlipCoins` + a gate), `SetBasePower`/`SetBasePowerToughness`
  (→ `SetBaseStats(power: DynamicAmount?, toughness: DynamicAmount?)`, also fixes the
  `DynamicAmount`-vs-`Int` asymmetry), `AddCreatureType`/`AddSubtype` (differ only by a
  creature type-check), `ChangeColorToChosen`/`BecomeChosenManaColor` (differ only in context slot).

### 5. True one-card monoliths (the D's) — *bespoke executors that should be compositions*

| Effect | Card | Should be |
|---|---|---|
| `ExileFromTopRepeatingEffect` | Demonlord Belzenlok | `RepeatWhile(GatherUntilMatch → Move)` + damage tail |
| `EachPlayerDiscardsOrLoseLifeEffect` | Strongarm Tactics | `ForEachPlayer(discard)` + conditional life-loss tail |
| `EachPlayerDrawsForDamageDealtToSourceEffect` | Grothama | per-player count `DynamicAmount` + draw |
| `GrantAttackBlockTaxPerCreatureTypeEffect` | Whipgrass Entangler | generic pay-tax grant (raw `String` cost + baked oracle text — worst offender) |
| `GrantToEnchantedCreatureTypeGroupEffect` | Onslaught crowns | Gather(shares-type) → `ForEachInGroup`(ModifyStats + GrantKeyword + GrantProtection) |
| `GrantCastCreaturesFromGraveyardWithForageEffect` | Osteomancer Adept | graveyard-cast permission + forage cost + enters-with-counter |
| `MoveCounters` / `MoveChosenCountersToTarget` / `MoveCountersEachKindMissing` | Tester / Goldberry | the unified `MoveCounters` (see smell #3) |

**Justified singletons (keep as-is):** `OpenLifeBid`, `SecretBid`, `UnlockDoor`,
`ExchangeLifeAndPower`, `HijackNextTurn` — genuine custom decision-loops / distinct
game-actions. These are correct `add-feature` boundaries, not smells.

**Dead code:** `PutOnLibraryPositionOfChoiceEffect` and `PreventLandPlaysThisTurnEffect` have
**zero card references** — confirm live or delete.

---

## A sixth, subtler smell: god-atom creep

The biggest *long-term* risk isn't the monoliths — it's central atoms accreting boolean riders:

- `SelectFromCollection` — `matchChosenCreatureType`, `useTargetingUI`, `showAllCards`, `alwaysPrompt`
- `MoveCollection` — `faceDown`, `linkToSource`, `markEnteredViaSourceAbility`, `addCounterType`, …
- `CreateTokenEffect` — card-type booleans (`artifactToken`/`enchantmentToken`/`legendary`) +
  enter-state (`tapped`/`attacking`) + `exileAtStep`/`sacrificeAtStep`/`colorsFromChoice`
- `GrantMayPlayFromExile` — `withAnyManaType`, `condition`, `landEntersTapped`, `onPlayRider`, …
- `ModalEffect` — `chooseAllIfBlightPaid` (one card's mechanic on the core type)

Each new "...this way" clause becomes a flag instead of a sealed-vocabulary variant. This is the
*gradual* form of the same monolith smell — push these into the sealed types
(`SelectionRestriction`, `CardSource`, `CollectionFilter`, a structured enter-state value) rather
than more flags.

---

## Recommended refactor roadmap (prioritized by payoff)

1. **Collapse the 5 `CreateTokenCopy*` effects** into
   `CreateTokenCopy(source: CopySource, mods: CopyModifications)` — highest fragmentation,
   clearest win, removes ~20 duplicated fields.
2. **Unify the counter-move/distribute family** into one `MoveCounters` over
   (source × destination × kind-selector × amount) — folds ~6 single-card types into one.
3. **`ControlRecipient` parameter on `GainControl`** — kills 3 effect types; preserves
   `PlayerRankMetric`.
4. **Single-keyword grants → `GrantKeyword` facades** (`GrantToxic`, `GrantShroud`,
   `GrantHexproof`) — removes Effect *and* executor with zero card-author churn.
5. **Adopt the project's own `ForEachInGroup`+`Self` rule** for all `*Group` filter-variants.
6. **Finish the stalled merges** — spell-copy family, retarget quartet, the named pairs in
   smell #4.
7. **Decompose the ~7 D-grade monoliths** into pipelines; delete the 2 dead effects.
8. **Push central-atom boolean riders into sealed vocabularies** to halt god-atom creep.
9. **Add a hygiene test** (in the spirit of `FacadeBoundaryTest` / `TapEventEnforcementTest`)
   that flags a new effect type used by ≤1 card, forcing the "can this compose?" question at
   authoring time.

None of this is firefighting — the engine is healthy. It's paying down a known, well-bounded
duplication debt so the effect count drifts *down* even as the card corpus grows, which is the
real test of the compose-don't-add thesis.

---

## Per-cluster detail

The grading below is condensed; each cluster's full per-effect table was produced during the
audit and can be regenerated. Smell-level findings are captured above; this section records the
cluster-specific notes worth keeping.

### Library / Draw / Reveal / Pipeline (A−)

Strongest evidence the vision is real: the Gather (`GatherCardsEffect` + `CardSource`) → Filter
(`FilterCollectionEffect` + `CollectionFilter`) → Select (`SelectFromCollectionEffect`) → Move
(`MoveCollectionEffect`) spine carries hundreds of cards each, and `IfYouDo`/`MayPayMana`/`MayPayX`
lower to a single `GatedEffect`. Edge problems: 3 D-monoliths (`ExileFromTopRepeating`,
`EachPlayerDiscardsOrLoseLife`, `EachPlayerDrawsForDamageDealtToSource`); three `Emit*Event` twins
(scried/surveiled/manifest-dread) → `EmitLibraryActionEvent(kind)`; legacy `ChooseCreatureTypeEffect`
vs `ChooseOptionEffect`; god-atom creep on Select/Move.

### Damage / Combat / Life (B+)

`PreventDamageEffect` (`scope`/`direction`/`sourceFilter`/`onPrevented`/`nextInstanceOnly`) and
`SetSuspectedEffect` (status-flag atom, composed by `Effects.Suspect`) are templates for the module.
Smells: collapse `CantAttack`/`CantAttackGroup`/`CantBlock`/`CantBlockGroup` to two subject-parameterized
effects; fold `RedirectCombatDamageToController` + `ReflectCombatDamage` into `RedirectNextDamage` /
the `onPrevented` reflection idiom; `DividedDamageEffect` carries an awkward dual `totalDamage:Int` +
`dynamicTotal:DynamicAmount?` (collapse to one `DynamicAmount`); `OwnerGainsLife` ≡
`GainLife(target = OwnerOfTarget)`; `PayLife`/`PayDynamicLife` should be one type.

### Counters / Stats (B−)

Healthy core (`AddCounters` 346, `AddDynamicCounters` 36, `Proliferate`, exemplary
`GrantCounterPlacementModifier`). The counter-move/distribute cluster (smell #3) is the headline
refactor; `DoubleCounters` should evaporate into `AddDynamicCounters(amount = CountersOnTarget(type))`
once that `DynamicAmount` exists; `SetBasePower` + `SetBasePowerToughness` → `SetBaseStats`.

### Token / Copy / Stack (B−)

Best: `CounterEffect` (absorbed 7 types), `ChainCopyEffect` ("unified chain copy for all Chain of X"),
`CreatePredefinedTokenEffect` (data-driven, 134+ cards), `CopyCardIntoCollectionEffect`. Worst: the 5
`CreateTokenCopy*` (smell #3), the stalled spell-copy merge (smell #4), the retarget quartet, and
`WardCost` being a parallel cost vocabulary that should re-express over the shared `CostAtom`.

### Control / Removal / Player / Protection (B)

Excellent base atoms (`GainControl`, `Regenerate` 126, `Sacrifice`/`SacrificeSelf` 324, `ForceSacrifice`,
`MoveToZone`, `CreateGlobalTriggeredAbility`, `ChooseColorThen`, `TheRingTemptsYou` 45, split
`AddCombatPhase`/`AddMainPhase`). Dominant smell: recipient/quality fragmentation (smell #1) plus a
fragmented `Skip*` step family (`SkipCombatPhases`/`SkipUntap`/`SkipNextDrawStep`/`SkipNextTurn` →
`SkipStep(kind, count)`) and player-restriction trio (`CantCastSpells`/`CantPlayCardsFromHand`/`CantActivateLoyaltyAbilities`
→ `RestrictPlayer(action)`). Only true D: `GrantCastCreaturesFromGraveyardWithForage`.

### Keyword / Type / Transform / Permanent / Tap (B)

Anchored by `GrantKeyword` (704), `TapUntap` (`tap:Boolean`, 52), `BecomeCreature` (39), the
`Grant{Triggered,Activated,Static}Ability` payload trio. Smells: single-keyword grants duplicating
`GrantKeyword` (smell #1); `*Group` filter-variants (smell #2); near-duplicate pairs (smell #4);
`AnimateLand` ⊂ `BecomeCreature`, `BecomeArtifact` ∥ `BecomeCreature` (extract a `BecomePermanent`
base or make CREATURE optional); lone D = `GrantToEnchantedCreatureTypeGroup`.

### Control-flow combinators (A)

`ForEachEffect/IterationSpace` and `GatedEffect/Gate` are exemplary; base `Effect` interface
(`description`/`runtimeDescription`/`TextReplaceable`/auto-flattening `then`) is minimal and right;
`FaceDownMode` and `SuccessCriterion` are clean extension points. Holdouts: fold
`RepeatDynamicTimesEffect` into `IterationSpace.Times` or `RepeatCondition.NTimes` (two repeat
executors today); lower `PayOrSufferEffect` (41 uses) onto `GatedEffect(Gate.MayPay, otherwise=suffer)`;
converge the three "choose-one-of-N-effects" surfaces (`ModalEffect.chooseOne`, `ChooseActionEffect`,
`BudgetModalEffect`) onto one modal executor.

### Mana + mechanic-specific (A− / mixed)

`AddMana` (268) and `AddManaOfChoice` (replaced 5 legacy effects) are textbook; the three orthogonal
axes `ManaExpiry`/`ManaRestriction`/`ManaSpellRider` are a clean design. Owed: fold `AddColorlessMana`
(nullable `color`), `AddDynamicMana` (`distribution` flag), `AddOneManaOfEachColorAmong` ("one-of-each"
mode), and `AddAnyColorManaSpendOnChosenType` (deferred restriction) into the two add-atoms; replace the
five fixed spell-predicate `ManaRestriction` objects (`InstantOrSorceryOnly`, `CreatureSpellsOnly`,
`LegendarySpellsOnly`, `SpellsMV4OrGreater`, `CreatureMV4OrXCost`) with one `SpellMatching(filter)`.
`GrantSuspendEffect` is the gold standard; `LevelUpClass` is a thin mechanic marker; the bid/guess
mini-games (`OpenLifeBid`, `SecretBid`) are justified `add-feature` boundaries.

---

# Appendix — every effect, individually rated

All effect types, one row each, grouped by source file. Ratings: **A** exemplary · **B** solid ·
**C** smell (overlap/over-specific/should be a parameter) · **D** one-card monolith. Reuse counts
(where shown) are `mtg-sets` references at audit time and are directional. Items tagged
*(facade)* are `fun` builders that lower onto an atom (not their own serializable type); items
tagged *(ReplacementEffect)* implement `ReplacementEffect`, not `Effect`.

## LibraryEffects.kt / DrawingEffects.kt / RevealEffects.kt / PipelineEffects.kt / CompositeEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `GatherCardsEffect` | A | Pipeline head (384); `CardSource` sealed vocab is the real extensibility win. |
| `FilterCollectionEffect` (+`CollectionFilter`) | A | Auto-partition atom; `CollectionFilter` cleanly extensible (`MatchesFilter`/`GreatestPower`/`ManaValueAtMost`/`InZone`). |
| `SelectFromCollectionEffect` | A | Central Select atom (287). Watch creep: `matchChosenCreatureType`/`useTargetingUI`/`showAllCards`/`alwaysPrompt` should be a `SelectionRestriction`. |
| `MoveCollectionEffect` | A | The Move atom (370). Heavy rider surface (`faceDown`/`linkToSource`/`markEnteredViaSourceAbility`/`addCounterType`) — push into sealed vocab. |
| `RevealCollectionEffect` | A | Reveal-the-collection with no move side-effects (35). Good separation. |
| `GatherUntilMatchEffect` | A | Walk-until-match-then-store (28); pairs with `RevealCollection`. |
| `ConditionalOnCollectionEffect` | A | Collection-size gate (33); `countDistinctCardTypes`/`filter` reasonable. |
| `ShuffleLibraryEffect` | A | Tiny `target`-parameterized leaf (~91). |
| `ScryEffect` / `SurveilEffect` | A | Compact macro-markers expanding to shared pipelines (109/66); named-canonical justified. |
| `DrawCardsEffect` | A | Canonical draw atom (`DynamicAmount` + target), 725. |
| `RevealHandEffect` | A | Atomic public reveal that composes (37). |
| `SelectTargetEffect` | A | Mid-pipeline targeting reusing full `TargetRequirement` (19). |
| `StoreNumberEffect` | A | Pipeline number-capture guarding re-projection drift. |
| `GrantMayPlayFromExileEffect` | A | Core impulse-draw atom (76); most rider-laden — resist new boolean clauses. |
| `GrantPlayWithoutPayingCostEffect` | A | Small grant composing onto a granted `from` collection. |
| `CastFromCollectionWithoutPayingCostEffect` | A | Well-parameterized (`from`/`payManaCost`/`storeCastTo`), 19; designed to avoid bespoke executors. |
| `ChooseOptionEffect` | A | Generic chooser (`OptionType`: creature-type/color/basic-land/card-name) — the intended unifier. |
| `CompositeEffect` | A | Foundational sequencer; `descriptionOverride`/`descriptionAmounts` escape hatch. |
| `GatherSubtypesEffect` | B | Subtype-extraction atom feeding `HasSubtypeInEachStoredGroup` (4). |
| `CaptureControllersEffect` | B | Niche (2) but clean — captures controller before `ControllerComponent` is lost. |
| `ChoosePileEffect` | B | Binary-pile router (Fact or Fiction), 8. |
| `NoteCreatureTypeEffect` | B | Justified extra behavior (dynamic exclusion + write-back), 2. |
| `LookAtTargetHandEffect` | B | Simple peek atom (19). |
| `LookAtFaceDownEffect` | B | `FaceDownLookScope` folds single/all-controlled cleanly (6). |
| `MayRevealCardFromHandEffect` | B | Reusable "may reveal [filter]; otherwise [effect]" (2). |
| `DrawUpToEffect` | B | Clean atom with `storeNotDrawnAs` for pipeline scaling (3). |
| `ReplaceNextDrawWithEffect` | B | Generic draw-replacement shield wrapping any `Effect` (2, whole "Words of" cycle). |
| `StoreCardNameEffect` | B | Capture a card's name; counterpart to `ChooseOption(CARD_NAME)` (2). |
| `MakePlottedEffect` | B | Plot-grant over an exiled collection (4). |
| `GrantPlayWithAdditionalCostEffect` | B | Small grant atom composing onto `from`. |
| `GrantPlayWithCostIncreaseEffect` | B | Small grant atom composing onto `from`. |
| `ExileLibraryUntilManaValueEffect` | C | `storeAs` saves it from D, but the until-MV loop is a one-off; prefer a `GatherUntilManaValue` source. |
| `CascadeEffect` | C | `data object` keyword-macro whose own kdoc describes a flow the pipeline atoms already cover. |
| `EachPlayerReturnsPermanentToHandEffect` | C | `data object`, single card; should be `ForEachPlayer(Select → Move(hand))`. |
| `ForEachCapturedControllerEffect` | C | Reusable shape but heavy tri-collection signature (2). |
| `ChooseCreatureTypeEffect` | C | Superseded by `ChooseOption(CREATURE_TYPE)` yet ~15 uses linger — migrate & delete. |
| `EachPlayerChoosesCreatureTypeEffect` | C | Should be `ForEachPlayer(ChooseOption(CREATURE_TYPE) accumulate)` (3). |
| `GrantFreeCastTargetFromExileEffect` | C | Single-target twin of the collection-grant pair; express via `SelectTarget` + grants. |
| `EmitScriedEventEffect` / `EmitSurveiledEventEffect` / `EmitManifestedDreadEventEffect` | C | Three internal tail-markers differing only in event + default collection → `EmitLibraryActionEvent(kind)`. |
| `ExileFromTopRepeatingEffect` | D | Belzenlok monolith (`matchFilter`+`repeatIfManaValueAtLeast`+`damagePerCard`) → `RepeatWhile(GatherUntilMatch → Move)` + damage tail. |
| `EachPlayerDiscardsOrLoseLifeEffect` | D | Strongarm Tactics → `ForEachPlayer(discard)` + conditional life-loss tail. |
| `EachPlayerDrawsForDamageDealtToSourceEffect` | D | Grothama `data object`; hyper-specific per-player damage-map read. |

## DamageEffects.kt / CombatEffects.kt / LifeEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `DealDamageEffect` | A | Workhorse (~930); `DynamicAmount`+`damageSource`+`cantBePrevented`+`excessToController`. |
| `GainLifeEffect` / `LoseLifeEffect` | A | Mirror twins (~440/~260); pure `DynamicAmount`+target. |
| `FightEffect` | A | Canonical two-target fight primitive (~43). |
| `PreventDamageEffect` (+`PreventionScope`/`Direction`/`SourceFilter`) | A | Best-in-class: one parametrized type replaces all prior prevention effects. |
| `ProvokeEffect` | A | Canonical untap-and-force-block (~20). |
| `MustBeBlockedEffect` | A | `allCreatures` flag spans Lure vs Gaea's Protector cleanly (~24). |
| `GoadEffect` | A | Proper CR 701.15 atom; goader-of-record via context. |
| `RemoveFromCombatEffect` | A | `unblockSoleBlockedAttackers` flag handles 509.1h vs modern default (~11). |
| `CanAttackDespiteDefenderThisTurnEffect` | A | Clean activated counterpart to the static (~13). |
| `SetSuspectedEffect` | A | Status-flag-only atom; `Effects.Suspect` composes it with `GrantKeyword(MENACE)`+`CantBlock`. |
| `RedirectNextDamageEffect` (+`RedirectScope`) | A− | `NEXT_INSTANCE`/`NEXT_BATCH`/`CONTINUOUS` scope is clean (~8). |
| `SetLifeTotalEffect` | B | `DynamicAmount`+target, 118.5. Solid. |
| `PayLifeEffect` | B | Fixed-`Int` cost atom (~50); fold into `PayDynamicLife(Fixed)`. |
| `PayDynamicLifeEffect` | B | `DynamicAmount`+`payer` twin of `PayLife`; pair should be one type. |
| `DamageCantBePreventedThisTurnEffect` | B | Clean turn-scoped `data object` flag. |
| `ForceBlockEffect` | B | Provoke-without-untap (~3); could be `Provoke(untap=false)`. |
| `TauntEffect` | B | "Creatures attack you next turn if able"; distinct enough from goad. |
| `MarkMustAttackThisTurnEffect` | B | "Attacks this turn if able"; good ForEach-composition citizen. |
| `GrantCantBeBlockedExceptByEffect` | B | Filter-based evasion grant via shared enforcement channel — the general form. |
| `GrantCantBeBlockedByChosenColorEffect` | B | Inverse-polarity chosen-color variant; reasonable as distinct shape. |
| `GrantDamageBonusEffect` | B | Properly parameterized (`bonusAmount`/`sourceFilter`/duration), Flame of Keld. |
| `OwnerGainsLifeEffect` | C | ≡ `GainLife(target = OwnerOfTarget)`; kill in favor of an `EffectTarget`. |
| `ExchangeLifeAndPowerEffect` | C | Evra one-off "exchange A with B"; a generic `ExchangeEffect` would absorb the next such card. |
| `DividedDamageEffect` | C | Awkward dual `totalDamage:Int` + `dynamicTotal:DynamicAmount?` → one `DynamicAmount`. |
| `DealDamagePerEntityInZoneEffect` | C | Novelty is the collection read → `DynamicAmount.EntitiesStillInZone`, not a damage effect (2). |
| `AmplifyNoncombatDamageThisTurnEffect` | C | Taii Wakeen one-off; generalize to a parameterized damage-amount replacement this turn. |
| `RedirectCombatDamageToControllerEffect` | C | ≡ `RedirectNextDamage(redirectTo=Controller)` + combat-only flag. |
| `ReflectCombatDamageEffect` | C | Harsh Justice one-off; expressible via `PreventDamage.onPrevented` reflect idiom. |
| `CantAttackEffect` / `CantBlockEffect` | C | Single-target halves of the `*Group` versions; unify subject = {target \| filter}. |
| `CantAttackGroupEffect` / `CantBlockGroupEffect` | C | Filter versions; four near-identical types should be two parameterized ones. |
| `GrantCantBeBlockedExceptByColorEffect` | C | A color is a filter — fold into `GrantCantBeBlockedExceptBy`. |
| `GrantKeywordToAttackersBlockedByEffect` | C | Ride Down; the "blocked-by this combat" selector should be a group + generic `GrantKeyword`. |
| `GrantAttackBlockTaxPerCreatureTypeEffect` | D | Whipgrass Entangler; raw `String` cost + baked oracle text → generic pay-tax grant. |

## CounterEffects.kt / StatsEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `AddCountersEffect` | A | Workhorse (346); fixed `count:Int`. |
| `AddDynamicCountersEffect` | A | Clean dynamic sibling (36); right 2-atom split with `AddCounters`. |
| `ProliferateEffect` | A | Named mechanic, parameterless `data object`. Correct shape. |
| `GrantCounterPlacementModifierEffect` | A | "General by construction" — parameterized over modifier/duration/counterType/recipient. Model citizen. |
| `ModifyStatsEffect` | A | 911 uses; `DynamicAmount` P/T with `Fixed` convenience ctor. The exemplar. |
| `RemoveCountersEffect` | B | Fixed kind+count removal (20); the "negative AddCounters." |
| `MoveAllLastKnownCountersEffect` | B | Distinct: reads LKI counter map of a dead source (8); still the "all-kinds, source=LKI" mode. |
| `AddCountersToCollectionEffect` | B | Collection-target sibling (14); `amount` override duplicates the fixed-vs-dynamic split a 3rd time. |
| `GrantDynamicStatsEffect` | B | Layer-7c bonus, filter/power/toughness (19); description hardcodes "+X/+X" (minor bug). |
| `DoubleCountersEffect` | C | Evaporates into `AddDynamicCounters(amount = CountersOnTarget(type))` once that `DynamicAmount` exists. |
| `RemoveAllCountersEffect` / `RemoveAnyNumberOfCountersEffect` | C | "Move counters to nowhere" — fold into the unified move primitive with `destination = none`. |
| `DistributeCountersFromSelfEffect` | C | `DistributeDecision` from self → overlaps the next row; unify. |
| `DistributeCountersAmongTargetsEffect` | C | `DistributeDecision` among targets (6); unify both into `DistributeCounters(source, recipients, total, minPerTarget)`. |
| `ConvertCountersToTokensEffect` | C | Tetravus forward-half is a bespoke remove-N→mint-N loop a `ForEach` could express. |
| `SetBasePowerEffect` / `SetBasePowerToughnessEffect` | C | Power-only vs both, with `DynamicAmount`-vs-`Int` asymmetry → one `SetBaseStats(power:DynamicAmount?, toughness:DynamicAmount?)`. |
| `MoveCountersEffect` | D | 1 use; its kdoc cross-references the next two as siblings → unify all into one `MoveCounters`. |
| `MoveChosenCountersToTargetEffect` | D | 1 use; `drawCardOnMove` rider belongs in an `IfYouDo`/`GatedEffect`, not baked in. |
| `MoveCountersEachKindMissingEffect` | D | 1 use; a kind-selector mode of the move primitive, not a type. |

## TokenEffects.kt / CopyEffects.kt / ChainCopyEffects.kt / StackEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `CounterEffect` (+`CounterTarget`/`Source`/`Destination`/`Condition`) | A | Gold-standard unification — kdoc lists the 7 effects it replaced (28). |
| `ChainCopyEffect` | A | Self-described unification for all "Chain of X"; generic `action`/`copyRecipient`/`copyCost`. |
| `CreatePredefinedTokenEffect` | A | Data-driven (134+); token bodies are `CardDefinition`s. One nit: `count` + `dynamicCount` redundant pair. |
| `CopyCardIntoCollectionEffect` | A | Exemplary pipeline primitive (11); composes with `CastFromCollectionWithoutPayingCost`. |
| `StormCopyEffect` | A− | Synthesized, not card-facing; reused for Storm/Conspire/Replicate. Good reuse. |
| `EachPermanentBecomesCopyOfTargetEffect` | B+ | Solid in-place become-a-copy (16); `affected` escape-hatch overloads two modes. |
| `CreateTokenEffect` | B | Universal token atom (~165) accreting into a god-object: card-type booleans → `Set<CardType>`; `exile/sacrifice/colorsFromChoice` riders → structured sub-values. |
| `CreateRoleTokenEffect` | B | Thin/justified (Role replacement is real behavior); watch for generalization later. |
| `CounterAllOnStackEffect` | B | Mass-counter atom (`spells`/`abilities`/`opponentsOnly`/`storeCountAs`); fine. |
| `ExileTargetSpellEffect` | B | Explicitly not-a-counter (bypasses can't-be-countered); small/clear. |
| `DestroySourceOfTargetedAbilityEffect` | B | Niche (Teferi's Response) but built as a composition step before `CounterEffect`. |
| `MakeNextSpellUncounterableEffect` | B | One-shot pending rider mirroring `CopyNextSpellCast` shape. |
| `CopyTargetSpellEffect` | B | Heavily reused (51) but accreting token-resolution riders that duplicate the token-copy cluster — factor out shared rider bag. |
| `WardCounterEffect` (+`WardCost`) | B/C− | Effect fine; `WardCost` is a parallel cost vocab — re-express over shared `CostAtom`. |
| `CreateTokenCopyOfSourceEffect` / `…OfTargetEffect` / `…OfChosenPermanentEffect` / `…OfEquippedCreatureEffect` / `CreateRandomCreatureTokenWithManaValueEffect` | C | **Headline smell:** five effects differing only in copy-source → `CreateTokenCopy(source: CopySource, mods: CopyModifications)`. `…OfTarget` has ~20 fields; siblings re-declare uneven subsets. |
| `BecomeCopyOfLinkedExileEffect` | C | Near-dup of `EachPermanentBecomesCopyOfTarget`; fold via a linked-exile `CopySource` + while-attached `Duration`. |
| `ChangeSpellTargetEffect` / `ChangeTargetEffect` / `ReselectTargetRandomlyEffect` / `ChangeTriggeringObjectTargetsEffect` | C | Four overlapping retargets → one `RetargetEffect(scope, chooser, constraint)`; kdocs admit the relationship. |
| `CopyEachTargetSpellEffect` / `CopyTargetTriggeredAbilityEffect` / `CopyTargetSpellOrAbilityEffect` | C | `…SpellOrAbility` kdoc says it generalizes the others → converge on `CopyOnStackEffect(scope, riders)`. |
| `CopyNextSpellCastEffect` / `CopyEachSpellCastEffect` | C | Identical shape, differ only consume-once vs persist → `consumption ∈ {Once, EachThisTurn}`. |
| `MarkSpellExileWithCountersEffect` / `MarkSpellPlotOnResolveEffect` | C | Sibling re-route-on-resolve effects → `MarkSpellExileOnResolveEffect(withCounters?, plotted)`. |
| `ReturnSpellToOwnersHandEffect` / `ReturnSpellOrPermanentToOwnersHandEffect` | C | The `…OrPermanent` already handles the spell case; keep only the dual, make spell-only a facade. |
| `GrantKeywordToSpellEffect` | C | `keyword:String` + spell `target`; overlaps `CopyTargetSpell.keywordsForCopy` and `GrantSpellKeyword` — converge the "grant keyword to a stack object" paths. |

## ControlEffects.kt / RemovalEffects.kt / PlayerEffects.kt / ProtectionEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `GainControlEffect` | A | Base control atom (38), duration-parametric. Should become the only control effect. |
| `RegenerateEffect` | A | Canonical destruction-replacement shield (126). |
| `CantBeRegeneratedEffect` | A | Clean composable marker (22). |
| `SacrificeEffect` | A | Well-parameterized (`filter`/`count`/`any`/`excludeSource`), 104. |
| `SacrificeSelfEffect` | A | Minimal `data object` (324). |
| `ForceSacrificeEffect` | A | Edicts (23); `dynamicCount` fallback keeps snapshots stable. |
| `ExileUntilLeavesEffect` | A | Solid O-Ring atom (25). |
| `MoveToZoneEffect` | A | The one parameterized move atom (banned from cards by `FacadeBoundaryTest`); many flags but done right. |
| `CreateGlobalTriggeredAbilityEffect` | A | The single duration-parametric form that folded prior variants (9). |
| `AddCombatPhaseEffect` / `AddMainPhaseEffect` | A | Split from the old bundled monolith so cards compose exactly what they print. |
| `TakeExtraTurnEffect` | A | `loseAtEndStep`/target (7). |
| `GiftGivenEffect` | A | Pure event emitter `data object` (22). |
| `TheRingTemptsYouEffect` | A | Mechanic atom, heavily reused (45). |
| `ChooseNumberThenEffect` | A | Combinator (choose number → run inner). |
| `ChooseColorThenEffect` | A | Exemplary combinator (19); kdoc tells authors to compose it not invent monoliths. |
| `GrantPlayerProtectionEffect` | A | `ProtectionScope` (Everything/Color/EachOpponent) — right parameterization. |
| `SacrificeTargetEffect` | B | `sacrificedByItsController` flag well-documented; distinct (36). |
| `MarkExileOnDeathEffect` | B | Composable "if it would die, exile instead" marker (9). |
| `ExileAndGrantOwnerPlayPermissionEffect` | B | Cleanly parameterized (`opponentCostIncrease`), Soul Partition family. |
| `ReturnSelfToBattlefieldAttachedEffect` | B | Dragon aura cycle (5). |
| `ReturnOneFromLinkedExileEffect` | B | `data object`; documented reusable for any linked-exile gradual return. |
| `ExileOpponentsGraveyardsEffect` | B | Acceptable (2); `ForEachPlayer(opponents)` would compose it away. |
| `WarpExileEffect` | B | Warp mechanic-support atom. |
| `PlayAdditionalLandsEffect` | B | Clean `count` (2). |
| `AddAdditionalUpkeepStepsEffect` / `AddAdditionalEndStepsEffect` | B | Mild merge opportunity with the phase data-objects into `AddPhaseOrStep(kind, amount)`. |
| `LoseGameEffect` / `WinGameEffect` | B | Reasonable pair (Win = all opponents lose), 3/3. |
| `HijackNextTurnEffect` | B | Mindslaver; controls a *player* — legitimately distinct. |
| `GrantDamageBonusEffect` | B | (also in damage cluster) properly parameterized reusable shape. |
| `GrantSpellKeywordEffect` | B | Emblem grant (`keyword`/`spellFilter`); parameterized. |
| `CreatePermanentEmblemEffect` | B | Generic emblem factory (Oko-style), reusable shape. |
| `GainCitysBlessingEffect` / `RemoveMaximumHandSizeEffect` / `LockLifeGainEffect` | B | Player-property one-shots, each the survives-the-source counterpart to a static. |
| `ChooseNumberForSourceEffect` | B | Durable-on-source counterpart to `ChooseNumberThen`. |
| `GrantProtectionFromChosenCardTypeEffect` | B | Self-contained (fixed card-type option set). |
| `ExchangeControlEffect` | B | Genuinely bidirectional swap; keep, but consume shared `ControlRecipient` machinery. |
| `GainControlByActivePlayerEffect` | C | Differs only by recipient → `GainControl(recipient = ActivePlayer)`. |
| `GainControlByMostEffect` | C | → `GainControl(recipient = PlayerWithMost(metric))`; keep the good `PlayerRankMetric`. |
| `GiveControlToTargetPlayerEffect` | C | ≡ `GainControl(recipient = TargetPlayer)`; clearest collapse. |
| `RemoveDamageShieldEffect` | C | Near-dup of Regenerate → `Regenerate(tap=false, removeFromCombat=false)`. |
| `MarkExileControllerGraveyardOnDeathEffect` | C | Bespoke death-rider → parameter of a general "when target dies, do E" marker. |
| `DestroyAllEquipmentOnTargetEffect` | C | → `Destroy(GroupFilter.equipmentAttachedTo(target))` (a filter, not an effect). |
| `ReturnCreaturesPutInGraveyardThisTurnEffect` | C | Reads as a `Patterns.Graveyard` composition, not a named effect. |
| `ForceExileMultiZoneEffect` | C | Lich's Mastery multi-zone monolith; flag as one-off. |
| `SkipCombatPhasesEffect` / `SkipUntapEffect` / `SkipNextDrawStepEffect` / `SkipNextTurnEffect` | C | Fragmented "skip X" family → one `SkipStep(kind, target, count)`. |
| `CantCastSpellsEffect` / `CantPlayCardsFromHandEffect` / `CantActivateLoyaltyAbilitiesEffect` | C | Identical `(target, duration)` shape → `RestrictPlayer(action, target, duration)`. |
| `GrantShroudEffect` / `GrantHexproofEffect` | C | Identical but for keyword → `GrantEvasionKeyword(keyword, target, duration)`. |
| `GrantFlashToSpellsEffect` / `GrantSpellsCantBeCounteredEffect` | C | Both player-scoped spell-property grants → `GrantSpellProperty(property, spellFilter, …)`. |
| `GrantHexproofFromChosenColorEffect` / `GrantProtectionFromChosenColorEffect` | C | Differ only by keyword under `ChooseColorThen` → `GrantFromChosenColor(quality)`. |
| `ExileWithAurasNotingCountersEffect` / `ReturnNotedExileTappedWithAurasEffect` | C | Bespoke Tawnos's Coffin pair; revisit if a 2nd "note state, restore" card appears. |
| `PutOnLibraryPositionOfChoiceEffect` | C | Well-shaped (`positions`) but **zero card refs** — confirm live or delete. |
| `PreventLandPlaysThisTurnEffect` | C | Fine shape but **zero card refs** — confirm live or delete. |
| `GrantCastCreaturesFromGraveyardWithForageEffect` | D | Osteomancer; hard-codes forage-cast + finality counter → compose grant + cost + enters-with-counter. |

## KeywordAndAbilityEffects.kt / TypeAndColorEffects.kt / TransformEffects.kt / PermanentEffects.kt / TapEffects.kt

| Effect | Rating | Verdict |
|---|---|---|
| `GrantKeywordEffect` | A | The exemplar — one `keyword:String`, 704; `GrantProtectionFromColor` already composes it. |
| `RemoveKeywordEffect` | A | Clean mirror, same shape (9). |
| `GrantTriggeredAbilityEffect` / `GrantActivatedAbilityEffect` / `GrantStaticAbilityEffect` | A | Correct generalization — parameterized by an `ability` payload (36/57/2). |
| `SetCreatureSubtypesEffect` | A | Parameterized (`subtypes` + `fromChosenValueKey`), 6. |
| `TransformEffect` | A | Canonical in-place flip (701.27), 5. |
| `BecomeCreatureEffect` | A | The big parameterized animate atom (39) — others should express against it. |
| `AttachEquipmentEffect` | A | Core equip atom (42). |
| `ExploreEffect` | A | Named mechanic, single target (9). |
| `TapUntapEffect` | A | Exemplar — `tap:Boolean` collapses tap+untap (52). |
| `RemoveAllAbilitiesEffect` | B | Legit distinct (strip-all ≠ remove-one), layer-6 (11). |
| `LoseAllCreatureTypesEffect` | B | Small legit type-layer atom (2). |
| `BecomeCreatureTypeEffect` | B | Player-choice type; distinct from fixed `SetCreatureSubtypes` (3). |
| `SetLandTypeEffect` | B | Lands analogue of `SetCreatureSubtypes` (3); long-term merge into a categorized "set subtypes". |
| `AddCardTypeEffect` | B | Parameterized `cardType:String`, additive (9). |
| `ChangeColorEffect` / `AddColorEffect` | B | Good Set-vs-Add split, both `Set<String>` (7/2). |
| `ChooseColorForTargetEffect` | B | Stores a color choice for later static reads (2). |
| `ExileAndReturnTransformedEffect` | B | `ReturnFace` enum covers both directions — enum-parameterization done right (4). |
| `ReturnSelfFromExileTransformedEffect` | B | Craft-only `data object`; specificity justified in kdoc (1). |
| `MassAnimateEffect` | B | Filter-set companion to `BecomeCreature`; justified (captured set, per-entity P/T) (2). |
| `BecomeSaddledEffect` | B | Saddle marker atom (wired via keyword builder). |
| `BecomePreparedEffect` / `UnprepareEffect` | B | Prepare mechanic + inverse (20/2). |
| `TurnFaceDownEffect` / `TurnFaceUpEffect` | B | Morph/manifest atoms (3 each). |
| `RevealFaceDownPermanentEffect` | B | Purely-informational reveal (708), composes with a turn-up gate (2). |
| `AttachTargetEquipmentToCreatureEffect` | B | Genuinely two explicit targets; not a dup of `AttachEquipment` (5). |
| `PutOntoBattlefieldAttachedToChosenEffect` | B | Aura+Equipment with `hostFilter`; justified (2). |
| `GrantExileOnLeaveEffect` | B | Reanimation marker atom (2). |
| `IncrementAbilityResolutionCountEffect` | B | `data object` tracker (4). |
| `TapUntapCollectionEffect` | B | Pipeline collection variant, `tap:Boolean` (7). |
| `PhaseOutEffect` | B | Phasing atom (702.26), 4. |
| `PhaseOutUntilLeavesEffect` / `PhaseInLinkedToSourceEffect` | B | Source-linked phasing pair (Oubliette), analogue of ExileUntilLeaves (2 each). |
| `GrantToxicEffect` | C | kdoc admits it emits `TOXIC_<n>` → make it a `GrantKeyword` facade, delete the type. |
| `GrantHarmonizeEffect` / `GrantFlashbackEffect` | C | Identical graveyard-cast-grant shape → `GrantGraveyardCastKeyword(keyword, cost)` (covers Retrace/Jump-start/Disturb). |
| `GrantActivatedAbilityToGroupEffect` | C | Duplicates the single-target payload with `filter` → `ForEachInGroup`+`Self`. |
| `SetGroupCreatureSubtypesEffect` | C | Filter-variant of `SetCreatureSubtypes` → `ForEachInGroup`+`Self` (1). |
| `ChangeGroupColorEffect` | C | Filter-variant of `ChangeColor` → `ForEachInGroup`+`ChangeColor(Self)` (1). |
| `AddCreatureTypeEffect` / `AddSubtypeEffect` | C | Differ only by a creature type-check → one `AddSubtype(subtype, requireCreature)`. |
| `ChangeColorToChosenEffect` / `BecomeChosenManaColorEffect` | C | Both recolor-to-context-color, differ only in context slot → `ChangeColorToContextColor(source)`. |
| `ChangeCreatureTypeTextEffect` / `ChangeWordInTextEffect` | C | Text-replacement pair; `ChangeWordInText` already subsumes via `Duration.Permanent` → `ChangeTextWords(category, duration)`. |
| `AnimateLandEffect` | C | Strict subset of `BecomeCreature` (its kdoc says so) → demote to a facade. |
| `BecomeArtifactEffect` | C | Parallel monolith of `BecomeCreature` → fold in (make CREATURE optional) or extract `BecomePermanent` base (1). |
| `GrantToEnchantedCreatureTypeGroupEffect` | D | Onslaught Crown monolith bundling stats+keyword+protection+type-share → Gather→ForEachInGroup pipeline (1). |

## ForEachEffects.kt / GatedEffects.kt / ConditionalEffect.kt / Effect.kt (combinators) + support

| Effect | Rating | Verdict |
|---|---|---|
| `Effect` (sealed) + `then` | A | Minimal base: `description`/`runtimeDescription`/`TextReplaceable`/auto-flattening `then`. |
| `ForEachEffect` (+`IterationSpace`) | A | Flagship: one executor, pause-safe continuation, five lowering facades. |
| `GatedEffect` (+`Gate`) | A | Flagship: whole optional/gated cluster in one frame; CR order correct by construction. |
| `ConditionalEffect` *(facade)* | A | Most-used combinator (234) is now a 4-line facade onto `Gate.WhenCondition`. |
| `MayEffect` / `OptionalCostEffect` / `IfYouDoEffect` / `MayPayManaEffect` / `MayPayXForEffect` *(facades)* | A | All lower to `GatedEffect` (171/13/28/39/3); only naming sugar survives. |
| `FlipCoinsEffect` | A | Composable coin atom — tallies heads, scales downstream via `VariableReference`. |
| `SuccessCriterion` (Auto/CollectionNonEmpty/Always/DamageDealt) | A | `Auto.canInfer` single source of truth, validated at card-load. |
| `FaceDownMode` | A | Textbook extension point (adding Disguise = one variant, not a new effect). |
| `ModalEffect` (+`Mode`) | B | Heavily reused (88) but accreting flags; `chooseAllIfBlightPaid` is one card's mechanic — push into `dynamicChooseCount`. |
| `RepeatWhileEffect` (+`RepeatCondition`) | B | Clean do-while; see next row for the missed unification. |
| `ReflexiveTriggerEffect` | B | Legit-distinct from `Gate.DoAction` (new stack trigger w/ own targets), 35; `optional` re-implements `Gate.MayDecide`. |
| `CreateDelayedTriggerEffect` (+timing/expiry) | B | Genuine "schedule for later"; flag-heavy but axes orthogonal; `DelayedTriggerExpiry` single-variant (speculative). |
| `AnyPlayerMayPayEffect` | B | Correctly not folded (APNAP loop ≠ one `decisionMaker`); standalone primitive (1). |
| `ZonePlacement` | B | Clean enum, but `TappedAndAttacking` composite alongside primitives — split if a 3rd combo appears. |
| `BudgetModalEffect` (+`BudgetMode`) | C | Parallel modal duplicating "choose modes, run effects" → fold a `budget` into `ModalEffect`. |
| `ChooseActionEffect` (+`EffectChoice`/`FeasibilityCheck`) | C | Overlaps `ModalEffect.chooseOne`; converge the choose-one-of-N surfaces. |
| `RepeatDynamicTimesEffect` | C | "Repeat over a count" → `IterationSpace.Times` or `RepeatCondition.NTimes`; don't keep two repeat executors. |
| `PayOrSufferEffect` | C | ≡ `GatedEffect(Gate.MayPay, otherwise = suffer)` (41) — lower to a facade per the planned migration. |
| `FlipCoinEffect` / `FlipTwoCoinsEffect` | C | Branching coin variants → `FlipCoins` + a gate; `FlipTwoCoins` even drops `mixedEffect` in its description. |
| `BeholdEffect` | C | Mechanic bundle re-creating `Gate.MayDecide` + feasibility + reveal; compose those (0 direct uses). |

## ManaEffects.kt + axes / SuspendEffects / AmassEffect / ClassEffects / RoomEffects / AuctionEffects / SecretBidEffects / GuessEffects

| Effect | Rating | Verdict |
|---|---|---|
| `AddManaEffect` | A | Core mana atom (`color`+`DynamicAmount`+`restriction`+`expiry`), 268. |
| `AddManaOfChoiceEffect` | A | Model consolidation — replaced 5 legacy effects via `ManaColorSet`+riders (44). |
| `ManaExpiry` (enum) | A | Orthogonal duration axis (`END_OF_TURN`/`END_OF_COMBAT`). |
| `ManaRestriction` (sealed) | A | Excellent hierarchy; `AnyOf` composes — but see the 5 fixed-predicate objects below. |
| `ManaSpellRider` (sealed) | A | The what-happens-to-the-spell axis; composable, set-valued. |
| `MayPlayExpiry` (sealed) | A | `UntilControllerStep(step, includeCurrentTurn)` covers the family with shortcuts. |
| `GrantSuspendEffect` | A | Gold standard — pure marker; `MoveToZone`+`AddCounters` do the real work. |
| `AddColorlessManaEffect` | B | Near-clone existing only because `Color` lacks COLORLESS → fold into `AddMana(color: Color? = null)`. |
| `AddDynamicManaEffect` | B | Distinct (distribute X across colors) but `allowedColors` dups `ManaColorSet` → add a `distribution` flag (3). |
| `AddOneManaOfEachColorAmongEffect` | B | Different semantics but → a "one-of-each in resolved set" mode on `AddManaOfChoice` (2). |
| `PayManaCostEffect` | A | Minimal non-optional resolution-time payment atom. |
| `PayDynamicManaCostEffect` | B | Good (`amount`+`payer`+optional `color`); "pay {N} for each X" + cross-player. |
| `AmassEffect` | B | Internally a monolith but a real CR 701.47 keyword reused at scale (27); `amount` is `DynamicAmount`. |
| `UnlockDoorEffect` | B | Distinct game-action (must emit the same door events as the special action); earns a type. |
| `OpponentGuessesTopCardKindEffect` | B | Reusable guess primitive (`onGuessedRight/Wrong` sub-effects + `Chooser`), 2. |
| `AddAnyColorManaSpendOnChosenTypeEffect` | C | ≡ `AddManaOfChoice(AnyColor, restriction = ChosenSubtype)` once restriction is deferrable. |
| `LevelUpClassEffect` | C | Trivial one-mechanic marker (`targetLevel:Int`) → generalize to a level/counter write. |
| `OpenLifeBidEffect` | C | Mages' Contest; heavy bespoke auction loop for one card (partial credit: `onWin` sub-effect). |
| `SecretBidEffect` | C | Menacing Ogre; bespoke secret-number mini-game (outcomes are `Effect?` slots — the good part). |
| `ManaRestriction` fixed objects (`InstantOrSorceryOnly`/`CreatureSpellsOnly`/`LegendarySpellsOnly`/`SpellsMV4OrGreater`/`CreatureMV4OrXCost`) | C | Five "spell matches predicate" one-offs → one `SpellMatching(filter)` over `GameObjectFilter`. |

## ReplacementEffect.kt (related — implement `ReplacementEffect`, not `Effect`)

| Effect | Rating | Verdict |
|---|---|---|
| `OnEnterRunEffect` *(ReplacementEffect)* | A | Exemplary: wraps any `Effect` to run inline at ETB ("compose ETB-time choices out of existing atoms"); kdoc-documented composition pattern. |
| `ReplaceDrawWithEffect` *(ReplacementEffect)* | B | Generic "replace drawing with [effect]" (`optional` flag), Underrealm Lich; reusable. |
| `RedirectZoneChangeWithEffect` *(ReplacementEffect)* | B | "Redirect zone change + run additional effect" (Ugin's Nexus); cleanly extends `RedirectZoneChange` with an `additionalEffect`. |

*Total: 284 `Effect` types across `scripting/effects/` (the 6 facades and 3 replacement effects above are listed for completeness and are not part of that count).*
