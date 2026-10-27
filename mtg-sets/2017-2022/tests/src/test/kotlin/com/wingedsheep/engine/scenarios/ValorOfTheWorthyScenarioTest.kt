package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Valor of the Worthy (KHM #37):
 *
 *   {W} Enchantment — Aura
 *   Enchant creature
 *   Enchanted creature gets +1/+1.
 *   When enchanted creature leaves the battlefield, create a 1/1 white Spirit creature token with flying.
 *
 * "Leaves the battlefield" — not just "dies": exiling the creature must also make the Spirit.
 */
class ValorOfTheWorthyScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val exileSpell = card("Exile Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Exile target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Exile(c)
        }
    }

    private val slay = card("Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Destroy(c)
        }
    }

    init {
        cardRegistry.register(listOf(exileSpell, slay))

        test("enchanted creature gets +1/+1") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Valor of the Worthy", "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = projector.project(game.state)
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        test("exiling the enchanted creature creates a 1/1 flying Spirit") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Valor of the Worthy", "Grizzly Bears")
                .withCardInHand(1, "Exile Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Exile Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Valor of the Worthy") shouldBe true
            val spirits = game.findPermanents("Spirit Token")
            spirits.size shouldBe 1
            val projected = projector.project(game.state)
            projected.getPower(spirits.single()) shouldBe 1
            projected.getToughness(spirits.single()) shouldBe 1
        }

        test("the enchanted creature dying also creates a Spirit") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Valor of the Worthy", "Grizzly Bears")
                .withCardInHand(1, "Slay Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Slay Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            // The Aura's controller creates the token, even though the creature was the opponent's.
            val spirits = game.findPermanents("Spirit Token")
            spirits.size shouldBe 1
            projector.project(game.state).getController(spirits.single()) shouldBe game.player1Id
        }
    }
}
