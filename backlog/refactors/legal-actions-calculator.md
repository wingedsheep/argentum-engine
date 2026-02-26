# LegalActionsCalculator Performance & Architecture Refactor

Goals: **Lazy Target Evaluation** (saving CPU), **Fast-Failing Validation** (skipping expensive checks), **Modular Architecture** (making mechanics like Flashback or Escape easy to add later).

## Problem

`LegalActionsCalculator` is a performance bottleneck and an architectural liability:

- **Eager target computation is expensive.** `LegalActionInfo` includes `validTargets`, which means `TargetFinder` scans the entire board for every card in the player's hand on every state tick. Most of these targets are never used — the player will only cast one spell at a time.
- **No short-circuiting.** When checking if a spell *can* be cast, we build the full `List<EntityId>` of legal targets just to check `list.size >= minTargets`. We only need to know if enough targets exist, not what they all are.
- **Redundant recalculation.** Legal actions are recalculated on every minor state change, even when nothing relevant to the player has changed.
- **Duplicated validation logic.** Timing, cost, and target checks are implemented separately in `LegalActionsCalculator` and the `ActionHandler`s (e.g., `CastSpellHandler`). These can drift out of sync — one says you can cast, the other rejects.
- **Monolithic structure.** All action types (hand casts, activated abilities, combat, cycling) are tangled together in one giant `calculate()` function. Adding a new zone to cast from (graveyard for Flashback, exile for Escape, command zone) means editing the monolith rather than registering a new provider.

## Implementation Order

### Step 1: Remove `validTargets` from `LegalActionInfo` DTO
The biggest immediate win. Stops computing and serializing full target lists for every castable card on every state update. The engine already handles targeting via `ChooseTargetsDecision` when a spell is actually cast, so the upfront list is redundant — it only existed for the client to show highlighting, which can be deferred to cast time.

- [ ] Update `LegalActionInfo` in `ServerMessage.kt` — remove `validTargets` from the base DTO
- [ ] Client should only know `requiresTargets = true`
- [ ] Targeting flow: on cast, server replies with `PausedForDecision` containing `ChooseTargetsDecision` (already how the engine handles targeting)
- [ ] Fix all compiler errors caused by this change in `LegalActionsCalculator` and downstream
- [ ] Fix web-client to no longer depend on `validTargets` in the payload

### Step 2: Implement `TargetFinder.hasLegalTargets()` Short-Circuit
Even after removing `validTargets` from the DTO, we still need to know *whether* a spell can be cast (i.e., enough legal targets exist). Currently this calls `findLegalTargets()` which builds the entire list. A short-circuiting `hasLegalTargets()` can bail out the moment it finds `minTargets` matches, avoiding a full board scan for spells that target "any creature" on a crowded battlefield.

- [ ] Add `hasLegalTargets(state, requirement, controllerId, sourceId?)` to `TargetFinder`
  ```kotlin
  fun hasLegalTargets(
      state: GameState,
      requirement: TargetRequirement,
      controllerId: EntityId,
      sourceId: EntityId?
  ): Boolean {
      // Implement like findLegalTargets, but return true
      // the MOMENT it finds 'minTargets' valid entities, safely short-circuiting
      // the rest of the battlefield/graveyard scan.
  }
  ```
- [ ] Implement like `findLegalTargets` but return `true` the moment `minTargets` valid entities are found
- [ ] Swap into `LegalActionsCalculator` wherever target validation occurs (replace `findLegalTargets().size >= min` patterns)

### Step 3: Implement GameState Memoization
Legal actions only change when the game state changes (new spell resolved, phase advanced, permanent entered/left). But `getLegalActions()` gets called on every priority pass, even when the state hasn't ticked. A simple timestamp-based cache avoids redundant recalculation entirely for repeat calls on the same state.

