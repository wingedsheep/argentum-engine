package com.wingedsheep.gameserver.coverage

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.legality.LegalityData
import com.wingedsheep.sdk.core.DeckFormat
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
 * committed `coverage/set-totals.json` resource, split into **draft** (booster-relevant,
 * Scryfall `booster: true`) and **extra** (completionist exclusives), same partitioning
 * as `card-status` so the numbers match the mtgish coverage TUI.
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
 */
@Service
class SetCoverageService {

    /** One canonical card with its set-specific Scryfall art, as baked by `scripts/gen-set-totals`. */
    @Serializable
    private data class CanonicalCard(val name: String, val img: String? = null)

    /** One catalogued set's canonical card universe, as baked by `scripts/gen-set-totals`. */
    @Serializable
    private data class CanonicalSet(
        val code: String,
        val name: String,
        val releaseDate: String? = null,
        val setType: String? = null,
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
        /** Booster (draft) canonical card count — the headline denominator. */
        val total: Int,
        /** Completionist extras we've authored (starter exclusives, bonus sheets, Special Guests). */
        val extraImplemented: Int,
        /** Completionist extra canonical card count. */
        val extraTotal: Int,
        /** `implemented / total * 100` (booster cards), one decimal. `0.0` when [total] is `0`. */
        val percent: Double,
        /** Whether this set is currently legal in the Standard format. See [isInStandard]. */
        val inStandard: Boolean,
    )

    /** One canonical card and whether we've implemented it — for the per-set detail view. */
    data class CardCoverageDTO(
        val name: String,
        val implemented: Boolean,
        /** Set-specific Scryfall art (direct CDN URL, normal size); null if Scryfall had none. */
        val imageUri: String?,
    )

    /** A single set's full canonical card list, split into booster + extra, each marked. */
    data class SetDetailDTO(
        val code: String,
        val name: String,
        val releaseDate: String?,
        val block: String?,
        val implemented: Int,
        val total: Int,
        val extraImplemented: Int,
        val extraTotal: Int,
        val percent: Double,
        /** Booster (draft) cards, A→Z. */
        val draft: List<CardCoverageDTO>,
        /** Completionist extras, A→Z. Empty if the set has none. */
        val extra: List<CardCoverageDTO>,
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
                val implemented = c.mainCards.count { frontFace(it.name) in authored }
                val extraImplemented = c.secondaryCards.count { frontFace(it.name) in authored }
                SetCoverageDTO(
                    code = c.code,
                    name = c.name,
                    releaseDate = c.releaseDate,
                    setType = c.setType,
                    block = set?.block,
                    implemented = implemented,
                    total = c.mainCards.size,
                    extraImplemented = extraImplemented,
                    extraTotal = c.secondaryCards.size,
                    percent = percent(implemented, c.mainCards.size),
                    inStandard = isInStandard(c),
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
        val universe = canonical.asSequence().flatMap { it.mainCards.asSequence() }.map { frontFace(it.name) }.toSet()
        val distinctImplemented = universe.count { it in authoredAnywhere }
        // Distinct extra universe: completionist exclusives, partitioned away from the booster universe so
        // a card that is a booster card in one set and an extra in another counts only as a booster card —
        // booster + extra distinct never double-count the same name.
        val extraUniverse =
            canonical.asSequence().flatMap { it.secondaryCards.asSequence() }.map { frontFace(it.name) }.toSet() -
                universe
        val extraDistinctImplemented = extraUniverse.count { it in authoredAnywhere }

        var printingsImplemented = 0
        var printingsTotal = 0
        var setsComplete = 0
        for (c in canonical) {
            val authored = authoredNames(MtgSetCatalog.byCode(c.code))
            val implemented = c.mainCards.count { frontFace(it.name) in authored }
            printingsImplemented += implemented
            printingsTotal += c.mainCards.size
            if (percent(implemented, c.mainCards.size) >= 100.0) setsComplete++
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
            setsComplete = setsComplete,
            setCount = canonical.size,
        )
    }

    /** Full canonical card list for one set, each card marked implemented / missing. Null if unknown. */
    fun detail(code: String): SetDetailDTO? {
        val c = byCode[code.uppercase()] ?: return null
        val set = MtgSetCatalog.byCode(c.code)
        val authored = authoredNames(set)
        fun mark(cards: List<CanonicalCard>) =
            cards.map { CardCoverageDTO(it.name, frontFace(it.name) in authored, it.img) }
        val draft = mark(c.mainCards)
        val extra = mark(c.secondaryCards)
        return SetDetailDTO(
            code = c.code,
            name = c.name,
            releaseDate = c.releaseDate,
            block = set?.block,
            implemented = draft.count { it.implemented },
            total = draft.size,
            extraImplemented = extra.count { it.implemented },
            extraTotal = extra.size,
            percent = percent(draft.count { it.implemented }, draft.size),
            draft = draft,
            extra = extra,
        )
    }

    /**
     * Distinct-implemented-cards-over-time series (one cumulative point per calendar day since
     * the project began), baked from git history by `scripts/card-progress-graph`. Drives the
     * progress chart behind the Set Completion overall-progress element.
     */
    fun progress(): List<ProgressPointDTO> = progress

    /**
     * Whether a set is currently legal in Standard.
     *
     * Scryfall exposes no per-set Standard flag (its set object carries no legality at all), and
     * WotC only publishes rotation as prose announcements — so there is no authoritative per-set
     * source to read. Legality is fundamentally per-*card*, and we already mirror Scryfall's
     * per-card legalities in [LegalityData] (the same data the deckbuilder's format filter uses).
     *
     * We derive set membership from that. The catch: [LegalityData] unions a card's legality across
     * *every* printing by name, so an old set keeps a handful of staples flagged Standard-legal only
     * because they were reprinted into a current set — "any card is Standard-legal" would wrongly
     * light up half the back catalog. A set is genuinely *in* Standard only when the bulk of its own
     * cards are Standard-legal, so we require a majority of the booster (main) pool. The split is
     * sharply bimodal (a current set is ~100%, a rotated/old set is a few %), so the threshold is
     * not sensitive. Best of all it auto-tracks rotation as the synced legality data changes — no
     * second hand-maintained list of Standard set codes to keep in sync.
     */
    private fun isInStandard(c: CanonicalSet): Boolean {
        val main = c.mainCards
        if (main.isEmpty()) return false
        val legal = main.count { DeckFormat.STANDARD in legalityFor(it.name) }
        return legal * 2 >= main.size
    }

    /** Per-card legality, tolerating DFC names by falling back to the front face. */
    private fun legalityFor(name: String): Set<DeckFormat> =
        LegalityData.forCard(name).ifEmpty { LegalityData.forCard(frontFace(name)) }

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

    private companion object {
        const val RESOURCE_PATH = "coverage/set-totals.json"
        const val PROGRESS_PATH = "coverage/implementation-history.json"
        val JSON = Json { ignoreUnknownKeys = true }

        fun percent(implemented: Int, total: Int): Double =
            if (total == 0) 0.0 else Math.round(implemented * 1000.0 / total) / 10.0

        /** Strip a ` // back` suffix so DFC / adventure names match canonical front-faces. */
        fun frontFace(name: String): String = name.substringBefore(" // ").trim()
    }
}
