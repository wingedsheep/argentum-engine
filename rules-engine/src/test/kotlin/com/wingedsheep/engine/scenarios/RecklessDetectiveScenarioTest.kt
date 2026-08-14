package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Reckless Detective (MKM) — "Whenever this creature attacks, you may sacrifice an artifact or
 * discard a card. If you do, draw a card and this creature gets +2/+0 until end of turn."
 *
 * "**If** you do" (not "when you do") means the payoff runs inside the same resolution, so both
 * branches of the choice fold the draw and the pump into themselves. The tests pin that the 0/3
 * only swings for 2 when a cost was actually paid, and that the feasibility gates keep an
 * unpayable branch off the menu — including the case where *neither* branch can be paid and the
 * "you may" is never offered at all.
 */
class RecklessDetectiveScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun power(game: TestGame): Int =
        stateProjector.project(game.state).getPower(game.findPermanent("Reckless Detective")!!) ?: 0

    init {
        test("sacrificing an artifact draws a card and swings for 2") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Reckless Detective", summoningSickness = false)
                .withCardOnBattlefield(1, "Wrench")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)
            game.declareAttackers(mapOf("Reckless Detective" to 2)).error shouldBe null
            game.resolveStack()

            // Only the sacrifice branch is payable (empty hand), so the choice collapses to it.
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Wrench") shouldBe true
            game.handSize(1) shouldBe handBefore + 1
            power(game) shouldBe 2
        }

        test("declining draws nothing and leaves a 0/3") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Reckless Detective", summoningSickness = false)
                .withCardOnBattlefield(1, "Wrench")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)
            game.declareAttackers(mapOf("Reckless Detective" to 2)).error shouldBe null
            game.resolveStack()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wrench") shouldBe true
            game.handSize(1) shouldBe handBefore
            power(game) shouldBe 0
        }

        test("with no artifact the discard branch pays instead") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Reckless Detective", summoningSickness = false)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Savannah Lions")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Reckless Detective" to 2)).error shouldBe null
            game.resolveStack()

            game.answerYesNo(true).error shouldBe null
            // Two cards in hand, so the discard genuinely asks which one.
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Centaur Courser") shouldBe true
            power(game) shouldBe 2
        }

        test("with nothing to pay, the 'you may' is never offered") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Reckless Detective", summoningSickness = false)
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Reckless Detective" to 2)).error shouldBe null
            game.resolveStack()

            // No artifact, no cards in hand — neither branch is feasible, so no prompt appears
            // rather than a yes/no that would resolve into a free card.
            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe 0
            power(game) shouldBe 0
        }
    }
}
