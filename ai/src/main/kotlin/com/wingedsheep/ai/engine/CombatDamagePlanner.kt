package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdge
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.sdk.model.EntityId

/**
 * Divides combat damage on a [CombatResolutionDecision] for the AI.
 *
 * CR 510.1c/d: a creature blocked by (or blocking) two or more creatures divides its combat damage
 * among them as its controller chooses — there is no damage-assignment order, so the only things
 * that constrain the split are the source's power and CR 702.19b (a trampler reaches the player
 * only once every blocker it's assigning to has lethal damage, counting damage from the other
 * creatures assigning this step). The engine's pre-filled amounts are "lethal to each in
 * declaration order, remainder to the last", which can throw away a kill: a 2/2 blocked by a 4/4
 * and a 4/2 in that order puts both points on the 4/4 and kills nothing.
 *
 * For a source the decider controls, this picks the set of creatures to kill that maximises the
 * total [value] of what dies (a small knapsack over the blockers, ties broken toward spending less
 * damage), then:
 * - with trample, if every blocker can be brought to lethal, does so and sends the excess to the
 *   drain (player / planeswalker / battle);
 * - otherwise drops the leftover on the most valuable creature it did not kill (chip damage).
 *
 * Deathtouch makes any nonzero assignment lethal (CR 702.2c), so a deathtouch source needs one point
 * per kill. Damage the other edges of the step already carry (another attacker on the same blocker,
 * edges the decider doesn't own) is counted toward lethal.
 *
 * For a source banding handed to the decider (CR 702.22j/k: the opponent's creature, dividing its
 * damage among the decider's creatures), the goal flips to losing as little as possible: pile it on
 * a creature that's already dying, else spread it so nothing reaches lethal, else let the least
 * valuable creature soak all of it.
 */
internal object CombatDamagePlanner {

    /** Above this many candidate targets the knapsack falls back to a value-per-damage greedy. */
    private const val EXHAUSTIVE_LIMIT = 12

    /**
     * @param controllerOf the controller of a combat creature, to tell the decider's own sources
     *   from banding-inverted ones.
     * @param value how much the decider gains when the opponent loses this creature (or loses when
     *   it loses its own); 0 or less means killing it is worthless (e.g. indestructible).
     */
    fun plan(
        decision: CombatResolutionDecision,
        playerId: EntityId,
        controllerOf: (EntityId) -> EntityId?,
        value: (EntityId) -> Double,
    ): CombatResolutionResponse {
        val amounts = LinkedHashMap<String, Int>().apply { decision.edges.forEach { put(it.id, it.amount) } }
        val board = Board(decision)

        val owned = decision.edges.filter { it.editableBy == playerId }
        val ownedIds = owned.mapTo(HashSet()) { it.id }

        // Damage this decision won't let us move still counts toward lethal on its target.
        for (edge in decision.edges) {
            if (edge.id in ownedIds || edge.isTrampleDrain) continue
            board.apply(edge.sourceId, edge.targetId, edge.amount)
        }

        for ((sourceId, edges) in owned.groupBy { it.sourceId }) {
            val fixedFromSource = decision.edges
                .filter { it.sourceId == sourceId && it.id !in ownedIds }
                .sumOf { it.amount }
            val power = (edges.maxOf { it.maximum } - fixedFromSource).coerceAtLeast(0)
            val creatureEdges = edges.filter { !it.isTrampleDrain }
            if (creatureEdges.isEmpty() || power == 0) {
                // Nothing to divide among creatures (e.g. every blocker left combat): keep the default.
                edges.forEach { board.apply(sourceId, it.targetId, if (it.isTrampleDrain) 0 else it.amount) }
                continue
            }
            val drains = edges.filter { it.isTrampleDrain }
            val split = if (controllerOf(sourceId) == playerId) {
                planOwnSource(board, sourceId, power, creatureEdges, drains, value)
            } else {
                planInvertedSource(board, sourceId, power, creatureEdges, value)
            }
            for (edge in edges) {
                val amount = split[edge.id] ?: 0
                amounts[edge.id] = amount
                if (!edge.isTrampleDrain) board.apply(sourceId, edge.targetId, amount)
            }
        }

        return CombatResolutionResponse(decision.id, amounts.map { (id, amount) -> DamageEdgeAmount(id, amount) })
    }

