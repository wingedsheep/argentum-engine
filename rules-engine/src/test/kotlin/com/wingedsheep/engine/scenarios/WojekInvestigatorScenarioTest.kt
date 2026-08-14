package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.WojekInvestigator
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wojek Investigator (MKM) — "At the beginning of your upkeep, investigate once for each opponent
 * who has more cards in hand than you."
 *
 * Two things are under test and both live in the count rather than in the trigger:
 *
 * 1. **The comparison is strict and controller-relative.** `DynamicAmount.CountPlayersWith` rebinds
 *    the resolution context's controller to each candidate opponent, so `Player.You` inside the
 *    condition is *that opponent*; the ability's own controller is reached with
 *    [com.wingedsheep.sdk.scripting.references.Player.ControllerOfSource], which reads control off
 *    the Investigator itself and so survives the rebind. Get that wrong and the comparison collapses
 *    to "opponent vs. opponent" — always false — or to "you vs. you".
 * 2. **Zero is a legal count.** No qualifying opponent means no Clue at all, not one.
 *
 * The board starts on the Investigator's controller's UNTAP and advances into their UPKEEP, so the
 * hands are exactly what each test sets — no draw step or cleanup discard has run yet.
 */
class WojekInvestigatorScenarioTest : ScenarioTestBase() {

    /**
     * Player 1 controls Wojek Investigator and it is player 1's turn. Deals [myHand] cards to
     * player 1 and [theirHand] to player 2, then walks into player 1's upkeep and resolves.
     */
    private fun runUpkeep(myHand: Int, theirHand: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Wojek Investigator", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        repeat(myHand) { builder.withCardInHand(1, "Mountain") }
        repeat(theirHand) { builder.withCardInHand(2, "Mountain") }
        builder.withCardInLibrary(1, "Mountain")
        builder.withCardInLibrary(2, "Mountain")
        val game = builder.build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        cardRegistry.register(WojekInvestigator)

        context("Wojek Investigator") {

            test("an opponent with more cards in hand than you yields one Clue") {
                val game = runUpkeep(myHand = 1, theirHand = 3)
                withClue("3 > 1 — the one opponent qualifies, so investigate once") {
                    game.findPermanents("Clue").size shouldBe 1
                }
            }

            test("an opponent with an equal hand yields nothing — the comparison is strict") {
                val game = runUpkeep(myHand = 3, theirHand = 3)
                withClue("\"more cards in hand than you\" is > , not >=") {
                    game.findPermanents("Clue").size shouldBe 0
                }
            }

            test("an opponent with a smaller hand yields nothing") {
                val game = runUpkeep(myHand = 4, theirHand = 1)
                game.findPermanents("Clue").size shouldBe 0
            }

            test("the comparison reads the controller's hand, not the candidate's own") {
                // Both hands are empty except the opponent's single card. If the inner comparison
                // had collapsed to "candidate vs. candidate" it would be 1 > 1 = false and no Clue
                // would appear; the correct reading is 1 > 0 = true.
                val game = runUpkeep(myHand = 0, theirHand = 1)
                game.findPermanents("Clue").size shouldBe 1
            }
        }
    }
}
