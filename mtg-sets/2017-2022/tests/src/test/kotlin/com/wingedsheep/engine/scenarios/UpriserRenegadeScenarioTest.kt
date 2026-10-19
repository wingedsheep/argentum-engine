package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Upriser Renegade (NEO #170) — {1}{R} Creature — Human Samurai, 1/3.
 *
 * "This creature gets +2/+0 for each other modified creature you control. (Equipment, Auras you
 *  control, and counters are modifications.)"
 *
 * Covers the static's count of modified creatures in projection: an Aura you control counts, an
 * opponent's Aura on your creature doesn't (CR 700.9), and modifying the Renegade itself adds nothing.
 */
class UpriserRenegadeScenarioTest : ScenarioTestBase() {

    init {
        fun base() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Upriser Renegade")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Upriser Renegade") {

            test("no modified creatures: printed 1/3") {
                val game = base().build()
                val renegade = game.findPermanent("Upriser Renegade")!!
                game.state.projectedState.getPower(renegade) shouldBe 1
                game.state.projectedState.getToughness(renegade) shouldBe 3
            }

            test("an Aura you control on another creature gives +2/+0") {
                val game = base().withCardAttachedTo(1, "Pacifism", "Grizzly Bears").build()
                val renegade = game.findPermanent("Upriser Renegade")!!
                game.state.projectedState.getPower(renegade) shouldBe 3
                game.state.projectedState.getToughness(renegade) shouldBe 3
            }

            test("two other modified creatures give +4/+0") {
                val game = base()
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Hill Giant")
                    .build()
                val renegade = game.findPermanent("Upriser Renegade")!!
                game.state.projectedState.getPower(renegade) shouldBe 5
            }

            test("an opponent's Aura doesn't make your creature modified") {
                val game = base().withCardAttachedTo(2, "Pacifism", "Grizzly Bears").build()
                val renegade = game.findPermanent("Upriser Renegade")!!
                withClue("Bob's Pacifism is not a modification of Alice's Grizzly Bears") {
                    game.state.projectedState.getPower(renegade) shouldBe 1
                }
            }

            test("modifying the Renegade itself adds nothing") {
                val game = base().withCardAttachedTo(1, "Pacifism", "Upriser Renegade").build()
                val renegade = game.findPermanent("Upriser Renegade")!!
                game.state.projectedState.getPower(renegade) shouldBe 1
            }
        }
    }
}
