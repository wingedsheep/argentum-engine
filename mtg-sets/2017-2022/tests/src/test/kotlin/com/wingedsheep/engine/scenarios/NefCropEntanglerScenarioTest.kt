package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Nef-Crop Entangler (AKH #144).
 *
 *   Trample
 *   You may exert this creature as it attacks. When you do, it gets +1/+2 until end of turn.
 */
class NefCropEntanglerScenarioTest : ScenarioTestBase() {

    init {
        context("Nef-Crop Entangler") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Nef-Crop Entangler")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting makes it a 3/3 trampler until end of turn") {
                val game = game()
                val entangler = game.findPermanent("Nef-Crop Entangler")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Nef-Crop Entangler" to 2)).error shouldBe null
                game.selectCards(listOf(entangler))
                game.state.getEntity(entangler)!!.has<ExertedComponent>() shouldBe true
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(entangler) shouldBe 3
                projected.getToughness(entangler) shouldBe 3
                projected.hasKeyword(entangler, Keyword.TRAMPLE) shouldBe true
            }

            test("declining the exert leaves it a 2/1 with no trigger") {
                val game = game()
                val entangler = game.findPermanent("Nef-Crop Entangler")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Nef-Crop Entangler" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(entangler)!!.has<ExertedComponent>() shouldBe false
                val projected = game.state.projectedState
                projected.getPower(entangler) shouldBe 2
                projected.getToughness(entangler) shouldBe 1
                projected.hasKeyword(entangler, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
