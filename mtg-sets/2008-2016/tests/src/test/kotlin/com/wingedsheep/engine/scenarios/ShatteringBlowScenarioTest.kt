package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Shattering Blow (GTC) — exile target artifact. */
class ShatteringBlowScenarioTest : ScenarioTestBase() {
    init {
        test("exiles target artifact") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shattering Blow")
                .withCardOnBattlefield(2, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shattering Blow", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Ornithopter") shouldBe true
            game.isInGraveyard(2, "Ornithopter") shouldBe false
        }

        test("can't target a non-artifact creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shattering Blow")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shattering Blow", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
