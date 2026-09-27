package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.ChooseColorOrColorlessForProtectionContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.targeting.ColorProtection
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromColorlessOrChosenColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantProtectionFromColorlessOrChosenColorEffect] — "gains protection from colorless
 * or from the color of your choice" (Angelic Intervention).
 *
 * Presents a [ChooseOptionDecision] over Colorless plus the five colors and pushes a
 * [ChooseColorOrColorlessForProtectionContinuation]; the resumer grants the floating
 * `PROTECTION_FROM_<QUALITY>` keyword that [ColorProtection] reads.
 */
class GrantProtectionFromColorlessOrChosenColorExecutor :
    EffectExecutor<GrantProtectionFromColorlessOrChosenColorEffect> {

    override val effectType: KClass<GrantProtectionFromColorlessOrChosenColorEffect> =
        GrantProtectionFromColorlessOrChosenColorEffect::class

    override fun execute(
        state: GameState,
        effect: GrantProtectionFromColorlessOrChosenColorEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state.tick())
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state.tick())
        }

        val qualities = listOf(ColorProtection.COLORLESS) + Color.entries.map { it.name }
        val labels = listOf("Colorless") + Color.entries.map { it.displayName }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose colorless or a color",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = labels
        ) }

        val continuation = ChooseColorOrColorlessForProtectionContinuation(
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            targetId = targetId,
            qualities = qualities,
            duration = effect.duration
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}
