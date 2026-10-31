package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.BecomePreparedEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import kotlin.math.pow

/**
 * Heuristic sealed deck builder that uses [LimitedCardRater] to evaluate card quality.
 *
 * Algorithm:
 * 1. Rate every card in the pool
 * 2. Score each color by the total rating of its cards (weighted toward creatures)
 * 3. Pick the best 2 colors
 * 4. Select the top 23 on-color spells by rating — each copy valued at its marginal worth, so a
 *    legend's duplicates rank below its first copy ([rankByMarginalValue]) — then refine: drop
 *    spells that do nothing in this deck ([DeckDependencies]), reach [CREATURES_PER_40] creatures
 *    when the pool allows, swap toward a deck that feeds its own payoffs ([DeckSynergy]), and cut
 *    cards whose pips the mana base can't support ([PipDemand])
 * 5. When the pair can't fill the spell slots, add a light splash ([lightSplash]) so the deck runs
 *    at most one land over the 17 rather than padding the gap with basics
 * 6. Fill to 40 cards with basic lands split by weighted pip demand, with per-color source floors
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

/** Creatures a 40-card limited deck wants; scaled to the deck size. */
internal const val CREATURES_PER_40 = 14

/** Creatures at mana value 3 or less a 40-card limited deck wants. */
private const val CHEAP_CREATURES_PER_40 = 5

/**
 * Rating points a deck gives up per payoff-share it leaves unfed ([SpellSelection.feedPayoffs]):
 * under one rarity step, so a payoff with no fuel falls toward a vanilla body of its cost, not off
 * the bottom of the pool.
 */
internal const val UNDERFED_COST = 0.75

/** Most synergy swaps [SpellSelection.feedPayoffs] makes; each one is the best swap left. */
private const val SYNERGY_SWAPS = 6

/** Smallest whole-deck gain worth a swap; below it a swap is noise between near-equal cards. */
private const val MIN_SWAP_GAIN = 0.05

/** Rounds of the mana-support repair before the builder settles for what it has. */
private const val MANA_REPAIR_ROUNDS = 10

