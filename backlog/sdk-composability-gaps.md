# SDK Composability Gaps

Remaining gaps preventing new cards from being implemented without SDK changes.
Builds on the completed [effect-pipeline-roadmap.md](effect-pipeline-roadmap.md) which brought
zone-manipulation pipeline coverage to ~85%.

**Goal:** Any new card can be composed from existing atomic parts (effects, triggers, conditions,
costs, amounts, filters) without adding new sealed interface variants to the SDK.

**Current state:** ~82% of cards need no SDK changes. The gaps below block specific mechanic families.

---

## Gap 1: Missing Cost Types

`PayCost` only has 5 variants (Mana, Sacrifice, Discard, ReturnToHand, RevealCard). Several
common cost patterns are missing, forcing workarounds or new executors per card.

### 1a. `PayCost.PayLife(amount: DynamicAmount)`
- **Blocked cards:** Morph face-up costs with life payment, Phyrexian mana, cards like
  Toxic Deluge, Unspeakable Symbol
- **Current workaround:** `PayLifeEffect` exists as an effect but can't be used as a cost
  in `OptionalCostEffect` or morph definitions
- **Effort:** Small — add variant + handler in CostHandler

### 1b. `PayCost.TapPermanents(count: Int, filter: GameObjectFilter)`
- **Blocked cards:** Convoke, cards like Heritage Druid ("tap three Elves"), Opposition
- **Current workaround:** Each card needs a bespoke activated ability definition
- **Effort:** Medium — needs targeting UI for selecting which permanents to tap

### 1c. `PayCost.Exile(filter: GameObjectFilter, zone: Zone, count: Int)`
- **Blocked cards:** Delve, Force of Will ("exile a blue card from hand"), Snapback
- **Current workaround:** None clean — requires custom effect chains
- **Effort:** Small-medium — similar to Sacrifice handler

### 1d. `PayCost.Composite(costs: List<PayCost>)`
- **Blocked cards:** Any card with "pay X and sacrifice Y" combined costs
- **Current workaround:** Separate cost checks, fragile
- **Effort:** Small — iterate and resolve each sub-cost

---

## Gap 2: Missing Trigger Events

`GameEvent` covers zone changes, damage, combat, and casting, but several trigger families
have no event infrastructure.

### 2a. Mana Events (`ManaProducedEvent`, `ManaSpentEvent`)
- **Blocked cards:** Manabarbs, Overabundance, Mirari's Wake mana-triggered abilities
- **Effort:** Medium — requires mana system to emit events during resolution

### 2b. `SpellCastFromZoneEvent` (cast source tracking)
- **Blocked cards:** "Whenever you cast a spell from exile/graveyard" (e.g., Prosper)
- **Current state:** `CastEvent` exists but doesn't track which zone the spell was cast from
- **Effort:** Small — extend `CastEvent` with `fromZone: Zone` field

### 2c. `LifeChangedEvent` (unified life tracking)
- **Blocked cards:** "Whenever you gain/lose life" triggers (e.g., Ajani's Pridemate,
  Vilis, Broker of Blood)
- **Current state:** `LifeGainedEvent` exists but `LifeLostEvent` does not
- **Effort:** Small — add `LifeLostEvent` + emit from `LoseLifeEffect` and damage resolution

---

## Gap 3: Missing Conditions

`Condition` has ~40 variants but lacks some commonly needed predicates.

### 3a. `ManaWasSpentOfColor(color: Color)`
- **Blocked cards:** Hybrid mana "if {R} was spent" effects (e.g., Boros Reckoner),
  Beseech the Queen "if {B}{B}{B} was spent"
- **Depends on:** Gap 2a (mana tracking)
- **Effort:** Medium

### 3b. `HasKeyword(keyword: Keyword)` condition for source entity
- **Blocked cards:** "If this creature has flying, do X" conditional effects
- **Current state:** `HasSubtype` exists but no keyword equivalent
- **Effort:** Small — mirror `HasSubtype` pattern

