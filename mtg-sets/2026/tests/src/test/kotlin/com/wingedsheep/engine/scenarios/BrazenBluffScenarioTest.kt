package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Brazen Bluff (TRK #140) — {R}{R} Instant:
 *   Choose target noncreature spell an opponent controls. That player may exile it. If they
 *   don't, you may cast a noncreature spell from your hand without paying its mana cost.
 *
 * Focus: the yes/no belongs to the targeted spell's controller, and declining hands the Bluff's
 * caster a free noncreature cast during resolution.
 */
class BrazenBluffScenarioTest : ScenarioTestBase() {

    init {
        context("Brazen Bluff") {

            fun board(): TestGame = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Brazen Bluff")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun bluffTheBolt(game: TestGame) {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Brazen Bluff", "Lightning Bolt").error shouldBe null
                game.resolveStack()
            }

            test("the spell's controller may exile it, and then no free cast is offered") {
                val game = board()
                bluffTheBolt(game)

                val decision = game.state.pendingDecision
                (decision is YesNoDecision) shouldBe true
                decision!!.playerId shouldBe game.player2Id
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Lightning Bolt") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.state.pendingDecision shouldBe null
            }

            test("if they don't, the caster casts a noncreature spell from hand for free") {
                val game = board()
                bluffTheBolt(game)

                game.answerYesNo(false).error shouldBe null
                (game.state.pendingDecision is SelectCardsDecision) shouldBe true
                game.selectCards(listOf(game.findCardsInHand(1, "Lightning Bolt").single())).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 17
                game.isInHand(1, "Lightning Bolt") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
