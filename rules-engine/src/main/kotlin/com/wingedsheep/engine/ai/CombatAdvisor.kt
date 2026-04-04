package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.advisor.CardAdvisorRegistry
import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Specialized advisor for attack and block decisions.
 *
 * Per-creature attack decisions use simulation: declare this creature attacking,
 * predict the opponent's block using [chooseBlockers], resolve combat damage,
 * and compare the resulting board evaluation to not attacking.
 */
class CombatAdvisor(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val cardRegistry: CardRegistry? = null,
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry()
) {
    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     *
     * Two-phase approach:
     * 1. Heuristic seed: always-attack creatures (evasive, vigilance, indestructible),
     *    plus lethal alpha-strike detection
     * 2. Local search: try adding/removing one attacker at a time, simulate each through
     *    the engine (opponent blocks via heuristic, combat resolves), keep improvements
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

        val opponentId = state.getOpponent(playerId) ?: defendingPlayers.first()
        val opponentLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20
        val opponentCreatures = CombatMath.getOpponentUntappedCreatures(state, projected, opponentId)
        val mandatory = legalAction.mandatoryAttackers ?: emptyList()

        // ── Lethal check: alpha-strike if damage gets through even with optimal blocking ──
        if (isLethalAttack(state, projected, validAttackers, opponentCreatures, opponentLife)) {
            return DeclareAttackers(playerId, validAttackers.associateWith { opponentId })
        }

        // ── Heuristic seed: no-downside and clearly profitable attackers ──
        val seedMap = mutableMapOf<EntityId, EntityId>()
        for (entityId in mandatory) {
            seedMap[entityId] = opponentId
        }
        for (entityId in validAttackers) {
            if (entityId in seedMap) continue
            val power = projected.getPower(entityId) ?: 0
            if (power <= 0) continue

            val toughness = projected.getToughness(entityId) ?: 0
            val keywords = projected.getKeywords(entityId)
            val isEvasive = CombatMath.isEvasive(state, projected, entityId, opponentCreatures)

            // Always attack: no risk
            if (Keyword.VIGILANCE.name in keywords ||
                Keyword.INDESTRUCTIBLE.name in keywords ||
                opponentCreatures.isEmpty() ||
                isEvasive
            ) {
                seedMap[entityId] = opponentId
                continue
            }

            // Attack if we survive any single blocker (toughness > all opponent power)
            val survivesAllBlockers = opponentCreatures.all { blockerId ->
                val bPower = projected.getPower(blockerId) ?: 0
                toughness > bPower
            }
            if (survivesAllBlockers) {
                seedMap[entityId] = opponentId
                continue
            }

            // Attack if trample and we'd deal significant damage through
            if (Keyword.TRAMPLE.name in keywords) {
                val bestBlockerToughness = opponentCreatures
                    .filter { CombatMath.canBeBlockedBy(state, projected, entityId, it) }
                    .maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
                val damageThrough = (power - bestBlockerToughness).coerceAtLeast(0)
                if (damageThrough > 0 && toughness > (opponentCreatures.minOfOrNull { projected.getPower(it) ?: 0 } ?: 0)) {
                    seedMap[entityId] = opponentId
                    continue
                }
            }
        }

        // Attack if we have more attackers than they have blockers (excess gets through)
        if (seedMap.size < validAttackers.size) {
            val unblockedSlots = validAttackers.size - opponentCreatures.size
            if (unblockedSlots > 0) {
                // Add remaining creatures sorted by power descending — the best ones go unblocked
                val remaining = validAttackers
                    .filter { it !in seedMap && (projected.getPower(it) ?: 0) > 0 }
                    .sortedByDescending { projected.getPower(it) ?: 0 }
                for (entityId in remaining) {
                    seedMap[entityId] = opponentId
                }
            }
        }

        // ── Local search: try add/remove mutations via simulation ──
        // Only run if we're at DECLARE_ATTACKERS (simulation needs to submit DeclareAttackers)
        if (state.step == Step.DECLARE_ATTACKERS) {
            val deadline = System.currentTimeMillis() + 1000
            improveAttackViaLocalSearch(
                state, playerId, opponentId, validAttackers, mandatory.toSet(), seedMap, deadline
            )
        }

        return DeclareAttackers(playerId, seedMap)
    }

    /**
     * Build a DeclareBlockers action choosing which creatures block which attackers.
     *
     * Uses a two-phase approach:
     * 1. Generate a seed plan using fast combat math heuristics
     * 2. Improve via local search: try small mutations (swap/add/remove one blocker),
     *    simulate each through the engine, keep improvements until no mutation helps
     *
     * The engine simulation correctly handles all keyword interactions (first strike,
     * deathtouch, trample, lifelink, indestructible, double strike, etc.) without
     * heuristic blind spots. Local search keeps the simulation count low (~10-20).
     *
     * When called from within attack simulation (nested), skips local search
     * to avoid exponential cost and uses the heuristic seed plan directly.
     */
    fun chooseBlockers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        useSimulation: Boolean = false
    ): GameAction {
        val projected = state.projectedState
        val validBlockers = legalAction.validBlockers ?: emptyList()
        val mandatory = legalAction.mandatoryBlockerAssignments ?: emptyMap()

        if (validBlockers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val attackers = getAttackingCreatures(state)
        if (attackers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20

        // Build mandatory blocker base (preserved across all plans)
        val mandatoryMap = mutableMapOf<EntityId, List<EntityId>>()
        val mandatoryBlockerIds = mutableSetOf<EntityId>()
        for ((blockerId, mustBlockAttackers) in mandatory) {
            if (mustBlockAttackers.isNotEmpty()) {
                mandatoryMap[blockerId] = listOf(mustBlockAttackers.first())
                mandatoryBlockerIds.add(blockerId)
            }
        }

        val sortedAttackers = attackers.sortedByDescending { CombatMath.effectiveDamage(projected, it) }

        // ── Phase 1: Generate seed plan via heuristic ──
        val seedMap = mandatoryMap.toMutableMap()
        val seedAssigned = mandatoryBlockerIds.toMutableSet()
        chooseBlockersViaHeuristic(
            state, projected, sortedAttackers, validBlockers,
            seedAssigned, seedMap, myLife
        )

        // ── Phase 2: Improve via local search (if not nested) ──
        val bestMap = if (useSimulation) {
            improveViaLocalSearch(
                state, projected, playerId, attackers, validBlockers,
                mandatoryBlockerIds, seedMap
            )
        } else {
            seedMap
        }

        // ── Chump-block pass: if still facing lethal, assign remaining blockers ──
        val assignedBlockers = bestMap.keys.toMutableSet()
        if (isLifeInDanger(state, projected, attackers, bestMap, myLife, playerId)) {
            val unblockedAttackers = attackers
                .filter { attacker -> bestMap.values.none { attacker in it } }
                .sortedByDescending { CombatMath.effectiveDamage(projected, it) }

            for (attacker in unblockedAttackers) {
                val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }
                val cheapest = available.firstOrNull() ?: continue
                bestMap[cheapest] = listOf(attacker)
                assignedBlockers.add(cheapest)
            }
        }

        // ── Menace fix: remove illegal single-blocker assignments for menace attackers ──
        fixMenaceAssignments(state, projected, bestMap, validBlockers, assignedBlockers)

        return DeclareBlockers(playerId, bestMap)
    }

    /**
     * Fix illegal blocker assignments for menace attackers.
     * Menace requires 2+ blockers. If only 1 blocker is assigned, try to add a second;
     * if none available, remove the assignment entirely.
     */
    private fun fixMenaceAssignments(
        state: GameState,
        projected: ProjectedState,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        validBlockers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>
    ) {
        // Build attacker → blockers map
        val attackerBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerList) in blockerMap) {
            for (attackerId in attackerList) {
                attackerBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        for ((attackerId, blockers) in attackerBlockers) {
            val keywords = projected.getKeywords(attackerId)
            if (Keyword.MENACE.name !in keywords) continue
            if (blockers.size >= 2) continue

            // Single blocker on menace — try to find a second
            val available = availableBlockersFor(state, projected, attackerId, validBlockers, assignedBlockers)
            if (available.isNotEmpty()) {
                val second = available.minByOrNull { CombatMath.creatureValue(state, projected, it) }!!
                blockerMap[second] = listOf(attackerId)
                assignedBlockers.add(second)
            } else {
                // Can't find a second blocker — remove the single illegal assignment
                for (b in blockers) {
                    blockerMap.remove(b)
                    assignedBlockers.remove(b)
                }
            }
        }
    }

    /**
     * Local search: starting from the heuristic seed plan, try small mutations
     * and simulate each through the engine. Keep improvements until convergence
     * or time budget is exhausted.
     *
     * Mutations tried per iteration:
     * - Remove each non-mandatory blocker
     * - Move each non-mandatory blocker to a different attacker
     * - Add each unassigned blocker to each attacker
     *
     * Typically converges in 1-3 iterations with ~10-20 total simulations.
     */
    private fun improveViaLocalSearch(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryBlockerIds: Set<EntityId>,
        seedMap: MutableMap<EntityId, List<EntityId>>
    ): MutableMap<EntityId, List<EntityId>> {
        val deadline = System.currentTimeMillis() + 200 // 200ms budget
        var currentPlan = seedMap.toMutableMap()
        var currentScore = evaluateBlockingPlan(state, playerId, currentPlan) ?: return currentPlan
        val maxIterations = 3

        for (iteration in 1..maxIterations) {
            if (System.currentTimeMillis() > deadline) break
            var improved = false

            // Try each mutation, keep the best improvement
            var bestMutation: MutableMap<EntityId, List<EntityId>>? = null
            var bestScore = currentScore

            // Mutation 1: Remove each non-mandatory blocker
            for (blockerId in currentPlan.keys.toList()) {
                if (System.currentTimeMillis() > deadline) break
                if (blockerId in mandatoryBlockerIds) continue
                val mutation = currentPlan.toMutableMap()
                mutation.remove(blockerId)
                val score = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            // Mutation 2: Move each non-mandatory blocker to a different attacker
            for (blockerId in currentPlan.keys.toList()) {
                if (System.currentTimeMillis() > deadline) break
                if (blockerId in mandatoryBlockerIds) continue
                val currentTarget = currentPlan[blockerId]?.firstOrNull() ?: continue
                for (attacker in attackers) {
                    if (attacker == currentTarget) continue
                    if (!CombatMath.canBeBlockedBy(state, projected, attacker, blockerId, cardRegistry)) continue
                    val mutation = currentPlan.toMutableMap()
                    mutation[blockerId] = listOf(attacker)
                    val score = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                    if (score > bestScore) {
                        bestScore = score
                        bestMutation = mutation
                    }
                }
            }

            // Mutation 3: Add each unassigned blocker to each attacker
            val assigned = currentPlan.keys
            for (blockerId in validBlockers) {
                if (System.currentTimeMillis() > deadline) break
                if (blockerId in assigned) continue
                for (attacker in attackers) {
                    if (!CombatMath.canBeBlockedBy(state, projected, attacker, blockerId, cardRegistry)) continue
                    val mutation = currentPlan.toMutableMap()
                    mutation[blockerId] = listOf(attacker)
                    val score = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                    if (score > bestScore) {
                        bestScore = score
                        bestMutation = mutation
                    }
                }
            }

            if (bestMutation != null) {
                currentPlan = bestMutation
                currentScore = bestScore
                improved = true
            }

            if (!improved) break // converged
        }

        return currentPlan
    }

    /**
     * Fast heuristic blocker selection using combat math only (no engine simulation).
     *
     * Three-pass priority system:
     * - Pass 1 (Free kills): blocker kills attacker AND survives — always take these
     * - Pass 2 (Profitable trades): blocker kills attacker, dies, trade favorable by value
     * - Pass 3 (Damage prevention): blocker survives without killing attacker, prevents damage
     * - Pass 4 (Gang blocks): two blockers combine to kill what neither could alone
     */
    private fun chooseBlockersViaHeuristic(
        state: GameState,
        projected: ProjectedState,
        sortedAttackers: List<EntityId>,
        validBlockers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        myLife: Int
    ) {
        val tradeRatio = CombatMath.tradeWillingnessRatio(myLife)
        // Track which attackers are already blocked (e.g., by mandatory blockers)
        val blockedAttackers = blockerMap.values.flatten().toMutableSet()

        // ── Pass 1: Free kills — blocker kills attacker and survives ──
        for (attacker in sortedAttackers) {
            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.effectivelyKillThem && info.weSurvive
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 2: Profitable trades — blocker kills attacker but dies ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val attackerValue = CombatMath.creatureValue(state, projected, attacker)

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.effectivelyKillThem && !info.weSurvive &&
                    info.blockerValue * tradeRatio <= attackerValue
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 3: Damage prevention — blocker survives, prevents damage ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.weSurvive && info.blockerValue < aPower * 1.5
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 4: Gang blocks for remaining unblocked attackers ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aKeywords = projected.getKeywords(attacker)
            if (Keyword.DEATHTOUCH.name in aKeywords) continue

            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.creatureValue(state, projected, attacker)

            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            if (available.size < 2) continue

            val sorted = available.sortedBy { CombatMath.creatureValue(state, projected, it) }
            for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    val b1 = sorted[i]
                    val b2 = sorted[j]
                    val combinedPower = (projected.getPower(b1) ?: 0) + (projected.getPower(b2) ?: 0)
                    if (combinedPower < aToughness) continue

                    if (aHasFirstStrike) {
                        val b1Survives = !CombatMath.wouldKillInCombat(state, projected, attacker, b1)
                        val b2Survives = !CombatMath.wouldKillInCombat(state, projected, attacker, b2)
                        if (!b1Survives || !b2Survives) continue
                    }

                    val dyingBlocker = if ((projected.getToughness(b1) ?: 0) <= aPower) b1 else b2
                    val dyingValue = CombatMath.creatureValue(state, projected, dyingBlocker)
                    if (attackerValue > dyingValue * 1.2) {
                        blockerMap[b1] = listOf(attacker)
                        blockerMap[b2] = listOf(attacker)
                        assignedBlockers.add(b1)
                        assignedBlockers.add(b2)
                        blockedAttackers.add(attacker)
                        break
                    }
                }
                if (attacker in blockedAttackers) break
            }
        }
    }

    /**
     * Info about a potential blocker for evaluating blocking decisions.
     */
    private data class BlockInfo(
        val blockerId: EntityId,
        val effectivelyKillThem: Boolean,
        val weSurvive: Boolean,
        val blockerValue: Double
    )

    /**
     * Find the best single blocker for an attacker matching the given predicate.
     * Returns the cheapest matching blocker, or null if none qualifies.
     */
    private fun findSingleBlocker(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        validBlockers: List<EntityId>,
        assignedBlockers: Set<EntityId>,
        predicate: (BlockInfo) -> Boolean
    ): EntityId? {
        val aPower = projected.getPower(attacker) ?: 0
        val aToughness = projected.getToughness(attacker) ?: 0
        val aKeywords = projected.getKeywords(attacker)
        val aHasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords
        val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords

        return availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            .mapNotNull { blockerId ->
                val bPower = projected.getPower(blockerId) ?: 0
                val bToughness = projected.getToughness(blockerId) ?: 0
                val bKeywords = projected.getKeywords(blockerId)
                val bHasDeathtouch = Keyword.DEATHTOUCH.name in bKeywords
                val bHasFirstStrike = Keyword.FIRST_STRIKE.name in bKeywords || Keyword.DOUBLE_STRIKE.name in bKeywords
                val blockerValue = CombatMath.creatureValue(state, projected, blockerId)

                val weKillThem = bPower >= aToughness || bHasDeathtouch
                val bIsIndestructible = Keyword.INDESTRUCTIBLE.name in bKeywords
                val weSurvive = if (bIsIndestructible) {
                    true // indestructible creatures always survive combat
                } else if (!aHasDeathtouch) {
                    (bToughness > aPower) ||
                        (bHasFirstStrike && !aHasFirstStrike && weKillThem)
                } else {
                    false
                }
                val blockerDealsDamage = CombatMath.blockerDealsDamage(state, projected, attacker, blockerId)
                val effectivelyKillThem = weKillThem && blockerDealsDamage

                val info = BlockInfo(blockerId, effectivelyKillThem, weSurvive, blockerValue)
                if (predicate(info)) info else null
            }
            .minByOrNull { it.blockerValue }
            ?.blockerId
    }

    /**
     * Simulate a blocking plan through the engine's combat resolution and return
     * the board evaluation score from the blocker's perspective.
     *
     * Returns null if the simulation fails.
     */
    private fun evaluateBlockingPlan(
        state: GameState,
        playerId: EntityId,
        blockerMap: Map<EntityId, List<EntityId>>
    ): Double? {
        val blockAction = DeclareBlockers(playerId, blockerMap)
        val simResult = simulator.simulate(state, blockAction)
        if (simResult is SimulationResult.Illegal) return null

        var current = simResult.state

        // Drive through combat damage resolution
        var iterations = 0
        while (iterations < 50 && !current.gameOver && current.pendingDecision == null) {
            iterations++
            val priorityPlayer = current.priorityPlayerId ?: break
            current = simulator.simulate(current, PassPriority(priorityPlayer)).state
            if (current.phase != Phase.COMBAT) break
        }

        return evaluator.evaluate(current, current.projectedState, playerId)
    }

    /**
     * Check if unblocked damage would kill us — either this turn (immediate lethal)
     * or set us up to die on the opponent's next attack (next-turn lethal).
     *
     * The next-turn check considers all opponent creatures (they'll untap) vs our
     * available blockers, and returns true if the combined damage across both turns
     * would be fatal. This makes the AI block more aggressively when at low life.
     */
    private fun isLifeInDanger(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
        myLife: Int,
        playerId: EntityId
    ): Boolean {
        val incomingDamage = calculateIncomingDamage(state, projected, attackers, blockerMap)

        // Immediate lethal
        if (incomingDamage >= myLife) return true

        // Next-turn check: after taking this damage, would opponent's next attack kill us?
        val lifeAfter = myLife - incomingDamage
        val opponentId = state.getOpponent(playerId) ?: return false

        // Our blockers next turn: untapped creatures that aren't currently assigned to block
        // (conservatively — some may die in this combat, but this is a fast heuristic)
        val myBlockers = projected.getBattlefieldControlledBy(playerId)
            .filter { entityId ->
                projected.isCreature(entityId) &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true
            }

        val nextTurnDamage = CombatMath.estimateNextTurnDamage(state, projected, opponentId, myBlockers)
        if (nextTurnDamage > 0 && lifeAfter <= nextTurnDamage) return true

        return false
    }

    /**
     * Calculate damage that will get through given current blocking assignments.
     * Accounts for unblocked creatures and trample overflow.
     */
    private fun calculateIncomingDamage(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>
    ): Int {
        val blockedAttackerIds = blockerMap.values.flatten().toSet()
        var incomingDamage = 0
        for (attacker in attackers) {
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue
            if (attacker !in blockedAttackerIds) {
                incomingDamage += aPower
            } else {
                val aKeywords = projected.getKeywords(attacker)
                if (Keyword.TRAMPLE.name in aKeywords) {
                    val blockers = blockerMap.entries
                        .filter { (_, targets) -> attacker in targets }
                        .map { it.key }
                    val totalToughness = blockers.sumOf { projected.getToughness(it) ?: 0 }
                    val hasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords
                    val lethalToBlockers = if (hasDeathtouch) blockers.size else totalToughness
                    incomingDamage += (aPower - lethalToBlockers).coerceAtLeast(0)
                }
            }
        }
        return incomingDamage
    }



    // ── Lethal Analysis ─────────────────────────────────────────────────

    /**
     * Check if attacking with all creatures would be lethal even through optimal blocking.
     */
    private fun isLethalAttack(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        opponentBlockers: List<EntityId>,
        opponentLife: Int
    ): Boolean {
        val totalPower = attackers.sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
        if (totalPower < opponentLife) return false

        // Guaranteed evasive damage
        val evasiveDamage = CombatMath.calculateEvasiveDamage(state, projected, attackers, opponentBlockers)
        if (evasiveDamage >= opponentLife) return true

        // Full simulation of optimal blocking
        val damageThrough = CombatMath.calculateDamageThroughOptimalBlocking(
            state, projected, attackers, opponentBlockers
        )
        return damageThrough >= opponentLife
    }

    /**
     * Simulate a full attack with an arbitrary set of attackers: declare attackers,
     * pass priority through to declare blockers, let the blocking AI choose blocks,
     * then resolve through combat damage to get the post-combat state.
     *
     * Returns the post-combat GameState, or null if the simulation fails.
     */
    private fun simulateFullAttack(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        attackerMap: Map<EntityId, EntityId>
    ): GameState? {
        val attackAction = DeclareAttackers(playerId, attackerMap)
        val simResult = simulator.simulate(state, attackAction)
        if (simResult is SimulationResult.Illegal) return null
        var current = simResult.state

        // Drive through combat: pass priority, handle blockers, resolve damage.
        var iterations = 0
        var needsBlockerCheck = true
        while (iterations < 50 && !current.gameOver && current.pendingDecision == null) {
            iterations++
            val priorityPlayer = current.priorityPlayerId ?: break

            if (needsBlockerCheck && priorityPlayer == opponentId) {
                val legalActions = simulator.getLegalActions(current, opponentId)
                val blockAction = legalActions.find { it.actionType == "DeclareBlockers" }
                if (blockAction != null) {
                    val blockerAction = chooseBlockers(current, blockAction, opponentId, useSimulation = false)
                    current = simulator.simulate(current, blockerAction).state
                    needsBlockerCheck = false
                    continue
                }
            }

            current = simulator.simulate(current, PassPriority(priorityPlayer)).state
            if (current.phase != Phase.COMBAT) break
        }

        return current
    }

    /**
     * Evaluate an attack plan by simulating it through the engine.
     * Returns the board score from the attacker's perspective, or null on failure.
     */
    /**
     * Evaluate an attack plan by simulating it through the engine.
     * Returns the board score from the attacker's perspective, or null on failure.
     */
    private fun evaluateAttackPlan(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        attackerMap: Map<EntityId, EntityId>
    ): Double? {
        val postCombat = simulateFullAttack(state, playerId, opponentId, attackerMap)
        if (postCombat == null) return null
        return evaluator.evaluate(postCombat, postCombat.projectedState, playerId)
    }

    /**
     * Local search for attack plan: starting from heuristic seed, try add/remove
     * mutations and simulate each. Keep improvements until convergence.
     *
     * Also tries the "no attack" baseline to ensure attacking is better than passing.
     */
    private fun improveAttackViaLocalSearch(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        validAttackers: List<EntityId>,
        mandatoryAttackers: Set<EntityId>,
        attackerMap: MutableMap<EntityId, EntityId>,
        deadline: Long
    ) {
        // Baseline: use current board evaluation. Attack plans must beat this.
        val noAttackScore = evaluateAttackPlan(state, playerId, opponentId, emptyMap())
            ?: evaluator.evaluate(state, state.projectedState, playerId)
        var currentScore = if (attackerMap.isEmpty()) {
            noAttackScore
        } else {
            evaluateAttackPlan(state, playerId, opponentId, attackerMap) ?: return
        }

        // If seed plan is worse than not attacking, start from empty
        if (currentScore < noAttackScore && mandatoryAttackers.isEmpty()) {
            attackerMap.clear()
            currentScore = noAttackScore
        }

        val maxIterations = 3
        for (iteration in 1..maxIterations) {
            if (System.currentTimeMillis() > deadline) break
            var bestMutation: Map<EntityId, EntityId>? = null
            var bestScore = currentScore

            // Mutation 1: Add each non-attacking creature
            for (attacker in validAttackers) {
                if (System.currentTimeMillis() > deadline) break
                if (attacker in attackerMap) continue
                val mutation = attackerMap.toMutableMap()
                mutation[attacker] = opponentId
                val score = evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            // Mutation 2: Remove each non-mandatory attacker
            for (attacker in attackerMap.keys.toList()) {
                if (System.currentTimeMillis() > deadline) break
                if (attacker in mandatoryAttackers) continue
                val mutation = attackerMap.toMutableMap()
                mutation.remove(attacker)
                // For empty plan, use no-attack baseline score
                val score = if (mutation.isEmpty()) {
                    noAttackScore
                } else {
                    evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                }
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            if (bestMutation != null) {
                attackerMap.clear()
                attackerMap.putAll(bestMutation)
                currentScore = bestScore
            } else {
                break // converged
            }
        }
    }

    /** Return unassigned blockers that can legally block [attacker] (evasion + blocker restriction check). */
    private fun availableBlockersFor(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        validBlockers: List<EntityId>,
        assignedBlockers: Set<EntityId>
    ): List<EntityId> {
        return validBlockers.filter { it !in assignedBlockers && CombatMath.canBeBlockedBy(state, projected, attacker, it, cardRegistry) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun getAttackingCreatures(state: GameState): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
        }
    }
}