private fun buildFreshSealedDeck(pool: List<CardDefinition>, targetSize: Int): Map<String, Int> {
    val deck = mutableMapOf<String, Int>()
    val ratings = pool.associateWith { LimitedCardRater.rate(it) }

    val nonLands = pool.filter { !it.typeLine.isLand }
    val poolLands = pool.filter { it.typeLine.isLand && (!it.typeLine.isBasicLand || it.typeLine.isSnow) }

    // Score each color by the total limited rating of its cards
    val colorScores = mutableMapOf<Color, Double>()
    for (card in nonLands) {
        val rating = ratings[card] ?: 0.0
        for (color in spellColors(card)) {
            colorScores[color] = (colorScores[color] ?: 0.0) + rating
        }
    }

    // Pick top 2 colors. Only a pool too thin in them to fill the spell slots splashes a third.
    val bestColors = colorScores.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()

    // Cards castable off the chosen colors, best first — by each copy's *marginal* value, so a
    // second copy of a legend ranks below the first (see [rankByMarginalValue]).
    val ranked = rankByMarginalValue(nonLands.filter { castableWithColors(it, bestColors) }) { ratings[it] ?: 0.0 }

    // On-color non-basic lands (capped at 4 per name) are fixed first: the mana-support check
    // below needs to know which colors they already make.
    val nonbasics = mutableMapOf<String, Int>()
    fun takeLands(fits: (CardDefinition) -> Boolean) {
        for (land in poolLands) {
            if (!fits(land)) continue
            val current = nonbasics[land.name] ?: 0
            if (current >= 4) continue
            nonbasics[land.name] = current + 1
        }
    }
    takeLands { landFitsColors(it, bestColors) }
    val landDefs = poolLands.associateBy { it.name }
    val fixedSources: (Color) -> Int = { color ->
        nonbasics.entries.sumOf { (name, n) -> if (color in producedColors(landDefs.getValue(name))) n else 0 }
    }

    // Spell slots scale with deck size (23 nonland cards in a 40-card limited deck).
    val spellTarget = (targetSize * 23) / 40
    val selection = SpellSelection(ranked, spellTarget)
    selection.fill()
    selection.dropInertCards()
    selection.meetCreatureMinimum(
        minCreatures = targetSize * CREATURES_PER_40 / 40,
        minCheap = targetSize * CHEAP_CREATURES_PER_40 / 40,
    )
    selection.feedPayoffs()
    selection.supportPips(bestColors, nonbasicCount = nonbasics.values.sum(), targetSize = targetSize, fixedSources = fixedSources)

    // A pool too thin in the pair fills the gap with a light splash before it reaches for a
    // nineteenth land: 22 lands beside 18 spells floods out of most games (2026-10-31 game 2d31a409).
    val maxLands = targetSize - spellTarget + 1
    val splash = lightSplash(
        nonLands = nonLands,
        ratings = ratings,
        mainColors = bestColors,
        taken = selection.selected.map { it.card },
        need = minOf(targetSize - maxLands - selection.selected.size, targetSize * SPLASH_CARDS_PER_40 / 40),
    )

    // A splash colour is a deck colour for the lands too: a dual making it and a main colour is a
    // splash source, not an off-colour land.
    if (splash != null) {
        val withSplash = bestColors + splash.color
        takeLands { splash.color in producedColors(it) && !landFitsColors(it, bestColors) && landFitsColors(it, withSplash) }
    }
    val deckSpells = selection.selected.map { it.card } + splash?.cards.orEmpty()
    utilityLand(poolLands, bestColors + listOfNotNull(splash?.color), deckSpells)?.let { nonbasics[it.name] = 1 }

    for (slot in selection.selected) {
        deck[slot.card.name] = (deck[slot.card.name] ?: 0) + 1
    }
    splash?.cards?.forEach { deck[it.name] = (deck[it.name] ?: 0) + 1 }
    for ((name, n) in nonbasics) deck[name] = (deck[name] ?: 0) + n

    // A splash colour gets [SPLASH_SOURCES] sources — its non-basics first, basics for the rest.
    if (splash != null) {
        val land = COLOR_TO_LAND.getValue(splash.color)
        val basics = (SPLASH_SOURCES - fixedSources(splash.color)).coerceAtLeast(0)
        if (basics > 0) deck[land] = (deck[land] ?: 0) + basics
    }

    // Fill to target size with basic lands split by weighted pip demand. Only a pool with nothing
    // castable left — not even a splash — still pads past [maxLands]: a deck has to reach 40.
    val landsNeeded = (targetSize - deck.values.sum()).coerceAtLeast(0)
    addBasicLands(
        deck, bestColors, landsNeeded,
        spells = selection.selected.map { it.card to 1 },
        fixedSources = fixedSources,
        landCount = targetSize - selection.selected.size - (splash?.cards?.size ?: 0),
    )

    return deck
}

/** Sources a light splash runs: enough to cast a single-pip card by the mid-game. */
private const val SPLASH_SOURCES = 3

/** Most cards a light splash adds to a 40-card deck: past that, the pair itself was the wrong call. */
private const val SPLASH_CARDS_PER_40 = 4

/** The off-colour cards a light splash adds, all needing one pip of [color] and nothing else off-colour. */
private class Splash(val color: Color, val cards: List<CardDefinition>)

/**
 * Up to [need] off-colour spells for a two-colour deck that couldn't fill its spell slots: the
 * best-rated cards that a single basic-land colour on top of [mainColors] makes castable, each
 * asking exactly one plain pip of it. The colour picked is the one whose best [need] cards are
 * worth the most. `null` when nothing is needed or nothing in the pool qualifies.
 */
private fun lightSplash(
    nonLands: List<CardDefinition>,
    ratings: Map<CardDefinition, Double>,
    mainColors: Set<Color>,
    taken: List<CardDefinition>,
    need: Int,
): Splash? {
    if (need <= 0 || mainColors.size != 2) return null
    val takenNames = taken.groupingBy { it.name }.eachCount()
    val offColour = nonLands.filter { !castableWithColors(it, mainColors) }
    val options = (Color.entries - mainColors).mapNotNull { color ->
        val withSplash = mainColors + color
        val copies = mutableMapOf<String, Int>()
        val cards = offColour
            .filter { castableWithColors(it, withSplash) && strictPips(it, color) == 1 && !DeckDependencies.inert(it, taken) }
            .sortedByDescending { ratings[it] ?: 0.0 }
            .filter { card ->
                val n = (takenNames[card.name] ?: 0) + (copies[card.name] ?: 0)
                (n < 4).also { if (it) copies[card.name] = (copies[card.name] ?: 0) + 1 }
            }
            .take(need)
        cards.takeIf { it.isNotEmpty() }?.let { Splash(color, it) }
    }
    return options.maxByOrNull { option -> option.cards.sumOf { ratings[it] ?: 0.0 } }
}

