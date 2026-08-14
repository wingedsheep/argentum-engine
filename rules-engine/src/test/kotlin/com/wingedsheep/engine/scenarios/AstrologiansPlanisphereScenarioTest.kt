package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Astrologian's Planisphere. */
class AstrologiansPlanisphereScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Astrologian's Planisphere") {
            test("equipped Hero is a Wizard and gains a +1/+1 counter when its controller casts a noncreature spell") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Astrologian's Planisphere")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Astrologian's Planisphere")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val hero = game.findPermanent("Hero Token")!!
                val before = stateProjector.project(game.state)
                withClue("Equipped Hero is a Wizard, base 1/1 before any noncreature spell") {
                    before.hasSubtype(hero, "Wizard") shouldBe true
                    before.getPower(hero) shouldBe 1
                    before.getToughness(hero) shouldBe 1
                }

                val giant = game.findPermanent("Hill Giant")!!
                val shock = game.castSpell(1, "Shock", giant)
                withClue("Shock should succeed: ${shock.error}") { shock.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val after = stateProjector.project(game.state)
                withClue("Casting a noncreature spell puts a +1/+1 counter on the Hero → 2/2") {
                    after.getPower(hero) shouldBe 2
                    after.getToughness(hero) shouldBe 2
                }
            }
        }
    }
}
