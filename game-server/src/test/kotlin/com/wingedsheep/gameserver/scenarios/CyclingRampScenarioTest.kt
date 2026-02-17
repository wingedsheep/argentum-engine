package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Explosive Vegetation, Solar Blast, and Krosan Tusker.
 *
 * Explosive Vegetation: {3}{G} Sorcery - Search library for up to two basic lands,
 *   put them onto the battlefield tapped, then shuffle.
 *
 * Solar Blast: {3}{R} Instant - Deals 3 damage to any target.
 *   Cycling {1}{R}{R}. When cycled, may deal 1 damage to any target.
 *
 * Krosan Tusker: {5}{G}{G} Creature 6/5.
 *   Cycling {2}{G}. When cycled, may search library for a basic land, reveal it,
 *   put it into your hand, then shuffle.
 */
class CyclingRampScenarioTest : ScenarioTestBase() {

    init {
        context("Explosive Vegetation") {
            test("searches for two basic lands and puts them onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Explosive Vegetation")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Explosive Vegetation")
                game.resolveStack()

                // Should have a pending library search decision
                withClue("Should have pending library search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select two lands from the library
                val decision = game.getPendingDecision()!! as com.wingedsheep.engine.core.SelectCardsDecision
                val forestId = decision.cardInfo!!.entries.first { it.value.name == "Forest" }.key
                val mountainId = decision.cardInfo!!.entries.first { it.value.name == "Mountain" }.key
                game.selectCards(listOf(forestId, mountainId))

                // Both lands should be on the battlefield
                withClue("Forest should be on the battlefield") {
                    // There are already 4 Forests from setup, so we need at least 5
                    game.findAllPermanents("Forest").size shouldBe 5
                }
                withClue("Mountain should be on the battlefield") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }

                // The newly placed lands should be tapped
                val mountainEntityId = game.findPermanent("Mountain")!!
                withClue("Mountain should enter tapped") {
                    game.state.getEntity(mountainEntityId)?.has<TappedComponent>() shouldBe true
                }
            }

            test("can choose to find only one land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Explosive Vegetation")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears") // Not a basic land
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Explosive Vegetation")
                game.resolveStack()

                val decision = game.getPendingDecision()!! as com.wingedsheep.engine.core.SelectCardsDecision
                val mountainId = decision.cardInfo!!.entries.first { it.value.name == "Mountain" }.key
                game.selectCards(listOf(mountainId))

                withClue("Mountain should be on the battlefield") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }
            }
        }

        context("Solar Blast") {
            test("deals 3 damage to a creature when cast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Solar Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Solar Blast", targetId)
                game.resolveStack()

                withClue("Grizzly Bears should be destroyed by 3 damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("cycling trigger deals 1 damage to target creature when player chooses yes") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Solar Blast")
                    .withLandsOnBattlefield(1, "Mountain", 3) // {1}{R}{R} cycling cost
                    .withCardInLibrary(1, "Forest") // Card to draw from cycling
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 - survives 1 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLife = game.getLifeTotal(2)

                // Cycle Solar Blast
                game.cycleCard(1, "Solar Blast")

                // Cycling triggers - MayEffect asks yes/no first
                withClue("Solar Blast cycling trigger should present may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Target the opponent player
                game.selectTargets(listOf(game.player2Id))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                withClue("Opponent should have lost 1 life from cycling trigger") {
                    game.getLifeTotal(2) shouldBe opponentLife - 1
                }
            }

            test("cycling trigger does not deal damage when player declines") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Solar Blast")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Solar Blast")

                // Decline the may ability (before target selection)
                game.answerYesNo(false)

                withClue("Glory Seeker should survive when player declines") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }

        context("Krosan Tusker") {
            test("cycling trigger searches for a basic land and puts it into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Krosan Tusker")
                    .withLandsOnBattlefield(1, "Forest", 3) // {2}{G} cycling cost
                    .withCardInLibrary(1, "Grizzly Bears") // Non-land card to draw from cycling (top of library)
                    .withCardInLibrary(1, "Mountain") // Basic land to find via search
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle Krosan Tusker
                game.cycleCard(1, "Krosan Tusker")

                // Cycling draws Grizzly Bears, then trigger goes on the stack
                // Resolve the triggered ability
                game.resolveStack()

                // MayEffect asks yes/no
                withClue("Should have may decision for library search") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Library search decision
                withClue("Should have pending library search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()!! as com.wingedsheep.engine.core.SelectCardsDecision
                val mountainId = decision.cardInfo!!.entries.first { it.value.name == "Mountain" }.key
                game.selectCards(listOf(mountainId))

                // Krosan Tusker should be in graveyard (cycled)
                withClue("Krosan Tusker should be in graveyard (cycled)") {
                    game.isInGraveyard(1, "Krosan Tusker") shouldBe true
                }

                // Mountain should NOT be on the battlefield (goes to hand, not battlefield)
                withClue("Mountain should not be on battlefield (goes to hand)") {
                    game.isOnBattlefield("Mountain") shouldBe false
                }
            }

            test("cycling trigger can be declined") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Krosan Tusker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Grizzly Bears") // Card to draw from cycling (top of library)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Krosan Tusker")
                game.resolveStack()

                // Decline the may ability
                game.answerYesNo(false)

                // Should not have a library search decision
                withClue("No further decision after declining") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
