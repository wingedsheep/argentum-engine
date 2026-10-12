package com.wingedsheep.ai.engine

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet

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
 * When [locked] is non-empty the builder switches to **completion mode**: the locked cards are
 * treated as already-chosen (always kept, counted toward the 4-of limit) and only the remaining
 * slots are filled. This powers the deckbuild "Auto-build" button — an empty deck builds fresh, a
 * partial deck is completed without dropping the player's existing picks.
 *
 * @param pool The sealed pool (typically 90 cards from 6 boosters)
 * @param locked Cards already in the deck (name → count). Empty = build fresh (default).
 * @param targetSize Total deck size to fill to (default 40 for limited).
 * @param resolveCard Resolves a locked card name that isn't in [pool] (e.g. basic lands) to its
 *   definition, for color/land classification. Defaults to "unknown" (treated as a colorless spell).
 * @return A deck list as a map of card name to count
 */
fun buildHeuristicSealedDeck(
    pool: List<CardDefinition>,
    locked: Map<String, Int> = emptyMap(),
    targetSize: Int = 40,
    resolveCard: (String) -> CardDefinition? = { null },
): Map<String, Int> {
    val sanitizedLocked = locked.filterValues { it > 0 }
    if (sanitizedLocked.isNotEmpty()) {
        return completeSealedDeck(pool, sanitizedLocked, targetSize, resolveCard)
    }
    return buildFreshSealedDeck(pool, targetSize)
}

private fun buildFreshSealedDeck(pool: List<CardDefinition>, targetSize: Int): Map<String, Int> {
    val deck = mutableMapOf<String, Int>()
    val ratings = pool.associateWith { LimitedCardRater.rate(it) }

    val nonLands = pool.filter { !it.typeLine.isLand }
    val poolLands = pool.filter { it.typeLine.isLand && !it.typeLine.isBasicLand }

    // Score each color by the total limited rating of its cards
    val colorScores = mutableMapOf<Color, Double>()
    for (card in nonLands) {
        val rating = ratings[card] ?: 0.0
        for (color in spellColors(card)) {
            colorScores[color] = (colorScores[color] ?: 0.0) + rating
        }
    }

    // Pick top 2 colors
    val bestColors = colorScores.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()

    // Select cards castable off the chosen colors, sorted by rating (best first)
    val candidates = nonLands.filter { castableWithColors(it, bestColors) }.sortedByDescending { ratings[it] ?: 0.0 }

    // Pick top 23, but ensure a reasonable mana curve:
    // At least 5 creatures at CMC <= 3, and at least 13 creatures total
    val selected = mutableListOf<CardDefinition>()
    val remaining = candidates.toMutableList()

    // Spell slots scale with deck size (23 nonland cards in a 40-card limited deck).
    val spellTarget = (targetSize * 23) / 40

    // First pass: grab the best cards by rating, capped at 4 copies per name (sealed rule)
    val picksByName = mutableMapOf<String, Int>()
    val topPicks = mutableListOf<CardDefinition>()
    val iter = remaining.iterator()
    while (iter.hasNext() && topPicks.size < spellTarget) {
        val card = iter.next()
        val current = picksByName[card.name] ?: 0
        if (current >= 4) continue
        picksByName[card.name] = current + 1
        topPicks.add(card)
        iter.remove()
    }
    selected.addAll(topPicks)

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

        var cutIdx = 0
        var replaceIdx = 0
        var swapsDone = 0
        while (swapsDone < swapCount && cutIdx < cuttable.size && replaceIdx < cheapReplacements.size) {
            val replacement = cheapReplacements[replaceIdx]
            val current = picksByName[replacement.name] ?: 0
            if (current >= 4) { replaceIdx++; continue }
            val cut = cuttable[cutIdx]
            if (selected.remove(cut)) {
                picksByName[cut.name] = ((picksByName[cut.name] ?: 0) - 1).coerceAtLeast(0)
                selected.add(replacement)
                picksByName[replacement.name] = current + 1
                swapsDone++
            }
            cutIdx++
            replaceIdx++
        }
    }

    for (card in selected) {
        deck[card.name] = (deck[card.name] ?: 0) + 1
    }

    // Add on-color non-basic lands (capped at 4 per name)
    for (land in poolLands) {
        if (!landFitsColors(land, bestColors)) continue
        val current = deck[land.name] ?: 0
        if (current >= 4) continue
        deck[land.name] = current + 1
    }

    // Fill to target size with basic lands split by color
    val landsNeeded = (targetSize - deck.values.sum()).coerceAtLeast(0)
    addBasicLands(deck, bestColors, landsNeeded) { color -> selected.sumOf { it.manaCost.colorCount[color] ?: 0 } }

    return deck
}

