package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.engine.deck.SetArchetypes
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.mtg.sets.MtgSetCatalog
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Set catalog endpoints.
 *
 * - `GET /api/sets`                       — every catalogued set (deckbuilder, search filters).
 * - `GET /api/sets/booster-ready`         — subset draftable for sealed/draft (booster generation).
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
    private val boosterGenerator: BoosterGenerator
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