/**
 * The fresh build's spell picks, refined in passes over the value-ordered [ranked] candidates:
 * take the best, drop cards that do nothing in this deck, reach the creature minimum, and cut
 * cards whose colored pips the mana base can't support. Each pass swaps one card for the best
 * remaining candidate that fixes the problem, so the deck stays as strong as the fix allows.
 */
private class SpellSelection(private val ranked: List<RankedCopy>, private val spellTarget: Int) {
    private val value = ranked.associate { it.slot to it.value }
    val selected = mutableListOf<PoolSlot>()
    private val excluded = mutableSetOf<String>()
    private val copies = mutableMapOf<String, Int>()

    private fun cards() = selected.map { it.card }
    private fun creatures() = selected.count { it.card.typeLine.isCreature }

    private fun canTake(slot: PoolSlot) =
        slot !in selected && slot.card.name !in excluded && (copies[slot.card.name] ?: 0) < 4

    private fun take(slot: PoolSlot) {
        selected += slot
        copies[slot.card.name] = (copies[slot.card.name] ?: 0) + 1
    }

    private fun drop(slot: PoolSlot) {
        if (selected.remove(slot)) copies[slot.card.name] = ((copies[slot.card.name] ?: 0) - 1).coerceAtLeast(0)
    }

    private fun best(predicate: (PoolSlot) -> Boolean): PoolSlot? =
        ranked.firstOrNull { canTake(it.slot) && predicate(it.slot) }?.slot

    /** First pass: the best cards by value, capped at 4 copies per name (sealed rule). */
    fun fill(accept: (PoolSlot) -> Boolean = { true }) {
        while (selected.size < spellTarget) take(best(accept) ?: return)
    }

    /**
     * Drop every spell that would do nothing in this deck ([DeckDependencies.inert]) — a
     * planeswalker tutor with no planeswalkers — and refill with cards that do something. Repeats
     * because a drop can strand another card that relied on it.
     */
    fun dropInertCards() {
        repeat(5) {
            val dead = selected.filter { DeckDependencies.inert(it.card, cards()) }
            if (dead.isEmpty()) return
            dead.forEach { excluded += it.card.name; drop(it) }
            fill { !DeckDependencies.inert(it.card, cards()) }
        }
    }

    /**
     * Reach [minCreatures] (and [minCheap] cheap ones) when the pool allows, cutting the weakest
     * noncreature spells — with combat tricks and creature Auras discounted as the creature count
     * drops, since they need bodies to do anything. When the pool runs out of creatures, the
     * tricks still left lose their slots to any better-valued noncreature spell.
     */
    fun meetCreatureMinimum(minCreatures: Int, minCheap: Int) {
        fun adjusted(slot: PoolSlot): Double {
            val base = value[slot] ?: 0.0
            val creatures = creatures()
            if (creatures >= minCreatures || !needsCreatures(slot.card)) return base
            return base * creatures.toDouble() / minCreatures
        }
        fun live(slot: PoolSlot) = !DeckDependencies.inert(slot.card, cards())

        while (creatures() < minCreatures) {
            val add = best { it.card.typeLine.isCreature } ?: break
            val victim = selected.filter { !it.card.typeLine.isCreature }.minByOrNull(::adjusted) ?: break
            drop(victim)
            take(add)
        }
        fun cheap(slot: PoolSlot) = slot.card.typeLine.isCreature && slot.card.cmc <= 3
        while (selected.count(::cheap) < minCheap) {
            val add = best(::cheap) ?: break
            val victim = selected.filter { !cheap(it) }.minByOrNull(::adjusted) ?: break
            drop(victim)
            take(add)
        }
        if (creatures() >= minCreatures) return
        repeat(selected.size) {
            val trick = selected.filter { needsCreatures(it.card) }.minByOrNull(::adjusted) ?: return
            val add = best { !it.card.typeLine.isCreature && !needsCreatures(it.card) && live(it) } ?: return
            if ((value[add] ?: 0.0) <= adjusted(trick)) return
            drop(trick)
            take(add)
        }
    }