/**
 * Completion mode: keep every locked card, then fill the remaining slots with the best on-color
 * spells from the pool, on-color non-basic lands, and a basic-land mana base — up to [targetSize].
 */
private fun completeSealedDeck(
    pool: List<CardDefinition>,
    locked: Map<String, Int>,
    targetSize: Int,
    resolveCard: (String) -> CardDefinition?,
): Map<String, Int> {
    val deck = locked.toMutableMap()
    val ratings = pool.associateWith { LimitedCardRater.rate(it) }
    val poolCounts = pool.groupingBy { it.name }.eachCount()
    val byName = pool.associateBy { it.name }
    fun defOf(name: String): CardDefinition? = byName[name] ?: resolveCard(name)

    // Bias colors toward what the player has already locked in (so completion stays on-theme),
    // falling back to the pool's overall color weight if the partial deck has no colored spells yet.
    val colorScores = mutableMapOf<Color, Double>()
    for ((name, count) in deck) {
        val def = defOf(name) ?: continue
        if (def.typeLine.isLand) continue
        for (color in spellColors(def)) colorScores[color] = (colorScores[color] ?: 0.0) + count
    }
    if (colorScores.isEmpty()) {
        for (card in pool.filter { !it.typeLine.isLand }) {
            val rating = ratings[card] ?: 0.0
            for (color in spellColors(card)) colorScores[color] = (colorScores[color] ?: 0.0) + rating
        }
    }
    val bestColors = colorScores.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()

    val spellTarget = (targetSize * 23) / 40
    fun total() = deck.values.sum()
    fun nonLandCount() = deck.entries.sumOf { (n, c) -> if (defOf(n)?.typeLine?.isLand == true) 0 else c }

    // Fill spell slots with the best on-color (or colorless) candidates not already maxed out.
    val candidates = pool
        .filter { !it.typeLine.isLand && castableWithColors(it, bestColors) }
        .sortedByDescending { ratings[it] ?: 0.0 }
    for (card in candidates) {
        if (nonLandCount() >= spellTarget || total() >= targetSize) break
        val have = deck[card.name] ?: 0
        val available = poolCounts[card.name] ?: 0
        if (have >= minOf(4, available)) continue
        deck[card.name] = have + 1
    }

    // On-color non-basic lands from the pool.
    for (land in pool.filter { it.typeLine.isLand && !it.typeLine.isBasicLand && landFitsColors(it, bestColors) }) {
        if (total() >= targetSize) break
        val have = deck[land.name] ?: 0
        val available = poolCounts[land.name] ?: 0
        if (have >= minOf(4, available)) continue
        deck[land.name] = have + 1
    }

    // Basic-land mana base for the rest, split by the deck's actual colored mana symbols.
    val landsNeeded = (targetSize - total()).coerceAtLeast(0)
    addBasicLands(deck, bestColors, landsNeeded) { color ->
        deck.entries.sumOf { (n, c) ->
            val def = defOf(n) ?: return@sumOf 0
            if (def.typeLine.isLand) 0 else (def.manaCost.colorCount[color] ?: 0) * c
        }
    }

    return deck
}

private val COLOR_TO_LAND = mapOf(
    Color.WHITE to "Plains", Color.BLUE to "Island", Color.BLACK to "Swamp",
    Color.RED to "Mountain", Color.GREEN to "Forest"
)

/**
 * Add [landsNeeded] basic lands to [deck], split between [bestColors] proportionally to each color's
 * mana-symbol weight (from [symbolCount]). Mono-color dumps all into one type; colorless falls back
 * to Forest. No-op when no lands are needed.
 */
private fun addBasicLands(
    deck: MutableMap<String, Int>,
    bestColors: Set<Color>,
    landsNeeded: Int,
    symbolCount: (Color) -> Int,
) {
    if (landsNeeded <= 0) return
    when {
        bestColors.size >= 2 -> {
            val c1 = bestColors.first()
            val c2 = bestColors.last()
            val c1count = symbolCount(c1)
            val c2count = symbolCount(c2)
            val total = (c1count + c2count).coerceAtLeast(1)
            val l1 = (landsNeeded * c1count / total).coerceIn(1, landsNeeded)
            deck[COLOR_TO_LAND[c1]!!] = (deck[COLOR_TO_LAND[c1]!!] ?: 0) + l1
            deck[COLOR_TO_LAND[c2]!!] = (deck[COLOR_TO_LAND[c2]!!] ?: 0) + (landsNeeded - l1)
        }
        bestColors.size == 1 -> {
            val land = COLOR_TO_LAND[bestColors.first()]!!
            deck[land] = (deck[land] ?: 0) + landsNeeded
        }
        else -> deck["Forest"] = (deck["Forest"] ?: 0) + landsNeeded
    }
}

