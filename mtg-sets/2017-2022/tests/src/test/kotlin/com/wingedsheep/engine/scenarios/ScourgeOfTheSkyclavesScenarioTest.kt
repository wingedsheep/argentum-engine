package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scourge of the Skyclaves (ZNR #122) — {1}{B} Creature — Demon, * / *.
 *
 *   Kicker {4}{B}
 *   When you cast this spell, if it was kicked, each player loses half their life, rounded up.
 *   Scourge of the Skyclaves's power and toughness are each equal to 20 minus the highest life
 *   total among players.
 *
 * Covers the kicked cast trigger resolving before the creature spell (each player halving their
 * own life, rounded up) and the CDA reading the highest life total afterwards.
 */
class ScourgeOfTheSkyclavesScenarioTest : ScenarioTestBase() {

    private fun board(myLife: Int, opponentLife: Int): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Scourge of the Skyclaves")
        .withLandsOnBattlefield(1, "Swamp", 7)
        .withLifeTotal(1, myLife)
        .withLifeTotal(2, opponentLife)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Scourge of the Skyclaves").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    init {
        test("kicked at 20 and 20: everyone drops to 10 and the Scourge enters as a 10/10") {
            val game = board(myLife = 20, opponentLife = 20)
            game.cast(kicked = true)

            game.getLifeTotal(1) shouldBe 10
            game.getLifeTotal(2) shouldBe 10
            val id = game.findPermanent("Scourge of the Skyclaves")!!
            game.state.projectedState.getPower(id) shouldBe 10
            game.state.projectedState.getToughness(id) shouldBe 10
        }

        test("kicked: each player loses half of their own life, rounded up") {
            val game = board(myLife = 15, opponentLife = 25)
            game.cast(kicked = true)

            withClue("15 loses 8 (half rounded up)") { game.getLifeTotal(1) shouldBe 7 }
            withClue("25 loses 13 (half rounded up)") { game.getLifeTotal(2) shouldBe 12 }
            val id = game.findPermanent("Scourge of the Skyclaves")!!
            withClue("20 minus the highest life total (12)") {
                game.state.projectedState.getPower(id) shouldBe 8
                game.state.projectedState.getToughness(id) shouldBe 8
            }
        }

        test("unkicked at 20 life: no life loss, the Scourge is 0/0 and dies") {
            val game = board(myLife = 20, opponentLife = 20)
            game.cast(kicked = false)

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Scourge of the Skyclaves") shouldBe false
            game.isInGraveyard(1, "Scourge of the Skyclaves") shouldBe true
        }

        test("unkicked below 20 life: P/T is 20 minus the highest life total") {
            val game = board(myLife = 14, opponentLife = 12)
            game.cast(kicked = false)

            val id = game.findPermanent("Scourge of the Skyclaves")!!
            game.state.projectedState.getPower(id) shouldBe 6
            game.state.projectedState.getToughness(id) shouldBe 6
        }
    }
}
