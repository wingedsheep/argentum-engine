package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cranial Ram (MH3) — living weapon Equipment: equipped creature gets +X/+1, where X is the
 * number of artifacts you control. Equip {2}.
 */
class CranialRamScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("living weapon makes a Germ that counts the Ram itself: 1/1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Cranial Ram")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Cranial Ram")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val ram = game.findPermanent("Cranial Ram")!!
            withClue("Cranial Ram is attached to the Germ") {
                game.state.getEntity(ram)?.get<AttachedToComponent>()?.targetId shouldBe germ
            }
            val projected = stateProjector.project(game.state)
            withClue("0/0 Germ +1/+1 with one artifact (the Ram)") {
                projected.getPower(germ!!) shouldBe 1
                projected.getToughness(germ) shouldBe 1
            }
        }

        test("X counts only artifacts you control and toughness bonus stays +1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Cranial Ram", "Grizzly Bears")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = stateProjector.project(game.state)
            withClue("2/2 + X=3 (Ram + two Ornithopters; Bob's ignored) / +1") {
                projected.getPower(bears) shouldBe 5
                projected.getToughness(bears) shouldBe 3
            }
        }
    }
}
