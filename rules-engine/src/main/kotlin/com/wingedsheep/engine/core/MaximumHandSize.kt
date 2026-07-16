package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.player.PlayerMaximumHandSizeReductionComponent
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The single source of truth for a player's effective maximum hand size (CR 402.2).
 *
 * Used in two places that must agree: the cleanup step ([CleanupPhaseManager]), which discards down
 * to this value, and the client view ([com.wingedsheep.engine.view.ClientStateTransformer]), which
 * surfaces it so the player can see when an effect has changed their limit. Keeping one
 * implementation guarantees the badge the player reads matches the number cleanup enforces.
 *
 * Stateless — the caller supplies the evaluators so projection stays cached on the immutable
 * [GameState] (no evaluator allocation per call).
 */
object MaximumHandSize {

    /** Default maximum hand size (CR 402.2), absent any effect that sets or removes it. */
    const val DEFAULT = 7

    /**
     * The effective maximum hand size for [playerId], or `null` when they have no maximum
     * (Reliquary Tower / Wisdom of Ages — [hasNoMaximum]).
     *
     * Starts from [DEFAULT] (CR 402.2) and applies every [SetMaximumHandSize] static ability on the
     * battlefield whose [SetMaximumHandSize.player] scope (resolved relative to the source's
     * controller) includes [playerId], taking the most restrictive (smallest) value. A
     * [ConditionalStaticAbility] wrapper is unwrapped and its condition evaluated against the
     * source's controller, so "as long as …" gates (Winter's Delirium) are honored.
     */
    fun effective(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
        dynamicAmountEvaluator: DynamicAmountEvaluator,
    ): Int? {
        if (hasNoMaximum(state, playerId, cardRegistry)) return null
        var max = DEFAULT
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.isEmpty()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val context = EffectContext(sourceId = permanentId, controllerId = controllerId)
            for (raw in cardDef.script.staticAbilities) {
                val setAbility = activeSetMaximumHandSize(state, raw, context, conditionEvaluator) ?: continue
                if (!playerScopeIncludes(setAbility.player, playerId, controllerId, state, permanentId)) continue
                val value = dynamicAmountEvaluator.evaluate(state, setAbility.amount, context)
                    .coerceAtLeast(0)
                max = minOf(max, value)
            }
        }
        // Player-scoped rest-of-game reductions (Inspired Idea) apply after the SetMaximumHandSize
        // statics have chosen the most restrictive base, and never below 0. A no-maximum player
        // short-circuited above, so there is always a finite base to reduce here.
        val reduction = reductionFor(state, playerId)
        return (max - reduction).coerceAtLeast(0)
    }

    /**
     * The accumulated rest-of-game maximum-hand-size reduction for [playerId]
     * ([PlayerMaximumHandSizeReductionComponent]), or 0 if none. Conferred by
     * [com.wingedsheep.sdk.scripting.effects.ReduceMaximumHandSizeEffect] (Inspired Idea) and
     * stacked across repeat applications.
     */
    private fun reductionFor(state: GameState, playerId: EntityId): Int =
        state.getEntity(playerId)?.get<PlayerMaximumHandSizeReductionComponent>()?.amount ?: 0

    /**
     * Unwrap [raw] to a live [SetMaximumHandSize] if it is one (directly or behind a
     * [ConditionalStaticAbility] whose condition holds), else `null`.
     */
    private fun activeSetMaximumHandSize(
        state: GameState,
        raw: StaticAbility,
        context: EffectContext,
        conditionEvaluator: ConditionEvaluator,
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
    private fun playerScopeIncludes(
        scope: Player,
        playerId: EntityId,
        controllerId: EntityId,
        state: GameState,
        sourceId: EntityId,
    ): Boolean =
        when (scope) {
            Player.You -> playerId == controllerId
            Player.EachOpponent -> playerId != controllerId
            Player.Each, Player.Any, Player.ActivePlayerFirst -> true
            // The opponent durably chosen as the source entered (Cursed Rack), read from the
            // source's CastChoicesComponent[OPPONENT].
            Player.ChosenOpponent -> state.getEntity(sourceId)?.chosenOpponent() == playerId
            else -> false
        }

    /**
     * Check if [playerId] has no maximum hand size — either from a permanent they control with the
     * [NoMaximumHandSize] static ability (Thought Vessel, Reliquary Tower) or from a player-scoped
     * rest-of-game effect ([PlayerNoMaximumHandSizeComponent], conferred by Wisdom of Ages).
     */
    fun hasNoMaximum(state: GameState, playerId: EntityId, cardRegistry: CardRegistry): Boolean {
        if (state.getEntity(playerId)?.has<PlayerNoMaximumHandSizeComponent>() == true) {
            return true
        }
        val projected = state.projectedState
        for (permanentId in projected.getBattlefieldControlledBy(playerId)) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            // Routed through RoomFaceStatics so a Room face's "You have no maximum hand size"
            // (e.g. Steaming Sauna) counts only while that door is unlocked (CR 709.5).
            if (RoomFaceStatics.activeStaticAbilities(container, cardDef).any { it is NoMaximumHandSize }) {
                return true
            }
        }
        return false
    }
}
