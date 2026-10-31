package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.engine.deck.SetArchetypes
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.coverage.AiReadingService
import com.wingedsheep.gameserver.coverage.SetCoverageService
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Set catalog endpoints.
 *
 * - `GET /api/sets`                       — every catalogued set (deckbuilder, search filters).
 * - `GET /api/sets/booster-ready`         — subset draftable for sealed/draft (booster generation).
 * - `GET /api/sets/coverage`              — per-set card-implementation coverage (Set Completion view).
 * - `GET /api/sets/coverage/summary`      — project-wide distinct + printing rollup (Set Completion banner).
 * - `GET /api/sets/coverage/ai-reading`   — per-set AI-reading score: authored cards the engine AI reads fully.
 * - `GET /api/sets/{code}/ai-reading`     — one set's unread cards, grouped by the effect type the AI misses.
 * - `GET /api/sets/progress`              — distinct-cards-over-time series (Set Completion chart).
 * - `GET /api/sets/{setCode}/archetypes`  — limited archetype synergies for a single set.
 * - `GET /api/sets/{setCode}/cube-cards`  — a set's implemented cards with their rarity in it, for
 *   building a cube from a set (the cube editor's "Add a set").
 *
 * The bare `/api/sets` used to mean "draftable" — that intent now lives at the
 * `booster-ready` path so the bare resource matches the deckbuilder's
 * "every implemented set" expectation. No frontend or backend consumer
 * referenced the old bare path; the AI/limited code reaches into
 * `BoosterGenerator.availableSets` directly.
 */
@RestController
@RequestMapping("/api/sets")
class SetsController(
    private val boosterGenerator: BoosterGenerator,
    private val setCoverageService: SetCoverageService,
    private val aiReadingService: AiReadingService,
) {

    /**
     * Lean set entry — `code`, display `name`, and ISO `releaseDate` (nullable).
     * Used by the deckbuilder filters to sort by name/date and surface the year.
     */
    data class SetEntryDTO(val code: String, val name: String, val releaseDate: String?)

    data class BoosterSetDTO(
        val setCode: String,
        val setName: String,
        val implementedCount: Int,
        val incomplete: Boolean,
        val releaseDate: String? = null,
        val extensionSet: Boolean = false,
    )

    data class ArchetypeDTO(
        val name: String,
        val colors: List<String>,
        val description: String,
        val creatureTypes: List<String>
    )

    data class SetSynergiesDTO(
        val setCode: String,
        val setName: String,
        val archetypes: List<ArchetypeDTO>
    )

    @GetMapping
    fun getCatalogSets(): List<SetEntryDTO> =
        MtgSetCatalog.all.map { SetEntryDTO(it.code, it.displayName, it.releaseDate) }

    @GetMapping("/booster-ready")
    fun getBoosterReadySets(): List<BoosterSetDTO> =
        boosterGenerator.availableSets.values
            .filter { it.fullyImplemented }
            .map { config ->
                BoosterSetDTO(
                    setCode = config.setCode,
                    setName = config.setName,
                    implementedCount = config.distinctCardCount,
                    incomplete = config.incomplete,
                    releaseDate = config.releaseDate,
                    extensionSet = config.extensionSet,
                )
            }

    /**
     * Per-set card-implementation coverage (implemented / canonical total + %),
     * newest release first. Powers the Set Completion view.
     */
    @GetMapping("/coverage")
    fun getCoverage(): List<SetCoverageService.SetCoverageDTO> = setCoverageService.coverage()

    /**
     * Project-wide coverage rollup for the Set Completion banner: distinct booster cards (reprints
     * deduped by name) alongside the printing sum of the per-set rows. Summing `/coverage` yourself
     * gives the printing figures; this also gives the distinct ones.
     */
    @GetMapping("/coverage/summary")
    fun getCoverageSummary(): SetCoverageService.CoverageSummaryDTO = setCoverageService.summary()

    /**
     * Per set, how many of its authored cards the engine AI's static readers read fully (the AI
     * card-reading audit). Its own route rather than a field of `/coverage` because it is computed
     * lazily in the background: the first request starts the index and answers `ready = false`, and
     * the Set Completion view polls until it is ready instead of waiting on it.
     */
    @GetMapping("/coverage/ai-reading")
    fun getAiReading(): AiReadingService.SetSummary = aiReadingService.setSummary()

    /**
     * One set's AI-reading detail: its unread cards, grouped by the effect type the AI misses,
     * biggest group first. Same lazy index as [getAiReading]; 404 for an uncatalogued set.
     */
    @GetMapping("/{setCode}/ai-reading")
    fun getSetAiReading(@PathVariable setCode: String): AiReadingService.SetDetail =
        aiReadingService.setDetail(setCode.uppercase())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No set $setCode")

    /**
     * One set's full canonical card list, each card marked implemented / missing
     * (booster cards + completionist extras). Drives the set detail view. 404 if the
     * code isn't a catalogued set with baked totals.
     */
    @GetMapping("/{setCode}/coverage")
    fun getSetCoverageDetail(@PathVariable setCode: String): SetCoverageService.SetDetailDTO =
        setCoverageService.detail(setCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No coverage data for set $setCode")

    /**
     * Distinct-implemented-cards-over-time series (one cumulative point per day since the project
     * began). Drives the progress chart behind the Set Completion overall-progress element.
     */
    @GetMapping("/progress")
    fun getProgress(): List<SetCoverageService.ProgressPointDTO> = setCoverageService.progress()

    /**
     * One implemented card of a set, as the cube editor imports it. [rarity] is the card's rarity *in
     * this set* (a reprint's canonical rarity is its first printing's); [reprint] is true when the card
     * was first printed elsewhere, which is how "Bloomburrow Commander's new cards" is told apart from
     * the staples its decks reprint; [product] is null for the set's booster pool and names the
     * optional product otherwise.
     */
    data class CubeSetCardDTO(
        val name: String,
        val rarity: String,
        val reprint: Boolean,
        val product: String?,
    )

    data class CubeSetDTO(
        val code: String,
        val name: String,
        val releaseDate: String?,
        val cards: List<CubeSetCardDTO>,
    )

    @GetMapping("/{setCode}/cube-cards")
    fun getCubeCards(@PathVariable setCode: String): CubeSetDTO {
        val code = setCode.uppercase()
        val config = boosterGenerator.getSetConfig(code)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No card pool for set $setCode")
        val ownNames = MtgSetCatalog.byCode(code)?.cards?.mapTo(hashSetOf()) { it.name }.orEmpty()
        fun dto(card: CardDefinition, product: String?) = CubeSetCardDTO(
            name = card.name,
            rarity = card.metadata.rarity.name,
            reprint = card.name !in ownNames,
            product = product,
        )
        val boosterCards = config.cards.map { dto(it, product = null) }
        val boosterNames = boosterCards.mapTo(hashSetOf()) { it.name }
        // A card can sit in several optional products; list it once, under the first.
        val productCards = config.extraCardsByProduct.entries
            .flatMap { (product, cards) -> cards.map { dto(it, product) } }
            .filter { it.name !in boosterNames }
            .distinctBy { it.name }
        return CubeSetDTO(
            code = config.setCode,
            name = config.setName,
            releaseDate = config.releaseDate,
            cards = boosterCards + productCards,
        )
    }

    @GetMapping("/{setCode}/archetypes")
    fun getArchetypes(@PathVariable setCode: String): SetSynergiesDTO? {
        val synergies = SetArchetypes.getForSet(setCode) ?: return null
        return SetSynergiesDTO(
            setCode = synergies.setCode,
            setName = synergies.setName,
            archetypes = synergies.archetypes.map { arch ->
                ArchetypeDTO(
                    name = arch.name,
                    colors = arch.colors.map { it.symbol.toString() },
                    description = arch.description,
                    creatureTypes = arch.creatureTypes
                )
            }
        )
    }
}
