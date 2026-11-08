package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Force of Despair {1}{B}{B} — Instant.
 *
 * "If it's not your turn, you may exile a black card from your hand rather than pay this spell's
 *  mana cost. Destroy all creatures that entered this turn."
 *
 * The sweep reads "entered this turn" from the event tracker, under *any* player's control, so
 * creatures that were already on the battlefield at the start of the turn survive.
 */
class ForceOfDespairScenarioTest : ScenarioTestBase() {
    init {
        context("Force of Despair") {
            test("destroys exactly the creatures that entered this turn, whoever controls them") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Force of Despair")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant", enteredThisTurn = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", enteredThisTurn = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Force of Despair").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe false
                game.findAllPermanents("Grizzly Bears").size shouldBe 1
                game.findAllPermanents("Hill Giant").size shouldBe 1
            }

            test("off your turn, exiling a black card from hand pays for it") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Force of Despair")
                    .withCardInHand(1, "Mind Rot")
                    .withCardOnBattlefield(2, "Grizzly Bears", enteredThisTurn = true)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withPriorityPlayer(1)
                    .build()

                val spell = game.findCardsInHand(1, "Force of Despair").single()
                val fodder = game.findCardsInHand(1, "Mind Rot").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(fodder)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Mind Rot") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
