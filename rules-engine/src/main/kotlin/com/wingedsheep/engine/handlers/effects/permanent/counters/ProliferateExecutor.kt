package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ProliferateContinuation
import com.wingedsheep.engine.core.ProliferatedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.KeywordActionReplacements
import com.wingedsheep.engine.handlers.effects.ReplaceableKeywordAction
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ProliferateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [ProliferateEffect] — "give each another counter of each kind already there".
 *
 * Two resolutions, one placement rule ([addOneOfEachKind]):
 *
 * - **Untargeted (`effect.target == null`) — proliferate, CR 701.34a.** The recipients are chosen
 *   at resolution:
 *   1. Build the eligible set: every permanent on the battlefield and every player that has at
 *      least one counter of any kind on it.
 *   2. If empty, no counters move — but the player still proliferated (choosing zero), so a
 *      [ProliferatedEvent] is emitted all the same.
 *   3. Otherwise pause with a [SelectCardsDecision] (min=0, max=eligibleEntities.size,
 *      `useTargetingUI=true`) so the controller picks directly on the board.
 *   4. The continuation handler ([ProliferateContinuation]) reads the chosen entities and calls
 *      [addOneOfEachKind], then emits the [ProliferatedEvent] that "whenever you proliferate"
 *      triggers watch — also when the controller chose nothing (the ONE rulings).
 *   A [com.wingedsheep.sdk.scripting.RepeatKeywordAction] ("proliferate twice instead",
 *   Tekuthal) turns this into several proliferates in a row — see [proliferate].
 *
 * - **Targeted (`effect.target != null`).** The recipient was chosen on announcement
 *   (CR 601.2c) and its legality already re-checked on resolution (CR 608.2b), so there is no
 *   decision at all: resolve the target and call [addOneOfEachKind] on it directly. A recipient
 *   that no longer resolves — or that resolves to something which is neither a battlefield
 *   permanent nor a player any more — is a silent no-op.
 *
 * The zone check on the targeted branch is *not* what enforces CR 608.2b for a declared target:
 * `StackResolver` already drops individually-illegal targets before resolution, so a
 * `BoundVariable`/`ContextTarget` naming one resolves to null here. It exists for the
 * [EffectTarget]s that are never rechecked because they aren't targets at all — `Self`,
 * `SpecificEntity`, `TriggeringEntity` — which this effect accepts. Those can name a permanent
 * that has since left the battlefield, and a permanent keeps its `CountersComponent` in the
 * graveyard, so without the check the counters would land on a graveyard object.
 */
class ProliferateExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ProliferateEffect> {

    override val effectType: KClass<ProliferateEffect> = ProliferateEffect::class

    override fun execute(
        state: GameState,
        effect: ProliferateEffect,
        context: EffectContext
    ): EffectResult {
        effect.target?.let { target ->
            // A player recipient ("target permanent or player" resolved to a player, or a
            // PlayerRef) only resolves through the player-resolution path.
            val recipientId = if (target is EffectTarget.PlayerRef) {
                context.resolvePlayerTarget(target, state)
            } else {
                context.resolveTarget(target, state)
            } ?: return EffectResult.success(state, emptyList())

            // Only a permanent still on the battlefield, or a player, can receive them.
            if (recipientId !in state.getBattlefield() && recipientId !in state.turnOrder) {
                return EffectResult.success(state, emptyList())
            }

            val (newState, events) = addOneOfEachKind(state, listOf(recipientId), context.controllerId, predicateEvaluator = predicateEvaluator)
            return EffectResult.success(newState, events)
        }

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Proliferate"

        // "If you would proliferate, proliferate twice instead" (CR 614.1a): the replacement
        // decides how many proliferates this one becomes before the first choice is offered.
        val times = KeywordActionReplacements.repetitions(
            state, context.controllerId, ReplaceableKeywordAction.PROLIFERATE
        )
        return EffectResult.from(
            proliferate(state, context.controllerId, context.sourceId, sourceName, times, emptyList())
        )
    }

