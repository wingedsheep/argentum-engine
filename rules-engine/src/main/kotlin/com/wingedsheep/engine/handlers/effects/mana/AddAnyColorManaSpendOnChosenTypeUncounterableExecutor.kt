package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeUncounterableEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import kotlin.reflect.KClass

/**
 * Executor for [AddAnyColorManaSpendOnChosenTypeUncounterableEffect].
 *
 * Reads the source permanent's [ChosenCreatureTypeComponent] and adds mana of the
 * player's chosen color with a [ManaRestriction.CreatureSubtypeUncounterableOnly]
 * restriction baked to that specific subtype.
 * If no creature type has been chosen on the source, no mana is produced.
 */
class AddAnyColorManaSpendOnChosenTypeUncounterableExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddAnyColorManaSpendOnChosenTypeUncounterableEffect> {

    override val effectType: KClass<AddAnyColorManaSpendOnChosenTypeUncounterableEffect> =
        AddAnyColorManaSpendOnChosenTypeUncounterableEffect::class

    override fun execute(
        state: GameState,
        effect: AddAnyColorManaSpendOnChosenTypeUncounterableEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val chosenType = state.getEntity(sourceId)
            ?.get<ChosenCreatureTypeComponent>()?.creatureType
            ?: return EffectResult.success(state)

        val color = context.manaColorChoice ?: Color.GREEN
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val restriction = ManaRestriction.CreatureSubtypeUncounterableOnly(chosenType)
        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.addRestricted(color, amount, restriction))
        }
        return EffectResult.success(newState)
    }
}
