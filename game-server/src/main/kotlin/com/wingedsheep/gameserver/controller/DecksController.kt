package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.deck.DeckValidationResult
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.sdk.model.CardDefinition
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoints supporting the deck picker UI:
 *
 * - `GET  /api/decks/cards`     — slim metadata for every card in the registry (for picker stats + autocomplete).
 * - `GET  /api/decks/examples`  — built-in example decks the picker offers as starting points.
 * - `POST /api/decks/validate`  — server-authoritative deck validation (count rules, unknown cards, ≥60).
 *
 * The cards endpoint is intentionally minimal — clients build the deck picker UI from this without
 * needing oracle text, abilities, or images. Image lookup happens via the existing card-image flow.
 */
@RestController
@RequestMapping("/api/decks")
class DecksController(
    private val cardRegistry: CardRegistry,
    private val deckValidator: DeckValidator
) {

    data class CardSummaryDTO(
        val name: String,
        val manaCost: String,
        val cmc: Int,
        val colors: List<String>,
        val cardTypes: List<String>,
        val supertypes: List<String>,
        val subtypes: List<String>,
        val basicLand: Boolean,
        val rarity: String,
        val setCode: String?,
        val collectorNumber: String?
    )

    data class ExampleDeckDTO(
        val id: String,
        val name: String,
        val description: String,
        val cards: Map<String, Int>
    )

    data class ValidateRequest(
        val deckList: Map<String, Int>
    )

    @GetMapping("/cards")
    fun getCards(): List<CardSummaryDTO> =
        cardRegistry.allCardNames()
            .mapNotNull { cardRegistry.getCard(it) }
            .map { it.toSummary() }
            .sortedBy { it.name }

    @GetMapping("/examples")
    fun getExamples(): List<ExampleDeckDTO> = EXAMPLE_DECKS

    @PostMapping("/validate")
    fun validate(@RequestBody request: ValidateRequest): DeckValidationResult =
        deckValidator.validate(request.deckList)

    private fun CardDefinition.toSummary(): CardSummaryDTO = CardSummaryDTO(
        name = name,
        manaCost = manaCost.toString(),
        cmc = cmc,
        colors = colors.map { it.name },
        cardTypes = typeLine.cardTypes.map { it.name },
        supertypes = typeLine.supertypes.map { it.name },
        subtypes = typeLine.subtypes.map { it.toString() },
        basicLand = typeLine.isBasicLand,
        rarity = metadata.rarity.name,
        setCode = setCode,
        collectorNumber = metadata.collectorNumber
    )

    companion object {
        // Four 60-card Bloomburrow starter decks. Each list respects the 4-of-non-basic rule
        // and is sized to a friendly 60 cards exactly.
        private val EXAMPLE_DECKS = listOf(
            ExampleDeckDTO(
                id = "boros_mice",
                name = "Boros Mice",
                description = "RW Mice aggro from Bloomburrow.",
                cards = mapOf(
                    "Heartfire Hero" to 4,
                    "Flowerfoot Swordmaster" to 4,
                    "Emberheart Challenger" to 4,
                    "Manifold Mouse" to 4,
                    "Whiskervale Forerunner" to 4,
                    "Seedglaive Mentor" to 4,
                    "Might of the Meek" to 4,
                    "Crumb and Get It" to 4,
                    "Rabid Gnaw" to 4,
                    "Lupinflower Village" to 4,
                    "Rockface Village" to 4,
                    "Mountain" to 8,
                    "Plains" to 8
                )
            ),
            ExampleDeckDTO(
                id = "rakdos_lizards",
                name = "Rakdos Lizards",
                description = "BR Lizards from Bloomburrow.",
                cards = mapOf(
                    "Hired Claw" to 4,
                    "Fireglass Mentor" to 4,
                    "Flamecache Gecko" to 4,
                    "Iridescent Vinelasher" to 4,
                    "Thought-Stalker Warlock" to 4,
                    "Gev, Scaled Scorch" to 4,
                    "Valley Flamecaller" to 4,
                    "Scales of Shale" to 4,
                    "Savor" to 4,
                    "Mudflat Village" to 4,
                    "Rockface Village" to 4,
                    "Swamp" to 8,
                    "Mountain" to 8
                )
            ),
            ExampleDeckDTO(
                id = "golgari_squirrels",
                name = "Golgari Squirrels",
                description = "BG Squirrels from Bloomburrow.",
                cards = mapOf(
                    "Valley Rotcaller" to 4,
                    "Honored Dreyleader" to 4,
                    "Bonecache Necromancer" to 4,
                    "Vinereap Mentor" to 4,
                    "Camellia, the Seedmiser" to 4,
                    "Hazel's Nocturne" to 4,
                    "For the Common Good" to 4,
                    "Cache Grab" to 4,
                    "Feed the Cycle" to 4,
                    "Mudflat Village" to 4,
                    "Oakhollow Village" to 4,
                    "Swamp" to 8,
                    "Forest" to 8
                )
            ),
            ExampleDeckDTO(
                id = "simic_frogs",
                name = "Simic Frogs",
                description = "GU Frogs from Bloomburrow.",
                cards = mapOf(
                    "Valley Mightcaller" to 4,
                    "Sunshower Druid" to 4,
                    "Pond Prophet" to 4,
                    "Stickytongue Sentinel" to 4,
                    "Diresight Angler" to 4,
                    "Clement, the Worrywort" to 4,
                    "Polliwallop" to 4,
                    "Into the Flood Maw" to 4,
                    "Run Away Together" to 4,
                    "Lilypad Village" to 4,
                    "Oakhollow Village" to 4,
                    "Forest" to 8,
                    "Island" to 8
                )
            )
        )
    }
}
