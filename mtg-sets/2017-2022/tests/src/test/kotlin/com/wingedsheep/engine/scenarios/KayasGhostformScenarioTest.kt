package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kaya's Ghostform (WAR #94):
 *
 *   {B} Enchantment — Aura
 *   Enchant creature or planeswalker you control
 *   When enchanted permanent dies or is put into exile, return that card to the battlefield under
 *   your control.
 *
 * Both destinations return the card from the zone it went to, even though the Aura itself has
 * already left the battlefield when the trigger resolves.
 */
class KayasGhostformScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val exileSpell = card("Ghostform Exile Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Exile target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Exile(c)
        }
    }

    private val slay = card("Ghostform Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Destroy(c)
        }
    }

    // Farewell's creatures + enchantments modes in one simultaneous exile.
    private val exileBoth = card("Ghostform Farewell Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Exile all creatures and enchantments."
        spell {
            effect = Effects.Pipeline {
                exile(gather(GameObjectFilter.Creature or GameObjectFilter.Enchantment))
            }
        }
    }

    init {
        cardRegistry.register(listOf(exileSpell, slay, exileBoth))

        test("enchanted creature dies and returns to the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Kaya's Ghostform", "Grizzly Bears")
                .withCardInHand(2, "Ghostform Slay Test Spell")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Ghostform Slay Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            projector.project(game.state).getController(returned) shouldBe game.player1Id
            game.isInGraveyard(1, "Kaya's Ghostform") shouldBe true
        }

        test("enchanted creature is exiled and returns to the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Kaya's Ghostform", "Grizzly Bears")
                .withCardInHand(2, "Ghostform Exile Test Spell")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Ghostform Exile Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe false
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.isInGraveyard(1, "Kaya's Ghostform") shouldBe true
        }

        test("aura exiled together with the enchanted creature still returns it (ruling)") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Kaya's Ghostform", "Grizzly Bears")
                .withCardInHand(2, "Ghostform Farewell Test Spell")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Ghostform Farewell Test Spell").error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe false
            val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            projector.project(game.state).getController(returned) shouldBe game.player1Id
            game.isInExile(1, "Kaya's Ghostform") shouldBe true
        }
    }
}
