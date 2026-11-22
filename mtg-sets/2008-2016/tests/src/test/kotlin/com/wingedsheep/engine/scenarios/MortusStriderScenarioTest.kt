package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Mortus Strider (GTC) — when it dies, return it to its owner's hand. */
class MortusStriderScenarioTest : ScenarioTestBase() {
    init {
        test("returns to its owner's hand when it dies") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Mortus Strider")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Murder", game.findPermanent("Mortus Strider")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Mortus Strider") shouldBe false
            game.isInGraveyard(2, "Mortus Strider") shouldBe false
            game.isInHand(2, "Mortus Strider") shouldBe true
        }
    }
}
