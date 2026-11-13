package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Aligned Hedron Network — "When this artifact enters, exile all creatures with power 5 or greater
 * until this artifact leaves the battlefield. (Those creatures return under their owners' control.)"
 */
class AlignedHedronNetworkScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Aligned Hedron Network")
        .withCardInHand(1, "Shatter")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Craw Wurm")                      // 6/4, your own
        .withCardOnBattlefield(2, "Craw Wurm")                      // 6/4, theirs
        .withCardOnBattlefield(2, "Hill Giant")                     // 3/3, stays
        .withCardOnBattlefield(2, "Craw Wurm", isToken = true)      // token, ceases to exist
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiles every creature with power 5 or greater, whoever controls it, until it leaves") {
            val game = board()
            game.castSpell(1, "Aligned Hedron Network").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.findAllPermanents("Craw Wurm").size shouldBe 0
            game.isInExile(1, "Craw Wurm") shouldBe true
            game.isInExile(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Aligned Hedron Network") shouldBe true

            val network = game.findPermanent("Aligned Hedron Network")!!
            game.castSpell(1, "Shatter", network).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            // Both cards return at once, each under its owner's control; the token is gone for good.
            game.isInGraveyard(1, "Aligned Hedron Network") shouldBe true
            val wurms = game.findAllPermanents("Craw Wurm")
            wurms.size shouldBe 2
            wurms.map { game.state.projectedState.getController(it) }.toSet() shouldBe
                setOf(game.player1Id, game.player2Id)
            game.state.stack.size shouldBe 0
        }

        test("power is checked as the ability resolves: a creature pumped to 5 or more is exiled too") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Giant Growth", giant).error shouldBe null // 3/3 -> 6/6 until end of turn
            game.payIfAsked()
            game.resolveStack()
            game.state.projectedState.getPower(giant) shouldBe 6

            game.castSpell(1, "Aligned Hedron Network").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("nothing is exiled if the Network leaves before its enters ability resolves") {
            val game = board()
            game.castSpell(1, "Aligned Hedron Network").error shouldBe null
            game.payIfAsked()
            game.passPriority()
            game.passPriority()
            game.state.stack.size shouldBe 1 // the enters trigger

            val network = game.findPermanent("Aligned Hedron Network")!!
            game.castSpell(1, "Shatter", network).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.isInGraveyard(1, "Aligned Hedron Network") shouldBe true
            game.findAllPermanents("Craw Wurm").size shouldBe 3
            game.isInExile(1, "Craw Wurm") shouldBe false
            game.isInExile(2, "Craw Wurm") shouldBe false
        }
    }
}
