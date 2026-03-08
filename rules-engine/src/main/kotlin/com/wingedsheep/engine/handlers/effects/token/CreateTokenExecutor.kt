package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
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
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenEffect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmountEvaluator].
 */
class CreateTokenExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
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
            EffectExecutorUtils.resolvePlayerTarget(controller, context, state)
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

            val tokenComponent = CardComponent(
                cardDefinitionId = "token:${effect.creatureTypes.joinToString("-")}",
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - ${effect.creatureTypes.joinToString(" ")}"),
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
            val container = ComponentContainer.of(*components.toTypedArray())

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            val battlefieldZone = ZoneKey(tokenControllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
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
