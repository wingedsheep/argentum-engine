package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import kotlin.reflect.KClass

/**
 * Executor for TurnFaceUpEffect.
 * "Turn target face-down creature face up."
 *
 * Removes FaceDownComponent from the target creature, turning it face up.
 * Unlike the morph special action (TurnFaceUpHandler), this does not require
 * payment of the morph cost — the spell effect simply flips the creature.
 */
class TurnFaceUpExecutor(
    cardRegistry: CardRegistry
) : EffectExecutor<TurnFaceUpEffect> {

    private val staticAbilityHandler = StaticAbilityHandler(cardRegistry)

    override val effectType: KClass<TurnFaceUpEffect> = TurnFaceUpEffect::class

    override fun execute(
        state: GameState,
        effect: TurnFaceUpEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for turn face up")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found")

        // Already face-up — nothing to do
        if (!container.has<FaceDownComponent>()) {
            return EffectResult.success(state)
        }

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId
        val cardComponent = container.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"

        // CR 701.40g / 701.58g: a manifested or cloaked permanent represented by an instant or
        // sorcery card that *would* turn face up is instead revealed and left face down, and
        // "whenever a permanent is turned face up" abilities don't trigger. Only manifest and
        // cloak can put such a card onto the battlefield in the first place — a morph/disguise
        // face-down permanent is always a creature card.
        val mode = container.get<FaceDownModeComponent>()?.mode
        val isInstantOrSorcery = cardComponent?.typeLine?.let { it.isInstant || it.isSorcery } == true
        if ((mode == FaceDownMode.MANIFEST || mode == FaceDownMode.CLOAK) && isInstantOrSorcery) {
            return EffectResult.success(
                state,
                listOf(
                    CardsRevealedEvent(
                        revealingPlayerId = controllerId,
                        cardIds = listOf(targetId),
                        cardNames = listOf(cardName),
                        imageUris = listOf(cardComponent.imageUri),
                        source = "Turned face up (stays face down — not a creature card)"
                    )
                )
            )
        }

        val newState = state.updateEntity(targetId) { c ->
            // The characteristic-defining effect — and with it disguise's/cloak's ward {2} — ends
            // when the permanent is turned face up (CR 701.40a / 701.58a / 702.168a).
            var updated = c.without<FaceDownComponent>().without<FaceDownModeComponent>()
            updated = staticAbilityHandler.addContinuousEffectComponent(updated)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            updated
        }

        return EffectResult.success(
            newState,
            listOf(TurnFaceUpEvent(targetId, cardName, controllerId))
        )
    }
}
