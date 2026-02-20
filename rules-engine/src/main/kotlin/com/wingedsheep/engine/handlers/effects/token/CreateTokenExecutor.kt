package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
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

        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(count) {
            val tokenId = EntityId.generate()
            createdTokens.add(tokenId)

            // Create token entity
            val defaultName = effect.creatureTypes.joinToString(" ")
            val tokenName = effect.name ?: defaultName
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:${effect.creatureTypes.joinToString("-")}",
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - ${effect.creatureTypes.joinToString(" ")}"),
                baseStats = CreatureStats(effect.power, effect.toughness),
                baseKeywords = effect.keywords,
                colors = effect.colors,
                ownerId = context.controllerId,
                imageUri = effect.imageUri
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(context.controllerId),
                SummoningSicknessComponent
            )

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            val battlefieldZone = ZoneKey(context.controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }
}