### 3c. `OpponentControlsMoreOfType(filter: GameObjectFilter)`
- **Blocked cards:** "If an opponent controls more creatures than you" (e.g., Mercy Killing choices)
- **Effort:** Small — variant of existing `ControlsCreatureCount`

---

## Gap 4: Missing DynamicAmount Variants

The AST-based `DynamicAmount` system is excellent but has coverage gaps.

### 4a. `HighestPowerAmong(filter: GameObjectFilter)` / `LowestToughnessAmong`
- **Blocked cards:** Rite of Replication copying highest-power creature,
  Dusk // Dawn (CMC-based filtering by aggregate)
- **Effort:** Small — aggregate over filtered battlefield entities

### 4b. `OpponentLifeTotal` / `OpponentHandSize`
- **Blocked cards:** Triskaidekaphobia ("if opponent's life is 13"),
  Sudden Impact ("damage equal to cards in opponent's hand")
- **Effort:** Small — mirror existing `LifeTotal`/`HandSize` for opponents

### 4c. `NumberOfColorsAmong(filter: GameObjectFilter)`
- **Blocked cards:** Domain mechanic, Converge, "for each color among permanents you control"
- **Effort:** Small — count distinct colors across filtered entities

---

## Gap 5: Damage Prevention & Redirection

No composable primitives exist for damage manipulation. Each prevention/redirection card
currently needs a bespoke replacement effect or executor.

### 5a. `PreventDamageEffect(amount: DynamicAmount, target: EffectTarget, scope: PreventionScope)`
- **Blocked cards:** Holy Day, Fog, Healing Salve, Guardian Angel, damage shields
- `PreventionScope`: `NextInstance`, `AllThisTurn`, `FromSource(filter)`
- **Effort:** High — interacts with damage pipeline internals and replacement effect system

### 5b. `RedirectDamageEffect(from: EffectTarget, to: EffectTarget, amount: DynamicAmount)`
- **Blocked cards:** Deflecting Swat, Shaman en-Kor, Captain's Maneuver
- **Effort:** High — same replacement effect complexity as prevention

### Assessment
These are architecturally complex and may remain as bespoke executors rather than
becoming generic composable atoms. Marking as **investigate** — may be better served
by a small set of parameterized prevention/redirection executors rather than full
composability.

---

## Gap 6: Type Modification (Add vs Replace)

Current type-changing effects only support "set to" (replace all types). No atomic
"add type" or "remove specific type" primitives exist.

### 6a. `AddCreatureTypeEffect(subtype: CreatureType, target: EffectTarget, duration: Duration)`
- **Blocked cards:** Conspiracy ("all creatures are chosen type in addition to other types"),
  Artificial Evolution, Amoeboid Changeling
- **Effort:** Small — add `Modification.AddSubtype` to continuous effect layer

### 6b. `RemoveCreatureTypeEffect(subtype: CreatureType, target: EffectTarget, duration: Duration)`
- **Blocked cards:** Imagecrafter, cards that strip types
- **Effort:** Small — mirror of AddCreatureType

---

## Gap 7: Conditional Continuous Effects

Static abilities and continuous effects can't currently be conditional on game state.
Each conditional variant needs a new static ability type.

