package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ChooseCardTypeForSourceContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseCardTypeForSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseCardTypeForSourceEffect].
 *
 * The controller chooses a card type (CR 205.2a); the choice is written durably onto the source
 * entity's cast-choices bag under the effect's slot ([com.wingedsheep.sdk.scripting.ChoiceSlot
 * .CARD_TYPE]), where [com.wingedsheep.sdk.scripting.predicates.CardPredicate
 * .CardTypeEqualsChosenComponent] reads it at cost-calculation / projection time (Arachne, Psionic
 * Weaver's "spells of the chosen type cost {1} more").
 *
 * When [ChooseCardTypeForSourceEffect.lookAtOpponentHand] is set, the controller first sees an
 * opponent's hand (a durable reveal) before choosing. With a single allowed type the choice is
 * forced and recorded without a prompt; otherwise it pauses for a [ChooseOptionDecision] and the
 * [ChooseCardTypeForSourceContinuation] resumer writes the pick. No source entity → no-op.
 */
class ChooseCardTypeForSourceExecutor : EffectExecutor<ChooseCardTypeForSourceEffect> {

    override val effectType: KClass<ChooseCardTypeForSourceEffect> = ChooseCardTypeForSourceEffect::class

    /** The card types (CR 205.2a) offered when the effect doesn't restrict the set. */
    private val allCardTypes = listOf(
        "Artifact", "Battle", "Creature", "Enchantment", "Instant", "Land", "Planeswalker", "Sorcery"
    )

    override fun execute(
        state: GameState,
        effect: ChooseCardTypeForSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val source = state.getEntity(sourceId) ?: return EffectResult.success(state)

        val options = effect.allowedCardTypes ?: allCardTypes
        if (options.isEmpty()) return EffectResult.success(state)

        // "look at an opponent's hand, then choose …" — a durable reveal to the controller first.
        val (baseState, lookEvents) = if (effect.lookAtOpponentHand) {
            PermanentEntryReplacements.revealOpponentHandForEntersChoice(state, context.controllerId)
        } else state to emptyList()

        // Sole allowed type → forced choice, no prompt.
        if (options.size == 1) {
            val newState = baseState.updateEntity(sourceId) { container ->
                container.withCastChoice(effect.slot, ChoiceValue.TextChoice(options.single()))
            }
            return EffectResult.success(newState, lookEvents)
        }

        val decisionId = "choose-card-type-for-source-${sourceId.value}"
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = effect.prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = source.get<CardComponent>()?.name ?: "Unknown",
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )
        val continuation = ChooseCardTypeForSourceContinuation(
            decisionId = decisionId,
            sourceId = sourceId,
            controllerId = context.controllerId,
            slot = effect.slot,
            cardTypes = options
        )

        return EffectResult.paused(
            baseState.withPendingDecision(decision).pushContinuation(continuation),
            decision,
            lookEvents
        )
    }
}
