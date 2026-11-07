package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Exultant Cultist (EMN #59) — {2}{U} Creature — Human Wizard, 2/2.
 *
 *   When this creature dies, draw a card.
 */
class ExultantCultistScenarioTest : ScenarioTestBase() {

    init {
        test("when it dies its controller draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Exultant Cultist")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            val cultist = game.findPermanent("Exultant Cultist")!!
            game.castSpell(2, "Lightning Bolt", cultist).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Exultant Cultist") shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }
    }
}
