package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Mind's Desire.
 *
 * Card reference:
 * - Mind's Desire ({4}{U}{U}): Sorcery
 *   "Shuffle your library. Then exile the top card of your library.
 *    Until end of turn, you may play that card without paying its mana cost.
 *    Storm"
 */
class MindsDesireScenarioTest : ScenarioTestBase() {

    private fun getExile(game: TestGame, playerNumber: Int): List<EntityId> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId)
    }

    init {
        context("Mind's Desire basic effect") {
            test("shuffles library and exiles top card with play-from-exile and play-for-free permissions") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mind's Desire")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Mind's Desire")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Should have exiled exactly 1 card
                val exile = getExile(game, 1)
                withClue("Player should have 1 card in exile") {
                    exile shouldHaveSize 1
                }

                // The exiled card should have both components
                val exiledEntity = game.state.getEntity(exile.first())
                withClue("Exiled card should have MayPlayFromExileComponent") {
                    exiledEntity?.get<MayPlayFromExileComponent>() shouldNotBe null
                    exiledEntity?.get<MayPlayFromExileComponent>()?.controllerId shouldBe game.player1Id
                }
                withClue("Exiled card should have PlayWithoutPayingCostComponent") {
                    exiledEntity?.get<PlayWithoutPayingCostComponent>() shouldNotBe null
                    exiledEntity?.get<PlayWithoutPayingCostComponent>()?.controllerId shouldBe game.player1Id
                }
            }

            test("exiled creature card can be cast without paying mana cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mind's Desire")
                    .withLandsOnBattlefield(1, "Island", 6)
                    // Only Goblin Sky Raiders in library so shuffle doesn't matter
                    .withCardInLibrary(1, "Goblin Sky Raider") // {2}{R} creature
                    .withCardInLibrary(1, "Goblin Sky Raider")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Mind's Desire (taps 6 islands)
                val castResult = game.castSpell(1, "Mind's Desire")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Get the exiled card
                val exile = getExile(game, 1)
                exile shouldHaveSize 1
                val exiledCardId = exile.first()

                // Cast the exiled card for free (no mana needed, all lands are tapped)
                val freeCastResult = game.execute(CastSpell(game.player1Id, exiledCardId))
                withClue("Free cast from exile should succeed: ${freeCastResult.error}") {
                    freeCastResult.error shouldBe null
                }

                // Verify the card is on the stack
                withClue("Stack should have the creature spell") {
                    game.state.stack.isEmpty() shouldBe false
                }

                // Resolve the creature spell
                game.resolveStack()

                // The creature should be on the battlefield now
                // Check both player's battlefield (in case of ownership issues)
                val allBattlefield = game.state.getBattlefield()
                val creatureOnBattlefield = allBattlefield.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Sky Raider"
                }
                withClue("Goblin Sky Raider should be on the battlefield (all permanents: ${allBattlefield.map { game.state.getEntity(it)?.get<CardComponent>()?.name }})") {
                    creatureOnBattlefield shouldBe true
                }

                // Exile should be empty (card was cast from there)
                val exileAfter = getExile(game, 1)
                withClue("Exile should be empty after casting the card") {
                    exileAfter shouldHaveSize 0
                }
            }
        }

        context("Mind's Desire storm interaction") {
            test("storm copies also exile and grant free play") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gilded Light")  // Cast first for storm count
                    .withCardInHand(1, "Mind's Desire")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    // Library needs enough cards for shuffle + exile (1 original + 1 storm copy = 2 exiled)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Goblin Sky Raider")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Gilded Light first (storm count = 1 after this)
                game.castSpell(1, "Gilded Light")
                game.resolveStack()

                // Now cast Mind's Desire (storm count = 1, so 1 copy)
                val castResult = game.castSpell(1, "Mind's Desire")
                withClue("Mind's Desire cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the storm trigger and the original spell
                game.resolveStack()

                // After full resolution, should have 2 cards in exile (original + 1 storm copy)
                val exile = getExile(game, 1)
                withClue("Player should have 2 cards exiled (original + 1 storm copy)") {
                    exile shouldHaveSize 2
                }

                // Both exiled cards should have both components
                for (cardId in exile) {
                    val entity = game.state.getEntity(cardId)
                    val cardName = entity?.get<CardComponent>()?.name
                    withClue("Exiled card $cardName should have MayPlayFromExileComponent") {
                        entity?.get<MayPlayFromExileComponent>() shouldNotBe null
                    }
                    withClue("Exiled card $cardName should have PlayWithoutPayingCostComponent") {
                        entity?.get<PlayWithoutPayingCostComponent>() shouldNotBe null
                    }
                }
            }
        }
    }
}
