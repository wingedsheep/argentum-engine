package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Aviation Pioneer ({2}{U}, 1/2):
 * "When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying."
 */
class AviationPioneerScenarioTest : ScenarioTestBase() {

    init {
        context("Aviation Pioneer") {

            test("entering creates a 1/1 colorless flying Thopter artifact creature token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aviation Pioneer")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Aviation Pioneer").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Aviation Pioneer") shouldBe true
                val thopter = game.findPermanent("Thopter Token")
                withClue("the enter trigger made a Thopter") { thopter.shouldNotBeNull() }

                val projected = game.state.projectedState
                projected.getPower(thopter!!) shouldBe 1
                projected.getToughness(thopter) shouldBe 1
                projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                projected.isCreature(thopter) shouldBe true
                withClue("colorless") { projected.getColors(thopter).isEmpty() shouldBe true }
            }
        }
    }
}
