package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Constricting Sliver (M15 #7).
 *
 * "Sliver creatures you control have "When this creature enters, you may exile target creature an
 * opponent controls until this creature leaves the battlefield.""
 *
 * Under test: the granted trigger fires for Constricting Sliver itself and for another Sliver
 * entering; "this creature" is the Sliver that has the ability, so the exile lasts until *that*
 * Sliver leaves — it survives Constricting Sliver leaving (and the ability going with it) — and the
 * "you may" is a resolution-time choice.
 */
class ConstrictingSliverScenarioTest : ScenarioTestBase() {

    private fun board(constrictingInPlay: Boolean): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .apply {
            if (constrictingInPlay) {
                withCardOnBattlefield(1, "Constricting Sliver")
                withCardInHand(1, "Venom Sliver")
                withCardInHand(1, "Murder")
                withCardInHand(1, "Murder")
                withLandsOnBattlefield(1, "Forest", 2)
                withLandsOnBattlefield(1, "Swamp", 6)
            } else {
                withCardInHand(1, "Constricting Sliver")
                withLandsOnBattlefield(1, "Plains", 6)
            }
        }
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast [sliver], resolve it, and aim its granted enters trigger at the opponent's Bears. */
    private fun TestGame.castSliverTargetingBears(sliver: String) {
        castSpell(1, sliver).error shouldBe null
        resolveStack()

        val bears = findPermanent("Grizzly Bears")!!
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("only creatures an opponent controls are legal targets") {
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
        }
        selectTargets(listOf(bears)).error shouldBe null
        resolveStack()
    }

    init {
        test("Constricting Sliver's own ability triggers when it enters, and the exile is optional") {
            val game = board(constrictingInPlay = false)
            game.castSliverTargetingBears("Constricting Sliver")
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe false
        }

        test("another Sliver's exile lasts until that Sliver leaves, even after Constricting Sliver is gone") {
            val game = board(constrictingInPlay = true)
            game.castSliverTargetingBears("Venom Sliver")
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true

            val constricting = game.findPermanent("Constricting Sliver")!!
            game.castSpell(1, "Murder", constricting).error shouldBe null
            game.resolveStack()

            withClue("Constricting Sliver leaving doesn't end the Venom Sliver's exile") {
                game.isInGraveyard(1, "Constricting Sliver") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            val venom = game.findPermanent("Venom Sliver")!!
            game.castSpell(1, "Murder", venom).error shouldBe null
            game.resolveStack()

            withClue("the Venom Sliver left, so the Bears return under their owner's control") {
                game.isInGraveyard(1, "Venom Sliver") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }
        }
    }
}
