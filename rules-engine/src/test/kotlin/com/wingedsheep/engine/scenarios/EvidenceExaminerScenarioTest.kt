package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Evidence Examiner (MKM #201) — {G}{U} 2/2 Merfolk Detective.
 *
 * "At the beginning of combat on your turn, you may collect evidence 4.
 *  Whenever you collect evidence, investigate."
 *
 * The two abilities are written as if unrelated and are wired that way — the collect emits an
 * evidence event, the payoff watches for one, and neither references the other. These tests prove
 * the chain actually closes, and that it closes only when a collection genuinely happened:
 *
 *  - accepting the beginning-of-combat collect exiles the graveyard **and** yields a Clue;
 *  - declining it leaves the graveyard intact and makes no Clue — a payoff hung off the *trigger*
 *    rather than off the collection would still fire here;
 *  - a graveyard that can't reach total mana value 4 is never even offered the choice
 *    (CR 701.59b), which is a stronger claim than "the player says no": there is no pending
 *    decision at all.
 */
class EvidenceExaminerScenarioTest : ScenarioTestBase() {

    private fun clues(game: TestGame): Int = game.findPermanents("Clue").size

    /**
     * Advance to the beginning-of-combat step *and drain the stack*. `passUntilPhase` stops at the
     * step's priority window with begin-of-step triggers queued but unresolved, so without the
     * `resolveStack` the collect-evidence prompt has not been raised yet.
     */
    private fun beginCombat(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        game.resolveStack()
    }

    init {
        test("collecting evidence at the beginning of combat feeds its own payoff") {
            val game = scenario()
                .withPlayers("Detective", "Opponent")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            beginCombat(game)

            // "You may collect evidence 4" — Centaur Courser (3) + Lightning Bolt (1) = 4 exactly.
            game.answerYesNo(true)
            game.resolveStack()
            if (game.getPendingDecision() != null) {
                game.selectCards(
                    game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).toList()
                )
            }
            game.resolveStack()

            withClue("the collection exiled the graveyard") {
                game.isInExile(1, "Centaur Courser") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
            withClue("and the unlinked payoff investigated off the back of it") {
                clues(game) shouldBe 1
            }
        }

        test("declining leaves the graveyard alone and makes no Clue") {
            val game = scenario()
                .withPlayers("Detective", "Opponent")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            beginCombat(game)
            game.answerYesNo(false)
            game.resolveStack()

            withClue("nothing was collected") {
                game.isInGraveyard(1, "Centaur Courser") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }
            withClue("so the payoff never fired — it watches collections, not the trigger") {
                clues(game) shouldBe 0
            }
        }

        test("CR 701.59b — a graveyard that can't reach 4 is never asked") {
            val game = scenario()
                .withPlayers("Detective", "Opponent")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            beginCombat(game)

            withClue("a total mana value of 1 can't reach 4, so no choice is offered") {
                game.getPendingDecision() shouldBe null
            }
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            clues(game) shouldBe 0
        }
    }
}
