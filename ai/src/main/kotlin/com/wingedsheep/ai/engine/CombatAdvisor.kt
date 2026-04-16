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
    companion object {
        /** Max engine simulations for blocking local search. Keeps decision time bounded. */
        const val MAX_BLOCK_SIMULATIONS = 10
    }

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

            // Attack if we survive any single blocker
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

            // Attack if every blocking option is worse for the opponent than taking the damage
            // (e.g., a 3/3 into a board of 2/2s — opponent must take 3 or trade down)
            // Accept even trades when we have more creatures — trading favors the larger army
            if (CombatMath.isProfitableAttack(
                    state, projected, entityId, opponentCreatures, cardRegistry,
                    myCreatureCount = validAttackers.size,
                    opponentCreatureCount = opponentCreatures.size
                )) {
                seedMap[entityId] = opponentId
                continue
            }
        }

        // Attack if we have more attackers than they have blockers (excess gets through)
        if (seedMap.size < validAttackers.size) {
            val unblockedSlots = validAttackers.size - opponentCreatures.size
            if (unblockedSlots > 0) {
                // Sort remaining by value ascending — send cheapest creatures first,
                // hold back the most valuable ones in case we need a blocker
                val remaining = validAttackers
                    .filter { it !in seedMap && (projected.getPower(it) ?: 0) > 0 }
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }

                // Estimate opponent's crack-back damage through our optimal blocking.
                // Accounts for evasion — flying creatures we can't block deal guaranteed damage.
                val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
                val allOpponentAttackers = projected.getBattlefieldControlledBy(opponentId).filter {
                    projected.isCreature(it) && Keyword.DEFENDER.name !in projected.getKeywords(it)
                }
                val myPotentialBlockers = validAttackers.filter { it !in seedMap }
                val crackBackDamage = if (allOpponentAttackers.isNotEmpty()) {
                    CombatMath.calculateDamageThroughOptimalBlocking(
                        state, projected, allOpponentAttackers, myPotentialBlockers
                    )
                } else 0

                // If opponent threatens near-lethal damage next turn, hold back our best blocker.
                // Prefer deathtouch creatures as hold-backs (they trade with anything).
                val holdBack = if (crackBackDamage > 0 && myLife <= crackBackDamage * 1.5) {
                    remaining.maxByOrNull { entityId ->
                        val keywords = projected.getKeywords(entityId)
                        val hasDeathtouch = Keyword.DEATHTOUCH.name in keywords
                        val toughness = projected.getToughness(entityId) ?: 0
                        if (hasDeathtouch) 1000 + toughness else toughness
                    }
                } else null

                for (entityId in remaining) {
                    if (entityId != holdBack) {
                        seedMap[entityId] = opponentId
                    }
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

        // ── Chump-block pass: if facing immediate lethal, assign remaining blockers ──
        // Only chump-block when unblocked damage would kill us THIS turn.
        // Don't chump-block for next-turn lethal — losing a blocker now makes next turn worse.
        val assignedBlockers = bestMap.keys.toMutableSet()
        val incomingDamage = calculateIncomingDamage(state, projected, attackers, bestMap)
        if (incomingDamage >= myLife) {
            val unblockedAttackers = attackers
                .filter { attacker -> bestMap.values.none { attacker in it } }

            // Sort by damage actually prevented by a chump block: non-tramplers first
            // (chump blocks all damage) then tramplers (only prevents blocker toughness).
            // Among each group, prefer blocking higher-power attackers.
            val sortedUnblocked = unblockedAttackers.sortedByDescending { attacker ->
                val aKeywords = projected.getKeywords(attacker)
                val aPower = projected.getPower(attacker) ?: 0
                if (Keyword.TRAMPLE.name in aKeywords) {
                    // Trample: chump only prevents ~1-2 damage (blocker toughness)
                    // Use negative power so tramplers sort after non-tramplers
                    aPower * -1
                } else {
                    // Non-trampler: chump prevents ALL damage
                    aPower * 1000
                }
            }

            for (attacker in sortedUnblocked) {
                val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }
                val cheapest = available.firstOrNull() ?: continue
                bestMap[cheapest] = listOf(attacker)
                assignedBlockers.add(cheapest)
            }
        }

        // ── Proactive chump-block: if facing lethal within 2 turns, sacrifice low-value creatures ──
        // Only do this when NOT already facing immediate lethal (that's handled above).
        if (incomingDamage < myLife) {
            val inDanger = isLifeInDanger(state, projected, attackers, bestMap, myLife, playerId)
            if (inDanger) {
                val unblockedAttackers = attackers
                    .filter { attacker -> bestMap.values.none { attacker in it } }
                    .sortedByDescending { attacker ->
                        val aKeywords = projected.getKeywords(attacker)
                        val aPower = projected.getPower(attacker) ?: 0
                        if (Keyword.TRAMPLE.name in aKeywords) aPower * -1 else aPower * 1000
                    }

                for (attacker in unblockedAttackers) {
                    val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                        .filter { CombatMath.creatureValue(state, projected, it) < 2.0 }
                        .sortedBy { CombatMath.creatureValue(state, projected, it) }
                    val cheapest = available.firstOrNull() ?: continue
                    bestMap[cheapest] = listOf(attacker)
                    assignedBlockers.add(cheapest)
                }
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
     * Local search: starting from the heuristic seed plan, try targeted mutations
     * and simulate each through the engine. Keep improvements until convergence.
     *
     * Instead of exhaustively trying all possible mutations (O(B×A) simulations),
     * generates only the most promising candidates using combat math as a filter.
     * Each candidate is then validated via full engine simulation to catch triggers,
     * replacement effects, and keyword interactions that math alone would miss.
     *
     * Caps at [MAX_BLOCK_SIMULATIONS] total simulations to keep decision time bounded.
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
        var currentPlan = seedMap.toMutableMap()
        var currentScore = evaluateBlockingPlan(state, playerId, currentPlan) ?: return currentPlan
        var simulationsLeft = MAX_BLOCK_SIMULATIONS

        val maxIterations = 2
        for (iteration in 1..maxIterations) {
            if (simulationsLeft <= 0) break

            val mutations = generateBlockMutations(
                state, projected, attackers, validBlockers,
                mandatoryBlockerIds, currentPlan
            )

            var bestMutation: MutableMap<EntityId, List<EntityId>>? = null
            var bestScore = currentScore

            for (mutation in mutations) {
                if (simulationsLeft <= 0) break
                simulationsLeft--
                val score = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            if (bestMutation != null) {
                currentPlan = bestMutation
                currentScore = bestScore
            } else {
                break // converged
            }
        }

        return currentPlan
    }

    /**
     * Generate a small set of promising blocking mutations from the current plan.
     * Uses combat math to filter out obviously bad candidates before expensive simulation.
     *
     * Returns at most ~8-12 candidate plans.
     */
    private fun generateBlockMutations(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryBlockerIds: Set<EntityId>,
        currentPlan: Map<EntityId, List<EntityId>>
    ): List<MutableMap<EntityId, List<EntityId>>> {
        val mutations = mutableListOf<MutableMap<EntityId, List<EntityId>>>()

        val blockedAttackerIds = currentPlan.values.flatten().toSet()
        val assignedBlockerIds = currentPlan.keys

        // 1. Remove: try removing blockers assigned to low-value attackers
        //    (the heuristic might have over-blocked)
        for (blockerId in currentPlan.keys) {
            if (blockerId in mandatoryBlockerIds) continue
            val targetAttacker = currentPlan[blockerId]?.firstOrNull() ?: continue
            val blockerValue = CombatMath.creatureValue(state, projected, blockerId)
            val attackerValue = CombatMath.creatureValue(state, projected, targetAttacker)
            // Only try removing if the blocker is more valuable than what it's blocking
            // or if the blocker doesn't kill the attacker
            val kills = CombatMath.wouldKillInCombat(state, projected, blockerId, targetAttacker)
            if (blockerValue > attackerValue || !kills) {
                val mutation = currentPlan.toMutableMap()
                mutation.remove(blockerId)
                mutations.add(mutation)
            }
        }

        // 2. Add: try assigning unassigned blockers to unblocked attackers
        //    (the heuristic might have under-blocked)
        val unblockedAttackers = attackers.filter { it !in blockedAttackerIds }
            .sortedByDescending { CombatMath.effectiveDamage(projected, it) }
        val unassignedBlockers = validBlockers.filter { it !in assignedBlockerIds }
            .sortedBy { CombatMath.creatureValue(state, projected, it) }

        for (attacker in unblockedAttackers.take(3)) {
            for (blocker in unassignedBlockers.take(3)) {
                if (!CombatMath.canBeBlockedBy(state, projected, attacker, blocker, cardRegistry)) continue
                val mutation = currentPlan.toMutableMap()
                mutation[blocker] = listOf(attacker)
                mutations.add(mutation)
            }
        }

        // 3. Move: try moving a blocker from a low-value assignment to an unblocked attacker
        if (unblockedAttackers.isNotEmpty()) {
            val worstAssignment = currentPlan.entries
                .filter { it.key !in mandatoryBlockerIds }
                .minByOrNull { (_, targets) ->
                    val attacker = targets.firstOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    CombatMath.creatureValue(state, projected, attacker)
                }
            if (worstAssignment != null) {
                val bestUnblocked = unblockedAttackers.first()
                if (CombatMath.canBeBlockedBy(state, projected, bestUnblocked, worstAssignment.key, cardRegistry)) {
                    val mutation = currentPlan.toMutableMap()
                    mutation[worstAssignment.key] = listOf(bestUnblocked)
                    mutations.add(mutation)
                }
            }
        }

        return mutations
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
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.effectivelyKillThem && !info.weSurvive &&
                    info.blockerValue * tradeRatio <= attackerValue
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 3: Damage prevention — blocker survives, always block ──
        // If the blocker survives, the cost is essentially zero (blocking doesn't tap).
        // Always take free damage prevention.
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.weSurvive
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
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

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
                        // Check if surviving blockers still have enough power to kill the attacker.
                        // A blocker that dies to first strike is fine if the other one finishes the job.
                        // Also count power from a dying blocker if it has first/double strike itself
                        // (it deals damage simultaneously in the first-strike step).
                        val b1Keywords = projected.getKeywords(b1)
                        val b2Keywords = projected.getKeywords(b2)
                        val b1HasFS = Keyword.FIRST_STRIKE.name in b1Keywords || Keyword.DOUBLE_STRIKE.name in b1Keywords
                        val b2HasFS = Keyword.FIRST_STRIKE.name in b2Keywords || Keyword.DOUBLE_STRIKE.name in b2Keywords
                        val survivingPower =
                            (if (b1Survives || b1HasFS) projected.getPower(b1) ?: 0 else 0) +
                            (if (b2Survives || b2HasFS) projected.getPower(b2) ?: 0 else 0)
                        if (survivingPower < aToughness) continue
                    }

                    val dyingBlocker = if ((projected.getToughness(b1) ?: 0) <= aPower) b1 else b2
                    val dyingValue = CombatMath.combatTradeValue(projected, dyingBlocker)
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

        // ── Pass 5: Triple gang blocks for large creatures that pairs can't kill ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aKeywords = projected.getKeywords(attacker)
            if (Keyword.DEATHTOUCH.name in aKeywords) continue

            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            if (available.size < 3) continue

            // Cap candidates to keep combinatorics reasonable (C(8,3) = 56)
            val sorted = available
                .sortedBy { CombatMath.combatTradeValue(projected, it) }
                .take(8)

            var found = false
            for (i in sorted.indices) {
                if (found) break
                for (j in i + 1 until sorted.size) {
                    if (found) break
                    for (k in j + 1 until sorted.size) {
                        val b1 = sorted[i]; val b2 = sorted[j]; val b3 = sorted[k]
                        val combinedPower = (projected.getPower(b1) ?: 0) +
                            (projected.getPower(b2) ?: 0) + (projected.getPower(b3) ?: 0)
                        if (combinedPower < aToughness) continue

                        if (aHasFirstStrike) {
                            val survivors = listOf(b1, b2, b3).filter { blocker ->
                                !CombatMath.wouldKillInCombat(state, projected, attacker, blocker) ||
                                    Keyword.FIRST_STRIKE.name in projected.getKeywords(blocker) ||
                                    Keyword.DOUBLE_STRIKE.name in projected.getKeywords(blocker)
                            }
                            val survivingPower = survivors.sumOf { projected.getPower(it) ?: 0 }
                            if (survivingPower < aToughness) continue
                        }

                        // Value check: attacker must be worth more than the blockers we lose.
                        // Use combatTradeValue (ignores tapped/sickness state multipliers).
                        val blockersByToughness = listOf(b1, b2, b3)
                            .sortedBy { projected.getToughness(it) ?: 0 }
                        var damageLeft = aPower
                        var dyingValue = 0.0
                        for (b in blockersByToughness) {
                            val bToughness = projected.getToughness(b) ?: 0
                            if (damageLeft >= bToughness) {
                                dyingValue += CombatMath.combatTradeValue(projected, b)
                                damageLeft -= bToughness
                            }
                        }
                        if (attackerValue > dyingValue * 1.2) {
                            blockerMap[b1] = listOf(attacker)
                            blockerMap[b2] = listOf(attacker)
                            blockerMap[b3] = listOf(attacker)
                            assignedBlockers.add(b1)
                            assignedBlockers.add(b2)
                            assignedBlockers.add(b3)
                            blockedAttackers.add(attacker)
                            found = true
                        }
                    }
                }
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
                val blockerValue = CombatMath.combatTradeValue(projected, blockerId)

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

        val postProjected = current.projectedState
        val baseScore = evaluator.evaluate(current, postProjected, playerId)

        // Estimate our next-turn attack potential: what damage can we push through?
        val opponentId = state.getOpponent(playerId) ?: return baseScore
        val myAttackers = CombatMath.getCreaturesThatCanAttack(current, postProjected, playerId)
        val opponentBlockers = CombatMath.getOpponentUntappedCreatures(current, postProjected, opponentId)
        val ourDamageThrough = if (myAttackers.isNotEmpty()) {
            CombatMath.calculateDamageThroughOptimalBlocking(current, postProjected, myAttackers, opponentBlockers)
        } else 0
        val opponentLife = current.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20

        // Small bonus for blocking plans that preserve our attack potential.
        // Nudges the AI toward blocks that keep our counter-attack alive.
        val counterAttackBonus = if (ourDamageThrough >= opponentLife) {
            3.0
        } else if (ourDamageThrough > 0) {
            ourDamageThrough.toDouble() * 0.15
        } else {
            0.0
        }

        return baseScore + counterAttackBonus
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
     * After this combat resolves, also estimates the opponent's counter-attack
     * on their next turn using [CombatMath] (no recursive simulation — just math).
     *
     * Returns the board score from the attacker's perspective, or null on failure.
     */
    private fun evaluateAttackPlan(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        attackerMap: Map<EntityId, EntityId>
    ): Double? {
        val postCombat = simulateFullAttack(state, playerId, opponentId, attackerMap)
            ?: return null
        val postProjected = postCombat.projectedState
        val baseScore = evaluator.evaluate(postCombat, postProjected, playerId)

        // Estimate next-turn counter-attack: what damage can the opponent deal through our blocks?
        val myBlockers = postProjected.getBattlefieldControlledBy(playerId).filter { entityId ->
            postProjected.isCreature(entityId) &&
                postCombat.getEntity(entityId)?.has<TappedComponent>() != true
        }
        val nextTurnDamage = CombatMath.estimateNextTurnDamage(postCombat, postProjected, opponentId, myBlockers)
        val myLife = postCombat.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20

        // Light penalty for plans that leave us dead to the crack-back.
        // Keep this small — the base evaluator already scores life totals and threats.
        // This just nudges the AI to prefer attack plans that don't leave us wide open.
        val crackBackPenalty = if (nextTurnDamage >= myLife) {
            -3.0
        } else {
            0.0
        }

        return baseScore + crackBackPenalty
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
