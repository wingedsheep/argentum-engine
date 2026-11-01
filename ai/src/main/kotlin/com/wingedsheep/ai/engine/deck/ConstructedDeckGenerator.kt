package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimDeckBuilder
import com.wingedsheep.ai.draftsim.DraftsimDeckShape
import com.wingedsheep.ai.draftsim.DraftsimPoolCard
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.engine.DeckDependencies
import com.wingedsheep.ai.engine.PipDemand
import com.wingedsheep.ai.engine.fitsDeckColors
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.landFitsColors
import com.wingedsheep.ai.engine.producedColors
import com.wingedsheep.ai.engine.spellColors
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.CanadianHighlanderPoints
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.EntersTapped
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * Builds a 60-card constructed deck that is legal in a given [DeckFormat] — or, for the Highlander
 * formats, a 100-card singleton one.
 *
 * This is the constructed counterpart to [com.wingedsheep.ai.engine.SealedDeckGenerator]: where that
 * one opens boosters and autobuilds a 40-card limited deck, this one draws from a *constructed* card
 * pool — every card the format allows. A 60-card deck is built the constructed way
 * ([constructedBuild]: playsets of the colours' best cards on a low curve, interaction and creature
 * quotas, a build-around when the top cards share one); a 100-card Highlander deck still goes
 * through the Draftsim autobuilder at [DraftsimDeckShape.HIGHLANDER], where singleton construction
 * makes a limited-style pick order the right shape.
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
        }.filterNot { beyondCurve(it) }
            // The Highlander build is Draftsim's, which has no dead-card pass ([deadIn]); its
            // creature floor makes every one a creature deck, so a wipe of our own side is out
            // up front — Famine in a 36-creature list.
            .filterNot { shape == DeckShape.HIGHLANDER && CardIntentAnalyzer.sweepsOwnSide(it) }
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

        val build = when (shape) {
            DeckShape.CONSTRUCTED -> constructedBuild(pool)
            DeckShape.HIGHLANDER -> draftsimBuild(pool, shape)
        }
        val deckList = build
            // Pin the basics to the set's art, exactly as a human's submitted deck is.
            ?.let { BoosterGenerator.withBasicLandArt(it, basics.associateBy { basic -> basic.name }) }
            ?: run {
                logger.warn(
                    "No {} build from {} legal cards; falling back to the random builder",
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
     * Refuse a deck the pool was too thin to really build: too few spells (the shortfall went to
     * basics) or too few creatures to put a body on the board. Throwing is what lets
     * `RandomDeckResolver` fall back to another answer instead of seating a 44-land pile.
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
        require(nonland >= shape.minNonland && creatures >= shape.minCreatures) {
            "The ${format.displayName}-legal pool is too thin for a playable deck: " +
                "$nonland nonland cards (need ${shape.minNonland}), $creatures creatures (need ${shape.minCreatures})"
        }
    }

    /** Convenience overload: build from the whole format-legal card base. */
    fun generate(format: DeckFormat): Map<String, Int> = generate(emptyList(), format)

    /**
     * Build a 60-card deck the way a constructed player does, or null when the pool can't fill one.
     *
     * The limited autobuilder this replaces (still behind the Highlander shape) drew a 120-card
     * random shortlist and ran Draftsim's 23+17 curve scaled up, which seated "generic piles": a few
     * random four-ofs of whatever the draw turned up, three six-drops, little interaction, and no
     * plan (2026-10-31 review: 0/4 against tuned Standard lists). A constructed deck is the opposite
     * shape — the *best* cards the colours have, in playsets, on a low curve. So:
     *
     *  1. **Quality.** Every legal spell is rated ([ConstructedRatings.rate]: Draftsim's merged
     *     ratings, else what the card's effects read as), discounted for mana value since a
     *     constructed game is decided earlier than a limited one, and jittered a little per build so
     *     the margins turn over.
     *  2. **Colours.** The two-colour pair whose dozen best castable cards rate highest, drawn
     *     weighted among the top three pairs so the AI doesn't always play the same guild.
     *  3. **Plan.** Among the pair's best cards, the one whose [CommanderSynergy] reading (tribe,
     *     rewarded keyword, theme) the most other top cards answer is the deck's build-around; cards
     *     that answer it are weighted up and it goes in first.
     *  4. **Slots.** Interaction first ([CommanderDeckRoles] `REMOVAL`, up to [INTERACTION_COPIES]),
     *     then creatures (up to [CREATURE_COPIES]), then the best of the rest — each new card as a
     *     playset (two for a legend), inside a per-mana-value curve budget and with noncreature
     *     spells held to [MAX_NONCREATURE]. Later passes relax the curve, then the playset rule and
     *     the spell cap, so a thin pool still fills. The budget is [CURVE] blended with where the
     *     colours' best cards actually sit ([curveShares]), so decks stop sharing one template.
     *  5. **Dead cards out.** A card that does nothing in the finished deck ([deadIn]) — a spell
     *     whose effect hangs on cards the deck lacks, a payoff nothing feeds, a sweeper in a creature
     *     deck — is struck and its slots refilled, for a few rounds.
     *
     * The land count follows the curve ([landCountFor]): an aggro deck topping out at three runs
     * fewer lands than a midrange one. Up to [MAX_DUAL_COPIES] of them are the pool's lands that make
     * both colours ([pickDualLands]); the rest are basics split by the deck's coloured pips.
     */
    private fun constructedBuild(pool: List<CardDefinition>): Map<String, Int>? {
        val shape = DeckShape.CONSTRUCTED
        val spells = pool.filterNot { it.isLand }.distinctBy { it.name }
        if (spells.isEmpty()) return null

        // One jitter per card per build: the best cards still lead, the margins turn over.
        val quality = spells.associate { card ->
            card.name to ConstructedRatings.rate(card) * curveDiscount(card) * random.nextDouble(1 - JITTER, 1 + JITTER)
        }
        val colours = pickColours(spells, quality)
        val castable = spells.filter { castableIn(it, colours) }.sortedByDescending { quality.getValue(it.name) }
        // The best cards overall, plus the colours' best interaction whether or not it made that
        // cut: over a whole card base the top 150 can be all creatures, which left the interaction
        // pass nothing to take and seated decks with no removal at all.
        val removal = castable.asSequence().filter { DeckRole.REMOVAL in CommanderDeckRoles.rolesOf(it) }.take(REMOVAL_CANDIDATES)
        val candidates = (castable.take(CANDIDATES) + removal).distinctBy { it.name }
        val roles = candidates.associate { it.name to CommanderDeckRoles.rolesOf(it) }
        val anchor = buildAround(candidates)
        val score = candidates.associate { card ->
            val synergy = anchor?.second?.score(card)?.coerceAtMost(MAX_SYNERGY) ?: 0.0
            val interaction = if (DeckRole.REMOVAL in roles.getValue(card.name)) INTERACTION_BONUS else 1.0
            card.name to quality.getValue(card.name) * (1 + SYNERGY_WEIGHT * synergy) * interaction
        }
        val ranked = candidates.sortedByDescending { score.getValue(it.name) }
        val shares = curveShares(candidates, quality)

        // Build, strike what's dead and resize to the curve's land count, then build again. Each
        // round only adds exclusions, so it settles; the last round's deck stands either way.
        var nonland = shape.draftsim.nonlandCount
        val excluded = HashSet<String>()
        var picks: Map<CardDefinition, Int> = emptyMap()
        var lands = shape.deckSize - nonland
        for (round in 0 until MAX_ROUNDS) {
            picks = fillPicks(ranked.filterNot { it.name in excluded }, roles, anchor?.first?.takeIf { it.name !in excluded }, nonland, shares)
            val deck = picks.flatMap { (card, n) -> List(n) { card } }
            val dead = picks.keys.filter { deadIn(it, deck) }.map { it.name }
            lands = landCountFor(deck)
            if (dead.isEmpty() && shape.deckSize - lands == nonland) break
            if (dead.isNotEmpty()) logger.info("Constructed build strikes dead cards: {}", dead)
            excluded += dead
            nonland = shape.deckSize - lands
        }

        val spellCount = picks.values.sum()
        if (spellCount < shape.minNonland) {
            logger.warn("Constructed build found only {} of {} spells", spellCount, nonland)
            return null
        }
        val deckCards = picks.flatMap { (card, n) -> List(n) { card } }
        val used = colours.filter { colour -> deckCards.any { colour in spellColors(it) } }.toSet().ifEmpty { colours }
        val landSlots = shape.deckSize - spellCount
        val duals = pickDualLands(pool, used, landSlots)
        // Each colour's floor is what its hungriest card needs at this land count ([PipDemand]'s
        // limited table, scaled): a one-pip card in a 24-land deck wants about nine sources.
        val floors = used.associateWith { colour ->
            deckCards.maxOfOrNull { PipDemand.sourcesNeeded((it.manaCost.colorCount[colour] ?: 0).toDouble(), landSlots) } ?: 0
        }
        val basics = BasicLandSplit.split(
            deckCards, used, landSlots - duals.values.sum(), MIN_COLOUR_SHARE, floors,
        ) { colour -> duals.entries.filter { colour in producedColors(it.key) }.sumOf { it.value } }

        logger.info(
            "Constructed build: {} in {}, {} spells ({} creatures, {} interaction), {} lands ({} non-basic)",
            anchor?.first?.name?.let { "around $it" } ?: "no build-around",
            colours.joinToString("") { it.symbol.toString() },
            spellCount,
            picks.entries.filter { it.key.typeLine.isCreature }.sumOf { it.value },
            picks.entries.filter { DeckRole.REMOVAL in roles.getValue(it.key.name) }.sumOf { it.value },
            landSlots,
            duals.values.sum(),
        )
        val deckList = LinkedHashMap<String, Int>()
        picks.forEach { (card, n) -> deckList[card.name] = n }
        duals.forEach { (card, n) -> deckList[card.name] = n }
        deckList.putAll(basics)
        return deckList
    }

    /**
     * The slot-filling passes of [constructedBuild] for one round: [nonland] spells out of [ranked],
     * build-around first, inside a curve budget of [shares] × [nonland] per mana value.
     */
    private fun fillPicks(
        ranked: List<CardDefinition>,
        roles: Map<String, Set<DeckRole>>,
        anchor: CardDefinition?,
        nonland: Int,
        shares: DoubleArray,
    ): Map<CardDefinition, Int> {
        val budget = IntArray(shares.size) { (shares[it] * nonland).roundToInt() }
        val picks = LinkedHashMap<CardDefinition, Int>()
        val curve = IntArray(budget.size)
        fun filled() = picks.values.sum()

        /**
         * Walk [from] best-first, adding copies until [want] copies matching [counts] are in the
         * deck. [slack] widens every curve bucket (null: no curve); a card new to the deck comes in
         * with at least [minCopies] copies or not at all, while there's room for that many. While
         * [capSpells] holds, noncreature spells stop at [MAX_NONCREATURE] copies, so a pool deep in
         * well-rated removal still leaves the deck its creatures.
         */
        fun fill(
            from: List<CardDefinition>,
            want: Int,
            slack: Int?,
            minCopies: Int,
            capSpells: Boolean = true,
            counts: (CardDefinition) -> Boolean = { true },
        ) {
            var got = picks.entries.filter { counts(it.key) }.sumOf { it.value }
            for (card in from) {
                if (got >= want || filled() >= nonland) return
                val held = picks[card] ?: 0
                val bucket = curveBucket(card)
                val bucketRoom = if (slack == null) nonland else budget[bucket] + slack - curve[bucket]
                val spellRoom = if (capSpells && !card.typeLine.isCreature) {
                    MAX_NONCREATURE - picks.entries.filterNot { it.key.typeLine.isCreature }.sumOf { it.value }
                } else {
                    nonland
                }
                val n = minOf(copyCap(card) - held, bucketRoom, spellRoom, nonland - filled())
                if (n <= 0) continue
                if (held == 0 && n < minCopies && nonland - filled() >= minCopies) continue
                picks[card] = held + n
                curve[bucket] += n
                if (counts(card)) got += n
            }
        }

        anchor?.let { fill(listOf(it), want = copyCap(it), slack = null, minCopies = 1) }
        fill(ranked.filter { DeckRole.REMOVAL in roles.getValue(it.name) }, INTERACTION_COPIES, slack = 0, minCopies = PLAYSET_MIN) {
            DeckRole.REMOVAL in roles.getValue(it.name)
        }
        fill(ranked.filter { it.typeLine.isCreature }, CREATURE_COPIES, slack = 0, minCopies = PLAYSET_MIN) { it.typeLine.isCreature }
        fill(ranked, nonland, slack = 0, minCopies = PLAYSET_MIN)
        fill(ranked, nonland, slack = CURVE_SLACK, minCopies = 2)
        fill(ranked, nonland, slack = null, minCopies = 1, capSpells = false)
        return picks
    }

    /**
     * Each mana value's share of the spell slots: [CURVE] blended with where the colours' best
     * cards sit — the summed quality of the [CURVE_MASS_CARDS] best [candidates] at each mana value
     * (quality already discounts the top end). A pair deep in two-drops builds lower than one whose
     * best cards are four-drops, so the decks stop coming out on one identical curve.
     */
    private fun curveShares(candidates: List<CardDefinition>, quality: Map<String, Double>): DoubleArray {
        val templateTotal = CURVE.sum().toDouble()
        val mass = DoubleArray(CURVE.size)
        candidates.groupBy { curveBucket(it) }.forEach { (bucket, cards) ->
            mass[bucket] = cards.map { quality.getValue(it.name) }.sortedDescending().take(CURVE_MASS_CARDS).sum()
        }
        val massTotal = mass.sum()
        return DoubleArray(CURVE.size) { bucket ->
            val template = CURVE[bucket] / templateTotal
            if (massTotal <= 0.0) template else (1 - CURVE_POOL_WEIGHT) * template + CURVE_POOL_WEIGHT * mass[bucket] / massTotal
        }
    }

    /**
     * Whether [card] does nothing in [deck] (the deck's spells, one entry per copy):
     *  - a spell whose whole effect hangs on cards the deck lacks ([DeckDependencies.inert]);
     *  - a payoff for a kind of card or a creature type the rest of the deck barely holds — at most
     *    a third of what it wants ([DeckSynergy.unfed]): four Auratogs and no enchantment to feed
     *    them, a lord with no tribe. A payoff that also rewards an axis (Smuggler's Copter's loot, a
     *    spell trigger on a 5/5) is left to its rating: there the synergy is a rider on a card that
     *    plays fine without it;
     *  - a sweeper that also hits its own side ([CardIntentAnalyzer.sweepsOwnSide]) in a deck of
     *    [SWEEPER_CREATURE_LIMIT] or more other creatures, which it would wipe along with the
     *    opponent's: four Wraths of God in a twenty-creature deck, Unstable Glyphbridge's
     *    enters-the-battlefield wipe, Bane of the Living's -X/-X beside a Goblin swarm. A one-sided
     *    wipe (Plague Wind) and a symmetric activation the deck can decline (Pestilence) stay in.
     */
    private fun deadIn(card: CardDefinition, deck: List<CardDefinition>): Boolean {
        if (DeckDependencies.inert(card, deck)) return true
        val profile = SynergyAxes.of(card)
        if (profile.isPayoff && profile.rewards.isEmpty() &&
            DeckSynergy.unfed(profile, DeckSynergy.counts(deck.map { SynergyAxes.of(it) })) > DEAD_UNFED
        ) {
            return true
        }
        return CardIntentAnalyzer.sweepsOwnSide(card) &&
            deck.count { it !== card && it.typeLine.isCreature } >= SWEEPER_CREATURE_LIMIT
    }

    /**
     * Lands for a 60-card deck whose spells are [deck], from their average mana value: about 22 for
     * an aggro deck averaging two, 24 for a midrange deck near 2.7, held to [MIN_LANDS]..[MAX_LANDS].
     */
    private fun landCountFor(deck: List<CardDefinition>): Int {
        if (deck.isEmpty()) return DeckShape.CONSTRUCTED.draftsim.landCount
        val average = deck.map { manaValue(it) }.average()
        return (LAND_BASE + LAND_PER_MANA_VALUE * average).roundToInt().coerceIn(MIN_LANDS, MAX_LANDS)
    }

    /**
     * The pool's lands that make two of the deck's [colours] — a dual, a tri-land, a fetch naming
     * both basic types — as playsets (one copy of a legendary land), untapped before tapped, up to
     * [MAX_DUAL_COPIES] and a third of the [landSlots]. A one-colour deck gets none: a basic does
     * the job without the drawback.
     */
    private fun pickDualLands(pool: List<CardDefinition>, colours: Set<Color>, landSlots: Int): Map<CardDefinition, Int> {
        if (colours.size < 2) return emptyMap()
        val cap = minOf(MAX_DUAL_COPIES, landSlots / 3)
        val ranked = pool.asSequence()
            .filter { it.isLand && !it.typeLine.isBasicLand }
            .distinctBy { it.name }
            .filter { land ->
                val produced = producedColors(land)
                // A real dual: makes the deck's colours and at most one other. A land that taps for
                // any colour is a filter, a pain land or a conditional utility land far more often
                // than a fixer (Villainous Hideout, Great Hall of the Biblioplex, Mirrex).
                produced.size <= MAX_DUAL_COLOURS && produced.count { it in colours } >= 2 && landFitsColors(land, colours)
            }
            .toList()
            .shuffled(random)
            .sortedBy { land -> land.script.replacementEffects.any { it is EntersTapped && it.payLifeCost == null } }
        val picks = LinkedHashMap<CardDefinition, Int>()
        for (land in ranked) {
            val room = cap - picks.values.sum()
            if (room <= 0) break
            picks[land] = minOf(if (land.typeLine.isLegendary) 1 else 4, room)
        }
        return picks
    }

    /**
     * The two colours to build in: the pair whose [CORE_SIZE] best castable cards rate highest,
     * drawn among the [COLOUR_CHOICES] best pairs with a weight steep enough that a clearly better
     * pair nearly always wins. A one-colour pool lands on a pair holding its colour; the basics
     * then follow the cards actually played.
     */
    private fun pickColours(spells: List<CardDefinition>, quality: Map<String, Double>): Set<Color> {
        val pairs = Color.entries.flatMapIndexed { i, a -> Color.entries.drop(i + 1).map { b -> setOf(a, b) } }
        val value = pairs.associateWith { pair ->
            spells.filter { castableIn(it, pair) }
                .map { quality.getValue(it.name) }
                .sortedDescending()
                .take(CORE_SIZE)
                .sum()
        }
        val best = value.values.max()
        if (best <= 0.0) return pairs.first()
        return value.entries.sortedByDescending { it.value }
            .take(COLOUR_CHOICES)
            .weightedSample(1, random) { (it.value / best).pow(COLOUR_SHARPNESS) }
            .first().key
    }

    /**
     * The deck's build-around among its best few cards, with the reading that scores the rest — or
     * null when none of them asks for something [MIN_SUPPORT] of the other top cards supply. A
     * card that only shares a creature type with others isn't a plan; one whose text names a tribe,
     * rewards a keyword or pays off a theme is.
     */
    private fun buildAround(candidates: List<CardDefinition>): Pair<CardDefinition, CommanderSynergy>? {
        val core = candidates.take(ANCHOR_CORE)
        return candidates.take(ANCHOR_CHOICES)
            .mapNotNull { card ->
                val synergy = CommanderSynergy(card).takeIf { it.asksForSomething } ?: return@mapNotNull null
                val support = core.count { it.name != card.name && synergy.score(it) >= MIN_SUPPORT_SCORE }
                Triple(card, synergy, support)
            }
            .filter { it.third >= MIN_SUPPORT }
            .maxByOrNull { it.third }
            ?.let { (card, synergy, _) -> card to synergy }
    }

    /**
     * Autobuild [pool] with Draftsim at [shape] (the Highlander shape; 60-card decks go through
     * [constructedBuild]), or null when it can't fill a deck.
     *
     * Two things have to be bridged to get a limited autobuilder onto a constructed pool:
     *
     *  - **Size.** A sealed pool is ~90 cards; a constructed one is thousands, and the scorer is
     *    quadratic in pool size (every card is scored against the whole pool). So the pool is first
     *    [shortlist]ed down to sealed scale.
     *  - **Copies.** Draftsim picks from *physical* pool cards, so [DeckShape.copiesPerCard]
     *    instances of each shortlisted card are handed over — one for a singleton deck.
     *
     * Nonbasic lands are left out of the shortlist, so the manabase comes back as pure basics. The
     * builder *can* use pool lands, but it takes them unranked, and a constructed pool's land slice
     * is mostly narrow utility lands — a bad trade against a clean basics manabase.
     */
    private fun draftsimBuild(pool: List<CardDefinition>, shape: DeckShape): Map<String, Int>? {
        val tables = ConstructedRatings.tables()
        val shortlist = shortlist(pool.filterNot { it.isLand }, shape.shortlistSize)
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
     * Unrated cards (a set Draftsim never covered, or our own content) are rated off their effects
     * ([ConstructedRatings.rate]) rather than dropping out, so a pool with no ratings at all still
     * shortlists sanely.
     */
    private fun shortlist(pool: List<CardDefinition>, size: Int): List<CardDefinition> =
        pool.weightedSample(size, random) { card ->
            val rating = ConstructedRatings.rate(card)
            rating * rating * rating
        }

    /**
     * The two deck shapes this generator builds.
     *
     * @property shortlistSize How many distinct cards the Draftsim autobuilder sees (Highlander only;
 *   the 60-card shape is built by [constructedBuild] from the whole pool). Sealed scale —
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

        /**
         * Copies per mana value in a 60-card deck's 36 spells, MV 1 (and 0) to 6+: the low, wide
         * curve of a constructed midrange list rather than limited's fat middle and top end.
         */
        private val CURVE = intArrayOf(0, 6, 12, 9, 6, 2, 1)

        /** How far the second fill pass may overrun a curve bucket before the curve is dropped. */
        private const val CURVE_SLACK = 2

        /** How far the colours' own quality profile moves the curve off [CURVE]. */
        private const val CURVE_POOL_WEIGHT = 0.4

        /** Best cards per mana value whose quality makes up that mana value's weight. */
        private const val CURVE_MASS_CARDS = 6

        /** Build-and-strike rounds before the deck stands as it is. */
        private const val MAX_ROUNDS = 5

        /** A payoff left more unfed than this (≤ one feeder of the three it wants) is dead. */
        private const val DEAD_UNFED = 0.66

        /** Creatures at which a sweeper hurts its own deck more than the opponent's. */
        private const val SWEEPER_CREATURE_LIMIT = 14

        /** Highest mana value [beyondCurve] lets into a build. */
        const val MAX_MANA_VALUE = 7
        private val COST_REDUCING_KEYWORDS = setOf(Keyword.DELVE, Keyword.CONVOKE, Keyword.AFFINITY, Keyword.IMPROVISE)

        /** Lands = [LAND_BASE] + [LAND_PER_MANA_VALUE] × the spells' average mana value. */
        private const val LAND_BASE = 16.5
        private const val LAND_PER_MANA_VALUE = 2.6
        private const val MIN_LANDS = 21
        private const val MAX_LANDS = 25

        /** Most dual-land copies in a deck's manabase. */
        private const val MAX_DUAL_COPIES = 8

        /** Most colours a land may make and still count as a dual ([pickDualLands]): a tri-land at most. */
        private const val MAX_DUAL_COLOURS = 3

        /** A card new to the deck comes in as at least this many copies on the strict passes. */
        private const val PLAYSET_MIN = 3

        /** Interaction copies taken before anything else — about one in five spells. */
        private const val INTERACTION_COPIES = 8

        /** Creature copies taken before the best-of-the-rest pass. */
        private const val CREATURE_COPIES = 16

        /** Most noncreature spell copies before the last fill pass; leaves twenty creature slots. */
        private const val MAX_NONCREATURE = 16

        /** Interaction is worth a little more than its limited rating in constructed. */
        private const val INTERACTION_BONUS = 1.2

        /** Per-build rating jitter: ±this share. */
        private const val JITTER = 0.15

        /** Best-rated spells per colour pair considered at all — four times the deck's distinct cards. */
        private const val CANDIDATES = 150

        /** The colours' best interaction kept as candidates beyond the [CANDIDATES] cut. */
        private const val REMOVAL_CANDIDATES = 24

        /** Cards a colour pair is judged by when picking the deck's colours. */
        private const val CORE_SIZE = 12
        private const val COLOUR_CHOICES = 3
        private const val COLOUR_SHARPNESS = 8.0

        /** Top cards tried as the build-around, and the top cards that may support it. */
        private const val ANCHOR_CHOICES = 8
        private const val ANCHOR_CORE = 40
        private const val MIN_SUPPORT = 6
        private const val MIN_SUPPORT_SCORE = 1.0

        /** How far the build-around's synergy moves a card's score: × (1 + this × score). */
        private const val SYNERGY_WEIGHT = 0.3
        private const val MAX_SYNERGY = 5.0

        /** Share of the basics every played colour gets before the pips divide the rest. */
        private const val MIN_COLOUR_SHARE = 0.25

        /**
         * [card] fits a deck of [colours]: its cost is payable off those basics and, if it's a mana
         * rock, it makes one of them. Reads the cost, not [CardDefinition.colors] — a devoid card is
         * colourless with a coloured cost (Writhing Chrysalis, {2}{R}{G}) and a Room's colours live
         * on its doors, and both slipped into off-colour decks when the colours were read instead.
         */
        fun castableIn(card: CardDefinition, colours: Set<Color>): Boolean = fitsDeckColors(card, colours)

        /**
         * The mana value a card is played at: a split card or Room (no cost of its own) at its
         * cheapest half, and an X spell for at least two more than its printed MV.
         */
        fun manaValue(card: CardDefinition): Int {
            val printed = if (card.manaCost.symbols.isEmpty() && card.cardFaces.isNotEmpty()) {
                card.cardFaces.minOf { it.manaCost.cmc }
            } else {
                card.cmc
            }
            return printed + if (card.manaCost.hasX) 2 else 0
        }

        fun curveBucket(card: CardDefinition): Int = manaValue(card).coerceIn(1, CURVE.size - 1)

        /** Constructed games are decided earlier than limited ones; the top end is worth less. */
        fun curveDiscount(card: CardDefinition): Double = when (manaValue(card)) {
            in 0..3 -> 1.0
            4 -> 0.92
            5 -> 0.8
            6 -> 0.65
            else -> 0.5
        }

        /**
         * A spell no 60-card or Highlander deck built here can expect to cast: mana value above
         * [MAX_MANA_VALUE] with no keyword that pays it down from the board or graveyard (delve,
         * convoke, affinity, improvise). These decks run no ramp package, so a well-rated 13-drop
         * (Emrakul, the Promised End) is a dead card in an aggro deck — and the Highlander build's
         * limited-style pick order would otherwise take it on rating alone.
         */
        fun beyondCurve(card: CardDefinition): Boolean =
            !card.isLand && manaValue(card) > MAX_MANA_VALUE && card.keywords.none { it in COST_REDUCING_KEYWORDS }

        /** The four-of rule; a legend is played as two, since a second copy on the battlefield dies. */
        fun copyCap(card: CardDefinition): Int = if (card.typeLine.isLegendary) 2 else 4


        private val COLOR_TO_BASIC =
            mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")
    }
}
