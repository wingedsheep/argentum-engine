package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Intrusive Packbeast (GRN #17) — {4}{W} 3/3 Beast.
 *
 *   Vigilance
 *   When this creature enters, tap up to two target creatures your opponents control.
 */
class IntrusivePackbeastScenarioTest : ScenarioTestBase() {

    init {
        test("entering taps the two chosen opposing creatures and leaves the third untapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Intrusive Packbeast")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Intrusive Packbeast").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(giant, bears)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(lions)!!.has<TappedComponent>() shouldBe false
            val packbeast = game.findPermanent("Intrusive Packbeast")!!
            game.state.projectedState.hasKeyword(packbeast, Keyword.VIGILANCE) shouldBe true
        }

        test("up to two: choosing no targets taps nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Intrusive Packbeast")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Intrusive Packbeast").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.skipTargets().error shouldBe null
                game.resolveStack()
            }

            game.isOnBattlefield("Intrusive Packbeast") shouldBe true
            game.state.getEntity(game.findPermanent("Hill Giant")!!)!!.has<TappedComponent>() shouldBe false
        }
    }
}
