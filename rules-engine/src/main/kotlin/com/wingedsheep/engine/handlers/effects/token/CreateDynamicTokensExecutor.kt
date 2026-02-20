package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateDynamicTokensEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateDynamicTokensEffect.
 * Creates tokens where the count is determined dynamically at resolution time.
 */
class CreateDynamicTokensExecutor(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<CreateDynamicTokensEffect> {

    override val effectType: KClass<CreateDynamicTokensEffect> = CreateDynamicTokensEffect::class

    override fun execute(
        state: GameState,
        effect: CreateDynamicTokensEffect,
        context: EffectContext
    ): ExecutionResult {
        val count = dynamicAmountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return ExecutionResult.success(state)

        var newState = state

        repeat(count) {
            val tokenId = EntityId.generate()

            val defaultName = "${effect.creatureTypes.joinToString(" ")} Token"
            val tokenName = effect.name ?: defaultName
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:$tokenName",
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

            val battlefieldZone = ZoneKey(context.controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }
}
