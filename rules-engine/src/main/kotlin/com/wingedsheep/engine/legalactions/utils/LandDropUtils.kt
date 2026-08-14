package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Utility for calculating additional land drops from static abilities.
 */
object LandDropUtils {

    /**
     * Count additional land drops granted by [GrantAdditionalLandDrop] static abilities
     * on permanents controlled by the given player. Multiple sources are additive.
     *
     * A [ConditionalStaticAbility] wrapper is unwrapped and its condition evaluated against the
     * source permanent, so "as long as …" gates are honored — Thranduil's Company only grants the
     * extra drop while you control another Elf. Without the unwrap the grant silently no-ops, the
     * same trap [com.wingedsheep.engine.core.MaximumHandSize] documents for `SetMaximumHandSize`.
     */
    fun getAdditionalLandDrops(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator = ConditionEvaluator(),
    ): Int {
        var bonus = 0
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                when (ability) {
                    is GrantAdditionalLandDrop -> bonus += ability.count
                    is ConditionalStaticAbility -> {
                        val inner = ability.ability as? GrantAdditionalLandDrop ?: continue
                        val context = EffectContext(sourceId = entityId, controllerId = playerId)
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) {
                            bonus += inner.count
                        }
                    }
                    else -> {}
                }
            }
        }
        return bonus
    }
}
