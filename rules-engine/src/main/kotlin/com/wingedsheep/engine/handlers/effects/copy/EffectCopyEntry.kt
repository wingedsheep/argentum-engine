package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/** A null copied identity records a declined choice without changing the card in its old zone. */
@Serializable
data class EntryCopyChoice(
    val replacement: EntersAsCopy,
    val copiedCard: CardComponent? = null,
    val copiedEntity: EntityId? = null,
)

/** Prepare all choices against the pre-entry battlefield, then replay just the move instruction. */
object EffectCopyEntry {
    fun prepare(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
        evaluator: PredicateEvaluator,
    ): EffectResult? {
        val playerOrder = state.apnapOrder.withIndex().associate { it.value to it.index }
        for ((id, controller) in entrants.entries.sortedBy { playerOrder[it.value] ?: Int.MAX_VALUE }) {
            if (id in context.entryCopies || id in state.getBattlefield()) continue
            val card = state.getEntity(id)?.get<CardComponent>() ?: continue
            val replacement = registry.getCard(card.cardDefinitionId)?.script?.replacementEffects
                ?.filterIsInstance<EntersAsCopy>()?.firstOrNull() ?: continue
            val candidates = PermanentEntryReplacements.entersAsCopyCandidates(
                state, id, controller, replacement, evaluator
            ).filter { candidate ->
                // No mana was spent to cast a permanent arriving by a zone-moving effect.
                !replacement.filterByTotalManaSpent ||
                    (state.getEntity(candidate)?.get<CardComponent>()?.manaValue ?: 0) == 0
            }
            if (candidates.isEmpty()) continue
            val question = { decisionId: String -> SelectCardsDecision(
                id = decisionId, playerId = controller,
                prompt = "Choose a ${replacement.copyFilter.description} for ${card.name} to copy",
                context = DecisionContext(sourceId = id, sourceName = card.name, phase = DecisionPhase.RESOLUTION),
                options = candidates, minSelections = if (replacement.optional) 0 else 1,
                maxSelections = 1, useTargetingUI = replacement.copyFromZone == Zone.BATTLEFIELD,
            ) }
            return EffectResult.from(state.suspendForDecision(question,
                EffectCopyEntryContinuation(effect, context, id, replacement)))
        }
        return null
    }

    /** Install the selected identity only when the zone transition actually reaches the battlefield. */
    fun apply(state: GameState, id: EntityId, choice: EntryCopyChoice): GameState {
        val target = choice.copiedCard ?: return state
        val original = state.getEntity(id)?.get<CardComponent>() ?: return state
        val r = choice.replacement
        val exceptions = r.exceptions.over(com.wingedsheep.sdk.scripting.effects.CopyExceptions(
            nameOverride = r.nameOverride, addedKeywords = r.additionalKeywords.toSet(),
            addedSubtypes = r.additionalSubtypes.map { com.wingedsheep.sdk.core.Subtype(it) }.toSet(),
            addedColors = r.additionalColors, powerOverride = r.powerOverride, toughnessOverride = r.toughnessOverride,
        ))
        val copied = CopyExceptionApplier.apply(target.copy(ownerId = original.ownerId,
            isDoubleFaced = original.isDoubleFaced), exceptions)
        return state.updateEntity(id) { it.with(copied).with(CopyOfComponent(
            originalCardDefinitionId = original.cardDefinitionId,
            copiedCardDefinitionId = target.cardDefinitionId, originalCardComponent = original)) }
    }
}
