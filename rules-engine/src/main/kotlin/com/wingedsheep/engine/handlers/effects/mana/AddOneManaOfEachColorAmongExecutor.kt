package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.mechanics.mana.ChosenTypeManaRestriction
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.ManaColorSource
import kotlin.reflect.KClass

/**
 * Executor for [AddOneManaOfEachColorAmongEffect].
 * "{T}: For each color among permanents you control, add one mana of that color."
 *
 * Unlike [AddManaOfColorAmongExecutor] (which lets the player pick one color), this adds
 * one mana of EVERY color found in the union of matching permanents' colors — simultaneously,
 * no choice. Colors are read from projected state so recolor effects are honored.
 */
class AddOneManaOfEachColorAmongExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<AddOneManaOfEachColorAmongEffect> {

    override val effectType: KClass<AddOneManaOfEachColorAmongEffect> =
        AddOneManaOfEachColorAmongEffect::class

    override fun execute(
        state: GameState,
        effect: AddOneManaOfEachColorAmongEffect,
        context: EffectContext
    ): EffectResult {
        val availableColors = mutableSetOf<Color>()
        when (effect.colorSource) {
            ManaColorSource.MatchingPermanents -> {
                val projected = state.projectedState
                val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, effect.filter, context, predicateEvaluator = predicateEvaluator)
                for (entityId in matched) {
                    val colors = projected.getColors(entityId)
                    for (colorName in colors) {
                        Color.entries.find { it.name == colorName }?.let { availableColors.add(it) }
                    }
                }
            }
            ManaColorSource.CraftedMaterials -> {
                // The exile-zone materials' colors: printed, unless an off-battlefield color
                // effect (Mycosynth Lattice) sets them.
                val materials = context.sourceId?.let { state.getEntity(it) }
                    ?.get<com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent>()
                materials?.exiledIds?.forEach { exiledId ->
                    availableColors.addAll(state.projectedState.colorSetOf(exiledId))
                }
            }
        }

        if (availableColors.isEmpty()) {
            return EffectResult.success(state)
        }

        val restriction = ChosenTypeManaRestriction.bind(effect.restriction, state, context.sourceId)
        val newState = state.updateEntity(context.controllerId) { container ->
            var manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            for (color in availableColors) {
                manaPool = if (restriction != null) {
                    manaPool.addRestricted(color, 1, restriction)
                } else {
                    manaPool.add(color, 1)
                }
            }
            container.with(manaPool)
        }

        return EffectResult.success(newState)
    }
}
