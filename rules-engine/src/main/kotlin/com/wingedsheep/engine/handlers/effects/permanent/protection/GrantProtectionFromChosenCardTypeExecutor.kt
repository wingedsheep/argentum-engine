package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.ChooseCardTypeForProtectionContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenCardTypeEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [GrantProtectionFromChosenCardTypeEffect] — "another target creature you control
 * gains protection from the card type of your choice" (Pippin, Guard of the Citadel).
 *
 * The card-type analogue of the chosen-color protection flow: it presents a
 * [ChooseOptionDecision] over the fixed protectable card types and pushes a
 * [ChooseCardTypeForProtectionContinuation]; the resumer grants the floating
 * `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword.
 */
class GrantProtectionFromChosenCardTypeExecutor :
    EffectExecutor<GrantProtectionFromChosenCardTypeEffect> {

    override val effectType: KClass<GrantProtectionFromChosenCardTypeEffect> =
        GrantProtectionFromChosenCardTypeEffect::class

    override fun execute(
        state: GameState,
        effect: GrantProtectionFromChosenCardTypeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state.tick())

        // Target must still be on the battlefield to gain protection.
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state.tick())
        }

        val cardTypes = GrantProtectionFromChosenCardTypeEffect.PROTECTABLE_CARD_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose a card type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = cardTypes
        )

        val continuation = ChooseCardTypeForProtectionContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetId = targetId,
            cardTypes = cardTypes,
            duration = effect.duration
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
