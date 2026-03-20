package com.wingedsheep.engine.ai.evaluation

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Life differential: your life minus opponent's life.
 *
 * At low life totals, each point is worth more (being at 3 life is much worse
 * than being at 13 life, even though the difference is only 10 both ways from 20).
 */
object LifeDifferential : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0
        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
        val theirLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 0

        // Non-linear: being at 5 life is much worse per-point than being at 15
        return lifeValue(myLife) - lifeValue(theirLife)
    }

    private fun lifeValue(life: Int): Double = when {
        life <= 0 -> -100.0
        life <= 3 -> life * 3.0   // each point is critical
        life <= 7 -> 9.0 + (life - 3) * 2.0  // danger zone
        life <= 15 -> 17.0 + (life - 7) * 1.0  // comfortable
        else -> 25.0 + (life - 15) * 0.3  // excess life has diminishing value
    }
}

/**
 * Board presence: total effective power controlled, accounting for creature quality.
 *
 * Measures damage potential and board dominance.
 */
object BoardPresence : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0
        return boardValue(state, projected, playerId) - boardValue(state, projected, opponentId)
    }

    private fun boardValue(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        var total = 0.0
        for (entityId in projected.getBattlefieldControlledBy(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            total += permanentValue(state, projected, entityId, card)
        }
        return total
    }

    internal fun permanentValue(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        card: CardComponent
    ): Double {
        val container = state.getEntity(entityId) ?: return 0.0

        if (projected.isCreature(entityId)) {
            val power = projected.getPower(entityId) ?: 0
            val toughness = projected.getToughness(entityId) ?: 0
            var value = power.toDouble() + toughness.toDouble() * 0.5

            // Keyword bonuses
            val keywords = projected.getKeywords(entityId)
            if (Keyword.FLYING.name in keywords) value += 1.5
            if (Keyword.TRAMPLE.name in keywords) value += 0.8
            if (Keyword.LIFELINK.name in keywords) value += 1.0
            if (Keyword.DEATHTOUCH.name in keywords) value += 1.5
            if (Keyword.FIRST_STRIKE.name in keywords) value += 1.0
            if (Keyword.DOUBLE_STRIKE.name in keywords) value += 2.0
            if (Keyword.VIGILANCE.name in keywords) value += 0.5
            if (Keyword.MENACE.name in keywords) value += 0.5
            if (Keyword.HEXPROOF.name in keywords) value += 1.0
            if (Keyword.INDESTRUCTIBLE.name in keywords) value += 2.0
            if (Keyword.DEFENDER.name in keywords) value -= 1.0

            // +1/+1 counters are extra valuable (permanent buff)
            val counters = container.get<CountersComponent>()
            val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            value += plusCounters * 0.3

            // Summoning sickness / tapped penalty
            if (container.has<SummoningSicknessComponent>() && Keyword.HASTE.name !in keywords) {
                value *= 0.7
            }
            if (container.has<TappedComponent>()) {
                value *= 0.8
            }

            return value.coerceAtLeast(0.0)
        }

        // Non-creature permanents: lands are worth a bit, enchantments/artifacts more
        if (card.isLand) return 0.5
        return 1.5  // enchantments, artifacts, planeswalkers
    }
}

/**
 * Card advantage: cards in hand + card-like resources relative to opponent.
 *
 * Having more options means more flexibility and resilience.
 */
object CardAdvantage : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0
        val myCards = state.getZone(playerId, Zone.HAND).size
        val theirCards = state.getZone(opponentId, Zone.HAND).size

        // Each card in hand is worth about 1.5 points, with diminishing returns
        return cardValue(myCards) - cardValue(theirCards)
    }

    private fun cardValue(count: Int): Double = when {
        count <= 0 -> -2.0  // empty hand is very bad (topdeck mode)
        count <= 3 -> count * 1.5
        count <= 7 -> 4.5 + (count - 3) * 1.0  // diminishing returns
        else -> 8.5 + (count - 7) * 0.3  // excess cards (approaching discard)
    }
}
