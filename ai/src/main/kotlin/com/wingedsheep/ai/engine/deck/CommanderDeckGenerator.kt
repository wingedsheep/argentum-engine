package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.engine.landFitsColors
import com.wingedsheep.ai.engine.producedColors
import com.wingedsheep.ai.engine.utilityLand
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import com.wingedsheep.sdk.scripting.EntersTapped
import kotlin.math.pow
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
 * ## How the library is shaped
 *
 * A Commander deck is not its 63 best-rated cards. It is a commander, a theme that commander
 * rewards, and the slots every deck fills whatever its theme: ramp, interaction, card draw, and a
 * manabase that casts all of it. So the library is built quota-first — see [build] — with the jobs
 * read by [CommanderDeckRoles] (over the AI's own [com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer])
 * and every pick tilted towards the commander's tribe, keywords and theme by [CommanderSynergy].
 * The manabase is basics plus the pool's on-colour duals and utility lands, picked by the same land
 * rules the limited builder uses.
 *
 * ## What it deliberately doesn't do
 *
 * - **Partners, Backgrounds, companions.** The deck model carries one commander; so does this.
 * - **Colourless commanders.** CR 903.5d bans the five basics from a colourless-identity deck, and
 *   Brawl's escape hatch (CR 903.12e, "any number of basics of one type") doesn't exist in
 *   Commander. Rather than build a five-basic deck the validator would reject, an Eldrazi is simply
 *   never picked — see [commanderCandidates].
 * - **Combos and named staples.** Synergy is read from what the commander's text asks for, not
 *   from a list of known packages.
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
     * [setCodes] (empty means the whole format-legal card base), led by [commander] when one is
     * named and by a drawn one otherwise.
     *
     * @throws IllegalArgumentException if [format] is not commander-shaped.
     * @return the deck, or null when the pool holds no legal commander with a buildable colour
     *         identity behind it — the caller decides what to do instead, because "no Commander deck
     *         from three Portal sets" is a lobby configuration, not a bug.
     */
    fun generate(setCodes: List<String>, format: DeckFormat, commander: String? = null): GeneratedDeck? {
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
            commanderName = commander,
        )
    }

    /**
     * Build a commander deck out of a *limited* pool — the cards a seat drafted or opened — rather
     * than out of a format's legal card base.
     *
     * Three things differ from [generate], all because the pool is the legality universe here (see
     * `DeckValidator.validateCommanderLimited`): [deckSize] is the lobby's minimum rather than a
     * format constant, copies are capped by how many the seat actually owns, and singleton is the
     * lobby's own toggle rather than CR 903.5b. The commander choice, the colour-identity filter,
     * the role quotas and the manabase rules are the same, so they are the same code.
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
     * The library is built in the order a Commander player builds one: the role quotas first
     * ([RAMP_QUOTA] ramp — fixing ramp first for a three-plus-colour commander — then
     * [REMOVAL_QUOTA] interaction and [DRAW_QUOTA] card draw), the land count from how much ramp
     * made it in, [THEME_QUOTA] cards on the commander's theme, then the rest of the curve. Every draw is weighted by rating *and* by how much the
     * card does what the commander asks ([CommanderSynergy]), so the tribe and the theme come
     * through without starving the deck of answers.
     *
     * @param pool distinct candidate cards; already scoped to whatever legality the caller enforces.
     * @param basics basic-land printings whose art the manabase is pinned to.
     * @param deckSize total cards including the commander.
     * @param copyLimit how many copies of a card may be played — 1 for singleton, the pool count
     *        for limited.
     * @param commanderName build around this commander instead of drawing one; null draws.
     */
    private fun build(
        pool: List<CardDefinition>,
        basics: List<CardDefinition>,
        deckSize: Int,
        copyLimit: (CardDefinition) -> Int,
        label: String,
        commanderName: String? = null,
    ): GeneratedDeck? {
        val commander = if (commanderName != null) {
            commanderCandidates(pool).firstOrNull { it.name == commanderName }
        } else {
            pickCommander(pool, deckSize - 1, copyLimit)
        } ?: run {
            logger.warn("No legal commander in a pool of {} cards ({})", pool.size, label)
            return null
        }
        val identity = commander.colorIdentity

        val inIdentity = pool.filter { card ->
            card.name != commander.name && card.colorIdentity.all { it in identity }
        }
        val body = inIdentity.filterNot { it.isLand }

        val librarySize = deckSize - 1
        val scale = librarySize / SINGLETON_HUNDRED_LIBRARY.toDouble()
        val synergy = CommanderSynergy(commander)
        val weight = selectionWeight(synergy)
        val roles = HashMap<String, Set<DeckRole>>()
        fun rolesOf(card: CardDefinition) = roles.getOrPut(card.name) { CommanderDeckRoles.rolesOf(card) }

        val quota = Quotas(
            ramp = (RAMP_QUOTA * scale).roundToInt(),
            removal = (REMOVAL_QUOTA * scale).roundToInt(),
            draw = (DRAW_QUOTA * scale).roundToInt(),
            theme = (THEME_QUOTA * scale).roundToInt(),
        )
        val chosen = mutableListOf<CardDefinition>()
        fun fill(want: Int, candidates: List<CardDefinition>, extra: (CardDefinition) -> Double = { 1.0 }) {
            if (want <= 0) return
            val taken = chosen.mapTo(HashSet()) { it.name }
            chosen += candidates.filterNot { it.name in taken }.weightedSample(want, random) { weight(it) * extra(it) }
        }

        val rampPool = body.filter { DeckRole.RAMP in rolesOf(it) }
        if (identity.size >= FIXING_COLOURS) {
            fill((quota.ramp + 1) / 2, rampPool.filter { CommanderDeckRoles.fixes(it, identity) }, ::rampCost)
        }
        fill(quota.ramp - chosen.size, rampPool, ::rampCost)
        val rampCount = chosen.size

        // Paper Commander's 37–38 lands of 99 assume a deck with no ramp in it; each few ramp
        // pieces stand in for a land, down to a floor that still hits land drops on its own.
        val landCount = ((librarySize * LAND_RATIO).roundToInt() - rampCount / RAMP_PER_LAND)
            .coerceAtLeast((librarySize * MIN_LAND_RATIO).roundToInt())

        fill(quota.removal, body.filter { DeckRole.REMOVAL in rolesOf(it) })
        fill(quota.draw, body.filter { DeckRole.DRAW in rolesOf(it) })
        // The theme gets slots of its own, not just better odds: in a whole-card-base pool the
        // commander's 250 Vampires are a twentieth of the cards in its colours, and a weight alone
        // left a Vampire commander with seven of them.
        fill(quota.theme, body.filter { synergy.score(it) >= MIN_THEME_SCORE })

        val spells = pickSpells(body, librarySize - landCount, copyLimit, weight, chosen)

        val landSlots = librarySize - spells.size
        val nonbasics = pickNonbasicLands(inIdentity.filter { it.isLand && !it.typeLine.isBasicLand }, identity, spells, landSlots, scale)

        // Whatever the pool couldn't fill becomes extra basics. They are the one card type with no
        // copy cap (CR 903.5b), so this always reaches the exact size the format demands — a deck
        // one card short is a deck the validator rejects and the seat can't play.
        val basicsNeeded = landSlots - nonbasics.size
        val manabase = BasicLandSplit.split(spells + commander, identity, basicsNeeded, MIN_COLOUR_SHARE)

        val deckList = LinkedHashMap<String, Int>()
        for (card in spells + nonbasics) deckList.merge(card.name, 1, Int::plus)
        for ((name, count) in manabase) deckList.merge(name, count, Int::plus)

        val pinned = BoosterGenerator.withBasicLandArt(deckList, basics.associateBy { it.name })
        val distinctSpells = spells.distinctBy { it.name }
        logger.info(
            "Built a {} deck for {} ({}): {} spells ({} ramp, {} interaction, {} draw), {} lands ({} non-basic)",
            label,
            commander.name,
            identity.joinToString("/") { it.name.first().toString() }.ifEmpty { "C" },
            spells.size,
            distinctSpells.count { DeckRole.RAMP in rolesOf(it) },
            distinctSpells.count { DeckRole.REMOVAL in rolesOf(it) },
            distinctSpells.count { DeckRole.DRAW in rolesOf(it) },
            landSlots,
            nonbasics.size,
        )
        return GeneratedDeck(deckList = pinned, commander = commander.name)
    }

    private data class Quotas(val ramp: Int, val removal: Int, val draw: Int, val theme: Int)

    /**
     * The sampling weight of a card: its rating cubed (so the bombs come out and the filler still
     * varies), multiplied up by how much it does what the commander asks. Memoized per build — the
     * same card is weighed once per quota it is a candidate for and again on the curve.
     */
    private fun selectionWeight(synergy: CommanderSynergy): (CardDefinition) -> Double {
        val ops = ConstructedRatings.ops()
        val cache = HashMap<String, Double>()
        return { card ->
            cache.getOrPut(card.name) {
                val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
                rating * rating * rating * (1 + SYNERGY_WEIGHT * synergy.score(card).coerceAtMost(MAX_SYNERGY))
            }
        }
    }

    /** Ramp is worth most when it comes down early; a six-mana rock is a six-drop that taps for one. */
    private fun rampCost(card: CardDefinition): Double = when {
        card.cmc <= 2 -> 2.0
        card.cmc == 3 -> 1.2
        card.cmc == 4 -> 0.5
        else -> 0.2
    }

    /**
     * The non-basic lands worth a basic's slot: the [landFitsColors] rule the limited builder uses
     * (a dual for two deck colours, or an untapped on-colour land), held to CR 903.5d — every colour
     * it makes is in the commander's identity — plus at most one colourless utility land with a job
     * in this deck ([utilityLand]). A fetch for "a basic land card" makes the cut only when there are
     * three colours to fix; in a one- or two-colour deck it is a tapped basic.
     *
     * Lands that fix more of the deck's colours go first, untapped before tapped, so a three-colour
     * deck's land slots fill with its tri-lands and duals before anything else.
     */
    private fun pickNonbasicLands(
        lands: List<CardDefinition>,
        identity: Set<Color>,
        spells: List<CardDefinition>,
        slots: Int,
        scale: Double,
    ): List<CardDefinition> {
        val cap = minOf(slots, ((NONBASIC_LAND_CAP[identity.size] ?: MAX_NONBASIC_LANDS) * scale).roundToInt())
        if (cap <= 0) return emptyList()
        val fitting = lands.filter { land ->
            val produced = producedColors(land)
            produced.all { it in identity } &&
                (produced.isNotEmpty() || identity.size >= FIXING_COLOURS) &&
                landFitsColors(land, identity)
        }
        val ranked = fitting.shuffled(random).sortedWith(
            compareByDescending<CardDefinition> { land -> producedColors(land).count { it in identity } }
                .thenBy { land -> land.script.replacementEffects.any { it is EntersTapped } },
        )
        val utility = utilityLand(lands, identity, spells)
        return ranked.take(if (utility != null) cap - 1 else cap) + listOfNotNull(utility)
    }

    /**
     * Draw one commander from [pool], biased towards cards Draftsim rates highly and towards narrow
     * colour identities.
     *
     * The identity bias is the load-bearing half. Identity is a *deck-construction* constraint here,
     * not a bonus: a five-colour commander opens the whole pool but hands the builder a manabase
     * split five ways, which casts little even with the pool's duals in it. One and two colours are
     * the shapes a mostly-basic manabase supports, three is playable with fixing, and four or five
     * is a last resort for a pool with nothing narrower in it.
     *
     * The narrow-identity bias has a floor, though: the pool has to hold enough spells inside the
     * commander's identity to fill the library's spell slots ([spellSupport]). Whatever the pool
     * can't fill becomes basics, so an unchecked mono-coloured pick from one set (Eluge out of
     * Bloomburrow) seated a 99 with 46 lands. A candidate short of that is drawn only when no
     * candidate clears it, weighted down by how far short it falls.
     */
    private fun pickCommander(
        pool: List<CardDefinition>,
        librarySize: Int,
        copyLimit: (CardDefinition) -> Int,
    ): CardDefinition? {
        val ops = ConstructedRatings.ops()
        val candidates = commanderCandidates(pool)
        if (candidates.isEmpty()) return null
        val support = spellSupport(pool, copyLimit)
        val needed = (librarySize * (1 - LAND_RATIO)).roundToInt().coerceAtLeast(1)
        fun supportOf(card: CardDefinition) = support(card.colorIdentity) - (if (card.isLand) 0 else copyLimit(card))
        val viable = candidates.filter { supportOf(it) >= needed }
        return viable.ifEmpty { candidates }.weightedSample(1, random) { card ->
            val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            val shortfall = if (viable.isEmpty()) (supportOf(card).toDouble() / needed).coerceIn(0.01, 1.0).pow(SHORTFALL_SHARPNESS) else 1.0
            rating * (IDENTITY_WEIGHT[card.colorIdentity.size] ?: LAST_RESORT_IDENTITY_WEIGHT) * shortfall
        }.firstOrNull()
    }

    /**
     * How many nonland cards (counting the copies [copyLimit] allows) [pool] holds inside a given
     * colour identity. Counted once per identity bitmask and summed over subsets, so asking for
     * every candidate commander stays linear in the pool even over the whole card base.
     */
    private fun spellSupport(pool: List<CardDefinition>, copyLimit: (CardDefinition) -> Int): (Set<Color>) -> Int {
        fun mask(colours: Set<Color>) = colours.fold(0) { acc, c -> acc or (1 shl c.ordinal) }
        val byMask = IntArray(1 shl Color.entries.size)
        pool.forEach { card -> if (!card.isLand) byMask[mask(card.colorIdentity)] += copyLimit(card) }
        return { identity ->
            val m = mask(identity)
            byMask.indices.sumOf { sub -> if (sub and m.inv() == 0) byMask[sub] else 0 }
        }
    }

    /**
     * The cards in [pool] that could lead a deck.
     *
     * Colourless-identity commanders are excluded rather than supported: CR 903.5d allows a basic
     * land in the deck only if every colour it produces is in the commander's identity, so an
     * Eldrazi deck's manabase has to be Wastes or colourless utility lands — neither of which this
     * builder's basic-led manabase can produce. Building one anyway would produce a deck the deck
     * validator rejects for colour identity, which is worse than declining the commander.
     */
    private fun commanderCandidates(pool: List<CardDefinition>): List<CardDefinition> =
        pool.filter { CommanderEligibility.isLegalCommander(it) && it.colorIdentity.isNotEmpty() }

    /**
     * Fill [slots] with the [preselected] role cards plus the best cards [body] offers at each
     * point on the curve.
     *
     * Bucket by mana value and draw each bucket's share separately, rather than taking the top
     * [slots] cards overall: rating alone would hand back a pile of six-drops, because Draftsim
     * rates a card's power and not its cost. A bucket's share counts the role cards already in it,
     * so a deck whose ramp is all two-drops plays fewer other two-drops.
     *
     * A bucket the pool can't fill spills its deficit forward into the next one, and anything left
     * at the end is taken from whatever remains at any cost — a slightly wrong curve beats a short
     * deck. What the pool genuinely cannot supply comes back as fewer cards than [slots]; the caller
     * turns the difference into lands.
     */
    private fun pickSpells(
        body: List<CardDefinition>,
        slots: Int,
        copyLimit: (CardDefinition) -> Int,
        weight: (CardDefinition) -> Double,
        preselected: List<CardDefinition>,
    ): List<CardDefinition> {
        if (slots <= 0) return emptyList()
        val fixed = preselected.take(slots)
        val takenNames = fixed.mapTo(mutableSetOf()) { it.name }
        val byBucket = body.filterNot { it.name in takenNames }.groupBy { minOf(it.cmc, TOP_CURVE_BUCKET) }
        val fixedPerBucket = fixed.groupingBy { minOf(it.cmc, TOP_CURVE_BUCKET) }.eachCount()

        var extras = mutableListOf<CardDefinition>()
        var carriedDeficit = 0
        for ((bucket, share) in CURVE) {
            val want = (slots * share).roundToInt() - (fixedPerBucket[bucket] ?: 0) + carriedDeficit
            if (want <= 0) {
                carriedDeficit = 0
                continue
            }
            val available = byBucket[bucket].orEmpty().filterNot { it.name in takenNames }
            val drawn = available.weightedSample(want, random, weight)
            extras += drawn
            drawn.forEach { takenNames += it.name }
            carriedDeficit = want - drawn.size
        }

        // Curve targets are fractions and the pool is lumpy, so the buckets rarely land exactly on
        // `slots`. Trim the overshoot from the top of the curve (the most expendable cards — never
        // a role card) and top up any shortfall from whatever the pool still has.
        val open = slots - fixed.size
        if (extras.size > open) extras = extras.sortedBy { it.cmc }.take(open).toMutableList()
        if (extras.size < open) {
            val remaining = body.filterNot { it.name in takenNames }
            extras += remaining.weightedSample(open - extras.size, random, weight)
        }
        return duplicateToFill(fixed + extras, body, slots, copyLimit, weight)
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

        /** The library of a hundred-card deck; the role quotas below are per 99 and scale with it. */
        private const val SINGLETON_HUNDRED_LIBRARY = SINGLETON_HUNDRED_DECK_SIZE - 1

        /**
         * Share of the library that is land before ramp is counted. 38% is the paper-Commander
         * consensus for a deck with no ramp (37–38 of 99) and scales to the 60-card shapes (22 of 59).
         */
        private const val LAND_RATIO = 0.38

        /** How steeply an under-supported commander loses weight when every candidate is short. */
        private const val SHORTFALL_SHARPNESS = 3.0

        /** The floor the ramp discount can't take the land count below: 36 of 99, 21 of 59. */
        private const val MIN_LAND_RATIO = 0.36

        /** One land comes out of the manabase per this many ramp pieces in the deck. */
        private const val RAMP_PER_LAND = 5

        /**
         * Role quotas per 99 cards — the shape most Commander deck-building guides give: about ten
         * ramp, eight or so pieces of interaction, and a handful of real card draw. A pool that
         * can't supply a quota leaves the slot to the curve.
         */
        private const val RAMP_QUOTA = 10
        private const val REMOVAL_QUOTA = 8
        private const val DRAW_QUOTA = 7

        /**
         * Slots per 99 reserved for cards that do what the commander asks ([CommanderSynergy] score
         * of at least [MIN_THEME_SCORE] — one real hit, not just a shared evergreen keyword). With
         * the role cards that happen to be on theme too, this makes a tribal deck mostly its tribe.
         */
        private const val THEME_QUOTA = 25
        private const val MIN_THEME_SCORE = 1.0

        /** From this many colours on, half the ramp quota goes to ramp that also fixes. */
        private const val FIXING_COLOURS = 3

        /**
         * Most non-basic lands per 99 cards by identity size. Few in one colour — there is nothing
         * to fix, only untapped utility lands earn a slot — and most of the lands' worth in three or
         * more, where the duals and tri-lands are the fixing.
         */
        private val NONBASIC_LAND_CAP = mapOf(1 to 3, 2 to 8)
        private const val MAX_NONBASIC_LANDS = 14

        /** Share of the basics every identity colour gets before the pips divide the rest. */
        private const val MIN_COLOUR_SHARE = 0.5

        /**
         * How far [CommanderSynergy] moves a card's sampling weight: weight × (1 + this × score),
         * with the score capped at [MAX_SYNERGY]. A creature of the tribe the commander names (score 5)
         * is eleven times as likely as an equally rated stranger: a tribal deck is mostly its tribe.
         */
        private const val SYNERGY_WEIGHT = 2.0
        private const val MAX_SYNERGY = 6.0

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
         * a mostly-basic manabase supports one or two colours comfortably and three with fixing.
         */
        private val IDENTITY_WEIGHT = mapOf(1 to 1.0, 2 to 0.9, 3 to 0.35)
        private const val LAST_RESORT_IDENTITY_WEIGHT = 0.05
    }
}
