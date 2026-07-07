package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fungal Fortitude (LCI #106):
 *
 *   {1}{B} Enchantment — Aura, Common
 *   Flash
 *   Enchant creature
 *   Enchanted creature gets +2/+0.
 *   When enchanted creature dies, return it to the battlefield tapped under its owner's control.
 *
 * Tests:
 *  - The attached creature's power increases by 2, toughness is unchanged.
 *  - When the enchanted creature dies, it returns to the battlefield tapped.
 */
class FungalFortitudeScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    // {0} sorcery to destroy a chosen creature, so the "enchanted creature dies" trigger fires.
    private val slay = card("Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target("target creature", Targets.Creature)
            effect = Effects.Destroy(c)
        }
    }

    init {
        cardRegistry.register(listOf(slay))

        // FungalFortitude is auto-discovered from the LCI set; no manual register needed.

        test("enchanted creature gets +2/+0") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")   // base 2/2
                .withCardAttachedTo(1, "Fungal Fortitude", "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = projector.project(game.state)
            projected.getPower(bears) shouldBe 4    // 2 + 2
            projected.getToughness(bears) shouldBe 2 // 2 + 0
        }

        test("when enchanted creature dies it returns to the battlefield tapped") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Fungal Fortitude", "Grizzly Bears")
                .withCardInHand(1, "Slay Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            // Destroy the enchanted creature; this fires the "enchanted creature dies" trigger.
            game.castSpell(1, "Slay Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            // The trigger should have returned Grizzly Bears to the battlefield (not the graveyard).
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false

            // The returned creature must be tapped (as the oracle text specifies).
            val returnedBears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(returnedBears)!!.has<TappedComponent>() shouldBe true
        }
    }
}
