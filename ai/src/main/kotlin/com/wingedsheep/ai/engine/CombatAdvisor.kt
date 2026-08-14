package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.LifeDifferential
import com.wingedsheep.ai.insight.CombatPlanTrace
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry(),
    /**
     * Price the estimated crack-back as life actually lost, instead of a flat penalty that only
     * fires when it is exactly lethal.
     *
     * The old rule is a cliff: at 5 life, four incoming damage costs **nothing** and five costs
     * 3.0. Everything between "free" and "dead" reads as free, so an attack plan that empties the
     * board of blockers is scored purely on the damage it deals — which is `race-03`, where the
     * AI sends the ground creature that was the only answer to the crack-back.
     *
     * When on, the penalty is `lifeValue(now) - lifeValue(now - incoming)` scaled by the
     * evaluator's own life weight. That is not a new guess: it is the same curve
     * [com.wingedsheep.ai.engine.evaluation.LifeDifferential] already uses, so anticipated damage
     * is charged at exactly the rate real damage is. It is continuous by construction, and still
     * dominant at lethal because `lifeValue` prices death at −100.
     */
    private val priceCrackBackAsLife: Boolean = false,
    /** The composite evaluator's `life` coefficient, so the two are in the same units. */
    private val lifeWeight: Double = 1.0,
) {
    companion object {
        /**
         * Max engine simulations for blocking local search — now a **floor**, not a ceiling.
         *
         * `DecisionBudget` derives the real allowance from the tier, and combat declaration is
         * always CRITICAL, so combat never gets less search than it did before Phase 4. This
         * constant is what `SearchAllowances.LEGACY` and every sub-NORMAL tier resolve to.
         */
        const val MAX_BLOCK_SIMULATIONS = 10
    }

    /**
     * Sum of the [CardAdvisor.attackPenalty]s declared for the creatures in an attack plan.
     *
     * Advisors use this to discourage attacking with a specific creature (a wall you
     * want back on defence, a creature whose value is its static ability). Returns 0.0
     * when no attacker has an advisor, which is the case for every card today.
     */
    private fun attackPenaltyFor(
        state: GameState,
        projected: ProjectedState,
        attackers: Collection<EntityId>,
        playerId: EntityId
    ): Double {
        if (attackers.isEmpty()) return 0.0
        return attackers.sumOf { entityId ->
            val name = state.getEntity(entityId)?.get<CardComponent>()?.name ?: return@sumOf 0.0
            val advisor = advisorRegistry.getAdvisor(name) ?: return@sumOf 0.0
            advisor.attackPenalty(state, projected, entityId, playerId) ?: 0.0
        }
    }

    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     *
     * Two-phase approach:
     * 1. Heuristic seed: always-attack creatures (evasive, vigilance, indestructible),
     *    plus lethal alpha-strike detection. Lives in [CombatSeed] since Phase 7, because a
     *    rollout playout needs the seed without the simulation-driven second phase.
     * 2. Local search: try adding/removing one attacker at a time, simulate each through
     *    the engine (opponent blocks via heuristic, combat resolves), keep improvements
     */
    fun chooseAttackers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        budget: DecisionBudget = DecisionBudget.legacy(),
        /** Local testing mode: collects the plans local search simulated. Null in production. */
        trace: CombatPlanTrace? = null,
    ): GameAction {
        val projected = state.projectedState
        val validAttackers = legalAction.validAttackers ?: emptyList()
        val mandatory = legalAction.mandatoryAttackers ?: emptyList()

        val seed = CombatSeed.attackers(
            state, projected, legalAction, playerId, cardRegistry,
            attackPenalty = { entityId ->
                attackPenaltyFor(state, projected, listOf(entityId), playerId)
            },
        )
        val opponentId = seed.defenderId ?: return DeclareAttackers(playerId, emptyMap())
        if (seed.lethal) return DeclareAttackers(playerId, seed.attackers)

        val seedMap = seed.attackers.toMutableMap()

        // ── Local search: try add/remove mutations via simulation ──
        // Only run if we're at DECLARE_ATTACKERS (simulation needs to submit DeclareAttackers).
        // Skip entirely when no opponent controls a creature: nothing can block, so combat is
        // deterministic, and with zero enemy creatures there's no crack-back to weigh — the
        // heuristic seed (attack with everything) is already optimal. Running a full-combat
        // simulation here only burns time without ever changing the plan.
        val enemyControlsCreature = state.turnOrder.any { other ->
            other != playerId && projected.getBattlefieldControlledBy(other).any { projected.isCreature(it) }
        }
        if (state.step == Step.DECLARE_ATTACKERS && enemyControlsCreature) {
            improveAttackViaLocalSearch(
                state, playerId, opponentId, validAttackers, mandatory.toSet(), seedMap, budget, trace
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
        useSimulation: Boolean = false,
        budget: DecisionBudget = DecisionBudget.legacy(),
        /** Local testing mode: collects the plans local search simulated. Null in production. */
        trace: CombatPlanTrace? = null,
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

        val myLife = state.lifeTotal(playerId)

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
        var bestMap = if (useSimulation) {
            improveViaLocalSearch(
                state, projected, playerId, attackers, validBlockers,
                mandatoryBlockerIds, seedMap, budget, trace
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

            val sortedUnblocked = unblockedAttackers.sortedByDescending { chumpPriority(projected, it) }

            for (attacker in sortedUnblocked) {
                val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }
                val cheapest = available.firstOrNull() ?: continue
                bestMap[cheapest] = listOf(attacker)
                assignedBlockers.add(cheapest)
            }

            // ── Survival pass: a plan that still lets lethal through is not a plan ──
            // Everything above prices a block by what it *wins* — a favourable trade, a gang block
            // that eats the best attacker — which is the right currency only if we get another turn
            // to spend the board on. When the leftovers are lethal we do not, and two blockers
            // standing in front of one creature while a bigger one walks in unblocked is a loss
            // however good the trade reads. So rebuild the assignment for damage *prevented*, and
            // adopt it only when it genuinely saves us: positions the AI already survives keep the
            // plan they had.
            if (calculateIncomingDamage(state, projected, attackers, bestMap) >= myLife) {
                val survival = damageMinimisingPlan(state, projected, attackers, validBlockers, mandatoryMap)
                if (calculateIncomingDamage(state, projected, attackers, survival) < myLife) {
                    bestMap = survival
                    assignedBlockers.clear()
                    assignedBlockers.addAll(survival.keys)
                }
            }
        }

        // ── Proactive chump-block: if facing lethal within 2 turns, sacrifice low-value creatures ──
        // Only do this when NOT already facing immediate lethal (that's handled above).
        if (incomingDamage < myLife) {
            val inDanger = isLifeInDanger(state, projected, attackers, bestMap, myLife, playerId)
            if (inDanger) {
                val unblockedAttackers = attackers
                    .filter { attacker -> bestMap.values.none { attacker in it } }
                    .sortedByDescending { chumpPriority(projected, it) }

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

        return DeclareBlockers(
            playerId,
            legalizeBlockerPlan(state, playerId, bestMap, mandatoryBlockerIds)
        )
    }

    /**
     * Pairwise combat checks cannot see assignment-wide restrictions such as "can't be blocked by
     * more than one creature". Validate the completed plan through the authoritative engine and
     * peel off optional assignments until it is legal. Mandatory assignments are preserved.
     */
    private fun legalizeBlockerPlan(
        state: GameState,
        playerId: EntityId,
        proposed: Map<EntityId, List<EntityId>>,
        mandatoryBlockerIds: Set<EntityId>,
    ): Map<EntityId, List<EntityId>> {
        val candidate = proposed.toMutableMap()
        while (simulator.simulate(state, DeclareBlockers(playerId, candidate)) is SimulationResult.Illegal) {
            val removable = candidate.keys
                .filterNot { it in mandatoryBlockerIds }
                .minByOrNull { CombatMath.creatureValue(state, state.projectedState, it) }
                ?: return candidate.filterKeys { it in mandatoryBlockerIds }
            candidate.remove(removable)
        }
        return candidate
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
     * Caps at `budget.allowances.blockSimulations` total simulations to keep decision time
     * bounded — [MAX_BLOCK_SIMULATIONS] is that allowance's floor, so blocking never searches
     * less than it did before the budget existed.
     */
    private fun improveViaLocalSearch(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryBlockerIds: Set<EntityId>,
        seedMap: MutableMap<EntityId, List<EntityId>>,
        budget: DecisionBudget,
        trace: CombatPlanTrace? = null,
    ): MutableMap<EntityId, List<EntityId>> {
        var currentPlan = seedMap.toMutableMap()
        var currentScore = evaluateBlockingPlan(state, playerId, currentPlan) ?: return currentPlan
        trace?.recordBlock(currentPlan, currentScore)
        var simulationsLeft = budget.allowances.blockSimulations
        val deadline = budget.combatDeadlineNanos

        val maxIterations = 2
        for (iteration in 1..maxIterations) {
            if (simulationsLeft <= 0 || System.nanoTime() > deadline) break

            val mutations = generateBlockMutations(
                state, projected, attackers, validBlockers,
                mandatoryBlockerIds, currentPlan
            )

            var bestMutation: MutableMap<EntityId, List<EntityId>>? = null
            var bestScore = currentScore

            for (mutation in mutations) {
                if (simulationsLeft <= 0 || System.nanoTime() > deadline) break
                simulationsLeft--
                val score = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                trace?.recordBlock(mutation, score)
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

                    // Cost of the gang-block = sum of every blocker the attacker can kill
                    // with its power (MTG rule 702.19c: attacker assigns lethal damage in order).
                    // The attacker picks the order that kills as many blockers as possible, so
                    // start from the lowest-toughness blocker.
                    val blockersByToughness = listOf(b1, b2)
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
        // Scored against the opponent it pays off best against — in a pod we get to choose who to
        // swing at, so the counter-attack is worth what its *best* target is worth. In 1v1 there is
        // one opponent and this is the original single calculation.
        val myAttackers = CombatMath.getCreaturesThatCanAttack(current, postProjected, playerId)
        val counterAttackBonus = current.getOpponents(playerId).maxOfOrNull { opponentId ->
            val opponentBlockers = CombatMath.getOpponentUntappedCreatures(current, postProjected, opponentId)
            val ourDamageThrough = if (myAttackers.isNotEmpty()) {
                CombatMath.calculateDamageThroughOptimalBlocking(current, postProjected, myAttackers, opponentBlockers)
            } else 0
            val opponentLife = current.lifeTotal(opponentId)

            // Small bonus for blocking plans that preserve our attack potential.
            // Nudges the AI toward blocks that keep our counter-attack alive.
            if (ourDamageThrough >= opponentLife) {
                3.0
            } else if (ourDamageThrough > 0) {
                ourDamageThrough.toDouble() * 0.15
            } else {
                0.0
            }
        } ?: 0.0

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

        // Our blockers next turn: untapped creatures that aren't currently assigned to block
        // (conservatively — some may die in this combat, but this is a fast heuristic)
        val myBlockers = projected.getBattlefieldControlledBy(playerId)
            .filter { entityId ->
                projected.isCreature(entityId) &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true
            }

        val nextTurnDamage = incomingNextTurnDamage(state, projected, playerId, myBlockers)
        if (nextTurnDamage > 0 && lifeAfter <= nextTurnDamage) return true

        return false
    }

    /**
     * Damage the most dangerous opposing *side* can push through [myBlockers] on its next turn.
     *
     * Summed within a team (CR 805.10 — teammates attack in one combat, against one set of
     * blockers) but maxed across teams, because opposing teams attack on separate turns and we
     * untap in between. In a two-player game this is one team of one player: the original call.
     */
    private fun incomingNextTurnDamage(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        myBlockers: List<EntityId>
    ): Int {
        val sides = state.sidesFor(playerId) ?: return 0
        return sides.opponents.maxOf { team ->
            team.sumOf { CombatMath.estimateNextTurnDamage(state, projected, it, myBlockers) }
        }
    }

    /**
     * How much a chump block on [attacker] is worth, as a sort key: the damage it takes off the
     * table, biggest first.
     *
     * A trampler keeps hitting us for everything the blocker cannot soak, so a chump in front of
     * one buys a point or two; a chump in front of anything else buys the whole hit. Scaling
     * separates the two groups outright rather than trying to price them on one scale — no
     * trampler is ever worth chumping ahead of a non-trampler.
     */
    private fun chumpPriority(projected: ProjectedState, attacker: EntityId): Int {
        val power = projected.getPower(attacker) ?: 0
        return if (Keyword.TRAMPLE.name in projected.getKeywords(attacker)) -power else power * 1000
    }

    /**
     * Assign blockers for damage prevented rather than for value: the biggest hits first, paid for
     * with the cheapest legal body that can stand in front of each.
     *
     * Only [mandatoryMap] is carried over — every other blocker is put back in the pool, which is
     * the point. This is the plan for a turn we do not survive otherwise, so a blocker committed to
     * a trade or to the second half of a gang block is a blocker standing in the wrong place.
     */
    private fun damageMinimisingPlan(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryMap: Map<EntityId, List<EntityId>>,
    ): MutableMap<EntityId, List<EntityId>> {
        val plan = mandatoryMap.toMutableMap()
        val assigned = plan.keys.toMutableSet()
        val alreadyBlocked = plan.values.flatten().toSet()

        val byDamage = attackers
            .filter { it !in alreadyBlocked }
            .sortedByDescending { chumpPriority(projected, it) }

        for (attacker in byDamage) {
            val keywords = projected.getKeywords(attacker)
            // Menace wants two bodies or none — a lone blocker on it is an illegal plan that
            // `fixMenaceAssignments` would strip right back out, damage and all.
            val needed = if (Keyword.MENACE.name in keywords) 2 else 1
            val available = availableBlockersFor(state, projected, attacker, validBlockers, assigned)
                .sortedWith(
                    if (Keyword.TRAMPLE.name in keywords) {
                        // Trample spills whatever the blockers cannot soak, so here the wall is
                        // worth more than the cheap body.
                        compareByDescending<EntityId> { projected.getToughness(it) ?: 0 }
                            .thenBy { CombatMath.creatureValue(state, projected, it) }
                    } else {
                        compareBy { CombatMath.creatureValue(state, projected, it) }
                    }
                )
            if (available.size < needed) continue
            available.take(needed).forEach { blocker ->
                plan[blocker] = listOf(attacker)
                assigned.add(blocker)
            }
        }
        return plan
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

        // Per-card advisor penalties, read off the pre-combat state (the attackers may
        // be dead by now). Deliberately not applied to the lethal alpha strike in
        // [chooseAttackers] — a kill beats any advisor preference.
        val advisorPenalty = attackPenaltyFor(state, state.projectedState, attackerMap.keys, playerId)

        // Estimate next-turn counter-attack: what damage can the opponent deal through our blocks?
        val myBlockers = postProjected.getBattlefieldControlledBy(playerId).filter { entityId ->
            postProjected.isCreature(entityId) &&
                postCombat.getEntity(entityId)?.has<TappedComponent>() != true
        }
        val nextTurnDamage = incomingNextTurnDamage(postCombat, postProjected, playerId, myBlockers)
        val myLife = postCombat.lifeTotal(playerId)

        // Penalty for plans that hand the opponent a crack-back. See [priceCrackBackAsLife] for
        // why the flat version is a cliff and what replaces it.
        val crackBackPenalty = if (priceCrackBackAsLife) {
            val after = myLife - nextTurnDamage
            -(LifeDifferential.lifeValue(myLife) - LifeDifferential.lifeValue(after)) * lifeWeight
        } else if (nextTurnDamage >= myLife) {
            -3.0
        } else {
            0.0
        }

        return baseScore + crackBackPenalty - advisorPenalty
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
        budget: DecisionBudget,
        trace: CombatPlanTrace? = null,
    ) {
        val deadline = budget.combatDeadlineNanos
        // Baseline: use current board evaluation. Attack plans must beat this.
        val noAttackScore = evaluateAttackPlan(state, playerId, opponentId, emptyMap())
            ?: evaluator.evaluate(state, state.projectedState, playerId)
        trace?.recordAttack(emptyMap(), noAttackScore)
        var currentScore = if (attackerMap.isEmpty()) {
            noAttackScore
        } else {
            evaluateAttackPlan(state, playerId, opponentId, attackerMap) ?: return
        }
        trace?.recordAttack(attackerMap, currentScore)

        // If seed plan is worse than not attacking, start from empty
        if (currentScore < noAttackScore && mandatoryAttackers.isEmpty()) {
            attackerMap.clear()
            currentScore = noAttackScore
        }

        val maxIterations = budget.allowances.attackSearchIterations
        for (iteration in 1..maxIterations) {
            if (System.nanoTime() > deadline) break
            var bestMutation: Map<EntityId, EntityId>? = null
            var bestScore = currentScore

            // Mutation 1: Add each non-attacking creature
            for (attacker in validAttackers) {
                if (System.nanoTime() > deadline) break
                if (attacker in attackerMap) continue
                val mutation = attackerMap.toMutableMap()
                mutation[attacker] = opponentId
                val score = evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                trace?.recordAttack(mutation, score)
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            // Mutation 2: Remove each non-mandatory attacker
            for (attacker in attackerMap.keys.toList()) {
                if (System.nanoTime() > deadline) break
                if (attacker in mandatoryAttackers) continue
                val mutation = attackerMap.toMutableMap()
                mutation.remove(attacker)
                // For empty plan, use no-attack baseline score
                val score = if (mutation.isEmpty()) {
                    noAttackScore
                } else {
                    evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                }
                trace?.recordAttack(mutation, score)
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
