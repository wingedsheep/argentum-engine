package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * Specialized advisor for attack and block decisions.
 *
 * Combat in MTG is combinatorially explosive (5 attackers × 5 blockers = huge space).
 * Rather than brute-force searching all combinations, the CombatAdvisor uses
 * MTG-specific heuristics: profitable trades, evasion, lethal math, and chump blocking.
 */
class CombatAdvisor(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator
) {
    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     */
    fun chooseAttackers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): GameAction {
        val projected = state.projectedState
        val validAttackers = legalAction.validAttackers ?: emptyList()
        val defendingPlayers = legalAction.validAttackTargets ?: emptyList()

        if (validAttackers.isEmpty() || defendingPlayers.isEmpty()) {
            return DeclareAttackers(playerId, emptyMap())
        }

        // Default defending target is the opponent player (first non-planeswalker target)
        val opponentId = state.getOpponent(playerId) ?: defendingPlayers.first()

        val opponentLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20
        val analyses = validAttackers.map { analyzeAttacker(state, projected, it, playerId) }

        // Check if we have lethal on board
        val totalPower = analyses.sumOf { it.power }
        if (totalPower >= opponentLife) {
            // Alpha strike — send everything for lethal
            val attackers = validAttackers.associateWith { opponentId }
            return DeclareAttackers(playerId, attackers)
        }

        // Evaluate each creature for profitable attacks
        val attackerMap = mutableMapOf<EntityId, EntityId>()
        val opponentCreatures = getOpponentUntappedCreatures(state, projected, playerId)

        for (analysis in analyses) {
            val shouldAttack = shouldAttack(analysis, opponentCreatures, projected, state, opponentLife)
            if (shouldAttack) {
                attackerMap[analysis.entityId] = opponentId
            }
        }

        // If simulation is better, use it for close calls
        if (attackerMap.size in 1..6) {
            val attackAction = DeclareAttackers(playerId, attackerMap)
            val noAttackAction = DeclareAttackers(playerId, emptyMap())
            val attackScore = evaluateAction(state, attackAction, playerId)
            val noAttackScore = evaluateAction(state, noAttackAction, playerId)
            if (noAttackScore > attackScore) {
                return noAttackAction
            }
        }

        return DeclareAttackers(playerId, attackerMap)
    }

    /**
     * Build a DeclareBlockers action choosing which creatures block which attackers.
     */
    fun chooseBlockers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): GameAction {
        val projected = state.projectedState
        val validBlockers = legalAction.validBlockers ?: emptyList()
        val mandatory = legalAction.mandatoryBlockerAssignments ?: emptyMap()

        if (validBlockers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        // Find all attacking creatures
        val attackers = getAttackingCreatures(state, projected)
        if (attackers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        val incomingDamage = attackers.sumOf { projected.getPower(it) ?: 0 }
        val isLethal = incomingDamage >= myLife

        val blockerMap = mutableMapOf<EntityId, List<EntityId>>()

        // Handle mandatory blockers first
        for ((blockerId, mustBlockAttackers) in mandatory) {
            if (mustBlockAttackers.isNotEmpty()) {
                blockerMap[blockerId] = listOf(mustBlockAttackers.first())
            }
        }

        val assignedBlockers = blockerMap.keys.toMutableSet()
        val blockedAttackers = blockerMap.values.flatten().toMutableSet()

        if (isLethal) {
            // Must block to survive — greedily assign blockers to reduce damage
            assignBlocksForSurvival(
                state, projected, validBlockers, attackers,
                assignedBlockers, blockedAttackers, blockerMap, myLife
            )
        } else {
            // Block for profitable trades
            assignBlocksForProfit(
                state, projected, validBlockers, attackers,
                assignedBlockers, blockedAttackers, blockerMap
            )
        }

        return DeclareBlockers(playerId, blockerMap)
    }

    // ── Attack analysis ──────────────────────────────────────────────────

    private data class AttackAnalysis(
        val entityId: EntityId,
        val power: Int,
        val toughness: Int,
        val hasFlying: Boolean,
        val hasTrample: Boolean,
        val hasMenace: Boolean,
        val hasVigilance: Boolean,
        val hasDeathtouch: Boolean,
        val hasFirstStrike: Boolean,
        val hasLifelink: Boolean,
        val hasIndestructible: Boolean,
        val value: Double // how much we'd lose if this creature dies
    )

    private fun analyzeAttacker(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        playerId: EntityId
    ): AttackAnalysis {
        val power = projected.getPower(entityId) ?: 0
        val toughness = projected.getToughness(entityId) ?: 0
        val keywords = projected.getKeywords(entityId)
        val card = state.getEntity(entityId)?.get<CardComponent>()

        return AttackAnalysis(
            entityId = entityId,
            power = power,
            toughness = toughness,
            hasFlying = Keyword.FLYING.name in keywords,
            hasTrample = Keyword.TRAMPLE.name in keywords,
            hasMenace = Keyword.MENACE.name in keywords,
            hasVigilance = Keyword.VIGILANCE.name in keywords,
            hasDeathtouch = Keyword.DEATHTOUCH.name in keywords,
            hasFirstStrike = Keyword.FIRST_STRIKE.name in keywords || Keyword.DOUBLE_STRIKE.name in keywords,
            hasLifelink = Keyword.LIFELINK.name in keywords,
            hasIndestructible = Keyword.INDESTRUCTIBLE.name in keywords,
            value = card?.let { BoardPresence.permanentValue(state, projected, entityId, it) } ?: 0.0
        )
    }

    private fun shouldAttack(
        analysis: AttackAnalysis,
        opponentCreatures: List<EntityId>,
        projected: ProjectedState,
        state: GameState,
        opponentLife: Int
    ): Boolean {
        // Always attack with 0-power creatures? No.
        if (analysis.power <= 0) return false

        // Always attack with vigilance (no downside from tapping)
        if (analysis.hasVigilance) return true

        // Always attack with indestructible (can't die in combat)
        if (analysis.hasIndestructible) return true

        // Evasion creatures are almost always good attacks
        if (analysis.hasFlying) {
            // Check if opponent has flyers/reach that could block profitably
            val canBeBlocked = opponentCreatures.any { blocker ->
                val bKeywords = projected.getKeywords(blocker)
                (Keyword.FLYING.name in bKeywords || Keyword.REACH.name in bKeywords) &&
                    wouldDieInCombat(analysis, projected, blocker, state)
            }
            if (!canBeBlocked) return true
        }

        // Menace is harder to block
        if (analysis.hasMenace && opponentCreatures.size <= 1) return true

        // Check if any opponent creature can profitably block us
        val wouldTradeDown = opponentCreatures.any { blocker ->
            val blockerPower = projected.getPower(blocker) ?: 0
            val blockerToughness = projected.getToughness(blocker) ?: 0
            // They can kill us and their creature is less valuable
            blockerPower >= analysis.toughness && blockerToughness > analysis.power &&
                creatureValue(state, projected, blocker) < analysis.value
        }

        // If no one can profitably block us, attack
        if (!wouldTradeDown && opponentCreatures.none { blocker ->
            val blockerToughness = projected.getToughness(blocker) ?: 0
            blockerToughness > analysis.power // they survive blocking
        }) return true

        // Deathtouch creatures trade up
        if (analysis.hasDeathtouch) return true

        // First strike creatures have an advantage
        if (analysis.hasFirstStrike) return true

        // If opponent is at low life, be more aggressive
        if (opponentLife <= analysis.power * 3) return true

        // Default: attack if we're bigger than most of their creatures
        val avgOpponentToughness = if (opponentCreatures.isNotEmpty()) {
            opponentCreatures.mapNotNull { projected.getToughness(it) }.average()
        } else 0.0

        return analysis.power >= avgOpponentToughness
    }

    private fun wouldDieInCombat(
        analysis: AttackAnalysis,
        projected: ProjectedState,
        blockerId: EntityId,
        state: GameState
    ): Boolean {
        val blockerPower = projected.getPower(blockerId) ?: 0
        val blockerKeywords = projected.getKeywords(blockerId)
        val hasBlockerDeathtouch = Keyword.DEATHTOUCH.name in blockerKeywords

        if (analysis.hasIndestructible) return false
        if (hasBlockerDeathtouch && blockerPower > 0) return true
        return blockerPower >= analysis.toughness
    }

    // ── Blocking strategies ──────────────────────────────────────────────

    private fun assignBlocksForSurvival(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        myLife: Int
    ) {
        val available = validBlockers.filter { it !in assignedBlockers }
        val unblocked = attackers.filter { it !in blockedAttackers }

        // Sort attackers by power descending (block biggest threats first)
        val sortedAttackers = unblocked.sortedByDescending { projected.getPower(it) ?: 0 }

        for (attacker in sortedAttackers) {
            val attackerPower = projected.getPower(attacker) ?: 0
            if (attackerPower <= 0) continue

            // Find a blocker that can at least chump
            val bestBlocker = available
                .filter { it !in assignedBlockers }
                .minByOrNull { blocker ->
                    // Prefer blocking with creatures of lower value
                    creatureValue(state, projected, blocker)
                }

            if (bestBlocker != null) {
                blockerMap[bestBlocker] = listOf(attacker)
                assignedBlockers.add(bestBlocker)
                blockedAttackers.add(attacker)
            }
        }
    }

    private fun assignBlocksForProfit(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        val available = validBlockers.filter { it !in assignedBlockers }
        val unblocked = attackers.filter { it !in blockedAttackers }

        for (attacker in unblocked) {
            val attackerPower = projected.getPower(attacker) ?: 0
            val attackerToughness = projected.getToughness(attacker) ?: 0
            val attackerValue = creatureValue(state, projected, attacker)
            val attackerKeywords = projected.getKeywords(attacker)
            val attackerHasDeathtouch = Keyword.DEATHTOUCH.name in attackerKeywords

            // Find a blocker that trades profitably
            val profitableBlocker = available
                .filter { it !in assignedBlockers }
                .filter { blocker ->
                    val blockerPower = projected.getPower(blocker) ?: 0
                    val blockerToughness = projected.getToughness(blocker) ?: 0
                    val blockerValue = creatureValue(state, projected, blocker)
                    val blockerKeywords = projected.getKeywords(blocker)
                    val blockerHasDeathtouch = Keyword.DEATHTOUCH.name in blockerKeywords

                    // Can we kill the attacker?
                    val weKillThem = blockerPower >= attackerToughness || blockerHasDeathtouch
                    // Do we survive?
                    val weSurvive = blockerToughness > attackerPower && !attackerHasDeathtouch
                    // Is it a favorable trade? (we kill them and survive, or we kill them and their value > ours)
                    (weKillThem && weSurvive) || (weKillThem && attackerValue > blockerValue * 1.2)
                }
                .minByOrNull { creatureValue(state, projected, it) } // use cheapest profitable blocker

            if (profitableBlocker != null) {
                blockerMap[profitableBlocker] = listOf(attacker)
                assignedBlockers.add(profitableBlocker)
                blockedAttackers.add(attacker)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun getOpponentUntappedCreatures(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId
    ): List<EntityId> {
        val opponentId = state.getOpponent(playerId) ?: return emptyList()
        return projected.getBattlefieldControlledBy(opponentId).filter { entityId ->
            projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() != true
        }
    }

    private fun getAttackingCreatures(state: GameState, projected: ProjectedState): List<EntityId> {
        // Attacking creatures have the AttackingComponent
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
        }
    }

    private fun creatureValue(state: GameState, projected: ProjectedState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, projected, entityId, card)
    }

    private fun evaluateAction(state: GameState, action: GameAction, playerId: EntityId): Double {
        val result = simulator.simulate(state, action)
        return evaluator.evaluate(result.state, result.state.projectedState, playerId)
    }
}
