package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import kotlin.reflect.KClass

/**
 * Creates a named Aura token attached to the target object — a Role (Cursed Role, Wicked Role) or
 * any other predefined Aura token (Fallout's Settlement).
 *
 * CR 303.4i: an Aura token whose object is undefined (an "up to one target" left unchosen) or that
 * it can't legally enchant isn't created at all. Legality is the Aura's own enchant restriction plus
 * protection, read by [EnchantRestriction.couldAttach] against the token as it would exist.
 *
 * Role replacement (CR 303.7a): when the new token is a Role and the object already has a Role
 * controlled by the same player, the existing Role is put into the graveyard as the new one enters.
 */
class CreateAuraTokenExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateAuraTokenEffect> {

    override val effectType: KClass<CreateAuraTokenEffect> = CreateAuraTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateAuraTokenEffect,
        context: EffectContext
    ): EffectResult {
        val cardDef = cardRegistry.getCard(effect.tokenName)
            ?: return EffectResult.error(state, "No CardDefinition registered for Aura token '${effect.tokenName}'")

        // No object to attach to: the token isn't created (CR 303.4i).
        val hostId = context.resolveTarget(effect.target, state)
            ?.takeIf { it in state.getBattlefield() }
            ?: return EffectResult.success(state)

        val tokenControllerId = context.controllerId
        val tokenComponent = CardComponent(
            cardDefinitionId = effect.tokenName,
            name = effect.tokenName,
            manaCost = ManaCost.ZERO,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            colors = cardDef.colorIdentityOverride ?: cardDef.colors,
            ownerId = tokenControllerId,
            imageUri = cardDef.metadata.imageUri
        )

        // The would-be token, judged off the battlefield: an object it can't legally enchant means
        // no token at all (CR 303.4i), so the unchanged state is returned.
        val (tokenId, stateWithId) = state.newEntity()
        val probe = stateWithId.withEntity(tokenId, ComponentContainer.of(tokenComponent, ControllerComponent(tokenControllerId)))
        val legal = EnchantRestriction.couldAttach(
            probe, probe.projectedState, zones.predicateEvaluator, cardRegistry,
            tokenId, tokenComponent, hostId, tokenControllerId
        )
        if (!legal) return EffectResult.success(state)

        val events = mutableListOf<GameEvent>()
        var newState = stateWithId

        if (cardDef.typeLine.isRole) {
            for (permanentId in newState.getBattlefield().toList()) {
                val container = newState.getEntity(permanentId) ?: continue
                val attachedTo = container.get<AttachedToComponent>()?.targetId ?: continue
                if (attachedTo != hostId) continue
                val card = container.get<CardComponent>() ?: continue
                if (!card.typeLine.isRole) continue
                val roleController = container.get<ControllerComponent>()?.playerId ?: continue
                if (roleController != tokenControllerId) continue

                val transitionResult = zones.moveToZone(newState, permanentId, Zone.GRAVEYARD)
                newState = transitionResult.state
                events.addAll(transitionResult.events)
            }
        }

        var container = ComponentContainer.of(
            tokenComponent,
            TokenComponent,
            ControllerComponent(tokenControllerId),
            AttachedToComponent(hostId),
            EnteredThisTurnComponent
        )

        if (staticAbilityHandler != null) {
            container = staticAbilityHandler.addContinuousEffectComponent(container, cardDef)
            container = staticAbilityHandler.addReplacementEffectComponent(container, cardDef)
        }

        newState = newState.withEntity(tokenId, container)

        // Update the host's AttachmentsComponent
        newState = newState.updateEntity(hostId) { c ->
            val existing = c.get<AttachmentsComponent>()
            val updatedIds = (existing?.attachedIds ?: emptyList()) + tokenId
            c.with(AttachmentsComponent(updatedIds))
        }

        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, tokenControllerId, tokenId)

        events.add(
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = effect.tokenName,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId,
                oldObject = null,
                newObject = newState.objectRef(tokenId)
            )
        )

        return EffectResult.success(newState, events)
    }
}
