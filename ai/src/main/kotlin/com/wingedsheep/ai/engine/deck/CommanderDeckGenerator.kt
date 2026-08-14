package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import kotlin.math.roundToInt
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * Builds a legal commander-shaped deck — a designated commander plus a library inside its colour
 * identity — for a seat that has to bring one and hasn't.
 *
 * This is the third constructed builder, alongside [com.wingedsheep.ai.engine.SealedDeckGenerator]
 * (open boosters, autobuild 40) and [ConstructedDeckGenerator] (60-card, four-of). It exists
 * because those two answer "which cards", and a commander deck's hard part is the *structure*: the
 * commander is chosen first and every later choice is downstream of it.
 *
 * ## The rules it implements
 *
 * - **CR 903.3 / 903.3a** — the commander is a legendary creature, or a card whose text says it can
 *   be one. Read through [CommanderEligibility], the same predicate the server validates against.
 * - **CR 903.5a / 903.12d** — the deck is exactly [DeckFormat]'s size *including* the commander, so
 *   the library is one short of it.
 * - **CR 903.5b** — singleton: every non-basic name appears once.
 * - **CR 903.5c** — every card's colour identity fits inside the commander's.
 * - **CR 903.5d** — a card with a basic land type is only legal if every colour it produces is in
 *   the commander's identity, so the manabase only ever uses the commander's own colours.
 *
 * ## What it deliberately doesn't do
 *
 * - **Partners, Backgrounds, companions.** The deck model carries one commander; so does this.
 * - **Colourless commanders.** CR 903.5d bans the five basics from a colourless-identity deck, and
 *   Brawl's escape hatch (CR 903.12e, "any number of basics of one type") doesn't exist in
 *   Commander. Rather than build a five-basic deck the validator would reject, an Eldrazi is simply
 *   never picked — see [commanderCandidates].
 * - **Non-basic manabases.** The library's lands are basics only. A commander pool's non-basic
 *   lands are mostly narrow utility lands that Draftsim doesn't rate, and every one of them is a
 *   fresh CR 903.5d question; a clean basics manabase is a better deck *and* an obviously legal one.
 * - **Synergy.** Cards are picked on raw rating and curve, not on whether they do what the commander
 *   wants. That is what makes this a legal deck rather than a good one.
 */
