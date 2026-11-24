package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Detention Sphere (RTR #155) — "When this enchantment enters, you may exile target nonland
 * permanent not named Detention Sphere and all other permanents with the same name as that
 * permanent. When this enchantment leaves the battlefield, return the exiled cards to the
 * battlefield under their owner's control."
 */
class DetentionSphereScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    /** Resolve the stack, answering the enters trigger's "you may" with [choice] when it is asked. */
    private fun TestGame.resolveAnswering(choice: Boolean) {
        resolveStack()
        while (getPendingDecision() is YesNoDecision) {
            answerYesNo(choice).error shouldBe null
            resolveStack()
        }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Detention Sphere")
        .withCardInHand(1, "Disenchant")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("exiles the target and every same-named permanent, returning them when the Sphere leaves") {
            val game = board()
                .withCardOnBattlefield(2, "Detention Sphere")
                .build()

            val opponentGiants = game.findPermanents("Hill Giant")
                .filter { game.state.projectedState.getController(it) == game.player2Id }
            opponentGiants shouldHaveSize 2
            val myGiant = game.findPermanents("Hill Giant").single { it !in opponentGiants }
            val bears = game.findPermanent("Grizzly Bears")!!
            val opponentSphere = game.findPermanent("Detention Sphere")!!

            game.castSpell(1, "Detention Sphere").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            withClue("any nonland permanent not named Detention Sphere") {
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder
                    (opponentGiants + myGiant + bears)
            }
            game.selectTargets(listOf(opponentGiants.first())).error shouldBe null
            game.resolveAnswering(true)

            withClue("all three Hill Giants are exiled, whoever controls them; the Bears stay") {
                game.findPermanents("Hill Giant").shouldBeEmpty()
                game.findPermanent("Grizzly Bears") shouldBe bears
                game.findPermanents("Detention Sphere") shouldHaveSize 2
                (opponentSphere in game.findPermanents("Detention Sphere")) shouldBe true
            }

            val mySphere = game.findPermanents("Detention Sphere").single { it != opponentSphere }
            game.castSpell(1, "Disenchant", mySphere).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the Sphere left, so every Giant returns under its owner's control") {
                game.isInGraveyard(1, "Detention Sphere") shouldBe true
                val giants = game.findPermanents("Hill Giant")
                giants shouldHaveSize 3
                giants.count { game.state.projectedState.getController(it) == game.player2Id } shouldBe 2
                giants.count { game.state.projectedState.getController(it) == game.player1Id } shouldBe 1
            }
        }

        test("declining the may exiles nothing") {
            val game = board().build()

            val giant = game.findPermanents("Hill Giant").first()

            game.castSpell(1, "Detention Sphere").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveAnswering(false)

            game.findPermanents("Hill Giant") shouldHaveSize 3
        }

        test("if the Sphere leaves before its enters trigger resolves, the cards are exiled for good") {
            val game = board().build()

            val opponentGiant = game.findPermanents("Hill Giant")
                .first { game.state.projectedState.getController(it) == game.player2Id }

            game.castSpell(1, "Detention Sphere").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            game.selectTargets(listOf(opponentGiant)).error shouldBe null

            // Disenchant the Sphere in response, with its enters trigger still on the stack.
            val sphere = game.findPermanent("Detention Sphere")!!
            game.castSpell(1, "Disenchant", sphere).error shouldBe null
            game.payIfAsked()
            game.resolveAnswering(true)

            withClue("the leaves trigger found nothing; the enters trigger then exiled every Giant indefinitely") {
                game.isInGraveyard(1, "Detention Sphere") shouldBe true
                game.findPermanents("Hill Giant").shouldBeEmpty()
                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInExile(1, "Hill Giant") shouldBe true
                game.state.stack.shouldBeEmpty()
            }
        }
    }
}
