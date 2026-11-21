package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Underworld Cerberus (THS #208) — the dies trigger: "exile it and each player returns all
 * creature cards from their graveyard to their hand." Cerberus must be exiled before the sweep,
 * so it doesn't come back to its owner's hand with the rest.
 */
class UnderworldCerberusScenarioTest : ScenarioTestBase() {
    init {
        test("when it dies it is exiled and every player's creature cards return to their owner's hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sip of Hemlock")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(2, "Underworld Cerberus")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sip of Hemlock", game.findPermanent("Underworld Cerberus")!!).error shouldBe null
            game.resolveStack()

            withClue("Cerberus is exiled, not returned to hand") {
                game.isInExile(2, "Underworld Cerberus") shouldBe true
                game.isInHand(2, "Underworld Cerberus") shouldBe false
            }
            withClue("each player's creature cards return to their own hand") {
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
            withClue("noncreature cards stay in the graveyard") {
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }
        }
    }
}
