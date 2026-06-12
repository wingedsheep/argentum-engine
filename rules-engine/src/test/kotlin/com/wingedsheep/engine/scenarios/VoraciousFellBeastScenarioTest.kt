package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Voracious Fell Beast — "When this creature enters, each opponent sacrifices a creature of their
 * choice. Create a Food token for each creature sacrificed this way." Exercises the new
 * `DynamicAmount.PermanentsSacrificedThisWay`, which reads the resolving context's sacrificedPermanents.
 */
class VoraciousFellBeastScenarioTest : ScenarioTestBase() {

    private fun gameWithOpponentCreatures(vararg opponentCreatures: String) = scenario()
        .withPlayers()
        .withCardInHand(1, "Voracious Fell Beast")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .apply { opponentCreatures.forEach { withCardOnBattlefield(2, it) } }
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("opponent sacrifices their creature and a Food is made for it") {
            val game = gameWithOpponentCreatures("Grizzly Bears")

            game.castSpell(1, "Voracious Fell Beast").error shouldBe null
            game.resolveStack()

            // The lone opposing creature is auto-sacrificed, and one Food is created for it.
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.findPermanents("Food").size shouldBe 1
        }

        test("no creature to sacrifice: no Food is created") {
            val game = gameWithOpponentCreatures() // opponent controls no creatures

            game.castSpell(1, "Voracious Fell Beast").error shouldBe null
            game.resolveStack()

            game.findPermanents("Food").size shouldBe 0
        }
    }
}
