package com.wingedsheep.engine.ai

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition

/**
 * Heuristic sealed deck builder that uses [LimitedCardRater] to evaluate card quality.
 *
 * Algorithm:
 * 1. Rate every card in the pool
 * 2. Score each color by the total rating of its cards (weighted toward creatures)
 * 3. Pick the best 2 colors
 * 4. Select the top 23 on-color spells by rating, with mana curve awareness
 * 5. Fill to 40 cards with basic lands split proportionally by mana symbols
 *
 * @param pool The sealed pool (typically 90 cards from 6 boosters)
 * @return A deck list as a map of card name to count
 */
fun buildHeuristicSealedDeck(pool: List<CardDefinition>): Map<String, Int> {
    val deck = mutableMapOf<String, Int>()
    val ratings = pool.associateWith { LimitedCardRater.rate(it) }

    val nonLands = pool.filter { !it.typeLine.isLand }
    val poolLands = pool.filter { it.typeLine.isLand && !it.typeLine.isBasicLand }

    // Score each color by the total limited rating of its cards
    val colorScores = mutableMapOf<Color, Double>()
    for (card in nonLands) {
        val rating = ratings[card] ?: 0.0
        for (color in card.colors) {
            colorScores[color] = (colorScores[color] ?: 0.0) + rating
        }
    }

    // Pick top 2 colors
    val bestColors = colorScores.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()

    // Select on-color + colorless cards, sorted by rating (best first)
    val candidates = nonLands.filter { card ->
        card.colors.isEmpty() || card.colors.all { it in bestColors }
    }.sortedByDescending { ratings[it] ?: 0.0 }

    // Pick top 23, but ensure a reasonable mana curve:
    // At least 5 creatures at CMC <= 3, and at least 13 creatures total
    val selected = mutableListOf<CardDefinition>()
    val remaining = candidates.toMutableList()

    // First pass: grab the best cards by rating
    val topPicks = remaining.take(23)
    selected.addAll(topPicks)
    remaining.removeAll(topPicks.toSet())

    // Check curve: if too few cheap creatures, swap in some
    val cheapCreatures = selected.count { it.typeLine.isCreature && it.cmc <= 3 }
    val totalCreatures = selected.count { it.typeLine.isCreature }

    if (cheapCreatures < 5 || totalCreatures < 13) {
        // Find cheap creatures we didn't pick
        val cheapReplacements = remaining.filter { it.typeLine.isCreature && it.cmc <= 3 }
            .sortedByDescending { ratings[it] ?: 0.0 }
        // Find expensive non-creatures or weak cards to cut
        val cuttable = selected.sortedBy { ratings[it] ?: 0.0 }

        var neededCheap = (5 - cheapCreatures).coerceAtLeast(0)
        var neededCreatures = (13 - totalCreatures).coerceAtLeast(0)
        val swapCount = neededCheap.coerceAtLeast(neededCreatures).coerceAtMost(cheapReplacements.size)

        for (i in 0 until swapCount) {
            if (cuttable.size > i && cheapReplacements.size > i) {
                selected.remove(cuttable[i])
                selected.add(cheapReplacements[i])
            }
        }
    }

    for (card in selected) {
        deck[card.name] = (deck[card.name] ?: 0) + 1
    }

    // Add on-color non-basic lands
    for (land in poolLands) {
        deck[land.name] = (deck[land.name] ?: 0) + 1
    }

    // Fill to 40 with basic lands split by color
    val landsNeeded = (40 - deck.values.sum()).coerceAtLeast(0)

    val colorToLand = mapOf(
        Color.WHITE to "Plains", Color.BLUE to "Island", Color.BLACK to "Swamp",
        Color.RED to "Mountain", Color.GREEN to "Forest"
    )

    if (bestColors.size >= 2) {
        val c1 = bestColors.first()
        val c2 = bestColors.last()
        // Count mana symbols to split lands proportionally
        val c1count = selected.sumOf { card -> card.manaCost.colorCount[c1] ?: 0 }
        val c2count = selected.sumOf { card -> card.manaCost.colorCount[c2] ?: 0 }
        val total = (c1count + c2count).coerceAtLeast(1)
        val l1 = (landsNeeded * c1count / total).coerceAtLeast(1)
        deck[colorToLand[c1]!!] = (deck[colorToLand[c1]!!] ?: 0) + l1
        deck[colorToLand[c2]!!] = (deck[colorToLand[c2]!!] ?: 0) + (landsNeeded - l1)
    } else if (bestColors.size == 1) {
        val land = colorToLand[bestColors.first()]!!
        deck[land] = (deck[land] ?: 0) + landsNeeded
    } else {
        deck["Forest"] = (deck["Forest"] ?: 0) + landsNeeded
    }

    return deck
}
