package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfEquippedCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfEquippedCreatureEffect.
 * Creates a token that's a copy of the creature equipped by the source equipment.
 *
 * The source must have an AttachedToComponent pointing to a creature.
 * The token copies the creature's CardComponent. Optionally:
 * - Removes legendary supertype if [CreateTokenCopyOfEquippedCreatureEffect.removeLegendary] is true
 * - Grants haste if [CreateTokenCopyOfEquippedCreatureEffect.grantHaste] is true
 */
class CreateTokenCopyOfEquippedCreatureExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenCopyOfEquippedCreatureEffect> {

    override val effectType: KClass<CreateTokenCopyOfEquippedCreatureEffect> =
        CreateTokenCopyOfEquippedCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfEquippedCreatureEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.success(state)

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.success(state)

        // Find the equipped creature via AttachedToComponent
        val attachedTo = sourceContainer.get<AttachedToComponent>()
            ?: return EffectResult.success(state) // Not equipped, do nothing

        val equippedId = attachedTo.targetId
        val equippedContainer = state.getEntity(equippedId)
            ?: return EffectResult.success(state)

        val equippedCard = equippedContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val controllerId = context.controllerId

        var newState = state
        val tokenId = EntityId.generate()

        // Copy the equipped creature's CardComponent
        var tokenCard = equippedCard.copy(ownerId = controllerId)

        // Remove legendary if requested
        if (effect.removeLegendary) {
            val newTypeLine = tokenCard.typeLine.withoutLegendary()
            tokenCard = tokenCard.copy(typeLine = newTypeLine)
        }

        // Grant haste if requested
        if (effect.grantHaste) {
            val newKeywords = tokenCard.baseKeywords + Keyword.HASTE
            tokenCard = tokenCard.copy(baseKeywords = newKeywords)
        }

        val components = mutableListOf<Component>(
            tokenCard,
            TokenComponent,
            ControllerComponent(controllerId),
            SummoningSicknessComponent
        )

        var container = ComponentContainer.of(*components.toTypedArray())

        // Add static abilities from the card definition
        if (staticAbilityHandler != null) {
            container = staticAbilityHandler.addContinuousEffectComponent(container)
            container = staticAbilityHandler.addReplacementEffectComponent(container)
        }

        newState = newState.withEntity(tokenId, container)

        // Add to battlefield
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, tokenId)

        val events = listOf(
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = tokenCard.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = controllerId
            )
        )

        return EffectResult.success(newState, events)
    }
}
