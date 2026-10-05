package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.Printing
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Per-printing catalog endpoints for the deckbuilder picker and the saved-deck browser.
 *
 * - `GET /api/cards/{name}/printings`  — every known printing of a single card.
 * - `GET /api/printings?names=A,B,C`   — batched lookup keyed by card name (saved-deck preview).
 *
 * Sits next to [CardsController] (which serves the canonical/default printing per card)
 * and is queried only when a player wants to pin or render a non-default printing. Names
 * with no entries in the [PrintingRegistry] AND no fallback in [CardRegistry] return 404
 * for the singular endpoint, and are simply absent from the batched map. The batch shape
 * (`Map<name, List<PrintingDTO>>`) lets the saved-deck browser resolve every distinct
 * printing referenced by a deck card preview in a single round-trip.
 */
@RestController
class PrintingsController(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
) {

    @GetMapping("/api/cards/{name}/printings")
    fun getPrintingsForCard(@PathVariable name: String): ResponseEntity<List<PrintingDTO>> {
        val printings = resolvePrintings(name)
        return if (printings == null) ResponseEntity.notFound().build()
        else ResponseEntity.ok(printings)
    }

    @GetMapping("/api/printings")
    fun getPrintingsBatch(@RequestParam(name = "names") names: List<String>): Map<String, List<PrintingDTO>> {
        if (names.isEmpty()) return emptyMap()
        // Spring binds `?names=A,B,C` and `?names=A&names=B` both to a List<String>; dedupe so
        // a deck listing `Lightning Bolt` four times only triggers a single lookup.
        return names.toSet()
            .mapNotNull { name -> resolvePrintings(name)?.let { name to it } }
            .toMap()
    }

    /**
     * Resolve every known printing of [name]. Falls back to the default printing synthesised
     * from the card's own [com.wingedsheep.sdk.model.CardDefinition] when the registry has no
     * explicit rows — this keeps the picker functional for cards whose Scryfall ingestion
     * hasn't run yet. Returns `null` when neither registry knows the card.
     */
    private fun resolvePrintings(name: String): List<PrintingDTO>? {
        val fromRegistry = printingRegistry.printingsOf(name)
        if (fromRegistry.isNotEmpty()) {
            return fromRegistry
                .sortedWith(printingOrder)
                .map { it.toDto() }
        }
        // No printing rows yet — synthesise from the CardDefinition's metadata so the picker
        // can still render a single option ("the canonical printing"). 404 only when even that
        // fails.
        val card = cardRegistry.getCard(name) ?: return null
        val ref = card.defaultPrintingRef ?: return null
        val synth = Printing(
            oracleId = card.oracleId ?: card.name,
            name = card.name,
            setCode = ref.setCode,
            collectorNumber = ref.collectorNumber,
            scryfallId = card.metadata.scryfallId,
            artist = card.metadata.artist,
            imageUri = card.metadata.imageUri,
            backFaceImageUri = card.backFace?.metadata?.imageUri,
            releaseDate = card.metadata.releaseDate,
            rarity = card.metadata.rarity,
        )
        return listOf(synth.toDto())
    }

    /**
     * Newest-first by release date — what a deckbuilder picker should show at the top.
     * Printings missing a release date sort last (matches [PrintingRegistry.defaultPrinting]).
     * Within a release date the plain (non-alternate-frame) printing sorts ahead of its
     * showcase/borderless variants, so the set-filter art override picks the normal printing
     * and the picker lists the standard frame first. Final tiebreaker on
     * `(setCode, collectorNumber)` for a stable, deterministic order.
     */
    private val printingOrder: Comparator<Printing> =
        compareBy<Printing> { it.releaseDate == null }
            .thenByDescending { it.releaseDate ?: "" }
            .thenBy { it.isAlternateFrame }
            .thenBy { it.setCode }
            .thenBy { it.collectorNumber }

    private fun Printing.toDto(): PrintingDTO = PrintingDTO(
        setCode = setCode,
        setName = MtgSetCatalog.byCode(setCode)?.displayName,
        collectorNumber = collectorNumber,
        imageUri = imageUri,
        backFaceImageUri = backFaceImageUri,
        rarity = rarity.name,
        artist = artist,
        releaseDate = releaseDate,
        scryfallId = scryfallId,
        isPromo = isPromo,
        isFullArt = isFullArt,
        frameEffects = frameEffects,
        borderColor = borderColor,
        printedName = printedName,
    )

    /**
     * Wire shape for a single printing. `name` is intentionally absent — it lives in the
     * URL path / batch map key, never duplicated in the body. The deckbuilder already knows
     * the name of the card it's asking about.
     */
    data class PrintingDTO(
        val setCode: String,
        val setName: String?,
        val collectorNumber: String,
        val imageUri: String?,
        val backFaceImageUri: String?,
        val rarity: String,
        val artist: String?,
        val releaseDate: String?,
        val scryfallId: String?,
        val isPromo: Boolean,
        val isFullArt: Boolean,
        val frameEffects: List<String>,
        val borderColor: String?,
        val printedName: String? = null,
    )
}
