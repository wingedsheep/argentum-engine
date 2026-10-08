package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mishra's Onslaught (BRO #143) — choose one: create two 1/1 colorless Soldier artifact creature
 * tokens, or creatures you control get +2/+0 until end of turn.
 */
class MishrasOnslaughtScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Mishra's Onslaught")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("mode one creates two Soldier artifact creature tokens") {
            val game = game()
            game.castSpellWithMode(1, "Mishra's Onslaught", 0).error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Soldier Token")
            withClue("two Soldier tokens") { tokens.size shouldBe 2 }
            tokens.forEach { game.state.projectedState.hasType(it, "ARTIFACT") shouldBe true }
        }

        test("mode two gives only your creatures +2/+0") {
            val game = game()
            game.castSpellWithMode(1, "Mishra's Onslaught", 1).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            withClue("Grizzly Bears is 4/2") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 2
            }
            withClue("the opponent's Hill Giant is unchanged") { projected.getPower(giant) shouldBe 3 }
        }
    }
}
