package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Cunning Geysermage (ZNR #55) — {2}{U} Creature — Human Wizard, 3/2.
 *
 *   Kicker {2}{U}
 *   When this creature enters, if it was kicked, return up to one other target creature to its
 *   owner's hand.
 */
class CunningGeysermageScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Cunning Geysermage")
        .withLandsOnBattlefield(1, "Island", 6)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Cunning Geysermage").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    init {
        test("kicked: returns another target creature to its owner's hand") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(kicked = true)

            val mage = game.findPermanent("Cunning Geysermage")!!
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].orEmpty()
            withClue("\"other\" excludes the Geysermage itself") { legal shouldNotContain mage }
            legal shouldContain bears

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the Bears go back to the opponent's hand") {
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            game.isOnBattlefield("Cunning Geysermage") shouldBe true
        }

        test("kicked: \"up to one\" lets the trigger pick no target") {
            val game = board()
            game.cast(kicked = true)

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(emptyList()).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("unkicked: the trigger does nothing") {
            val game = board()
            game.cast(kicked = false)

            withClue("no target prompt and nothing on the stack") {
                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
