package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateRoleTokenEffect
import kotlin.reflect.KClass

/**
 * Creates a Role token attached to the target creature.
 *
 * Role replacement rule: if the target creature already has a Role token controlled by the
 * same player, the existing Role is put into the graveyard before the new one enters.
 */
class CreateRoleTokenExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateRoleTokenEffect> {

    override val effectType: KClass<CreateRoleTokenEffect> = CreateRoleTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateRoleTokenEffect,
        context: EffectContext
    ): EffectResult {
        val cardDef = cardRegistry.getCard(effect.roleName)
            ?: return EffectResult.error(state, "No CardDefinition registered for role token '${effect.roleName}'")

        val targetCreatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "Cannot resolve target for CreateRoleToken")

        val tokenControllerId = context.controllerId
        val events = mutableListOf<GameEvent>()
        var newState = state

        // Role replacement: if the target already has a Role controlled by this player, exile it
        for (permanentId in newState.getBattlefield().toList()) {
            val container = newState.getEntity(permanentId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()?.targetId ?: continue
            if (attachedTo != targetCreatureId) continue
            val card = container.get<CardComponent>() ?: continue
            if (!card.typeLine.isRole) continue
            val roleController = container.get<ControllerComponent>()?.playerId ?: continue
            if (roleController != tokenControllerId) continue

            // Put existing Role into graveyard
            val transitionResult = ZoneTransitionService.moveToZone(newState, permanentId, Zone.GRAVEYARD)
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        // Create the Role token entity
        val tokenId = EntityId.generate()

        val tokenComponent = CardComponent(
            cardDefinitionId = effect.roleName,
            name = effect.roleName,
            manaCost = ManaCost.ZERO,
            typeLine = cardDef.typeLine,
            ownerId = tokenControllerId,
            imageUri = cardDef.metadata.imageUri
        )

        var container = ComponentContainer.of(
            tokenComponent,
            TokenComponent,
            ControllerComponent(tokenControllerId),
            AttachedToComponent(targetCreatureId),
            EnteredThisTurnComponent
        )

        if (staticAbilityHandler != null) {
            container = staticAbilityHandler.addContinuousEffectComponent(container, cardDef)
            container = staticAbilityHandler.addReplacementEffectComponent(container, cardDef)
        }

        newState = newState.withEntity(tokenId, container)

        // Update the target creature's AttachmentsComponent
        newState = newState.updateEntity(targetCreatureId) { c ->
            val existing = c.get<AttachmentsComponent>()
            val updatedIds = (existing?.attachedIds ?: emptyList()) + tokenId
            c.with(AttachmentsComponent(updatedIds))
        }

        val battlefieldZone = ZoneKey(tokenControllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, tokenId)

        events.add(
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = effect.roleName,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
        )

        return EffectResult.success(newState, events)
    }
}
