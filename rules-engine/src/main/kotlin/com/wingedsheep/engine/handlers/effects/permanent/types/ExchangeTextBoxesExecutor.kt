package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TextBoxesExchangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.exchangeRulesTextFor
import com.wingedsheep.sdk.scripting.effects.ExchangeTextBoxesEffect
import kotlin.reflect.KClass

/**
 * Executor for [ExchangeTextBoxesEffect] (CR 612.5, 701.12h): each permanent's rules text becomes the
 * other's previous rules text, read off both before either changes.
 *
 * Nothing is exchanged (CR 701.12a) unless both sides resolve to two different permanents on the
 * battlefield — a declined or empty choice, a side that left first.
 *
 * Not supported: a face-down side. By the rules (CR 708.2a) it has no text to give and would take
 * the other's, but face-down status here hides the [CardComponent] the exchange rewrites, so the
 * exchange is refused rather than half-done. Cards offering the choice filter face-down
 * permanents out (`GameObjectFilter.faceUp()`), so no player picks one and sees nothing happen.
 */
class ExchangeTextBoxesExecutor(private val cardRegistry: CardRegistry) : EffectExecutor<ExchangeTextBoxesEffect> {

    override val effectType: KClass<ExchangeTextBoxesEffect> = ExchangeTextBoxesEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeTextBoxesEffect,
        context: EffectContext
    ): EffectResult {
        val firstId = context.resolveTarget(effect.first, state) ?: return EffectResult.success(state)
        val secondId = context.resolveTarget(effect.second, state) ?: return EffectResult.success(state)
        if (firstId == secondId) return EffectResult.success(state)
        val battlefield = state.getBattlefield()
        if (firstId !in battlefield || secondId !in battlefield) return EffectResult.success(state)
        val first = state.getEntity(firstId) ?: return EffectResult.success(state)
        val second = state.getEntity(secondId) ?: return EffectResult.success(state)
        if (first.has<FaceDownComponent>() || second.has<FaceDownComponent>()) return EffectResult.success(state)
        val firstText = first.get<CardComponent>() ?: return EffectResult.success(state)
        val secondText = second.get<CardComponent>() ?: return EffectResult.success(state)

        val newState = state
            .updateEntity(firstId) { it.exchangeRulesTextFor(secondText, cardRegistry) }
            .updateEntity(secondId) { it.exchangeRulesTextFor(firstText, cardRegistry) }
        return EffectResult.success(newState, listOf(TextBoxesExchangedEvent(firstId, secondId)))
    }
}
