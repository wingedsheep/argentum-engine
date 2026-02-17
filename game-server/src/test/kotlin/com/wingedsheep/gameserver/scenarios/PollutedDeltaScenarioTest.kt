package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Polluted Delta.
 *
 * Polluted Delta
 * Land
 * {T}, Pay 1 life, Sacrifice Polluted Delta: Search your library for an Island or Swamp card,
 * put it onto the battlefield, then shuffle.
 */
class PollutedDeltaScenarioTest : ScenarioTestBase() {

    init {
        context("Polluted Delta fetch land") {

            test("can fetch an Island from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polluted Delta")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deltaId = game.findPermanent("Polluted Delta")!!
                val cardDef = cardRegistry.getCard("Polluted Delta")!!
                val ability = cardDef.script.activatedAbilities.first()

                val startingLife = game.getLifeTotal(1)

                // Activate Polluted Delta
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = deltaId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Polluted Delta should be sacrificed
                withClue("Polluted Delta should be sacrificed") {
                    game.isOnBattlefield("Polluted Delta") shouldBe false
                    game.isInGraveyard(1, "Polluted Delta") shouldBe true
                }

                // Player should have paid 1 life
                withClue("Player should have paid 1 life") {
                    game.getLifeTotal(1) shouldBe startingLife - 1
                }

                // Resolve the ability
                game.resolveStack()

                // Should have a library search decision
                withClue("Should have pending library search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Only Island should be selectable (not Forest)
                withClue("Only Island should be available (not Forest)") {
                    decision.cardInfo!!.values.any { it.name == "Island" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Forest" } shouldBe false
                }

                // Select the Island
                val islandId = decision.cardInfo!!.entries.first { it.value.name == "Island" }.key
                game.selectCards(listOf(islandId))

                // Island should be on the battlefield (untapped)
                withClue("Island should be on the battlefield") {
                    game.isOnBattlefield("Island") shouldBe true
                }
            }

            test("can fetch a Swamp from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polluted Delta")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deltaId = game.findPermanent("Polluted Delta")!!
                val cardDef = cardRegistry.getCard("Polluted Delta")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Polluted Delta
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = deltaId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Only Swamp should be selectable (not Mountain)
                withClue("Only Swamp should be available (not Mountain)") {
                    decision.cardInfo!!.values.any { it.name == "Swamp" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Mountain" } shouldBe false
                }

                // Select the Swamp
                val swampId = decision.cardInfo!!.entries.first { it.value.name == "Swamp" }.key
                game.selectCards(listOf(swampId))

                // Swamp should be on the battlefield
                withClue("Swamp should be on the battlefield") {
                    game.isOnBattlefield("Swamp") shouldBe true
                }
            }

            test("can choose between Island and Swamp when both are available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polluted Delta")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deltaId = game.findPermanent("Polluted Delta")!!
                val cardDef = cardRegistry.getCard("Polluted Delta")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = deltaId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Both Island and Swamp should be available
                withClue("Both Island and Swamp should be available") {
                    decision.cardInfo!!.values.any { it.name == "Island" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Swamp" } shouldBe true
                }

                // Select the Island
                val islandId = decision.cardInfo!!.entries.first { it.value.name == "Island" }.key
                game.selectCards(listOf(islandId))

                withClue("Island should be on the battlefield") {
                    game.isOnBattlefield("Island") shouldBe true
                }
            }

            test("pays 1 life as part of activation cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Polluted Delta")
                    .withCardInLibrary(1, "Swamp")
                    .withLifeTotal(1, 15)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deltaId = game.findPermanent("Polluted Delta")!!
                val cardDef = cardRegistry.getCard("Polluted Delta")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = deltaId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                withClue("Player should have paid 1 life (15 -> 14)") {
                    game.getLifeTotal(1) shouldBe 14
                }
            }
        }
    }
}
