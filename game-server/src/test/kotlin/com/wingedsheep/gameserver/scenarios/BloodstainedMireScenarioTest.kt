package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Bloodstained Mire.
 *
 * Bloodstained Mire
 * Land
 * {T}, Pay 1 life, Sacrifice Bloodstained Mire: Search your library for a Swamp or Mountain card,
 * put it onto the battlefield, then shuffle.
 */
class BloodstainedMireScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodstained Mire fetch land") {

            test("can fetch a Swamp from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodstained Mire")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mireId = game.findPermanent("Bloodstained Mire")!!
                val cardDef = cardRegistry.getCard("Bloodstained Mire")!!
                val ability = cardDef.script.activatedAbilities.first()

                val startingLife = game.getLifeTotal(1)

                // Activate Bloodstained Mire
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mireId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Bloodstained Mire should be sacrificed
                withClue("Bloodstained Mire should be sacrificed") {
                    game.isOnBattlefield("Bloodstained Mire") shouldBe false
                    game.isInGraveyard(1, "Bloodstained Mire") shouldBe true
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

                // Only Swamp should be selectable (not Forest)
                withClue("Only Swamp should be available (not Forest)") {
                    decision.cardInfo!!.values.any { it.name == "Swamp" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Forest" } shouldBe false
                }

                // Select the Swamp
                val swampId = decision.cardInfo!!.entries.first { it.value.name == "Swamp" }.key
                game.selectCards(listOf(swampId))

                // Swamp should be on the battlefield (untapped)
                withClue("Swamp should be on the battlefield") {
                    game.isOnBattlefield("Swamp") shouldBe true
                }
            }

            test("can fetch a Mountain from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodstained Mire")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mireId = game.findPermanent("Bloodstained Mire")!!
                val cardDef = cardRegistry.getCard("Bloodstained Mire")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Bloodstained Mire
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mireId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Only Mountain should be selectable (not Island)
                withClue("Only Mountain should be available (not Island)") {
                    decision.cardInfo!!.values.any { it.name == "Mountain" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Island" } shouldBe false
                }

                // Select the Mountain
                val mountainId = decision.cardInfo!!.entries.first { it.value.name == "Mountain" }.key
                game.selectCards(listOf(mountainId))

                // Mountain should be on the battlefield
                withClue("Mountain should be on the battlefield") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }
            }

            test("can choose between Swamp and Mountain when both are available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodstained Mire")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mireId = game.findPermanent("Bloodstained Mire")!!
                val cardDef = cardRegistry.getCard("Bloodstained Mire")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mireId,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val decision = game.getPendingDecision()!! as SelectCardsDecision

                // Both Swamp and Mountain should be available
                withClue("Both Swamp and Mountain should be available") {
                    decision.cardInfo!!.values.any { it.name == "Swamp" } shouldBe true
                    decision.cardInfo!!.values.any { it.name == "Mountain" } shouldBe true
                }

                // Select the Swamp
                val swampId = decision.cardInfo!!.entries.first { it.value.name == "Swamp" }.key
                game.selectCards(listOf(swampId))

                withClue("Swamp should be on the battlefield") {
                    game.isOnBattlefield("Swamp") shouldBe true
                }
            }

            test("pays 1 life as part of activation cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodstained Mire")
                    .withCardInLibrary(1, "Swamp")
                    .withLifeTotal(1, 15)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mireId = game.findPermanent("Bloodstained Mire")!!
                val cardDef = cardRegistry.getCard("Bloodstained Mire")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mireId,
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
