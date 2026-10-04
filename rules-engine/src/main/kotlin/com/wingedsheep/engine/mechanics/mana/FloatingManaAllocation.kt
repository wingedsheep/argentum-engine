package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/** An entire pool payment: fixed pips and X compete for the same exact units. */
internal data class FloatingManaAllocation(
    val pool: ManaPool,
    val spent: ManaPool,
    val xSpentByColor: Map<Color, Int>,
)

/**
 * Find a complete assignment, maximizing distinct activation contributions before tie-breaking
 * by restricted/non-snow mana and native colors. Ordinary payments retain their existing fast path.
 * Activation identities are allocated once per production; containing scopes share that identity.
 */
internal fun ManaPool.allocateFloating(
    cost: ManaCost,
    context: SpellPaymentContext?,
    xAmount: Int = 0,
    xColors: Set<Color> = emptySet(),
): FloatingManaAllocation? {
    val available = total + if (context == null) restrictedMana.count { it.restriction == ManaRestriction.AnySpend }
        else getTotalEligibleRestricted(context)
    if (xAmount > available) return null
    val fixed = cost.symbols.filter { it !is ManaSymbol.X && it !is ManaSymbol.MonocolorHybrid }
    val hybrids = cost.symbols.filterIsInstance<ManaSymbol.MonocolorHybrid>().groupingBy { it }.eachCount()
    var best: FloatingManaAllocation? = null
    fun consider(symbols: List<ManaSymbol>) {
        val pips = mutableListOf<PoolPip>()
        for (symbol in symbols) {
            if (symbol is ManaSymbol.Generic) {
                if (symbol.amount > available - pips.size) return
                repeat(symbol.amount) { pips.add(PoolPip(symbol = ManaSymbol.Generic(1))) }
            } else pips.add(PoolPip(symbol))
        }
        if (xAmount > available - pips.size) return
        repeat(xAmount) { pips.add(PoolPip(ManaSymbol.Generic(1), isX = true)) }
        val result = matchFloating(pips, context, xColors) ?: return
        val prior = best
        if (prior == null || result.pool.dischargedObligations.size > prior.pool.dischargedObligations.size ||
            (result.pool.dischargedObligations.size == prior.pool.dischargedObligations.size &&
                result.spent.total < prior.spent.total)) best = result
    }
    // Repeated identical mono-hybrids need n+1 splits, rather than 2^n permutations.
    val kinds = hybrids.entries.toList()
    fun choose(index: Int, symbols: List<ManaSymbol>) {
        if (index == kinds.size) { consider(symbols); return }
        val (symbol, count) = kinds[index]
        for (colored in count downTo 0) {
            choose(index + 1, symbols + List(colored) { ManaSymbol.Colored(symbol.color) } +
                ManaSymbol.Generic((count - colored) * symbol.generic))
        }
    }
    choose(0, fixed)
    return best
}

private data class PoolPip(val symbol: ManaSymbol, val isX: Boolean = false)
private data class PoolUnits(
    val color: Color?,
    val snow: Boolean,
    val capacity: Int,
    val restrictedIndex: Int? = null,
    val obligations: Set<String> = emptySet(),
)

