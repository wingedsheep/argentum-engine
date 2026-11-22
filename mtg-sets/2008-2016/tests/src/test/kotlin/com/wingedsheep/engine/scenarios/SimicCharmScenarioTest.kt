package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Simic Charm (GTC) — choose one: +3/+3, hexproof for your permanents, or bounce a creature. */
class SimicCharmScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Wind Drake")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInHand(1, "Simic Charm")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("mode 1 gives target creature +3/+3 until end of turn") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpellWithMode(1, "Simic Charm", 0, bears)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 5
        }

        test("mode 2 gives only permanents you control hexproof") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val drake = game.findPermanent("Wind Drake")!!

            game.castSpellWithMode(1, "Simic Charm", 1).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            game.state.projectedState.hasKeyword(drake, Keyword.HEXPROOF) shouldBe false
        }

        test("mode 3 returns target creature to its owner's hand") {
            val game = board()
            val drake = game.findPermanent("Wind Drake")!!

            game.castSpellWithMode(1, "Simic Charm", 2, drake).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wind Drake") shouldBe false
            game.isInHand(2, "Wind Drake") shouldBe true
        }
    }
}
