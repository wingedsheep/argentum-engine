package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectEntryChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry
import com.wingedsheep.engine.mechanics.RiotSynthesis
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The "as this enters, choose …" answers prepared for one entrant. [asked] also holds the choices
 * that could not be presented (no other creature to choose), so a replay doesn't ask again.
 *
 * [riotInstances] / [riotModes] are the *granted* Riot answers ("Nontoken creatures you control have
 * riot" — Rhythm of the Wild), kept apart from [values] because a permanent may answer several riot
 * instances (CR 702.136b) and a printed `MODE` choice besides. [riotInstances] is counted once,
 * against the pre-entry board, and `null` until then.
 */
@Serializable
data class EntryChoiceAnswers(
    val asked: Set<ChoiceType> = emptySet(),
    val values: Map<ChoiceSlot, ChoiceValue> = emptyMap(),
    val riotInstances: Int? = null,
    val riotModes: List<String> = emptyList(),
)

/**
 * [EntersWithChoice] for a card an *effect* puts onto the battlefield — a reanimation, a blink's
 * return, a library search. CR 614.12a: the choice is made before the permanent enters, so every
 * entrant's questions are asked against the pre-entry battlefield, recorded on the
 * [EffectContext], and the move instruction is replayed; the zone transition then stamps the
 * answers on arrival ([ZoneEntryOptions.entryChoices]). The same prepare-then-replay shape as
 * [EffectCopyEntry], and it runs after it: an entrant that enters as a copy answers the *copied*
 * card's questions, never its own.
 *
 * After its printed questions, an entrant is asked one counter-or-haste question per instance of
 * Riot that a battlefield lord *grants* it ([RiotSynthesis]) — the same synthesis the spell, token
 * and land entry seams run. The lord's filter is read as the entrant would exist on the battlefield
 * (CR 614.12), i.e. under the controller it enters under. The answers ride
 * [ZoneEntryOptions.grantedRiotModes] and are applied as it arrives.
 */
object EffectEntryChoices {
    fun prepare(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
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
                .orEmpty()
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
                return ask(prompt, effect, context, id, answers)
            }

            val riotInstances = answers.riotInstances
                ?: grantedRiotInstances(state, id, controller, registry, predicateEvaluator)
            answers = answers.copy(riotInstances = riotInstances)
            val owed = riotInstances - answers.riotModes.size
            if (owed > 0) {
                val prompt = PermanentEntryReplacements.entersChoicePrompt(
                    state, id, controller, entering, RiotSynthesis.RIOT_CHOICE, fromZone = null,
                    syntheticRiot = true, syntheticRiotRemaining = owed - 1,
                ) ?: continue
                return ask(prompt, effect, context, id, answers)
            }
        }
        return null
    }

    private fun ask(
        prompt: PermanentEntryReplacements.EntersChoicePrompt,
        effect: Effect,
        context: EffectContext,
        id: EntityId,
        answers: EntryChoiceAnswers,
    ): EffectResult {
        val asking = context.copy(entryChoices = context.entryChoices + (id to answers))
        return EffectResult.from(prompt.state.suspendForDecision(
            prompt.question,
            EffectEntryChoiceContinuation(effect, asking, id, prompt.answer),
            prompt.events,
        ))
    }

    /**
     * Instances of Riot battlefield lords grant [id] as it would enter under [controller]: the lords'
     * "creatures you control" filters must see the controller it enters under, not the one it has
     * in its current zone (a reanimated opponent's creature).
     */
    private fun grantedRiotInstances(
        state: GameState,
        id: EntityId,
        controller: EntityId,
        registry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): Int {
        val asEntering = state.updateEntity(id) { it.with(ControllerComponent(controller)) }
        return RiotSynthesis.grantedRiotInstanceCount(asEntering, id, registry, predicateEvaluator)
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

    /** [context] with one more granted-Riot answer ([modeId]: counter or haste) for [entityId]. */
    fun answeredRiot(context: EffectContext, entityId: EntityId, modeId: String): EffectContext {
        val prior = context.entryChoices[entityId] ?: EntryChoiceAnswers()
        return context.copy(entryChoices = context.entryChoices + (entityId to prior.copy(
            riotModes = prior.riotModes + modeId,
        )))
    }
}