private fun ManaPool.matchFloating(
    pips: List<PoolPip>,
    context: SpellPaymentContext?,
    xColors: Set<Color>,
): FloatingManaAllocation? {
    val units = buildList {
        restrictedMana.forEachIndexed { index, entry ->
            val eligible = if (context == null) entry.restriction == ManaRestriction.AnySpend
                else entry.restriction.isSatisfiedBy(context)
            if (eligible) add(PoolUnits(entry.color,
                entry.source?.isSnow == true, 1, index, entry.obligationIds - dischargedObligations))
        }
        for (color in Color.entries.map { it as Color? } + null) {
            val count = if (color == null) colorless else get(color)
            val snow = if (color == null) snowColorless else snowMana[color] ?: 0
            if (count > snow) add(PoolUnits(color, false, minOf(count - snow, pips.size)))
            if (snow > 0) add(PoolUnits(color, true, minOf(snow, pips.size)))
        }
    }
    if (units.sumOf { it.capacity } < pips.size) return null
    val groups = units.map { it.obligations }.filter { it.isNotEmpty() }.distinct()
    val source = 0
    val groupStart = 1
    val unitStart = groupStart + groups.size
    val pipStart = unitStart + units.size
    val sink = pipStart + pips.size
    val network = AllocationNetwork(sink + 1)
    // All lower-priority preferences together must cost less than one new contribution.
    val contributionWeight = (pips.size.toLong() + 1) * 16
    groups.forEachIndexed { i, ids ->
        val capacity = units.filter { it.obligations == ids }.sumOf { it.capacity }
        network.add(source, groupStart + i, 1, -contributionWeight * ids.size)
        network.add(source, groupStart + i, capacity - 1, 0)
    }
    units.forEachIndexed { i, unit ->
        val origin = if (unit.obligations.isEmpty()) source else groupStart + groups.indexOf(unit.obligations)
        network.add(origin, unitStart + i, unit.capacity, 0)
    }
    val assignments = mutableListOf<Triple<Int, Int, AllocationNetwork.Edge>>()
    fun acceptsColor(unit: PoolUnits, color: Color) = unit.color == color ||
        unit.color in spendingColors[color].orEmpty() || (unit.color == null && context?.colorlessAsAnyColor == true)
    fun accepts(unit: PoolUnits, pip: PoolPip): Boolean {
        if (pip.isX) return xColors.isEmpty() || unit.color in xColors
        return when (val symbol = pip.symbol) {
            is ManaSymbol.Colored -> acceptsColor(unit, symbol.color)
            is ManaSymbol.Colorless -> unit.color == null
            is ManaSymbol.Hybrid, is ManaSymbol.HybridPhyrexian ->
                acceptsColor(unit, symbol.color1) || acceptsColor(unit, symbol.color2)
            is ManaSymbol.Phyrexian -> acceptsColor(unit, symbol.color)
            is ManaSymbol.Generic -> true
            ManaSymbol.Snow -> unit.snow
            is ManaSymbol.X, is ManaSymbol.MonocolorHybrid -> error("Unexpanded allocation pip")
        }
    }
    pips.forEachIndexed { p, pip ->
        network.add(pipStart + p, sink, 1, 0)
        units.forEachIndexed { u, unit ->
            if (accepts(unit, pip)) {
                val preference = (if (unit.restrictedIndex == null) 2 else 0) +
                    (if (unit.snow && pip.symbol != ManaSymbol.Snow) 4 else 0) +
                    when (val symbol = pip.symbol) {
                        is ManaSymbol.Colored -> if (unit.color == symbol.color) 0 else 1
                        is ManaSymbol.Phyrexian -> if (unit.color == symbol.color) 0 else 1
                        else -> 0
                    }
                assignments.add(Triple(u, p, network.add(unitStart + u, pipStart + p, 1, preference.toLong())))
            }
        }
    }
    if (!network.send(source, sink, pips.size)) return null
    val used = IntArray(units.size)
    var spent = ManaPool.EMPTY
    val xSpent = mutableMapOf<Color, Int>()
    for ((u, p, edge) in assignments) {
        if (edge.capacity != 0) continue
        used[u]++
        val color = units[u].color
        spent = if (color == null) spent.addColorless() else spent.add(color)
        if (pips[p].isX && color != null) xSpent.merge(color, 1, Int::plus)
    }
    var pool = this
    val removed = mutableSetOf<Int>()
    val discharged = dischargedObligations.toMutableSet()
    units.forEachIndexed { i, unit ->
        val count = used[i]
        if (count == 0) return@forEachIndexed
        if (unit.restrictedIndex != null) {
            removed.add(unit.restrictedIndex)
            discharged.addAll(unit.obligations)
        } else {
            // Deduct the selected snow/plain bucket exactly, independent of pip visitation order.
            val color = unit.color
            pool = if (color == null) pool.copy(colorless = pool.colorless - count,
                snowColorless = pool.snowColorless - if (unit.snow) count else 0)
            else {
                val nextSnow = (pool.snowMana[color] ?: 0) - if (unit.snow) count else 0
                pool.add(color, -count).copy(snowMana = if (nextSnow == 0) pool.snowMana - color
                    else pool.snowMana + (color to nextSnow))
            }
        }
    }
    return FloatingManaAllocation(pool.copy(
        restrictedMana = restrictedMana.filterIndexed { index, _ -> index !in removed },
        dischargedObligations = discharged,
    ), spent, xSpent)
}

/** Successive shortest augmenting paths; residual edges can reassign earlier flexible pips. */
private class AllocationNetwork(size: Int) {
    class Edge(val to: Int, val reverse: Int, var capacity: Int, val cost: Long)
    private val edges = Array(size) { mutableListOf<Edge>() }
    fun add(from: Int, to: Int, capacity: Int, cost: Long): Edge {
        val forward = Edge(to, edges[to].size, capacity, cost)
        val reverse = Edge(from, edges[from].size, 0, -cost)
        edges[from].add(forward)
        edges[to].add(reverse)
        return forward
    }
    fun send(source: Int, sink: Int, amount: Int): Boolean {
        repeat(amount) {
            val distance = LongArray(edges.size) { Long.MAX_VALUE }
            val previousNode = IntArray(edges.size) { -1 }
            val previousEdge = IntArray(edges.size)
            val queued = BooleanArray(edges.size)
            val queue = ArrayDeque<Int>()
            distance[source] = 0
            queue.add(source); queued[source] = true
            while (queue.isNotEmpty()) {
                val from = queue.removeFirst(); queued[from] = false
                edges[from].forEachIndexed { index, edge ->
                    if (edge.capacity > 0 && distance[from] + edge.cost < distance[edge.to]) {
                        distance[edge.to] = distance[from] + edge.cost
                        previousNode[edge.to] = from; previousEdge[edge.to] = index
                        if (!queued[edge.to]) { queue.add(edge.to); queued[edge.to] = true }
                    }
                }
            }
            if (previousNode[sink] < 0) return false
            var at = sink
            while (at != source) {
                val from = previousNode[at]
                val edge = edges[from][previousEdge[at]]
                edge.capacity--
                edges[at][edge.reverse].capacity++
                at = from
            }
        }
        return true
    }
}
