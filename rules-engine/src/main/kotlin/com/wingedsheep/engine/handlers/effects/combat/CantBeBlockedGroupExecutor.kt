package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.effects.CantBeBlockedGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBeBlockedGroupEffect.
 * "[Filter] creatures can't be blocked this turn." (Jace, Arcane Strategist's −7)
 *
 * Creates one floating effect granting CANT_BE_BLOCKED to every creature matching the filter,
 * re-evaluated on each projection. Per Rule 611.2c a rule-modifying effect applies to matching
 * objects that arrive after it resolves, unlike a keyword grant, which locks its set in.
 */
class CantBeBlockedGroupExecutor : EffectExecutor<CantBeBlockedGroupEffect> {

    override val effectType: KClass<CantBeBlockedGroupEffect> = CantBeBlockedGroupEffect::class

    override fun execute(
        state: GameState,
        effect: CantBeBlockedGroupEffect,
        context: EffectContext
    ): EffectResult {
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED.name),
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = effect.filter
        )

        return EffectResult.success(newState)
    }
}
