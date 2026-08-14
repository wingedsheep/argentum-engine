package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unstoppable Plan — "At the beginning of your end step, untap all nonland permanents you control."
 *
 * The three ways a group untap can go wrong, one per assertion: it must reach *every* nonland
 * permanent you control (not only creatures), it must skip lands, and it must not touch permanents
 * an opponent controls.
 */
class UnstoppablePlanScenarioTest : ScenarioTestBase() {

    init {
        test("untaps your nonland permanents at your end step, sparing lands and the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Unstoppable Plan")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Air Response Unit", tapped = true)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Centaur Courser", tapped = true)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myBears = game.findPermanent("Grizzly Bears")!!
            val theirCourser = game.findPermanent("Centaur Courser")!!
            val vehicle = game.findPermanent("Air Response Unit")!!
            val island = game.findPermanent("Island")!!
            game.state = game.state.updateEntity(island) { it.with(TappedComponent) }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("A tapped creature you control untaps") {
                game.state.getEntity(myBears)?.has<TappedComponent>() shouldBe false
            }
            withClue("So does a noncreature nonland permanent — a tapped Vehicle") {
                game.state.getEntity(vehicle)?.has<TappedComponent>() shouldBe false
            }
            withClue("Lands are excluded: \"all nonland permanents\"") {
                game.state.getEntity(island)?.has<TappedComponent>() shouldBe true
            }
            withClue("And it is \"you control\", so the opponent's creature stays tapped") {
                game.state.getEntity(theirCourser)?.has<TappedComponent>() shouldBe true
            }
        }
    }

    /** Both libraries stocked so nobody decks out while passing to the end step. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(8) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}
