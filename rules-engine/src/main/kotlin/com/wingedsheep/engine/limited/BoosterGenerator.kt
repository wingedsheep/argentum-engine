package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.BasicLandArt
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import kotlin.random.Random

/**
 * Generates booster packs and sealed pools from card sets.
 *
 * Pack composition is delegated to each set's [BoosterStrategy], which defaults by
 * era: classic sets use the 15-card [StandardBooster] (11C / 3U / 1R), mythic-era
 * sets (Shards of Alara, 2008, onward) drop to 10 commons, and Play-Booster-era sets
 * (Murders at Karlov Manor, 2024, onward) use [com.wingedsheep.sdk.limited.PlayBooster].
 * Sets can override with custom strategies (guaranteed legendary, commander draft).
 *
 * ## Scope
 *
 * A pure function of (set configs) → card pool. No Spring, I/O, or WebSocket
 * dependencies. Callers supply their own [SetConfig] map — the generator
 * itself does not import any specific card set, so it stays compatible with
 * rules-engine's "no card-specific dependencies" rule.
 *
 * Application-level set catalogues (Portal, Bloomburrow, …) live in their
 * respective consumer modules (e.g. `game-server`'s `sealed/SetConfigs.kt`).
 */
class BoosterGenerator(
    val availableSets: Map<String, SetConfig>
) {

    /**
     * Return an isolated generator containing this generator's catalogue plus [extra].
     *
     * The original generator and its map are left untouched. Entries in [extra] intentionally
     * replace entries with the same set code, which lets a lobby install a scoped synthetic set
     * without mutating the application-wide catalogue.
     */
    fun withSets(extra: Map<String, SetConfig>): BoosterGenerator =
        BoosterGenerator(availableSets + extra)

    /**
     * Configuration for a card set that can be used for sealed.
     */
    data class SetConfig(
        val setCode: String,
        val setName: String,
        val cards: List<CardDefinition>,
        val basicLands: List<CardDefinition>,
        /**
         * Implemented cards carrying this set code that were not in its paper booster product.
         * Normal generation ignores them; a lobby may opt in by deriving a scoped config whose
         * [cards] is the union. Kept separate so paper-accurate limited remains the default.
         */
        val extraCardsByProduct: Map<String, List<CardDefinition>> = emptyMap(),
        val incomplete: Boolean = false,
        /**
         * Whether this set is curated/validated for sealed & draft play. Sets that aren't
         * sealed-supported can still be selected (so every set is playable), but clients surface
         * them as "partial" so a host opts into them deliberately. Defaults to `true` so existing
         * callers that hand-build configs keep their prior behaviour.
         */
        val sealedSupported: Boolean = true,
        /**
         * True for extension sets (bonus sheets like The Big Score) that are fully implemented but
         * too thin to carry a sealed/draft pool alone. See [com.wingedsheep.sdk.model.MtgSet.extensionSet];
         * lobbies require at least one non-extension set in any selection containing one.
         */
        val extensionSet: Boolean = false,
        val block: String? = null,
        /** Set release date in ISO `YYYY-MM-DD` form, or null if unknown. Used by clients to sort sets chronologically. */
        val releaseDate: String? = null,
        val boosterStrategy: BoosterStrategy = StandardBooster(),
        /**
         * Per-printing rows the set contributes (typically [com.wingedsheep.sdk.model.MtgSet.printings]).
         * Only the alternate-frame entries among these are eligible for the variant slot; canonical
         * reprints are ignored by [applyVariantPrintings].
         */
        val printings: List<Printing> = emptyList(),
        /** See [com.wingedsheep.sdk.model.MtgSet.boosterVariantChance]. 0.0 disables the variant slot. */
        val variantChance: Double = 0.0,
    ) {
        /**
         * A set is "fully implemented" — and therefore shown by default in set pickers — only when
         * it is both curated for sealed/draft and not flagged incomplete. Everything else is
         * "partial": still selectable, but hidden behind the lobby's partial-sets toggle.
         */
        val fullyImplemented: Boolean get() = sealedSupported && !incomplete

        /**
         * Number of distinct cards the set contributes, for "X cards" displays in set pickers.
         *
         * Counts reprint [printings] — a Commander/precon set's reprints have their canonical
         * [CardDefinition] in an earlier set, so they never appear in [cards] and would otherwise
         * be undercounted. De-duplicating by name collapses any alternate-frame printing of a card
         * already defined here, while still counting reprint-only names exactly once.
         */
        val distinctCardCount: Int
            get() = (cards.asSequence().map { it.name } + printings.asSequence().map { it.name })
                .distinct().count()
    }

    companion object {

        /**
         * Re-skin each generated card with one of its alternate-frame printings (showcase /
         * borderless) with probability [chance], leaving the card's oracle identity untouched.
         *
         * Pure and deterministic for a given [random]: the roll is independent per card, only
         * fires for a card that has a matching [Printing.isAlternateFrame] printing of the same
         * name, and otherwise passes the card through unchanged. A non-positive [chance] or an
         * empty printing pool is a no-op. See [CardDefinition.withPrinting] for the overlay.
         */
        fun applyVariantPrintings(
            cards: List<CardDefinition>,
            printings: List<Printing>,
            chance: Double,
            random: Random,
        ): List<CardDefinition> {
            if (chance <= 0.0) return cards
            val variantsByName = printings.filter { it.isAlternateFrame }.groupBy { it.name }
            if (variantsByName.isEmpty()) return cards
            return cards.map { card ->
                val variants = variantsByName[card.name]
                if (!variants.isNullOrEmpty() && random.nextDouble() < chance) {
                    card.withPrinting(variants.random(random))
                } else {
                    card
                }
            }
        }

        /**
         * Pin every basic land in a deck list to the printing it was built with.
         *
         * Replaces plain land names (e.g., "Plains" → 8) with the printing identifier of the
         * matching entry in [basics] (e.g., "Plains#BLB-262" → 8). [basics] is the very map the
         * player's deck builder was handed by [getBasicLands], so the art previewed while building
         * is the art the deck is played with — one standard-art printing per type, not a mix.
         *
         * Without this the name would resolve to whichever printing the card registry considers
         * canonical for "Plains" (some other set entirely), so the stamp is what keeps a limited
         * deck's basics inside the set that was drafted.
         *
         * Names absent from [basics] pass through untouched — that covers every spell, and a basic
         * land type the set doesn't print. Keying off [basics] rather than a hardcoded name list is
         * also what lets a colorless-basic set (Final Fantasy's Wastes) get its art stamped.
         *
         * Counts are merged rather than replaced, so a deck list that already names the printing it
         * gets stamped with (premade lists may carry `Name#SET-CN` entries) keeps all its copies.
         *
         * The identifiers are `Name#SetCode-CollectorNumber` strings. The server session converts them
         * into rich `CardEntry` printing references at game start, so the printing registry can
         * overlay the correct art without changing the card's canonical rules identity.
         *
         * @param deckList The submitted deck list, keyed by card name
         * @param basics Land name to the printing that deck building offered, from [getBasicLands]
         * @return The deck list with basic land names replaced by printing identifiers
         */
        fun withBasicLandArt(
            deckList: Map<String, Int>,
            basics: Map<String, CardDefinition>,
        ): Map<String, Int> = withCardArt(deckList, basics.values)

        /** Pin submitted cards to the exact set printing present in a Limited pool. */
        fun withCardArt(
            deckList: Map<String, Int>,
            cards: Collection<CardDefinition>,
        ): Map<String, Int> {
            val cardsByName = cards.associateBy { it.name }
            val result = mutableMapOf<String, Int>()
            for ((cardName, count) in deckList) {
                val identifier = cardsByName[cardName]?.let { printingIdentifier(cardName, it) } ?: cardName
                result.merge(identifier, count, Int::plus)
            }
            return result
        }

        /**
         * The `Name#SetCode-CollectorNumber` identifier for [land]. Both coordinates are required:
         * a collector number without a set code cannot address a unique printing.
         */
        private fun printingIdentifier(cardName: String, land: CardDefinition): String {
            val collectorNumber = land.metadata.collectorNumber ?: return cardName
            val setCode = land.setCode ?: return cardName
            return "$cardName#$setCode-$collectorNumber"
        }
    }

    /**
     * Get set configuration by set code.
     */
    fun getSetConfig(setCode: String): SetConfig? = availableSets[setCode]

    /**
     * Generate a single booster pack from the specified set.
     *
     * @param setCode The set code to generate from (e.g., "POR" for Portal)
     * @param strategy Optional override; when null, uses the set's configured strategy.
     *                 Pass an explicit strategy (e.g., [com.wingedsheep.sdk.limited.CommanderDraftBooster])
     *                 to ship Brawl / Commander Draft packs without mutating [SetConfig].
     * @param bannedCardNames Oracle card names to exclude from the pool before packing (tournament
     *                        host ban list). Matched case-insensitively; empty = no exclusions.
     * @return List of card definitions
     * @throws IllegalArgumentException if set code is not found
     */
    fun generateBooster(
        setCode: String,
        strategy: BoosterStrategy? = null,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        val setConfig = availableSets[setCode]
            ?: throw IllegalArgumentException("Unknown set code: $setCode")
        val effectiveStrategy = strategy ?: setConfig.boosterStrategy
        val generated = effectiveStrategy.generate(boosterPool(setConfig.cards, bannedCardNames), Random.Default)
        return applyVariantPrintings(generated, setConfig.printings, setConfig.variantChance, Random.Default)
    }

    /**
     * Generate a single booster pack from one of the specified sets.
     * Randomly selects one set and generates a booster from that set only.
     * Each booster contains cards from a single set, never mixed.
     *
     * @param setCodes The set codes to choose from
     * @param strategy Optional strategy override applied to whichever set is picked.
     * @param chaos When true, the booster pulls from the union of all selected sets' card pools
     *              instead of a single set (Chaos Boosters lobby option). Incomplete sets are
     *              always merged regardless of this flag.
     * @return List of card definitions
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateBooster(
        setCodes: List<String>,
        strategy: BoosterStrategy? = null,
        chaos: Boolean = false,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }

        // Validate all set codes exist
        val setConfigs = setCodes.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // Combined-pool path: forced by Chaos mode, or by any incomplete set in the selection.
        if (chaos || setConfigs.any { it.incomplete }) {
            val combinedCards = setConfigs.flatMap { it.cards }
            val effectiveStrategy = strategy ?: combinedPoolStrategy(setConfigs)
            return effectiveStrategy.generate(boosterPool(combinedCards, bannedCardNames), Random.Default)
        }

        // Pick a random set and generate a booster from it
        val selectedSet = setCodes.random()
        return generateBooster(selectedSet, strategy, bannedCardNames)
    }

    /**
     * Generate a sealed pool of [boosterCount] boosters from the specified set.
     *
     * @param setCode The set code to generate from
     * @param boosterCount Number of boosters to open (default 6)
     * @param strategy Optional strategy override applied to every generated pack.
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if set code is not found
     */
    fun generateSealedPool(
        setCode: String,
        boosterCount: Int = 6,
        strategy: BoosterStrategy? = null,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        return (1..boosterCount).flatMap { generateBooster(setCode, strategy, bannedCardNames) }
    }

    /**
     * Generate a sealed pool from multiple sets with equal distribution.
     * Boosters are distributed evenly across all sets. Any remainder is
     * distributed deterministically based on the distributionSeed.
     *
     * Example: 2 sets with 6 boosters → 3 boosters from each set
     * Example: 3 sets with 6 boosters → 2 boosters from each set
     * Example: 2 sets with 5 boosters → 3 from one set, 2 from the other
     *
     * @param setCodes The set codes to generate from
     * @param boosterCount Number of boosters to open (default 6)
     * @param distributionSeed Seed for remainder distribution. Use the same seed
     *                         for all players in a tournament to ensure they get
     *                         the same set distribution (e.g., both get 3 Portal + 2 Onslaught).
     *                         If null, uses random distribution.
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateSealedPool(
        setCodes: List<String>,
        boosterCount: Int = 6,
        distributionSeed: Long? = null,
        strategy: BoosterStrategy? = null,
        chaos: Boolean = false,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }
        if (setCodes.size == 1) {
            return generateSealedPool(setCodes.first(), boosterCount, strategy, bannedCardNames)
        }

        // Validate all set codes exist
        val setConfigs = setCodes.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // Combined-pool path: forced by Chaos mode, or by any incomplete set.
        if (chaos || setConfigs.any { it.incomplete }) {
            val combinedCards = setConfigs.flatMap { it.cards }
            val combinedStrategy = strategy ?: combinedPoolStrategy(setConfigs)
            val combinedPool = boosterPool(combinedCards, bannedCardNames)
            return (1..boosterCount).flatMap { combinedStrategy.generate(combinedPool, Random.Default) }
        }

        // Use seeded random for deterministic distribution, or default random
        val distributionRandom = distributionSeed?.let { Random(it) } ?: Random

        // Calculate even distribution
        val boostersPerSet = boosterCount / setCodes.size
        val remainder = boosterCount % setCodes.size

        // Build list of set codes for each booster
        val boosterAssignments = mutableListOf<String>()

        // Add base allocation for each set
        setCodes.forEach { setCode ->
            repeat(boostersPerSet) {
                boosterAssignments.add(setCode)
            }
        }

        // Distribute remainder using seeded random (deterministic for same seed)
        val shuffledSets = setCodes.shuffled(distributionRandom)
        repeat(remainder) { i ->
            boosterAssignments.add(shuffledSets[i])
        }

        // Note: We don't shuffle the final assignments - each player gets their
        // own random card contents anyway, only the set distribution needs to match
        return boosterAssignments.flatMap { generateBooster(it, strategy, bannedCardNames) }
    }

    /**
     * Generate a sealed pool using an explicit per-set booster distribution.
     *
     * @param boosterDistribution Map of set code to number of boosters from that set
     * @return List of all cards in the sealed pool
     * @throws IllegalArgumentException if any set code is not found
     */
    fun generateSealedPool(
        boosterDistribution: Map<String, Int>,
        strategy: BoosterStrategy? = null,
        chaos: Boolean = false,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        if (boosterDistribution.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }

        // Validate all set codes exist
        val setConfigs = boosterDistribution.keys.map { setCode ->
            availableSets[setCode]
                ?: throw IllegalArgumentException("Unknown set code: $setCode")
        }

        // Combined-pool path: forced by Chaos mode, or by any incomplete set. Pack count = sum.
        if (chaos || setConfigs.any { it.incomplete }) {
            val combinedCards = setConfigs.flatMap { it.cards }
            val totalBoosters = boosterDistribution.values.sum()
            val combinedStrategy = strategy ?: combinedPoolStrategy(setConfigs)
            val combinedPool = boosterPool(combinedCards, bannedCardNames)
            return (1..totalBoosters).flatMap { combinedStrategy.generate(combinedPool, Random.Default) }
        }

        // Generate boosters per set according to distribution
        return boosterDistribution.flatMap { (setCode, count) ->
            (1..count).flatMap { generateBooster(setCode, strategy, bannedCardNames) }
        }
    }

    /**
     * The basic lands a set offers for limited deck building: one printing per land type.
     *
     * A set prints several arts of each basic, but a limited deck gets exactly one of them — the
     * set's **standard** art, i.e. the lowest-numbered variant that's actually in the draft/sealed
     * product (see [BasicLandArt]). Special treatments (full-art, extended, borderless) stay
     * defined for collection and display; they're simply not what a drafted deck is played with.
     * The chosen printing is both what the deck builder previews and — via [withBasicLandArt] —
     * what every copy in the submitted deck resolves to.
     *
     * @param setCode The set code
     * @return Map of land name to the set's standard printing of it
     */
    fun getBasicLands(setCode: String): Map<String, CardDefinition> {
        val setConfig = availableSets[setCode]
            ?: throw IllegalArgumentException("Unknown set code: $setCode")

        return setConfig.basicLands
            .filter { it.metadata.inBooster }
            .groupBy { it.name }
            .mapValues { (_, variants) -> variants.minWith(BasicLandArt.standardFirst) }
    }

    /**
     * [getBasicLands] for a multi-set pool: the basics come from the first set that prints any, so
     * a mixed pool still hands out one coherent set of lands rather than a blend.
     *
     * @param setCodes The set codes
     * @return Map of land name to the standard printing of it, empty if no set prints basics
     */
    fun getBasicLands(setCodes: List<String>): Map<String, CardDefinition> {
        if (setCodes.isEmpty()) {
            throw IllegalArgumentException("At least one set code is required")
        }
        return setCodes.asSequence()
            .map { getBasicLands(it) }
            .firstOrNull { it.isNotEmpty() }
            ?: emptyMap()
    }

    /**
     * Strategy for the combined-pool (chaos / incomplete-set) path: a single set keeps its own
     * configured pack shape; a genuine multi-set mix falls back to the classic 15-card booster,
     * since the merged pool has no one era to honor.
     */
    private fun combinedPoolStrategy(setConfigs: List<SetConfig>): BoosterStrategy =
        setConfigs.singleOrNull()?.boosterStrategy ?: StandardBooster()

    /**
     * Strip basic lands, meld results, and non-booster cards (Special Guests / The List / promos),
     * plus any cards on the tournament host's [bannedCardNames] ban list; strategies operate on the
     * booster pool only. Banned names are matched case-insensitively so a host typo in casing
     * still excludes the card.
     *
     * Meld results ([CardDefinition.meldResult]) are dropped here rather than left to
     * `metadata.inBooster`: Scryfall reports them as `booster: true` — the physical card *is* in
     * the pack, as the meld parts' back halves — so the product-level flag can't tell a drafter
     * they're unobtainable. Opening one hands a player a permanent they can never cast.
     */
    private fun boosterPool(
        allCards: List<CardDefinition>,
        bannedCardNames: Set<String> = emptySet(),
    ): List<CardDefinition> {
        if (bannedCardNames.isEmpty()) {
            return allCards.filter { it.isBoosterEligible }
        }
        val banned = bannedCardNames.mapTo(HashSet(bannedCardNames.size)) { it.trim().lowercase() }
        return allCards.filter { it.isBoosterEligible && it.name.trim().lowercase() !in banned }
    }

    private val CardDefinition.isBoosterEligible: Boolean
        get() = !typeLine.isBasicLand && !meldResult && metadata.inBooster
}
