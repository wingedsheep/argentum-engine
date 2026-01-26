package com.wingedsheep.gameserver.deck

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * Generates random decks from a card pool with good mana curve distribution.
 *
 * Features:
 * - Randomly picks 1-2 colors
 * - Selects cards matching those colors
 * - Ensures good mana curve (more 2-3 drops, fewer high-cost cards)
 * - Adds appropriate basic lands based on color distribution
 * - Uses random art variants for basic lands
 */
class RandomDeckGenerator(
    private val cardPool: List<CardDefinition>,
    private val basicLandVariants: List<CardDefinition> = emptyList(),
    private val random: Random = Random.Default
) {
    companion object {
        const val DECK_SIZE = 60
        const val LAND_COUNT = 24
        const val SPELL_COUNT = DECK_SIZE - LAND_COUNT // 36

        // Target mana curve distribution (as percentages of non-land cards)
        private val MANA_CURVE = mapOf(
            0 to 0.00,  // CMC 0: 0%
            1 to 0.11,  // CMC 1: ~4 cards
            2 to 0.25,  // CMC 2: ~9 cards
            3 to 0.25,  // CMC 3: ~9 cards
            4 to 0.19,  // CMC 4: ~7 cards
            5 to 0.11,  // CMC 5: ~4 cards
            6 to 0.09   // CMC 6+: ~3 cards
        )

        // Maximum copies of any non-basic card
        const val MAX_COPIES = 4

        // Map color to basic land name
        private val COLOR_TO_LAND = mapOf(
            Color.WHITE to "Plains",
            Color.BLUE to "Island",
            Color.BLACK to "Swamp",
            Color.RED to "Mountain",
            Color.GREEN to "Forest"
        )
    }

    // Group land variants by land type for easy random selection
    private val landVariantsByType: Map<String, List<CardDefinition>> = basicLandVariants
        .groupBy { it.name }

    /**
     * Generates a random deck.
     *
     * @return A map of card names/identifiers to counts.
     *         Land variants use "Name#CollectorNumber" format (e.g., "Plains#196")
     */
    fun generate(): Map<String, Int> {
        // Step 1: Pick 1-2 colors randomly
        val colors = pickColors()

        // Step 2: Filter card pool to matching colors (non-lands)
        val availableSpells = cardPool.filter { card ->
            !card.isLand && cardMatchesColors(card, colors)
        }

        // Step 3: Select spells with good mana curve
        val selectedSpells = selectSpellsWithCurve(availableSpells)

        // Step 4: Calculate land distribution based on color weight
        val landCounts = calculateLandCounts(selectedSpells, colors)

        // Step 5: Pick random land variants
        val landVariants = pickLandVariants(landCounts)

        // Step 6: Combine into deck map
        return buildDeckMap(selectedSpells, landVariants)
    }

    /**
     * Picks 1-2 colors randomly.
     */
    private fun pickColors(): Set<Color> {
        val allColors = Color.entries.toList()
        val shuffled = allColors.shuffled(random)

        // 40% chance of mono-color, 60% chance of two colors
        return if (random.nextFloat() < 0.4f) {
            setOf(shuffled.first())
        } else {
            setOf(shuffled[0], shuffled[1])
        }
    }

    /**
     * Checks if a card matches the selected colors.
     * A card matches if it's colorless or all its colors are in the selected set.
     */
    private fun cardMatchesColors(card: CardDefinition, colors: Set<Color>): Boolean {
        val cardColors = card.colors
        // Colorless cards match any color combination
        if (cardColors.isEmpty()) return true
        // All card colors must be in our selected colors
        return cardColors.all { it in colors }
    }

    /**
     * Selects spells following a mana curve distribution.
     */
    private fun selectSpellsWithCurve(available: List<CardDefinition>): List<CardDefinition> {
        if (available.isEmpty()) {
            throw IllegalStateException("No cards available for the selected colors")
        }

        val selected = mutableListOf<CardDefinition>()
        val cardCounts = mutableMapOf<String, Int>()

        // Group available cards by CMC (capping at 6+)
        val byCmc = available.groupBy { minOf(it.cmc, 6) }

        // Calculate target counts for each CMC bucket
        val targetCounts = MANA_CURVE.mapValues { (_, percentage) ->
            (SPELL_COUNT * percentage).toInt()
        }.toMutableMap()

        // Adjust to ensure we hit exactly SPELL_COUNT
        val totalTarget = targetCounts.values.sum()
        if (totalTarget < SPELL_COUNT) {
            // Add remaining to CMC 3 (the most flexible spot)
            targetCounts[3] = (targetCounts[3] ?: 0) + (SPELL_COUNT - totalTarget)
        }

        // Fill each CMC bucket
        for ((cmc, targetCount) in targetCounts) {
            val cardsAtCmc = byCmc[cmc]?.shuffled(random) ?: emptyList()
            var added = 0

            for (card in cardsAtCmc) {
                if (added >= targetCount) break

                val currentCount = cardCounts[card.name] ?: 0
                if (currentCount < MAX_COPIES) {
                    // Add 1-4 copies depending on how many we still need
                    val maxToAdd = minOf(MAX_COPIES - currentCount, targetCount - added)
                    val copiesToAdd = if (maxToAdd >= 2) random.nextInt(1, maxToAdd + 1) else maxToAdd

                    repeat(copiesToAdd) {
                        selected.add(card)
                    }
                    cardCounts[card.name] = currentCount + copiesToAdd
                    added += copiesToAdd
                }
            }

            // If we couldn't fill this bucket, try to borrow from adjacent CMC
            if (added < targetCount) {
                val deficit = targetCount - added
                // Try adjacent CMC buckets
                val adjacentCmcs = listOf(cmc - 1, cmc + 1).filter { it in 1..6 }
                for (adjacentCmc in adjacentCmcs) {
                    val adjacentCards = byCmc[adjacentCmc]?.shuffled(random) ?: continue
                    for (card in adjacentCards) {
                        if (added >= targetCount) break
                        val currentCount = cardCounts[card.name] ?: 0
                        if (currentCount < MAX_COPIES) {
                            selected.add(card)
                            cardCounts[card.name] = currentCount + 1
                            added++
                        }
                    }
                }
            }
        }

        // If we still don't have enough cards, fill with any available cards
        while (selected.size < SPELL_COUNT) {
            val shuffledAvailable = available.shuffled(random)
            for (card in shuffledAvailable) {
                if (selected.size >= SPELL_COUNT) break
                val currentCount = cardCounts[card.name] ?: 0
                if (currentCount < MAX_COPIES) {
                    selected.add(card)
                    cardCounts[card.name] = currentCount + 1
                }
            }
            // Prevent infinite loop if we truly can't fill the deck
            if (available.all { (cardCounts[it.name] ?: 0) >= MAX_COPIES }) {
                break
            }
        }

        return selected
    }

    /**
     * Calculates land distribution based on color weight in selected spells.
     * Returns a map of land type name to count needed.
     */
    private fun calculateLandCounts(spells: List<CardDefinition>, colors: Set<Color>): Map<String, Int> {
        if (colors.size == 1) {
            // Mono-color: all lands of that color
            val landName = COLOR_TO_LAND[colors.first()]!!
            return mapOf(landName to LAND_COUNT)
        }

        // Count colored mana symbols in the deck
        val colorWeights = mutableMapOf<Color, Int>()
        for (spell in spells) {
            for ((color, count) in spell.manaCost.colorCount) {
                if (color in colors) {
                    colorWeights[color] = (colorWeights[color] ?: 0) + count
                }
            }
        }

        // If no colored symbols (all colorless spells), split evenly
        if (colorWeights.isEmpty()) {
            val splitCount = LAND_COUNT / colors.size
            val remainder = LAND_COUNT % colors.size
            return colors.mapIndexed { index, color ->
                val landName = COLOR_TO_LAND[color]!!
                val count = splitCount + if (index < remainder) 1 else 0
                landName to count
            }.toMap()
        }

        // Calculate proportional land counts
        val totalWeight = colorWeights.values.sum().toDouble()
        val lands = mutableMapOf<String, Int>()
        var assignedLands = 0

        val sortedColors = colors.sortedByDescending { colorWeights[it] ?: 0 }
        for ((index, color) in sortedColors.withIndex()) {
            val weight = colorWeights[color] ?: 0
            val landName = COLOR_TO_LAND[color]!!

            val count = if (index == sortedColors.lastIndex) {
                // Last color gets remaining lands
                LAND_COUNT - assignedLands
            } else {
                val proportion = weight / totalWeight
                (LAND_COUNT * proportion).toInt().coerceAtLeast(1)
            }

            lands[landName] = count
            assignedLands += count
        }

        return lands
    }

    /**
     * Picks random land variants for the given land counts.
     * Returns a list of land identifiers in "Name#CollectorNumber" format if variants are available,
     * otherwise just the land name.
     */
    private fun pickLandVariants(landCounts: Map<String, Int>): List<String> {
        val result = mutableListOf<String>()

        for ((landName, count) in landCounts) {
            val variants = landVariantsByType[landName]

            if (variants.isNullOrEmpty()) {
                // No variants available, use plain land name
                repeat(count) { result.add(landName) }
            } else {
                // Pick random variants for each land
                repeat(count) {
                    val variant = variants[random.nextInt(variants.size)]
                    val collectorNumber = variant.metadata.collectorNumber
                    // Use "Name#CollectorNumber" format if collector number is available
                    val identifier = if (collectorNumber != null) {
                        "$landName#$collectorNumber"
                    } else {
                        landName
                    }
                    result.add(identifier)
                }
            }
        }

        return result
    }

    /**
     * Builds the final deck map from selected spells and land variants.
     */
    private fun buildDeckMap(spells: List<CardDefinition>, landVariants: List<String>): Map<String, Int> {
        val deckMap = mutableMapOf<String, Int>()

        // Count spells
        for (spell in spells) {
            deckMap[spell.name] = (deckMap[spell.name] ?: 0) + 1
        }

        // Add land variants (each might be unique like "Plains#196")
        for (land in landVariants) {
            deckMap[land] = (deckMap[land] ?: 0) + 1
        }

        return deckMap
    }
}
