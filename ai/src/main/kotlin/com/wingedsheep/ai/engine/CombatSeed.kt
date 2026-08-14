package com.wingedsheep.ai.engine

import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * The attack plan `CombatAdvisor` starts from, before it simulates anything.
 *
 * [CombatAdvisor.chooseAttackers] has always been two phases — a heuristic seed, then a local search
 * that mutates it one attacker at a time and scores each mutation with a full engine simulation.
 * Phase 7 needs the first phase without the second: a rollout's combat step must be decided in
 * microseconds, and anything that simulates *inside* a playout makes the playout quadratic.
 *
 * So the seed lives here and has two callers — `CombatAdvisor`, which local-searches from it, and
 * [com.wingedsheep.ai.engine.rollout.PlayoutPolicy], which plays it as-is. One implementation, so
 * the combat a rollout imagines is the combat the AI would actually seed.
 *
 * Blocking needs no equivalent: `CombatAdvisor.chooseBlockers(useSimulation = false)` is already the
 * seed-only path and reaches no simulator.
 */
object CombatSeed {

    /**
     * Which creatures to send in, before local search.
     *
     * Three passes, in order:
     * 1. **Lethal alpha strike** — if damage gets through even against optimal blocking, everything
     *    attacks and nothing else matters. Deliberately ignores [attackPenalty]: a `CardAdvisor`
     *    that would rather keep a wall home does not get a vote on the turn we win.
     * 2. **No-downside and clearly profitable attackers** — evasive, vigilant, indestructible,
     *    survives every blocker, profitable trades.
     * 3. **Excess attackers** — more bodies than they have blockers means the surplus gets through,
     *    minus one held back when the crack-back is close to lethal.
     *
     * @param attackPenalty per-creature discouragement from a `CardAdvisor`, summed by the caller.
     *   A discouraged creature is left out of the seed; `CombatAdvisor`'s local search can still add
     *   it back when it pays. Defaults to "no advisor has an opinion", which is every card today.
     */
    fun attackers(
        state: GameState,
        projected: ProjectedState,
        legalAction: LegalAction,
        playerId: EntityId,
        cardRegistry: CardRegistry? = null,
        attackPenalty: (EntityId) -> Double = { 0.0 },
    ): AttackSeed {
        val validAttackers = legalAction.validAttackers ?: emptyList()
        val defendingPlayers = legalAction.validAttackTargets ?: emptyList()

        if (validAttackers.isEmpty() || defendingPlayers.isEmpty()) {
            return AttackSeed(emptyMap(), defendingPlayers.firstOrNull(), lethal = false)
        }

        // CR 802.2a — who we attack is a choice, and `validAttackTargets` mixes players with
        // opponent planeswalkers. Pick the *player* closest to dying: in 1v1 that is the sole
        // opponent (unchanged), and in a pod it is the seat an alpha strike can actually finish.
        val opponentId = defendingPlayers.filter { it in state.turnOrder }
            .minByOrNull { state.lifeTotal(it) }
            ?: defendingPlayers.first()
        val opponentLife = if (opponentId in state.turnOrder) state.lifeTotal(opponentId) else 20
        val opponentCreatures = CombatMath.getOpponentUntappedCreatures(state, projected, opponentId)
        val mandatory = legalAction.mandatoryAttackers ?: emptyList()

        // ── Lethal check: alpha-strike if damage gets through even with optimal blocking ──
        if (CombatMath.isLethalAttack(state, projected, validAttackers, opponentCreatures, opponentLife)) {
            return AttackSeed(validAttackers.associateWith { opponentId }, opponentId, lethal = true)
        }

        // ── Heuristic seed: no-downside and clearly profitable attackers ──
        // Creatures an advisor discourages from attacking are left out of the seed;
        // the local search below can still add them back when it pays.
        val discouraged = validAttackers.filter { entityId ->
            entityId !in mandatory && attackPenalty(entityId) > 0.0
        }.toSet()

        val seedMap = mutableMapOf<EntityId, EntityId>()
        for (entityId in mandatory) {
            seedMap[entityId] = opponentId
        }
        for (entityId in validAttackers) {
            if (entityId in seedMap || entityId in discouraged) continue
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
                if (damageThrough > 0 &&
                    toughness > (opponentCreatures.minOfOrNull { projected.getPower(it) ?: 0 } ?: 0)
                ) {
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
                )
            ) {
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
                    .filter { it !in seedMap && it !in discouraged && (projected.getPower(it) ?: 0) > 0 }
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }

                // Estimate opponent's crack-back damage through our optimal blocking.
                // Accounts for evasion — flying creatures we can't block deal guaranteed damage.
                val myLife = state.lifeTotal(playerId)
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

        return AttackSeed(seedMap, opponentId, lethal = false)
    }

    /**
     * The seed plan, plus the two things `CombatAdvisor`'s local search needs to know about it.
     *
     * @property attackers creature → the player or planeswalker it attacks.
     * @property defenderId the seat the seed aims at, or null when nothing can legally be attacked.
     * @property lethal whether this is the alpha strike from the lethal check. A local search over
     *   a winning attack can only make it worse, so `CombatAdvisor` skips it.
     */
    data class AttackSeed(
        val attackers: Map<EntityId, EntityId>,
        val defenderId: EntityId?,
        val lethal: Boolean,
    )
}
