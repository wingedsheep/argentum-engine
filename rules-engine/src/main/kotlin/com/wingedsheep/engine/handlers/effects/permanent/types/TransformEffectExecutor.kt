package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import kotlin.reflect.KClass

/**
 * Executor for [TransformEffect] (CR 701.28).
 *
 * Swaps the target's [CardComponent] to the opposite face of its [DoubleFacedComponent].
 * Counters, damage, attachments, controller, and timestamp all persist — only the identity
 * characteristics (name, type line, P/T, keywords, colors, oracle text, abilities) change.
 *
 * The card's static abilities' [ContinuousEffectSourceComponent] and
 * [ReplacementEffectSourceComponent] are rebuilt from the new face's definition so that
 * layer projection picks up the new face's static abilities immediately.
 *
 * Emits a [TransformedEvent] so triggered abilities keyed on "when this transforms"
 * fire through the standard trigger pipeline.
 */
class TransformEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<TransformEffect> {

    private val staticAbilityHandler = StaticAbilityHandler(cardRegistry)

    override val effectType: KClass<TransformEffect> = TransformEffect::class

    override fun execute(
        state: GameState,
        effect: TransformEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for transform")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found")

        val dfc = container.get<DoubleFacedComponent>()
            ?: return EffectResult.error(state, "Target is not a double-faced permanent")

        // Rule 712.4a: transforming a DFC flips to the opposite face.
        val nextFace = when (dfc.currentFace) {
            DoubleFacedComponent.Face.FRONT -> DoubleFacedComponent.Face.BACK
            DoubleFacedComponent.Face.BACK -> DoubleFacedComponent.Face.FRONT
        }
        val intoBackFace = nextFace == DoubleFacedComponent.Face.BACK

        val nextDefinitionId = when (nextFace) {
            DoubleFacedComponent.Face.FRONT -> dfc.frontCardDefinitionId
            DoubleFacedComponent.Face.BACK -> dfc.backCardDefinitionId
        }

        val nextCardDef = cardRegistry.getCard(nextDefinitionId)
            ?: return EffectResult.error(state, "Opposite face not registered: $nextDefinitionId")

        val currentCard = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target has no CardComponent")

        val swappedCard = buildCardComponentForFace(currentCard, nextCardDef)

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId

        val newState = state.updateEntity(targetId) { c ->
            var updated = c
                .with(swappedCard)
                .with(dfc.copy(currentFace = nextFace))
                // Strip stale static-ability effect components so the layer projector stops
                // applying the old face's static abilities on the very next projection.
                .without<ContinuousEffectSourceComponent>()
                .without<ReplacementEffectSourceComponent>()

            // Re-register the new face's static and replacement effects.
            updated = staticAbilityHandler.addContinuousEffectComponent(updated, nextCardDef)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated, nextCardDef)
            updated
        }

        return EffectResult.success(
            newState,
            listOf(
                TransformedEvent(
                    entityId = targetId,
                    intoBackFace = intoBackFace,
                    newFaceName = nextCardDef.name,
                    controllerId = controllerId
                )
            )
        )
    }

    /**
     * Build a fresh [CardComponent] for the given face while preserving the permanent's
     * owner identity. This mirrors the wholesale-swap approach used by Clone's copy effect.
     */
    private fun buildCardComponentForFace(
        current: CardComponent,
        face: CardDefinition
    ): CardComponent = CardComponent(
        cardDefinitionId = face.name,
        name = face.name,
        manaCost = face.manaCost,
        typeLine = face.typeLine,
        oracleText = face.oracleText,
        baseStats = face.creatureStats,
        baseKeywords = face.keywords,
        baseFlags = face.flags,
        colors = face.colors,
        ownerId = current.ownerId,
        spellEffect = face.spellEffect,
        imageUri = face.metadata.imageUri ?: current.imageUri
    )
}
