package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Startling Development — "Until end of turn, target creature becomes a blue Serpent with base
 * power and toughness 4/4. Cycling {1}"
 */
class StartlingDevelopmentScenarioTest : ScenarioTestBase() {
    init {
        test("the target becomes a blue 4/4 Serpent until end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Startling Development")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.castSpell(1, "Startling Development", targetId = bears)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.getColors(bears) shouldBe setOf("BLUE")
            projected.getSubtypes(bears) shouldBe setOf("Serpent")
        }

        test("it sets base P/T, so a later pump still applies on top") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Startling Development")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Startling Development", targetId = bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 7
            game.state.projectedState.getToughness(bears) shouldBe 7
        }

        test("cycling for {1} discards it and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Startling Development")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.cycleCard(1, "Startling Development")
            withClue("cycling should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Startling Development") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
