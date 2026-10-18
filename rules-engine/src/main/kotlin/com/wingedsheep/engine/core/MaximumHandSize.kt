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
import com.wingedsheep.engine.state.components.identity.CardComponent
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
     * The effective maximum hand size for [playerId], or `null` when they have no maximum.
     *
     * Every effect that sets a player's maximum hand size or removes it is a rules-modifying
     * continuous effect, and CR 613.11 applies those in timestamp order — the latest one wins,
     * whichever kind it is. So with Spellbook ("no maximum hand size") entering before Twenty-Toed
     * Toad ("your maximum hand size is twenty"), the limit is twenty; the other way round, there is
     * none. The candidates are:
     *
     * - [SetMaximumHandSize] statics on the battlefield whose [SetMaximumHandSize.player] scope
     *   (resolved relative to the source's controller) includes [playerId]. A
     *   [ConditionalStaticAbility] wrapper is unwrapped and its condition evaluated against the
     *   source's controller, so "as long as …" gates (Winter's Delirium) are honored. A set effect
     *   can raise the limit above [DEFAULT] as readily as lower it (Doctor Octopus sets eight).
     * - [NoMaximumHandSize] abilities of permanents [playerId] controls — printed (Reliquary
     *   Tower) or granted (Nerd Rage) — read from projection by [noMaximumTimestamps].
     * - The rest-of-game [PlayerNoMaximumHandSizeComponent] (Wisdom of Ages).
     *
     * A static ability's effect takes its permanent's battlefield-entry timestamp (CR 613.7a).
     * With no candidate the limit is the CR 402.2 default of seven. Player-scoped rest-of-game
     * reductions (Inspired Idea) then apply to a finite limit, never below 0.
     */
    fun effective(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
        dynamicAmountEvaluator: DynamicAmountEvaluator,
    ): Int? {
        // (timestamp, limit) — a null limit is "no maximum hand size".
        val candidates = mutableListOf<Pair<Long, Int?>>()
        state.getEntity(playerId)?.get<PlayerNoMaximumHandSizeComponent>()?.let {
            candidates += it.timestamp to null
        }
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val controllerId = projected.getController(permanentId) ?: continue
            if (controllerId == playerId) noMaximumTimestamps(state, permanentId).forEach { candidates += it to null }
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.isEmpty()) continue
            val timestamp = entryTimestamp(state, permanentId)
            val context = EffectContext(sourceId = permanentId, controllerId = controllerId)
            for (raw in cardDef.script.staticAbilities) {
                val setAbility = activeSetMaximumHandSize(state, raw, context, conditionEvaluator) ?: continue
                if (!playerScopeIncludes(setAbility.player, playerId, controllerId, state, permanentId)) continue
                val value = dynamicAmountEvaluator.evaluate(state, setAbility.amount, context)
                    .coerceAtLeast(0)
                candidates += timestamp to value
            }
        }
        // sortedBy is stable, so two effects sharing a timestamp keep scan order.
        val latest = candidates.sortedBy { it.first }.lastOrNull()
        if (latest != null && latest.second == null) return null
        val max = latest?.second ?: DEFAULT
        val reduction = reductionFor(state, playerId)
        return (max - reduction).coerceAtLeast(0)
    }

    /**
     * The timestamps of every "You have no maximum hand size" [permanentId] has, read from its
     * projected static abilities — so a printed one (Reliquary Tower; a Room face only while its
     * door is unlocked, CR 709.5), one gained from a resolved effect, and one granted by a
     * [com.wingedsheep.sdk.scripting.GrantStaticAbility] (Nerd Rage's enchanted creature) all
     * count, and none survives the permanent losing all its abilities. Each is dated by the later
     * of the permanent's own timestamp and that of the effect that gave it the ability
     * (CR 613.7a): the granting permanent, or a resolved grant's own timestamp.
     */
    private fun noMaximumTimestamps(state: GameState, permanentId: EntityId): List<Long> {
        val projected = state.projectedState
        val abilities = projected.getStaticAbilities(permanentId)
        if (abilities.none { it is NoMaximumHandSize }) return emptyList()
        val sourceIds = projected.getStaticAbilitySourceIds(permanentId)
        val holderTimestamp = entryTimestamp(state, permanentId)
        // Only grants projection itself applied (same filters as StateProjector's resolved-grant
        // pass), so an expired grant can't re-date a printed instance.
        val resolvedGrantTimestamp = state.grantedStaticAbilities
            .filter {
                it.entityId == permanentId && it.ability is NoMaximumHandSize && it.layerTimestamp != null &&
                    GrantDurationGate.holds(state, permanentId, it.sourceId, it.duration)
            }
            .maxOfOrNull { it.layerTimestamp!! } ?: 0L
        return abilities.indices.filter { abilities[it] is NoMaximumHandSize }.map { index ->
            val sourceId = sourceIds.getOrNull(index) ?: permanentId
            if (sourceId == permanentId) maxOf(holderTimestamp, resolvedGrantTimestamp)
            else maxOf(holderTimestamp, entryTimestamp(state, sourceId))
        }
    }

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
            Player.Each, Player.Any, Player.ActivePlayerFirst -> true
            // The opponent durably chosen as the source entered (Cursed Rack), read from the
            // source's CastChoicesComponent[OPPONENT].
            Player.ChosenOpponent -> state.getEntity(sourceId)?.chosenOpponent() == playerId
            Player.ChosenPlayer -> state.getEntity(sourceId)?.chosenPlayer() == playerId
            else -> false
        }
}
