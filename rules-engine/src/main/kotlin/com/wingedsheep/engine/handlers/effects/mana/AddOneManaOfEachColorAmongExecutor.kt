package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddOneManaOfEachColorAmongEffect].
 * "{T}: For each color among permanents you control, add one mana of that color."
 *
 * Unlike [AddManaOfColorAmongExecutor] (which lets the player pick one color), this adds
 * one mana of EVERY color found in the union of matching permanents' colors — simultaneously,
 * no choice. Colors are read from projected state so recolor effects are honored.
 */
class AddOneManaOfEachColorAmongExecutor : EffectExecutor<AddOneManaOfEachColorAmongEffect> {

    override val effectType: KClass<AddOneManaOfEachColorAmongEffect> =
        AddOneManaOfEachColorAmongEffect::class

    override fun execute(
        state: GameState,
        effect: AddOneManaOfEachColorAmongEffect,
        context: EffectContext
    ): EffectResult {
        val projected = state.projectedState
        val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, effect.filter, context)

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

        val newState = state.updateEntity(context.controllerId) { container ->
            var manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            for (color in availableColors) {
                manaPool = if (effect.restriction != null) {
                    manaPool.addRestricted(color, 1, effect.restriction!!)
                } else {
                    manaPool.add(color, 1)
                }
            }
            container.with(manaPool)
        }

        return EffectResult.success(newState)
    }
}
