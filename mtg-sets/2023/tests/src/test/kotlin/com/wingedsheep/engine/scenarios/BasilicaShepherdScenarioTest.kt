package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Basilica Shepherd (ONE #4) — {3}{W}{W} 3/3 Phyrexian Angel, flying.
 * "When this creature enters, create two 1/1 colorless Phyrexian Mite artifact creature tokens
 *  with toxic 1 and 'This token can't block.'"
 */
class BasilicaShepherdScenarioTest : ScenarioTestBase() {
    init {
        test("entering creates two Phyrexian Mite tokens for its controller") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Basilica Shepherd")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Basilica Shepherd").error shouldBe null
            game.resolveStack()

            val shepherd = game.findPermanent("Basilica Shepherd")
            shepherd shouldNotBe null
            game.state.projectedState.hasKeyword(shepherd!!, Keyword.FLYING) shouldBe true

            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 2
            val projected = game.state.projectedState
            mites.forEach { mite ->
                projected.getController(mite) shouldBe game.player1Id
                projected.getPower(mite) shouldBe 1
                projected.getToughness(mite) shouldBe 1
                projected.hasType(mite, "ARTIFACT") shouldBe true
                projected.hasKeyword(mite, Keyword.TOXIC) shouldBe true
            }
        }
    }
}