    /**
     * Swap cards while a swap raises the whole selection's worth — the sum of the cards' values,
     * less [UNDERFED_COST] per payoff-share the deck leaves unfed ([DeckSynergy.shortfall]). So a
     * payoff with too little fuel drops toward its body, a second sacrifice outlet earns its slot
     * beside Blood Artist, and cutting the last outlet for a slightly better vanilla creature has a
     * price. Swaps compare whole decks, never one card's score against another's.
     *
     * The feeding swaps go first, with every payoff kept; only then may a payoff still short of
     * fuel be cut. Swaps keep a card's role ([bestSynergySwap]) and never bring in a card that does
     * nothing in this deck.
     */
    fun feedPayoffs() {
        var swaps = 0
        // Feed first, cut second: one swap at a time, cutting an unfed payoff always beats feeding
        // it a third of the way, though three feeding swaps beat the cut.
        for (keepPayoffs in listOf(true, false)) {
            while (swaps < SYNERGY_SWAPS) {
                val candidates = ranked.map { it.slot }.filter { canTake(it) && !DeckDependencies.inert(it.card, cards()) }
                val (out, inn) = bestSynergySwap(selected, candidates, keepPayoffs) { value[it] ?: 0.0 } ?: break
                drop(out)
                take(inn)
                swaps++
            }
        }
    }

    /**
     * Cut the cards whose colored pips the mana base can't support. The most demanding card of
     * each color sets that color's source floor ([PipDemand.sourcesNeeded]); when the two floors
     * don't fit in the deck's lands, the heaviest card of the *less* demanded color goes, replaced
     * by the best candidate that asks less of that color.
     */
    fun supportPips(colors: Set<Color>, nonbasicCount: Int, targetSize: Int, fixedSources: (Color) -> Int) {
        if (colors.size < 2) return
        repeat(MANA_REPAIR_ROUNDS) {
            val landCount = targetSize - selected.size
            val basics = (landCount - nonbasicCount).coerceAtLeast(0)
            val spells = selected.map { it.card to 1 }
            val demand = pipDemand(spells, colors)
            val floors = sourceFloors(spells, colors, landCount)
            val allocation = PipDemand.allocate(demand, basics, floors, fixedSources)
            if (PipDemand.shortfall(allocation, floors, fixedSources) == 0) return

            val minor = colors.minByOrNull { demand[it] ?: 0.0 } ?: return
            val heaviest = selected.maxOfOrNull { strictPips(it.card, minor) } ?: return
            if (heaviest == 0) return
            val victim = selected.filter { strictPips(it.card, minor) == heaviest }
                .minByOrNull { value[it] ?: 0.0 } ?: return
            excluded += victim.card.name
            selected.filter { it.card.name == victim.card.name }.forEach(::drop)
            fill { strictPips(it.card, minor) < heaviest && !DeckDependencies.inert(it.card, cards()) }
        }
    }
}

/**
 * The one swap of a [selected] card for a [candidates] card that most raises the selection's worth —
 * the sum of [value], less [UNDERFED_COST] per payoff-share left unfed ([DeckSynergy.shortfall]) —
 * or null when none gains [MIN_SWAP_GAIN]. A swap keeps the card's role (creature for creature,
 * cheap creature for cheap creature), so it never undoes the creature minimum. With [keepPayoffs]
 * no payoff is swapped out.
 */
internal fun bestSynergySwap(
    selected: List<PoolSlot>,
    candidates: List<PoolSlot>,
    keepPayoffs: Boolean = false,
    value: (PoolSlot) -> Double,
): Pair<PoolSlot, PoolSlot>? {
    fun cheap(card: CardDefinition) = card.typeLine.isCreature && card.cmc <= 3
    fun sameRole(a: CardDefinition, b: CardDefinition) =
        a.typeLine.isCreature == b.typeLine.isCreature && cheap(a) == cheap(b)
    fun worth(slots: List<PoolSlot>) =
        slots.sumOf(value) - UNDERFED_COST * DeckSynergy.shortfall(slots.map { SynergyAxes.of(it.card) })

    val current = worth(selected)
    var best: Pair<PoolSlot, PoolSlot>? = null
    var bestGain = MIN_SWAP_GAIN
    for (out in selected) for (inn in candidates) {
        if (keepPayoffs && SynergyAxes.of(out.card).rewards.isNotEmpty()) continue
        if (inn.card.name == out.card.name || !sameRole(inn.card, out.card)) continue
        val gain = worth(selected.map { if (it == out) inn else it }) - current
        if (gain > bestGain) { bestGain = gain; best = out to inn }
    }
    return best
}

