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
 * Colossal Dreadmask (MH3) — living weapon Equipment: +6/+6 and trample. Equip {3}{G}{G}.
 */
class ColossalDreadmaskScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("living weapon creates a Phyrexian Germ and attaches to it, making a 6/6 trampler") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Colossal Dreadmask")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Colossal Dreadmask")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val mask = game.findPermanent("Colossal Dreadmask")!!
            withClue("Colossal Dreadmask is attached to the Germ") {
                game.state.getEntity(mask)?.get<AttachedToComponent>()?.targetId shouldBe germ
            }
            val projected = stateProjector.project(game.state)
            withClue("Equipped 0/0 Germ is a 6/6 with trample") {
                projected.getPower(germ!!) shouldBe 6
                projected.getToughness(germ) shouldBe 6
                projected.hasKeyword(germ, Keyword.TRAMPLE) shouldBe true
            }
        }

        test("equipped creature gets +6/+6 and has trample") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Colossal Dreadmask", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = stateProjector.project(game.state)
            withClue("Equipped Grizzly Bears is an 8/8 trampler") {
                projected.getPower(bears) shouldBe 8
                projected.getToughness(bears) shouldBe 8
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
