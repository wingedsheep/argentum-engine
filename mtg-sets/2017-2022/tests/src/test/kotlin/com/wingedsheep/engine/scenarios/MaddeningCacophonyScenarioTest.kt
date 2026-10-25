package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maddening Cacophony (ZNR #67).
 *
 * "Each opponent mills eight cards. If this spell was kicked, instead each opponent mills half their
 * library, rounded up." The kicked count is per opponent: it must be read off the *opponent's*
 * library inside the per-player iteration, not the caster's — so the two libraries here differ in size.
 */
class MaddeningCacophonyScenarioTest : ScenarioTestBase() {

    private fun board(opponentLibrary: Int, yourLibrary: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Maddening Cacophony")
            .withLandsOnBattlefield(1, "Island", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(opponentLibrary) { builder.withCardInLibrary(2, "Grizzly Bears") }
        repeat(yourLibrary) { builder.withCardInLibrary(1, "Grizzly Bears") }
        return builder.build()
    }

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Maddening Cacophony").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    init {
        test("unkicked: each opponent mills eight cards") {
            val game = board(opponentLibrary = 12, yourLibrary = 3)
            game.cast(kicked = false)

            game.librarySize(2) shouldBe 4
            game.graveyardSize(2) shouldBe 8
            withClue("the caster mills nothing") { game.librarySize(1) shouldBe 3 }
        }

        test("kicked: the opponent mills half of their own library, rounded up") {
            val game = board(opponentLibrary = 7, yourLibrary = 2)
            game.cast(kicked = true)

            withClue("half of 7 rounded up is 4 — read off the opponent's library, not the caster's") {
                game.graveyardSize(2) shouldBe 4
                game.librarySize(2) shouldBe 3
            }
            game.librarySize(1) shouldBe 2
        }

        test("kicked against a 20-card library mills 10, not 8") {
            val game = board(opponentLibrary = 20, yourLibrary = 1)
            game.cast(kicked = true)

            game.librarySize(2) shouldBe 10
        }
    }
}
