package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.engine.deck.SetArchetypes
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.coverage.SetCoverageService
import com.wingedsheep.mtg.sets.MtgSetCatalog
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
 * - `GET /api/sets/progress`              — distinct-cards-over-time series (Set Completion chart).
 * - `GET /api/sets/{setCode}/archetypes`  — limited archetype synergies for a single set.
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
                    incomplete = config.incomplete
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

    @GetMapping("/{setCode}/archetypes")
    fun getArchetypes(@PathVariable setCode: String): SetSynergiesDTO? {
        val synergies = SetArchetypes.getForSet(setCode) ?: return null
        return SetSynergiesDTO(
            setCode = synergies.setCode,
            setName = synergies.setName,
            archetypes = synergies.archetypes.map { arch ->
                ArchetypeDTO(
                    name = arch.name,
                    colors = arch.colors.map { it.name.first().uppercase() },
                    description = arch.description,
                    creatureTypes = arch.creatureTypes
                )
            }
        )
    }
}
