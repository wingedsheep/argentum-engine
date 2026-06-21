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
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtEndOfTurnComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent
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
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentLeftBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsSacrificedThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent
import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Handles all end-of-turn cleanup: discard to hand size, damage removal,
 * expiration of temporary effects, and per-turn tracker resets.
 */
class CleanupPhaseManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler
) {

    /** Default maximum hand size (CR 402.2), absent any effect that sets or removes it. */
    private val defaultMaxHandSize = 7

    // Stateless evaluators (default projection) used to read SetMaximumHandSize abilities and
    // their ConditionalStaticAbility gates at cleanup time.
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
        val maxHandSize = maximumHandSize(newState, activePlayer)
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

        // Remove damage from all permanents on the battlefield (Rule 514.2).
        // Includes vehicles that reverted from creature status this turn — their damage
        // (and P/T from the expired floating effect) must be cleared.
        val battlefield = newState.getBattlefield().toSet()
        val permanentsWithDamage = newState.entities.filter { (entityId, container) ->
            entityId in battlefield && container.has<DamageComponent>()
        }.keys

        for (entityId in permanentsWithDamage) {
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
     * The effective maximum hand size for [playerId] at cleanup, or `null` when they have no
     * maximum (Reliquary Tower / Wisdom of Ages — [hasNoMaximumHandSize]).
     *
     * Starts from [defaultMaxHandSize] (CR 402.2) and applies every [SetMaximumHandSize] static
     * ability on the battlefield whose [SetMaximumHandSize.player] scope (resolved relative to the
     * source's controller) includes [playerId], taking the most restrictive (smallest) value. A
     * [ConditionalStaticAbility] wrapper is unwrapped and its condition evaluated against the
     * source's controller, so "as long as …" gates (Winter's Delirium) are honored.
     */
    private fun maximumHandSize(state: GameState, playerId: EntityId): Int? {
        if (hasNoMaximumHandSize(state, playerId)) return null
        var max = defaultMaxHandSize
        val registry = cardRegistry
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.isEmpty()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val context = EffectContext(sourceId = permanentId, controllerId = controllerId)
            for (raw in cardDef.script.staticAbilities) {
                val setAbility = activeSetMaximumHandSize(state, raw, context) ?: continue
                if (!playerScopeIncludes(setAbility.player, playerId, controllerId)) continue
                val value = dynamicAmountEvaluator.evaluate(state, setAbility.amount, context)
                    .coerceAtLeast(0)
                max = minOf(max, value)
            }
        }
        return max
    }

    /**
     * Unwrap [raw] to a live [SetMaximumHandSize] if it is one (directly or behind a
     * [ConditionalStaticAbility] whose condition holds), else `null`.
     */
    private fun activeSetMaximumHandSize(
        state: GameState,
        raw: com.wingedsheep.sdk.scripting.StaticAbility,
        context: EffectContext
    ): SetMaximumHandSize? = when (raw) {
        is SetMaximumHandSize -> raw
        is ConditionalStaticAbility -> {
            val inner = raw.ability as? SetMaximumHandSize
            if (inner != null && conditionEvaluator.evaluate(state, raw.condition, context)) inner else null
        }
        else -> null
    }

    /**
     * Whether a [SetMaximumHandSize.player] scope, resolved relative to [controllerId] (the
     * source's controller), includes [playerId]. Mirrors the player-scope switch in
     * [com.wingedsheep.engine.handlers.effects.DamageUtils.isLifeGainPrevented].
     */
    private fun playerScopeIncludes(scope: Player, playerId: EntityId, controllerId: EntityId): Boolean =
        when (scope) {
            Player.You -> playerId == controllerId
            Player.EachOpponent -> playerId != controllerId
            Player.Each, Player.Any, Player.ActivePlayerFirst -> true
            else -> false
        }

    /**
     * Check if [playerId] has no maximum hand size — either from a permanent they control with the
     * [NoMaximumHandSize] static ability (Thought Vessel, Reliquary Tower) or from a player-scoped
     * rest-of-game effect ([PlayerNoMaximumHandSizeComponent], conferred by Wisdom of Ages).
     */
    private fun hasNoMaximumHandSize(state: GameState, playerId: EntityId): Boolean {
        if (state.getEntity(playerId)?.has<PlayerNoMaximumHandSizeComponent>() == true) {
            return true
        }
        val registry = cardRegistry
        val projected = state.projectedState
        for (permanentId in projected.getBattlefieldControlledBy(playerId)) {
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is NoMaximumHandSize }) {
                return true
            }
        }
        return false
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

        // 2. Empty mana pools for all players (unless prevented by a static ability like Upwelling)
        if (!isManaPoolEmptyingPrevented(newState)) {
            for (playerId in newState.turnOrder) {
                newState = newState.updateEntity(playerId) { container ->
                    val manaPool = container.get<ManaPoolComponent>()
                    if (manaPool != null && !manaPool.isEmpty) {
                        container.with(manaPool.empty())
                    } else {
                        container
                    }
                }
            }
        }

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
                if (result.has<AdditionalCombatPhasesComponent>()) {
                    result = result.without<AdditionalCombatPhasesComponent>()
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
                if (result.has<OpponentCreaturesExiledThisTurnComponent>()) {
                    result = result.without<OpponentCreaturesExiledThisTurnComponent>()
                }
                if (result.has<PlayerDescendedThisTurnComponent>()) {
                    result = result.without<PlayerDescendedThisTurnComponent>()
                }
                if (result.has<PermanentsSacrificedThisTurnComponent>()) {
                    result = result.without<PermanentsSacrificedThisTurnComponent>()
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
}
