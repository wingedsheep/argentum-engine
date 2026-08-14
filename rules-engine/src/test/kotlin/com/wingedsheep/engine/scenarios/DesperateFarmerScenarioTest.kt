package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Desperate Farmer // Depraved Harvester (VOW #104).
 *
 *   Front — Desperate Farmer (2/2, Lifelink) — When another creature you control dies, transform
 *           this creature.
 *   Back  — Depraved Harvester (4/3, Lifelink).
 *
 * Exercises the "another creature you control dies" transform trigger — it fires when a *different*
 * creature dies, but not when the Farmer itself is the only casualty.
 */
class DesperateFarmerScenarioTest : ScenarioTestBase() {

    init {
        context("Desperate Farmer") {

            test("when another creature you control dies, it transforms into Depraved Harvester") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desperate Farmer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val farmer = game.findPermanent("Desperate Farmer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Kill the other creature you control; its death triggers the Farmer's transform.
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                var guard = 0
                while (game.state.getEntity(farmer)!!.get<CardComponent>()!!.name == "Desperate Farmer" && guard++ < 10) {
                    game.resolveStack()
                }

                withClue("another creature died → Farmer transformed to Depraved Harvester (4/3)") {
                    game.state.getEntity(farmer)!!.get<CardComponent>()!!.name shouldBe "Depraved Harvester"
                    game.state.projectedState.getPower(farmer) shouldBe 4
                    game.state.projectedState.getToughness(farmer) shouldBe 3
                }
            }

            test("the Farmer does not transform off of its own death") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desperate Farmer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val farmer = game.findPermanent("Desperate Farmer")!!

                // Bolt the Farmer itself. "Another creature" excludes it, so it just dies.
                game.castSpell(1, "Lightning Bolt", targetId = farmer).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                repeat(3) { game.resolveStack() }

                withClue("its own death does not transform it — it goes to the graveyard as Desperate Farmer") {
                    game.isInGraveyard(1, "Desperate Farmer") shouldBe true
                    game.findPermanent("Depraved Harvester") shouldBe null
                }
            }
        }
    }
}
