package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicLong

private val logger = LoggerFactory.getLogger(DevScenarioController::class.java)

/**
 * Development-only REST controller for creating test scenarios.
 *
 * This allows manual testing of the UI against specific game states
 * without needing to play through an entire game to reach that state.
 *
 * **WARNING:** This endpoint should NEVER be enabled in production.
 * Enable with: game.dev-endpoints.enabled=true
 */
@RestController
@RequestMapping("/api/dev/scenarios")
@ConditionalOnProperty(name = ["game.dev-endpoints.enabled"], havingValue = "true")
@Tag(name = "Dev Scenarios", description = "Development-only endpoints for creating test game scenarios")
class DevScenarioController(
    private val cardRegistry: CardRegistry,
    private val gameRepository: GameRepository,
    private val sessionRegistry: SessionRegistry
) {
    private val stateTransformer = ClientStateTransformer(cardRegistry)

    /**
     * Create a new game session with a pre-configured scenario.
     *
     * POST /api/dev/scenarios
     *
     * After creating the scenario, connect to the WebSocket at /game
     * and send a Connect message with the token returned in the response.
     */
    @PostMapping
    @Operation(
        summary = "Create a test scenario",
        description = """
            Creates a new game session with a pre-configured board state.

            After creating the scenario:
            1. Copy the token for the player you want to play as
            2. Open the web client
            3. Connect via WebSocket using the token

            The game will be in the specified phase with all cards already in place.
        """,
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = [Content(
                mediaType = "application/json",
                examples = [
                    ExampleObject(
                        name = "Butcher Orgg - Divide Combat Damage",
                        summary = "Divide 6 combat damage among defender and creatures",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": [],
    "battlefield": [
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Butcher Orgg"}
    ],
    "library": ["Mountain", "Mountain"]
  },
  "player2": {
    "lifeTotal": 20,
    "hand": [],
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Grizzly Bears"},
      {"name": "Hill Giant"},
      {"name": "Goblin Bully"}
    ],
    "library": ["Swamp", "Swamp"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Combat Tricks - Giant Growth & Terror",
                        summary = "Giant Growth pump vs Terror removal",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Giant Growth", "Lightning Bolt"],
    "battlefield": [
      {"name": "Forest"},
      {"name": "Mountain"},
      {"name": "Grizzly Bears"}
    ],
    "library": ["Island", "Plains", "Swamp"]
  },
  "player2": {
    "lifeTotal": 18,
    "hand": ["Terror"],
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"}
    ]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Forked Lightning - Divided Damage",
                        summary = "Test damage division among 1-3 creatures",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Forked Lightning"],
    "battlefield": [
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Mountain"}
    ],
    "library": ["Mountain", "Mountain"]
  },
  "player2": {
    "lifeTotal": 20,
    "hand": [],
    "battlefield": [
      {"name": "Raging Goblin"},
      {"name": "Grizzly Bears"},
      {"name": "Hill Giant"}
    ],
    "library": ["Swamp", "Swamp"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Wrath of God - Board Wipe Dilemma",
                        summary = "Clear the board or attack with your army?",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 12,
    "hand": ["Wrath of God"],
    "battlefield": [
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Grizzly Bears"},
      {"name": "Devoted Hero"}
    ],
    "library": ["Plains", "Plains"]
  },
  "player2": {
    "lifeTotal": 20,
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Arrogant Vampire"},
      {"name": "Dread Reaper"},
      {"name": "Feral Shadow"}
    ],
    "library": ["Swamp", "Swamp"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Astral Slide - Cycling Synergy",
                        summary = "Cycle cards to exile and return creatures",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Barkhide Mauler", "Daru Lancer", "Aura Extraction"],
    "battlefield": [
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Forest"},
      {"name": "Forest"},
      {"name": "Astral Slide"},
      {"name": "Grizzly Bears"}
    ],
    "library": ["Plains", "Forest", "Plains"]
  },
  "player2": {
    "lifeTotal": 20,
    "battlefield": [
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Hill Giant"},
      {"name": "Raging Goblin"}
    ],
    "library": ["Mountain", "Mountain"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Gravedigger - Recursion",
                        summary = "Recover creatures from the graveyard",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 14,
    "hand": ["Gravedigger", "Raise Dead"],
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Bog Wraith"}
    ],
    "graveyard": ["Arrogant Vampire", "Feral Shadow", "Dread Reaper"],
    "library": ["Swamp", "Swamp"]
  },
  "player2": {
    "lifeTotal": 20,
    "battlefield": [
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Plains"},
      {"name": "Ardent Militia"},
      {"name": "Wall of Swords"}
    ],
    "library": ["Plains", "Plains"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Tribal Elves - Wellwisher & Symbiotic Elf",
                        summary = "Elf tribal lifegain and token generation vs Infest",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Symbiotic Elf", "Elvish Vanguard"],
    "battlefield": [
      {"name": "Forest"},
      {"name": "Forest"},
      {"name": "Forest"},
      {"name": "Forest"},
      {"name": "Wellwisher"},
      {"name": "Wirewood Elf"},
      {"name": "Elvish Warrior"}
    ],
    "library": ["Forest", "Forest", "Forest"]
  },
  "player2": {
    "lifeTotal": 20,
    "hand": ["Infest"],
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Severed Legion"},
      {"name": "Nantuko Husk"}
    ],
    "library": ["Swamp", "Swamp"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Arcanis the Omnipotent - Card Advantage Engine",
                        summary = "Tap to draw 3 or bounce to dodge removal",
                        value = """
{
  "player1Name": "Alice",
  "player2Name": "Bob",
  "player1": {
    "lifeTotal": 20,
    "hand": ["Mystic Denial"],
    "battlefield": [
      {"name": "Island"},
      {"name": "Island"},
      {"name": "Island"},
      {"name": "Island"},
      {"name": "Island"},
      {"name": "Island"},
      {"name": "Arcanis the Omnipotent"},
      {"name": "Phantom Warrior"}
    ],
    "library": ["Island", "Wind Drake", "Cloud Spirit", "Man-o'-War", "Time Ebb", "Island"]
  },
  "player2": {
    "lifeTotal": 15,
    "hand": ["Hand of Death", "Volcanic Hammer"],
    "battlefield": [
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Swamp"},
      {"name": "Mountain"},
      {"name": "Mountain"},
      {"name": "Hulking Cyclops"},
      {"name": "Raging Minotaur"}
    ],
    "library": ["Swamp", "Mountain"]
  },
  "phase": "PRECOMBAT_MAIN",
  "activePlayer": 1
}
                        """
                    ),
                    ExampleObject(
                        name = "Minimal scenario",
                        summary = "Empty board, just lands",
                        value = """
{
  "player1": {
    "battlefield": [{"name": "Forest"}, {"name": "Forest"}]
  },
  "player2": {
    "battlefield": [{"name": "Swamp"}]
  }
}
                        """
                    )
                ]
            )]
        ),
        responses = [
            ApiResponse(responseCode = "200", description = "Scenario created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid scenario configuration (e.g., unknown card name)")
        ]
    )
    fun createScenario(
        @RequestBody request: ScenarioRequest,
        @RequestParam(required = false) player1Token: String?,
        @RequestParam(required = false) player2Token: String?
    ): ResponseEntity<ScenarioResponse> {
        logger.info("Creating dev scenario: player1=${request.player1Name}, player2=${request.player2Name}")

        try {
            val builder = ScenarioBuilder(cardRegistry)

            // Initialize players
            builder.withPlayers(request.player1Name, request.player2Name)

            // Set up player 1
            request.player1?.let { config ->
                config.lifeTotal?.let { builder.withLifeTotal(1, it) }
                config.hand.forEach { builder.withCardInHand(1, it) }
                config.battlefield.forEach { card ->
                    builder.withCardOnBattlefield(
                        1, card.name,
                        tapped = card.tapped,
                        summoningSickness = card.summoningSickness
                    )
                }
                config.graveyard.forEach { builder.withCardInGraveyard(1, it) }
                config.library.forEach { builder.withCardInLibrary(1, it) }
            }

            // Set up player 2
            request.player2?.let { config ->
                config.lifeTotal?.let { builder.withLifeTotal(2, it) }
                config.hand.forEach { builder.withCardInHand(2, it) }
                config.battlefield.forEach { card ->
                    builder.withCardOnBattlefield(
                        2, card.name,
                        tapped = card.tapped,
                        summoningSickness = card.summoningSickness
                    )
                }
                config.graveyard.forEach { builder.withCardInGraveyard(2, it) }
                config.library.forEach { builder.withCardInLibrary(2, it) }
            }

            // Set game state
            request.phase?.let { phase ->
                val step = request.step ?: phaseToDefaultStep(phase)
                builder.inPhase(phase, step)
            }
            request.activePlayer?.let { builder.withActivePlayer(it) }
            request.priorityPlayer?.let { builder.withPriorityPlayer(it) }

            // Build the scenario
            val (state, player1Id, player2Id) = builder.build()

            // Create the game session
            val gameSession = GameSession(
                cardRegistry = cardRegistry,
                stateTransformer = stateTransformer
            )

            // Inject the pre-built state (without player sessions - they'll connect later)
            gameSession.injectStateForDevScenario(state)

            // Apply per-step stop overrides (prevents auto-pass at specified steps)
            if (request.player1StopAtSteps.isNotEmpty()) {
                gameSession.setStopOverrides(player1Id, request.player1StopAtSteps.toSet(), emptySet())
            }
            if (request.player2StopAtSteps.isNotEmpty()) {
                gameSession.setStopOverrides(player2Id, request.player2StopAtSteps.toSet(), emptySet())
            }

            // Save the session
            gameRepository.save(gameSession)

            // Create player identities with matching player IDs from the scenario
            // Use provided tokens from query params if available, otherwise generate random UUIDs
            val identity1 = PlayerIdentity(
                token = player1Token ?: java.util.UUID.randomUUID().toString(),
                playerId = player1Id,
                playerName = request.player1Name
            ).apply {
                currentGameSessionId = gameSession.sessionId
            }

            val identity2 = PlayerIdentity(
                token = player2Token ?: java.util.UUID.randomUUID().toString(),
                playerId = player2Id,
                playerName = request.player2Name
            ).apply {
                currentGameSessionId = gameSession.sessionId
            }

            // Pre-register identities so players can connect with their tokens
            sessionRegistry.preRegisterIdentity(identity1)
            sessionRegistry.preRegisterIdentity(identity2)

            logger.info("Created scenario session ${gameSession.sessionId}")

            return ResponseEntity.ok(ScenarioResponse(
                sessionId = gameSession.sessionId,
                player1 = PlayerInfo(
                    name = request.player1Name,
                    token = identity1.token,
                    playerId = player1Id.value
                ),
                player2 = PlayerInfo(
                    name = request.player2Name,
                    token = identity2.token,
                    playerId = player2Id.value
                ),
                message = "Scenario created. Connect via WebSocket at /game and send Connect message with your token."
            ))
        } catch (e: Exception) {
            logger.error("Failed to create scenario", e)
            return ResponseEntity.badRequest().body(ScenarioResponse(
                sessionId = "",
                player1 = PlayerInfo("", "", ""),
                player2 = PlayerInfo("", "", ""),
                message = "Failed to create scenario: ${e.message}"
            ))
        }
    }

    /**
     * List available cards for scenario building.
     */
    @GetMapping("/cards")
    @Operation(
        summary = "List available cards",
        description = "Returns a sorted list of all card names that can be used in scenarios."
    )
    fun listCards(): ResponseEntity<List<String>> {
        val cardNames = cardRegistry.allCardNames().sorted()
        return ResponseEntity.ok(cardNames)
    }

    private fun phaseToDefaultStep(phase: Phase): Step {
        return when (phase) {
            Phase.BEGINNING -> Step.UNTAP
            Phase.PRECOMBAT_MAIN -> Step.PRECOMBAT_MAIN
            Phase.COMBAT -> Step.BEGIN_COMBAT
            Phase.POSTCOMBAT_MAIN -> Step.POSTCOMBAT_MAIN
            Phase.ENDING -> Step.END
        }
    }

    /**
     * Internal scenario builder (similar to ScenarioTestBase.ScenarioBuilder).
     */
    private inner class ScenarioBuilder(private val cardRegistry: CardRegistry) {
        private val entityIdCounter = AtomicLong(1000)
        private var state = GameState()

        private var player1Id: EntityId? = null
        private var player2Id: EntityId? = null

        fun withPlayers(player1Name: String, player2Name: String): ScenarioBuilder {
            player1Id = EntityId.of("player-1")
            player2Id = EntityId.of("player-2")

            val p1Container = ComponentContainer.of(
                PlayerComponent(player1Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            val p2Container = ComponentContainer.of(
                PlayerComponent(player2Name),
                LifeTotalComponent(20),
                ManaPoolComponent(),
                LandDropsComponent(remaining = 1, maxPerTurn = 1)
            )

            state = state
                .withEntity(player1Id!!, p1Container)
                .withEntity(player2Id!!, p2Container)
                .copy(
                    turnOrder = listOf(player1Id!!, player2Id!!),
                    activePlayerId = player1Id,
                    priorityPlayerId = player1Id,
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                    turnNumber = 1
                )

            // Initialize empty zones for both players
            for (playerId in listOf(player1Id!!, player2Id!!)) {
                for (zoneType in listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.BATTLEFIELD)) {
                    val zoneKey = ZoneKey(playerId, zoneType)
                    state = state.copy(zones = state.zones + (zoneKey to emptyList()))
                }
            }

            return this
        }

        fun withCardInHand(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.HAND), cardId)
            return this
        }

        fun withCardOnBattlefield(
            playerNumber: Int,
            cardName: String,
            tapped: Boolean = false,
            summoningSickness: Boolean = false
        ): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)

            state = state.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)

            var container = state.getEntity(cardId)!!
            container = container.with(ControllerComponent(playerId))

            if (tapped) {
                container = container.with(TappedComponent)
            }

            if (summoningSickness) {
                container = container.with(SummoningSicknessComponent)
            }

            // Add continuous effects from static abilities and replacement effects
            val cardDef = cardRegistry.getCard(cardName)
            if (cardDef != null) {
                val staticHandler = StaticAbilityHandler(cardRegistry)
                container = staticHandler.addContinuousEffectComponent(container, cardDef)
                container = staticHandler.addReplacementEffectComponent(container, cardDef)
            }

            state = state.withEntity(cardId, container)
            return this
        }

        fun withCardInGraveyard(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.GRAVEYARD), cardId)
            return this
        }

        fun withCardInLibrary(playerNumber: Int, cardName: String): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            val cardId = createCard(cardName, playerId)
            state = state.addToZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            return this
        }

        fun withLifeTotal(playerNumber: Int, life: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(life))
            }
            return this
        }

        fun inPhase(phase: Phase, step: Step): ScenarioBuilder {
            state = state.copy(phase = phase, step = step)
            return this
        }

        fun withActivePlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(activePlayerId = playerId, priorityPlayerId = playerId)
            return this
        }

        fun withPriorityPlayer(playerNumber: Int): ScenarioBuilder {
            val playerId = if (playerNumber == 1) player1Id!! else player2Id!!
            state = state.copy(priorityPlayerId = playerId)
            return this
        }

        fun build(): Triple<GameState, EntityId, EntityId> {
            return Triple(state, player1Id!!, player2Id!!)
        }

        private fun createCard(cardName: String, ownerId: EntityId): EntityId {
            val cardDef = cardRegistry.getCard(cardName)
                ?: error("Card not found in registry: $cardName")

            val cardId = EntityId.of("card-${entityIdCounter.incrementAndGet()}")

            val cardComponent = CardComponent(
                cardDefinitionId = cardDef.name,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                colors = cardDef.colors,
                baseKeywords = cardDef.keywords,
                baseStats = cardDef.creatureStats,
                ownerId = ownerId,
                spellEffect = cardDef.spellEffect,
                imageUri = cardDef.metadata.imageUri
            )

            var container = ComponentContainer.of(
                cardComponent,
                OwnerComponent(ownerId),
                ControllerComponent(ownerId)
            )

            // Add ProtectionComponent for cards with protection from color/subtype
            val protectionColors = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.ProtectionFromColor>()
                .map { it.color }
                .toSet() +
                cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.ProtectionFromColors>()
                    .flatMap { it.colors }
                    .toSet()
            val protectionSubtypes = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.ProtectionFromCreatureSubtype>()
                .map { it.subtype }
                .toSet()
            if (protectionColors.isNotEmpty() || protectionSubtypes.isNotEmpty()) {
                container = container.with(ProtectionComponent(protectionColors, protectionSubtypes))
            }

            state = state.withEntity(cardId, container)
            return cardId
        }
    }
}

