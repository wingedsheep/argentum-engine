package com.wingedsheep.gameserver.jumpstart

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/** Published paper packs. A missing or banned card disables the entire variant, never substitutes. */
data class JumpstartPack(val id: String, val cards: List<CardDefinition>) {
    val theme: String get() = id.substringBefore(" (")
}

class JumpstartPacks(generator: BoosterGenerator, setCode: String = "JMP") {
    // Packs include reprints whose canonical definitions can live in any earlier set.
    private val cardsByName = generator.availableSets.values
        .flatMap { it.cards + it.basicLands }
        .associateBy { it.name } + generator.getSetConfig(setCode)?.let {
            (it.cards + it.basicLands).associateBy { card -> card.name }
        }.orEmpty()

    val packs: List<JumpstartPack> = listsFor(setCode).mapNotNull { (id, names) ->
        val cards = names.map { cardsByName[it] ?: return@mapNotNull null }
        JumpstartPack(id, cards)
    }

    fun available(banned: Set<String>): List<JumpstartPack> = packs.filter { pack ->
        pack.cards.none { card -> banned.any { it.equals(card.name, ignoreCase = true) } }
    }

    /** Offer distinct themes, then roll a variant within each theme. */
    fun offer(banned: Set<String>, random: Random = Random.Default): List<String> =
        available(banned).groupBy { it.theme }.values.shuffled(random).take(3)
            .map { it.random(random).id }

    /**
     * The two rows a deck is chosen from. Six distinct themes when there are that many, so no row
     * repeats the other; with fewer (heavy bans), each row is drawn on its own and may overlap.
     */
    fun offerPair(banned: Set<String>, random: Random = Random.Default): Pair<List<String>, List<String>> {
        val themes = available(banned).groupBy { it.theme }.values.shuffled(random)
        if (themes.size < 6) return offer(banned, random) to offer(banned, random)
        val ids = themes.take(6).map { it.random(random).id }
        return ids.take(3) to ids.drop(3)
    }

    companion object {
        val lists: Map<String, List<String>> get() = listsFor("JMP")

        /** Jumpstart sets with checked-in paper pack lists (`/jumpstart/<code>.txt`), newest first. */
        val SUPPORTED_SETS: List<String> = listOf("J25", "J22", "JMP")

        private val published by lazy {
            SUPPORTED_SETS.associateWith { loadLists(it) }
        }

        fun listsFor(setCode: String): Map<String, List<String>> =
            requireNotNull(published[setCode]) { "Unsupported Jumpstart set: $setCode" }

        private fun loadLists(setCode: String): Map<String, List<String>> {
            val result = linkedMapOf<String, MutableList<String>>()
            var current: MutableList<String>? = null
            requireNotNull(JumpstartPacks::class.java.getResourceAsStream("/jumpstart/${setCode.lowercase()}.txt"))
                .bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                        if (line.startsWith("[")) {
                            current = mutableListOf<String>().also { result[line.removeSurrounding("[", "]")] = it }
                        } else {
                            val (count, name) = line.split(" ", limit = 2)
                            repeat(count.toInt()) { requireNotNull(current).add(name) }
                        }
                    }
                }
            require(result.size == 121)
            require(result.values.all { it.size == 20 }) { "Every Jumpstart pack must contain exactly 20 cards" }
            return result
        }
    }
}