### 7a. `ConditionalStaticAbility(condition: Condition, ability: StaticAbilityType)`
- **Blocked cards:** "Has flying as long as you control an Island" (Wonder in graveyard),
  "Gets +2/+2 as long as you control no other creatures" (Konda's Banner conditionals)
- **Current workaround:** Each conditional pattern is a separate `StaticAbilityType` variant
- **Effort:** Medium — requires `StateProjector` to evaluate conditions during layer application

### 7b. `ConditionalKeywordGrant(condition: Condition, keyword: Keyword)`
- **Blocked cards:** "Has hexproof as long as it's untapped" (Silhana Ledgewalker pattern)
- Subset of 7a but common enough to note separately
- **Effort:** Medium — same StateProjector integration

---

## Gap 8: Copy Effects with Modifications

`CopyTargetSpellEffect` copies exactly. No parameterized way to copy-with-changes.

### 8a. `CopyWithModifications(modifications: List<CopyModification>)`
- `CopyModification`: `AddKeyword`, `SetPowerToughness`, `AddEffect`, `ChangeTarget`
- **Blocked cards:** Mirrorwing Dragon (copy + change targets), Heat Shimmer (copy creature
  with haste), Rite of Replication (copy N times)
- **Effort:** Medium-high — requires extending copy infrastructure

---

## Gap 9: Combat Restriction Effects

Limited primitives for attack/block restrictions beyond the existing `CantBlockGroupEffect`.

### 9a. `CantAttackEffect(filter: GameObjectFilter, duration: Duration)`
- **Blocked cards:** Meekstone ("creatures with power 3+ don't untap" adjacent),
  Silent Arbiter, Propaganda-like deterrents
- **Effort:** Small — mirror `CantBlockGroupEffect` for attacks

### 9b. `MustBlockEffect(target: EffectTarget, duration: Duration)`
- **Blocked cards:** Lure, Provoke mechanic, "must be blocked if able"
- **Effort:** Medium — requires combat system changes for block validation

### 9c. `AttackRestriction(maxAttackers: DynamicAmount)`
- **Blocked cards:** Silent Arbiter, Dueling Grounds
- **Effort:** Medium — requires combat system enforcement

---

## Priority Matrix

| Gap | Effort | Card Coverage Impact | Priority |
|-----|--------|---------------------|----------|
| **1a. PayCost.PayLife** | Small | Medium (Phyrexian mana, morphs) | High |
| **1c. PayCost.Exile** | Small-Med | Medium (Delve, Force cycle) | High |
| **2c. LifeLostEvent** | Small | Medium (lifegain/lifedrain triggers) | High |
| **3b. HasKeyword condition** | Small | Medium (conditional combat tricks) | High |
| **6a. AddCreatureType** | Small | Low-Medium (tribal modifiers) | High |
| **4a. Highest/LowestAmong** | Small | Low-Medium (aggregate P/T) | Medium |
| **4b. OpponentLifeTotal** | Small | Low (opponent stat references) | Medium |
| **2b. SpellCastFromZone** | Small | Low-Medium (exile-cast triggers) | Medium |
| **1b. PayCost.TapPermanents** | Medium | Medium (Convoke, tap abilities) | Medium |
| **1d. PayCost.Composite** | Small | Low (multi-cost activated) | Medium |
| **9a. CantAttackEffect** | Small | Medium (combat restrictions) | Medium |
| **7a. ConditionalStaticAbility** | Medium | High (conditional keywords/stats) | Medium |
| **3a. ManaWasSpentOfColor** | Medium | Low-Medium (hybrid triggers) | Low |
| **2a. Mana Events** | Medium | Low (mana triggers are rare) | Low |
| **5a/5b. Prevention/Redirection** | High | Medium (Fog, redirects) | Low |
| **8a. CopyWithModifications** | Med-High | Low-Medium (copy variants) | Low |
| **9b/9c. MustBlock/AttackCap** | Medium | Low-Medium (combat forcing) | Low |
| **4c. NumberOfColors** | Small | Low (Domain/Converge) | Low |
| **6b. RemoveCreatureType** | Small | Low (type stripping is rare) | Low |

---

## Success Criteria

When all **High** and **Medium** priority gaps are closed:
- ~95% of cards implementable without SDK changes
- Remaining 5% are genuinely novel mechanics (new keyword abilities, unique stack
  interactions) that warrant intentional SDK extension

When **all** gaps are closed:
- ~98% coverage — only truly unprecedented mechanics need SDK work
- The SDK becomes a stable contract that rarely changes
