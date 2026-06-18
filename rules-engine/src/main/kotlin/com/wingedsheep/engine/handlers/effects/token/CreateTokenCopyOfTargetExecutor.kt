package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfTargetEffect.
 *
 * Creates N token copies of a targeted permanent (resolved via EffectTarget).
 * Used for "Create X tokens that are copies of target token you control."
 */
class CreateTokenCopyOfTargetExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null
) : EffectExecutor<CreateTokenCopyOfTargetEffect> {

    override val effectType: KClass<CreateTokenCopyOfTargetEffect> = CreateTokenCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val targetCard = targetContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return EffectResult.success(state)

        val controllerId = context.controllerId

        // Check for token creation replacement effects (e.g., Mirrormind Crown).
        // Mirrormind's replacement copies the equipped creature instead of this
        // effect's intended copy, dropping any added keywords / triggered abilities.
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, count, controllerId, cardRegistry, staticAbilityHandler
        )
        if (replacementResult != null) return replacementResult

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val createdTokens = mutableListOf<EntityId>()

        repeat(com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "target-copy tokens")) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId
            val op = effect.overridePower
            val ot = effect.overrideToughness
            val overrideStats = if (op != null && ot != null) {
                CreatureStats(op, ot)
            } else null
            val extraCardTypes = effect.addCardTypes
                .mapNotNull { name -> runCatching { CardType.valueOf(name.uppercase()) }.getOrNull() }
                .toSet()
            val tokenTypeLine = targetCard.typeLine.copy(
                cardTypes = targetCard.typeLine.cardTypes + extraCardTypes,
                supertypes = targetCard.typeLine.supertypes + effect.addedSupertypes - effect.removedSupertypes,
                subtypes = effect.overrideSubtypes ?: (targetCard.typeLine.subtypes + effect.addedSubtypes)
            )
            val tokenCard = targetCard.copy(
                ownerId = controllerId,
                typeLine = tokenTypeLine,
                baseStats = overrideStats ?: targetCard.baseStats,
                baseKeywords = targetCard.baseKeywords + effect.addedKeywords,
                colors = effect.overrideColors ?: targetCard.colors
            )

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            // Only creatures can be attacking. A copy of a card whose printed type line isn't a
            // creature (e.g. an animated permanent exiled and reverted to its printed type) still
            // enters tapped but never attacking — see Mardu Siegebreaker's rulings.
            if (effect.attacking && tokenCard.typeLine.isCreature) {
                // The token joins the source's attack (CR 802.2a) — see CreateTokenExecutor.
                val defenderId = com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                    .resolveDefendingPlayer(context, newState)
                    ?: newState.getOpponents(controllerId).firstOrNull()
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId))
                }
            }
            // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
            // with the same face up as the source. Counters
            // are intentionally not copied (handled by the absence of CountersComponent copy
            // throughout this executor).
            targetContainer.get<DoubleFacedComponent>()?.let { sourceDfc ->
                components.add(
                    DoubleFacedComponent(
                        frontCardDefinitionId = sourceDfc.frontCardDefinitionId,
                        backCardDefinitionId = sourceDfc.backCardDefinitionId,
                        currentFace = sourceDfc.currentFace
                    )
                )
            }

            var container = ComponentContainer.of(*components.toTypedArray())

            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)
            createdTokens.add(tokenId)

            for (ability in effect.triggeredAbilities) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        }

        // If sacrificeAtStep is set, create a delayed trigger to sacrifice each created token
        // copy at that step (e.g. Mardu Siegebreaker: "at the beginning of your next end step,
        // sacrifice those tokens"). Mirrors CreateTokenExecutor's sacrificeAtStep handling.
        val sacrificeStep = effect.sacrificeAtStep
        if (sacrificeStep != null && createdTokens.isNotEmpty()) {
            val sourceId = context.sourceId ?: controllerId
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
            for (tokenId in createdTokens) {
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(tokenId)),
                    fireAtStep = sacrificeStep,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    // "sacrifice at the beginning of your next end step" → gate the firing step
                    // to the token controller's turn (the single fireOnPlayerId gate).
                    fireOnPlayerId = if (effect.sacrificeOnlyOnControllersTurn) controllerId else null
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        return EffectResult.success(newState, events)
    }
}
