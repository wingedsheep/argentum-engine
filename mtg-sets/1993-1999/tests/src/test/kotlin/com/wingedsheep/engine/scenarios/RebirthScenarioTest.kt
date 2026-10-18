package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rebirth {3}{G}{G}{G} — Sorcery:
 * "Each player may ante the top card of their library. If a player does, that player's life total
 * becomes 20."
 */
class RebirthScenarioTest : ScenarioTestBase() {

    private fun setUp(opponentLibrary: List<String> = listOf("Plains", "Serra Angel")) = scenario()
        .withPlayers("Player1", "Player2")
        .playingForAnte()
        .withCardInHand(1, "Rebirth")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Shivan Dragon")
        .withCardInLibrary(1, "Forest")
        .apply { opponentLibrary.forEach { withCardInLibrary(2, it) } }
        .withLifeTotal(1, 4)
        .withLifeTotal(2, 7)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a player who antes their top card goes to 20 life; one who declines keeps theirs") {
            val game = setUp()
            game.castSpell(1, "Rebirth").error shouldBe null
            game.resolveStack()

            withClue("the active player decides first") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)
            game.answerYesNo(false)

            withClue("player 1 anted their own top card and is at 20") {
                game.isInAnte(1, "Shivan Dragon") shouldBe true
                game.librarySize(1) shouldBe 1
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("player 2 declined: library and life untouched") {
                game.anteSize(2) shouldBe 0
                game.librarySize(2) shouldBe 2
                game.getLifeTotal(2) shouldBe 7
            }
        }

        test("a player with an empty library isn't asked and keeps their life total") {
            val game = setUp(opponentLibrary = emptyList())
            game.castSpell(1, "Rebirth").error shouldBe null
            game.resolveStack()

            game.answerYesNo(true)

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 7
        }
    }
}
