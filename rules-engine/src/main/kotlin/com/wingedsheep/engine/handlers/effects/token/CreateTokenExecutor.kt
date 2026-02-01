package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
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
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenEffect.
 * "Create a 1/1 white Soldier creature token"
 */
class CreateTokenExecutor : EffectExecutor<CreateTokenEffect> {

    override val effectType: KClass<CreateTokenEffect> = CreateTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(effect.count) {
            val tokenId = EntityId.generate()
            createdTokens.add(tokenId)

            // Create token entity
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

            // Add to battlefield
            val battlefieldZone = ZoneKey(context.controllerId, ZoneType.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }
}
