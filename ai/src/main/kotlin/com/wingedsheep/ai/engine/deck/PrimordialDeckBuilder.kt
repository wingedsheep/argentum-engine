package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimDeckBuilder
import com.wingedsheep.ai.draftsim.DraftsimDeckShape
import com.wingedsheep.ai.draftsim.DraftsimPoolCard
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.engine.fitsDeckColors
import com.wingedsheep.ai.engine.spellColors
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.PrimordialFormat
import com.wingedsheep.sdk.core.PrimordialFormat.Slot
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * Builds a Primordial deck ([PrimordialFormat]): 40 cards from one block or standalone set, with
 * exactly two rares/mythics (one of each), exactly six uncommons (two of each at most) and commons
 * as four-ofs at most.
 *
 * A Primordial deck is a limited deck with constructed copies, so it is built the limited way:
 * the Draftsim autobuilder at [DraftsimDeckShape.LIMITED] over the pool, each card offered as many
 * times as its rarity allows. The builder knows no rarity budget, so [enforceBudget] then trades
 * copies until the deck spends exactly its two rare and six uncommon slots — best on-colour cards
 * in, weakest commons out.
 */
internal class PrimordialDeckBuilder(
    private val boosterGenerator: BoosterGenerator,
    private val cardRegistry: CardRegistry,
    private val random: Random,
) {
    private val basicsSource = FormatCardPool(boosterGenerator, cardRegistry)

    /**
     * @param setCodes the lobby's set selection; the pool the first of them belongs to is built
     *   from. Empty means any playable pool the server has enough cards of.
     * @throws IllegalArgumentException when no pool can fill a deck.
     */
    fun generate(setCodes: List<String>): GeneratedDeck {
        val wanted = if (setCodes.isEmpty()) {
            PrimordialFormat.pools.filter { it.playable }
        } else {
            setCodes.mapNotNull(PrimordialFormat::poolOf).distinct()
        }
        val viable = wanted.mapNotNull { pool -> slotsIn(pool).takeIf(::canFill)?.let { pool to it } }
        require(viable.isNotEmpty()) {
            "No Primordial block or set has enough implemented cards to build from" +
                if (setCodes.isEmpty()) "" else " in ${setCodes.joinToString(", ")}"
        }
        val (pool, slots) = viable.random(random)
        val deck = build(slots) ?: throw IllegalArgumentException("Primordial build from ${pool.name} failed")
        val available = pool.setCodes.filter { it in boosterGenerator.availableSets }
        val basics = basicsSource.basicLands(available).associateBy { it.name }
        logger.info("Built a Primordial deck from {}: {}", pool.name, deck)
        return GeneratedDeck(BoosterGenerator.withBasicLandArt(deck, basics))
    }

    /** Every non-basic card [pool] prints that the server has, at its lowest rarity there. */
    private fun slotsIn(pool: PrimordialFormat.Pool): Map<CardDefinition, Slot> {
        val bySlot = LinkedHashMap<String, Pair<CardDefinition, Slot>>()
        fun offer(card: CardDefinition?, slot: Slot) {
            if (card == null || card.typeLine.isBasicLand || card.meldResult || PrimordialFormat.isBanned(card.name)) return
            val held = bySlot[card.name]
            if (held == null || slot.ordinal > held.second.ordinal) bySlot[card.name] = card to slot
        }
        for (code in pool.setCodes) {
            val config = boosterGenerator.availableSets[code] ?: continue
            config.cards.forEach { offer(cardRegistry.getCard(it.name) ?: it, Slot.of(it.metadata.rarity)) }
            config.printings.forEach { offer(cardRegistry.getCard(it.name), Slot.of(it.rarity)) }
        }
        return bySlot.values.toMap()
    }

    private fun canFill(slots: Map<CardDefinition, Slot>): Boolean {
        val nonland = slots.filterKeys { !it.isLand }
        return nonland.count { it.value == Slot.COMMON } >= MIN_COMMONS &&
            slots.count { it.value == Slot.RARE } >= PrimordialFormat.RARE_SLOTS &&
            slots.count { it.value == Slot.UNCOMMON } * Slot.UNCOMMON.maxCopies >= PrimordialFormat.UNCOMMON_SLOTS
    }

    private fun build(slots: Map<CardDefinition, Slot>): Map<String, Int>? {
        val spells = slots.filterKeys { !it.isLand }
        // Sealed-scale shortlist per slot: keeps Draftsim's quadratic scoring cheap.
        val shortlist = Slot.entries.flatMap { slot ->
            spells.filterValues { it == slot }.keys.toList()
                .weightedSample(SHORTLIST.getValue(slot), random) { ConstructedRatings.rate(it).let { r -> r * r * r } }
        }
        val instances = shortlist.flatMapIndexed { index, card ->
            val scorer = card.toScorerCard()
            (0 until slots.getValue(card).maxCopies.coerceAtMost(card.copyCap())).map { DraftsimPoolCard(scorer, "prim-$index-$it") }
        }
        val nameByInstance = instances.associate { it.instanceId to it.card.name }
        val build = runCatching {
            DraftsimDeckBuilder(ConstructedRatings.tables(), DraftsimDeckShape.LIMITED).buildDecks(instances, mode = "draft")
        }.onFailure { logger.warn("Draftsim Primordial build threw", it) }.getOrNull()?.firstOrNull() ?: return null

        val byName = slots.keys.associateBy { it.name }
        val picks = LinkedHashMap<CardDefinition, Int>()
        for (id in build.deckInstanceIds) {
            val card = byName[nameByInstance[id] ?: continue] ?: continue
            picks[card] = (picks[card] ?: 0) + 1
        }
        val colours = deckColours(picks, spells.keys)
        enforceBudget(picks, slots, colours, spellTarget = DraftsimDeckShape.LIMITED.nonlandCount)

        val deck = LinkedHashMap<String, Int>()
        picks.forEach { (card, n) -> if (n > 0) deck[card.name] = n }
        basicsFor(picks, colours, DECK_SIZE - deck.values.sum()).forEach { (name, n) -> if (n > 0) deck[name] = n }
        return deck
    }

    /**
     * Trade copies in [picks] until the deck holds exactly [PrimordialFormat.RARE_SLOTS] rares and
     * [PrimordialFormat.UNCOMMON_SLOTS] uncommons, keeping its spell count: surplus rares and
     * uncommons go weakest first, a shortfall is filled with the best cards castable in [colours]
     * (any card of the slot when none is), and the spell count is brought to [spellTarget] with
     * commons.
     */
    private fun enforceBudget(
        picks: MutableMap<CardDefinition, Int>,
        slots: Map<CardDefinition, Slot>,
        colours: Set<Color>,
        spellTarget: Int,
    ) {
        val spellCount = spellTarget
        fun count(slot: Slot) = picks.entries.filter { slots[it.key] == slot }.sumOf { it.value }
        fun rate(card: CardDefinition) = ConstructedRatings.rate(card)
        fun cap(card: CardDefinition) = slots.getValue(card).maxCopies.coerceAtMost(card.copyCap())
        fun removeWeakest(slot: Slot): Boolean {
            val card = picks.filter { slots[it.key] == slot && it.value > 0 }.keys.minByOrNull(::rate) ?: return false
            picks[card] = picks.getValue(card) - 1
            if (picks.getValue(card) == 0) picks.remove(card)
            return true
        }
        fun addBest(slot: Slot): Boolean {
            val open = slots.filter { (card, s) -> s == slot && (picks[card] ?: 0) < cap(card) }.keys
            val card = open.filter { fitsDeckColors(it, colours) }.maxByOrNull(::rate)
                ?: open.filter { spellColors(it).isEmpty() }.maxByOrNull(::rate)
                ?: open.maxByOrNull(::rate)
                ?: return false
            picks[card] = (picks[card] ?: 0) + 1
            return true
        }
        for ((slot, target) in listOf(Slot.RARE to PrimordialFormat.RARE_SLOTS, Slot.UNCOMMON to PrimordialFormat.UNCOMMON_SLOTS)) {
            while (count(slot) > target && removeWeakest(slot)) Unit
            while (count(slot) < target && addBest(slot)) Unit
        }
        while (picks.values.sum() > spellCount && removeWeakest(Slot.COMMON)) Unit
        while (picks.values.sum() < spellCount && addBest(Slot.COMMON)) Unit
    }

    /**
     * The colours Draftsim's build plays — at most two, the ones its spells lean on most — or, when
     * it came back empty, the two colours whose spells in [pool] rate highest.
     */
    private fun deckColours(picks: Map<CardDefinition, Int>, pool: Collection<CardDefinition>): Set<Color> {
        val weight = HashMap<Color, Double>()
        val source = if (picks.isNotEmpty()) picks.keys else pool
        for (card in source) for (colour in spellColors(card)) {
            weight.merge(colour, ConstructedRatings.rate(card) * (picks[card] ?: 1), Double::plus)
        }
        return weight.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()
            .ifEmpty { setOf(Color.GREEN) }
    }

    /** [landSlots] basics split over [colours] by the coloured pips the deck's spells show. */
    private fun basicsFor(picks: Map<CardDefinition, Int>, colours: Set<Color>, landSlots: Int): Map<String, Int> {
        if (landSlots <= 0) return emptyMap()
        val pips = colours.associateWith { colour ->
            picks.entries.sumOf { (card, n) -> (card.manaCost.colorCount[colour] ?: 0) * n }.coerceAtLeast(1)
        }
        val total = pips.values.sum()
        val split = LinkedHashMap<String, Int>()
        var left = landSlots
        pips.entries.sortedByDescending { it.value }.forEachIndexed { i, (colour, n) ->
            val share = if (i == pips.size - 1) left else (landSlots * n + total / 2) / total
            val basic = COLOR_TO_BASIC.getValue(colour.symbol.toString())
            split[basic] = share.coerceAtMost(left)
            left -= split.getValue(basic)
        }
        return split
    }

    private fun CardDefinition.copyCap(): Int = if (typeLine.isLegendary) 2 else 4

    private companion object {
        private val logger = LoggerFactory.getLogger(PrimordialDeckBuilder::class.java)
        private const val DECK_SIZE = 40
        private const val MIN_COMMONS = 20
        private val SHORTLIST = mapOf(Slot.COMMON to 45, Slot.UNCOMMON to 20, Slot.RARE to 12)
        private val COLOR_TO_BASIC =
            mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")
    }
}
