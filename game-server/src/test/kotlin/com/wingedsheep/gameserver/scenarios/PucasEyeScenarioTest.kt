package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PucasEyeScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseColor(color: Color) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(ColorChosenResponse(decision.id, color))
    }

    init {
        context("Puca's Eye") {

            test("ETB trigger draws, chooses a color, and makes Puca's Eye that color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Puca's Eye")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Swamp")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Puca's Eye")
                withClue("Puca's Eye should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()
                game.chooseColor(Color.BLUE)

                val eyeId = game.findPermanent("Puca's Eye")!!
                val chosenColor = game.state.getEntity(eyeId)?.get<ChosenColorComponent>()
                chosenColor.shouldNotBeNull()
                chosenColor.color shouldBe Color.BLUE
                game.state.projectedState.getColors(eyeId) shouldBe setOf("BLUE")
                game.state.getHand(game.player1Id).size shouldBe 1
            }

            test("activated ability requires five colors among permanents you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Puca's Eye")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Swamp")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Serpent Warrior")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Puca's Eye")
                game.resolveStack()
                game.chooseColor(Color.BLUE)

                val eyeId = game.findPermanent("Puca's Eye")!!
                val ability = cardRegistry.getCard("Puca's Eye")!!.script.activatedAbilities.first()
                val handSizeBefore = game.state.getHand(game.player1Id).size

                val activateResult = game.execute(ActivateAbility(game.player1Id, eyeId, ability.id))
                withClue("Puca's Eye draw ability should be activatable with five colors: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()
                game.state.getHand(game.player1Id).size shouldBe handSizeBefore + 1
            }

            test("activated ability cannot be activated below five colors") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Puca's Eye")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Swamp")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Puca's Eye")
                game.resolveStack()
                game.chooseColor(Color.BLUE)

                val eyeId = game.findPermanent("Puca's Eye")!!
                val ability = cardRegistry.getCard("Puca's Eye")!!.script.activatedAbilities.first()
                val activateResult = game.execute(ActivateAbility(game.player1Id, eyeId, ability.id))

                activateResult.error shouldNotBe null
            }
        }
    }
}
