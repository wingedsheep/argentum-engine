package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EmergentGrowthScenarioTest : ScenarioTestBase() {
    init {
        test("requires one blocker while allowing other creatures to block another attacker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Emergent Growth")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Gray Ogre")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Emergent Growth", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 7
            game.state.projectedState.getToughness(bears) shouldBe 7

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareNoBlockers().outcome shouldNotBe Outcome.Done
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Hill Giant")))
                .outcome shouldNotBe Outcome.Done
            // Unlike Lure, the spell leaves other blockers free to block another attacker.
            game.declareBlockers(mapOf(
                "Savannah Lions" to listOf("Grizzly Bears"),
                "Gray Ogre" to listOf("Hill Giant"),
            )).outcome shouldBe Outcome.Done
        }

        test("does not force an illegal block when only ground creatures could defend a flyer") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardInHand(1, "Emergent Growth")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Air Elemental", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Emergent Growth", game.findPermanent("Wind Drake")!!).error shouldBe null
            game.resolveStack()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wind Drake" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().outcome shouldBe Outcome.Done
        }
    }
}
