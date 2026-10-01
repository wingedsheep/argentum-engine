package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hexgold Hoverwings (ONE #14) — For Mirrodin! makes a 2/2 red Rebel and attaches to it; the
 * equipped creature has flying, and every equipped creature you control gets +1/+0.
 */
class HexgoldHoverwingsScenarioTest : ScenarioTestBase() {

    init {
        context("Hexgold Hoverwings") {
            test("For Mirrodin! Rebel becomes a 3/2 flyer; unequipped creatures are unaffected") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Hexgold Hoverwings")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Hexgold Hoverwings")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val wings = game.findPermanent("Hexgold Hoverwings")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Hoverwings is attached to the Rebel") {
                    game.state.getEntity(wings)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                }
                val projected = game.state.projectedState
                withClue("Equipped Rebel is a 3/2 flyer") {
                    projected.getPower(rebel) shouldBe 3
                    projected.getToughness(rebel) shouldBe 2
                    projected.hasKeyword(rebel, Keyword.FLYING) shouldBe true
                }
                withClue("Unequipped Grizzly Bears stays a 2/2 without flying") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.FLYING) shouldBe false
                }
            }

            test("the +1/+0 applies to every equipped creature you control, so two Hoverwings stack") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Hexgold Hoverwings")
                    .withCardInHand(1, "Hexgold Hoverwings")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                repeat(2) {
                    val cast = game.castSpell(1, "Hexgold Hoverwings")
                    withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                    if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }

                val rebels = game.findPermanents("Rebel Token")
                withClue("Two Rebels") { rebels.size shouldBe 2 }
                val projected = game.state.projectedState
                rebels.forEach { rebel ->
                    withClue("Each equipped Rebel gets +1/+0 from both Hoverwings: 4/2") {
                        projected.getPower(rebel) shouldBe 4
                        projected.getToughness(rebel) shouldBe 2
                        projected.hasKeyword(rebel, Keyword.FLYING) shouldBe true
                    }
                }
            }
        }
    }
}
