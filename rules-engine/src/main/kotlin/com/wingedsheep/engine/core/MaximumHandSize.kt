package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.battlefield.chosenPlayer
import com.wingedsheep.engine.state.components.player.PlayerMaximumHandSizeReductionComponent
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyMaximumHandSize
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

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
     * The effective maximum hand size for [playerId], or `null` when they have no maximum.
     *
     * Every effect that sets, modifies or removes a player's maximum hand size is a rules-modifying
     * continuous effect, and CR 613.11 applies those in timestamp order — the latest one wins,
     * whichever kind it is, and a modification adjusts whatever limit stands at its timestamp. So
     * with Spellbook ("no maximum hand size") entering before Twenty-Toed Toad ("your maximum hand
     * size is twenty"), the limit is twenty; the other way round, there is none. The candidates are:
     *
     * - [SetMaximumHandSize] statics on the battlefield whose [SetMaximumHandSize.player] scope
     *   (resolved relative to the source's controller) includes [playerId]. A
     *   [ConditionalStaticAbility] wrapper is unwrapped and its condition evaluated against the
     *   source's controller, so "as long as …" gates (Winter's Delirium) are honored. A set effect
     *   can raise the limit above [DEFAULT] as readily as lower it (Doctor Octopus sets eight).
     * - [NoMaximumHandSize] abilities on the battlefield whose player scope (relative to the
     *   holder's controller) includes [playerId] — printed (Reliquary Tower; Folio of Fancies for
     *   every player) or granted (Nerd Rage) — read from projection by [projectedTimestamps].
     * - The rest-of-game [PlayerNoMaximumHandSizeComponent] (Wisdom of Ages).
     * - [ModifyMaximumHandSize] abilities on the battlefield, read from projection like
     *   [NoMaximumHandSize], whose player scope (relative to the holder's controller) includes
     *   [playerId] (Jin-Gitaxias, Core Augur: each opponent's is reduced by seven). They change
     *   the limit standing at their timestamp; a player with no maximum has nothing to change.
     *
     * A static ability's effect takes its permanent's battlefield-entry timestamp (CR 613.7a).
     * The fold starts from the CR 402.2 default of seven. Player-scoped rest-of-game reductions
     * (Inspired Idea) then apply to a finite limit, and the result is never below 0.
     */
    fun effective(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
        dynamicAmountEvaluator: DynamicAmountEvaluator,
    ): Int? {
        val candidates = mutableListOf<Pair<Long, Op>>()
        state.getEntity(playerId)?.get<PlayerNoMaximumHandSizeComponent>()?.let {
            candidates += it.timestamp to Op.NoMaximum
        }
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val controllerId = projected.getController(permanentId) ?: continue
            val context = EffectContext(sourceId = permanentId, controllerId = controllerId)
            for ((ability, timestamp) in projectedTimestamps(state, permanentId)) {
                when (ability) {
                    is NoMaximumHandSize ->
                        if (playerScopeIncludes(ability.player, playerId, controllerId, state, permanentId)) {
                            candidates += timestamp to Op.NoMaximum
                        }
                    is ModifyMaximumHandSize ->
                        if (playerScopeIncludes(ability.player, playerId, controllerId, state, permanentId)) {
                            candidates += timestamp to Op.Modify(dynamicAmountEvaluator.evaluate(state, ability.amount, context))
                        }
                    else -> Unit
                }
            }
            val container = state.getEntity(permanentId) ?: continue
            val cardDef = container.faceUpCardDefinition(cardRegistry) ?: continue
            if (cardDef.script.staticAbilities.isEmpty()) continue
            val timestamp = entryTimestamp(state, permanentId)
            for (raw in cardDef.script.staticAbilities) {
                val setAbility = activeSetMaximumHandSize(state, raw, context, conditionEvaluator) ?: continue
                if (!playerScopeIncludes(setAbility.player, playerId, controllerId, state, permanentId)) continue
                val value = dynamicAmountEvaluator.evaluate(state, setAbility.amount, context)
                    .coerceAtLeast(0)
                candidates += timestamp to Op.Set(value)
            }
        }
        // Fold in timestamp order (sortedBy is stable, so equal timestamps keep scan order). A
        // modification of "no maximum" leaves no maximum; a later set or removal overrides it.
        var limit: Int? = DEFAULT
        for ((_, op) in candidates.sortedBy { it.first }) {
            limit = when (op) {
                is Op.Set -> op.value
                Op.NoMaximum -> null
                is Op.Modify -> limit?.plus(op.delta)
            }
        }
        return limit?.let { (it - reductionFor(state, playerId)).coerceAtLeast(0) }
    }

    /** One maximum-hand-size effect, applied in timestamp order by [effective]. */
    private sealed interface Op {
        data class Set(val value: Int) : Op
        data object NoMaximum : Op
        data class Modify(val delta: Int) : Op
    }

    /**
     * Every [NoMaximumHandSize] and [ModifyMaximumHandSize] [permanentId] has, paired with its
     * timestamp, read from its projected static abilities — so a printed one (Reliquary Tower; a
     * Room face only while its door is unlocked, CR 709.5), one gained from a resolved effect, and
     * one granted by a [com.wingedsheep.sdk.scripting.GrantStaticAbility] (Nerd Rage's enchanted
     * creature) all count, and none survives the permanent losing all its abilities. Each is dated
     * by the later of the permanent's own timestamp and that of the effect that gave it the
     * ability (CR 613.7a): the granting permanent, or a resolved grant's own timestamp.
     */
    private fun projectedTimestamps(state: GameState, permanentId: EntityId): List<Pair<StaticAbility, Long>> {
        val projected = state.projectedState
        val abilities = projected.getStaticAbilities(permanentId)
        if (abilities.none { it.isHandSizeStatic() }) return emptyList()
        val sourceIds = projected.getStaticAbilitySourceIds(permanentId)
        val holderTimestamp = entryTimestamp(state, permanentId)
        return abilities.indices.filter { abilities[it].isHandSizeStatic() }.map { index ->
            val ability = abilities[index]
            val sourceId = sourceIds.getOrNull(index) ?: permanentId
            val timestamp = if (sourceId == permanentId) {
                // Only grants projection itself applied (same filters as StateProjector's
                // resolved-grant pass), so an expired grant can't re-date a printed instance.
                val resolvedGrantTimestamp = state.grantedStaticAbilities
                    .filter {
                        it.entityId == permanentId && it.ability == ability && it.layerTimestamp != null &&
                            GrantDurationGate.holds(state, permanentId, it.sourceId, it.duration)
                    }
                    .maxOfOrNull { it.layerTimestamp!! } ?: 0L
                maxOf(holderTimestamp, resolvedGrantTimestamp)
            } else {
                maxOf(holderTimestamp, entryTimestamp(state, sourceId))
            }
            ability to timestamp
        }
    }

    private fun StaticAbility.isHandSizeStatic(): Boolean = this is NoMaximumHandSize || this is ModifyMaximumHandSize

    private fun entryTimestamp(state: GameState, permanentId: EntityId): Long =
        state.getEntity(permanentId)?.get<BattlefieldEntryTimestampComponent>()?.timestamp ?: 0L

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
            Player.Each, Player.Any, Player.ActivePlayerFirst, Player.EachStartingWithYou,
            Player.EachStartingWithNextOpponent -> true
            // The opponent durably chosen as the source entered (Cursed Rack), read from the
            // source's CastChoicesComponent[OPPONENT].
            Player.ChosenOpponent -> state.getEntity(sourceId)?.chosenOpponent() == playerId
            Player.ChosenPlayer -> state.getEntity(sourceId)?.chosenPlayer() == playerId
            else -> false
        }
}
