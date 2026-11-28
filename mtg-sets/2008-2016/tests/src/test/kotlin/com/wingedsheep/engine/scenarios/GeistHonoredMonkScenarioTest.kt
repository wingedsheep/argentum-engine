package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Geist-Honored Monk (ISD #17) — {3}{W}{W} * / * Human Monk.
 *
 *   Vigilance
 *   Geist-Honored Monk's power and toughness are each equal to the number of creatures you control.
 *   When this creature enters, create two 1/1 white Spirit creature tokens with flying.
 */
class GeistHonoredMonkScenarioTest : ScenarioTestBase() {

    init {
        test("enters with two flying Spirits and counts only creatures you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Geist-Honored Monk")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Geist-Honored Monk")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val spirits = game.findAllPermanents("Spirit Token")
            spirits.size shouldBe 2
            spirits.forEach { game.state.projectedState.hasKeyword(it, Keyword.FLYING) shouldBe true }

            val monk = game.findPermanent("Geist-Honored Monk")!!
            withClue("Monk + Grizzly Bears + two Spirits = 4; the opponent's Hill Giant doesn't count") {
                game.state.projectedState.getPower(monk) shouldBe 4
                game.state.projectedState.getToughness(monk) shouldBe 4
            }
            game.state.projectedState.hasKeyword(monk, Keyword.VIGILANCE) shouldBe true
        }
    }
}
