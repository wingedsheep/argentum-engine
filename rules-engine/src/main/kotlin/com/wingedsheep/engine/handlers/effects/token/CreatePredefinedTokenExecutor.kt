package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.TokenArtRegistry
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
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val tokenArtRegistry: TokenArtRegistry? = null
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

        // Art: an explicit per-card override wins, then the art printed by the set the creating
        // card came from (so a reprint mints its own set's Treasure), then the one canonical
        // printing shared engine-wide by PredefinedTokens.
        //
        // A list, because a set may have printed the same token with several illustrations: the
        // batch is dealt out of it in order and wraps. Indexing by position in the batch keeps it
        // deterministic, so a replay re-simulates the same board. See CreateTokenExecutor.
        val sourceCard = context.sourceId
            ?.let { state.getEntity(it) }
            ?.get<CardComponent>()
        val resolvedImageUris = effect.imageUri?.let(::listOf)
            ?: tokenArtRegistry?.resolveAll(
                sourceCardDefinitionId = sourceCard?.cardDefinitionId,
                tokenName = effect.tokenType,
                sourcePrintingSetCode = sourceCard?.printingSetCode,
            )?.takeIf { it.isNotEmpty() }
            ?: listOf(cardDef.metadata.imageUri)

        var newState = state
        val createdTokenIds = mutableListOf<EntityId>()

        repeat(com.wingedsheep.engine.core.GameLimits.cappedTokenCount(tokenCount, "predefined tokens")) { indexInBatch ->
            val resolvedImageUri = resolvedImageUris[indexInBatch % resolvedImageUris.size]
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            createdTokenIds.add(tokenId)

            val tokenComponent = CardComponent(
                cardDefinitionId = effect.tokenType,
                name = effect.tokenType,
                manaCost = ManaCost.ZERO,
                typeLine = cardDef.typeLine,
                baseStats = cardDef.creatureStats,
                baseKeywords = cardDef.keywords,
                // Tokens have no mana cost, so a colored token's printed color lives in its
                // color indicator (CR 204), stored on the definition as colorIdentityOverride.
                // Fall back to the mana-cost-derived colors for tokens without an override.
                colors = cardDef.colorIdentityOverride ?: cardDef.colors,
                ownerId = tokenControllerId,
                imageUri = resolvedImageUri
            )

            var container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )

            if (effect.tapped) {
                container = container.with(TappedComponent)
            }

            // Transforming double-faced tokens (CR 701.51b — Incubator). The token
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

            // Predefined Map/Treasure/Clue/etc. tokens honor global "[filter] enter tapped"
            // replacements (Dauntless Dismantler taps an opponent's artifact token).
            newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .applyCreatedTokenEntryTap(
                    newState, tokenId, tokenControllerId, definedTapped = effect.tapped,
                )
        }

        val events = createdTokenIds.map { tokenId ->
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = effect.tokenType,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
        }.toMutableList<com.wingedsheep.engine.core.GameEvent>()

        // Apply "create those tokens plus an additional X token" replacements (Worldwalker
        // Helm) once for this batch. Only the original tokens are matched against the filter,
        // so the added token can't recursively re-trigger.
        val (afterAdditional, additionalEvents) = TokenCreationReplacementHelper
            .applyAdditionalTokenReplacements(
                newState, tokenControllerId, createdTokenIds, effect.tapped,
                cardRegistry, staticAbilityHandler
            )
        newState = afterAdditional
        events.addAll(additionalEvents)

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
}
