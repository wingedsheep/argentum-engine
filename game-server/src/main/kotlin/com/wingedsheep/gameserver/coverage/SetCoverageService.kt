package com.wingedsheep.gameserver.coverage

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.MtgSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

/**
 * Computes per-set card-implementation coverage for the Set Completion view.
 *
 * The denominator — how many cards a set canonically *has* — is not knowable at
 * runtime: it lives only in the local Scryfall cache that `scripts/card-status`
 * populates. `scripts/gen-set-totals` bakes those canonical card names into the
 * committed `coverage/set-totals.json` resource, split into **draft** (booster-relevant —
 * some printing of the card in that set is Scryfall `booster: true`) and **extra**
 * (completionist exclusives), same partitioning as `card-status` so the numbers match the
 * mtgish coverage TUI.
 *
 * At request time this service joins that static denominator with the *live*
 * [MtgSetCatalog] numerator: a set's implemented count is the number of its canonical
 * names we've actually authored (`cards` + `basicLands` + reprint `printings`). Because
 * coverage is an intersection against the canonical name set, `implemented` can never
 * exceed the total — a name we author that Scryfall doesn't list for the set simply
 * doesn't count, rather than pushing the bar past 100%.
 *
 * The headline percentage is over the **booster (draft)** cards only — a set reads 100%
 * once every boosterable card is implemented; the completionist extras are reported
 * separately. Sets with no booster at all (Commander decks, supplemental products) have
 * every card flagged `booster: false`, so there the whole set *is* the main pool and there
 * are no separate extras — otherwise the headline would read a useless 0/0.
 *
 * The extras are broken out into Scryfall's own set-page sections — "Starter Decks", "Promos",
 * "Beginner Box", … — see [ExtraGroupDTO].
 *
 * A few cards are flagged [NotPlanned] in `coverage/card-exclusions.json` — they need mechanics
 * the engine will never carry (ante, subgames, physical dexterity). They stay in the card lists
 * so the detail view can show them, but they drop out of the denominator while unimplemented, so
 * "complete" means *everything we intend to build is built* rather than a bar that can never fill.
 */
@Service
class SetCoverageService {

    /**
     * Why a card will never be implemented. Exclusion is represented *as* its reason rather than
     * a boolean plus an optional note, so a not-planned card without a "why" can't be expressed —
     * every surface, down to the tooltip, always has something to show.
     */
    @Serializable
    data class NotPlanned(
        /** Short reason key — `ante`, `subgame`, `dexterity`. Groups cards and labels the badge. */
        val kind: String,
        /** Player-facing sentence explaining the decision. */
        val why: String,
    )

    /** One canonical card with its set-specific Scryfall art, as baked by `scripts/gen-set-totals`. */
    @Serializable
    private data class CanonicalCard(
        val name: String,
        val img: String? = null,
        /**
         * Scryfall-style section heading for an extra — "Starter Decks", "Promos", … — derived by
         * `scripts/gen-set-totals` from the printing's `promo_types`. Null on booster cards and on
         * baked resources predating the split; both fall back to a single unnamed extras section.
         */
        val group: String? = null,
        /** Non-null when we've decided never to implement this card. */
        val notPlanned: NotPlanned? = null,
    )

    /** One catalogued set's canonical card universe, as baked by `scripts/gen-set-totals`. */
    @Serializable
    private data class CanonicalSet(
        val code: String,
        val name: String,
        val releaseDate: String? = null,
        val setType: String? = null,
        /**
         * Whether the set is currently legal in the Standard format, baked by `scripts/gen-set-totals`
         * from the full canonical Scryfall pool (same rule as `scripts/card-status`). Baked rather than
         * derived at runtime because the only runtime legality source is the implemented-card mirror,
         * which under-counts partially-implemented sets. Defaults `false` for older baked resources.
         */
        val standardLegal: Boolean = false,
        val draft: List<CanonicalCard> = emptyList(),
        val extra: List<CanonicalCard> = emptyList(),
    ) {
        /**
         * Cards that drive the headline %: the booster (draft) pool when the set has one,
         * otherwise the whole set (Commander / supplemental products have no booster).
         */
        val mainCards: List<CanonicalCard> get() = draft.ifEmpty { extra }

        /** Completionist extras reported separately — only when the set also has a booster. */
        val secondaryCards: List<CanonicalCard> get() = if (draft.isEmpty()) emptyList() else extra
    }

