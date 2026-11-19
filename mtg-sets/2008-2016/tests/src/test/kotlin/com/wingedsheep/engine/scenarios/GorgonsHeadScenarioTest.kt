package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gorgon's Head (BNG #158) — "Equipped creature has deathtouch. Equip {2}"
 */
class GorgonsHeadScenarioTest : ScenarioTestBase() {
    init {
        val equipId = cardRegistry.getCard("Gorgon's Head")!!.script.activatedAbilities.single().id

        test("equip {2} gives the equipped creature deathtouch") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Gorgon's Head")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val head = game.findPermanent("Gorgon's Head")!!

            withClue("unattached, the Head grants nothing") {
                game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
            }

            val result = game.execute(
                ActivateAbility(game.player1Id, head, equipId, listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("equip should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
        }
    }
}
