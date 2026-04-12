package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import kotlin.reflect.KClass

/**
 * Executor for AddManaOfColorAmongEffect.
 * "{T}: Add one mana of any color among legendary creatures and planeswalkers you control."
 *
 * Examines the controller's battlefield for permanents matching the filter,
 * collects the union of their colors (using projected state), and adds one mana
 * of the chosen color. If no colors are available, no mana is produced.
 */
class AddManaOfColorAmongExecutor : EffectExecutor<AddManaOfColorAmongEffect> {

    override val effectType: KClass<AddManaOfColorAmongEffect> = AddManaOfColorAmongEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaOfColorAmongEffect,
        context: EffectContext
    ): EffectResult {
        val projected = state.projectedState
        val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, effect.filter, context)

        // Collect colors from matching permanents
        val availableColors = mutableSetOf<Color>()
        for (entityId in matched) {
            val colors = projected.getColors(entityId)
            for (colorName in colors) {
                Color.entries.find { it.name == colorName }?.let { availableColors.add(it) }
            }
        }

        if (availableColors.isEmpty()) {
            return EffectResult.success(state)
        }

        // Use the chosen color, defaulting to the first available color
        val chosenColor = context.manaColorChoice?.takeIf { it in availableColors }
            ?: availableColors.first()

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = if (effect.restriction != null) {
                manaPool.addRestricted(chosenColor, 1, effect.restriction!!)
            } else {
                manaPool.add(chosenColor, 1)
            }
            container.with(updatedPool)
        }

        return EffectResult.success(newState)
    }
}
