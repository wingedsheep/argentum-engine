package com.wingedsheep.engine.ai.advisor.modules

import com.wingedsheep.engine.ai.advisor.*
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * AI advisors for Onslaught (ONS) cards.
 */
class OnslaughtAdvisorModule : CardAdvisorModule {
    override fun register(registry: CardAdvisorRegistry) {
        registry.register(GoblinSharpshooterAdvisor)
    }
}

/**
 * Goblin Sharpshooter: 1/1 that taps to deal 1 damage, untaps whenever a creature dies.
 *
 * Strategy:
 * - **Don't attack**: The tap ability is far more valuable than 1 combat damage.
 *   A 1/1 dies to any blocker, losing the repeatable damage engine permanently.
 * - **Activate aggressively**: Boost the score for using the tap ability, especially
 *   when there are 1-toughness creatures to pick off (which also untaps the Sharpshooter).
 * - **Target 1-toughness creatures first**: Killing a creature untaps it, enabling chains.
 *   Otherwise hit the opponent for direct damage.
 */
object GoblinSharpshooterAdvisor : CardAdvisor {
    override val cardNames = setOf("Goblin Sharpshooter")

    override fun attackPenalty(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        playerId: EntityId
    ): Double {
        // Large penalty: a 1/1 attacking for 1 is almost never worth risking
        // the repeatable damage engine. But not infinite — lethal alpha strikes
        // at aggression 5 bypass the per-creature check entirely, so this
        // only affects normal combat decisions.
        return 10.0
    }

    override fun evaluateCast(context: CastContext): Double? {
        // This fires for the activated ability ({T}: deal 1 damage)
        val state = context.state
        val playerId = context.playerId
        val opponentId = state.getOpponent(playerId) ?: return null

        val projected = context.projected
        val oppCreatures = projected.getBattlefieldControlledBy(opponentId)

        // Check if there are 1-toughness creatures to snipe
        val has1ToughnessTarget = oppCreatures.any { entityId ->
            if (!projected.isCreature(entityId)) return@any false
            val toughness = projected.getToughness(entityId) ?: return@any false
            val damage = state.getEntity(entityId)?.get<DamageComponent>()?.amount ?: 0
            toughness - damage <= 1
        }

        if (has1ToughnessTarget) {
            // Big boost: killing a creature untaps the Sharpshooter for another shot
            return context.defaultScore + 5.0
        }

        // Even without a killable creature, pinging the opponent is decent value
        // since something might die in combat and untap us
        return context.defaultScore + 1.0
    }

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseTargetsDecision ?: return null
        if (decision.targetRequirements.size != 1) return null

        val req = decision.targetRequirements.first()
        val targets = decision.legalTargets[req.index] ?: return null
        if (targets.isEmpty()) return null

        val state = context.state
        val playerId = context.playerId
        val opponentId = state.getOpponent(playerId) ?: return null
        val projected = context.projected

        // Priority 1: opponent creatures at exactly 1 remaining toughness (kills → untap → chain)
        val killableCreature = targets.filter { targetId ->
            val controller = projected.getController(targetId)
            if (controller == playerId) return@filter false
            if (!projected.isCreature(targetId)) return@filter false
            val toughness = projected.getToughness(targetId) ?: return@filter false
            val damage = state.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            toughness - damage <= 1
        }.maxByOrNull { targetId ->
            // Prefer the most valuable killable creature
            val power = projected.getPower(targetId) ?: 0
            val toughness = projected.getToughness(targetId) ?: 0
            power * 2.0 + toughness
        }

        if (killableCreature != null) {
            return TargetsResponse(decision.id, mapOf(req.index to listOf(killableCreature)))
        }

        // Priority 2: opponent player (direct damage adds up)
        if (opponentId in targets) {
            return TargetsResponse(decision.id, mapOf(req.index to listOf(opponentId)))
        }

        // Priority 3: any opponent creature (soften it up for next activation or combat)
        val oppCreature = targets.filter { targetId ->
            projected.getController(targetId) != playerId && projected.isCreature(targetId)
        }.maxByOrNull { targetId ->
            // Prefer creatures closest to dying
            val toughness = projected.getToughness(targetId) ?: 0
            val damage = state.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            damage.toDouble() / toughness.coerceAtLeast(1)
        }

        if (oppCreature != null) {
            return TargetsResponse(decision.id, mapOf(req.index to listOf(oppCreature)))
        }

        return null
    }
}
