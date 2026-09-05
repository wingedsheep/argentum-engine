package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseOptionEffect.
 *
 * Presents a ChooseOptionDecision with options determined by the OptionType.
 * When the player responds, the chosen value is stored in the pipeline's
 * EffectContext.chosenValues (via ChooseOptionPipelineContinuation) for
 * subsequent effects.
 *
 * For [OptionType.CARD_NAME] ("name a card") the option list is every card name the
 * [cardRegistry] knows about, sorted alphabetically — the client renders a searchable
 * list, so there is no free-text entry. Names that no card uses can't be matched by
 * anything in a zone anyway, so restricting to the registry loses nothing in practice.
 */
class ChooseOptionPipelineExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ChooseOptionEffect> {

    override val effectType: KClass<ChooseOptionEffect> = ChooseOptionEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseOptionEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val allOptions = when (effect.optionType) {
            OptionType.CREATURE_TYPE -> Subtype.ALL_CREATURE_TYPES
            OptionType.COLOR -> listOf("White", "Blue", "Black", "Red", "Green")
            OptionType.BASIC_LAND_TYPE -> Subtype.ALL_BASIC_LAND_TYPES.toList()
            OptionType.CARD_NAME -> cardRegistry.allCardNames().sorted()
        }

        val excludedLower = effect.excludedOptions.map { it.lowercase() }.toSet()
        val options = allOptions.filter { it.lowercase() !in excludedLower }

        val prompt = effect.prompt ?: when (effect.optionType) {
            OptionType.CREATURE_TYPE -> "Choose a creature type"
            OptionType.COLOR -> "Choose a color"
            OptionType.BASIC_LAND_TYPE -> "Choose a basic land type"
            OptionType.CARD_NAME -> "Name a card"
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )

        val continuation = ChooseOptionPipelineContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            storeAs = effect.storeAs,
            options = options
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
