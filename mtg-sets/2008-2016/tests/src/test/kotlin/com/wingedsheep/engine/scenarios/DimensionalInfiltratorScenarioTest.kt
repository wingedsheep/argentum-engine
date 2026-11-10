package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dimensional Infiltrator — {1}{U} 2/1 Eldrazi. "Devoid. Flash. Flying. {1}{C}: Target opponent
 * exiles the top card of their library. If it's a land card, you may return this creature to its
 * owner's hand."
 *
 * Covers the land check reading the card that actually got exiled, the resolution-time "may",
 * and the empty-library case.
 */
class DimensionalInfiltratorScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Dimensional Infiltrator")!!.script.activatedAbilities.single().id

        fun board(opponentTop: String?) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Dimensional Infiltrator")
            .withLandsOnBattlefield(1, "Wastes", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .apply { if (opponentTop != null) withCardInLibrary(2, opponentTop) }
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.activate() {
            val infiltrator = findPermanent("Dimensional Infiltrator")!!
            val result = execute(
                ActivateAbility(player1Id, infiltrator, abilityId, listOf(ChosenTarget.Player(player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        test("devoid: the blue-costed Infiltrator is colorless") {
            val game = board("Grizzly Bears")
            game.state.projectedState.getColors(game.findPermanent("Dimensional Infiltrator")!!) shouldBe emptySet()
        }

        test("a nonland top card is exiled and no return is offered") {
            val game = board("Grizzly Bears")
            game.activate()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Dimensional Infiltrator") shouldBe true
        }

        test("a land top card is exiled and accepting returns the Infiltrator to hand") {
            val game = board("Forest")
            game.activate()

            game.isInExile(2, "Forest") shouldBe true
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(true)

            game.isOnBattlefield("Dimensional Infiltrator") shouldBe false
            game.isInHand(1, "Dimensional Infiltrator") shouldBe true
        }

        test("a land top card can be declined, leaving the Infiltrator in play") {
            val game = board("Forest")
            game.activate()

            game.isInExile(2, "Forest") shouldBe true
            game.answerYesNo(false)

            game.isOnBattlefield("Dimensional Infiltrator") shouldBe true
            game.isInHand(1, "Dimensional Infiltrator") shouldBe false
        }

        test("an empty library exiles nothing and offers no return") {
            val game = board(null)
            game.activate()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Dimensional Infiltrator") shouldBe true
        }
    }
}
