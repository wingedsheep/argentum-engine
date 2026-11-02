package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sleep — "Tap all creatures target player controls. Those creatures don't untap during that
 * player's next untap step." Every creature they controlled as it resolved is held, including one
 * that was already tapped (2018-07-13 ruling); their lands untap; the lock lasts one untap step.
 */
class SleepScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(name: String) =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    /** On to Bob's next upkeep — just past his next untap step. */
    private fun TestGame.toBobsNextUpkeep() {
        do {
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        } while (state.activePlayerId != player2Id)
    }

    init {
        test("Bob's creatures stay tapped through his next untap step, then untap") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Sleep")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Sleep", 2).error shouldBe null
            game.resolveStack()
            withClue("Sleep taps Bob's untapped creature") { game.isTapped("Grizzly Bears") shouldBe true }

            game.toBobsNextUpkeep()
            withClue("both creatures held — the one already tapped too — but the land untaps") {
                game.isTapped("Grizzly Bears") shouldBe true
                game.isTapped("Hill Giant") shouldBe true
                game.isTapped("Forest") shouldBe false
            }

            game.toBobsNextUpkeep()
            withClue("the lock covered one untap step only") {
                game.isTapped("Grizzly Bears") shouldBe false
                game.isTapped("Hill Giant") shouldBe false
            }
        }
    }
}
