package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Glacial Grasp (ZNR #59) — "Tap target creature. Its controller mills two cards. That creature
 * doesn't untap during its controller's next untap step. Draw a card."
 *
 * Proves the mill lands on the *target creature's controller* (not the caster), and the caster
 * draws.
 */
class GlacialGraspScenarioTest : ScenarioTestBase() {

    init {
        context("Glacial Grasp") {

            test("taps the opponent's creature, the opponent mills two, the caster draws one") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Glacial Grasp")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val casterLibrary = game.librarySize(1)
                val casterHand = game.handSize(1)

                game.castSpell(1, "Glacial Grasp", bears).error.shouldBeNull()
                game.resolveStack()

                withClue("the target creature is tapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the target creature's controller milled two cards") {
                    game.findCardsInGraveyard(2, "Plains").size shouldBe 2
                    game.librarySize(2) shouldBe 1
                }
                withClue("the caster milled nothing and drew one card") {
                    game.librarySize(1) shouldBe casterLibrary - 1
                    game.handSize(1) shouldBe casterHand // cast Grasp (-1), drew (+1)
                    game.findCardsInGraveyard(1, "Glacial Grasp").size shouldBe 1
                    game.graveyardSize(1) shouldBe 1
                }
            }
        }
    }
}