class CommanderDeckGenerator(
    boosterGenerator: BoosterGenerator,
    cardRegistry: CardRegistry,
    /**
     * Randomness source for the commander draw and the curve fill. Defaults to [Random.Default] so
     * live lobbies keep drawing fresh decks; pass a seeded [Random] to make a run reproducible.
     */
    private val random: Random = Random.Default,
) {
    private val cardPool = FormatCardPool(boosterGenerator, cardRegistry)

    /**
     * Build a deck legal in the commander-shaped [format], drawing from the cards printed in
     * [setCodes] (empty means the whole format-legal card base).
     *
     * @throws IllegalArgumentException if [format] is not commander-shaped.
     * @return the deck, or null when the pool holds no legal commander with a buildable colour
     *         identity behind it — the caller decides what to do instead, because "no Commander deck
     *         from three Portal sets" is a lobby configuration, not a bug.
     */
    fun generate(setCodes: List<String>, format: DeckFormat): GeneratedDeck? {
        require(format.isCommanderShape) {
            "$format is not a commander-shape format; use ConstructedDeckGenerator."
        }
        val pool = cardPool.legalPool(setCodes, format)
        if (pool.isEmpty()) {
            logger.warn(
                "No {}-legal cards in {}",
                format.displayName,
                if (setCodes.isEmpty()) "the card base" else setCodes.joinToString(", "),
            )
            return null
        }
        return build(
            pool = pool,
            basics = cardPool.basicLands(setCodes),
            deckSize = deckSizeFor(format),
            copyLimit = { 1 },
            label = format.displayName,
        )
    }

    /**
     * Build a commander deck out of a *limited* pool — the cards a seat drafted or opened — rather
     * than out of a format's legal card base.
     *
     * Three things differ from [generate], all because the pool is the legality universe here (see
     * `DeckValidator.validateCommanderLimited`): [deckSize] is the lobby's minimum rather than a
     * format constant, copies are capped by how many the seat actually owns, and singleton is the
     * lobby's own toggle rather than CR 903.5b. The commander choice, the colour-identity filter and
     * the basics-only manabase are the same, so they are the same code.
     *
     * The manabase stays on plain basic-land names here, deliberately: a limited lobby validates a
     * submitted deck card-by-card against the pool it dealt, and a `Plains#196` art variant is not a
     * name that pool contains.
     *
     * @param pool every card the seat owns, with duplicates repeated.
     * @param deckSize total cards to build to, counting the commander.
     * @param allowDuplicates the lobby's own singleton toggle — true (its default) lets the deck play
     *        every copy the pool holds; false applies paper Commander's one-of rule to it.
     */
    fun generateFromPool(
        pool: List<CardDefinition>,
        deckSize: Int,
        allowDuplicates: Boolean = true,
    ): GeneratedDeck? {
        val copies = pool.groupingBy { it.name }.eachCount()
        return build(
            pool = pool.distinctBy { it.name },
            basics = emptyList(),
            deckSize = deckSize,
            copyLimit = if (allowDuplicates) ({ card -> copies[card.name] ?: 1 }) else ({ 1 }),
            label = "Commander limited",
        )
    }

    /**
     * Pick a commander, then fill a library inside its colour identity.
     *
     * @param pool distinct candidate cards; already scoped to whatever legality the caller enforces.
     * @param basics basic-land printings whose art the manabase is pinned to.
     * @param deckSize total cards including the commander.
     * @param copyLimit how many copies of a card may be played — 1 for singleton, the pool count
     *        for limited.
     */
    private fun build(
        pool: List<CardDefinition>,
        basics: List<CardDefinition>,
        deckSize: Int,
        copyLimit: (CardDefinition) -> Int,
        label: String,
    ): GeneratedDeck? {
        val commander = pickCommander(pool) ?: run {
            logger.warn("No legal commander in a pool of {} cards ({})", pool.size, label)
            return null
        }
        val identity = commander.colorIdentity

        val body = pool.filter { card ->
            card.name != commander.name &&
                !card.isLand &&
                card.colorIdentity.all { it in identity }
        }

        val librarySize = deckSize - 1
        val landCount = (librarySize * LAND_RATIO).roundToInt()
        val spells = pickSpells(body, librarySize - landCount, copyLimit)

        // Whatever the pool couldn't fill becomes extra basics. They are the one card type with no
        // copy cap (CR 903.5b), so this always reaches the exact size the format demands — a deck
        // one card short is a deck the validator rejects and the seat can't play.
        val basicsNeeded = librarySize - spells.size
        val manabase = basicLands(spells + commander, identity, basicsNeeded)

        val deckList = LinkedHashMap<String, Int>()
        for (card in spells) deckList.merge(card.name, 1, Int::plus)
        for ((name, count) in manabase) deckList.merge(name, count, Int::plus)

        val pinned = BoosterGenerator.withBasicLandArt(deckList, basics.associateBy { it.name })
        logger.info(
            "Built a {} deck for {} ({}): {} spells, {} lands",
            label,
            commander.name,
            identity.joinToString("/") { it.name.first().toString() }.ifEmpty { "C" },
            spells.size,
            basicsNeeded,
        )
        return GeneratedDeck(deckList = pinned, commander = commander.name)
    }

    /**
     * Draw one commander from [pool], biased towards cards Draftsim rates highly and towards narrow
     * colour identities.
     *
     * The identity bias is the load-bearing half. Identity is a *deck-construction* constraint here,
     * not a bonus: a five-colour commander opens the whole pool but hands the builder a manabase of
     * five basics split five ways, which casts nothing. One and two colours are the shapes an
     * all-basics manabase actually supports, three is playable, and four or five is a last resort
     * for a pool with nothing narrower in it.
     */
    private fun pickCommander(pool: List<CardDefinition>): CardDefinition? {
        val ops = ConstructedRatings.ops()
        val candidates = commanderCandidates(pool)
        if (candidates.isEmpty()) return null
        return candidates.weightedSample(1, random) { card ->
            val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            rating * (IDENTITY_WEIGHT[card.colorIdentity.size] ?: LAST_RESORT_IDENTITY_WEIGHT)
        }.firstOrNull()
    }

    /**
     * The cards in [pool] that could lead a deck.
     *
     * Colourless-identity commanders are excluded rather than supported: CR 903.5d allows a basic
     * land in the deck only if every colour it produces is in the commander's identity, so an
     * Eldrazi deck's manabase has to be Wastes or colourless utility lands — neither of which this
     * builder's basics-only manabase can produce. Building one anyway would produce a deck the deck
     * validator rejects for colour identity, which is worse than declining the commander.
     */
    private fun commanderCandidates(pool: List<CardDefinition>): List<CardDefinition> =
        pool.filter { CommanderEligibility.isLegalCommander(it) && it.colorIdentity.isNotEmpty() }

    /**
     * Fill [slots] with the best cards [body] offers at each point on the curve.
     *
     * Bucket by mana value and draw each bucket's share separately, rather than taking the top
     * [slots] cards overall: rating alone would hand back a pile of six-drops, because Draftsim
     * rates a card's power and not its cost. Within a bucket the draw is rating-weighted, so the
     * bombs come out and the filler still varies between games.
     *
     * A bucket the pool can't fill spills its deficit forward into the next one, and anything left
     * at the end is taken from whatever remains at any cost — a slightly wrong curve beats a short
     * deck. What the pool genuinely cannot supply comes back as fewer cards than [slots]; the caller
     * turns the difference into basics.
     */
    private fun pickSpells(
        body: List<CardDefinition>,
        slots: Int,
        copyLimit: (CardDefinition) -> Int,
    ): List<CardDefinition> {
        if (slots <= 0 || body.isEmpty()) return emptyList()
        val ops = ConstructedRatings.ops()
        val weight: (CardDefinition) -> Double = { card ->
            val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            rating * rating * rating
        }
        val byBucket = body.groupBy { minOf(it.cmc, TOP_CURVE_BUCKET) }

        val picked = mutableListOf<CardDefinition>()
        val takenNames = mutableSetOf<String>()
        var carriedDeficit = 0
        for ((bucket, share) in CURVE) {
            val want = (slots * share).roundToInt() + carriedDeficit
            if (want <= 0) continue
            val available = byBucket[bucket].orEmpty().filterNot { it.name in takenNames }
            val drawn = available.weightedSample(want, random, weight)
            picked += drawn
            drawn.forEach { takenNames += it.name }
            carriedDeficit = want - drawn.size
        }

        // Curve targets are fractions and the pool is lumpy, so the buckets rarely land exactly on
        // `slots`. Trim the overshoot from the top of the curve (the most expendable cards) and top
        // up any shortfall from whatever the pool still has.
        if (picked.size > slots) return picked.sortedBy { it.cmc }.take(slots)
        if (picked.size < slots) {
            val remaining = body.filterNot { it.name in takenNames }
            picked += remaining.weightedSample(slots - picked.size, random, weight)
        }
        return duplicateToFill(picked, body, slots, copyLimit, weight)
    }

    /**
     * Top a short selection up with extra copies of cards already in it, where the copy cap allows.
     *
     * A no-op under singleton, where [copyLimit] is always 1. It earns its keep on the limited path:
     * a drafted pool holds several copies of the commons a seat wheeled, and playing the second copy
     * of a good card beats playing another Island.
     */
    private fun duplicateToFill(
        picked: List<CardDefinition>,
        body: List<CardDefinition>,
        slots: Int,
        copyLimit: (CardDefinition) -> Int,
        weight: (CardDefinition) -> Double,
    ): List<CardDefinition> {
        if (picked.size >= slots) return picked
        val result = picked.toMutableList()
        val counts = result.groupingBy { it.name }.eachCount().toMutableMap()
        val repeatable = body
            .filter { (counts[it.name] ?: 0) < copyLimit(it) && (counts[it.name] ?: 0) > 0 }
            .sortedByDescending { weight(it) }
        if (repeatable.isEmpty()) return result
        var index = 0
        // Round-robin over the repeatable cards so the extra copies spread over the good ones
        // instead of stacking onto the single best.
        while (result.size < slots) {
            val card = repeatable[index % repeatable.size]
            index++
            val held = counts[card.name] ?: 0
            if (held >= copyLimit(card)) {
                if (repeatable.none { (counts[it.name] ?: 0) < copyLimit(it) }) break
                continue
            }
            result += card
            counts[card.name] = held + 1
        }
        return result
    }

    /**
     * Split [count] basic lands across the commander's [identity], weighted by the coloured pips the
     * deck actually asks for.
     *
     * Only the commander's own colours appear, which is CR 903.5d: a basic land is legal in the deck
     * only if every colour it can produce is in the commander's identity. Every colour in the
     * identity gets at least one land even when nothing in the library happens to need it — the
     * commander itself has to be castable.
     */
    private fun basicLands(cards: List<CardDefinition>, identity: Set<Color>, count: Int): Map<String, Int> {
        if (count <= 0 || identity.isEmpty()) return emptyMap()
        val ordered = identity.sortedBy { it.ordinal }
        val pips = ordered.associateWith { color ->
            cards.sumOf { it.manaCost.colorCount[color] ?: 0 }
        }
        val totalPips = pips.values.sum()

        val lands = LinkedHashMap<String, Int>()
        var assigned = 0
        for ((index, color) in ordered.withIndex()) {
            val name = COLOR_TO_BASIC.getValue(color)
            val share = if (index == ordered.lastIndex) {
                // The last colour absorbs the rounding so the counts sum to exactly `count`.
                count - assigned
            } else if (totalPips == 0) {
                count / ordered.size
            } else {
                ((count * (pips.getValue(color).toDouble() / totalPips)).toInt()).coerceAtLeast(1)
            }
            // Clamped to what's left so a deck with fewer lands than colours still sums to `count`
            // rather than overshooting on the "every colour gets one" floor.
            lands[name] = share.coerceIn(0, count - assigned)
            assigned += lands.getValue(name)
        }
        return lands.filterValues { it > 0 }
    }

    /** Total cards the format wants, counting the commander (CR 903.5a, CR 903.12d). */
    private fun deckSizeFor(format: DeckFormat): Int = when (format) {
        DeckFormat.STANDARD_BRAWL -> STANDARD_BRAWL_DECK_SIZE
        else -> SINGLETON_HUNDRED_DECK_SIZE
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CommanderDeckGenerator::class.java)

        /**
         * Deck sizes, mirroring `DeckValidator`'s profiles: 100 for Commander and Arena-style Brawl
         * (CR 903.5a), 60 for Standard Brawl. Paper Brawl's own 60 (CR 903.12d) isn't what our
         * `BRAWL` format means — it maps to Scryfall's `brawl` key, which is the 100-card Arena
         * variant.
         */
        private const val SINGLETON_HUNDRED_DECK_SIZE = 100
        private const val STANDARD_BRAWL_DECK_SIZE = 60

        /**
         * Share of the library that is land. 38% is the paper-Commander consensus (37–38 of 99) and
         * scales sanely down to the 60-card shapes (22 of 59). It runs a touch high for a 60-card
         * deck by constructed standards, which is the right error for a builder that puts no ramp
         * or card selection in the deck on purpose.
         */
        private const val LAND_RATIO = 0.38

        /** Mana values 7+ are one bucket; nothing above it curves differently enough to matter. */
        private const val TOP_CURVE_BUCKET = 7

        /**
         * Target curve as fractions of the non-land slots. Heavier at the top than a 60-card deck's
         * because a singleton deck plays more lands and expects to reach six mana.
         */
        private val CURVE = linkedMapOf(
            1 to 0.10,
            2 to 0.20,
            3 to 0.20,
            4 to 0.17,
            5 to 0.13,
            6 to 0.10,
            TOP_CURVE_BUCKET to 0.10,
        )

        /**
         * How strongly a candidate commander's colour count counts against it. See [pickCommander]:
         * an all-basics manabase supports one or two colours comfortably and three at a stretch.
         */
        private val IDENTITY_WEIGHT = mapOf(1 to 1.0, 2 to 0.9, 3 to 0.35)
        private const val LAST_RESORT_IDENTITY_WEIGHT = 0.05

        private val COLOR_TO_BASIC = mapOf(
            Color.WHITE to "Plains",
            Color.BLUE to "Island",
            Color.BLACK to "Swamp",
            Color.RED to "Mountain",
            Color.GREEN to "Forest",
        )
    }
}
