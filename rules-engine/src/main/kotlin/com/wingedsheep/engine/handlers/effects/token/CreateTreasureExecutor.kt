package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateTreasureTokensEffect

/**
 * Executor for CreateTreasureTokensEffect.
 * Creates Treasure artifact tokens.
 */
class CreateTreasureExecutor : EffectExecutor<CreateTreasureTokensEffect> {

    override fun execute(
        state: GameState,
        effect: CreateTreasureTokensEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state

        repeat(effect.count) {
            val tokenId = EntityId.generate()

            val tokenComponent = CardComponent(
                cardDefinitionId = "token:Treasure",
                name = "Treasure",
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Artifact - Treasure"),
                ownerId = context.controllerId
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(context.controllerId)
            )

            newState = newState.withEntity(tokenId, container)

            val battlefieldZone = ZoneKey(context.controllerId, ZoneType.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }
}
