package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Inferno Hellion — at the beginning of each end step, if it attacked or blocked this turn, its
 * owner shuffles it into their library.
 */
class InfernoHellionScenarioTest : ScenarioTestBase() {

    init {
        test("after attacking, it is shuffled into its owner's library at the end step") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Inferno Hellion")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Inferno Hellion" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("still on the battlefield after combat") {
                game.isOnBattlefield("Inferno Hellion") shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Inferno Hellion") shouldBe false
            game.findCardsInLibrary(1, "Inferno Hellion") shouldHaveSize 1
        }

        test("after blocking on the opponent's turn, it is shuffled into its owner's library") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Inferno Hellion")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Inferno Hellion" to listOf("Grizzly Bears"))).error shouldBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Inferno Hellion") shouldBe false
            withClue("goes to its owner's library, not the active player's") {
                game.findCardsInLibrary(2, "Inferno Hellion") shouldHaveSize 1
            }
        }

        test("a Hellion that neither attacked nor blocked stays on the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Inferno Hellion")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Inferno Hellion") shouldBe true
            game.findCardsInLibrary(1, "Inferno Hellion") shouldHaveSize 0
        }
    }
}