// ============================================================================
// Request/Response DTOs
// ============================================================================

data class ScenarioRequest(
    val player1Name: String = "Player1",
    val player2Name: String = "Player2",
    val player1: PlayerConfig? = null,
    val player2: PlayerConfig? = null,
    val phase: Phase? = null,
    val step: Step? = null,
    val activePlayer: Int? = null,
    val priorityPlayer: Int? = null,
    /** Steps where player 1 should stop on their own turn (prevents auto-pass) */
    val player1StopAtSteps: List<Step> = emptyList(),
    /** Steps where player 2 should stop on their own turn (prevents auto-pass) */
    val player2StopAtSteps: List<Step> = emptyList()
)

data class PlayerConfig(
    val lifeTotal: Int? = null,
    val hand: List<String> = emptyList(),
    val battlefield: List<BattlefieldCardConfig> = emptyList(),
    val graveyard: List<String> = emptyList(),
    val library: List<String> = emptyList()
)

/**
 * Configuration for a card on the battlefield.
 * For simple cases, just provide the name.
 * For detailed setup, also specify tapped/summoningSickness state.
 */
data class BattlefieldCardConfig(
    val name: String,
    val tapped: Boolean = false,
    val summoningSickness: Boolean = false
)

data class ScenarioResponse(
    val sessionId: String,
    val player1: PlayerInfo,
    val player2: PlayerInfo,
    val message: String
)

data class PlayerInfo(
    val name: String,
    val token: String,
    val playerId: String
)
