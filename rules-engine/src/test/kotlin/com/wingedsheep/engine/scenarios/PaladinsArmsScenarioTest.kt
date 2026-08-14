package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Paladin's Arms. */
class PaladinsArmsScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Paladin's Arms") {
            test("ETB makes a Hero token that is a 3/2 Knight with ward {1}") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Paladin's Arms")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Paladin's Arms")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val hero = game.findPermanent("Hero Token")!!
                val projected = stateProjector.project(game.state)
                withClue("Equipped Hero is 1/1 + (+2/+1) = 3/2") {
                    projected.getPower(hero) shouldBe 3
                    projected.getToughness(hero) shouldBe 2
                }
                withClue("Equipped Hero is a Knight in addition to Hero, and has ward") {
                    projected.hasSubtype(hero, "Knight") shouldBe true
                    projected.hasSubtype(hero, "Hero") shouldBe true
                    projected.hasKeyword(hero, Keyword.WARD) shouldBe true
                }
            }
        }
    }
}
