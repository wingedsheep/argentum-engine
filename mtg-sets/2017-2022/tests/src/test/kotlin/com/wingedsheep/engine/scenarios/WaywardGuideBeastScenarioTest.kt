package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Wayward Guide-Beast (ZNR #176) — {R} Creature — Beast, 2/2.
 *
 *   Trample, haste
 *   Whenever this creature deals combat damage to a player, return a land you control to its
 *   owner's hand.
 *
 * Pins the combat-damage trigger: the land is chosen on resolution (not targeted), only from lands
 * its controller controls, and it goes to hand.
 */
class WaywardGuideBeastScenarioTest : ScenarioTestBase() {

    init {
        context("Wayward Guide-Beast's combat-damage trigger") {

            test("returns a land you control, chosen on resolution, to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wayward Guide-Beast", summoningSickness = true)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myLands = game.findPermanents("Mountain")
                val forest = game.findPermanent("Forest")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("haste lets it attack the turn it arrived") {
                    game.declareAttackers(mapOf("Wayward Guide-Beast" to 2)).error shouldBe null
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only lands the Guide-Beast's controller controls are offered") {
                    decision.options.toSet() shouldBe myLands.toSet()
                    decision.options.contains(forest) shouldBe false
                }
                game.selectCards(listOf(myLands.first()))
                game.resolveStack()

                withClue("2 damage connected and one Mountain went back to hand") {
                    game.getLifeTotal(2) shouldBe 18
                    game.isInHand(1, "Mountain") shouldBe true
                    game.findPermanents("Mountain").size shouldBe 1
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }
        }
    }
}
