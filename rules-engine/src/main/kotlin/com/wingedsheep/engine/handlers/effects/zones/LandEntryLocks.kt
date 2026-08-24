package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.LandsCantEnterTheBattlefield

/**
 * "Lands can't enter the battlefield" ([LandsCantEnterTheBattlefield] — Worms of the Earth).
 *
 * Its own object rather than a private helper because the lock is global: it is owned by whichever
 * permanent prints it, not by the land being moved, so every path that puts a land onto the
 * battlefield has to ask the same question. Today that is the move-effect path;
 * *playing* a land is stopped earlier by `PlayersCantPlayLands`, and a land is never cast.
 */
object LandEntryLocks {

    private val conditionEvaluator = ConditionEvaluator()

    /** True if any permanent on the battlefield forbids lands from entering. */
    fun landsCantEnter(state: GameState, cardRegistry: CardRegistry): Boolean {
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                when (ability) {
                    is LandsCantEnterTheBattlefield -> return true
                    is ConditionalStaticAbility -> {
                        if (ability.ability !is LandsCantEnterTheBattlefield) continue
                        val controller = state.projectedState.getController(entityId) ?: continue
                        val context = EffectContext(sourceId = entityId, controllerId = controller)
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) return true
                    }
                    else -> {}
                }
            }
        }
        return false
    }
}
