package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MirrodinAvengedScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Mirrodin Avenged")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("an undamaged creature is not a legal target") {
            val game = board()
            game.castSpell(1, "Mirrodin Avenged", game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }

        test("destroys a creature that was dealt damage this turn and draws a card") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Shock", giant).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe false

            game.castSpell(1, "Mirrodin Avenged", giant).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }
}
