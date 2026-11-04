package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Their creatures **marked for a sweep we hold** — [com.wingedsheep.ai.engine.AiProfile.marksBeforeTheSweep].
 *
 * Tetzimoc, Primal Death puts prey counters from hand ({B}, reveal it) and, on entering, destroys
 * each creature its controller's opponents control with a prey counter. A counter moves no number
 * the evaluator reads — not power, not toughness, not a keyword — so marking read as {B} spent on
 * nothing, and with seven mana the AI cast Tetzimoc bare beside a Serra Angel it could have killed.
 *
 * So: on our own seat (we read our own hand and stack, never anyone else's), every card whose
 * entry — or whose resolution, for a spell — sweeps a group of creatures *with a counter* of some
 * kind is a marker; each opposing creature carrying that counter is worth [MARKED_SHARE] of its
 * board value to us, as if already half gone. The share is less than the whole because the sweep
 * still has to be cast; it is more than half because nothing but an answer to the sweeper stands
 * between the mark and the kill. Casting the sweeper then trades the share for the whole kill, so
 * the mark is never priced twice.
 *
 * Each creature counts once however many counters or markers it carries: a second prey counter
 * kills nothing the first did not.
 */
class MarkedForRemoval(private val intents: IntentCatalog) : BoardFeature {

    /** Per definition, by identity: the counter types its entry or resolution sweeps. */
    private val marks: MutableMap<CardDefinition, Set<CounterType>> =
        java.util.Collections.synchronizedMap(java.util.IdentityHashMap())

    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        if (!intents.isEnabled) return 0.0
        val counters = heldMarks(state, playerId)
        if (counters.isEmpty()) return 0.0
        val sides = state.sidesFor(playerId) ?: return 0.0
        return sides.opponents.flatten().sumOf { opponent ->
            projected.getBattlefieldControlledBy(opponent).sumOf { id ->
                if (!projected.isCreature(id) || !isMarked(state, id, counters)) return@sumOf 0.0
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@sumOf 0.0
                MARKED_SHARE * BoardPresence.permanentValue(state, projected, id, card, intents)
            }
        }
    }

    private fun isMarked(state: GameState, id: EntityId, counters: Set<CounterType>): Boolean {
        val held = state.getEntity(id)?.get<CountersComponent>() ?: return false
        return counters.any { held.getCount(it) > 0 }
    }

    /** The counter types the sweepers in [playerId]'s hand, or on the stack under their control, destroy. */
    private fun heldMarks(state: GameState, playerId: EntityId): Set<CounterType> {
        val onStack = state.stack.filter { state.getEntity(it)?.get<ControllerComponent>()?.playerId == playerId }
        return (state.getZone(playerId, Zone.HAND) + onStack).flatMapTo(HashSet()) { id ->
            val name = state.getEntity(id)?.get<CardComponent>()?.name ?: return@flatMapTo emptySet()
            val definition = intents.definition(name) ?: return@flatMapTo emptySet()
            marks.getOrPut(definition) { sweptCounters(definition) }
        }
    }

    private fun sweptCounters(definition: CardDefinition): Set<CounterType> {
        val script = definition.script
        val entries = script.triggeredAbilities.filter {
            it.binding == TriggerBinding.SELF && (it.trigger as? EventPattern.ZoneChangeEvent)?.to == Zone.BATTLEFIELD
        }.map { it.effect }
        return (entries + listOfNotNull(script.spellEffect)).flatMapTo(HashSet(), ::sweptCounters)
    }

    /** "Destroy each creature your opponents control with a prey counter on it": the counter types, if [effect] is that sweep. */
    private fun sweptCounters(effect: Effect): Set<CounterType> {
        if (IntentTag.REMOVAL !in CardIntentAnalyzer.analyzeEffect(effect).tags) return emptySet()
        return EffectWalker.payoffLeaves(effect).flatMapTo(HashSet()) { leaf ->
            val filter = ((leaf as? GatherCardsEffect)?.source as? CardSource.BattlefieldMatching)?.filter
                ?.takeIf { it.controllerPredicate != ControllerPredicate.ControlledByYou }
                ?: return@flatMapTo emptySet()
            filter.statePredicates.filterIsInstance<StatePredicate.HasCounter>().map { it.counterType }
        }
    }

    companion object {
        /** The share of a marked creature's board value the mark already claims; see the class doc. */
        const val MARKED_SHARE = 0.75
    }
}
