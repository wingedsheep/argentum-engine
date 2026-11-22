package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lavinia of the Tenth (DGM #80) — "When Lavinia enters, detain each nonland permanent your
 * opponents control with mana value 4 or less."
 *
 * The opponent controls Hill Giant (MV 4), Serra Angel (MV 5), Mind Stone (MV 2, a noncreature
 * artifact with an activated ability) and a Plains. The group detain must reach the noncreature
 * artifact too, and spare the MV-5 creature and the land.
 */
class LaviniaOfTheTenthScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Lavinia of the Tenth")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Serra Angel")
        .withCardOnBattlefield(2, "Mind Stone")
        .withLandsOnBattlefield(2, "Plains", 1)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opponentCanActivate(name: String): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(2).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    init {
        test("detains the opponent's nonland permanents with mana value 4 or less") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val angel = game.findPermanent("Serra Angel")!!
            val stone = game.findPermanent("Mind Stone")!!

            val cast = game.castSpell(1, "Lavinia of the Tenth")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack() // creature spell, then the ETB trigger

            withClue("Hill Giant (MV 4) can't attack or block") {
                game.state.projectedState.cantAttack(giant) shouldBe true
                game.state.projectedState.cantBlock(giant) shouldBe true
            }
            withClue("Mind Stone (MV 2, noncreature) is detained too") {
                game.state.projectedState.cantAttack(stone) shouldBe true
            }
            withClue("Serra Angel (MV 5) is untouched") {
                game.state.projectedState.cantAttack(angel) shouldBe false
                game.state.projectedState.cantBlock(angel) shouldBe false
            }

            game.passPriority() // opponent receives priority on an empty stack
            withClue("Mind Stone's activated abilities can't be activated") {
                game.opponentCanActivate("Mind Stone") shouldBe false
            }
        }

        test("without Lavinia, the opponent can activate Mind Stone") {
            val game = board()
            game.passPriority()
            game.opponentCanActivate("Mind Stone") shouldBe true
        }
    }
}
