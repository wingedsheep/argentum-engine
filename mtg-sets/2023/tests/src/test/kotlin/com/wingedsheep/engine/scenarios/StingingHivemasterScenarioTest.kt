package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stinging Hivemaster (ONE #110) — {2}{B} 3/2 Phyrexian Warlock, toxic 1.
 *
 * "When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with
 *  toxic 1 and 'This token can't block.'"
 */
class StingingHivemasterScenarioTest : ScenarioTestBase() {

    init {
        test("has toxic and leaves a Phyrexian Mite when it dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stinging Hivemaster")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val hivemaster = game.findPermanent("Stinging Hivemaster")!!
            game.state.projectedState.hasKeyword(hivemaster, Keyword.TOXIC) shouldBe true

            game.castSpell(1, "Shock", hivemaster).error shouldBe null
            game.resolveStack()

            game.findPermanent("Stinging Hivemaster") shouldBe null
            val mite = game.findPermanent("Phyrexian Mite")
            mite shouldNotBe null
            game.state.projectedState.getController(mite!!) shouldBe game.player1Id
            game.state.projectedState.hasKeyword(mite, Keyword.TOXIC) shouldBe true
        }
    }
}
