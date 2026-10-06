package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Impending Doom (THB #139) — {2}{R} Enchantment — Aura.
 * "Enchanted creature gets +3/+3 and attacks each combat if able. When enchanted creature dies,
 * this Aura deals 3 damage to that creature's controller."
 *
 * The damage goes to the dying creature's (last-known) controller, not the Aura's controller.
 */
class ImpendingDoomScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Impending Doom") {

            test("gives the enchanted creature +3/+3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Impending Doom", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = projector.project(game.state)
                val id = game.findPermanent("Grizzly Bears")!!
                projected.getPower(id) shouldBe 5
                projected.getToughness(id) shouldBe 5
            }

            test("deals 3 damage to the dying creature's controller, not the Aura's controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Impending Doom", "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