    /** Per-set coverage row served to the Set Completion grid. */
    data class SetCoverageDTO(
        val code: String,
        val name: String,
        val releaseDate: String?,
        val setType: String?,
        val block: String?,
        /** Booster (draft) cards we've authored. Always `<= total`; drives the headline %. */
        val implemented: Int,
        /** Booster (draft) cards we intend to build — canonical count minus [notPlanned]. */
        val total: Int,
        /** Completionist extras we've authored (starter exclusives, bonus sheets, Special Guests). */
        val extraImplemented: Int,
        /** Completionist extras we intend to build — canonical count minus [extraNotPlanned]. */
        val extraTotal: Int,
        /** Booster cards we've decided never to implement; excluded from [total]. */
        val notPlanned: Int,
        /** Completionist extras we've decided never to implement; excluded from [extraTotal]. */
        val extraNotPlanned: Int,
        /** `implemented / total * 100` (booster cards), one decimal. `0.0` when [total] is `0`. */
        val percent: Double,
        /** Whether this set is currently legal in the Standard format (baked by `scripts/gen-set-totals`). */
        val inStandard: Boolean,
    )

    /** One canonical card and whether we've implemented it — for the per-set detail view. */
    data class CardCoverageDTO(
        val name: String,
        val implemented: Boolean,
        /** Set-specific Scryfall art (direct CDN URL, normal size); null if Scryfall had none. */
        val imageUri: String?,
        /** Non-null when the card is deliberately never going to be implemented, and why. */
        val notPlanned: NotPlanned?,
    )

    /**
     * One section of a set's completionist extras, mirroring the way scryfall.com/sets/<code>
     * breaks a set page up — "Starter Decks", "Promos", "Beginner Box", … — so the view can say
     * *where* a non-booster card comes from rather than piling them all into one "Extras" list.
     *
     * Only the extras are sectioned. Scryfall's other headings are art-variant runs (Borderless,
     * Showcase, Extended Art, Raised Foil); those are alternate *printings* of cards already in the
     * draft pool, so against a card-name denominator they contain nothing new and never appear here.
     */
    data class ExtraGroupDTO(
        /** Section heading, e.g. `Starter Decks`. */
        val label: String,
        /** Cards in this section we've authored. */
        val implemented: Int,
        /** Cards in this section we intend to build — count minus [notPlanned]. */
        val total: Int,
        /** Cards in this section flagged never-to-implement; excluded from [total], still in [cards]. */
        val notPlanned: Int,
        /** The section's cards, A→Z, each marked. */
        val cards: List<CardCoverageDTO>,
    )

    /** A single set's full canonical card list, split into booster + sectioned extras, each marked. */
    data class SetDetailDTO(
        val code: String,
        val name: String,
        val releaseDate: String?,
        val block: String?,
        val implemented: Int,
        val total: Int,
        val extraImplemented: Int,
        val extraTotal: Int,
        /** Booster cards flagged never-to-implement; excluded from [total] but still listed in [draft]. */
        val notPlanned: Int,
        /** Extras flagged never-to-implement; excluded from [extraTotal] but still listed in [extra]. */
        val extraNotPlanned: Int,
        val percent: Double,
        /** Booster (draft) cards, A→Z — including the not-planned ones, each carrying its reason. */
        val draft: List<CardCoverageDTO>,
        /** Completionist extras in Scryfall's section order. Empty if the set has none. */
        val extraGroups: List<ExtraGroupDTO>,
    )

