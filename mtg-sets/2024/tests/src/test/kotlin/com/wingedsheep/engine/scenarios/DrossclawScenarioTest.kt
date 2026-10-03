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
 * Drossclaw (MH3) — living weapon Equipment: +1/+1, and whenever the equipped creature attacks,
 * each opponent loses 1 life. Equip {2}.
 */
class DrossclawScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("living weapon creates a Phyrexian Germ and attaches to it, making a 1/1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Drossclaw")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Drossclaw")
            withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val germ = game.findPermanent("Phyrexian Germ Token")
            withClue("A Germ token should exist") { germ shouldNotBe null }
            val claw = game.findPermanent("Drossclaw")!!
            withClue("Drossclaw is attached to the Germ") {
                game.state.getEntity(claw)?.get<AttachedToComponent>()?.targetId shouldBe germ
            }
            val projected = stateProjector.project(game.state)
            withClue("Equipped 0/0 Germ is a 1/1") {
                projected.getPower(germ!!) shouldBe 1
                projected.getToughness(germ) shouldBe 1
                projected.hasSubtype(germ, "Phyrexian") shouldBe true
                projected.hasSubtype(germ, "Germ") shouldBe true
            }
        }

        test("whenever equipped creature attacks, each opponent loses 1 life") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Drossclaw", "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = stateProjector.project(game.state)
            withClue("Equipped Grizzly Bears is 3/3") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val attack = game.declareAttackers(mapOf("Grizzly Bears" to 2))
            withClue("Attack should succeed: ${attack.error}") { attack.error shouldBe null }
            game.resolveStack()

            withClue("Bob loses 1 life from the attack trigger") { game.getLifeTotal(2) shouldBe 19 }
            withClue("Alice is unaffected") { game.getLifeTotal(1) shouldBe 20 }
        }
    }
}
