package com.wingedsheep.gym.service

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck

/**
 * Resolves a [DeckSpec] into the [Deck] format the engine consumes, and
 * validates explicit deck lists against the card registry.
 *
 * A `BoosterGenerator` is optional; it is only required when a caller
 * submits [DeckSpec.RandomSealed]. Explicit decks work without one.
 */
class DeckResolver(
    private val cardRegistry: CardRegistry,
    private val boosterGenerator: BoosterGenerator? = null
) {

    /** Turn a [DeckSpec] into a concrete [Deck] for [com.wingedsheep.engine.core.GameInitializer]. */
    fun resolve(spec: DeckSpec): Deck = when (spec) {
        is DeckSpec.Explicit -> expandCounts(spec.cards)
        is DeckSpec.RandomSealed -> {
            val bg = requireNotNull(boosterGenerator) {
                "DeckSpec.RandomSealed requires a BoosterGenerator"
            }
            val sealed = SealedDeckGenerator(bg)
            val deckMap = spec.setCode?.let { sealed.generate(it) } ?: sealed.generate()
            expandCounts(deckMap)
        }
    }

    /**
     * Open a sealed pool **without** building a deck — for the deckbuild env, which lets an
     * agent make the build choices itself. Returns the opened cards plus the basic-land
     * definitions (one art variant per land name) the agent may add without limit.
     */
    fun openSealedPool(setCode: String, boosterCount: Int = 6): SealedPool {
        val bg = requireNotNull(boosterGenerator) {
            "Opening a sealed pool requires a BoosterGenerator"
        }
        val pool = bg.generateSealedPool(setCode, boosterCount)
        val basics = bg.getBasicLands(setCode).values.toList()
        return SealedPool(pool, basics)
    }

    /**
     * Check an explicit deck list is playable:
     * - every card name resolves in the registry (variant suffixes like
     *   `"Plains#POR-331"` are tolerated — the base name before `#` is what
     *   the engine looks up);
     * - deck meets the minimum size.
     *
     * Does not enforce format-specific rules (banned lists, 4-of, etc.) —
     * those are policy decisions the caller can layer on top.
     */
    fun validate(cards: Map<String, Int>, minSize: Int = 40): DeckValidation {
        val errors = mutableListOf<String>()
        val total = cards.values.sum()
        if (total < minSize) {
            errors += "Deck has $total cards, minimum $minSize"
        }
        cards.forEach { (name, count) ->
            if (count < 0) errors += "Card '$name' has negative count: $count"
            val baseName = name.substringBefore('#')
            if (!cardRegistry.hasCard(baseName)) {
                errors += "Unknown card: $name"
            }
        }
        return DeckValidation(ok = errors.isEmpty(), errors = errors, totalCards = total)
    }

    private fun expandCounts(cards: Map<String, Int>): Deck =
        Deck(cards.flatMap { (name, count) -> List(count) { name } })
}

/** An opened sealed pool: the [pool] cards, plus the [basics] an agent may add without limit. */
data class SealedPool(
    val pool: List<CardDefinition>,
    val basics: List<CardDefinition>
)
