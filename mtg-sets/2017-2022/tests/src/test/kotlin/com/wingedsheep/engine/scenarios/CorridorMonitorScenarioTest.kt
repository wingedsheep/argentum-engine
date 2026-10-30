package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Corridor Monitor (ELD #41) — {1}{U} Artifact Creature — Construct 1/4.
 *
 * "When this creature enters, untap target artifact or creature you control."
 */
class CorridorMonitorScenarioTest : ScenarioTestBase() {

    init {
        context("Corridor Monitor") {

            test("ETB untaps a tapped noncreature artifact you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Corridor Monitor")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Arcane Signet", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val signet = game.findPermanent("Arcane Signet")!!
                game.castSpell(1, "Corridor Monitor").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(signet)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(signet)!!.has<TappedComponent>() shouldBe false
            }

            test("ETB untaps a tapped creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Corridor Monitor")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Corridor Monitor").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