/**
 * Whether [card] is a spell that only does something with a creature of ours to put it on — a
 * pump, a combat trick, an Aura that isn't a Pacifism-style answer.
 */
private fun needsCreatures(card: CardDefinition): Boolean {
    if (card.typeLine.isCreature) return false
    val tags = CardIntentAnalyzer.analyze(card).tags
    if (tags.any { it in ANSWER_TAGS }) return false
    return IntentTag.PUMP in tags || IntentTag.COMBAT_TRICK in tags || card.typeLine.hasSubtype(Subtype.AURA)
}

private val ANSWER_TAGS = setOf(
    IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.NEUTRALIZE, IntentTag.FIGHT,
)

/** Plain colored pips of [color] in [card]'s cost — the pips no other color or generic can pay. */
private fun strictPips(card: CardDefinition, color: Color): Int = card.manaCost.colorCount[color] ?: 0

/**
 * [card]'s pips per deck color for the basic split: plain pips count 1; a hybrid pip counts 1 for
 * its only on-color side or ½ for each when both are; Phyrexian and `{2/C}` pips count ½, since
 * they can be paid another way.
 */
private fun pipsFor(card: CardDefinition, colors: Set<Color>): Map<Color, Double> {
    val out = mutableMapOf<Color, Double>()
    fun add(color: Color, amount: Double) { if (color in colors) out[color] = (out[color] ?: 0.0) + amount }
    for (symbol in card.manaCost.symbols) {
        when (symbol) {
            is ManaSymbol.Colored -> add(symbol.color, 1.0)
            is ManaSymbol.Phyrexian -> add(symbol.color, 0.5)
            is ManaSymbol.MonocolorHybrid -> add(symbol.color, 0.5)
            is ManaSymbol.HybridPair -> {
                val sides = listOf(symbol.color1, symbol.color2).filter { it in colors }
                val share = if (symbol is ManaSymbol.HybridPhyrexian) 0.5 else 1.0
                sides.forEach { add(it, share / sides.size) }
            }
            else -> Unit
        }
    }
    return out
}

/** Weighted basic-land demand per color ([PipDemand.weight]) over [spells] (card → copies). */
private fun pipDemand(spells: List<Pair<CardDefinition, Int>>, colors: Set<Color>): Map<Color, Double> {
    val demand = mutableMapOf<Color, Double>()
    for ((card, count) in spells) {
        for ((color, pips) in pipsFor(card, colors)) {
            demand[color] = (demand[color] ?: 0.0) + PipDemand.weight(pips, card.cmc.toDouble()) * count
        }
    }
    return demand
}

