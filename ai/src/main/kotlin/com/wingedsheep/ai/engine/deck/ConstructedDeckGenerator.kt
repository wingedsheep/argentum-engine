package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimCardOps
import com.wingedsheep.ai.draftsim.DraftsimDeckBuilder
import com.wingedsheep.ai.draftsim.DraftsimDeckShape
import com.wingedsheep.ai.draftsim.DraftsimPoolCard
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.CanadianHighlanderPoints
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * Builds a 60-card constructed deck that is legal in a given [DeckFormat] — or, for the Highlander
 * formats, a 100-card singleton one.
 *
 * This is the constructed counterpart to [com.wingedsheep.ai.engine.SealedDeckGenerator]: where that
 * one opens boosters and autobuilds a 40-card limited deck, this one draws from a *constructed* card
 * pool — every card the format allows — and hands it to the same Draftsim autobuilder, running at
 * [DraftsimDeckShape.CONSTRUCTED] instead of the limited 23+17.
 *
 * It exists because the AI seat used to play a limited deck no matter what the lobby asked for. A
 * Pauper lobby validated the human's deck down to commons and then sat them across from a sealed
 * pool full of rares; the format restriction only ever applied to one side of the table.
 *
 * **Per-card legality is the whole of the pool filter.** [CardDefinition.legalFormats] is
 * Scryfall-sourced and already accounts for bans and set legality, so "is this card allowed in
 * Pauper" needs no rarity check here. A commander in the command zone is *not* modelled here:
 * commander-shape formats are rejected outright and belong to [CommanderDeckGenerator]. The
 * commander-less Highlander formats are, through [DeckShape.HIGHLANDER]: 100 singleton cards, and
 * for Canadian Highlander a pool without any card on the points list — a deck that spends no points
 * can never exceed the cap.
 */