    /** One day on the implementation-progress curve, as baked by `scripts/card-progress-graph`. */
    @Serializable
    data class ProgressPointDTO(val date: String, val added: Int, val total: Int)

    /**
     * Project-wide headline rollup for the Set Completion view, in two flavours:
     *
     * - **distinct** — booster cards deduped by front-face name across the whole catalog, so a
     *   staple reprinted in 15 sets counts once. This is the "how much of Magic do we cover" number.
     * - **printings** — the naive sum of every set's per-set counts, where each reprint counts once
     *   per set it appears in. This is the "how much booster content across all sets" number, and is
     *   what you get by summing the [coverage] rows.
     *
     * The two differ purely by reprint multiplicity; surfacing both stops the printings total from
     * masquerading as a distinct-card count.
     */
    data class CoverageSummaryDTO(
        /** Distinct booster card names (front-face) we've authored anywhere. */
        val distinctImplemented: Int,
        /** Distinct booster card names (front-face) across the whole catalog — the deduped universe. */
        val distinctTotal: Int,
        /** `distinctImplemented / distinctTotal * 100`, one decimal. */
        val distinctPercent: Double,
        /** Distinct completionist-extra card names we've authored, EXCLUDING any that are also booster cards. */
        val extraDistinctImplemented: Int,
        /** Distinct completionist-extra card names (front-face), partitioned away from the booster universe. */
        val extraDistinctTotal: Int,
        /** `extraDistinctImplemented / extraDistinctTotal * 100`, one decimal. */
        val extraDistinctPercent: Double,
        /** Booster-card printings implemented = sum of per-set [SetCoverageDTO.implemented]. */
        val printingsImplemented: Int,
        /** Booster-card printings total = sum of per-set [SetCoverageDTO.total]. */
        val printingsTotal: Int,
        /** `printingsImplemented / printingsTotal * 100`, one decimal. */
        val printingsPercent: Double,
        /** Distinct card names we've decided never to implement — excluded from every total above. */
        val distinctNotPlanned: Int,
        /** Sets at 100% booster coverage. */
        val setsComplete: Int,
        /** Catalogued sets with baked totals. */
        val setCount: Int,
    )

    private val canonical: List<CanonicalSet> =
        ClassPathResource(RESOURCE_PATH).inputStream.bufferedReader().use {
            JSON.decodeFromString<List<CanonicalSet>>(it.readText())
        }
    private val byCode: Map<String, CanonicalSet> = canonical.associateBy { it.code }
    private val limitedProducts: Map<String, Map<String, List<String>>> =
        ClassPathResource(PRODUCTS_PATH).inputStream.bufferedReader().use {
            JSON.decodeFromString(it.readText())
        }

    /**
     * Distinct card names eligible for this set's limited pool according to Scryfall's
     * printing-level `booster` flag. Supplemental products with no booster use their whole card
     * list, matching the coverage fallback; null means the baked catalog has no row for the set.
     */
    fun limitedCardNames(setCode: String): Set<String>? =
        byCode[setCode.uppercase()]?.mainCards?.mapTo(linkedSetOf()) { it.name }

    /** Optional non-booster product buckets keyed by Scryfall `promo_types` id. */
    fun limitedProducts(setCode: String): Map<String, Set<String>> =
        limitedProducts[setCode.uppercase()].orEmpty().mapValues { (_, names) -> names.toSet() }

    private val progress: List<ProgressPointDTO> =
        ClassPathResource(PROGRESS_PATH).inputStream.bufferedReader().use {
            JSON.decodeFromString<List<ProgressPointDTO>>(it.readText())
        }

