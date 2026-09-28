package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Assimilate Essence (MOM #47) — "Counter target creature or battle spell unless its controller
 * pays {4}. If they do, you incubate 2."
 */
class AssimilateEssenceScenarioTest : ScenarioTestBase() {

    private fun board(opponentSpell: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Assimilate Essence")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(2, opponentSpell)
        .withLandsOnBattlefield(2, "Forest", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Assimilate Essence") {

            test("counters a creature spell whose controller cannot pay {4}, and does not incubate") {
                val game = board("Grizzly Bears")
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Assimilate Essence", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("the incubate rider only follows a payment") {
                    game.findPermanent("Incubator") shouldBe null
                }
            }

            test("a noncreature, nonbattle spell is not a legal target") {
                val game = board("Millstone")
                game.castSpell(2, "Millstone").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Assimilate Essence", "Millstone").error shouldNotBe null
            }
        }
    }
}