- [ ] Add `cachedLegalActionsTimestamp` and `cachedLegalActions` to `GameSession`
- [ ] Short-circuit `getLegalActions()` when `state.timestamp` hasn't changed
- [ ] Early-return empty list when `priorityPlayerId != playerId` or `pendingDecision != null`

  ```kotlin
  // In GameSession.kt
  private var cachedLegalActionsTimestamp: Long = -1
  private var cachedLegalActions: List<LegalActionInfo> = emptyList()

  fun getLegalActions(playerId: EntityId): List<LegalActionInfo> {
      val state = gameState ?: return emptyList()

      // If the state hasn't ticked and it's the same player, return cache
      if (state.timestamp == cachedLegalActionsTimestamp) {
          return cachedLegalActions
      }

      if (state.priorityPlayerId != playerId || state.pendingDecision != null) {
          return emptyList()
      }

      // Construct by aggregating all registered providers
      val actions = providers.flatMap { it.getLegalActions(state, playerId) }

      cachedLegalActions = actions
      cachedLegalActionsTimestamp = state.timestamp

      return actions
  }
  ```

### Step 4: Modular Architecture (Clean-Up Phase)

#### 4a: Create Fast-Fail Validator Interfaces
Timing checks (is it your turn? right phase? stack empty?) are nearly free. Cost checks (can you pay mana?) are moderate. Target checks (board scan) are expensive. Right now these are interleaved unpredictably. Extracting them into a shared `ActionValidator` interface that runs cheapest-first means most illegal actions get rejected before touching `TargetFinder`. Sharing validators between `LegalActionsCalculator` and `ActionHandler`s also eliminates the duplication where one says you can cast but the other rejects.

- [ ] Create `validators` package with `ActionValidator<T : GameAction>` interface
  ```kotlin
  interface ActionValidator<T : GameAction> {
      // 1. Cheapest check: Is it your turn? Is the phase right? Is the stack empty?
      fun meetsTiming(state: GameState, action: T): Boolean

      // 2. Medium check: Can the player actually pay for this?
      fun canAfford(state: GameState, action: T): Boolean

      // 3. Expensive check: Do the minimum required targets exist?
      // Note: Returns a Boolean, NOT the actual list of targets.
      fun hasRequiredTargets(state: GameState, action: T): Boolean
  }
  ```
- [ ] Validators execute checks cheapest-to-most-expensive: `meetsTiming()` → `canAfford()` → `hasRequiredTargets()`
- [ ] `hasRequiredTargets()` returns `Boolean`, NOT the actual list of targets
- [ ] Inject validators into both `LegalActionProvider`s and existing `ActionHandler`s (eliminating duplication)

#### 4b: Break `LegalActionsCalculator` into `LegalActionProvider` Registry
The current monolithic `calculate()` handles hand casts, activated abilities, combat declarations, cycling, and morph in one function. Every new casting zone (graveyard for Flashback, exile for Escape, command zone for Commander) would mean more branches in the same function. A provider registry makes each source of legal actions an independent, testable unit. Adding Flashback becomes "register a `GraveyardActionProvider`" instead of editing a 300-line function.

- [ ] Define `LegalActionProvider` interface:
  ```kotlin
  interface LegalActionProvider {
      fun getLegalActions(state: GameState, playerId: EntityId): List<LegalActionInfo>
  }
  ```
- [ ] Implement `HandActionProvider` — loops hand, uses `SpellValidator`, emits `CastSpell` actions, checks Cycling/Typecycling
- [ ] Implement `BattlefieldAbilityProvider` — loops battlefield, uses `AbilityValidator`, emits `ActivateAbility` + `TurnFaceUp` actions
- [ ] Implement `CombatActionProvider` — emits `DeclareAttackers` / `DeclareBlockers` based on current step
- [ ] Stub `GraveyardActionProvider` for future Flashback/Escape/Commander support
- [ ] Wire providers into `GameSession.getLegalActions()` via `providers.flatMap { it.getLegalActions(state, playerId) }`
