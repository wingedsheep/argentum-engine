package com.wingedsheep.engine.ai

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
    private val cardRegistry: CardRegistry? = null
) {
    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     *
     * Uses a global aggression level (0-5) to gate per-creature attack decisions,
     * inspired by Forge's combat AI. The aggression level is derived from race analysis
     * (life-to-damage ratios) and attritional attack simulation.
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

        // Identify planeswalker targets
        val planeswalkerTargets = defendingPlayers.filter { it != opponentId && projected.isPlaneswalker(it) }

        // ── Lethal check: only alpha-strike if damage actually gets through ──
        if (isLethalAttack(state, projected, validAttackers, opponentCreatures, opponentLife)) {
            return DeclareAttackers(playerId, validAttackers.associateWith { opponentId })
        }

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        val myCreatures = projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) && state.getEntity(it)?.has<TappedComponent>() != true }

        // ── Global aggression level (0-5) ──
        val aggression = CombatMath.calculateAggressionLevel(
            state, projected, playerId, opponentId, myCreatures, opponentCreatures
        )

        // At aggression 0, don't attack at all
        if (aggression == 0) return DeclareAttackers(playerId, emptyMap())

        // ── Combat trick estimation ──
        val (trickPowerBonus, trickToughnessBonus) = CombatMath.estimateCombatTrickBonus(state, projected, opponentId)

        // ── Crackback estimation using creatures that can actually attack ──
        val opponentCrackbackCreatures = CombatMath.getCreaturesThatCanAttack(state, projected, opponentId)

        // ── Life-scaled trade willingness ──
        val tradeRatio = CombatMath.tradeWillingnessRatio(myLife)

        val attackerMap = mutableMapOf<EntityId, EntityId>()
        val simulationDeadline = System.currentTimeMillis() + 2000 // 2 second budget
        for (entityId in validAttackers) {
            // If we've exceeded the time budget, fall back to fast-path only (no more simulations)
            if (System.currentTimeMillis() > simulationDeadline) break

            val target = chooseAttackTarget(
                state, projected, entityId, opponentId, opponentCreatures,
                opponentLife, planeswalkerTargets, aggression,
                trickPowerBonus, trickToughnessBonus, tradeRatio
            )
            if (target != null) {
                attackerMap[entityId] = target
            }
        }

        // ── Defense budget: retain blockers against crackback ──
        if (attackerMap.isNotEmpty()) {
            retainDefenders(state, projected, playerId, opponentId, opponentCrackbackCreatures, attackerMap, myLife)
        }

        return DeclareAttackers(playerId, attackerMap)
    }

    /**
     * Build a DeclareBlockers action choosing which creatures block which attackers.
     *
     * Uses a three-pass escalation system inspired by Forge:
     * - Pass 1 (Normal): good blocks → gang blocks → profit trades
     * - Pass 2 (Emergency): if still lethal, clear and re-run with chump blocks + trades first
     * - Pass 3 (Desperate): if still lethal, clear and prioritize chump blocks above all else
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

        val attackers = getAttackingCreatures(state)
        if (attackers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20

        val blockerMap = mutableMapOf<EntityId, List<EntityId>>()
        val assignedBlockers = mutableSetOf<EntityId>()
        val blockedAttackers = mutableSetOf<EntityId>()

        // Handle mandatory blockers (preserved across all passes)
        for ((blockerId, mustBlockAttackers) in mandatory) {
            if (mustBlockAttackers.isNotEmpty()) {
                blockerMap[blockerId] = listOf(mustBlockAttackers.first())
                assignedBlockers.add(blockerId)
                blockedAttackers.add(mustBlockAttackers.first())
            }
        }

        // ── Pass 1 (Normal): good blocks, then gang blocks, then profit trades ──
        assignBlocksForProfit(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        assignGangBlocks(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)

        if (!isLifeInDanger(state, projected, attackers, blockerMap, myLife)) {
            return DeclareBlockers(playerId, blockerMap)
        }

        // ── Pass 2 (Emergency): clear non-mandatory blocks, chump + trade + good blocks ──
        clearNonMandatoryBlocks(mandatory, blockerMap, assignedBlockers, blockedAttackers)
        makeChumpBlocks(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        assignBlocksForProfit(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        assignGangBlocks(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        reinforceBlocksAgainstTrample(state, projected, validBlockers, attackers, assignedBlockers, blockerMap)

        if (!isLifeInDanger(state, projected, attackers, blockerMap, myLife)) {
            return DeclareBlockers(playerId, blockerMap)
        }

        // ── Pass 3 (Desperate): clear and prioritize chump blocks, then survival blocks ──
        clearNonMandatoryBlocks(mandatory, blockerMap, assignedBlockers, blockedAttackers)
        assignBlocksForSurvival(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        makeChumpBlocks(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        reinforceBlocksAgainstTrample(state, projected, validBlockers, attackers, assignedBlockers, blockerMap)

        return DeclareBlockers(playerId, blockerMap)
    }

    /**
     * Check if unblocked damage would kill us after current blocking assignments.
     */
    private fun isLifeInDanger(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
        myLife: Int
    ): Boolean {
        val blockedAttackerIds = blockerMap.values.flatten().toSet()
        var incomingDamage = 0
        for (attacker in attackers) {
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue
            if (attacker !in blockedAttackerIds) {
                // Unblocked — full damage
                incomingDamage += aPower
            } else {
                // Blocked — only trample overflow gets through
                val aKeywords = projected.getKeywords(attacker)
                if (Keyword.TRAMPLE.name in aKeywords) {
                    // Find all blockers assigned to this attacker
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
        return incomingDamage >= myLife
    }

    /**
     * Clear all non-mandatory block assignments for re-evaluation in escalation passes.
     */
    private fun clearNonMandatoryBlocks(
        mandatory: Map<EntityId, List<EntityId>>,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>
    ) {
        val mandatoryBlockerIds = mandatory.keys
        val toRemove = blockerMap.keys.filter { it !in mandatoryBlockerIds }
        for (blockerId in toRemove) {
            val targets = blockerMap.remove(blockerId) ?: continue
            assignedBlockers.remove(blockerId)
            // Only unmark attacker if no other blocker is assigned to it
            for (target in targets) {
                val stillBlocked = blockerMap.values.any { target in it }
                if (!stillBlocked) blockedAttackers.remove(target)
            }
        }
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

    // ── Attack decision ─────────────────────────────────────────────────

    /**
     * Decide whether a creature should attack based on the global aggression level.
     *
     * Aggression levels gate which creatures are sent:
     * - 5: Everything attacks (alpha strike / attritional win)
     * - 4: Attack if we survive or trade reasonably (life-scaled)
     * - 3: Attack if safe, evasive, or decent trade
     * - 2: Attack only if safe or evasive
     * - 1: Only evasive / no-downside creatures
     * - 0: Nothing attacks (handled in chooseAttackers)
     */
    private fun chooseAttackTarget(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        opponentId: EntityId,
        opponentCreatures: List<EntityId>,
        opponentLife: Int,
        planeswalkerTargets: List<EntityId>,
        aggression: Int,
        trickPowerBonus: Int,
        trickToughnessBonus: Int,
        tradeRatio: Double
    ): EntityId? {
        val power = projected.getPower(entityId) ?: 0
        val keywords = projected.getKeywords(entityId)

        if (power <= 0) return null

        val isEvasive = CombatMath.isEvasive(state, projected, entityId, opponentCreatures)

        // ── Fast paths: always attack (any aggression >= 1) ──
        if (Keyword.VIGILANCE.name in keywords) {
            return chooseBestTarget(state, projected, entityId, opponentId, planeswalkerTargets, isEvasive)
        }
        if (Keyword.INDESTRUCTIBLE.name in keywords) {
            return chooseBestTarget(state, projected, entityId, opponentId, planeswalkerTargets, isEvasive)
        }
        if (opponentCreatures.isEmpty()) {
            return chooseBestTarget(state, projected, entityId, opponentId, planeswalkerTargets, isEvasive)
        }
        if (isEvasive) {
            return chooseBestTarget(state, projected, entityId, opponentId, planeswalkerTargets, true)
        }

        if (aggression <= 1) return null // Only evasive/no-downside (handled above)
        if (aggression >= 5) return opponentId // All-out attack

        // ── Simulate: attack with this creature, let opponent block, resolve combat ──
        val baselineScore = evaluator.evaluate(state, projected, state.turnOrder.find { it != opponentId }!!)
        val postCombatState = simulateSingleAttack(state, entityId, opponentId)
            ?: return null // simulation failed → don't attack

        val postCombatScore = evaluator.evaluate(
            postCombatState, postCombatState.projectedState,
            state.turnOrder.find { it != opponentId }!!
        )

        // Aggression determines how much worse we're willing to accept
        val threshold = when {
            aggression >= 4 -> -1.0
            aggression >= 3 -> -0.3
            else -> 0.0 // Aggression 2: only if board improves or stays equal
        }

        return if (postCombatScore - baselineScore >= threshold) opponentId else null
    }

    /**
     * Simulate a single creature attacking: declare it as attacker, pass priority
     * through to declare blockers, let the blocking AI choose blocks, then resolve
     * through combat damage to get the post-combat state.
     *
     * Returns the post-combat GameState, or null if the simulation fails.
     */
    private fun simulateSingleAttack(
        state: GameState,
        attackerId: EntityId,
        opponentId: EntityId
    ): GameState? {
        val playerId = state.turnOrder.find { it != opponentId } ?: return null
        val attackAction = DeclareAttackers(playerId, mapOf(attackerId to opponentId))
        var current = simulator.simulate(state, attackAction).state

        // Drive through combat: pass priority, handle blockers, resolve damage.
        // Only enumerate legal actions when we expect a DeclareBlockers step (for the opponent).
        // Otherwise just pass priority directly to avoid expensive enumeration.
        var iterations = 0
        var needsBlockerCheck = true
        while (iterations < 50 && !current.gameOver && current.pendingDecision == null) {
            iterations++
            val priorityPlayer = current.priorityPlayerId ?: break

            if (needsBlockerCheck && priorityPlayer == opponentId) {
                // Opponent has priority — check if this is the declare blockers step
                val legalActions = simulator.getLegalActions(current, opponentId)
                val blockAction = legalActions.find { it.actionType == "DeclareBlockers" }
                if (blockAction != null) {
                    val blockerAction = chooseBlockers(current, blockAction, opponentId)
                    current = simulator.simulate(current, blockerAction).state
                    needsBlockerCheck = false // Blockers already declared, no need to check again
                    continue
                }
            }

            // Pass priority to advance through combat steps
            current = simulator.simulate(current, PassPriority(priorityPlayer)).state

            // Stop once we've left combat
            if (current.phase != Phase.COMBAT) break
        }

        return current
    }

    /**
     * Choose the best attack target: player or a planeswalker.
     */
    private fun chooseBestTarget(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        opponentId: EntityId,
        planeswalkerTargets: List<EntityId>,
        isEvasive: Boolean
    ): EntityId {
        if (planeswalkerTargets.isEmpty()) return opponentId

        // Only redirect evasive creatures to planeswalkers — ground creatures should pressure life total
        if (!isEvasive) return opponentId

        val power = projected.getPower(attacker) ?: 0

        // Find planeswalkers where our damage is meaningful (can kill or reduce significantly)
        val bestPw = planeswalkerTargets.maxByOrNull { pwId ->
            val loyalty = projected.getPower(pwId) ?: 0 // Loyalty is stored as power for PWs
            val pwValue = CombatMath.creatureValue(state, projected, pwId)
            // Prefer killing planeswalkers we can one-shot, or high-value ones
            if (power >= loyalty) pwValue + 5.0 else pwValue
        }

        if (bestPw != null) {
            val loyalty = projected.getPower(bestPw) ?: 0
            val pwValue = CombatMath.creatureValue(state, projected, bestPw)
            // Attack planeswalker if we can kill it, or if it's very valuable
            if (power >= loyalty || pwValue >= 6.0) return bestPw
        }

        return opponentId
    }

    /**
     * Holdback via counterattack simulation (inspired by Forge's notNeededAsBlockers).
     *
     * For each potential attacker, simulates removing it from the blocking pool and
     * checks if the opponent's crackback becomes lethal. Only retains creatures whose
     * absence from the blocking pool would cost more life than the damage they'd deal
     * by attacking.
     */
    private fun retainDefenders(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        opponentId: EntityId,
        opponentCrackbackCreatures: List<EntityId>,
        attackerMap: MutableMap<EntityId, EntityId>,
        myLife: Int
    ) {
        if (opponentCrackbackCreatures.isEmpty()) return // no crackback threat

        val myAllCreatures = projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) }

        // Vigilance creatures count as blockers even when attacking — skip them
        val nonVigilanceAttackers = attackerMap.keys.filter {
            Keyword.VIGILANCE.name !in projected.getKeywords(it)
        }
        if (nonVigilanceAttackers.isEmpty()) return

        // Baseline crackback: how much damage gets through with ALL our creatures as blockers
        // (including the ones we've declared as attackers, who won't actually be able to block)
        val vigilanceAttackers = attackerMap.keys.filter {
            Keyword.VIGILANCE.name in projected.getKeywords(it)
        }
        val nonAttackingBlockers = myAllCreatures.filter { it !in attackerMap } + vigilanceAttackers
        val baselineCrackback = CombatMath.calculateDamageThroughOptimalBlocking(
            state, projected, opponentCrackbackCreatures, nonAttackingBlockers
        )

        if (baselineCrackback < myLife) return // even without holdbacks, we survive

        // For each non-vigilance attacker, compute the marginal life cost of sending it
        // vs. the damage it would deal. Pull back those where defensive value > offensive value.
        data class AttackerCost(val entityId: EntityId, val offensiveValue: Int, val defensiveCost: Int)

        val costs = nonVigilanceAttackers.map { attackerId ->
            val attackerPower = (projected.getPower(attackerId) ?: 0).coerceAtLeast(0)

            // Simulate: what if this creature stayed back to block?
            val blockersWithThisCreature = nonAttackingBlockers + attackerId
            val crackbackWith = CombatMath.calculateDamageThroughOptimalBlocking(
                state, projected, opponentCrackbackCreatures, blockersWithThisCreature
            )
            val lifeSaved = baselineCrackback - crackbackWith

            AttackerCost(attackerId, attackerPower, lifeSaved)
        }

        // Pull back creatures where staying to block saves more life than attacking deals damage
        // Sort by net value (defensive - offensive) descending to pull back the best defenders first
        val pullBackCandidates = costs
            .filter { it.defensiveCost > it.offensiveValue }
            .sortedByDescending { it.defensiveCost - it.offensiveValue }

        for (candidate in pullBackCandidates) {
            attackerMap.remove(candidate.entityId)

            // Recalculate to check if we've pulled back enough
            val updatedBlockers = myAllCreatures.filter { it !in attackerMap || Keyword.VIGILANCE.name in projected.getKeywords(it) }
            val updatedCrackback = CombatMath.calculateDamageThroughOptimalBlocking(
                state, projected, opponentCrackbackCreatures, updatedBlockers
            )
            if (updatedCrackback < myLife) break
        }
    }

    // ── Blocking strategies ─────────────────────────────────────────────

    /**
     * Survival mode: minimize total damage taken using a damage-reduction matrix.
     * Accounts for trample and lifelink.
     */
    private fun assignBlocksForSurvival(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        val available = validBlockers.filter { it !in assignedBlockers }.toMutableList()
        val unblocked = attackers.filter { it !in blockedAttackers }.toMutableList()

        // Build a damage-reduction matrix: for each (attacker, blocker) pair,
        // compute how much effective damage is prevented by this assignment.
        // Greedily pick the pair with highest reduction.
        while (available.isNotEmpty() && unblocked.isNotEmpty()) {
            var bestAttacker: EntityId? = null
            var bestBlocker: EntityId? = null
            var bestReduction = 0

            for (attacker in unblocked) {
                val aPower = projected.getPower(attacker) ?: 0
                if (aPower <= 0) continue

                for (blocker in available) {
                    if (!CombatMath.canBeBlockedBy(state, projected, attacker, blocker)) continue
                    val reduction = CombatMath.effectiveDamagePrevented(projected, attacker, blocker)
                    if (reduction > bestReduction) {
                        bestReduction = reduction
                        bestAttacker = attacker
                        bestBlocker = blocker
                    }
                }
            }

            if (bestAttacker == null || bestBlocker == null || bestReduction <= 0) break

            blockerMap[bestBlocker] = listOf(bestAttacker)
            assignedBlockers.add(bestBlocker)
            blockedAttackers.add(bestAttacker)
            available.remove(bestBlocker)
            unblocked.remove(bestAttacker)
        }
    }

    /**
     * Profit mode: look for favorable trades, accounting for first strike, lifelink,
     * and life-scaled trade willingness. At low life, accepts worse trades to prevent damage.
     */
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

    private fun assignBlocksForProfit(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        // Sort attackers by effective damage descending — block lifelink creatures first
        val unblocked = attackers
            .filter { it !in blockedAttackers }
            .sortedByDescending { CombatMath.effectiveDamage(projected, it) }

        // Life-scaled trade acceptance for blocking:
        // When blocking, we're both killing the attacker AND preventing damage.
        // The trade threshold accounts for the damage prevention value.
        val playerId = state.turnOrder.find { id ->
            validBlockers.any { projected.getController(it) == id }
        }
        val myLife = if (playerId != null) {
            state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        } else 20
        val tradeRatio = CombatMath.tradeWillingnessRatio(myLife)

        for (attacker in unblocked) {
            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aKeywords = projected.getKeywords(attacker)
            val aHasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.creatureValue(state, projected, attacker)

            // ── Try single blocker first ──
            val singleBlocker = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                .filter { blockerId ->
                    val bPower = projected.getPower(blockerId) ?: 0
                    val bToughness = projected.getToughness(blockerId) ?: 0
                    val bKeywords = projected.getKeywords(blockerId)
                    val bHasDeathtouch = Keyword.DEATHTOUCH.name in bKeywords
                    val blockerValue = CombatMath.creatureValue(state, projected, blockerId)

                    val weKillThem = bPower >= aToughness || bHasDeathtouch
                    val weSurvive = bToughness > aPower && !aHasDeathtouch

                    // First strike check: if attacker has first strike and blocker doesn't,
                    // blocker dies before dealing damage — can't kill the attacker
                    val blockerDealsDamage = CombatMath.blockerDealsDamage(state, projected, attacker, blockerId)
                    val effectivelyKillThem = weKillThem && blockerDealsDamage

                    // Accept trade if: blocker survives, or blocker kills attacker and the trade
                    // is acceptable given life-scaled willingness. For blocking, we also prevent
                    // damage, so we're more willing to trade than when attacking.
                    (effectivelyKillThem && weSurvive) ||
                        (effectivelyKillThem && blockerValue * tradeRatio <= attackerValue)
                }
                .minByOrNull { CombatMath.creatureValue(state, projected, it) }

            if (singleBlocker != null) {
                blockerMap[singleBlocker] = listOf(attacker)
                assignedBlockers.add(singleBlocker)
                blockedAttackers.add(attacker)
            }
            // Gang blocks are handled in a separate pass (assignGangBlocks)
        }
    }

    /**
     * Extract gang-block assignments from the profit-mode flow into a separate pass.
     * Called independently so it can be re-ordered in the escalation passes.
     */
    private fun assignGangBlocks(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        val unblocked = attackers
            .filter { it !in blockedAttackers }
            .sortedByDescending { CombatMath.effectiveDamage(projected, it) }

        for (attacker in unblocked) {
            val aKeywords = projected.getKeywords(attacker)
            if (Keyword.DEATHTOUCH.name in aKeywords) continue

            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.creatureValue(state, projected, attacker)

            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            if (available.size < 2) continue

            val gangBlock = findProfitableGangBlock(state, projected, attacker, aPower, aToughness, aHasFirstStrike, attackerValue, available)
            if (gangBlock != null) {
                for (blocker in gangBlock) {
                    blockerMap[blocker] = listOf(attacker)
                    assignedBlockers.add(blocker)
                }
                blockedAttackers.add(attacker)
            }
        }
    }

    /**
     * Chump blocking with trample redirection (inspired by Forge).
     *
     * When chump blocking is needed to survive, assigns the least valuable creature
     * to block each unblocked attacker. Smart optimization: if the attacker has trample,
     * the chump is redirected to a non-trample attacker dealing similar damage, since
     * chump blocking a trampler is nearly useless (all damage overflows).
     */
    private fun makeChumpBlocks(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        // Sort unblocked attackers by effective damage descending (block biggest threats first)
        val unblocked = attackers
            .filter { it !in blockedAttackers }
            .sortedByDescending { CombatMath.effectiveDamage(projected, it) }

        // Separate trample vs non-trample for redirection
        val nonTrampleUnblocked = unblocked.filter {
            Keyword.TRAMPLE.name !in projected.getKeywords(it)
        }.toMutableList()

        for (attacker in unblocked) {
            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                .sortedBy { CombatMath.creatureValue(state, projected, it) }

            if (available.isEmpty()) continue

            val chumpBlocker = available.first()
            val aKeywords = projected.getKeywords(attacker)
            val hasTrample = Keyword.TRAMPLE.name in aKeywords

            if (hasTrample && nonTrampleUnblocked.isNotEmpty()) {
                // Redirect: chump block a non-trample attacker instead (where it actually prevents damage)
                val redirectTarget = nonTrampleUnblocked.first()
                blockerMap[chumpBlocker] = listOf(redirectTarget)
                assignedBlockers.add(chumpBlocker)
                blockedAttackers.add(redirectTarget)
                nonTrampleUnblocked.remove(redirectTarget)
            } else {
                // No redirection possible — chump block this attacker
                blockerMap[chumpBlocker] = listOf(attacker)
                assignedBlockers.add(chumpBlocker)
                blockedAttackers.add(attacker)
                nonTrampleUnblocked.remove(attacker)
            }
        }
    }

    /**
     * Add extra bodies to soak trample damage on already-blocked tramplers.
     */
    private fun reinforceBlocksAgainstTrample(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        // Find trample attackers that are already blocked but still deal overflow damage
        val tramplers = attackers.filter { attacker ->
            val aKeywords = projected.getKeywords(attacker)
            Keyword.TRAMPLE.name in aKeywords &&
                blockerMap.values.any { attacker in it }
        }

        for (trampler in tramplers) {
            val aPower = projected.getPower(trampler) ?: 0
            val aKeywords = projected.getKeywords(trampler)
            val hasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords

            // Find current blockers assigned to this trampler
            val currentBlockers = blockerMap.entries
                .filter { (_, targets) -> trampler in targets }
                .map { it.key }
            val currentToughness = currentBlockers.sumOf { projected.getToughness(it) ?: 0 }
            val lethalNeeded = if (hasDeathtouch) currentBlockers.size else currentToughness

            // If damage still overflows, add more blockers
            if (aPower > lethalNeeded) {
                val available = availableBlockersFor(state, projected, trampler, validBlockers, assignedBlockers)
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }

                var absorbed = lethalNeeded
                for (blocker in available) {
                    if (absorbed >= aPower) break
                    blockerMap[blocker] = listOf(trampler)
                    assignedBlockers.add(blocker)
                    absorbed += if (hasDeathtouch) 1 else (projected.getToughness(blocker) ?: 0)
                }
            }
        }
    }

    /**
     * Find two creatures that can gang-block an attacker profitably.
     *
     * Tier 1: Both blockers survive, kill attacker — best case.
     * Tier 2: One blocker dies, kill attacker — acceptable if attacker is much more valuable.
     * First strike: if attacker has first strike, it may kill one blocker before both deal damage.
     */
    private fun findProfitableGangBlock(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        aPower: Int,
        aToughness: Int,
        aHasFirstStrike: Boolean,
        attackerValue: Double,
        available: List<EntityId>
    ): List<EntityId>? {
        val sorted = available.sortedBy { CombatMath.creatureValue(state, projected, it) }

        var bestTier2: List<EntityId>? = null
        var bestTier2Cost = Double.MAX_VALUE

        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                val b1 = sorted[i]
                val b2 = sorted[j]
                val p1 = projected.getPower(b1) ?: 0
                val p2 = projected.getPower(b2) ?: 0
                val t1 = projected.getToughness(b1) ?: 0
                val t2 = projected.getToughness(b2) ?: 0

                val combinedPower = p1 + p2
                if (combinedPower < aToughness) continue

                // With first strike: attacker kills one blocker first.
                // The surviving blocker must have enough power alone to kill the attacker.
                if (aHasFirstStrike) {
                    // Attacker kills the weaker one (lower toughness) first
                    val b1SurvivesFS = !CombatMath.wouldKillInCombat(state, projected, attacker, b1)
                    val b2SurvivesFS = !CombatMath.wouldKillInCombat(state, projected, attacker, b2)
                    // Both need to survive first strike to deal damage together
                    if (!b1SurvivesFS || !b2SurvivesFS) continue
                }

                val bothSurvive = t1 > aPower && t2 > aPower
                val totalBlockerValue = CombatMath.creatureValue(state, projected, b1) + CombatMath.creatureValue(state, projected, b2)

                if (bothSurvive) {
                    // Tier 1: both survive, kill attacker
                    if (attackerValue >= totalBlockerValue * 0.4) {
                        return listOf(b1, b2)
                    }
                } else {
                    // Tier 2: one dies — acceptable if attacker is much more valuable
                    val dyingBlocker = if (t1 <= aPower) b1 else b2
                    val dyingValue = CombatMath.creatureValue(state, projected, dyingBlocker)
                    if (attackerValue > dyingValue * 1.5 && dyingValue < bestTier2Cost) {
                        bestTier2 = listOf(b1, b2)
                        bestTier2Cost = dyingValue
                    }
                }
            }
        }

        return bestTier2
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun getAttackingCreatures(state: GameState): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
        }
    }
}
