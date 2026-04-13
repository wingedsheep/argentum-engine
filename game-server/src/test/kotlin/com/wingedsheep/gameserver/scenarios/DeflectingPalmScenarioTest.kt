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
 * Scenario tests for Deflecting Palm.
 *
 * "The next time a source of your choice would deal damage to you this turn,
 *  prevent that damage. If damage is prevented this way, Deflecting Palm deals
 *  that much damage to that source's controller."
 */
class DeflectingPalmScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseSource(sourceName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        val selectDecision = decision
        val entityId = selectDecision.options.first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == sourceName
        }
        submitDecision(CardsSelectedResponse(decision.id, listOf(entityId)))
    }

    init {
        context("Deflecting Palm") {

            test("prevents combat damage and deals it to source's controller") {
                // Alpine Grizzly is a 4/2 in Khans
                val game = scenario()
                    .withPlayers("Boros Mage", "Attacker")
                    .withCardInHand(1, "Deflecting Palm")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Player 2 attacks with Alpine Grizzly
                val atkResult = game.declareAttackers(mapOf("Alpine Grizzly" to 1))
                atkResult.error shouldBe null

                // After declaring attackers, active player (P2) gets priority first
                // P2 passes, then P1 gets priority to cast Deflecting Palm
                game.passPriority()  // P2 passes priority

                // Player 1 casts Deflecting Palm (instant, during declare attackers step)
                val castResult = game.castSpell(1, "Deflecting Palm")
                castResult.error shouldBe null

                // Both players pass to resolve Deflecting Palm
                game.passPriority()  // P1 passes
                game.passPriority()  // P2 passes → Deflecting Palm resolves

                // After resolution, should have a ChooseOptionDecision pending
                game.chooseSource("Alpine Grizzly")

                // Advance through declare blockers and combat damage
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Pass through combat damage to postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Alpine Grizzly's 4 damage was prevented,
                // and 4 damage is dealt to its controller (player 2)
                val p2LifeAfter = game.getLifeTotal(2)
                p2LifeAfter shouldBe (20 - 4) // Player 2 takes 4 reflected damage

                val p1LifeAfter = game.getLifeTotal(1)
                p1LifeAfter shouldBe 20 // Player 1 takes no damage
            }

            test("prevents spell damage and deals it to source's controller") {
                // Use Shock (Onslaught) as an instant that deals damage
                val game = scenario()
                    .withPlayers("Boros Mage", "Red Mage")
                    .withCardInHand(1, "Deflecting Palm")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Shock targeting Player 1
                val shockResult = game.castSpellTargetingPlayer(2, "Shock", 1)
                shockResult.error shouldBe null

                // P2 retains priority after casting, must pass for P1 to respond
                game.passPriority()  // P2 passes

                // Player 1 responds with Deflecting Palm
                val palmResult = game.castSpell(1, "Deflecting Palm")
                palmResult.error shouldBe null

                // Resolve Deflecting Palm (LIFO — resolves first)
                game.passPriority()  // P1 passes
                game.passPriority()  // P2 passes → Deflecting Palm resolves

                // Choose Shock as the source (it's still on the stack)
                game.chooseSource("Shock")

                // Now resolve Shock — its damage should be prevented and reflected
                game.resolveStack()

                val p1LifeAfter = game.getLifeTotal(1)
                p1LifeAfter shouldBe 20 // Damage prevented

                val p2LifeAfter = game.getLifeTotal(2)
                p2LifeAfter shouldBe (20 - 2) // 2 damage reflected
            }
        }
    }
}
