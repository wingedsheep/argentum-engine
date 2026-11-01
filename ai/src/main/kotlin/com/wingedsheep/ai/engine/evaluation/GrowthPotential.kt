package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.serialization.CardSerialization
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * What a permanent's **recurring +1/+1-counter trigger** is still going to add to its controller's
 * board — the part of an Ouroboroid that its current 1/3 body does not show.
 *
 * `creatureValue` reads a creature as it stands: its projected stats, its keywords, the counters
 * already on it. That prices a fresh Ouroboroid exactly like a vanilla 1/3, though every one of its
 * controller's combats will put X +1/+1 counters on each creature they control. The AI therefore
 * spent its removal elsewhere, traded the card away cheaply, and never read "keep it alive" as
 * worth anything.
 *
 * ## The estimate is a simulation, not a reading
 *
 * Rather than a reader per effect shape ("X counters where X is its power", "a counter on each
 * other creature", "double the counters"), the trigger's own effect is **resolved by the engine's
 * executors on a copy of the state**, and the +1/+1 counters it put down are counted off the
 * [CountersAddedEvent]s it emitted. Whatever the effect does — a pipeline that snapshots the
 * source's power, a `ForEach` over the team, a replacement such as Hardened Scales adding one more
 * — the engine already knows, so the estimate cannot drift from the rules.
 *
 * It resolves **three times**, each on the state the last produced, because growth compounds: an
 * Ouroboroid that put one counter on itself puts two on everything next time, and four the time
 * after. The resolutions are weighted [NEXT_TRIGGER_WEIGHT], [SECOND_TRIGGER_WEIGHT] and
 * [THIRD_TRIGGER_WEIGHT] — the next one is likely, each later one needs the permanent to survive
 * another round of removal and combat.
 *
 * The horizon is three rather than two for the decision the estimate exists for. Killing the
 * creature *beside* an Ouroboroid also takes away the counters it would have received, so with
 * only two resolutions an opposing Ouroboroid + Hill Giant still read the 3/3 as the better
 * removal target — though the Giant alone is a 3/3 for the rest of the game, while the Ouroboroid
 * alone is a 4/6 after two combats and an 8/10 after three.
 *
 * Counters landing on an opponent's creature count against the estimate, so a symmetric "put a
 * +1/+1 counter on each creature" is worth what it nets.
 *
 * ## Which triggers
 *
 * Only triggers that will fire again without anyone choosing to set them off, and that need no
 * target to be chosen (an untargeted resolution is the only kind a copy of the state can run
 * faithfully):
 *
 *  - "At the beginning of [step] on **your** turn" ([EventPattern.StepEvent] with [Player.You]) —
 *    fires every one of its controller's turns.
 *  - "Whenever **this creature** attacks" and "whenever you attack" — fire only on a turn its
 *    controller attacks, so they are weighted by [ATTACK_LIKELIHOOD].
 *
 * Those are read off the card definition once per card name and cached; an ability is a candidate
 * only if its effect *mentions* the +1/+1 counter type anywhere in its data (the serialized SDK
 * form is the one traversal that reaches into pipelines, programs and `ForEach` bodies alike), so a
 * draw-a-card upkeep trigger is never simulated. Landfall, "whenever another creature enters" and
 * activated `{T}: put a counter` abilities are left out on purpose: each fires only when its
 * controller does something, which a static estimate cannot schedule.
 *
 * A permanent whose abilities are gone (Humility, Turn to Frog — [ProjectedState.hasLostAllAbilities])
 * has no growth. A simulation that throws counts as none: an estimate must never take the evaluator
 * down with it.
 */
class GrowthPotential private constructor(private val registry: CardRegistry) {

    private val services: EngineServices by lazy { EngineServices(registry) }

    /** Recurring growth triggers of each card name, read once. */
    private val triggersByName = ConcurrentHashMap<String, List<GrowthTrigger>>()

    /**
     * Board value of the growth still to come from the permanent [entityId], whose card is called
     * [cardName]. Zero when it has no recurring +1/+1-counter trigger.
     */
    fun value(state: GameState, projected: ProjectedState, entityId: EntityId, cardName: String): Double {
        val triggers = triggersOf(cardName)
        if (triggers.isEmpty()) return 0.0
        if (projected.hasLostAllAbilities(entityId)) return 0.0
        val controller = projected.getController(entityId) ?: return 0.0
        val fingerprint = boardFingerprint(state, projected)
        val counters = triggers.withIndex().sumOf { (index, trigger) ->
            val key = SimulationKey(entityId, index, cardName, fingerprint)
            val simulated = simulations[key] ?: expectedCounters(state, entityId, controller, trigger.effect).also {
                if (simulations.size >= MAX_CACHED_SIMULATIONS) simulations.clear()
                simulations[key] = it
            }
            trigger.likelihood * simulated
        }
        return (counters * COUNTER_VALUE).coerceIn(-GROWTH_VALUE_CAP, GROWTH_VALUE_CAP)
    }

    /**
     * Simulations already run, by the board they ran on. The evaluator scores thousands of leaves
     * per decision and nearly all of them share the battlefield the growth reads — a leaf where
     * the AI tapped a land or cast an instant has the same Ouroboroid over the same team — so
     * resolving the trigger anew at each one (three executor runs and two re-projections, about a
     * millisecond on a dozen creatures) would make every leaf with a grower on it thirty times as
     * expensive as one without.
     */
    private val simulations = ConcurrentHashMap<SimulationKey, Double>()

    private data class SimulationKey(val sourceId: EntityId, val trigger: Int, val cardName: String, val board: Long)

    /**
     * A 64-bit digest of what a growth trigger's resolution can read: every permanent on the
     * battlefield with its controller, and each creature's projected power and toughness and
     * counters (the amount "X is its power" reads, and the targets "each creature you control"
     * reaches); plus each player's life total, hand and graveyard size, the counts a dynamic
     * "where X is…" amount reads in practice. A digest, not an equality: a collision costs one
     * stale estimate, never a wrong rule.
     */
    private fun boardFingerprint(state: GameState, projected: ProjectedState): Long {
        var h = FNV_OFFSET
        fun mix(value: Long) {
            h = (h xor value) * FNV_PRIME
        }
        for (id in state.getBattlefield()) {
            mix(id.value.hashCode().toLong())
            mix(projected.getController(id)?.value?.hashCode()?.toLong() ?: 0L)
            if (projected.isCreature(id)) {
                mix(projected.getPower(id)?.toLong() ?: 0L)
                mix(projected.getToughness(id)?.toLong() ?: 0L)
                mix(state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE)?.toLong() ?: 0L)
            }
        }
        for (player in state.turnOrder) {
            mix(state.lifeTotal(player).toLong())
            mix(state.getZone(player, Zone.HAND).size.toLong())
            mix(state.getZone(player, Zone.GRAVEYARD).size.toLong())
        }
        return h
    }

    /**
     * The +1/+1 counters [effect] nets [controller], weighted over the next three resolutions. Public
     * to the module so a test can pin the simulation without the value scaling around it.
     */
    internal fun expectedCounters(state: GameState, sourceId: EntityId, controller: EntityId, effect: Effect): Double =
        try {
            var current = state
            var expected = 0.0
            for (weight in TRIGGER_WEIGHTS) {
                val result = services.effectExecutorRegistry.execute(
                    current,
                    effect,
                    EffectContext(sourceId = sourceId, controllerId = controller),
                )
                expected += weight * netCounters(current.projectedState, result.events, controller)
                // A resolution that stopped for a decision or was refused has not finished; its
                // state is not a board the next trigger would see.
                if (result.outcome !is Outcome.Done) break
                if (result.state.getEntity(sourceId) == null) break
                current = result.state
            }
            expected
        } catch (_: RuntimeException) {
            0.0
        }

    private fun netCounters(projected: ProjectedState, events: List<com.wingedsheep.engine.core.GameEvent>, controller: EntityId): Int =
        events.sumOf { event ->
            if (event !is CountersAddedEvent || event.counterType != CounterType.PLUS_ONE_PLUS_ONE) return@sumOf 0
            if (!projected.isCreature(event.entityId)) return@sumOf 0
            if (projected.getController(event.entityId) == controller) event.amount else -event.amount
        }

    internal fun triggersOf(cardName: String): List<GrowthTrigger> =
        triggersByName.getOrPut(cardName) {
            registry.getCard(cardName)?.let(::growthTriggers).orEmpty()
        }

    /** One recurring trigger worth simulating, and how likely it is to fire on a given turn. */
    internal data class GrowthTrigger(val effect: Effect, val likelihood: Double)

    companion object {
        /**
         * What one +1/+1 counter adds to a creature's body — the 1.0 per power and 0.4 per
         * toughness `statBodyValue` pays, so a counter still to come is priced in the same units
         * as one already on the board.
         */
        internal const val COUNTER_VALUE = 1.4

        /** Weight of the next resolution of a recurring trigger. */
        internal const val NEXT_TRIGGER_WEIGHT = 0.75

        /** Weight of the resolution after that — it needs the permanent to survive another round. */
        internal const val SECOND_TRIGGER_WEIGHT = 0.5

        /** Weight of the third resolution — two more rounds of removal and combat survived. */
        internal const val THIRD_TRIGGER_WEIGHT = 0.25

        private val TRIGGER_WEIGHTS = doubleArrayOf(NEXT_TRIGGER_WEIGHT, SECOND_TRIGGER_WEIGHT, THIRD_TRIGGER_WEIGHT)

        /** An attack trigger fires only on a turn its controller attacks. */
        internal const val ATTACK_LIKELIHOOD = 0.5

        /**
         * The most growth one permanent is credited with. An Ouroboroid at power 6 over a board of
         * six creatures simulates to over a hundred counters; past the point where the board is
         * winning anyway, more of it should not out-shout everything else the evaluator weighs.
         */
        internal const val GROWTH_VALUE_CAP = 10.0

        /** Past this many cached simulations the cache starts over; a decision needs a handful. */
        private const val MAX_CACHED_SIMULATIONS = 4096

        private const val FNV_OFFSET = -0x340d631b7bdddcdbL
        private const val FNV_PRIME = 0x100000001b3L

        private val PLUS_ONE_COUNTER_MENTION = "\"${CounterType.PLUS_ONE_PLUS_ONE.name}\""

        private val byRegistry: MutableMap<CardRegistry, GrowthPotential> =
            Collections.synchronizedMap(WeakHashMap())

        /** The reader for [registry], shared by every catalog over it. */
        fun of(registry: CardRegistry): GrowthPotential =
            byRegistry.getOrPut(registry) { GrowthPotential(registry) }

        internal fun growthTriggers(definition: CardDefinition): List<GrowthTrigger> =
            definition.triggeredAbilities.mapNotNull { ability ->
                val likelihood = recurringLikelihood(ability) ?: return@mapNotNull null
                if (ability.targetRequirement != null || ability.additionalTargetRequirements.isNotEmpty()) {
                    return@mapNotNull null
                }
                if (!mentionsPlusOneCounters(ability.effect)) return@mapNotNull null
                GrowthTrigger(ability.effect, likelihood)
            }

        /** How often [ability] fires on its controller's turn, or null when it is not recurring. */
        private fun recurringLikelihood(ability: TriggeredAbility): Double? {
            // A trigger gated by a condition may never fire; it is not growth we can count on.
            if (ability.interveningIf != null || ability.triggerRestriction != null) return null
            // An Aura's or Equipment's trigger is about the permanent it is attached to.
            if (ability.binding == TriggerBinding.ATTACHED) return null
            return when (val pattern = ability.trigger) {
                // A step trigger is bound to no object (`ANY`): "on your turn" is the whole clause.
                is EventPattern.StepEvent -> if (pattern.player == Player.You) 1.0 else null
                // "Whenever this creature attacks" — bound to the permanent itself.
                is EventPattern.AttackEvent ->
                    if (ability.binding == TriggerBinding.SELF && pattern.filter == null && pattern.requires.isEmpty()) {
                        ATTACK_LIKELIHOOD
                    } else {
                        null
                    }
                is EventPattern.YouAttackEvent ->
                    if (pattern.player == Player.You && pattern.attackerFilter == null && pattern.minAttackers <= 1 &&
                        pattern.attackedPlayer == null
                    ) {
                        ATTACK_LIKELIHOOD
                    } else {
                        null
                    }
                else -> null
            }
        }

        private fun mentionsPlusOneCounters(effect: Effect): Boolean =
            try {
                CardSerialization.compactJson.encodeToString(Effect.serializer(), effect)
                    .contains(PLUS_ONE_COUNTER_MENTION)
            } catch (_: kotlinx.serialization.SerializationException) {
                false
            }
    }
}
