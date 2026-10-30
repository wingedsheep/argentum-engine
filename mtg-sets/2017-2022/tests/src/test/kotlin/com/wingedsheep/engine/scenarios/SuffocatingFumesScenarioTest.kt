package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Suffocating Fumes (IKO #100) — {2}{B} Instant.
 *
 * "Creatures your opponents control get -1/-1 until end of turn. Cycling {2}"
 */
class SuffocatingFumesScenarioTest : ScenarioTestBase() {

    init {
        test("shrinks only the opponents' creatures, killing their 1-toughness ones") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Suffocating Fumes")
                .withCardOnBattlefield(1, "Llanowar Elves")     // 1/1, yours — unaffected
                .withCardOnBattlefield(2, "Llanowar Elves")     // 1/1, opponent's — dies
                .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2, opponent's — becomes 1/1
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Suffocating Fumes").error shouldBe null
            game.resolveStack()

            withClue("the opponent's Llanowar Elves died as a 0/0") {
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
            withClue("your own Llanowar Elves is untouched") {
                game.isInGraveyard(1, "Llanowar Elves") shouldBe false
                val elves = game.findPermanent("Llanowar Elves")!!
                game.state.projectedState.getPower(elves) shouldBe 1
                game.state.projectedState.getToughness(elves) shouldBe 1
            }
            withClue("the opponent's Grizzly Bears is a 1/1 until end of turn") {
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 1
                game.state.projectedState.getToughness(bears) shouldBe 1
            }
        }

        test("cycling {2} draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Suffocating Fumes")
                .withCardInLibrary(1, "Swamp")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycleCard(1, "Suffocating Fumes").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Suffocating Fumes") shouldBe true
            game.isInHand(1, "Swamp") shouldBe true
        }
    }
}
