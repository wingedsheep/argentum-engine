package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Divine Offering (LEG): "Destroy target artifact. You gain life equal to its mana value."
 *
 * The artifact is already in the graveyard when the life is gained, so the mana value is read
 * from the destroyed card.
 */
class DivineOfferingScenarioTest : ScenarioTestBase() {
    init {
        test("destroys the artifact and gains life equal to its mana value") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Divine Offering")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Jayemdae Tome")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Jayemdae Tome")!!
            game.castSpell(1, "Divine Offering", tome).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Jayemdae Tome") shouldBe true
            withClue("Jayemdae Tome has mana value 4") {
                game.getLifeTotal(1) shouldBe 24
            }
        }
    }
}
