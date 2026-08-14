package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimCardOps
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimSetTables
import kotlin.math.pow
import kotlin.random.Random

/**
 * The Draftsim card ratings a *constructed* build reads, and the sampler that uses them.
 *
 * Every rated set is merged in, regardless of which sets the pool was scoped to: ratings and the
 * removal list are per-*card* judgements that transfer straight to constructed, and a constructed
 * pool spans reprints from everywhere, so the widest name coverage is the most useful. The merge is
 * cached inside [DraftsimData], so it is paid once per process.
 *
 * The archetype tables are deliberately dropped. They encode one limited environment's synergy
 * pairs ("BLB Bats", "OTJ Outlaws"); merged across 40-odd sets they would have a builder rank a
 * nonsense mix of them. Empty archetypes put Draftsim's autobuilder on its no-archetype path, which
 * ranks the ten two-colour guilds instead.
 */
internal object ConstructedRatings {

    fun tables(): DraftsimSetTables =
        DraftsimData.tablesFor(DraftsimData.ratedSetCodes()).copy(archetypes = emptyMap())

    fun ops(): DraftsimCardOps = DraftsimCardOps(tables())

    /** Floor on a sampling weight; a zero would make `1/w` divide by zero. */
    const val MIN_WEIGHT = 0.1
}

/**
 * Draw [count] elements without replacement, biased towards high [weight].
 *
 * Efraimidis–Spirakis weighted sampling: key each element with `u^(1/w)` and keep the highest keys.
 * Both constructed builders want the same thing from it — the bombs almost always make the cut while
 * the tail still turns over between games, so the AI doesn't seat the identical deck every time.
 *
 * Callers supply the weight already shaped (the generators cube the rating to sharpen the bias);
 * weights are floored at [ConstructedRatings.MIN_WEIGHT] here so a zero-rated card can't divide by
 * zero.
 */
internal fun <T> List<T>.weightedSample(count: Int, random: Random, weight: (T) -> Double): List<T> {
    if (count >= size) return this
    if (count <= 0) return emptyList()
    return this
        .map { element ->
            val w = weight(element).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            element to random.nextDouble().pow(1.0 / w)
        }
        .sortedByDescending { it.second }
        .take(count)
        .map { it.first }
}
