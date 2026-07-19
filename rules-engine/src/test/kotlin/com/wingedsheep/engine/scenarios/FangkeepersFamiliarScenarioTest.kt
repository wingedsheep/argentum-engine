package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Fangkeeper's Familiar (TDM #183) — {1}{B}{G}{U} Snake, 3/3, Flash.
 *
 * "When this creature enters, choose one —
 *  • You gain 3 life and surveil 3.
 *  • Destroy target enchantment.
 *  • Counter target creature spell."
 *
 * Exercises the modal ETB trigger: casting the creature, resolving the ETB to a ChooseOption
 * decision, picking mode 0 (gain 3 life + surveil 3), and resolving the surveil look.
 */
class FangkeepersFamiliarScenarioTest : ScenarioTestBase() {

    init {
        context("Fangkeeper's Familiar's ETB modal trigger") {

            test("mode 1 gains 3 life and surveils 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fangkeeper's Familiar")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain") // surveil fodder (top three)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Fangkeeper's Familiar")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // The ETB trigger is on the stack; resolving it surfaces a mode choice.
                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB trigger; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                // The mode is picked as the trigger goes on the stack (CR 603.3c); the chosen
                // mode's effect runs when the ability itself resolves.
                game.resolveStack()

                // Mode 0 = gain 3 life and surveil 3 → a card selection over the top three cards.
                val surveil = game.getPendingDecision()
                withClue("Surveil 3 should pause for a library look") {
                    surveil.shouldBeInstanceOf<SelectCardsDecision>()
                }
                val lookedAt = (surveil as SelectCardsDecision).options
                withClue("Surveil 3 looks at the top three cards") { lookedAt.size shouldBe 3 }
                game.selectCards(lookedAt) // bin all three
                game.resolveStack()

                withClue("Player 1 gained 3 life") {
                    game.getLifeTotal(1) shouldBe 23
                }
                withClue("Surveil 3 binned all three looked-at cards") {
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                    game.findCardsInGraveyard(1, "Plains").size shouldBe 1
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                }
                withClue("Fangkeeper's Familiar is on the battlefield") {
                    game.isOnBattlefield("Fangkeeper's Familiar") shouldBe true
                }
            }
        }
    }
}
