package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChangeSpellTargetContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Where a "change the target of target spell" redirect points, read through what the redirected
 * spell or ability does to the slot it changes — [ChangeSpellTargetContinuation]'s two questions:
 *
 * - **"Choose a new target"** (Bolt Bend, Misdirection, Reroute): a slot the redirected object
 *   harms takes an opposing object, the most valuable first; one it helps takes one of ours. Only
 *   when every legal new target is on the wrong side does the choice fall back to the least loss —
 *   our own life total before a permanent, then our cheapest permanent.
 * - **"Choose the target to change"** (Spellskite on a spell with several targets, the new target
 *   fixed): take the slot whose current target loses the most by keeping it — our most valuable
 *   object under a harmful slot, an opponent's under a helpful one.
 *
 * Polarity is the target's own side of the effect ("destroy target creature" is harmful to
 * whichever creature it hits), so the reading holds whoever controls the redirected object. An
 * unread or two-sided slot answers null, and the caller keeps its generic ranking.
 */
internal class SpellRedirectChoice(
    private val cardRegistry: CardRegistry,
    private val intents: IntentCatalog,
) {

    fun choose(
        state: GameState,
        decision: SelectCardsDecision,
        playerId: EntityId,
        redirect: ChangeSpellTargetContinuation,
    ): List<EntityId>? {
        if (decision.options.isEmpty() || decision.maxSelections < 1) return null
        val polarities = slotPolarities(state, redirect.spellEntityId) ?: return null
        val targets = state.getEntity(redirect.spellEntityId)?.get<TargetsComponent>()?.targets ?: return null
        return if (redirect.fixedNewTarget == null) {
            // A chosen new target exists only for an object with exactly one target.
            val polarity = polarities.singleOrNull()?.takeIf { it.isOneSided() } ?: return null
            listOf(newTarget(state, decision.options, playerId, polarity))
        } else {
            targetToChange(state, decision.options, playerId, polarities, targets.map { it.entityId() })
                ?.let { listOf(it) }
        }
    }

    /** Per-slot polarity of the spell or ability on the stack at [stackId], or null when unread. */
    internal fun slotPolarities(state: GameState, stackId: EntityId): List<TargetPolarity>? {
        val container = state.getEntity(stackId) ?: return null
        val requirements = container.get<TargetsComponent>()?.targetRequirements?.takeIf { it.isNotEmpty() }
            ?: return null
        val effect: Effect = container.get<ActivatedAbilityOnStackComponent>()?.effect
            ?: container.get<TriggeredAbilityOnStackComponent>()?.effect
            ?: container.get<SpellOnStackComponent>()?.let {
                val definition = container.get<CardComponent>()?.let { card -> cardRegistry.getCard(card.cardDefinitionId) }
                // A split card or modal face: which half is on the stack isn't this reading's to know.
                definition?.takeIf { d -> d.cardFaces.isEmpty() }?.script?.spellEffect
            }
            ?: return null
        return TargetPolarityAnalyzer.forRequirements(effect, requirements).takeIf { it.size == requirements.size }
    }

    private fun newTarget(
        state: GameState,
        options: List<EntityId>,
        playerId: EntityId,
        polarity: TargetPolarity,
    ): EntityId {
        val wanted = options.filter { isOurs(state, it, playerId) == (polarity == TargetPolarity.BENEFICIAL) }
        if (wanted.isNotEmpty()) {
            // Permanents before players: a harmful slot would rather cost them a card than life.
            return wanted.maxWith(compareBy<EntityId> { it !in state.turnOrder }.thenBy { value(state, it) })
        }
        // Every option is on the wrong side: give away the least.
        return options.minWith(compareBy<EntityId> { it !in state.turnOrder }.thenBy { value(state, it) })
    }

    private fun targetToChange(
        state: GameState,
        options: List<EntityId>,
        playerId: EntityId,
        polarities: List<TargetPolarity>,
        currentTargets: List<EntityId>,
    ): EntityId? = options.filter { option ->
        val slot = currentTargets.indexOf(option)
        val polarity = polarities.getOrNull(slot) ?: return@filter false
        val ours = isOurs(state, option, playerId)
        (polarity == TargetPolarity.HARMFUL && ours) || (polarity == TargetPolarity.BENEFICIAL && !ours)
    }.maxByOrNull { value(state, it) + if (it in state.turnOrder) PLAYER_VALUE else 0.0 }

    private fun isOurs(state: GameState, id: EntityId, playerId: EntityId): Boolean =
        if (id in state.turnOrder) id == playerId else state.projectedState.getController(id) == playerId

    private fun value(state: GameState, id: EntityId): Double {
        val card = state.getEntity(id)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, state.projectedState, id, card, intents)
    }

    private fun TargetPolarity.isOneSided() = this == TargetPolarity.HARMFUL || this == TargetPolarity.BENEFICIAL

    private fun ChosenTarget.entityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }

    private companion object {
        /** A player under a harmful slot weighs as a solid permanent: their life is worth guarding. */
        const val PLAYER_VALUE = 3.0
    }
}
