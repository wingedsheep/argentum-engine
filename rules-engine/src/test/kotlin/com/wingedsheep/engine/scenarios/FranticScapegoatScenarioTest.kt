package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Frantic Scapegoat (MKM #126) — {R} 1/1 Goat with haste.
 *
 * "When this creature enters, suspect it.
 *  Whenever one or more other creatures you control enter, if this creature is suspected, you may
 *  suspect one of the other creatures. If you do, this creature is no longer suspected."
 *
 * The card is a *transfer*, and the three tests below pin down the three ways a naive implementation
 * gets that wrong:
 *
 *  1. the happy path has to move the designation both ways in one resolution — the new creature
 *     becomes suspected **and** the Goat stops being;
 *  2. declining the "may" must leave the Goat suspected. Gating the un-suspect on the *ability
 *     resolving* rather than on a creature actually being chosen would clear it either way, which
 *     is the exact thing "If you do" forbids;
 *  3. once the suspicion has been handed off, the intervening-if (CR 603.4) must stop the ability
 *     going on the stack at all. Without it the Goat would keep prompting on every creature that
 *     enters for the rest of the game;
 *  4. the batch is scoped to creatures *you control*. A filter missing that scope passes all three
 *     tests above — they only ever deploy creatures on one side — so the opponent case is tested
 *     explicitly.
 */
class FranticScapegoatScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("it enters suspected and hands the suspicion to a creature that enters later") {
            val game = scenario()
                .withPlayers("Sleuth", "Opponent")
                .withCardInHand(1, "Frantic Scapegoat")
                .withCardInHand(1, "Goblin Guide")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Frantic Scapegoat").error shouldBe null
            game.resolveStack()
            val goat = game.findPermanent("Frantic Scapegoat")!!

            withClue("the enters trigger suspects the Goat itself (CR 701.60a)") {
                val projected = projector.project(game.state)
                projected.isSuspected(goat) shouldBe true
                projected.hasKeyword(goat, Keyword.MENACE) shouldBe true
                projected.cantBlock(goat) shouldBe true
            }

            game.castSpell(1, "Goblin Guide").error shouldBe null
            game.resolveStack()
            val guide = game.findPermanent("Goblin Guide")!!

            withClue("the batch trigger pauses to offer the creatures that entered") {
                game.getPendingDecision() shouldNotBe null
            }
            game.selectCards(listOf(guide))
            game.resolveStack()

            val projected = projector.project(game.state)
            withClue("the chosen creature takes the blame") {
                projected.isSuspected(guide) shouldBe true
                projected.hasKeyword(guide, Keyword.MENACE) shouldBe true
                projected.cantBlock(guide) shouldBe true
            }
            withClue("and the Goat sheds both the designation and everything it granted") {
                projected.isSuspected(goat) shouldBe false
                projected.hasKeyword(goat, Keyword.MENACE) shouldBe false
                projected.cantBlock(goat) shouldBe false
            }
        }

        test("declining the may leaves the Goat suspected — 'If you do' is linked to the choice") {
            val game = scenario()
                .withPlayers("Sleuth", "Opponent")
                .withCardInHand(1, "Frantic Scapegoat")
                .withCardInHand(1, "Goblin Guide")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Frantic Scapegoat").error shouldBe null
            game.resolveStack()
            val goat = game.findPermanent("Frantic Scapegoat")!!

            game.castSpell(1, "Goblin Guide").error shouldBe null
            game.resolveStack()
            val guide = game.findPermanent("Goblin Guide")!!

            // Select nothing — the "up to 1" selection is how the "you may" declines.
            game.selectCards(emptyList())
            game.resolveStack()

            val projected = projector.project(game.state)
            withClue("no creature was chosen, so nothing was suspected") {
                projected.isSuspected(guide) shouldBe false
            }
            withClue("and the Goat keeps its own suspicion") {
                projected.isSuspected(goat) shouldBe true
                projected.cantBlock(goat) shouldBe true
            }
        }

        test("an opponent's creature entering is not 'a creature you control'") {
            val game = scenario()
                .withPlayers("Sleuth", "Opponent")
                .withCardInHand(1, "Frantic Scapegoat")
                .withCardInHand(2, "Goblin Guide")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(2, "Mountain", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Frantic Scapegoat").error shouldBe null
            game.resolveStack()
            val goat = game.findPermanent("Frantic Scapegoat")!!
            projector.project(game.state).isSuspected(goat) shouldBe true

            // Hand the turn over so the opponent can deploy a creature of their own.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            game.castSpell(2, "Goblin Guide").error shouldBe null
            game.resolveStack()

            withClue("the filter is scoped to creatures you control, so nothing triggers") {
                game.getPendingDecision() shouldBe null
            }
            withClue("and the Goat is left holding the suspicion") {
                projector.project(game.state).isSuspected(goat) shouldBe true
            }
        }

        test("once unsuspected the intervening-if stops it triggering again") {
            val game = scenario()
                .withPlayers("Sleuth", "Opponent")
                .withCardInHand(1, "Frantic Scapegoat")
                .withCardsInHand(1, "Goblin Guide", 2)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Frantic Scapegoat").error shouldBe null
            game.resolveStack()
            val goat = game.findPermanent("Frantic Scapegoat")!!

            // First Guide: hand the suspicion off.
            game.castSpell(1, "Goblin Guide").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findPermanent("Goblin Guide")!!))
            game.resolveStack()
            projector.project(game.state).isSuspected(goat) shouldBe false

            // Second Guide: the Goat is no longer suspected, so the ability never goes on the stack.
            game.castSpell(1, "Goblin Guide").error shouldBe null
            game.resolveStack()

            withClue("CR 603.4 — the intervening-if failed, so there is nothing to choose") {
                game.getPendingDecision() shouldBe null
            }
            withClue("and the Goat stays unsuspected") {
                projector.project(game.state).isSuspected(goat) shouldBe false
            }
        }
    }
}