/** Each color's source floor: what its most demanding card in [spells] needs ([PipDemand.sourcesNeeded]). */
private fun sourceFloors(spells: List<Pair<CardDefinition, Int>>, colors: Set<Color>, landCount: Int): Map<Color, Int> =
    colors.associateWith { color ->
        spells.maxOfOrNull { (card, _) -> PipDemand.sourcesNeeded(strictPips(card, color).toDouble(), landCount) } ?: 0
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
    // Copies the player already locked count toward the legend discount: a locked legend's next
    // copy from the pool is already its second.
    val candidates = rankByMarginalValue(
        pool.filter { !it.typeLine.isLand && castableWithColors(it, bestColors) },
        alreadyIncluded = { name -> locked[name] ?: 0 },
    ) { ratings[it] ?: 0.0 }.map { it.slot.card }
    for (card in candidates) {
        if (nonLandCount() >= spellTarget || total() >= targetSize) break
        val have = deck[card.name] ?: 0
        val available = poolCounts[card.name] ?: 0
        if (have >= minOf(4, available)) continue
        deck[card.name] = have + 1
    }

    // On-color non-basic lands from the pool.
    for (land in pool.filter { it.typeLine.isLand && (!it.typeLine.isBasicLand || it.typeLine.isSnow) && landFitsColors(it, bestColors) }) {
        if (total() >= targetSize) break
        val have = deck[land.name] ?: 0
        val available = poolCounts[land.name] ?: 0
        if (have >= minOf(4, available)) continue
        deck[land.name] = have + 1
    }
    if (total() < targetSize && deck.keys.none { name -> defOf(name)?.let(::isColorlessUtilityLand) == true }) {
        val spells = deck.keys.mapNotNull { defOf(it)?.takeIf { def -> !def.typeLine.isLand } }
        utilityLand(pool.filter { it.typeLine.isLand && !it.typeLine.isBasicLand }, bestColors, spells)
            ?.let { deck[it.name] = 1 }
    }

    // Basic-land mana base for the rest, split by the deck's weighted colored-pip demand.
    val landsNeeded = (targetSize - total()).coerceAtLeast(0)
    val deckLands = deck.mapNotNull { (n, c) -> defOf(n)?.takeIf { it.typeLine.isLand }?.let { it to c } }
    addBasicLands(
        deck, bestColors, landsNeeded,
        spells = deck.mapNotNull { (n, c) -> defOf(n)?.takeIf { !it.typeLine.isLand }?.let { it to c } },
        fixedSources = { color -> deckLands.sumOf { (land, c) -> if (color in producedColors(land)) c else 0 } },
        landCount = landsNeeded + deckLands.sumOf { it.second },
    )

    return deck
}

private val COLOR_TO_LAND = mapOf(
    Color.WHITE to "Plains", Color.BLUE to "Island", Color.BLACK to "Swamp",
    Color.RED to "Mountain", Color.GREEN to "Forest"
)

/**
 * Add [landsNeeded] basic lands to [deck], split between [bestColors] by the weighted pip demand of
 * [spells] (card → copies; [PipDemand.weight]) and raised to each color's source floor
 * ([PipDemand.sourcesNeeded], counting the [fixedSources] non-basic lands already make) where the
 * other color has room. Mono-color dumps all into one type; colorless falls back to Forest.
 */
private fun addBasicLands(
    deck: MutableMap<String, Int>,
    bestColors: Set<Color>,
    landsNeeded: Int,
    spells: List<Pair<CardDefinition, Int>>,
    fixedSources: (Color) -> Int,
    landCount: Int,
) {
    if (landsNeeded <= 0) return
    if (bestColors.isEmpty()) {
        deck["Forest"] = (deck["Forest"] ?: 0) + landsNeeded
        return
    }
    val demand = pipDemand(spells, bestColors).ifEmpty { bestColors.associateWith { 1.0 } }
    val floors = sourceFloors(spells, bestColors, landCount)
    for ((color, n) in PipDemand.allocate(demand, landsNeeded, floors, fixedSources)) {
        if (n <= 0) continue
        val land = COLOR_TO_LAND.getValue(color)
        deck[land] = (deck[land] ?: 0) + n
    }
}

/**
 * One physical card in the pool. Copies of the same name share a [CardDefinition] (equal, often the
 * same instance), so the builder tracks them by position to tell "the first Yargle" from the second.
 */
internal data class PoolSlot(val index: Int, val card: CardDefinition)

/** A pool copy and what adding it to the deck is worth, given the copies of its name ranked above it. */
internal data class RankedCopy(val slot: PoolSlot, val value: Double)

/**
 * What the second and later copies of a legendary permanent are worth, relative to the first, per
 * copy already in the deck. The legend rule (CR 704.5j) makes a drawn duplicate dead while the first
 * is on the battlefield — it is only insurance against removal — so `0.65^k` for the copy with `k`
 * copies ahead of it: a strong legend's second copy still makes a deck, a filler legend's doesn't.
 */
internal const val LEGEND_EXTRA_COPY_FACTOR = 0.65

/**
 * Rank [cards] best first by each copy's *marginal* value: [rating] for the first copy of a name,
 * discounted by [LEGEND_EXTRA_COPY_FACTOR] per earlier copy for a legendary permanent (copies already
 * in the deck, [alreadyIncluded], count as earlier). Non-legendary copies are worth their full
 * rating every time, so for a pool without legend duplicates this is exactly a by-rating sort.
 *
 * Ties keep pool order, so the result is deterministic.
 */
