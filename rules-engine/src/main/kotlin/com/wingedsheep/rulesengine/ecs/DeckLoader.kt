package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ability.CardScript
import com.wingedsheep.rulesengine.ability.CardScriptRepository
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.ecs.components.AbilitiesComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.sets.CardRegistry

/**
 * Service for loading decks into ECS game state.
 *
 * Translates deck lists (card name to quantity mappings) into ECS entities
 * with proper components baked in, following the ECS purity philosophy.
 *
 * @property cardRegistries List of card registries to search for card definitions
 * @property scriptRepository Optional repository for card scripts (abilities)
 */
class DeckLoader(
    private val cardRegistries: List<CardRegistry>,
    private val scriptRepository: CardScriptRepository? = null
) {
    /**
     * Result of a deck load operation.
     */
    sealed interface DeckLoadResult {
        /**
         * Deck loaded successfully.
         * @property state The updated game state with cards added
         * @property cardIds The IDs of all cards that were created
         */
        data class Success(
            val state: EcsGameState,
            val cardIds: List<EntityId>
        ) : DeckLoadResult

        /**
         * Deck loading failed.
         * @property error Description of the error
         * @property missingCards List of card names that couldn't be found
         */
        data class Failure(
            val error: String,
            val missingCards: List<String> = emptyList()
        ) : DeckLoadResult
    }

    /**
     * Load a deck for a player.
     *
     * Creates entity for each card in the deck, adds AbilitiesComponent
     * where applicable, and places all cards in the player's library zone.
     *
     * @param state The current game state
     * @param playerId The player who owns this deck
     * @param deckList Map of card names to quantities (e.g., "Forest" to 20)
     * @return DeckLoadResult with the new state and card IDs, or failure info
     */
    fun loadDeck(
        state: EcsGameState,
        playerId: EntityId,
        deckList: Map<String, Int>
    ): DeckLoadResult {
        // Validate all cards exist before creating any
        val missingCards = mutableListOf<String>()
        for (cardName in deckList.keys) {
            if (findCardDefinition(cardName) == null) {
                missingCards.add(cardName)
            }
        }

        if (missingCards.isNotEmpty()) {
            return DeckLoadResult.Failure(
                error = "Cards not found in any registry: ${missingCards.joinToString(", ")}",
                missingCards = missingCards
            )
        }

        // Create all card entities
        var currentState = state
        val createdCardIds = mutableListOf<EntityId>()
        val libraryZone = ZoneId.library(playerId)

        for ((cardName, quantity) in deckList) {
            val definition = findCardDefinition(cardName)!!
            val script = findCardScript(cardName)

            repeat(quantity) {
                val (cardId, newState) = createCardEntity(
                    currentState,
                    definition,
                    script,
                    playerId
                )
                currentState = newState.addToZone(cardId, libraryZone)
                createdCardIds.add(cardId)
            }
        }

        return DeckLoadResult.Success(
            state = currentState,
            cardIds = createdCardIds
        )
    }

    /**
     * Load multiple decks for multiple players.
     *
     * Convenience method for loading all player decks at once.
     *
     * @param state The current game state
     * @param decks Map of player IDs to their deck lists
     * @return DeckLoadResult with the new state, or failure info
     */
    fun loadDecks(
        state: EcsGameState,
        decks: Map<EntityId, Map<String, Int>>
    ): DeckLoadResult {
        var currentState = state
        val allCardIds = mutableListOf<EntityId>()

        for ((playerId, deckList) in decks) {
            val result = loadDeck(currentState, playerId, deckList)
            when (result) {
                is DeckLoadResult.Success -> {
                    currentState = result.state
                    allCardIds.addAll(result.cardIds)
                }
                is DeckLoadResult.Failure -> {
                    return result  // Fail fast
                }
            }
        }

        return DeckLoadResult.Success(
            state = currentState,
            cardIds = allCardIds
        )
    }

    /**
     * Find a card definition across all registries.
     */
    private fun findCardDefinition(cardName: String): CardDefinition? {
        for (registry in cardRegistries) {
            val definition = registry.getCardDefinition(cardName)
            if (definition != null) return definition
        }
        return null
    }

    /**
     * Find a card script across all registries and the optional repository.
     */
    private fun findCardScript(cardName: String): CardScript? {
        // Check script repository first
        scriptRepository?.getScript(cardName)?.let { return it }

        // Then check registries
        for (registry in cardRegistries) {
            val script = registry.getCardScript(cardName)
            if (script != null) return script
        }
        return null
    }

    /**
     * Create a card entity with all appropriate components.
     */
    private fun createCardEntity(
        state: EcsGameState,
        definition: CardDefinition,
        script: CardScript?,
        ownerId: EntityId
    ): Pair<EntityId, EcsGameState> {
        val cardId = EntityId.generate()

        // Base components every card has
        val cardComponent = CardComponent(definition, ownerId)
        val controllerComponent = ControllerComponent(ownerId)

        // Build abilities component if the card has abilities
        val abilitiesComponent = script?.let { buildAbilitiesComponent(it) }

        // Create the entity with components
        val components = buildList {
            add(cardComponent)
            add(controllerComponent)
            if (abilitiesComponent != null && abilitiesComponent != AbilitiesComponent.EMPTY) {
                add(abilitiesComponent)
            }
        }

        val (_, newState) = state.createEntity(cardId, *components.toTypedArray())
        return cardId to newState
    }

    /**
     * Build an AbilitiesComponent from a CardScript.
     */
    private fun buildAbilitiesComponent(script: CardScript): AbilitiesComponent {
        if (script.activatedAbilities.isEmpty() &&
            script.triggeredAbilities.isEmpty() &&
            script.staticAbilities.isEmpty()
        ) {
            return AbilitiesComponent.EMPTY
        }

        return AbilitiesComponent(
            activatedAbilities = script.activatedAbilities,
            triggeredAbilities = script.triggeredAbilities,
            staticAbilities = script.staticAbilities
        )
    }

    companion object {
        /**
         * Create a DeckLoader with the given registries.
         */
        fun create(vararg registries: CardRegistry): DeckLoader =
            DeckLoader(registries.toList())

        /**
         * Create a DeckLoader with registries and a script repository.
         */
        fun create(
            scriptRepository: CardScriptRepository,
            vararg registries: CardRegistry
        ): DeckLoader = DeckLoader(registries.toList(), scriptRepository)
    }
}
