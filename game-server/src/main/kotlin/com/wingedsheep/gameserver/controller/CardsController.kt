package com.wingedsheep.gameserver.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.search.ParseError
import com.wingedsheep.search.SearchCard
import com.wingedsheep.search.SearchService
import com.wingedsheep.search.Span
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.PrintingRef
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Card catalog endpoints. Split out of [DecksController] so the URL hierarchy
 * matches the resource: a card list is not "a sub-resource of decks", it's its
 * own thing that the deckbuilder, the lobby picker, future tooling, and the
 * search endpoint all read from.
 *
 * - `GET /api/cards`         — metadata for every implemented card.
 * - `GET /api/cards/search`  — Scryfall-style query language over the catalog.
 *
 * The deckbuilder still loads the full catalog once for instant client-side
 * filtering. The search endpoint exists for non-browser consumers (CLI, tests,
 * future mobile) and shares the [SearchService] grammar with the frontend.
 */
@RestController
@RequestMapping("/api/cards")
class CardsController(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
) {

    @GetMapping
    fun getCards(): List<CardSummaryDTO> =
        cardRegistry.allCardNames()
            .asSequence()
            .filter { it !in TOKEN_NAMES }
            .mapNotNull { cardRegistry.getCard(it) }
            .map { it.toSummary() }
            .sortedBy { it.name }
            .toList()

    /**
     * Scryfall-flavoured search. Returns the catalog filtered by [q] plus any
     * parse errors with character spans so callers can highlight the offending
     * region of the original query. An empty / blank query returns the full
     * catalog with no errors — same shape, never special-cased.
     */
    @GetMapping("/search")
    fun search(@RequestParam(name = "q", required = false, defaultValue = "") q: String): SearchResponse {
        val all = getCards()
        val result = SearchService.parse(q)
        val cards = all.filter(result.predicate)
        return SearchResponse(
            cards = cards,
            errors = result.errors.map { it.toDto() },
        )
    }

    data class SearchResponse(
        val cards: List<CardSummaryDTO>,
        val errors: List<SearchErrorDTO>,
    )

    data class SearchErrorDTO(
        val message: String,
        val span: SpanDTO,
        val suggestion: String?,
    )

    data class SpanDTO(val start: Int, val end: Int)

    private fun ParseError.toDto(): SearchErrorDTO =
        SearchErrorDTO(message, SpanDTO(span.start, span.end), suggestion)

    private fun Span.toDto(): SpanDTO = SpanDTO(start, end)

    /**
     * The flat card projection returned by every catalog endpoint and consumed
     * by the deckbuilder. Implements [SearchCard] so [SearchService] can run
     * directly against the same DTOs we serialise to clients — no extra
     * mapping step on the search hot path.
     */
    data class CardSummaryDTO(
        override val name: String,
        override val manaCost: String,
        override val cmc: Int,
        override val colors: List<String>,
        override val colorIdentity: List<String>,
        override val cardTypes: List<String>,
        override val supertypes: List<String>,
        override val subtypes: List<String>,
        override val basicLand: Boolean,
        override val rarity: String,
        override val setCode: String?,
        override val collectorNumber: String?,
        override val oracleText: String? = null,
        override val power: String? = null,
        override val toughness: String? = null,
        val imageUri: String? = null,
        override val keywords: List<String> = emptyList(),
        override val legalFormats: List<String> = emptyList(),
        // Kotlin generates a getter named `isDoubleFaced()` (no `get` prefix) for `Boolean` props
        // beginning with `is`. Jackson then serialises that as JSON `doubleFaced`, dropping the
        // `is`. The deckbuilder reads `card.isDoubleFaced`, so without this annotation the F-flip
        // hint never triggers because the field is missing under the expected name.
        @get:JsonProperty("isDoubleFaced")
        override val isDoubleFaced: Boolean = false,
        val backFaceName: String? = null,
        val backFaceImageUri: String? = null,
        /**
         * The card's printed layout (e.g. `NORMAL`, `SPLIT`, `ADVENTURE`). Lets the deckbuilder
         * render split cards (Pain // Suffering, Rooms like Unholy Annex // Ritual Chamber) rotated
         * to landscape in the hover preview, since their single catalog image is printed sideways.
         */
        val layout: String = "NORMAL",
        /**
         * The printing the catalog grid renders by default. Lets the deckbuilder picker
         * highlight the row that matches the catalog thumbnail without re-deriving it from
         * `setCode + collectorNumber` on the client. Null only for cards missing both
         * `setCode` and `metadata.collectorNumber`.
         */
        val defaultPrinting: PrintingRef? = null,
        /**
         * Every set this card has a registered printing in (canonical + reprints). Drives
         * the `s:`/`set:` search matcher so a reprint surfaces under its new set code even
         * though its [setCode] still points at the original printing's set.
         */
        override val printingSetCodes: List<String> = emptyList(),
    ) : SearchCard

    private fun CardDefinition.toSummary(): CardSummaryDTO {
        // Prefer the most-recent printing's art and picker target so reprints (e.g. EOE/KTK)
        // surface their newest frame in the catalog grid. The `s:` filter on the frontend
        // still overrides this via setPrintingOverride when the user pins a specific set.
        val latest = printingRegistry.defaultPrinting(name)
        val latestRef = latest?.let { PrintingRef(it.setCode, it.collectorNumber) }
        return CardSummaryDTO(
            name = name,
            manaCost = manaCost.toString(),
            cmc = cmc,
            colors = colors.map { it.name },
            colorIdentity = colorIdentity.map { it.name },
            cardTypes = typeLine.cardTypes.map { it.name },
            supertypes = typeLine.supertypes.map { it.name },
            subtypes = typeLine.subtypes.map { it.toString() },
            basicLand = typeLine.isBasicLand,
            rarity = metadata.rarity.name,
            setCode = setCode,
            collectorNumber = metadata.collectorNumber,
            oracleText = oracleText.takeIf { it.isNotBlank() },
            power = creatureStats?.power?.toString(),
            toughness = creatureStats?.toughness?.toString(),
            imageUri = latest?.imageUri ?: metadata.imageUri,
            keywords = keywords.map { it.name }.sorted(),
            legalFormats = legalFormats.map { it.name }.sorted(),
            isDoubleFaced = isDoubleFaced,
            backFaceName = backFace?.name,
            backFaceImageUri = latest?.backFaceImageUri ?: backFace?.metadata?.imageUri,
            layout = layout.name,
            defaultPrinting = latestRef ?: defaultPrintingRef,
            printingSetCodes = printingRegistry.printingsOf(name)
                .map { it.setCode }
                .distinct(),
        )
    }

    companion object {
        // Predefined tokens are registered in the CardRegistry so the engine can resolve
        // token abilities by name (e.g., a created Treasure → its mana ability), but they
        // are not real cards and must not appear in the deckbuilder catalog.
        private val TOKEN_NAMES: Set<String> =
            PredefinedTokens.allTokens.flatMap { listOfNotNull(it.name, it.backFace?.name) }.toSet()
    }
}
