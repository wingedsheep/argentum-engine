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
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateChosenTokenEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateChosenTokenEffect.
 * Creates a creature token using the chosen color and creature type from the source permanent,
 * with dynamic power/toughness evaluated at resolution time.
 */
class CreateChosenTokenExecutor(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<CreateChosenTokenEffect> {

    override val effectType: KClass<CreateChosenTokenEffect> = CreateChosenTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateChosenTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId ?: return ExecutionResult.success(state)
        val sourceEntity = state.getEntity(sourceId) ?: return ExecutionResult.success(state)

        // Read chosen color and creature type from source
        val chosenColor = sourceEntity.get<ChosenColorComponent>()?.color
        val chosenType = sourceEntity.get<ChosenCreatureTypeComponent>()?.creatureType

        val colors = if (chosenColor != null) setOf(chosenColor) else emptySet()
        val creatureTypes = if (chosenType != null) setOf(chosenType) else setOf("Creature")

        // Evaluate dynamic P/T
        val power = dynamicAmountEvaluator.evaluate(state, effect.dynamicPower, context)
        val toughness = dynamicAmountEvaluator.evaluate(state, effect.dynamicToughness, context)

        val tokenId = EntityId.generate()
        val tokenName = "${creatureTypes.joinToString(" ")} Token"
        val tokenComponent = CardComponent(
            cardDefinitionId = "token:$tokenName",
            name = tokenName,
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - ${creatureTypes.joinToString(" ")}"),
            baseStats = CreatureStats(power, toughness),
            colors = colors,
            ownerId = context.controllerId
        )

        val container = ComponentContainer.of(
            tokenComponent,
            TokenComponent,
            ControllerComponent(context.controllerId),
            SummoningSicknessComponent
        )

        var newState = state.withEntity(tokenId, container)
        val battlefieldZone = ZoneKey(context.controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, tokenId)

        return ExecutionResult.success(newState)
    }
}
