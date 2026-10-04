package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/** Proves contribution coverage for independent, zero-mana-cost tap abilities without changing state. */
internal fun canPayWithIndependentTaps(
    initial: ManaPool,
    pending: Set<String>,
    sources: List<ManaSource>,
    cost: ManaCost,
    context: SpellPaymentContext?,
    xAmount: Int = 0,
    xColors: Set<Color> = emptySet(),
): Boolean {
    val initialPool = if (context == null) initial.copy(restrictedMana = initial.restrictedMana.filter {
        it.restriction == ManaRestriction.AnySpend
    }) else initial
    val paymentContext = context ?: SpellPaymentContext()
    fun completes(pool: ManaPool, obligations: Set<String>): Boolean {
        val paid = pool.allocateFloating(cost, paymentContext, xAmount, xColors) ?: return false
        return paid.pool.dischargedObligations.containsAll(obligations)
    }
    if (completes(initialPool, pending)) return true
    // An independent tap pays no activation cost, so it cannot repair an identity without units.
    val liveIds = initialPool.restrictedMana.flatMap { it.obligationIds }.toSet()
    if (!liveIds.containsAll(pending)) return false
    val maxUnits = cost.cmc + xAmount
    val candidates = sources.filter { it.exactIndependentTap }
    if (candidates.isEmpty()) return false
    var prefix = "independent-plan:"
    while (pending.any { it.startsWith(prefix) }) prefix += ":"

    fun productions(source: ManaSource): List<List<RestrictedManaEntry>> {
        val kinds = source.availableColorsFor(context).map<Color, Color?> { it } +
            if (source.producesColorless) listOf(null) else emptyList()
        return kinds.mapNotNull { color ->
            val amount = source.amountFor(color).coerceAtMost(maxUnits)
            if (amount <= 0) return@mapNotNull null
            val restriction = color?.let { source.colorRestrictions[it] } ?: source.restriction ?: ManaRestriction.AnySpend
            if (context == null && restriction != ManaRestriction.AnySpend) return@mapNotNull null
            if (context != null && !restriction.isSatisfiedBy(context)) return@mapNotNull null
            // Feasibility only needs snow provenance. Canonical tags let equal sources share memo entries.
            List(amount) { RestrictedManaEntry(color, restriction,
                source = ManaSourceTag(EntityId("independent-plan"), isSnow = source.isSnow)) }
        }.distinct()
    }
    // Triggered bonuses have their own chooser, source, conditions and production boundaries.
    // Their aggregate preview is not proof; only already-floating bonus mana is used here.
    val options = candidates.map(::productions)
    // Every remaining kind is an optimistic upper bound: one future source cannot actually
    // produce all of its alternatives, but impossibility even with that excess prunes safely.
    val suffix = Array(options.size + 1) { emptyList<RestrictedManaEntry>() }
    for (index in options.indices.reversed()) suffix[index] = options[index].flatten() + suffix[index + 1]

    data class SearchKey(val index: Int, val selected: Int, val pool: ManaPool)
    val failed = mutableSetOf<SearchKey>()
    fun search(index: Int, selected: Int, pool: ManaPool): Boolean {
        val required = pending + (0 until selected).map { "$prefix$it" }
        if (completes(pool, required)) return true
        if (index == options.size || selected >= maxUnits) return false
        val key = SearchKey(index, selected, pool)
        if (key in failed) return false
        val optimistic = pool.copy(restrictedMana = pool.restrictedMana + suffix[index])
        if (!completes(optimistic, required)) {
            failed.add(key)
            return false
        }
        for (base in options[index]) {
            val id = "$prefix$selected"
            val next = pool.copy(restrictedMana = pool.restrictedMana +
                base.map { it.copy(obligationIds = setOf(id)) })
            if (search(index + 1, selected + 1, next)) return true
        }
        if (search(index + 1, selected, pool)) return true
        failed.add(key)
        return false
    }
    return search(0, 0, initialPool)
}
