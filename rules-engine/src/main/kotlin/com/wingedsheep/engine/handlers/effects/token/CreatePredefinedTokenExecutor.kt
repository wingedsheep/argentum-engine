package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
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
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
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

        // Evaluate dynamic count if set (e.g. Lobelia's "X = the exiled card's power"),
        // otherwise use the fixed count. Coerced to >= 0 — a negative count would be a
        // bug elsewhere, but clamping defends against odd dynamic-amount edge cases.
        val tokenCount = effect.dynamicCount?.let { dyn ->
            amountEvaluator.evaluate(state, dyn, context).coerceAtLeast(0)
        } ?: effect.count

        // Check for token creation replacement effects (e.g., Mirrormind Crown)
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, tokenCount, tokenControllerId, cardRegistry, staticAbilityHandler
        )
        if (replacementResult != null) return replacementResult

        var newState = state
        val createdTokenIds = mutableListOf<EntityId>()
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        repeat(com.wingedsheep.engine.core.GameLimits.cappedTokenCount(tokenCount, "predefined tokens")) {
            val (placed, tokenId, placeEvents) = placePredefinedToken(
                newState, cardDef, effect.tokenType, tokenControllerId, effect.tapped, staticAbilityHandler
            )
            newState = placed
            createdTokenIds.add(tokenId)
            events.addAll(placeEvents)
        }

        // Apply "create an additional token" replacements (Peregrin Took): if the controller
        // has any matching CreateAdditionalToken effects, create those extra predefined tokens
        // directly (no recursive replacement pass — CR 614.5 self-limiting).
        if (createdTokenIds.isNotEmpty()) {
            val (afterAdditional, additionalEvents) = TokenCreationReplacementHelper.createAdditionalTokens(
                newState, tokenControllerId, cardRegistry, staticAbilityHandler
            )
            newState = afterAdditional
            events.addAll(additionalEvents)
        }

        // Publish the freshly-created token entity IDs to the pipeline so
        // sibling effects in a CompositeEffect can address them via
        // EffectTarget.PipelineTarget(CREATED_TOKENS, index). This is how
        // Effects.Incubate(N) puts +1/+1 counters on the token it just
        // produced without threading an entity reference through a custom executor.
        return EffectResult(
            state = newState,
            events = events,
            updatedCollections = mapOf(CREATED_TOKENS to createdTokenIds)
        )
    }

    companion object {
        /**
         * Create and place a single predefined token on the battlefield, returning the new
         * state, the token's entity id, and the entry [ZoneChangeEvent]. Shared by the
         * normal predefined-token resolution path and the "create an additional token"
         * replacement path ([TokenCreationReplacementHelper.createAdditionalTokens]) so both
         * build the token identically.
         */
        fun placePredefinedToken(
            state: GameState,
            cardDef: com.wingedsheep.sdk.model.CardDefinition,
            tokenType: String,
            tokenControllerId: EntityId,
            tapped: Boolean,
            staticAbilityHandler: StaticAbilityHandler?
        ): Triple<GameState, EntityId, List<com.wingedsheep.engine.core.GameEvent>> {
            val (tokenId, stateWithId) = state.newEntity()
            var newState = stateWithId

            val tokenComponent = CardComponent(
                cardDefinitionId = tokenType,
                name = tokenType,
                manaCost = ManaCost.ZERO,
                typeLine = cardDef.typeLine,
                baseStats = cardDef.creatureStats,
                baseKeywords = cardDef.keywords,
                colors = cardDef.colors,
                ownerId = tokenControllerId,
                imageUri = cardDef.metadata.imageUri
            )

            var container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )

            if (tapped) {
                container = container.with(TappedComponent)
            }

            // Transforming double-faced tokens (CR 701.53b — Incubator). The token
            // enters with its front face up; the back face's CardDefinition is
            // already auto-registered in the CardRegistry by registry.register(...).
            cardDef.backFace?.let { backFace ->
                container = container.with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = DoubleFacedComponent.Face.FRONT
                    )
                )
            }

            // Wire up static abilities (e.g., equip bonuses, lord effects) from the
            // predefined token's CardDefinition into a ContinuousEffectSourceComponent
            // so the StateProjector applies them.
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container, cardDef)
                container = staticAbilityHandler.addReplacementEffectComponent(container, cardDef)
            }

            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, tokenControllerId, tokenId)

            val event = ZoneChangeEvent(
                entityId = tokenId,
                entityName = tokenType,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
            return Triple(newState, tokenId, listOf(event))
        }
    }
}
