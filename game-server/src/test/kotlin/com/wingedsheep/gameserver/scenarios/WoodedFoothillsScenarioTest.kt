package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wooded Foothills.
 *
 * Wooded Foothills
 * Land
 * {T}, Pay 1 life, Sacrifice Wooded Foothills: Search your library for a Mountain or Forest card,
 * put it onto the battlefield, then shuffle.
 */
class WoodedFoothillsScenarioTest : ScenarioTestBase() {

    init {
        context("Wooded Foothills fetch land") {

            test("can fetch a Mountain from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wooded Foothills")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foothillsId = game.findPermanent("Wooded Foothills")!!
                val cardDef = cardRegistry.getCard("Wooded Foothills")!!
                val ability = cardDef.script.activatedAbilities.first()

                val startingLife = game.getLifeTotal(1)

                // Activate Wooded Foothills
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = foothillsId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Wooded Foothills should be sacrificed
                withClue("Wooded Foothills should be sacrificed") {
                    game.isOnBattlefield("Wooded Foothills") shouldBe false
                    game.isInGraveyard(1, "Wooded Foothills") shouldBe true
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

                // Only Mountain should be selectable (not Plains)
                withClue("Only Mountain should be available (not Plains)") {
                    decision.cardInfo!!.values.any { it.name == "Mountain" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Plains" } shouldBe false
                }

                // Select the Mountain
                val mountainId = decision.cardInfo!!.entries.first { it.value.name == "Mountain" }.key
                game.selectCards(listOf(mountainId))

                // Mountain should be on the battlefield (untapped)
                withClue("Mountain should be on the battlefield") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }
            }

            test("can fetch a Forest from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wooded Foothills")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foothillsId = game.findPermanent("Wooded Foothills")!!
                val cardDef = cardRegistry.getCard("Wooded Foothills")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Wooded Foothills
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = foothillsId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Only Forest should be selectable (not Island)
                withClue("Only Forest should be available (not Island)") {
                    decision.cardInfo!!.values.any { it.name == "Forest" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Island" } shouldBe false
                }

                // Select the Forest
                val forestId = decision.cardInfo!!.entries.first { it.value.name == "Forest" }.key
                game.selectCards(listOf(forestId))

                // Forest should be on the battlefield
                withClue("Forest should be on the battlefield") {
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }

            test("can choose between Mountain and Forest when both are available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wooded Foothills")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foothillsId = game.findPermanent("Wooded Foothills")!!
                val cardDef = cardRegistry.getCard("Wooded Foothills")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = foothillsId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Both Mountain and Forest should be available
                withClue("Both Mountain and Forest should be available") {
                    decision.cardInfo!!.values.any { it.name == "Mountain" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Forest" } shouldBe true
                }

                // Select the Forest
                val forestId = decision.cardInfo!!.entries.first { it.value.name == "Forest" }.key
                game.selectCards(listOf(forestId))

                withClue("Forest should be on the battlefield") {
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }

            test("pays 1 life as part of activation cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wooded Foothills")
                    .withCardInLibrary(1, "Mountain")
                    .withLifeTotal(1, 15)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foothillsId = game.findPermanent("Wooded Foothills")!!
                val cardDef = cardRegistry.getCard("Wooded Foothills")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = foothillsId,
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
