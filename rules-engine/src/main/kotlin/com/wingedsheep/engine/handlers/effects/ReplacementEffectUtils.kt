package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.AffectsFilter
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Modification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.player.CountersLockedThisTurnComponent
import com.wingedsheep.sdk.scripting.CapCounterPlacementThisTurn
import com.wingedsheep.sdk.scripting.CounterPlacementScale
import com.wingedsheep.sdk.scripting.MaxCounters
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.PreventExtraTurns
import com.wingedsheep.sdk.scripting.ScaleCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Utility functions for applying replacement effects that modify game actions
 * before they produce events (counter placement modifiers, extra turn prevention).
 */
object ReplacementEffectUtils {

    /**
     * Check if extra turns are prevented by any PreventExtraTurns replacement effect
     * on the battlefield (e.g., Ugin's Nexus).
     */
    fun isExtraTurnPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            for (effect in replacementComponent.replacementEffects) {
                if (effect is PreventExtraTurns) return true
            }
        }
        return false
    }

    /**
     * Apply every counter-placement replacement to one placement of [count] [counterType] counters
     * on [targetId] and return how many are actually put there — the one chokepoint every path that
     * *puts* counters on a permanent or player goes through (effects, costs, entering with counters,
     * damage dealt as counters, the Saga turn-based action). CR 122.6 makes an object given counters
     * as it enters count as counters being put on it, so the entry paths route here too.
     *
     * Sources, in one pool:
     *  - static [ModifyCounterPlacement] (Hardened Scales, Winding Constrictor) and
     *    [ScaleCounterPlacement] (Doubling Season, Corpsejack Menace) on the battlefield — or, for
     *    an entrant ([replacementSourceState] given), on the battlefield before the entry plus the
     *    entrant's own `Recipient.Self` replacements (CR 614.12);
     *  - temporary, controller-scoped [GameState.activeCounterPlacementModifiers] (Prairie Dog);
     *  - [CapCounterPlacementThisTurn] (Melira) and the recipient's own [MaxCounters] limit
     *    (Rasputin Dreamweaver), always last.
     *
     * **Ordering (CR 616.1).** With several of these applying, the affected object's controller
     * (or the affected player) chooses the order, and the order is observable once additive and
     * doubling effects mix: Winding Constrictor + Corpsejack Menace turn 1 counter into 4 (add,
     * then double) or 3 (double, then add). Rather than ask, the engine applies the order the
     * affected player would pick:
     *  - for a counter kind its recipient wants (+1/+1, loyalty, lore, …) the largest result;
     *  - for a [harmful kind][isHarmfulToRecipient] (-1/-1 and other minus stat counters, poison,
     *    stun, rad) the smallest;
     *    found by trying the orders of the additions, doublings and halvings (Vorinclex) that apply;
     *  - caps last, which is the smallest result either way (Melira's ruling).
     * Once a step brings the count to zero the event no longer exists and nothing later applies.
     * This never depends on battlefield order. The one judgment call is a player who *wants* the
     * minus counters on their own creature (a persist loop) — the engine assumes they don't.
     *
     * @param placerId The player putting the counters — gates [ModifyCounterPlacement.placer] /
     *                 [ScaleCounterPlacement.placer] and the temporary modifiers. When null,
     *                 placer-gated effects do not apply.
     * @param byEffect Whether an *effect* is putting the counters. False for counters put on as a
     *                 cost (CR 118 — a [+N] loyalty cost, blight, "put a counter on this") and by a
     *                 turn-based action (CR 714.3c); [ScaleCounterPlacement.effectsOnly] (Doubling
     *                 Season's "if an effect would put") skips those.
     * @return The modified counter count, never negative.
     */
    fun applyCounterPlacementModifiers(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        count: Int,
        placerId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator,
        replacementSourceState: GameState? = null,
        byEffect: Boolean
    ): Int {
        if (count <= 0) return count
        // A capped recipient can't get more of this kind this turn: the placement doesn't happen.
        if (isCounterPlacementLocked(state, targetId, counterType)) return 0

        val additions = mutableListOf<Int>()
        var doublings = 0
        var halvings = 0

        val sources = replacementSourceState ?: state
        // 1. Static battlefield replacement effects (Hardened Scales, Doubling Season, …).
        val sourceIds = sources.getBattlefield()
        val entrantSelfSource = targetId.takeIf { replacementSourceState != null && it !in sourceIds }
        for (entityId in sourceIds + listOfNotNull(entrantSelfSource)) {
            val selfEntry = entityId == entrantSelfSource
            val container = (if (selfEntry) state else sources).getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            // A stolen source works for its new controller (layer 2): read the projected controller.
            val sourceControllerId = (if (selfEntry) null else sources.projectedState.getController(entityId))
                ?: container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                val counterEvent = when (effect) {
                    is ModifyCounterPlacement -> effect.appliesTo
                    is ScaleCounterPlacement -> effect.appliesTo
                    else -> continue
                }
                if (counterEvent !is com.wingedsheep.sdk.scripting.EventPattern.CounterPlacementEvent) continue
                // An entrant's own replacement applies to its entry only when it affects itself
                // specifically; broad battlefield grants (Doubling Season) are not active yet.
                if (selfEntry && counterEvent.recipient != com.wingedsheep.sdk.scripting.events.Recipient.Self) continue

                // Gate on "If YOU / AN OPPONENT would put..." — the placer relative to the source's controller.
                val placer = when (effect) {
                    is ModifyCounterPlacement -> effect.placer
                    is ScaleCounterPlacement -> effect.placer
                }
                if (!placerMatches(state, placer, placerId, sourceControllerId)) continue
                // "If an EFFECT would put..." — costs and turn-based actions aren't effects.
                if (effect is ScaleCounterPlacement && effect.effectsOnly && !byEffect) continue

                if (counterEvent.counterType != null && counterEvent.counterType != counterType) continue

                // Check recipient filter
                val recipientMatches = matchesRecipient(
                    counterEvent.recipient, state, targetId, entityId, sourceControllerId,
                    predicateEvaluator = predicateEvaluator
                )
                if (!recipientMatches) continue

                when (effect) {
                    is ModifyCounterPlacement -> additions += effect.modifier
                    is ScaleCounterPlacement -> when (effect.scale) {
                        CounterPlacementScale.DOUBLE -> doublings++
                        CounterPlacementScale.HALF_ROUNDED_DOWN -> halvings++
                    }
                }
            }
        }

        // 2. Temporary, duration-scoped, controller-scoped modifiers
        //    (GrantCounterPlacementModifierEffect — e.g. Prairie Dog's {4}{W}).
        //    These have no battlefield source permanent: the effect's stored controllerId IS
        //    the "you" for both the recipient filter ("a creature you control") and the placer
        //    gate (only the controller's own counter placements get the bonus).
        for (modifier in sources.activeCounterPlacementModifiers) {
            if (placerId != modifier.controllerId) continue
            if (modifier.counterType != counterType) continue
            // No battlefield source entity — pass the controller as the "source entity" so
            // Recipient.Self can't spuriously match, and the controller as controllerId.
            val recipientMatches = matchesRecipient(
                modifier.recipient, state, targetId, modifier.controllerId, modifier.controllerId,
                predicateEvaluator = predicateEvaluator
            )
            if (!recipientMatches) continue
            additions += modifier.modifier
        }

        // 3. The affected player's order (CR 616.1) — see the KDoc.
        var modifiedCount = orderedForAffectedPlayer(count, additions, doublings, counterType, halvings)
        if (modifiedCount <= 0) return 0

        // 4. Caps apply last — the order the affected player would pick (Melira's ruling), so a
        //    doubler or an additive modifier can't push a capped placement past the cap.
        val cap = counterPlacementCap(state, targetId, counterType, predicateEvaluator, sources)
        if (cap != null) modifiedCount = minOf(modifiedCount, cap)
        // …and so does a holder's "can't have more than N" limit (MaxCounters): only the counters
        // that fit are put on it, its entry counters included.
        val limit = maxCountersLimit(state, targetId, counterType)
        if (limit != null) {
            val already = state.getEntity(targetId)?.get<CountersComponent>()?.getCount(counterType) ?: 0
            modifiedCount = minOf(modifiedCount, limit - already)
        }

        return modifiedCount.coerceAtLeast(0)
    }

    /**
     * The tightest [MaxCounters] limit on [targetId] for [counterType]. A permanent reads its
     * projected statics (printed, granted, lost with its abilities); an entrant isn't in the
     * battlefield zone yet while its entry counters are placed, so it reads its own printed limits
     * off the continuous-effect component stamped as it became a permanent.
     */
    private fun maxCountersLimit(state: GameState, targetId: EntityId, counterType: CounterType): Int? {
        val statics = if (state.logicalZone(targetId)?.zoneType == Zone.BATTLEFIELD) state.projectedState.getStaticAbilities(targetId)
        else state.getEntity(targetId)?.get<ContinuousEffectSourceComponent>()?.effects.orEmpty()
            .filter { it.affectsFilter == AffectsFilter.Self }
            .mapNotNull { (it.modification as? Modification.GrantStaticAbility)?.ability }
        return statics.filterIsInstance<MaxCounters>().filter { it.counterType == counterType }.minOfOrNull { it.max }
    }

    /**
     * Fold [additions], [doublings] and [halvings] into [count] in the order the affected player
     * picks (CR 616.1): the largest result for a kind they want, the smallest for a
     * [harmful][isHarmfulToRecipient] one. A step that leaves nothing ends the event — no later
     * replacement can bring it back. Without halvings the best order is fixed (the additions that
     * help, then the doublings, then the rest); a halving's best slot depends on the count's parity
     * (Hardened Scales after Vorinclex's halving turns 4 into 3, before it into 2), so the orders
     * are searched — over distinct steps, memoised, as only a handful ever apply at once.
     */
    internal fun orderedForAffectedPlayer(
        count: Int,
        additions: List<Int>,
        doublings: Int,
        counterType: CounterType,
        halvings: Int = 0
    ): Int {
        val minimize = isHarmfulToRecipient(counterType)
        if (halvings == 0) {
            val (before, after) = additions.partition { if (minimize) it < 0 else it > 0 }
            var result = count
            for (add in before) {
                result += add
                if (result <= 0) return 0
            }
            repeat(doublings) { result = com.wingedsheep.engine.core.GameLimits.addClamped(result, result) }
            for (add in after) {
                result += add
                if (result <= 0) return 0
            }
            return result
        }
        // Steps: each distinct addition value, the doubling and the halving, each with a multiplicity.
        val adds = additions.groupingBy { it }.eachCount().entries.map { it.key to it.value }
        val memo = HashMap<Pair<Int, List<Int>>, Int>()
        fun best(current: Int, remaining: List<Int>): Int {
            if (current <= 0) return 0
            if (remaining.all { it == 0 }) return current
            val key = current to remaining
            memo[key]?.let { return it }
            var result: Int? = null
            for (i in remaining.indices) {
                if (remaining[i] == 0) continue
                val next = when {
                    i < adds.size -> current + adds[i].first
                    i == adds.size -> com.wingedsheep.engine.core.GameLimits.addClamped(current, current)
                    else -> current / 2
                }
                val value = best(next, remaining.toMutableList().also { it[i]-- })
                result = when {
                    result == null -> value
                    minimize -> minOf(result, value)
                    else -> maxOf(result, value)
                }
            }
            return result!!.also { memo[key] = it }
        }
        return best(count, adds.map { it.second } + doublings + halvings)
    }

    /**
     * Whether [placerId] stands in the [placer] relation to the replacement's controller — "if
     * counters would be put" ([Player.Any]), "if you would put" ([Player.You]), "if an opponent
     * would put" ([Player.EachOpponent], CR 102.3). An unknown placer satisfies only [Player.Any].
     */
    private fun placerMatches(state: GameState, placer: Player, placerId: EntityId?, sourceControllerId: EntityId): Boolean =
        when (placer) {
            Player.Any -> true
            Player.You -> placerId == sourceControllerId
            Player.EachOpponent -> placerId != null && state.isOpponentOf(placerId, sourceControllerId)
            // Not a relation the placer can stand in (the SDK documents the three above).
            else -> false
        }

    /**
     * Counter kinds the recipient's controller is assumed not to want more of when ordering
     * placement replacements: the minus stat counters (-1/-1, -1/-0, -2/-2, …), poison, stun and rad.
     */
    fun isHarmfulToRecipient(counterType: CounterType): Boolean =
        counterType.name.startsWith("MINUS_") ||
            counterType == CounterType.POISON ||
            counterType == CounterType.STUN ||
            counterType == CounterType.RAD

    /** Whether [targetId] already took a [CapCounterPlacementThisTurn] lock on [counterType] this turn. */
    fun isCounterPlacementLocked(state: GameState, targetId: EntityId, counterType: CounterType): Boolean =
        state.getEntity(targetId)?.get<CountersLockedThisTurnComponent>()?.kinds?.contains(counterType) == true

    /**
     * Record the lock a [CapCounterPlacementThisTurn] replacement leaves behind — "and you can't
     * get additional poison counters this turn". Call it after placing [placed] counters of
     * [counterType] on a player; it stamps the lock only when a cap actually applied to that
     * placement. The lock outlives the cap's source (it's the replacement's result), so it lives on
     * the player and is cleared at end of turn.
     */
    fun recordCounterPlacementLock(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        placed: Int,
        predicateEvaluator: PredicateEvaluator
    ): GameState {
        if (placed <= 0 || targetId !in state.turnOrder) return state
        counterPlacementCap(state, targetId, counterType, predicateEvaluator) ?: return state
        return state.updateEntity(targetId) { container ->
            val existing = container.get<CountersLockedThisTurnComponent>() ?: CountersLockedThisTurnComponent()
            container.with(existing.with(counterType))
        }
    }

    /** The tightest [CapCounterPlacementThisTurn] on the battlefield that covers this placement, or null. */
    private fun counterPlacementCap(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        predicateEvaluator: PredicateEvaluator,
        replacementSourceState: GameState = state
    ): Int? {
        var cap: Int? = null
        for (entityId in replacementSourceState.getBattlefield()) {
            val container = replacementSourceState.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            // "You" is the source's current (projected) controller — a stolen Melira protects its thief.
            val sourceControllerId = replacementSourceState.projectedState.getController(entityId) ?: continue
            for (effect in replacementComponent.replacementEffects) {
                if (effect !is CapCounterPlacementThisTurn) continue
                val counterEvent = effect.appliesTo as? com.wingedsheep.sdk.scripting.EventPattern.CounterPlacementEvent
                    ?: continue
                if (counterEvent.counterType != null && counterEvent.counterType != counterType) continue
                if (!matchesRecipient(
                        counterEvent.recipient, state, targetId, entityId, sourceControllerId,
                        predicateEvaluator = predicateEvaluator
                    )
                ) continue
                cap = minOf(cap ?: effect.amount, effect.amount)
            }
        }
        return cap
    }

    /**
     * The counter-placement side of [PredicateEvaluator.matchesRecipient]. A creature still on its
     * way onto the battlefield (entering with counters, CR 614.12) has no projection entry yet, so
     * the filter reads its own characteristics — as it would exist on the battlefield.
     */
    private fun matchesRecipient(
        recipient: Recipient,
        state: GameState,
        targetId: EntityId,
        sourceEntityId: EntityId,
        sourceControllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = predicateEvaluator.matchesRecipient(
        state, state.projectedState, targetId, recipient,
        PredicateContext(controllerId = sourceControllerId, sourceId = sourceEntityId),
    )
}
