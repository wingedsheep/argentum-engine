package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Natural Order with Mystic Denial interaction.
 *
 * Natural Order:
 * - {2}{G}{G} Sorcery
 * - "As an additional cost to cast this spell, sacrifice a green creature.
 *    Search your library for a green creature card, put it onto the battlefield, then shuffle."
 *
 * Mystic Denial:
 * - {1}{U}{U} Instant
 * - "Cast this spell only after an opponent casts a creature or sorcery spell.
 *    Counter target creature or sorcery spell."
 *
 * This tests:
 * 1. Natural Order's additional sacrifice cost works correctly
 * 2. Mystic Denial can properly target Natural Order on the stack
 */
class NaturalOrderScenarioTest : ScenarioTestBase() {

    init {
        context("Natural Order additional sacrifice cost") {
            test("Natural Order requires sacrificing a green creature to cast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Order")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 green creature
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Whiptail Wurm") // 8/5 green creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Natural Order, sacrificing Grizzly Bears
                val castResult = game.castSpellWithAdditionalSacrifice(1, "Natural Order", "Grizzly Bears")
                withClue("Natural Order should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Grizzly Bears should be in graveyard (sacrificed as additional cost)
                withClue("Grizzly Bears should be in graveyard (sacrificed as cost)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Natural Order should be on the stack
                withClue("Natural Order should be on the stack") {
                    game.state.stack.any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Natural Order"
                    } shouldBe true
                }
            }

            test("Natural Order cannot be cast without a green creature to sacrifice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Order")
                    // No green creature to sacrifice
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Create a GameSession to check legal actions
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Get legal actions for Player 1
                val legalActions = session.getLegalActions(game.player1Id)

                // Natural Order should NOT be in legal actions (no green creature to sacrifice)
                val naturalOrderAction = legalActions.find { it.description == "Cast Natural Order" }
                withClue("Natural Order should NOT be castable without a green creature to sacrifice") {
                    naturalOrderAction shouldBe null
                }
            }
        }

        context("Mystic Denial targeting Natural Order on the stack") {
            test("Mystic Denial can counter Natural Order") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Order")
                    .withCardOnBattlefield(1, "Grizzly Bears") // Will be sacrificed
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInHand(2, "Mystic Denial")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Whiptail Wurm") // The creature Natural Order would find
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Natural Order, sacrificing Grizzly Bears
                val castResult = game.castSpellWithAdditionalSacrifice(1, "Natural Order", "Grizzly Bears")
                withClue("Natural Order should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Grizzly Bears should be sacrificed
                withClue("Grizzly Bears should be sacrificed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Player 2 casts Mystic Denial targeting Natural Order
                val counterResult = game.castSpellTargetingStackSpell(2, "Mystic Denial", "Natural Order")
                withClue("Mystic Denial should be cast successfully: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }

                // Resolve the stack (Mystic Denial resolves first, countering Natural Order)
                game.resolveStack()

                // Natural Order should be countered (in graveyard)
                withClue("Natural Order should be in player 1's graveyard (countered)") {
                    game.isInGraveyard(1, "Natural Order") shouldBe true
                }

                // Whiptail Wurm should NOT be on the battlefield
                withClue("Whiptail Wurm should NOT be on the battlefield (spell was countered)") {
                    game.isOnBattlefield("Whiptail Wurm") shouldBe false
                }

                // Both the sacrifice and the spell were "wasted" - Grizzly Bears still in graveyard
                withClue("Grizzly Bears should still be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("Legal actions include Mystic Denial targeting Natural Order on stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Order")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInHand(2, "Mystic Denial")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Whiptail Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Natural Order
                val castResult = game.castSpellWithAdditionalSacrifice(1, "Natural Order", "Grizzly Bears")
                withClue("Natural Order should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority, now Player 2 has priority
                game.execute(PassPriority(game.player1Id))

                // Create a GameSession and inject the current state
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Verify Player 2 has priority
                withClue("Player 2 should have priority") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                }

                // Get legal actions for Player 2
                val legalActions = session.getLegalActions(game.player2Id)

                // Mystic Denial should be in legal actions
                val mysticDenialAction = legalActions.find { it.description == "Cast Mystic Denial" }
                withClue("Legal actions should include 'Cast Mystic Denial'") {
                    mysticDenialAction.shouldNotBeNull()
                }

                // Mystic Denial should have valid targets (the Natural Order on stack)
                withClue("Mystic Denial action should require targets") {
                    mysticDenialAction!!.requiresTargets shouldBe true
                }
                withClue("Mystic Denial action should have valid targets (the Natural Order on stack)") {
                    mysticDenialAction!!.validTargets.shouldNotBeNull()
                    mysticDenialAction.validTargets!!.size shouldBe 1
                }

                // The target should be the Natural Order spell
                val naturalOrderOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Natural Order"
                }
                withClue("Valid target should be Natural Order on the stack") {
                    mysticDenialAction!!.validTargets!! shouldContain naturalOrderOnStack
                }
            }
        }

        context("Natural Order resolves successfully when not countered") {
            test("Natural Order fetches and puts a green creature onto battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Order")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Whiptail Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Natural Order, sacrificing Grizzly Bears
                val castResult = game.castSpellWithAdditionalSacrifice(1, "Natural Order", "Grizzly Bears")
                withClue("Natural Order should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Should have a pending library search decision
                withClue("Should have pending library search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Find Whiptail Wurm in the library (available for selection)
                val decision = game.getPendingDecision()!!
                val libraryCards = (decision as? com.wingedsheep.engine.core.SearchLibraryDecision)?.cards
                withClue("Library search should include Whiptail Wurm") {
                    libraryCards.shouldNotBeNull()
                    libraryCards.values.any { it.name == "Whiptail Wurm" } shouldBe true
                }

                // Select Whiptail Wurm
                val crawWurmId = libraryCards!!.entries.first { it.value.name == "Whiptail Wurm" }.key
                game.selectCards(listOf(crawWurmId))

                // Whiptail Wurm should now be on the battlefield
                withClue("Whiptail Wurm should be on the battlefield") {
                    game.isOnBattlefield("Whiptail Wurm") shouldBe true
                }

                // Natural Order should be in graveyard
                withClue("Natural Order should be in graveyard after resolution") {
                    game.isInGraveyard(1, "Natural Order") shouldBe true
                }
            }
        }
    }
}