    private fun planOwnSource(
        board: Board,
        sourceId: EntityId,
        power: Int,
        creatureEdges: List<DamageEdge>,
        drains: List<DamageEdge>,
        value: (EntityId) -> Double,
    ): Map<String, Int> {
        // Trample: if every blocker can be brought to lethal (the engine's CR 702.19b check reads
        // `edge.lethal` against the damage attackers assign this step), do that and send the rest on.
        if (drains.isNotEmpty()) {
            val trampleNeed = creatureEdges.associate { it.id to (it.lethal - board.pending(it.targetId)).coerceAtLeast(0) }
            val total = trampleNeed.values.sum()
            if (total <= power) {
                val drain = drains.maxByOrNull { it.amount } ?: drains.first()
                return trampleNeed + (drain.id to power - total)
            }
        }

        val candidates = creatureEdges
            .filter { !board.isDead(it.targetId) }
            .map { Candidate(it, board.need(sourceId, it.targetId), value(it.targetId).coerceAtLeast(0.0)) }
            .filter { it.value > 0.0 }
        val kills = chooseKills(candidates, power)

        val split = HashMap<String, Int>()
        var remaining = power
        for (kill in kills) {
            split[kill.edge.id] = kill.need
            remaining -= kill.need
        }
        if (remaining > 0) {
            val killedIds = kills.mapTo(HashSet()) { it.edge.id }
            val dump = creatureEdges
                .filter { it.id !in killedIds && !board.isDead(it.targetId) }
                .maxByOrNull { value(it.targetId) }
                ?: kills.maxByOrNull { it.value }?.edge
                ?: creatureEdges.first()
            split[dump.id] = (split[dump.id] ?: 0) + remaining
        }
        return split
    }

    private fun planInvertedSource(
        board: Board,
        sourceId: EntityId,
        power: Int,
        creatureEdges: List<DamageEdge>,
        value: (EntityId) -> Double,
    ): Map<String, Int> {
        // A creature that's already dying soaks the whole lot for free.
        creatureEdges.firstOrNull { board.isDead(it.targetId) }?.let { return mapOf(it.id to power) }

        // Spread below lethal so nothing dies, if there's room.
        if (!board.hasDeathtouch(sourceId)) {
            val room = creatureEdges.associateWith { (board.need(sourceId, it.targetId) - 1).coerceAtLeast(0) }
            if (room.values.sum() >= power) {
                val split = HashMap<String, Int>()
                var remaining = power
                for ((edge, capacity) in room.entries.sortedByDescending { it.value }) {
                    if (remaining == 0) break
                    val amount = minOf(capacity, remaining)
                    split[edge.id] = amount
                    remaining -= amount
                }
                return split
            }
        }

        // Something dies either way: let the least valuable creature take all of it.
        val sponge = creatureEdges.minByOrNull { value(it.targetId) } ?: creatureEdges.first()
        return mapOf(sponge.id to power)
    }

    private class Candidate(val edge: DamageEdge, val need: Int, val value: Double)

    /** The subset of [candidates] whose kills fit in [power] with the greatest total value. */
    private fun chooseKills(candidates: List<Candidate>, power: Int): List<Candidate> {
        val affordable = candidates.filter { it.need <= power }
        if (affordable.isEmpty()) return emptyList()
        if (affordable.size > EXHAUSTIVE_LIMIT) {
            val picked = ArrayList<Candidate>()
            var remaining = power
            for (c in affordable.sortedByDescending { it.value / it.need }) {
                if (c.need <= remaining) {
                    picked += c
                    remaining -= c.need
                }
            }
            return picked
        }
        var bestMask = 0
        var bestValue = 0.0
        var bestCost = 0
        for (mask in 1 until (1 shl affordable.size)) {
            var cost = 0
            var total = 0.0
            for (i in affordable.indices) {
                if (mask and (1 shl i) != 0) {
                    cost += affordable[i].need
                    total += affordable[i].value
                }
            }
            if (cost > power) continue
            if (total > bestValue + 1e-9 || (total > bestValue - 1e-9 && cost < bestCost)) {
                bestMask = mask
                bestValue = total
                bestCost = cost
            }
        }
        return affordable.filterIndexed { i, _ -> bestMask and (1 shl i) != 0 }
    }

    /** Running damage tally over the decision's creature nodes. */
    private class Board(decision: CombatResolutionDecision) {
        private val toughnessLeft = HashMap<EntityId, Int>()
        private val deathtouch = HashSet<EntityId>()
        private val pending = HashMap<EntityId, Int>()
        private val dead = HashSet<EntityId>()

        init {
            for (a in decision.attackers) {
                toughnessLeft[a.id] = a.toughness - a.markedDamage
                if (a.hasDeathtouch) deathtouch += a.id
            }
            for (b in decision.blockers) {
                toughnessLeft[b.id] = b.toughness - b.markedDamage
                if (b.hasDeathtouch) deathtouch += b.id
            }
        }

        fun hasDeathtouch(sourceId: EntityId) = sourceId in deathtouch
        fun pending(targetId: EntityId) = pending[targetId] ?: 0
        fun isDead(targetId: EntityId) = targetId in dead
        private fun lethalBase(targetId: EntityId) = (toughnessLeft[targetId] ?: 1).coerceAtLeast(1)

        /** Damage [sourceId] must assign to [targetId] for it to die, given what's already assigned. */
        fun need(sourceId: EntityId, targetId: EntityId): Int = when {
            isDead(targetId) -> 0
            hasDeathtouch(sourceId) -> 1
            else -> (lethalBase(targetId) - pending(targetId)).coerceAtLeast(1)
        }

        fun apply(sourceId: EntityId, targetId: EntityId, amount: Int) {
            if (amount <= 0) return
            val total = pending(targetId) + amount
            pending[targetId] = total
            if (hasDeathtouch(sourceId) || total >= lethalBase(targetId)) dead += targetId
        }
    }
}
