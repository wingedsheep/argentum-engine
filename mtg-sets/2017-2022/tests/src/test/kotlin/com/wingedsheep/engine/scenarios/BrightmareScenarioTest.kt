package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Brightmare (JMP) — when it enters, tap up to one target creature; you gain
 * life equal to that creature's power.
 */
class BrightmareScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Brightmare")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Brightmare") {
            test("taps the target creature and gains life equal to its power") {
                val game = setup()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(1, "Brightmare").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the Hill Giant is tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                }
                withClue("Player1 gains 3 life — the Hill Giant's power") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("choosing no target taps nothing and gains no life") {
                val game = setup()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(1, "Brightmare").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.skipTargets().error shouldBe null
                }
                game.resolveStack()

                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Brightmare") shouldBe true
            }
        }
    }
}
