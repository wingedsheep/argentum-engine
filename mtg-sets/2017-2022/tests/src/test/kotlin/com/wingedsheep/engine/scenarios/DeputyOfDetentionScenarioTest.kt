package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Deputy of Detention (RNA #165) — "When this creature enters, exile target nonland permanent an
 * opponent controls and all other nonland permanents that player controls with the same name as
 * that permanent until this creature leaves the battlefield."
 */
class DeputyOfDetentionScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        test("exiles the target and that player's same-named permanents until the Deputy leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Deputy of Detention")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opponentGiants = game.findPermanents("Hill Giant")
                .filter { game.state.projectedState.getController(it) == game.player2Id }
            opponentGiants shouldHaveSize 2
            val myGiant = game.findPermanents("Hill Giant").single { it !in opponentGiants }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Deputy of Detention").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            withClue("only nonland permanents an opponent controls") {
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder
                    (opponentGiants + bears)
            }
            game.selectTargets(listOf(opponentGiants.first())).error shouldBe null
            game.resolveStack()

            withClue("both of the opponent's Hill Giants are exiled; their Bears and your Giant stay") {
                game.findPermanents("Hill Giant") shouldBe listOf(myGiant)
                game.findPermanent("Grizzly Bears") shouldBe bears
            }

            val deputy = game.findPermanent("Deputy of Detention")!!
            game.castSpell(1, "Lightning Bolt", deputy).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the Deputy died, so both Giants return under their owner's control") {
                game.isInGraveyard(1, "Deputy of Detention") shouldBe true
                val giants = game.findPermanents("Hill Giant")
                giants shouldHaveSize 3
                giants.count { game.state.projectedState.getController(it) == game.player2Id } shouldBe 2
            }
        }

        test("nothing is exiled if the Deputy leaves before its trigger resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Deputy of Detention")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Deputy of Detention").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            game.selectTargets(listOf(giant)).error shouldBe null

            // Bolt the Deputy in response, with its enters trigger still on the stack.
            val deputy = game.findPermanent("Deputy of Detention")!!
            game.castSpell(1, "Lightning Bolt", deputy).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the Deputy is gone before the trigger resolves, so the Giant stays") {
                game.isInGraveyard(1, "Deputy of Detention") shouldBe true
                game.findPermanent("Hill Giant") shouldBe giant
                game.isInExile(2, "Hill Giant") shouldBe false
            }
        }
    }
}
