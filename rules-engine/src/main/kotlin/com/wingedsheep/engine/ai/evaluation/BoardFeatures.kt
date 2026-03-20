package com.wingedsheep.engine.ai.evaluation

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

// ═════════════════════════════════════════════════════════════════════════════
// Life Differential
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Non-linear life differential. Being at 3 life is exponentially worse
 * than being at 13 — each point near death is worth much more.
 */
object LifeDifferential : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0
        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
        val theirLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 0
        return lifeValue(myLife) - lifeValue(theirLife)
    }

    private fun lifeValue(life: Int): Double = when {
        life <= 0 -> -100.0
        life <= 3 -> life * 3.0
        life <= 7 -> 9.0 + (life - 3) * 2.0
        life <= 15 -> 17.0 + (life - 7) * 1.0
        else -> 25.0 + (life - 15) * 0.3
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Board Presence
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Total effective board value. Creatures are scored by combat stats + keywords.
 * Non-creature permanents get type-appropriate values. Enchantments and artifacts
 * that aren't auras get a flat bonus (they're doing something even without P/T).
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
            return creatureValue(state, projected, entityId, container)
        }

        // Non-creature permanents
        if (card.isLand) {
            // Untapped lands are worth more (they represent available mana)
            return if (container.has<TappedComponent>()) 0.3 else 0.6
        }
        if (card.isPlaneswalker) return 4.0
        if (card.isAura) return 1.0
        // Enchantments and artifacts — typically doing something useful
        return 2.0
    }

    private fun creatureValue(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        container: com.wingedsheep.engine.state.ComponentContainer
    ): Double {
        val power = projected.getPower(entityId) ?: 0
        val toughness = projected.getToughness(entityId) ?: 0
        val keywords = projected.getKeywords(entityId)

        // Base: power matters more than toughness for winning
        var value = power * 1.0 + toughness * 0.4

        // ── Evasion (the most important combat keyword category) ──
        if (Keyword.FLYING.name in keywords) value += 1.5 + power * 0.3
        if (Keyword.MENACE.name in keywords) value += 0.8
        if (Keyword.FEAR.name in keywords) value += 0.8
        if (Keyword.INTIMIDATE.name in keywords) value += 0.8
        if (Keyword.SHADOW.name in keywords) value += 1.0
        // Landwalk — contextual but often evasion
        if (Keyword.SWAMPWALK.name in keywords) value += 0.6
        if (Keyword.FORESTWALK.name in keywords) value += 0.6
        if (Keyword.ISLANDWALK.name in keywords) value += 0.6
        if (Keyword.MOUNTAINWALK.name in keywords) value += 0.6

        // ── Combat modifiers ──
        if (Keyword.TRAMPLE.name in keywords) value += 0.5 + power * 0.2
        if (Keyword.DEATHTOUCH.name in keywords) value += 2.0  // trades with anything
        if (Keyword.FIRST_STRIKE.name in keywords) value += 1.0 + power * 0.2
        if (Keyword.DOUBLE_STRIKE.name in keywords) value += 2.0 + power * 0.5
        if (Keyword.LIFELINK.name in keywords) value += 0.5 + power * 0.3
        if (Keyword.VIGILANCE.name in keywords) value += 0.8  // attacks without cost
        if (Keyword.PROVOKE.name in keywords) value += 0.5

        // ── Survivability ──
        if (Keyword.INDESTRUCTIBLE.name in keywords) value += 3.0
        if (Keyword.HEXPROOF.name in keywords) value += 1.5
        if (Keyword.SHROUD.name in keywords) value += 1.2
        if (Keyword.PROTECTION.name in keywords) value += 1.0

        // ── Drawbacks ──
        if (Keyword.DEFENDER.name in keywords) value -= power * 0.8 // can't attack, power mostly wasted

        // ── Speed ──
        if (Keyword.HASTE.name in keywords && container.has<SummoningSicknessComponent>()) {
            value += 0.5 // haste is most valuable the turn it enters
        }

        // +1/+1 counters represent permanent investment
        val counters = container.get<CountersComponent>()
        val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        value += plusCounters * 0.5

        // Summoning sickness reduces immediate threat
        if (container.has<SummoningSicknessComponent>() && Keyword.HASTE.name !in keywords) {
            value *= 0.65
        }

        // Tapped creatures can't block and already attacked
        if (container.has<TappedComponent>()) {
            value *= 0.75
        }

        // Damaged creatures are closer to dying
        val damage = container.get<DamageComponent>()?.amount ?: 0
        if (damage > 0 && toughness > 0) {
            val healthFraction = (toughness - damage).toDouble() / toughness
            value *= (0.5 + 0.5 * healthFraction) // half value at 1 toughness remaining
        }

        return value.coerceAtLeast(0.1)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Card Advantage
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Cards in hand with non-linear scaling. Empty hand (topdeck mode) is heavily
 * penalized. Excess cards have diminishing returns since you'll discard at cleanup.
 */
object CardAdvantage : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0
        val myCards = state.getZone(playerId, Zone.HAND).size
        val theirCards = state.getZone(opponentId, Zone.HAND).size
        return cardValue(myCards) - cardValue(theirCards)
    }

    private fun cardValue(count: Int): Double = when {
        count <= 0 -> -3.0
        count == 1 -> 1.0
        count <= 3 -> 1.0 + (count - 1) * 1.5
        count <= 7 -> 4.0 + (count - 3) * 0.8
        else -> 7.2 + (count - 7) * 0.2 // past 7 cards, you're discarding anyway
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Threat Assessment
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Measures how close the opponent is to killing you with creatures on board.
 * A negative score means they have enough power to threaten lethal.
 *
 * This is distinct from LifeDifferential — it measures *potential* damage, not
 * actual life totals. An opponent at 5 life with 15 power on board is more
 * dangerous than one at 5 life with 1 power.
 */
object ThreatAssessment : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        val theirLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20

        // Calculate attack potential (power of untapped, non-sick creatures)
        val myAttackPower = attackPotential(state, projected, playerId)
        val theirAttackPower = attackPotential(state, projected, opponentId)

        // Calculate defense capability (total toughness of untapped creatures that can block)
        val myDefense = defensePotential(state, projected, playerId)
        val theirDefense = defensePotential(state, projected, opponentId)

        // How many turns until opponent kills us (if we can't block)
        val turnsUntilDead = if (theirAttackPower > 0) myLife.toDouble() / theirAttackPower else 99.0
        val turnsUntilWeKill = if (myAttackPower > 0) theirLife.toDouble() / myAttackPower else 99.0

        // Score: positive if we're the faster clock
        var score = 0.0

        // Being closer to killing them is good
        if (turnsUntilWeKill < turnsUntilDead) {
            score += (turnsUntilDead - turnsUntilWeKill) * 2.0
        } else {
            score -= (turnsUntilWeKill - turnsUntilDead) * 1.5
        }

        // Lethal on board next turn is very valuable
        if (myAttackPower >= theirLife && myAttackPower > theirDefense) score += 8.0
        if (theirAttackPower >= myLife && theirAttackPower > myDefense) score -= 10.0

        // Evasive damage (flying power they can't block)
        val theirEvasivePower = evasivePower(state, projected, opponentId, playerId)
        val myEvasivePower = evasivePower(state, projected, playerId, opponentId)
        score += (myEvasivePower - theirEvasivePower) * 0.5

        return score
    }

    private fun attackPotential(state: GameState, projected: ProjectedState, playerId: EntityId): Int {
        return projected.getBattlefieldControlledBy(playerId)
            .filter { entityId ->
                projected.isCreature(entityId) &&
                    !projected.cantAttack(entityId) &&
                    state.getEntity(entityId)?.has<SummoningSicknessComponent>() != true &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true
            }
            .sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
    }

    private fun defensePotential(state: GameState, projected: ProjectedState, playerId: EntityId): Int {
        return projected.getBattlefieldControlledBy(playerId)
            .filter { entityId ->
                projected.isCreature(entityId) &&
                    !projected.cantBlock(entityId) &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true
            }
            .sumOf { (projected.getToughness(it) ?: 0).coerceAtLeast(0) }
    }

    /** Power of creatures with flying/evasion that the defender can't block. */
    private fun evasivePower(
        state: GameState,
        projected: ProjectedState,
        attackerId: EntityId,
        defenderId: EntityId
    ): Int {
        val defenderHasFlyers = projected.getBattlefieldControlledBy(defenderId).any { entityId ->
            projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() != true &&
                (Keyword.FLYING.name in projected.getKeywords(entityId) ||
                    Keyword.REACH.name in projected.getKeywords(entityId))
        }

        if (defenderHasFlyers) return 0 // they can block flyers

        return projected.getBattlefieldControlledBy(attackerId)
            .filter { entityId ->
                projected.isCreature(entityId) &&
                    Keyword.FLYING.name in projected.getKeywords(entityId) &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true &&
                    state.getEntity(entityId)?.has<SummoningSicknessComponent>() != true
            }
            .sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Tempo
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Measures mana development and efficiency. Having more available mana means
 * casting bigger spells and holding up responses. Counts lands (not mana pool)
 * since land count is the durable measure of mana development.
 */
object Tempo : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val opponentId = state.getOpponent(playerId) ?: return 0.0

        val myLands = countLands(state, projected, playerId)
        val theirLands = countLands(state, projected, opponentId)

        // Each land ahead is worth about 1.5 points (mana advantage = tempo)
        // But the first few lands matter more than later ones
        return landValue(myLands) - landValue(theirLands)
    }

    private fun countLands(state: GameState, projected: ProjectedState, playerId: EntityId): Int {
        return projected.getBattlefieldControlledBy(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.isLand == true
        }
    }

    private fun landValue(count: Int): Double = when {
        count <= 0 -> -5.0  // no mana is terrible
        count <= 3 -> count * 2.0  // early mana is critical
        count <= 6 -> 6.0 + (count - 3) * 1.2  // mid-game mana
        else -> 9.6 + (count - 6) * 0.4  // diminishing returns for excess mana
    }
}
