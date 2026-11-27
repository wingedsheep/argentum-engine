package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Triumph of Ferocity (AVR #198) — {2}{G} Enchantment.
 *
 *   At the beginning of your upkeep, draw a card if you control the creature with the greatest power
 *   or tied for the greatest power.
 *
 * Covers the resolution-time condition: strictly greatest draws, a tie with an opposing creature draws,
 * a bigger opposing creature or controlling no creature doesn't — and the ability still triggers then.
 */
class TriumphOfFerocityScenarioTest : ScenarioTestBase() {

    private fun runUpkeep(yours: List<String>, theirs: List<String>): Pair<Int, Int> {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Triumph of Ferocity")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        yours.forEach { builder = builder.withCardOnBattlefield(1, it) }
        theirs.forEach { builder = builder.withCardOnBattlefield(2, it) }
        val game = builder.build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.activePlayerId shouldBe game.player1Id
        val stackSize = game.state.stack.size
        val before = game.handSize(1)
        game.resolveStack()
        return stackSize to (game.handSize(1) - before)
    }

    init {
        context("Triumph of Ferocity") {

            test("controlling the strictly greatest-power creature draws a card") {
                val (stack, drawn) = runUpkeep(listOf("Craw Wurm"), listOf("Grizzly Bears"))
                stack shouldBe 1
                drawn shouldBe 1
            }

            test("a tie with an opposing creature still draws") {
                val (stack, drawn) = runUpkeep(listOf("Runeclaw Bear"), listOf("Grizzly Bears"))
                stack shouldBe 1
                drawn shouldBe 1
            }

            test("an opposing creature with greater power: it triggers but draws nothing") {
                val (stack, drawn) = runUpkeep(listOf("Grizzly Bears"), listOf("Craw Wurm"))
                withClue("the ability triggers regardless (ruling)") { stack shouldBe 1 }
                drawn shouldBe 0
            }

            test("controlling no creature draws nothing") {
                val (_, drawn) = runUpkeep(emptyList(), listOf("Grizzly Bears"))
                drawn shouldBe 0
            }
        }
    }
}