    companion object {
        /**
         * Run [times] untargeted proliferates for [controllerId], one after another. Each one
         * gathers its own eligible set — after the previous one's counters have landed — and each
         * emits its own [ProliferatedEvent]. A proliferate with nothing eligible completes on the
         * spot; the first one that has a choice to make pauses, carrying the proliferates still
         * owed on its [ProliferateContinuation] so the resumer can pick the loop back up.
         * [events] are the events already produced by earlier proliferates in the same loop.
         */
        fun proliferate(
            state: GameState,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String,
            times: Int,
            events: List<GameEvent>
        ): ExecutionResult {
            val accumulated = events.toMutableList()
            repeat(times) { done ->
                val eligible = findEntitiesWithCounters(state)
                if (eligible.isEmpty()) {
                    accumulated.add(ProliferatedEvent(controllerId, sourceName))
                    return@repeat
                }

                val decision = { decisionId: String -> SelectCardsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Proliferate — choose any number of permanents and/or players that have a counter",
                    context = DecisionContext(
                        sourceId = sourceId,
                        sourceName = sourceName,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = eligible,
                    minSelections = 0,
                    maxSelections = eligible.size,
                    useTargetingUI = true
                ) }

                val continuation = ProliferateContinuation(
                    controllerId = controllerId,
                    eligibleEntities = eligible,
                    sourceName = sourceName,
                    sourceId = sourceId,
                    proliferatesRemaining = times - done - 1
                )

                return state.suspendForDecision(decision, continuation, accumulated, eventType = "PROLIFERATE")
            }
            return ExecutionResult.success(state, accumulated)
        }

        /**
         * All battlefield permanents + all players that currently have at least one
         * counter of any kind.
         */
        fun findEntitiesWithCounters(state: GameState): List<EntityId> {
            val permanents = state.getBattlefield().filter { entityId ->
                val counters = state.getEntity(entityId)?.get<CountersComponent>()
                counters != null && counters.counters.any { it.value > 0 }
            }
            val players = state.turnOrder.filter { playerId ->
                val counters = state.getEntity(playerId)?.get<CountersComponent>()
                counters != null && counters.counters.any { it.value > 0 }
            }
            return permanents + players
        }

        /**
         * Give each of [recipients] one additional counter of every kind it already has, as
         * [controllerId]. Shared by the targeted branch above and by the untargeted branch's
         * continuation resumer so both forms place counters identically.
         *
         * A recipient with no counters (or that is no longer in the game) is skipped, as is one
         * that can't have counters put on it — `canReceiveCounters`, the same prohibition
         * [com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersExecutor] and
         * every other counter-placing path honors (Blossombind; CR 614.17 "can't" effects). It applies to
         * proliferate exactly as it does to "put a +1/+1 counter on target creature", so both
         * shapes of this effect check it here. Players are never affected: the flag is a keyword
         * grant, and a player id never carries one.
         *
         * Placement goes through [ReplacementEffectUtils.applyCounterPlacementModifiers] (Hardened
         * Scales and friends), is recorded via [DamageUtils.recordCounterPlacement] with the kind
         * and the placer so the counter-history predicates see it, and emits a
         * [CountersAddedEvent] per kind.
         */
        fun addOneOfEachKind(
            state: GameState,
            recipients: List<EntityId>,
            controllerId: EntityId,
            predicateEvaluator: PredicateEvaluator
        ): Pair<GameState, List<GameEvent>> {
            var newState = state
            val events = mutableListOf<GameEvent>()
            // Projected once: the prohibition can't change as a side effect of placing counters,
            // and re-projecting per recipient would be O(recipients) full layer runs.
            val projected = state.projectedState

            for (entityId in recipients) {
                if (!projected.canReceiveCounters(entityId)) continue
                val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: continue
                // Snapshot the kinds present when this recipient is processed, so a counter added
                // here can't feed back into the same recipient's loop.
                val kinds = current.counters.filterValues { it > 0 }.keys.toList()
                if (kinds.isEmpty()) continue

                val entityName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""

                for (counterType in kinds) {
                    val modifiedAmount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                        newState, entityId, counterType, 1, placerId = controllerId,
                        predicateEvaluator = predicateEvaluator
                    )
                    if (modifiedAmount <= 0) continue

                    val before = newState.getEntity(entityId)?.get<CountersComponent>()
                        ?: CountersComponent()
                    newState = newState.updateEntity(entityId) { container ->
                        container.with(before.withAdded(counterType, modifiedAmount))
                    }
                    val (afterMark, firstThisTurn, firstOfTypeThisTurn) = DamageUtils.recordCounterPlacement(
                        newState,
                        entityId,
                        counterType,
                        placerId = controllerId,
                    )
                    newState = afterMark
                    events.add(
                        CountersAddedEvent(
                            entityId,
                            counterType,
                            modifiedAmount,
                            entityName,
                            firstThisTurn, firstOfTypeThisTurn = firstOfTypeThisTurn,
                            placedBy = controllerId
                        )
                    )
                }
            }

            return newState to events
        }
    }
}
