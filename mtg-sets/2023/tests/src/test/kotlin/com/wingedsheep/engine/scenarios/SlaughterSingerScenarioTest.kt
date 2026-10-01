package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Slaughter Singer (ONE #216) — {G}{W} 2/2 Phyrexian Cleric, toxic 2.
 *
 * "Whenever another creature you control with toxic attacks, it gets +1/+1 until end of turn."
 */
class SlaughterSingerScenarioTest : ScenarioTestBase() {

    init {
        test("another attacking toxic creature gets +1/+1; Singer itself and non-toxic attackers don't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Slaughter Singer")
                .withCardOnBattlefield(1, "Crawling Chorus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.state.projectedState.hasKeyword(game.findPermanent("Slaughter Singer")!!, Keyword.TOXIC) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Slaughter Singer" to 2, "Crawling Chorus" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val chorus = game.findPermanent("Crawling Chorus")!!
            withClue("the attacking toxic creature is pumped") {
                projected.getPower(chorus) shouldBe 2
                projected.getToughness(chorus) shouldBe 2
            }
            withClue("Singer doesn't pump itself") {
                projected.getPower(game.findPermanent("Slaughter Singer")!!) shouldBe 2
            }
            withClue("a creature without toxic isn't pumped") {
                projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the pump ends at end of turn") {
                game.state.projectedState.getPower(chorus) shouldBe 1
            }
        }
    }
}