class ConstructedDeckGenerator(
    private val boosterGenerator: BoosterGenerator,
    private val cardRegistry: CardRegistry,
    /**
     * Randomness source for the shortlist draw and, on the fallback path, the colour pick and curve
     * fill. Defaults to [Random.Default] so live lobbies keep drawing fresh decks; pass a seeded
     * [Random] to make a run reproducible — the arena does, so a rerun at the same seed plays the
     * same decks.
     */
    private val random: Random = Random.Default,
) {
    private val cardPool = FormatCardPool(boosterGenerator, cardRegistry)

    /**
     * Builds a format-legal deck from the cards printed in [setCodes].
     *
     * @param setCodes sets to draw from. Empty means "every set" — the full format-legal pool.
     * @param format the constructed format the deck must be legal in.
     * @throws IllegalArgumentException if [format] is commander-shaped (use [CommanderDeckGenerator]
     *         for those), or if the legal pool is too thin to build from.
     */
    fun generate(setCodes: List<String>, format: DeckFormat): Map<String, Int> =
        generateDeck(setCodes, format).deckList

    /**
     * [generate], plus a fifteen-card [Wishboard] drawn from the same legal pool — the cards the
     * deck owns outside the game (CR 100.4a), so a generated deck that rolls Learn or a wish has
     * something to fetch. The wishboard never repeats a card the maindeck already plays.
     */
    fun generateDeck(setCodes: List<String>, format: DeckFormat): GeneratedDeck {
        require(!format.isCommanderShape) {
            "Commander-shape formats need a designated commander and singleton rules; " +
                "ConstructedDeckGenerator only builds the 60-card constructed shape. " +
                "Use CommanderDeckGenerator instead."
        }

        val shape = if (format.isHighlanderShape) DeckShape.HIGHLANDER else DeckShape.CONSTRUCTED
        val pool = cardPool.legalPool(setCodes, format).let { legal ->
            if (format == DeckFormat.CANADIAN_HIGHLANDER) {
                legal.filter { CanadianHighlanderPoints.pointsFor(it.name) == 0 }
            } else {
                legal
            }
        }
        require(pool.isNotEmpty()) {
            "No ${format.displayName}-legal cards available" +
                if (setCodes.isEmpty()) "" else " in ${setCodes.joinToString(", ")}"
        }

        val basics = cardPool.basicLands(setCodes)

        logger.info(
            "Building a {} deck from {} legal cards ({}); nonland cards per colour: {}",
            format.displayName,
            pool.size,
            if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
            pool.filterNot { it.isLand }
                .flatMap { card -> card.colors.map { it.symbol.toString() }.ifEmpty { listOf("C") } }
                .groupingBy { it }.eachCount(),
        )

        val deckList = draftsimBuild(pool, shape)
            // Pin the basics to the set's art, exactly as a human's submitted deck is.
            ?.let { BoosterGenerator.withBasicLandArt(it, basics.associateBy { basic -> basic.name }) }
            ?: run {
                logger.warn(
                    "Draftsim produced no {} build from {} legal cards; falling back to the random builder",
                    format.displayName,
                    pool.size,
                )
                RandomDeckGenerator(
                    cardPool = pool,
                    basicLandVariants = basics,
                    setCodes = setCodes,
                    random = random,
                    deckSize = shape.deckSize,
                    landCount = shape.draftsim.landCount,
                    maxCopies = shape.copiesPerCard,
                ).generate()
            }
        val played = deckList.keys.mapTo(HashSet()) { it.substringBefore('#') }
        requirePlayable(deckList, pool, shape, format)
        // Canadian Highlander plays without a sideboard, so there is nowhere to put a wishboard.
        val sideboard = if (format == DeckFormat.CANADIAN_HIGHLANDER) {
            emptyMap()
        } else {
            Wishboard.build(pool, random, exclude = played)
        }
        return GeneratedDeck(deckList, sideboard = sideboard)
    }

    /**
     * Refuse a deck the pool was too thin to really build: short of the shape's size (the random
     * builder can't always fill its spell slots), too few spells (the shortfall went to basics), or
     * too few creatures to put a body on the board. Throwing is what lets `RandomDeckResolver` fall
     * back to another answer instead of seating a 44-land pile or a 52-card deck.
     */
    private fun requirePlayable(deckList: Map<String, Int>, pool: List<CardDefinition>, shape: DeckShape, format: DeckFormat) {
        val byName = pool.associateBy { it.name }
        var nonland = 0
        var creatures = 0
        for ((key, count) in deckList) {
            val card = byName[key.substringBefore('#')] ?: continue
            if (card.isLand) continue
            nonland += count
            if (card.typeLine.isCreature) creatures += count
        }
        val size = deckList.values.sum()
        require(size >= shape.deckSize) {
            "The ${format.displayName}-legal pool is too thin for a full deck: $size of ${shape.deckSize} cards"
        }
        require(nonland >= shape.minNonland && creatures >= shape.minCreatures) {
            "The ${format.displayName}-legal pool is too thin for a playable deck: " +
                "$nonland nonland cards (need ${shape.minNonland}), $creatures creatures (need ${shape.minCreatures})"
        }
    }

    /** Convenience overload: build from the whole format-legal card base. */
    fun generate(format: DeckFormat): Map<String, Int> = generate(emptyList(), format)

    /**
     * Autobuild [pool] with Draftsim at the constructed shape, or null when it can't fill a deck.
     *
     * Two things have to be bridged to get a limited autobuilder onto a constructed pool:
     *
     *  - **Size.** A sealed pool is ~90 cards; a constructed one is thousands, and the scorer is
     *    quadratic in pool size (every card is scored against the whole pool). So the pool is first
     *    [shortlist]ed down to sealed scale.
     *  - **Copies.** Draftsim picks from *physical* pool cards and caps a name at 4 (2 for legendary
     *    creatures). Constructed's four-of rule is exactly that cap, so handing it four instances of
     *    every shortlisted card gets four-ofs of the good cards for free.
     *
     * Nonbasic lands are left out of the shortlist, so the manabase comes back as pure basics. The
     * builder *can* use pool lands, but it takes them unranked, and a constructed pool's land slice
     * is mostly narrow utility lands — a bad trade against a clean basics manabase.
     */
    private fun draftsimBuild(pool: List<CardDefinition>, shape: DeckShape): Map<String, Int>? {
        val tables = ConstructedRatings.tables()
        val shortlist = shortlist(pool.filterNot { it.isLand }, DraftsimCardOps(tables), shape.shortlistSize)
        if (shortlist.isEmpty()) return null

        val instances = shortlist.flatMapIndexed { index, card ->
            val scorerCard = card.toScorerCard()
            (0 until shape.copiesPerCard).map { copy -> DraftsimPoolCard(scorerCard, "pool-$index-$copy") }
        }
        val nameByInstance = instances.associate { it.instanceId to it.card.name }

        // "draft" mode ranks the two best hypotheses and skips the three-color good-stuff build,
        // which would ask an all-basics manabase to cast three colors.
        val build = runCatching { DraftsimDeckBuilder(tables, shape.draftsim).buildDecks(instances, mode = "draft") }
            .onFailure { logger.warn("Draftsim constructed build threw", it) }
            .getOrNull()
            ?.firstOrNull()
            ?: return null

        val deckList = LinkedHashMap<String, Int>()
        for (instanceId in build.deckInstanceIds) {
            val name = nameByInstance[instanceId] ?: continue
            deckList[name] = (deckList[name] ?: 0) + 1
        }
        for ((color, count) in build.basicsNeeded) {
            val name = COLOR_TO_BASIC[color] ?: continue
            if (count > 0) deckList[name] = (deckList[name] ?: 0) + count
        }

        // A short build is a build the pool couldn't support; the random fallback fills the deck.
        val size = deckList.values.sum()
        if (size < shape.deckSize) {
            logger.warn("Draftsim constructed build came back {} cards short of {}", shape.deckSize - size, shape.deckSize)
            return null
        }
        // Draftsim makes up a spell shortfall in basics, so a full-size build can still be thin:
        // count the spells, not the deck.
        val spells = build.deckInstanceIds.count { it in nameByInstance }
        if (spells < shape.minNonland) {
            logger.warn("Draftsim constructed build found only {} of {} spells", spells, shape.draftsim.nonlandCount)
            return null
        }
        return deckList
    }

    /**
     * Draw [size] cards from [pool], biased hard towards the ones Draftsim rates highly.
     *
     * The weight is the rating *cubed*, so bombs almost always make the cut while the tail still
     * turns over between games — a constructed lobby that seated the identical AI deck every time
     * would be a worse experience than the unrated random build this replaces.
     *
     * Unrated cards (a set Draftsim never covered, or our own content) fall back to the rarity
     * ladder rather than dropping out, so a pool with no ratings at all still shortlists sanely.
     */
    private fun shortlist(pool: List<CardDefinition>, ops: DraftsimCardOps, size: Int): List<CardDefinition> =
        pool.weightedSample(size, random) { card ->
            val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            rating * rating * rating
        }

    /**
     * The two deck shapes this generator builds.
     *
     * @property shortlistSize How many distinct cards the autobuilder actually sees. Sealed scale —
     *   the builder's home turf, and what keeps its quadratic scoring cheap — while still leaving
     *   every two-colour guild enough castable cards to fill the nonland slots. A singleton deck
     *   needs one distinct card per slot, so its shortlist is wider.
     * @property copiesPerCard Instances per shortlisted card: Draftsim's own four-of cap for
     *   constructed, so it can build four-ofs; one for a singleton deck.
     */
    private enum class DeckShape(
        val draftsim: DraftsimDeckShape,
        val shortlistSize: Int,
        val copiesPerCard: Int,
    ) {
        CONSTRUCTED(DraftsimDeckShape.CONSTRUCTED, shortlistSize = 120, copiesPerCard = 4),
        HIGHLANDER(DraftsimDeckShape.HIGHLANDER, shortlistSize = 240, copiesPerCard = 1);

        val deckSize: Int get() = draftsim.nonlandCount + draftsim.landCount

        /** Fewest spells a playable build may hold: four in five of the nonland slots. */
        val minNonland: Int get() = draftsim.nonlandCount * 4 / 5

        /** Fewest creatures a playable build may hold: 8 of 60, 13 of 100. */
        val minCreatures: Int get() = draftsim.creatureFloor * 2 / 5
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ConstructedDeckGenerator::class.java)


        private val COLOR_TO_BASIC =
            mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")
    }
}
