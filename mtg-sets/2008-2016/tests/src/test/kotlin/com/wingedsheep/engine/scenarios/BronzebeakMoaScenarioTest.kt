package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bronzebeak Moa (DGM #60) — {2}{G}{W} 2/2 Creature — Bird.
 *
 *   Whenever another creature you control enters, this creature gets +3/+3 until end of turn.
 */
class BronzebeakMoaScenarioTest : ScenarioTestBase() {
    init {
        test("another creature entering under your control pumps it +3/+3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bronzebeak Moa")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Grizzly Bears")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val moa = game.findPermanent("Bronzebeak Moa")!!
            game.state.projectedState.getPower(moa) shouldBe 5
            game.state.projectedState.getToughness(moa) shouldBe 5
        }

        test("an opponent's creature entering doesn't pump it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bronzebeak Moa")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            val moa = game.findPermanent("Bronzebeak Moa")!!
            game.state.projectedState.getPower(moa) shouldBe 2
            game.state.projectedState.getToughness(moa) shouldBe 2
        }
    }
}
