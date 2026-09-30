package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.core.emptyingManaConversions
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaPoolChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.LoseUnspentManaEffect
import kotlin.reflect.KClass

class LoseUnspentManaExecutor(private val cardRegistry: CardRegistry) : EffectExecutor<LoseUnspentManaEffect> {
    override val effectType: KClass<LoseUnspentManaEffect> = LoseUnspentManaEffect::class

    override fun execute(state: GameState, effect: LoseUnspentManaEffect, context: EffectContext): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) return EffectResult.error(state, "No valid player for mana loss")

        val conversions = emptyingManaConversions(state, cardRegistry)
        var newState = state
        val events = mutableListOf<ManaPoolChangedEvent>()
        for (playerId in playerIds.distinct()) {
            val pool = newState.getEntity(playerId)?.get<ManaPoolComponent>() ?: continue
            if (pool.isEmpty) continue
            // Step/phase retention does not apply, but unconditional conversion replaces any loss.
            val conversion = conversions[playerId]
            val updatedPool = if (conversion == null) pool.empty() else {
                val plainMana = pool.white + pool.blue + pool.black + pool.red + pool.green + pool.colorless
                pool.copy(
                    white = 0, blue = 0, black = 0, red = 0, green = 0, colorless = 0,
                    restrictedMana = pool.restrictedMana.map { it.copy(color = conversion) }
                ).add(conversion, plainMana)
            }
            if (updatedPool == pool) continue
            newState = newState.updateEntity(playerId) { it.with(updatedPool) }
            events.add(ManaPoolChangedEvent(playerId))
        }
        return EffectResult.success(newState, events)
    }
}