internal fun rankByMarginalValue(
    cards: List<CardDefinition>,
    alreadyIncluded: (String) -> Int = { 0 },
    rating: (CardDefinition) -> Double,
): List<RankedCopy> {
    val seen = mutableMapOf<String, Int>()
    return cards.mapIndexed { index, card ->
        val earlier = alreadyIncluded(card.name) + (seen[card.name] ?: 0)
        seen[card.name] = (seen[card.name] ?: 0) + 1
        val legendDuplicate = earlier > 0 && card.typeLine.isLegendary && card.typeLine.isPermanent
        val value = if (legendDuplicate) rating(card) * LEGEND_EXTRA_COPY_FACTOR.pow(earlier) else rating(card)
        RankedCopy(PoolSlot(index, card), value)
    }.sortedByDescending { it.value }
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
 * be paid with 2 life (CR 107.4f) and a monocolored hybrid `{2/W}` with two generic (CR 107.4e) —
 * but at most [MAX_OFF_COLOR_TWOBRID_PIPS] of those off-color, or the card costs far above its slot.
 * A `{C}` pip needs a colorless *source*, which a basic-land mana base doesn't have. A card with no
 * mana cost at all (cast only by suspend or the like) falls back to its color indicator.
 */
internal fun castableWithColors(card: CardDefinition, deckColors: Set<Color>): Boolean {
    if (card.manaCost.symbols.isEmpty()) return (card.colorIndicator ?: emptySet()).all { it in deckColors }
    return manaCostPayableWith(card.manaCost, deckColors)
}

internal fun manaCostPayableWith(cost: ManaCost, deckColors: Set<Color>): Boolean =
    cost.symbols.count { it is ManaSymbol.MonocolorHybrid && it.color !in deckColors } <= MAX_OFF_COLOR_TWOBRID_PIPS &&
        cost.symbols.all { symbol ->
            when (symbol) {
                is ManaSymbol.Phyrexian, is ManaSymbol.HybridPhyrexian, is ManaSymbol.MonocolorHybrid -> true
                is ManaSymbol.Colorless -> false
                else -> symbol.colors.isEmpty() || symbol.colors.any { it in deckColors }
            }
        }

/**
 * Off-color `{2/C}` pips a castable card may hold. Each one costs two generic instead of one
 * colored, so a single one is a small tax but Karn, Gilded Guardian
 * (`{2/W}{2/U}{2/B}{2/R}{2/G}`) in a W/B deck is an eight-mana 5/5 (2026-10-15 game ca15647d).
 */
internal const val MAX_OFF_COLOR_TWOBRID_PIPS = 1

private val BASIC_TYPE_COLORS = mapOf(
    Subtype.PLAINS to Color.WHITE, Subtype.ISLAND to Color.BLUE, Subtype.SWAMP to Color.BLACK,
    Subtype.MOUNTAIN to Color.RED, Subtype.FOREST to Color.GREEN,
)

/**
 * Whether a non-basic [land] that makes colored mana belongs in a deck playing [deckColors] — the
 * splash colour included. A land earns its slot over a basic only when it is no worse than one:
 *  - it makes at least two deck colors (a dual for the pair, a tri-land, a five-color land), or
 *  - it makes only deck colors and enters untapped (an on-color utility land).
 * The colors a land makes are what it taps for (its basic land types, CR 305.6, and the `Add …`
 * effects of its activated abilities), else what it fetches (Wooded Foothills names Mountain and
 * Forest). So an off-color tapped dual — Stingerquill Annex ({B} or {R}) in a B/G deck — is a
 * tapped Swamp and stays out, whatever its "unless" condition, and a fetch naming one off-color
 * type is a Forest that costs life. A fetch for "a basic land card" (Evolving Wilds) fits any
 * deck. A land that makes no colored mana at all is a colorless utility land: see [utilityLand].
 */
internal fun landFitsColors(land: CardDefinition, deckColors: Set<Color>): Boolean {
    val produced = producedColors(land)
    if (produced.isEmpty()) return fetchesAnyBasic(land)
    val onColor = produced.count { it in deckColors }
    if (onColor >= 2) return true
    return onColor == 1 && produced.size == 1 && !entersTapped(land)
}

/** Creatures with a prepare spell a deck needs before a land that prepares them is worth its slot. */
private const val MIN_PREPARE_CREATURES = 3

/**
 * The one colorless utility land from [poolLands] worth a basic's slot in a deck playing
 * [deckColors] with [spells], or `null`. Such a land costs the deck a colored source, so it makes
 * the cut only with a real payoff: a non-mana ability whose activation costs are on-color and
 * which has something to do in this deck — Hexhaven Dueling Arena ("target creature becomes
 * prepared") needs [MIN_PREPARE_CREATURES] creatures with prepare spells. At most one, and one
 * copy, per deck (2026-10-31 game aa98: a U/R deck ran two Dueling Arenas and no
 * prepare creature in its 17 lands).
 */
internal fun utilityLand(poolLands: List<CardDefinition>, deckColors: Set<Color>, spells: List<CardDefinition>): CardDefinition? {
    return poolLands.firstOrNull { land ->
        isColorlessUtilityLand(land) && land.colorIdentity.all { it in deckColors } &&
            land.script.activatedAbilities.any { !it.isManaAbility && hasPayoff(it.effect, spells) }
    }
}

/** A land that makes no colored mana and fetches none: `{T}: Add {C}` plus whatever else it does. */
internal fun isColorlessUtilityLand(land: CardDefinition): Boolean =
    land.typeLine.isLand && !land.typeLine.isBasicLand && producedColors(land).isEmpty() && !fetchesAnyBasic(land)

private fun hasPayoff(effect: Effect, spells: List<CardDefinition>): Boolean = when (effect) {
    is BecomePreparedEffect -> spells.count { it.typeLine.isCreature && it.isPreparation } >= MIN_PREPARE_CREATURES
    is CompositeEffect -> effect.effects.all { hasPayoff(it, spells) }
    else -> true
}

/** Whether [land] enters tapped with no way to pay around it (a shock land's life payment doesn't count). */
private fun entersTapped(land: CardDefinition): Boolean =
    land.script.replacementEffects.any { it is EntersTapped && it.payLifeCost == null }

/** "Search your library for a basic land card" — a fetch that finds whichever basic the deck plays. */
private fun fetchesAnyBasic(land: CardDefinition): Boolean =
    Regex("""(?i)search your library for a basic land card""").containsMatchIn(land.oracleText)

/** The colors a non-basic [land] can put toward a spell: what it taps for, else what it fetches. */
internal fun producedColors(land: CardDefinition): Set<Color> = tappedColors(land).ifEmpty { fetchedColors(land) }

private fun tappedColors(land: CardDefinition): Set<Color> {
    val produced = mutableSetOf<Color>()
    land.typeLine.subtypes.forEach { BASIC_TYPE_COLORS[it]?.let(produced::add) }
    land.script.activatedAbilities.forEach { collectProducedColors(it.effect, produced) }
    return produced
}

private fun fetchedColors(land: CardDefinition): Set<Color> =
    BASIC_TYPE_COLORS.filterKeys { type ->
        Regex("""\b${type.value}\b""").containsMatchIn(land.oracleText)
    }.values.toSet()

private fun collectProducedColors(effect: Effect, out: MutableSet<Color>) {
    when (effect) {
        is AddManaEffect -> out.add(effect.color)
        is AddColorlessManaEffect -> Unit
        is AddManaOfChoiceEffect -> collectChoiceColors(effect.colorSet, out)
        is AddDynamicManaEffect -> out.addAll(effect.allowedColors)
        is AddOneManaOfEachColorAmongEffect -> out.addAll(Color.entries)
        is CompositeEffect -> effect.effects.forEach { collectProducedColors(it, out) }
        else -> Unit
    }
}

/** State-dependent color sets (chosen color, colors among permanents, …) could be any color. */
private fun collectChoiceColors(colorSet: ManaColorSet, out: MutableSet<Color>) {
    when (colorSet) {
        is ManaColorSet.Specific -> out.addAll(colorSet.colors)
        is ManaColorSet.Union -> colorSet.members.forEach { collectChoiceColors(it, out) }
        is ManaColorSet.ExceptChosenColor -> collectChoiceColors(colorSet.base, out)
        else -> out.addAll(Color.entries)
    }
}
