package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Borrowed Time (MID #6) — "When this enchantment enters, exile target nonland permanent an
 * opponent controls until this enchantment leaves the battlefield."
 */
class BorrowedTimeScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        test("exiles an opponent's nonland permanent until Borrowed Time leaves the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Borrowed Time")
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Borrowed Time").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            withClue("only the opponent's nonland permanents: not the Forest, not your own Bears") {
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(giant)
            }
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false

            game.castSpell(1, "Disenchant", game.findPermanent("Borrowed Time")!!).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the Hill Giant returns under its owner's control") {
                game.isInGraveyard(1, "Borrowed Time") shouldBe true
                val returned = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.state.projectedState.getController(returned) shouldBe game.player2Id
            }
        }
    }
}
