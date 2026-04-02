package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenEffect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmountEvaluator].
 */
class CreateTokenExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenEffect> {

    override val effectType: KClass<CreateTokenEffect> = CreateTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return ExecutionResult.success(state)

        // Resolve who receives the token — defaults to spell/ability controller
        val controller = effect.controller
        val tokenControllerId = if (controller != null) {
            TargetResolutionUtils.resolvePlayerTarget(controller, context, state)
                ?: context.controllerId
        } else {
            context.controllerId
        }

        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(count) {
            val tokenId = EntityId.generate()
            createdTokens.add(tokenId)

            // Create token entity
            val defaultName = "${effect.creatureTypes.joinToString(" ")} Token"
            val tokenName = effect.name ?: defaultName
            val tokenPower = effect.dynamicPower?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.power
            val tokenToughness = effect.dynamicToughness?.let { amountEvaluator.evaluate(state, it, context) } ?: effect.toughness

            val typeLinePrefix = buildString {
                if (effect.legendary) append("Legendary ")
                if (effect.artifactToken) append("Artifact ")
                append("Creature")
            }
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:${effect.creatureTypes.joinToString("-")}",
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("$typeLinePrefix - ${effect.creatureTypes.joinToString(" ")}"),
                baseStats = CreatureStats(tokenPower, tokenToughness),
                baseKeywords = effect.keywords,
                colors = effect.colors,
                ownerId = tokenControllerId,
                imageUri = effect.imageUri
            )

            val components = mutableListOf<Component>(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId),
                SummoningSicknessComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            if (effect.attacking) {
                // Token enters attacking — in 2-player games, the defender is the opponent
                val defenderId = newState.getOpponent(tokenControllerId)
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId))
                }
            }
            var container = ComponentContainer.of(*components.toTypedArray())
            if (effect.staticAbilities.isNotEmpty() && staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponentFromAbilities(
                    container, effect.staticAbilities
                )
            }

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            val battlefieldZone = ZoneKey(tokenControllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        // If exileAtStep is set, create delayed triggers to exile each created token
        val exileStep = effect.exileAtStep
        if (exileStep != null) {
            val sourceId = context.sourceId ?: context.controllerId
            val sourceName = sourceId.let { id ->
                state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            for (tokenId in createdTokens) {
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE),
                    fireAtStep = exileStep,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = tokenControllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // If triggered abilities are specified, grant them permanently to each created token
        if (effect.triggeredAbilities.isNotEmpty()) {
            for (tokenId in createdTokens) {
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
            }
        }

        // Prowess is a keyword ability with an intrinsic triggered ability.
        // Grant it automatically when the token has the PROWESS keyword.
        if (Keyword.PROWESS in effect.keywords) {
            val prowessAbility = TriggeredAbility.create(
                trigger = Triggers.YouCastNoncreature.event,
                binding = Triggers.YouCastNoncreature.binding,
                effect = ModifyStatsEffect(
                    powerModifier = 1,
                    toughnessModifier = 1,
                    target = EffectTarget.Self
                )
            )
            for (tokenId in createdTokens) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = prowessAbility,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }
        }

        val events = createdTokens.map { tokenId ->
            val entity = newState.getEntity(tokenId)!!
            val card = entity.get<CardComponent>()!!
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = card.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
