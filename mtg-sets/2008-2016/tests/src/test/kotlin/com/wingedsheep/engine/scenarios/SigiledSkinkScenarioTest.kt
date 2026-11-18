package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sigiled Skink (JOU #111) — {1}{R} Creature — Lizard 2/1.
 *
 *   Whenever this creature attacks, scry 1.
 */
class SigiledSkinkScenarioTest : ScenarioTestBase() {
    init {
        test("attacking scries 1, and the scried card can go to the bottom") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Sigiled Skink")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val top = game.state.getLibrary(game.player1Id).first()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sigiled Skink" to 2)).error shouldBe null
            game.resolveStack()

            val scry = withClue("the attack trigger asks which card to put on the bottom") {
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            }
            withClue("scry 1 looks at exactly the top card") {
                scry.options shouldBe listOf(top)
            }
            game.selectCards(listOf(top)).error shouldBe null

            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }
    }
}
