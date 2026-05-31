package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for [CopyCardIntoCollectionEffect].
 *
 * Resolves the source card, clones its copiable characteristics onto a brand-new entity, and
 * places that copy in the source card's current zone under the effect's controller — exactly
 * what Rule 707.12 requires of "cast a copy of a card" effects ("the copy is created in the
 * same zone the object is in"). The copy's id is published to the pipeline collection named by
 * [CopyCardIntoCollectionEffect.storeAs] so a downstream
 * [com.wingedsheep.engine.handlers.effects.library.CastFromCollectionWithoutPayingCostExecutor]
 * can cast it.
 *
 * The copy carries a [CopyOfComponent] with no pre-copy snapshot (`originalCardComponent = null`),
 * marking it a stack-style copy: when cast, [com.wingedsheep.engine.mechanics.stack.StackResolver]
 * turns a resolving permanent copy into a token and makes an instant/sorcery copy cease to exist
 * (Rule 707.10). A copy that is never cast is swept up by the Rule 707.10a state-based action
 * ([com.wingedsheep.engine.mechanics.sba.zone.PhantomCardCopiesCheck]).
 *
 * No source / source isn't a card / source isn't in a zone → no-op with an empty collection,
 * mirroring the gather/select primitives so the chain degrades gracefully.
 */
class CopyCardIntoCollectionExecutor : EffectExecutor<CopyCardIntoCollectionEffect> {

    override val effectType: KClass<CopyCardIntoCollectionEffect> =
        CopyCardIntoCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: CopyCardIntoCollectionEffect,
        context: EffectContext,
    ): EffectResult {
        val emptyResult = EffectResult.success(state).copy(
            updatedCollections = mapOf(effect.storeAs to emptyList())
        )

        val sourceId = context.resolveTarget(effect.source, state) ?: return emptyResult
        val sourceCard = state.getEntity(sourceId)?.get<CardComponent>() ?: return emptyResult
        val sourceZone = findEntityZone(state, sourceId) ?: return emptyResult

        val controllerId = context.controllerId

        // Clone the copiable characteristics (Rule 707.2). The new copy is owned and controlled
        // by the player casting it, and is created in the same zone type the original is in.
        val copyCard = sourceCard.copy(ownerId = controllerId)
        val container = ComponentContainer.of(
            copyCard,
            OwnerComponent(controllerId),
            ControllerComponent(controllerId),
            CopyOfComponent(
                originalCardDefinitionId = sourceCard.cardDefinitionId,
                copiedCardDefinitionId = sourceCard.cardDefinitionId,
            ),
        )

        val (copyId, stateWithId) = state.newEntity()
        val newState = stateWithId
            .withEntity(copyId, container)
            .addToZone(ZoneKey(controllerId, sourceZone.zoneType), copyId)

        return EffectResult.success(newState).copy(
            updatedCollections = mapOf(effect.storeAs to listOf(copyId))
        )
    }

    private fun findEntityZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) return zoneKey
        }
        return null
    }
}
