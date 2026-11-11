package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Reclaiming Vines — destroys an artifact, enchantment or land, never a creature. */
class ReclaimingVinesScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Reclaiming Vines")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardOnBattlefield(2, "Island")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroys target land") {
            val game = board()
            game.castSpell(1, "Reclaiming Vines", game.findPermanent("Island")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Island") shouldBe true
        }

        test("can't target a creature") {
            val game = board()
            game.castSpell(1, "Reclaiming Vines", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
