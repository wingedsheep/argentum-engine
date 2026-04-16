package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.engine.deck.SetArchetypes
import com.wingedsheep.engine.limited.BoosterGenerator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sets")
class SetsController(
    private val boosterGenerator: BoosterGenerator
) {

    data class SetInfoDTO(
        val setCode: String,
        val setName: String,
        val implementedCount: Int,
        val totalCount: Int?,
        val incomplete: Boolean
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
    fun getSets(): List<SetInfoDTO> =
        boosterGenerator.availableSets.values
            .filter { it.incomplete }
            .map { config ->
                SetInfoDTO(
                    setCode = config.setCode,
                    setName = config.setName,
                    implementedCount = config.cards.size,
                    totalCount = config.totalSetSize,
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
