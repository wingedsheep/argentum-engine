package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.sealed.BoosterGenerator
import org.springframework.web.bind.annotation.GetMapping
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
}
