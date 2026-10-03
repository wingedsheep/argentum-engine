package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mandibular Kite (MH3) — living weapon Equipment: +1/+1 and flying. Equip {3}{W}.
 */
class MandibularKiteScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("living weapon creates a Phyrexian Germ and attaches to it, making a 1/1 flyer") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Mandibular Kite")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Mandibular Kite")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val kite = game.findPermanent("Mandibular Kite")!!
            withClue("Mandibular Kite is attached to the Germ") {
                game.state.getEntity(kite)?.get<AttachedToComponent>()?.targetId shouldBe germ
            }
            val projected = stateProjector.project(game.state)
            withClue("Equipped 0/0 Germ is a 1/1 flyer") {
                projected.getPower(germ!!) shouldBe 1
                projected.getToughness(germ) shouldBe 1
                projected.hasKeyword(germ, Keyword.FLYING) shouldBe true
            }
        }

        test("equipped creature gets +1/+1 and flying") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Mandibular Kite", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = stateProjector.project(game.state)
            withClue("Equipped Grizzly Bears is a 3/3 flyer") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }
        }
    }
}
