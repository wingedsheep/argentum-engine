package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/** Picks cards by rarity without repeating names within a single booster. */
internal class RarityPicker(pool: List<CardDefinition>, private val random: Random) {
    private val byRarity: Map<Rarity, MutableList<CardDefinition>> =
        pool.groupBy { it.metadata.rarity }.mapValues { it.value.toMutableList() }
    private val used = mutableSetOf<String>()

    fun hasAny(rarity: Rarity): Boolean = !byRarity[rarity].isNullOrEmpty()

    /** Exclude [name] from later picks — for cards a strategy placed without this picker. */
    fun markUsed(name: String) {
        used.add(name)
    }

    fun pick(rarity: Rarity): CardDefinition? {
        val available = byRarity[rarity]?.filter { it.name !in used } ?: return null
        if (available.isEmpty()) return null
        val picked = available.random(random)
        used.add(picked.name)
        return picked
    }
}

/** Picks one of [items] with probability proportional to [weight]; null when nothing has positive weight. */
internal fun <T> weightedPick(items: List<T>, random: Random, weight: (T) -> Int): T? {
    val total = items.sumOf { weight(it).coerceAtLeast(0) }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (item in items) {
        roll -= weight(item).coerceAtLeast(0)
        if (roll < 0) return item
    }
    return null
}