    /**
     * Coverage for every catalogued set, newest release first (then by code) —
     * mirroring the mtgish dashboard ordering.
     */
    fun coverage(): List<SetCoverageDTO> =
        canonical
            .map { c ->
                val set = MtgSetCatalog.byCode(c.code)
                val authored = authoredNames(set)
                val main = c.mainCards.partition { it.counts(authored) }
                val secondary = c.secondaryCards.partition { it.counts(authored) }
                val implemented = main.first.count { frontFace(it.name) in authored }
                val extraImplemented = secondary.first.count { frontFace(it.name) in authored }
                SetCoverageDTO(
                    code = c.code,
                    name = c.name,
                    releaseDate = c.releaseDate,
                    setType = c.setType,
                    block = set?.block,
                    implemented = implemented,
                    total = main.first.size,
                    extraImplemented = extraImplemented,
                    extraTotal = secondary.first.size,
                    notPlanned = main.second.size,
                    extraNotPlanned = secondary.second.size,
                    percent = percent(implemented, main.first.size),
                    inStandard = c.standardLegal,
                )
            }
            .sortedWith(compareByDescending<SetCoverageDTO> { it.releaseDate ?: "" }.thenBy { it.code })

    /**
     * Project-wide headline rollup: distinct (reprints deduped by front-face name) alongside
     * printings (the naive per-set sum). See [CoverageSummaryDTO]. Powers the Set Completion banner,
     * which previously summed the per-set rows and so reported reprint-inflated printing counts as if
     * they were distinct cards.
     */
    fun summary(): CoverageSummaryDTO {
        // A card is implemented globally once we've authored its name in any set, not per set, so
        // dedup the numerator against the union of every set's authored names.
        val authoredAnywhere =
            canonical.asSequence().flatMap { authoredNames(MtgSetCatalog.byCode(it.code)).asSequence() }.toSet()
        // Distinct booster universe: every front-face main-pool name across the catalog, deduped.
        // Not-planned cards leave the universe entirely — they're excluded by name, so a card
        // dropped from Antiquities' denominator can't sneak back in via its Fourth Edition printing.
        val universe = canonical.asSequence()
            .flatMap { it.mainCards.asSequence() }
            .filter { it.counts(authoredAnywhere) }
            .map { frontFace(it.name) }
            .toSet()
        val distinctImplemented = universe.count { it in authoredAnywhere }
        // Distinct extra universe: completionist exclusives, partitioned away from the booster universe so
        // a card that is a booster card in one set and an extra in another counts only as a booster card —
        // booster + extra distinct never double-count the same name.
        val extraUniverse =
            canonical.asSequence()
                .flatMap { it.secondaryCards.asSequence() }
                .filter { it.counts(authoredAnywhere) }
                .map { frontFace(it.name) }
                .toSet() - universe
        val extraDistinctImplemented = extraUniverse.count { it in authoredAnywhere }
        val distinctNotPlanned = canonical.asSequence()
            .flatMap { it.mainCards.asSequence() + it.secondaryCards.asSequence() }
            .filterNot { it.counts(authoredAnywhere) }
            .map { frontFace(it.name) }
            .toSet()
            .size

        var printingsImplemented = 0
        var printingsTotal = 0
        var setsComplete = 0
        for (c in canonical) {
            val authored = authoredNames(MtgSetCatalog.byCode(c.code))
            val countable = c.mainCards.filter { it.counts(authored) }
            val implemented = countable.count { frontFace(it.name) in authored }
            printingsImplemented += implemented
            printingsTotal += countable.size
            if (percent(implemented, countable.size) >= 100.0) setsComplete++
        }

        return CoverageSummaryDTO(
            distinctImplemented = distinctImplemented,
            distinctTotal = universe.size,
            distinctPercent = percent(distinctImplemented, universe.size),
            extraDistinctImplemented = extraDistinctImplemented,
            extraDistinctTotal = extraUniverse.size,
            extraDistinctPercent = percent(extraDistinctImplemented, extraUniverse.size),
            printingsImplemented = printingsImplemented,
            printingsTotal = printingsTotal,
            printingsPercent = percent(printingsImplemented, printingsTotal),
            distinctNotPlanned = distinctNotPlanned,
            setsComplete = setsComplete,
            setCount = canonical.size,
        )
    }

