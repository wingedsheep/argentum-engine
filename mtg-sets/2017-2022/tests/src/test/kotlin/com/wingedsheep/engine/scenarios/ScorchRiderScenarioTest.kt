package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scorch Rider (ZNR #158) — {3}{R} Creature — Human Warrior, 4/3.
 *
 *   Kicker {1}{R}
 *   When this creature enters, if it was kicked, it gains haste until end of turn.
 */
class ScorchRiderScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Scorch Rider")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Scorch Rider").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    init {
        test("unkicked: no haste") {
            val game = board()
            game.cast(kicked = false)
            val rider = game.findPermanent("Scorch Rider")!!
            withClue("unkicked Rider does not gain haste") {
                game.state.projectedState.hasKeyword(rider, Keyword.HASTE.name) shouldBe false
            }
        }

        test("kicked: gains haste until end of turn") {
            val game = board()
            game.cast(kicked = true)
            val rider = game.findPermanent("Scorch Rider")!!
            withClue("kicked Rider gains haste") {
                game.state.projectedState.hasKeyword(rider, Keyword.HASTE.name) shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("haste ends at end of turn") {
                game.state.projectedState.hasKeyword(rider, Keyword.HASTE.name) shouldBe false
            }
        }
    }
}
