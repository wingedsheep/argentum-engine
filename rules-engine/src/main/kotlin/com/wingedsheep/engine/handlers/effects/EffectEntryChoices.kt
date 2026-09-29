package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectEntryChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The "as this enters, choose …" answers prepared for one entrant. [asked] also holds the choices
 * that could not be presented (no other creature to choose), so a replay doesn't ask again.
 */
@Serializable
data class EntryChoiceAnswers(
    val asked: Set<ChoiceType> = emptySet(),
    val values: Map<ChoiceSlot, ChoiceValue> = emptyMap(),
)

/**
 * [EntersWithChoice] for a card an *effect* puts onto the battlefield — a reanimation, a blink's
 * return, a library search. CR 614.12a: the choice is made before the permanent enters, so every
 * entrant's questions are asked against the pre-entry battlefield, recorded on the
 * [EffectContext], and the move instruction is replayed; the zone transition then stamps the
 * answers on arrival ([ZoneEntryOptions.entryChoices]). The same prepare-then-replay shape as
 * [EffectCopyEntry], and it runs after it: an entrant that enters as a copy answers the *copied*
 * card's questions, never its own.
 */
object EffectEntryChoices {
    fun prepare(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
    ): EffectResult? {
        val playerOrder = state.apnapOrder.withIndex().associate { it.value to it.index }
        for ((id, controller) in entrants.entries.sortedBy { playerOrder[it.value] ?: Int.MAX_VALUE }) {
            if (id in state.getBattlefield()) continue
            val entering = context.entryCopies[id]
                ?.let { EffectCopyEntry.apply(state, id, it).getEntity(id)?.get<CardComponent>() }
                ?: state.getEntity(id)?.get<CardComponent>()
                ?: continue
            val choices = registry.getCard(entering.cardDefinitionId)?.script?.replacementEffects
                ?.filterIsInstance<EntersWithChoice>()?.sortedBy { it.choiceType.ordinal }
                ?: continue
            var answers = context.entryChoices[id] ?: EntryChoiceAnswers()
            for (choice in choices) {
                if (choice.choiceType in answers.asked) continue
                val prompt = PermanentEntryReplacements.entersChoicePrompt(
                    state, id, controller, entering, choice, fromZone = null,
                    cardNameOptions = if (choice.choiceType == ChoiceType.CARD_NAME) {
                        registry.cardNamesIn(choice.cardNamePool).toList()
                    } else emptyList(),
                )
                if (prompt == null) {
                    answers = answers.copy(asked = answers.asked + choice.choiceType)
                    continue
                }
                val asking = context.copy(entryChoices = context.entryChoices + (id to answers))
                return EffectResult.from(prompt.state.suspendForDecision(
                    prompt.question,
                    EffectEntryChoiceContinuation(effect, asking, id, prompt.answer),
                    prompt.events,
                ))
            }
        }
        return null
    }

    /** [context] with one more answer recorded for [entityId]. */
    fun answered(
        context: EffectContext,
        entityId: EntityId,
        choiceType: ChoiceType,
        slot: ChoiceSlot,
        value: ChoiceValue,
    ): EffectContext {
        val prior = context.entryChoices[entityId] ?: EntryChoiceAnswers()
        return context.copy(entryChoices = context.entryChoices + (entityId to prior.copy(
            asked = prior.asked + choiceType, values = prior.values + (slot to value),
        )))
    }
}
