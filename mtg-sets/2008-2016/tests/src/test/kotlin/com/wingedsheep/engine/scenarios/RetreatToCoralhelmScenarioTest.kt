package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Retreat to Coralhelm — a modal landfall trigger whose first mode nests a resolution-time
 * "you may tap or untap" choice (the Pestermite idiom) under the mode's own target. Proves the
 * mode's target reaches the inner tap/untap choice, and that declining the "may" does nothing.
 */
class RetreatToCoralhelmScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.chooseOption(prefix: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(prefix) }
        check(index >= 0) { "$prefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
    }

    private fun TestGame.playIsland() {
        val island = findCardsInHand(1, "Island").first()
        withClue("playing the land is legal") { execute(PlayLand(player1Id, island)).error shouldBe null }
    }

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Retreat to Coralhelm")
        .withCardInHand(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Retreat to Coralhelm") {

            test("landfall untaps a tapped creature you control") {
                val game = base().withCardOnBattlefield(1, "Grizzly Bears", tapped = true).build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.playIsland()
                game.chooseOption("You may tap or untap")
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.chooseOption("Untap")
                game.resolveStack()

                game.isTapped(bears) shouldBe false
            }

            test("landfall taps an opponent's creature") {
                val game = base().withCardOnBattlefield(2, "Grizzly Bears").build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.playIsland()
                game.chooseOption("You may tap or untap")
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.chooseOption("Tap")
                game.resolveStack()

                game.isTapped(bears) shouldBe true
            }

            test("declining the may leaves the creature as it was") {
                val game = base().withCardOnBattlefield(2, "Grizzly Bears").build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.playIsland()
                game.chooseOption("You may tap or untap")
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                game.isTapped(bears) shouldBe false
            }
        }
    }
}