/**
 * The colors a spell asks of its caster: the colored symbols in its mana cost (hybrid and Phyrexian
 * pips count for their colors), plus a color indicator. Deliberately *not* [CardDefinition.colors] —
 * devoid makes a card colorless (CR 702.114a) without making its `{U}` payable with any mana.
 */
internal fun spellColors(card: CardDefinition): Set<Color> =
    card.manaCost.colors + (card.colorIndicator ?: emptySet())

/**
 * Whether a deck whose mana comes from [deckColors] basics can pay [card]'s mana cost. Every pip
 * must be payable: a colored pip needs its color, a hybrid either half, a Phyrexian pip can always
 * be paid with 2 life (CR 107.4f) and a monocolored hybrid `{2/W}` with two generic (CR 107.4e).
 * A `{C}` pip needs a colorless *source*, which a basic-land mana base doesn't have. A card with no
 * mana cost at all (cast only by suspend or the like) falls back to its color indicator.
 */
internal fun castableWithColors(card: CardDefinition, deckColors: Set<Color>): Boolean {
    if (card.manaCost.symbols.isEmpty()) return (card.colorIndicator ?: emptySet()).all { it in deckColors }
    return manaCostPayableWith(card.manaCost, deckColors)
}

internal fun manaCostPayableWith(cost: ManaCost, deckColors: Set<Color>): Boolean =
    cost.symbols.all { symbol ->
        when (symbol) {
            is ManaSymbol.Phyrexian, is ManaSymbol.HybridPhyrexian, is ManaSymbol.MonocolorHybrid -> true
            is ManaSymbol.Colorless -> false
            else -> symbol.colors.isEmpty() || symbol.colors.any { it in deckColors }
        }
    }

private val BASIC_TYPE_COLORS = mapOf(
    Subtype.PLAINS to Color.WHITE, Subtype.ISLAND to Color.BLUE, Subtype.SWAMP to Color.BLACK,
    Subtype.MOUNTAIN to Color.RED, Subtype.FOREST to Color.GREEN,
)

/**
 * Whether a non-basic [land] belongs in a deck playing [deckColors]:
 *  - a land that taps for colored mana (its basic land types, CR 305.6, and the `Add …` effects of
 *    its activated abilities) fits when at least one of those colors is a deck color — Goblin-town
 *    ({B} or {R}) is in a B/x deck, never a G/W one; a five-color land fits everywhere;
 *  - a land that only fetches (Wooded Foothills) fits when a basic land type it names is on-color,
 *    or when it names none ("a basic land card", Evolving Wilds);
 *  - anything else (colorless mana, pure utility) fits when every colored symbol in its rules text
 *    — the cost of its other abilities — is a deck color.
 */
internal fun landFitsColors(land: CardDefinition, deckColors: Set<Color>): Boolean {
    val produced = mutableSetOf<Color>()
    land.typeLine.subtypes.forEach { BASIC_TYPE_COLORS[it]?.let(produced::add) }
    land.script.activatedAbilities.forEach { collectProducedColors(it.effect, produced) }
    if (produced.isNotEmpty()) return produced.any { it in deckColors }

    val fetched = BASIC_TYPE_COLORS.filterKeys { type ->
        Regex("""\b${type.value}\b""").containsMatchIn(land.oracleText)
    }.values.toSet()
    if (fetched.isNotEmpty()) return fetched.any { it in deckColors }

    return land.colorIdentity.all { it in deckColors }
}

private fun collectProducedColors(effect: Effect, out: MutableSet<Color>) {
    when (effect) {
        is AddManaEffect -> out.add(effect.color)
        is AddColorlessManaEffect -> Unit
        is AddManaOfChoiceEffect -> collectChoiceColors(effect.colorSet, out)
        is AddDynamicManaEffect -> out.addAll(effect.allowedColors)
        is AddAnyColorManaSpendOnChosenTypeEffect, is AddOneManaOfEachColorAmongEffect -> out.addAll(Color.entries)
        is CompositeEffect -> effect.effects.forEach { collectProducedColors(it, out) }
        else -> Unit
    }
}

/** State-dependent color sets (chosen color, colors among permanents, …) could be any color. */
private fun collectChoiceColors(colorSet: ManaColorSet, out: MutableSet<Color>) {
    when (colorSet) {
        is ManaColorSet.Specific -> out.addAll(colorSet.colors)
        is ManaColorSet.Union -> colorSet.members.forEach { collectChoiceColors(it, out) }
        else -> out.addAll(Color.entries)
    }
}
