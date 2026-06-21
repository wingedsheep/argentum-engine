package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionResult
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import kotlin.reflect.KClass

/**
 * Executor for [TransformEffect] (CR 701.27).
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
            ?: return EffectResult.success(state)

        // Rule 701.27a: transforming a DFC flips to the opposite face.
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

        val swappedCard = buildCardComponentForDfcFace(currentCard, nextCardDef)

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId

        // Rule 712.8a: save the front face card so ZoneTransitionService can restore it
        // without a registry lookup when the DFC leaves the battlefield on its back face.
        val updatedDfc = if (intoBackFace) {
            dfc.copy(currentFace = nextFace, frontFaceCard = currentCard)
        } else {
            dfc.copy(currentFace = nextFace, frontFaceCard = null)
        }

        val newState = state.updateEntity(targetId) { c ->
            var updated = c
                .with(swappedCard)
                .with(updatedDfc)
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

}

/**
 * Build a fresh [CardComponent] for the given DFC face while preserving the permanent's
 * owner identity and inheriting the prior face's `imageUri` when the new face doesn't
 * declare its own. Shared by [TransformEffectExecutor] (CR 701.27 transform on the
 * battlefield) and [ReturnSelfFromExileTransformedExecutor] (CR 702.167 Craft return).
 */
internal fun buildCardComponentForDfcFace(
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

/**
 * Flip a double-faced entity that is currently **in exile** to [destinationFace] and return it to
 * the battlefield as a new object under its owner's control.
 *
 * The face swap is applied while the entity is still in exile so the EXILE → BATTLEFIELD move
 * registers the destination face's static abilities and (for a Saga face) the lore-counter entry
 * setup cleanly. Per Rule 712.8a the front face's [CardComponent] is stashed on the
 * [DoubleFacedComponent] when going to the back face, so the restore-on-leave path can swap back
 * without a registry lookup.
 *
 * Shared by [ReturnSelfFromExileTransformedExecutor] (CR 702.167a Craft return — always to the
 * back face) and [ExileAndReturnTransformedExecutor] (FIN Dominant / eikon — either direction).
 * The caller is responsible for having already moved the entity into exile.
 */
internal fun returnDfcFaceFromExile(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
    destinationFace: DoubleFacedComponent.Face
): ZoneTransitionResult {
    val container = state.getEntity(entityId)
        ?: return ZoneTransitionResult(state, emptyList())
    val dfc = container.get<DoubleFacedComponent>()
        ?: return ZoneTransitionResult(state, emptyList())
    val currentCard = container.get<CardComponent>()
        ?: return ZoneTransitionResult(state, emptyList())
    val ownerId = container.get<OwnerComponent>()?.playerId ?: currentCard.ownerId
        ?: return ZoneTransitionResult(state, emptyList())

    val destinationDefinitionId = when (destinationFace) {
        DoubleFacedComponent.Face.FRONT -> dfc.frontCardDefinitionId
        DoubleFacedComponent.Face.BACK -> dfc.backCardDefinitionId
    }
    val destinationDef = cardRegistry.getCard(destinationDefinitionId)
        ?: return ZoneTransitionResult(state, emptyList())

    val destinationCard = buildCardComponentForDfcFace(currentCard, destinationDef)
    val updatedDfc = when (destinationFace) {
        // currentCard is the front face here (the entity reverts to its front face on leaving the
        // battlefield, Rule 712.8a) — stash it so the back face can restore it on its next exit.
        DoubleFacedComponent.Face.BACK -> dfc.copy(currentFace = destinationFace, frontFaceCard = currentCard)
        DoubleFacedComponent.Face.FRONT -> dfc.copy(currentFace = destinationFace, frontFaceCard = null)
    }

    val prepared = state.updateEntity(entityId) { c -> c.with(destinationCard).with(updatedDfc) }
    return ZoneTransitionService.moveToZone(
        prepared,
        entityId,
        Zone.BATTLEFIELD,
        options = ZoneEntryOptions(controllerId = ownerId)
    )
}
