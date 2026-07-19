package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityResolutionCountThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtByPlayersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamagedBySourcesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DealtCombatDamageToPlayersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TokenReplacementOfferedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.BlockedOrWasBlockedByLegendaryThisTurnComponent
import com.wingedsheep.engine.state.components.combat.CanAttackDespiteDefenderThisTurnComponent
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtEndOfTurnComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtNextEndStepComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.engine.state.components.player.AdditionalEndStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalEndStepComponent
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.CantCastFromNonHandZonesComponent
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.state.components.player.DamageReceivedFromArtifactsThisTurnComponent
import com.wingedsheep.engine.state.components.player.FlippedCoinsThisTurnComponent
import com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent
import com.wingedsheep.engine.state.components.player.RetainUnspentManaComponent
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.LandsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentTypesEnteredBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageByLegendaryCreatureThisTurnComponent
import com.wingedsheep.engine.state.components.player.CombatDamageReceivedThisTurnComponent
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentLeftBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsSacrificedThisTurnComponent
import com.wingedsheep.engine.state.components.player.RedNoncombatDamageDealtThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent
import com.wingedsheep.engine.state.components.player.TurnedPermanentFaceUpThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent
import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DamagePersistsThroughCleanup
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying

/**
 * Handles all end-of-turn cleanup: discard to hand size, damage removal,
 * expiration of temporary effects, and per-turn tracker resets.
 */
class CleanupPhaseManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler
) {

    // Stateless evaluators (default projection) used to read SetMaximumHandSize abilities and
    // their ConditionalStaticAbility gates at cleanup time (delegated to [MaximumHandSize]).
    private val conditionEvaluator = ConditionEvaluator()
    private val dynamicAmountEvaluator = DynamicAmountEvaluator(conditionEvaluator)

    /**
     * Perform cleanup step actions.
     * - Discard down to maximum hand size (7)
     * - Remove damage from creatures
     * - Remove "until end of turn" effects
     */
    fun performCleanupStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if player needs to discard
        val handKey = ZoneKey(activePlayer, Zone.HAND)
        val hand = newState.getZone(handKey)
        val maxHandSize = MaximumHandSize.effective(
            newState, activePlayer, cardRegistry, conditionEvaluator, dynamicAmountEvaluator
        )
        val cardsToDiscard = if (maxHandSize == null) 0 else hand.size - maxHandSize

        if (cardsToDiscard > 0) {
            // Player needs to discard - create a decision
            events.add(DiscardRequiredEvent(activePlayer, cardsToDiscard))

            // Create the card selection decision
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Discard down to $maxHandSize cards (choose $cardsToDiscard to discard)",
                options = hand,
                minSelections = cardsToDiscard,
                maxSelections = cardsToDiscard,
                ordered = false,
                phase = DecisionPhase.STATE_BASED
            )

            // Push continuation to handle the response
            val continuation = HandSizeDiscardContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // CR 514.2 turn-based actions: remove marked damage and end the per-turn markers that
        // expire with the cleanup step. The hand-size discard (CR 514.1) above early-returns
        // *before* this point; its continuation re-runs the same actions via
        // [applyCleanupTurnBasedActions], so this block must stay side-effect-equivalent there.
        newState = applyCleanupTurnBasedActions(newState, cardRegistry)

        // No priority during cleanup (normally)
        newState = newState.copy(priorityPlayerId = null)

        return ExecutionResult.success(newState, events)
    }

    /**
     * Expire UntilYourNextTurn floating effects after the untap step of the
     * controller's next turn. The effect needs to be active during the untap
     * step (to prevent untapping), then removed afterward.
     */
    fun expireUntilYourNextTurnEffects(state: GameState, activePlayer: EntityId): GameState {
        val remainingFloating = state.floatingEffects.filter { floatingEffect ->
            !(floatingEffect.duration is Duration.UntilYourNextTurn &&
                floatingEffect.controllerId == activePlayer)
        }
        val remainingGlobal = state.globalGrantedTriggeredAbilities.filter { grant ->
            !(grant.duration is Duration.UntilYourNextTurn &&
                grant.controllerId == activePlayer)
        }
        val floatingChanged = remainingFloating.size != state.floatingEffects.size
        val globalChanged = remainingGlobal.size != state.globalGrantedTriggeredAbilities.size
        var result = if (floatingChanged || globalChanged) {
            state.copy(
                floatingEffects = if (floatingChanged) remainingFloating else state.floatingEffects,
                globalGrantedTriggeredAbilities = if (globalChanged) remainingGlobal else state.globalGrantedTriggeredAbilities
            )
        } else {
            state
        }
        // Player-component "until your next turn" effects (The One Ring's protection) expire on
        // the same post-untap hook as floating UntilYourNextTurn effects.
        val protection = result.getEntity(activePlayer)?.get<PlayerProtectionComponent>()
        if (protection?.removeOn == PlayerEffectRemoval.UntilYourNextTurn) {
            result = result.updateEntity(activePlayer) { it.without<PlayerProtectionComponent>() }
        }
        val cantGainLife = result.getEntity(activePlayer)?.get<CantGainLifeComponent>()
        if (cantGainLife?.removeOn == PlayerEffectRemoval.UntilYourNextTurn) {
            result = result.updateEntity(activePlayer) { it.without<CantGainLifeComponent>() }
        }
        // Memory Vessel's "they can't play cards from their hand until your next turn" expires on
        // the same post-untap hook. The window keys off the *activating* player (every affected
        // player's restriction lifts on that player's next turn), so scan every player and remove
        // the component when [activePlayer] matches its expiry player — the explicit
        // [PlayerCantPlayFromHandComponent.expiresForPlayerId] when set, else the component owner.
        for (playerId in result.turnOrder) {
            val cantPlay = result.getEntity(playerId)?.get<PlayerCantPlayFromHandComponent>() ?: continue
            if (cantPlay.removeOn != PlayerEffectRemoval.UntilYourNextTurn) continue
            val expiryPlayer = cantPlay.expiresForPlayerId ?: playerId
            if (expiryPlayer == activePlayer) {
                result = result.updateEntity(playerId) { it.without<PlayerCantPlayFromHandComponent>() }
            }
        }
        // Avatar's Wrath's "your opponents can't cast spells from anywhere other than their hands
        // until your next turn" expires on the same post-untap hook, keyed off the *casting*
        // player (every affected opponent's restriction lifts on the caster's next turn).
        for (playerId in result.turnOrder) {
            val restricted = result.getEntity(playerId)?.get<CantCastFromNonHandZonesComponent>() ?: continue
            if (restricted.removeOn != PlayerEffectRemoval.UntilYourNextTurn) continue
            val expiryPlayer = restricted.expiresForPlayerId ?: playerId
            if (expiryPlayer == activePlayer) {
                result = result.updateEntity(playerId) { it.without<CantCastFromNonHandZonesComponent>() }
            }
        }
        return result
    }

    /**
     * Expire UntilYourNextUpkeep floating effects (and globally-granted triggered abilities) at the
     * beginning of the controller's next upkeep step. Mirrors [expireUntilYourNextTurnEffects] but is
     * keyed to the upkeep step rather than post-untap, and is invoked when the upkeep step begins
     * (TurnManager Step.UPKEEP). Used by e.g. Xenic Poltergeist ("Until your next upkeep, target
     * noncreature artifact becomes an artifact creature …") and Erhnam Djinn's granted forestwalk.
     *
     * Note: a card that *re-applies* the effect every upkeep (Erhnam Djinn) does so via its own
     * upkeep-triggered ability, which resolves after this expiry; the freshly granted effect then
     * carries the new turn's timestamp and survives this same-step expiry on the following turn.
     */
    fun expireUntilYourNextUpkeepEffects(state: GameState, activePlayer: EntityId): GameState {
        val remainingFloating = state.floatingEffects.filter { floatingEffect ->
            !(floatingEffect.duration is Duration.UntilYourNextUpkeep &&
                floatingEffect.controllerId == activePlayer)
        }
        val remainingGlobal = state.globalGrantedTriggeredAbilities.filter { grant ->
            !(grant.duration is Duration.UntilYourNextUpkeep &&
                grant.controllerId == activePlayer)
        }
        val floatingChanged = remainingFloating.size != state.floatingEffects.size
        val globalChanged = remainingGlobal.size != state.globalGrantedTriggeredAbilities.size
        return if (floatingChanged || globalChanged) {
            state.copy(
                floatingEffects = if (floatingChanged) remainingFloating else state.floatingEffects,
                globalGrantedTriggeredAbilities = if (globalChanged) remainingGlobal else state.globalGrantedTriggeredAbilities
            )
        } else {
            state
        }
    }

    /**
     * Expire everything keyed to "the next end step", invoked on entry to the end step (TurnManager
     * Step.END) — one step earlier than end-of-turn cleanup. Two things wear off here:
     *
     *  - floating effects with [Duration.UntilNextEndStep], and
     *  - temporary copies tagged [RevertCopyAtNextEndStepComponent] (Niko, Light of Hope's "Shards
     *    you control become copies of it until the next end step"), restored from their
     *    [CopyOfComponent.originalCardComponent] snapshot — the same revert mechanism cleanup uses
     *    for [RevertCopyAtEndOfTurnComponent], just fired at the start of the step.
     *
     * Not player-keyed: "the next end step" is the next one of any player's turn. Because this runs
     * on *entry* to the end step, an effect created later in that same end step is not present yet,
     * so it correctly survives to the following end step — matching the paired delayed
     * "at the beginning of the next end step" trigger.
     */
    fun performNextEndStepExpiry(state: GameState): GameState {
        var result = state

        val remainingFloating = result.floatingEffects.filter { it.duration !is Duration.UntilNextEndStep }
        if (remainingFloating.size != result.floatingEffects.size) {
            result = result.copy(floatingEffects = remainingFloating)
        }

        for ((entityId, container) in result.entities) {
            if (!container.has<RevertCopyAtNextEndStepComponent>()) continue
            val originalCard = container.get<CopyOfComponent>()?.originalCardComponent
            result = result.updateEntity(entityId) { c ->
                var reverted = c.without<RevertCopyAtNextEndStepComponent>()
                if (originalCard != null) {
                    reverted = reverted.with(originalCard).without<CopyOfComponent>()
                }
                reverted
            }
        }

        return result
    }

    /**
     * Expire goaded designations (CR 701.15a) for which [activePlayer] is a goader.
     * Runs alongside [expireUntilYourNextTurnEffects] so all "until your next turn"
     * semantics share the same hook (post-untap of the goader's next turn). Removes
     * [activePlayer] from each [GoadedComponent.goaderIds] set on the battlefield,
     * dropping the component entirely when the set empties, and emits a
     * [CreatureNoLongerGoadedEvent] per affected creature.
     */
    fun expireGoadedDesignationFor(
        state: GameState,
        activePlayer: EntityId
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in newState.getBattlefield()) {
            val goaded = newState.getEntity(entityId)?.get<GoadedComponent>() ?: continue
            if (activePlayer !in goaded.goaderIds) continue
            val remaining = goaded.goaderIds - activePlayer
            newState = newState.updateEntity(entityId) { container ->
                if (remaining.isEmpty()) container.without<GoadedComponent>()
                else container.with(GoadedComponent(remaining))
            }
            val creatureName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Creature"
            events += CreatureNoLongerGoadedEvent(
                creatureId = entityId,
                creatureName = creatureName,
                expiredGoaderId = activePlayer,
                stillGoadedByPlayerIds = remaining
            )
        }
        return newState to events
    }

    /**
     * Expire UntilAfterAffectedControllersNextUntap floating effects after the untap step.
     * These expire when any of the affected entities are controlled by the active player,
     * meaning the affected creature's controller just had their untap step.
     */
    fun expireAffectedControllersNextUntapEffects(state: GameState, activePlayer: EntityId): GameState {
        val projected = state.projectedState
        val remaining = state.floatingEffects.filter { floatingEffect ->
            if (floatingEffect.duration !is Duration.UntilAfterAffectedControllersNextUntap) return@filter true
            // Expire if any affected entity is controlled by the active player
            val affectedByActivePlayer = floatingEffect.effect.affectedEntities.any { entityId ->
                projected.getController(entityId) == activePlayer
            }
            !affectedByActivePlayer
        }
        return if (remaining.size != state.floatingEffects.size) {
            state.copy(floatingEffects = remaining)
        } else {
            state
        }
    }

    /**
     * Remove WhileSourceTapped floating effects whose source is no longer tapped.
     * Called during untap step to prevent stale effects from accumulating.
     *
     * Also handles [Duration.WhileSourceTappedAndAffectedPowerAtMostSource] (Old Man of
     * the Sea); the power-comparison half is gated per-frame by [StateProjector], so
     * cleanup here only enforces the source-tapped half — same rule as [Duration.WhileSourceTapped].
     */
    fun cleanupWhileSourceTappedEffects(state: GameState): GameState {
        val remaining = state.floatingEffects.filter { floatingEffect ->
            when (floatingEffect.duration) {
                is Duration.WhileSourceTapped,
                is Duration.WhileSourceTappedAndAffectedPowerAtMostSource -> {
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && state.getBattlefield().contains(sourceId) &&
                        state.getEntity(sourceId)?.has<TappedComponent>() == true
                }
                else -> true
            }
        }
        return if (remaining.size != state.floatingEffects.size) {
            state.copy(floatingEffects = remaining)
        } else {
            state
        }
    }

    /**
     * Empty every player's unspent mana as a step or phase ends (CR 500.5 / 703.4q — a turn-based
     * action that doesn't use the stack). This is the single mana-emptying primitive, called at
     * every step/phase transition ([com.wingedsheep.engine.core.TurnManager.advanceStep]) and again
     * as the cleanup step ends (end of turn). It applies the per-player mana-loss statics:
     *  - Upwelling ([PreventManaPoolEmptying]) — no one loses mana at all (whole action skipped).
     *  - Ozai, the Phoenix King ([ConvertEmptyingManaToRed]) — the controller's would-be-lost mana
     *    becomes that many red mana instead (CR 614).
     *  - The Last Agni Kai ([RetainUnspentManaComponent]) — the named colours are kept.
     * Firebending (END_OF_COMBAT) mana is preserved by [ManaPoolComponent.emptyAtBoundary] and
     * handled instead by `CombatManager.endCombat`, since it lasts until end of combat, not step end.
     */
    fun emptyManaPools(state: GameState): GameState {
        // Runs on every step/phase boundary; almost always every pool is already empty (no mana
        // floated), so skip the battlefield scans below in that common case.
        if (state.turnOrder.all { state.getEntity(it)?.get<ManaPoolComponent>()?.isEmpty != false }) return state
        if (isManaPoolEmptyingPrevented(state)) return state
        var newState = state
        val convertToRedPlayers = playersConvertingEmptyingManaToRed(state, cardRegistry)
        for (playerId in state.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                val manaPool = container.get<ManaPoolComponent>()
                if (manaPool != null && !manaPool.isEmpty) {
                    val retained = container.get<RetainUnspentManaComponent>()?.colors ?: emptySet()
                    container.with(
                        manaPool.emptyAtBoundary(
                            convertToRed = playerId in convertToRedPlayers,
                            retain = retained
                        )
                    )
                } else {
                    container
                }
            }
        }
        return newState
    }

    /**
     * Check if any permanent on the battlefield has the PreventManaPoolEmptying static ability.
     * Used for cards like Upwelling: "Players don't lose unspent mana as steps and phases end."
     */
    private fun isManaPoolEmptyingPrevented(state: GameState): Boolean {
        val registry = cardRegistry
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventManaPoolEmptying }) {
                return true
            }
        }
        return false
    }

    /**
     * Clean up end-of-turn effects.
     *
     * This is called at the end of each turn and handles:
     * 1. Expiring "until end of turn" floating effects (Giant Growth, etc.)
     * 2. Emptying mana pools
     * 3. Resetting per-turn trackers (land drops)
     */
    fun cleanupEndOfTurn(state: GameState): GameState {
        var newState = state

        // 1. Expire floating effects with EndOfTurn duration
        val remainingEffects = newState.floatingEffects.filter { floatingEffect ->
            when (floatingEffect.duration) {
                is Duration.EndOfTurn -> false  // Remove it
                is Duration.EndOfCombat -> false  // Should already be removed, but clean up
                is Duration.UntilYourNextTurn -> true  // Keep until that player's next turn
                is Duration.UntilYourNextUpkeep -> true  // Keep until upkeep
                is Duration.UntilNextEndStep -> true  // Expired on entry to the next end step (performNextEndStepExpiry)
                is Duration.Permanent -> true  // Never expires
                is Duration.WhileSourceOnBattlefield -> {
                    // Keep if source is still on battlefield
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId)
                }
                is Duration.WhileYouControlSource -> {
                    // Keep if source is still on battlefield. The source-controller half is gated
                    // per-frame by StateProjector and physically latched by EndedDurationExpiryCheck,
                    // so end-of-turn cleanup only enforces the battlefield half here.
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId)
                }
                is Duration.WhileSourceTapped,
                is Duration.WhileSourceTappedAndAffectedPowerAtMostSource -> {
                    // Keep if source is still on battlefield AND tapped. The power-comparison
                    // half of WhileSourceTappedAndAffectedPowerAtMostSource is gated per-frame
                    // by StateProjector, so cleanup only enforces the source-tapped condition.
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId) &&
                        newState.getEntity(sourceId)?.has<TappedComponent>() == true
                }
                Duration.WhileSourceAttachedToAffected -> {
                    // Keep if the source (Aura/Equipment) is still on the battlefield. The
                    // per-affected "still attached to it" half is gated per-frame by StateProjector
                    // and latched off by EndedDurationExpiryCheck; cleanup only drops it once the
                    // source itself has left.
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId)
                }
                is Duration.WhileControlledByController -> true  // Gated at projection by controller; cleared on leaving play
                is Duration.WhileAffectedHasCounter -> true  // Gated per-frame by StateProjector (counter present); latched off by EndedDurationExpiryCheck when the counter leaves
                is Duration.WhileAffectedTapped -> true  // Gated per-frame by StateProjector / activation-legality checks; latched off by EndedDurationExpiryCheck when the permanent untaps
                is Duration.UntilAfterAffectedControllersNextUntap -> true  // Expires after affected entity's controller's untap
                is Duration.UntilPhase -> true  // Handle in phase transitions
                is Duration.UntilCondition -> true  // Handle condition checking elsewhere
            }
        }
        newState = newState.copy(floatingEffects = remainingEffects)

        // 1a. Expire "yield until end of turn" preferences (backlog §C). Whole-game yields and
        // auto-answers persist; only the per-turn auto-pass opt-in is cleared here at cleanup
        // (CR 514), keeping the lifecycle replay-deterministic alongside the other end-of-turn resets.
        newState = newState.clearUntilEndOfTurnYields()

        // 1b. Expire temporary counter-placement modifiers (GrantCounterPlacementModifierEffect,
        // e.g. Prairie Dog) whose duration ends at end of turn / end of combat. Longer durations
        // (UntilYourNextTurn, Permanent, …) are kept and handled by their own expiry path.
        val remainingCounterModifiers = newState.activeCounterPlacementModifiers.filter { modifier ->
            when (modifier.duration) {
                is Duration.EndOfTurn -> false
                is Duration.EndOfCombat -> false
                else -> true
            }
        }
        if (remainingCounterModifiers.size != newState.activeCounterPlacementModifiers.size) {
            newState = newState.copy(activeCounterPlacementModifiers = remainingCounterModifiers)
        }

        // 2. Empty mana pools as this (cleanup) step ends — one of the per-step/phase emptyings
        // (CR 500.5 / 703.4q; if cleanup grants priority, advanceStep empties once more, idempotently).
        // The RetainUnspentManaComponent marker (The Last Agni Kai) still keeps its colours here; the
        // marker itself is cleared in step 4 below.
        newState = emptyManaPools(newState)

        // 3. Reset per-turn trackers (land drops reset at start of turn, but clean up here too)
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                val landDrops = container.get<LandDropsComponent>()
                if (landDrops != null) {
                    container.with(landDrops.reset())
                } else {
                    container
                }
            }
        }

        // 4. Remove any unconsumed additional combat phase components, temporary player shroud, and damage tracking
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                var result = container
                if (result.has<AdditionalPhasesComponent>()) {
                    result = result.without<AdditionalPhasesComponent>()
                }
                if (result.has<InAdditionalCombatPhaseComponent>()) {
                    result = result.without<InAdditionalCombatPhaseComponent>()
                }
                // Clear any leftover additional-end-step state. The count is normally drained by the
                // TurnManager, but a turn could end with the marker still set (it persists across the
                // extra end steps so IsFirstEndStepOfTurn keeps reading false); reset both so the next
                // turn's first end step is genuinely "first" again.
                if (result.has<AdditionalEndStepsComponent>()) {
                    result = result.without<AdditionalEndStepsComponent>()
                }
                if (result.has<InAdditionalEndStepComponent>()) {
                    result = result.without<InAdditionalEndStepComponent>()
                }
                // Drop a Mindslaver-style hijack at end of the controlled turn (ACTIVE state).
                // Scheduled hijacks (SCHEDULED) survive cleanup so they fire on the player's
                // actual next turn, even if intervening turns are skipped.
                val hijack = result.get<PlayerTurnHijackedComponent>()
                if (hijack != null && hijack.state == PlayerTurnHijackedComponent.HijackState.ACTIVE) {
                    result = result.without<PlayerTurnHijackedComponent>()
                }
                val shroud = result.get<PlayerShroudComponent>()
                if (shroud?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerShroudComponent>()
                }
                val hexproof = result.get<PlayerHexproofComponent>()
                if (hexproof?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerHexproofComponent>()
                }
                val protection = result.get<PlayerProtectionComponent>()
                if (protection?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerProtectionComponent>()
                }
                val cantPlayFromHand = result.get<PlayerCantPlayFromHandComponent>()
                if (cantPlayFromHand?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerCantPlayFromHandComponent>()
                }
                val cantCast = result.get<CantCastSpellsComponent>()
                if (cantCast?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<CantCastSpellsComponent>()
                }
                val cantCastNonHand = result.get<CantCastFromNonHandZonesComponent>()
                if (cantCastNonHand?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<CantCastFromNonHandZonesComponent>()
                }
                val cantGainLife = result.get<CantGainLifeComponent>()
                if (cantGainLife?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<CantGainLifeComponent>()
                }
                val cantLoyalty = result.get<CantActivateLoyaltyAbilitiesComponent>()
                if (cantLoyalty?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<CantActivateLoyaltyAbilitiesComponent>()
                }
                val spellsUncounterable = result.get<SpellsCantBeCounteredComponent>()
                if (spellsUncounterable?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<SpellsCantBeCounteredComponent>()
                }
                val flashGrants = result.get<FlashGrantsThisTurnComponent>()
                if (flashGrants?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<FlashGrantsThisTurnComponent>()
                }
                // Colour-filtered mana retention (The Last Agni Kai): the kept mana already
                // survived the step-2 emptying above; drop the now-spent marker here.
                val retainMana = result.get<RetainUnspentManaComponent>()
                if (retainMana?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<RetainUnspentManaComponent>()
                }
                val graveyardForage = result.get<MayCastCreaturesFromGraveyardWithForageComponent>()
                if (graveyardForage?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<MayCastCreaturesFromGraveyardWithForageComponent>()
                }
                val damageBonus = result.get<DamageBonusComponent>()
                if (damageBonus?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<DamageBonusComponent>()
                }
                if (result.has<DamageReceivedThisTurnComponent>()) {
                    result = result.without<DamageReceivedThisTurnComponent>()
                }
                if (result.has<DamageReceivedFromArtifactsThisTurnComponent>()) {
                    result = result.without<DamageReceivedFromArtifactsThisTurnComponent>()
                }
                if (result.has<NonTokenCreaturesDiedThisTurnComponent>()) {
                    result = result.without<NonTokenCreaturesDiedThisTurnComponent>()
                }
                if (result.has<CreaturesDiedThisTurnComponent>()) {
                    result = result.without<CreaturesDiedThisTurnComponent>()
                }
                if (result.has<com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent>()) {
                    result = result.without<com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent>()
                }
                if (result.has<PermanentLeftBattlefieldThisTurnComponent>()) {
                    result = result.without<PermanentLeftBattlefieldThisTurnComponent>()
                }
                if (result.has<com.wingedsheep.engine.state.components.player.CreatureLeftBattlefieldThisTurnComponent>()) {
                    result = result.without<com.wingedsheep.engine.state.components.player.CreatureLeftBattlefieldThisTurnComponent>()
                }
                if (result.has<OpponentCreaturesExiledThisTurnComponent>()) {
                    result = result.without<OpponentCreaturesExiledThisTurnComponent>()
                }
                if (result.has<PlayerDescendedThisTurnComponent>()) {
                    result = result.without<PlayerDescendedThisTurnComponent>()
                }
                if (result.has<FlippedCoinsThisTurnComponent>()) {
                    result = result.without<FlippedCoinsThisTurnComponent>()
                }
                if (result.has<PermanentsSacrificedThisTurnComponent>()) {
                    result = result.without<PermanentsSacrificedThisTurnComponent>()
                }
                if (result.has<RedNoncombatDamageDealtThisTurnComponent>()) {
                    result = result.without<RedNoncombatDamageDealtThisTurnComponent>()
                }
                if (result.has<PermanentEnteredFaceDownThisTurnComponent>()) {
                    result = result.without<PermanentEnteredFaceDownThisTurnComponent>()
                }
                if (result.has<TurnedPermanentFaceUpThisTurnComponent>()) {
                    result = result.without<TurnedPermanentFaceUpThisTurnComponent>()
                }
                if (result.has<LifeGainedThisTurnComponent>()) {
                    result = result.without<LifeGainedThisTurnComponent>()
                }
                if (result.has<LifeGainedAmountThisTurnComponent>()) {
                    result = result.without<LifeGainedAmountThisTurnComponent>()
                }
                if (result.has<LifeLostThisTurnComponent>()) {
                    result = result.without<LifeLostThisTurnComponent>()
                }
                if (result.has<CardsLeftGraveyardThisTurnComponent>()) {
                    result = result.without<CardsLeftGraveyardThisTurnComponent>()
                }
                if (result.has<SacrificedFoodThisTurnComponent>()) {
                    result = result.without<SacrificedFoodThisTurnComponent>()
                }
                if (result.has<PermanentTypesEnteredBattlefieldThisTurnComponent>()) {
                    result = result.without<PermanentTypesEnteredBattlefieldThisTurnComponent>()
                }
                if (result.has<LandsEnteredUnderControlThisTurnComponent>()) {
                    result = result.without<LandsEnteredUnderControlThisTurnComponent>()
                }
                if (result.has<PermanentsEnteredUnderControlThisTurnComponent>()) {
                    result = result.without<PermanentsEnteredUnderControlThisTurnComponent>()
                }
                if (result.has<PutCounterOnCreatureThisTurnComponent>()) {
                    result = result.without<PutCounterOnCreatureThisTurnComponent>()
                }
                if (result.has<WasDealtCombatDamageThisTurnComponent>()) {
                    result = result.without<WasDealtCombatDamageThisTurnComponent>()
                }
                if (result.has<CombatDamageReceivedThisTurnComponent>()) {
                    result = result.without<CombatDamageReceivedThisTurnComponent>()
                }
                if (result.has<WasDealtCombatDamageByLegendaryCreatureThisTurnComponent>()) {
                    result = result.without<WasDealtCombatDamageByLegendaryCreatureThisTurnComponent>()
                }
                result
            }
        }

        // 5. Clear per-turn ability activation tracking, damage source tracking, and attack tracking
        for ((entityId, container) in newState.entities) {
            var needsUpdate = false
            if (container.has<AbilityActivatedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<DamageDealtToCreaturesThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<TargetedByControllerThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<ReceivedCountersThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<PlayerAttackedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<PlayerAttackersThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<com.wingedsheep.engine.state.components.combat.PlayerAttackedPlayersThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<GraveyardPlayPermissionUsedComponent>()) {
                needsUpdate = true
            }
            if (container.has<TriggeredAbilityFiredThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<AbilityResolutionCountThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<TokenReplacementOfferedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<WasDealtDamageThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<BlockedOrWasBlockedByLegendaryThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<DamageDealtByPlayersThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<DamagedBySourcesThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<DealtCombatDamageToPlayersThisTurnComponent>()) {
                needsUpdate = true
            }
            // Saddled lasts "until end of turn" (CR 702.171b)
            if (container.has<SaddledComponent>()) {
                needsUpdate = true
            }
            // "Creatures that crewed/saddled it this turn" resets each turn
            if (container.has<CrewSaddleContributorsComponent>()) {
                needsUpdate = true
            }
            if (needsUpdate) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<AbilityActivatedThisTurnComponent>()
                        .without<DamageDealtToCreaturesThisTurnComponent>()
                        .without<TargetedByControllerThisTurnComponent>()
                        .without<ReceivedCountersThisTurnComponent>()
                        .without<PlayerAttackedThisTurnComponent>()
                        .without<PlayerAttackersThisTurnComponent>()
                        .without<com.wingedsheep.engine.state.components.combat.PlayerAttackedPlayersThisTurnComponent>()
                        .without<GraveyardPlayPermissionUsedComponent>()
                        .without<TriggeredAbilityFiredThisTurnComponent>()
                        .without<AbilityResolutionCountThisTurnComponent>()
                        .without<TokenReplacementOfferedThisTurnComponent>()
                        .without<WasDealtDamageThisTurnComponent>()
                        .without<BlockedOrWasBlockedByLegendaryThisTurnComponent>()
                        .without<DamageDealtByPlayersThisTurnComponent>()
                        .without<DamagedBySourcesThisTurnComponent>()
                        .without<DealtCombatDamageToPlayersThisTurnComponent>()
                        .without<SaddledComponent>()
                        .without<CrewSaddleContributorsComponent>()
                }
            }
        }

        // 5b. Strip "until end of turn" text replacements (Crystal Spray). Indefinite
        // replacements (Artificial Evolution) have Duration.Permanent and survive.
        for ((entityId, container) in newState.entities) {
            val textReplacement = container.get<TextReplacementComponent>() ?: continue
            if (textReplacement.replacements.none { it.duration is Duration.EndOfTurn }) continue
            val remaining = textReplacement.replacements.filter { it.duration !is Duration.EndOfTurn }
            newState = newState.updateEntity(entityId) { c ->
                if (remaining.isEmpty()) c.without<TextReplacementComponent>()
                else c.with(textReplacement.copy(replacements = remaining))
            }
        }

        // 5c. Revert "becomes a copy of … until end of turn" group copies (Naga Fleshcrafter's
        // renew). Restore each tagged permanent's pre-copy CardComponent from its CopyOfComponent
        // snapshot and drop both the marker and the copy tag. Permanent copies (Mirrorform, Clone)
        // are never tagged, so they are untouched. If the snapshot is missing (defensive), only the
        // marker is removed so the entity isn't left flagged.
        for ((entityId, container) in newState.entities) {
            if (!container.has<RevertCopyAtEndOfTurnComponent>()) continue
            val originalCard = container.get<CopyOfComponent>()?.originalCardComponent
            newState = newState.updateEntity(entityId) { c ->
                var reverted = c.without<RevertCopyAtEndOfTurnComponent>()
                if (originalCard != null) {
                    reverted = reverted.with(originalCard).without<CopyOfComponent>()
                }
                reverted
            }
        }

        // 6. Expire granted triggered abilities with EndOfTurn duration
        if (newState.grantedTriggeredAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedTriggeredAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedTriggeredAbilities = remainingGrants)
        }

        // 7. Expire granted activated abilities with EndOfTurn duration
        if (newState.grantedActivatedAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedActivatedAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedActivatedAbilities = remainingGrants)
        }

        // 7a. Expire granted static abilities (e.g. Full Steam Ahead's "can't be blocked by
        // more than one creature") with EndOfTurn duration.
        if (newState.grantedStaticAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedStaticAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedStaticAbilities = remainingGrants)
        }

        // 7a-i. Expire granted replacement effects (e.g. Forgotten Cellar's "exile instead of
        // graveyard this turn") with EndOfTurn duration.
        if (newState.grantedReplacementEffects.isNotEmpty()) {
            val remainingGrants = newState.grantedReplacementEffects.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedReplacementEffects = remainingGrants)
        }

        // 7b. Expire granted cast-keyword abilities (e.g. Songcrafter Mage's harmonize) with
        // EndOfTurn duration.
        if (newState.grantedKeywordAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedKeywordAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedKeywordAbilities = remainingGrants)
        }

        // 8. Expire global granted triggered abilities with EndOfTurn duration
        if (newState.globalGrantedTriggeredAbilities.isNotEmpty()) {
            val remainingGrants = newState.globalGrantedTriggeredAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(globalGrantedTriggeredAbilities = remainingGrants)
        }

        // 9. Expire non-permanent permissions and PlayWithoutPayingCostComponent (end of turn)
        // Skip permanent ones (used by "for as long as it remains exiled" effects)
        // For expiresAfterTurn: keep alive until that turn number's end step
        // Also clear ExileEntryTurnComponent so "exiled with [granter] this turn" effects
        // (e.g. Maralen, Fae Ascendant) reset between turns. The engine's turnNumber only
        // increments per round, not per active player, so simply comparing turn numbers
        // would let an exile entry leak across the opponent's turn.
        for ((entityId, container) in newState.entities) {
            val playFree = container.get<PlayWithoutPayingCostComponent>()
            val removePlayFree = playFree != null && !playFree.permanent
            val removeLinkedExileUsed = container.get<com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent>() != null
            val removeFreeCastUsed = container.get<com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent>() != null
            val removeExileEntryTurn = container.get<com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent>() != null
            if (removePlayFree || removeLinkedExileUsed || removeFreeCastUsed || removeExileEntryTurn) {
                newState = newState.updateEntity(entityId) { c ->
                    var updated = c
                    if (removePlayFree) updated = updated.without<PlayWithoutPayingCostComponent>()
                    if (removeLinkedExileUsed) updated = updated.without<com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent>()
                    if (removeFreeCastUsed) updated = updated.without<com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent>()
                    if (removeExileEntryTurn) updated = updated.without<com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent>()
                    updated
                }
            }
        }

        // Expire non-permanent may-play permissions whose duration has elapsed.
        // `turnNumber` is round-based (it increments only when the starting player begins a
        // new turn), so it can't distinguish the controller's turn from the opponent's within
        // the same round. A permission that should last "until the end of your next turn" must
        // therefore only expire at the cleanup of the *controller's own* turn — otherwise the
        // non-starting player would lose it at the end of the starting player's turn in the
        // target round, one turn early (Burning Curiosity, Sizzling Changeling).
        if (newState.mayPlayPermissions.isNotEmpty()) {
            newState = newState.copy(
                mayPlayPermissions = newState.mayPlayPermissions.filterNot { permission ->
                    !permission.permanent && when {
                        permission.expiresAfterTurn != null ->
                            newState.turnNumber >= permission.expiresAfterTurn &&
                                newState.activePlayerId == (permission.expiryControllerId ?: permission.controllerId)
                        else -> true
                    }
                }
            )
        }

        // 10. Expire event-based delayed triggered abilities with EndOfTurn expiry
        // (e.g., Long River Lurker's "whenever that creature deals combat damage this turn").
        if (newState.delayedTriggers.isNotEmpty()) {
            val remainingDelayed = newState.delayedTriggers.filter { delayed ->
                delayed.expiry !is com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry.EndOfTurn
            }
            if (remainingDelayed.size != newState.delayedTriggers.size) {
                newState = newState.copy(delayedTriggers = remainingDelayed)
            }
        }

        return newState
    }

    companion object {
        /**
         * Apply the CR 514.2 turn-based cleanup actions: remove all marked damage from
         * permanents and end the per-turn combat/miracle markers that expire with the cleanup
         * step. Extracted from [performCleanupStep] so it can run in *both* cleanup paths.
         *
         * [performCleanupStep] performs the CR 514.1 hand-size discard first, and when a discard
         * is required it early-returns to ask the player — *before* these 514.2 actions. That
         * pause is resumed by [com.wingedsheep.engine.core.HandSizeDiscardContinuation], whose
         * resumer must call this to finish the step. If it doesn't, marked damage survives into
         * the next turn — most visibly deathtouch damage that an expiring "until end of turn"
         * indestructible (e.g. Saved by the Shell) had been suppressing, which then kills the
         * creature on the following turn's state-based-action check.
         */
        fun applyCleanupTurnBasedActions(
            state: GameState,
            cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
        ): GameState {
            var newState = state

            // Remove damage from all permanents on the battlefield (Rule 514.2).
            // Includes vehicles that reverted from creature status this turn — their damage
            // (and P/T from the expired floating effect) must be cleared. Permanents with a
            // [DamagePersistsThroughCleanup] static ability (Ancient Adamantoise) are the
            // exception — their marked damage is left in place and accumulates.
            val battlefield = newState.getBattlefield().toSet()
            val permanentsWithDamage = newState.entities.filter { (entityId, container) ->
                entityId in battlefield && container.has<DamageComponent>()
            }.keys
            for (entityId in permanentsWithDamage) {
                if (damagePersistsThroughCleanup(newState, cardRegistry, entityId)) continue
                newState = newState.updateEntity(entityId) { it.without<DamageComponent>() }
            }

            // Remove MustAttackThisTurnComponent from all creatures (Walking Desecration effect)
            val creaturesWithMustAttack = newState.entities.filter { (_, container) ->
                container.has<MustAttackThisTurnComponent>()
            }.keys
            for (entityId in creaturesWithMustAttack) {
                newState = newState.updateEntity(entityId) { it.without<MustAttackThisTurnComponent>() }
            }

            // Remove CanAttackDespiteDefenderThisTurnComponent (Krotiq Nestguard's "can attack
            // this turn as though it didn't have defender" activated ability).
            val creaturesWithCanAttackDespiteDefender = newState.entities.filter { (_, container) ->
                container.has<CanAttackDespiteDefenderThisTurnComponent>()
            }.keys
            for (entityId in creaturesWithCanAttackDespiteDefender) {
                newState = newState.updateEntity(entityId) { it.without<CanAttackDespiteDefenderThisTurnComponent>() }
            }

            // Close any open miracle windows (CR 702.94 — the chance to cast for the miracle cost lasts
            // only the turn the card was drawn).
            val cardsWithMiracleWindow = newState.entities.filter { (_, container) ->
                container.has<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>()
            }.keys
            for (entityId in cardsWithMiracleWindow) {
                newState = newState.updateEntity(entityId) {
                    it.without<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>()
                }
            }

            return newState
        }

        /**
         * True when [entityId]'s permanent has a [DamagePersistsThroughCleanup] static ability —
         * either printed on its (face-aware) script or granted to it — so the CR 514.2 cleanup
         * damage removal must skip it (Ancient Adamantoise).
         */
        private fun damagePersistsThroughCleanup(
            state: GameState,
            cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
            entityId: EntityId,
        ): Boolean {
            val container = state.getEntity(entityId) ?: return false
            val cardDef = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            val printed = cardDef != null &&
                RoomFaceStatics.activeStaticAbilities(container, cardDef)
                    .any { it is DamagePersistsThroughCleanup }
            if (printed) return true
            return state.grantedStaticAbilities.any {
                it.entityId == entityId && it.ability is DamagePersistsThroughCleanup
            }
        }
    }
}