    /** Full canonical card list for one set, each card marked implemented / missing. Null if unknown. */
    fun detail(code: String): SetDetailDTO? {
        val c = byCode[code.uppercase()] ?: return null
        val set = MtgSetCatalog.byCode(c.code)
        val authored = authoredNames(set)
        // Not-planned cards stay in the list — the detail view shows them with their reason —
        // but a card only counts toward the totals if we actually intend to build it.
        fun mark(cards: List<CanonicalCard>) =
            cards.map { card ->
                val implemented = frontFace(card.name) in authored
                CardCoverageDTO(card.name, implemented, card.img, card.notPlanned.takeIf { !implemented })
            }
        val draft = mark(c.mainCards)
        // The baked extras already arrive sorted into Scryfall's sections, so grouping by label in
        // encounter order reproduces that page's layout without re-deriving any ordering here.
        val extraGroups = c.secondaryCards
            .groupBy { it.group ?: DEFAULT_EXTRA_GROUP }
            .map { (label, cards) ->
                val marked = mark(cards)
                val countable = marked.count { it.notPlanned == null }
                ExtraGroupDTO(
                    label = label,
                    implemented = marked.count { it.implemented },
                    total = countable,
                    notPlanned = marked.size - countable,
                    cards = marked,
                )
            }
        val draftCountable = draft.count { it.notPlanned == null }
        return SetDetailDTO(
            code = c.code,
            name = c.name,
            releaseDate = c.releaseDate,
            block = set?.block,
            implemented = draft.count { it.implemented },
            total = draftCountable,
            extraImplemented = extraGroups.sumOf { it.implemented },
            extraTotal = extraGroups.sumOf { it.total },
            notPlanned = draft.size - draftCountable,
            extraNotPlanned = extraGroups.sumOf { it.notPlanned },
            percent = percent(draft.count { it.implemented }, draftCountable),
            draft = draft,
            extraGroups = extraGroups,
        )
    }

    /**
     * Distinct-implemented-cards-over-time series (one cumulative point per calendar day since
     * the project began), baked from git history by `scripts/card-progress-graph`. Drives the
     * progress chart behind the Set Completion overall-progress element.
     */
    fun progress(): List<ProgressPointDTO> = progress

    /**
     * Names we've authored for a set, matching `scripts/card-status`: every `card(...)`,
     * `basicLand(...)`, and reprint `Printing` row, reduced to front-face names.
     */
    private fun authoredNames(set: MtgSet?): Set<String> =
        set
            ?.let {
                it.cards.asSequence().map { cd -> cd.name } +
                    it.basicLands.asSequence().map { cd -> cd.name } +
                    it.printings.asSequence().map { p -> p.name }
            }
            ?.map(::frontFace)
            ?.toSet()
            ?: emptySet()

    /**
     * Whether this card counts toward a denominator: everything does, except a card flagged
     * never-to-implement that we haven't implemented anyway. Implementing one un-excludes it —
     * the flag only ever removes a card from the "still to do" bucket, never hides real work.
     */
    private fun CanonicalCard.counts(authored: Set<String>): Boolean =
        notPlanned == null || frontFace(name) in authored

    private companion object {
        /** Section heading for extras the generator couldn't attribute to a product. */
        const val DEFAULT_EXTRA_GROUP = "Extras"
        const val RESOURCE_PATH = "coverage/set-totals.json"
        const val PRODUCTS_PATH = "coverage/set-products.json"
        const val PROGRESS_PATH = "coverage/implementation-history.json"
        val JSON = Json { ignoreUnknownKeys = true }

        fun percent(implemented: Int, total: Int): Double =
            if (total == 0) 0.0 else Math.round(implemented * 1000.0 / total) / 10.0

        /** Strip a ` // back` suffix so DFC / adventure names match canonical front-faces. */
        fun frontFace(name: String): String = name.substringBefore(" // ").trim()
    }
}
