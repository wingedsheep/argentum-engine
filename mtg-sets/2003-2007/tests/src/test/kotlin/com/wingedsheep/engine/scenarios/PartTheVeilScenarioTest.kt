package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Part the Veil (CHK #77) — "Return all creatures you control to their owner's hand."
 *
 * Every creature its caster controls goes home; the opponent's creatures and its caster's
 * noncreature permanents stay.
 */
class PartTheVeilScenarioTest : ScenarioTestBase() {

    init {
        context("Part the Veil") {

            test("returns only the caster's creatures") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Part the Veil")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Graceful Adept")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Part the Veil").error shouldBe null
                game.resolveStack()

                withClue("both of Alice's creatures are back in her hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Graceful Adept") shouldBe true
                }
                withClue("Bob's creature and Alice's lands stay") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.findPermanents("Island").size shouldBe 4
                }
            }
        }
    }
}
