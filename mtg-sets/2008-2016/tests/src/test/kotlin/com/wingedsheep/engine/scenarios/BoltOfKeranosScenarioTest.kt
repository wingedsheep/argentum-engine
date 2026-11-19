package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bolt of Keranos (BNG #89) — "Bolt of Keranos deals 3 damage to any target. Scry 1."
 */
class BoltOfKeranosScenarioTest : ScenarioTestBase() {

    init {
        test("deals 3 damage to a creature, then scries 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Bolt of Keranos")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val top = game.state.getLibrary(game.player1Id).first()
            game.castSpell(1, "Bolt of Keranos", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            withClue("the 3/3 died to the 3 damage") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }
    }
}
