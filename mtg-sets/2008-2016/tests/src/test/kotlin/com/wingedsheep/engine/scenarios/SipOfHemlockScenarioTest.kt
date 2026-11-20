package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sip of Hemlock (THS #106) — {4}{B}{B} Sorcery.
 * "Destroy target creature. Its controller loses 2 life."
 */
class SipOfHemlockScenarioTest : ScenarioTestBase() {
    init {
        test("destroys the target creature and its controller loses 2 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sip of Hemlock")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sip of Hemlock", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            withClue("the creature is destroyed") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
            withClue("its controller loses 2 life; the caster loses none") {
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
