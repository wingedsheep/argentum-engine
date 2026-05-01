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
 * REST endpoints supporting the deck picker UI and the standalone deckbuilder:
 *
 * - `GET  /api/decks/cards`     — metadata for every card in the registry (picker stats + deckbuilder grid/search).
 * - `GET  /api/decks/examples`  — built-in example decks the picker offers as starting points.
 * - `POST /api/decks/validate`  — server-authoritative deck validation (count rules, unknown cards, ≥60).
 *
 * The cards endpoint covers both the lightweight picker (which only reads name/cost/types/colors/cmc
 * for stats and validation) and the standalone deckbuilder (which additionally needs oracle text,
 * power/toughness, keywords, and an image URI for the card grid). Optional fields default to null/empty
 * so existing picker callers ignore them transparently.
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
        val collectorNumber: String?,
        val oracleText: String? = null,
        val power: String? = null,
        val toughness: String? = null,
        val imageUri: String? = null,
        val keywords: List<String> = emptyList()
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
        collectorNumber = metadata.collectorNumber,
        oracleText = oracleText.takeIf { it.isNotBlank() },
        power = creatureStats?.power?.toString(),
        toughness = creatureStats?.toughness?.toString(),
        imageUri = metadata.imageUri,
        keywords = keywords.map { it.name }.sorted()
    )

    companion object {
        // Bloomburrow-only tribal decks. Selesnya Rabbits, Rakdos Lizards, Golgari Squirrels,
        // and Simic Frogs are taken from the Bloomburrow Constructed Midweek Magic decklists
        // (https://mtgazone.com/midweek-magic-bloomburrow-constructed/). Boros Mice and Orzhov
        // Bats are hand-built tribal lists restricted to Bloomburrow cards in the registry.
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
                id = "selesnya_rabbits",
                name = "Selesnya Rabbits",
                description = "GW Rabbits from Bloomburrow.",
                cards = mapOf(
                    "Pawpatch Recruit" to 4,
                    "Warren Elder" to 4,
                    "Burrowguard Mentor" to 4,
                    "Valley Questcaller" to 4,
                    "Finneas, Ace Archer" to 4,
                    "Harvestrite Host" to 4,
                    "Warren Warleader" to 4,
                    "Hop to It" to 4,
                    "Carrot Cake" to 4,
                    "Fabled Passage" to 4,
                    "Forest" to 7,
                    "Plains" to 13
                )
            ),
            ExampleDeckDTO(
                id = "rakdos_lizards",
                name = "Rakdos Lizards",
                description = "BR Lizards from Bloomburrow.",
                cards = mapOf(
                    "Iridescent Vinelasher" to 4,
                    "Hired Claw" to 4,
                    "Fireglass Mentor" to 4,
                    "Flamecache Gecko" to 4,
                    "Gev, Scaled Scorch" to 4,
                    "Thought-Stalker Warlock" to 4,
                    "Valley Flamecaller" to 4,
                    "Take Out the Trash" to 4,
                    "Fell" to 4,
                    "Fabled Passage" to 4,
                    "Rockface Village" to 2,
                    "Mountain" to 8,
                    "Swamp" to 10
                )
            ),
            ExampleDeckDTO(
                id = "golgari_squirrels",
                name = "Golgari Squirrels",
                description = "BG Squirrels from Bloomburrow.",
                cards = mapOf(
                    "Bonecache Overseer" to 4,
                    "Vinereap Mentor" to 4,
                    "Bakersbane Duo" to 2,
                    "Thornvault Forager" to 4,
                    "Osteomancer Adept" to 4,
                    "Valley Rotcaller" to 4,
                    "Bushy Bodyguard" to 2,
                    "Curious Forager" to 4,
                    "Camellia, the Seedmiser" to 4,
                    "Fell" to 4,
                    "Fabled Passage" to 4,
                    "Forest" to 10,
                    "Swamp" to 10
                )
            ),
            ExampleDeckDTO(
                id = "simic_frogs",
                name = "Simic Frogs",
                description = "GU Frogs from Bloomburrow.",
                cards = mapOf(
                    "Sunshower Druid" to 4,
                    "Valley Mightcaller" to 4,
                    "Pond Prophet" to 4,
                    "Three Tree Scribe" to 4,
                    "Dour Port-Mage" to 3,
                    "Long River Lurker" to 4,
                    "Clement, the Worrywort" to 3,
                    "Splash Lasher" to 2,
                    "Polliwallop" to 4,
                    "Splash Portal" to 4,
                    "Fabled Passage" to 4,
                    "Forest" to 11,
                    "Island" to 9
                )
            ),
            ExampleDeckDTO(
                id = "orzhov_bats",
                name = "Orzhov Bats",
                description = "WB Bats from Bloomburrow.",
                cards = mapOf(
                    "Essence Channeler" to 4,
                    "Starscape Cleric" to 4,
                    "Lifecreed Duo" to 4,
                    "Moonstone Harbinger" to 4,
                    "Starlit Soothsayer" to 4,
                    "Moonrise Cleric" to 4,
                    "Wax-Wane Witness" to 4,
                    "Zoraline, Cosmos Caller" to 3,
                    "Lunar Convocation" to 4,
                    "Sonar Strike" to 4,
                    "Fabled Passage" to 4,
                    "Uncharted Haven" to 3,
                    "Plains" to 7,
                    "Swamp" to 7
                )
            )
        )
    }
}
