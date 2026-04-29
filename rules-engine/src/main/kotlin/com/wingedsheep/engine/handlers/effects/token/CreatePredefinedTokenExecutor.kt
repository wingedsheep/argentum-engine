package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import kotlin.reflect.KClass

/**
 * Unified executor for all predefined token types (Treasure, Food, Lander, etc.).
 *
 * Looks up the token's CardDefinition from the CardRegistry to read type line, imageUri,
 * and abilities. The CardDefinition name must match [CreatePredefinedTokenEffect.tokenType].
 *
 * To add a new predefined token type, define it in `PredefinedTokens.kt` and add a facade
 * method to `Effects.kt` — no new executor needed.
 */
class CreatePredefinedTokenExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreatePredefinedTokenEffect> {

    override val effectType: KClass<CreatePredefinedTokenEffect> = CreatePredefinedTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreatePredefinedTokenEffect,
        context: EffectContext
    ): EffectResult {
        val cardDef = cardRegistry.getCard(effect.tokenType)
            ?: return EffectResult.error(state, "No CardDefinition registered for predefined token '${effect.tokenType}'")

        val controller = effect.controller
        val tokenControllerId = if (controller != null) {
            context.resolvePlayerTarget(controller, state)
                ?: context.controllerId
        } else {
            context.controllerId
        }

        // Check for token creation replacement effects (e.g., Mirrormind Crown)
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, effect.count, tokenControllerId, cardRegistry, staticAbilityHandler
        )
        if (replacementResult != null) return replacementResult

        var newState = state
        val createdTokenIds = mutableListOf<EntityId>()

        repeat(effect.count) {
            val tokenId = EntityId.generate()
            createdTokenIds.add(tokenId)

            val tokenComponent = CardComponent(
                cardDefinitionId = effect.tokenType,
                name = effect.tokenType,
                manaCost = ManaCost.ZERO,
                typeLine = cardDef.typeLine,
                ownerId = tokenControllerId,
                imageUri = cardDef.metadata.imageUri
            )

            var container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId)
            )

            if (effect.tapped) {
                container = container.with(TappedComponent)
            }

            // Wire up static abilities (e.g., equip bonuses, lord effects) from the
            // predefined token's CardDefinition into a ContinuousEffectSourceComponent
            // so the StateProjector applies them.
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container, cardDef)
                container = staticAbilityHandler.addReplacementEffectComponent(container, cardDef)
            }

            newState = newState.withEntity(tokenId, container)

            val battlefieldZone = ZoneKey(tokenControllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        val events = createdTokenIds.map { tokenId ->
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = effect.tokenType,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
        }

        return EffectResult.success(newState, events)
    }
}
