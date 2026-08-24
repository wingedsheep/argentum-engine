package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ForagedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ForagedEffect
import kotlin.reflect.KClass

/**
 * Executor for [ForagedEffect] — the marker `Patterns.Mechanic.forage` appends to each of its two
 * modes so "Whenever you forage" (CR 701.59a) sees a forage taken as an *effect*.
 *
 * Event-only, no state change, exactly like [GiftGivenExecutor]. The forager here is the effect's
 * controller: a forage the player *chose* to take on resolution is always their own, while a forage
 * paid as a **cost** can belong to an opponent — which is why that side emits from
 * [com.wingedsheep.engine.handlers.costs.ForageCostResolver] with the paying player instead of
 * routing through here.
 *
 * Because the marker lives inside each mode of the `ChooseActionEffect`, a declined forage — or one
 * where neither mode was feasible — never reaches this executor, which is the "only if it actually
 * happened" property the trigger needs.
 */
class ForagedExecutor : EffectExecutor<ForagedEffect> {

    override val effectType: KClass<ForagedEffect> = ForagedEffect::class

    override fun execute(
        state: GameState,
        effect: ForagedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        return EffectResult.success(
            state,
            listOf(ForagedEvent(playerId = context.controllerId, sourceName = sourceName))
        )
    }
}
