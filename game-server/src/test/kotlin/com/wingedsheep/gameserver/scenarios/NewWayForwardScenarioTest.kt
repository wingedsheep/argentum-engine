package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for New Way Forward.
 *
 * "The next time a source of your choice would deal damage to you this turn, prevent that damage.
 *  When damage is prevented this way, New Way Forward deals that much damage to that source's
 *  controller and you draw that many cards."
 *
 * Same chosen-source prevention chain as Deflecting Palm, with an extra "you draw that many cards"
 * reaction keyed to the prevented amount.
 */
class NewWayForwardScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseSource(sourceName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        val entityId = decision.options.first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == sourceName
        }
        submitDecision(CardsSelectedResponse(decision.id, listOf(entityId)))
    }

    init {
        context("New Way Forward") {

            test("prevents combat damage, reflects it, and draws that many cards") {
                // Alpine Grizzly is a 4/2 in Khans.
                var builder = scenario()
                    .withPlayers("Jeskai Mage", "Attacker")
                    .withCardInHand(1, "New Way Forward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Library cards so the four-card draw resolves without decking out.
                repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
                val game = builder.build()

                game.handSize(1) shouldBe 1 // just New Way Forward

                val atkResult = game.declareAttackers(mapOf("Alpine Grizzly" to 1))
                atkResult.error shouldBe null

                game.passPriority() // P2 passes; P1 gets priority

                val castResult = game.castSpell(1, "New Way Forward")
                castResult.error shouldBe null

                game.passPriority() // P1 passes
                game.passPriority() // P2 passes → New Way Forward resolves

                // Choose Alpine Grizzly as the source on resolution.
                game.chooseSource("Alpine Grizzly")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 1's 4 combat damage was prevented.
                game.getLifeTotal(1) shouldBe 20
                // 4 damage reflected to Alpine Grizzly's controller (player 2).
                game.getLifeTotal(2) shouldBe (20 - 4)
                // Player 1 drew 4 cards (the prevented amount).
                game.handSize(1) shouldBe 4
            }

            test("prevents spell damage, reflects it, and draws that many cards") {
                var builder = scenario()
                    .withPlayers("Jeskai Mage", "Red Mage")
                    .withCardInHand(1, "New Way Forward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
                val game = builder.build()

                // Player 2 casts Shock targeting Player 1 (2 damage).
                val shockResult = game.castSpellTargetingPlayer(2, "Shock", 1)
                shockResult.error shouldBe null

                game.passPriority() // P2 passes

                // Player 1 responds with New Way Forward.
                val nwfResult = game.castSpell(1, "New Way Forward")
                nwfResult.error shouldBe null

                game.passPriority() // P1 passes
                game.passPriority() // P2 passes → New Way Forward resolves first (LIFO)

                // Choose Shock as the source while it is still on the stack.
                game.chooseSource("Shock")

                // Resolve Shock — its 2 damage is prevented and reflected.
                game.resolveStack()

                // Player 1 took no damage.
                game.getLifeTotal(1) shouldBe 20
                // 2 damage reflected to Shock's controller (player 2).
                game.getLifeTotal(2) shouldBe (20 - 2)
                // Player 1 drew 2 cards.
                game.handSize(1) shouldBe 2
            }
        }
    }
}
