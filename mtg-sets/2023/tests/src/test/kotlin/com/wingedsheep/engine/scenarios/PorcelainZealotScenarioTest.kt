package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Porcelain Zealot (ONE #30) — {3}{W} 2/3 Creature — Phyrexian Soldier.
 *
 * "At the beginning of combat on your turn, target creature you control gets +1/+1 until end of
 * turn. If that creature has toxic, instead it gets +2/+2 until end of turn."
 */
class PorcelainZealotScenarioTest : ScenarioTestBase() {

    init {
        test("a creature without toxic gets +1/+1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Porcelain Zealot")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(bears))
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("a creature with toxic instead gets +2/+2, not both") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Porcelain Zealot")
                .withCardOnBattlefield(1, "Crawling Chorus")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chorus = game.findPermanent("Crawling Chorus")!!
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(chorus))
            game.resolveStack()

            withClue("Crawling Chorus is a 1/1 with toxic 1 — +2/+2 replaces the +1/+1") {
                game.state.projectedState.getPower(chorus) shouldBe 3
                game.state.projectedState.getToughness(chorus) shouldBe 3
            }
        }

        test("does not trigger on the opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Porcelain Zealot")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.state.pendingDecision shouldBe null
            game.state.stack.size shouldBe 0
        }
    }
}
