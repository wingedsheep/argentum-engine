# Mindslaver mechanic — "you control target opponent during their next turn"

Driving card: **The Dominion Bracelet** (Edge of Eternities). The first card to require this primitive; future cards (Mindslaver, Sorin Markov, etc.) will reuse it. The work is split into a card shell (PR 1, shipped) and three engine/UX phases (PR 2A done, 2B/2C pending).

## Current status

| Phase | Status | Commit |
|-------|--------|--------|
| **PR 1 — card shell, no-op hijack** | ✅ shipped | `0a80ba9b5` |
| **2A — engine lifecycle** | ✅ shipped | `874fc1575` |
| **2B — input routing** | ⏳ pending | — |
| **2C — frontend visibility / UX** | ⏳ pending | — |
| **PR 2D — proper card sourcing + cost reduction** | ⏳ pending | — |

## What works today

- Card is registered, castable, equippable. The granted activated ability is on the Bracelet itself (PR 1 placeholder) with cost `Composite(Mana({15}), ExileSelf)`, sorcery-speed, target-opponent.
- `HijackNextTurnEffect` resolves and attaches `PlayerTurnHijackedComponent(controllerId, SCHEDULED)` to the targeted player, latest-wins.
- `TurnManager.startTurn` transitions the component to `ACTIVE` when that player's actual next turn begins. Skipped turns wait per the Scryfall ruling.
- `CleanupPhaseManager.cleanupEndOfTurn` removes the component when the controlled turn ends.
- `GameState.actorFor(playerId)` is the public helper: returns the controller during ACTIVE, the player otherwise.

Tests: `rules-engine/src/test/kotlin/com/wingedsheep/engine/scenarios/PlayerTurnHijackedComponentTest.kt` covers SCHEDULED→ACTIVE, ACTIVE→removed, skip-turn waiting, and the helper.

**The mechanic does not yet route input.** The component flips state but `priorityPlayerId` and `pendingDecision.playerId` still point at the affected player — clicking has no effect because the controller can't submit on their behalf.

## Phase 2B — input routing (next)

The `actorFor` seam exists; consult it everywhere "who is allowed to act" is decided.

### Engine
- `rules-engine/.../handlers/actions/decision/SubmitDecisionHandler.validate` (line ~34): allow `action.playerId == state.actorFor(pending.playerId)` in addition to the strict equality check.
- Per-action handlers — audit each for `state.priorityPlayerId == action.playerId` checks and route through `actorFor`. Files to read:
  - `handlers/actions/spell/CastSpellHandler.kt`
  - `handlers/actions/ability/ActivateAbilityHandler.kt`
  - `handlers/actions/spell/PlayLandHandler.kt`
  - `handlers/actions/priority/PassPriorityHandler.kt`
  - `handlers/actions/combat/DeclareAttackersHandler.kt`
  - `handlers/actions/combat/DeclareBlockersHandler.kt`
  - `handlers/actions/combat/OrderBlockersHandler.kt`
- **Resource ownership stays with the affected player.** `ManaSolver` / `CostHandler` / etc. read costs against the *spell's* controller, not `action.playerId` — verify this assumption with a test. The actor switch is purely an authorization seam.

### Game-server
- `GameSession.getLegalActions(playerId)`: return enumerated actions when `playerId == state.actorFor(state.priorityPlayerId)` (currently strict equality).
- `GameSession.createStateUpdate`: send `pendingDecision` and `legalActions` to the actor (controller) when hijacked.

### Client DTO
- New optional fields on `ClientGameState` (in `rules-engine/.../view/ClientDTO.kt`):
  - `youAreHijacking: EntityId?` — the affected player you're currently controlling, if any.
  - `youAreHijackedBy: EntityId?` — the controller currently driving your turn, if any.
- Populate from `actorFor` in `ClientStateTransformer.transform`.

### Tests for 2B
- Scenario: hijacked player's `legalActions` is empty for them, populated for the controller.
- Scenario: controller submits `CastSpell` against the hijacked player's hand (using a synthetic in-hand spell), engine accepts and resolves with the spell controller = the affected player.
- Scenario: mana spent comes from the affected player's pool (not the controller's).

## Phase 2C — frontend visibility / UX

Designs landed: see conversation transcript on commit `874fc1575`. Summary:

- **Banner.** Top-bar pill, two states:
  - "You are controlling Vincent's turn — using only their resources" (controller's view).
  - "Your opponent is controlling your turn." (affected player's view, click handlers disabled, concede stays live).
- **Hand interactivity.** Promote the opponent's hand at the top from card-backs to a real `Hand` component when `youAreHijacking != null`. Reuses `GameCard` and `Hand`; reads V's hand data from `cards` in the client state. Opponent's view stays normal (face-up to themselves).
- **Tints / pills.** Purple frame on V's hand and V's battlefield row when controlled. Small `🔒 Vincent's hand (you control)` pill at the corner of the top hand and `Your hand` on the bottom.
- **Active surface cue.** Whichever hand currently has live legal actions gets the green "playable" highlight; the dimmed one indicates you can see but can't act yet.
- **Visibility.** `ClientStateTransformer.isZoneVisibleTo` (currently line ~316) — extend the `Zone.HAND` branch:
  ```kotlin
  Zone.HAND -> debugMode || zoneKey.ownerId == viewingPlayerId ||
      state.actorFor(zoneKey.ownerId) == viewingPlayerId
  ```
  Crucial constraint: do *not* leak hidden info that V themselves can't see (e.g. V's opponent's hand). The rule is "controller sees what V sees," not "all hidden zones."
- **Spectator masking.** Sanity-check `SpectatorStateBuilder` so spectators don't gain access to the hijacked hand.

### Tests for 2C
- Playwright: full flow — equip Bracelet, activate, opponent's hand becomes face-up next turn, controller casts a spell from it, opponent sees disabled UI + banner.

## Phase 2D — faithful card sourcing + cost reduction

PR 1 took two deliberate shortcuts that should be cleaned up once 2A–2C land. Without these, the card is technically playable but cost is unreduced.

### SDK
- New cost type `AbilityCost.ExileGrantingPermanent` — exiles the entity that granted this activated ability to the source. Resolved at activation time by walking `GrantActivatedAbilityToAttachedCreature` like `getStaticGrantedActivatedAbilities` already does.
- New field on `ActivatedAbility`:
  ```kotlin
  val genericCostReduction: DynamicAmount? = null
  ```
  Evaluated against the source entity at activation time, reducing the generic mana portion via `ManaCost.reduceGeneric(amount)`.

### Engine
- `ActivateAbilityHandler` (around line 160): after computing `effectiveCost`, evaluate `genericCostReduction` against the source and apply via `reduceGeneric`. Lock the reduced cost in (per Scryfall ruling, "cost is locked in before costs are paid").
- Granter tracking: `getStaticGrantedActivatedAbilities` currently returns a list of `ActivatedAbility` with no granter ID; thread the granter through (e.g. `Pair<ActivatedAbility, EntityId>`) so the executor for `ExileGrantingPermanent` can find the equipment to exile. Same change in `CastPermissionUtils.getStaticGrantedActivatedAbilities`.

### Card update
Switch the activated ability from being on the Bracelet to being granted to the equipped creature:
```kotlin
staticAbility {
    ability = GrantActivatedAbilityToAttachedCreature(
        ability = ActivatedAbility(
            cost = AbilityCost.Composite(
                AbilityCost.Mana(ManaCost.parse("{15}")),
                AbilityCost.ExileGrantingPermanent
            ),
            genericCostReduction = DynamicAmount.EntityProperty(
                EntityReference.Source, EntityNumericProperty.Power
            ),
            timing = TimingRule.SorcerySpeed,
            effect = Effects.HijackNextTurn(EffectTarget.ContextTarget(0)),
            targetRequirements = listOf(TargetOpponent())
        )
    )
}
```
Drop the placeholder activated-ability block on the Bracelet itself.

## Risks & open questions

- **AI advisors.** Combat advisors and the MCTS side will need `actorFor` awareness if the engine ever offers AI players. Currently low-pri — neither the AI nor the trainer cares about input authority, only state evolution.
- **Multiplayer.** Engine assumes 2-player. `actorFor` works for any seat count; "target opponent" → "the only opponent" simplification needs revisiting if/when multiplayer lands.
- **Concession.** Per ruling, V can still concede during the controlled turn. Verify `Concede` handler routes by `action.playerId` and ignores `actorFor` — concession is a player-level escape hatch, not a normal in-game action.
- **Triggered abilities.** The controller's own permanents may have triggers during V's turn. Those triggers' decisions belong to *them*, not V. Should fall out automatically since `ability.controllerId` is the controller, not V — but worth a focused test.

## Reference

- Scryfall: <https://scryfall.com/card/eoe/239/the-dominion-bracelet>
- Rulings (paraphrased) attached to the card definition in `mtg-sets/.../edgeofeternities/cards/TheDominionBracelet.kt`
- Conversation that produced this design: see commit messages on `0a80ba9b5` and `874fc1575`.
