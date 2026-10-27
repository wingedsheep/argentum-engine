package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Journey to Oblivion (ZNR #17) — "This spell costs {1} less to cast for each creature in your
 * party. When this enchantment enters, exile target nonland permanent an opponent controls until
 * this enchantment leaves the battlefield."
 *
 * The first card to price a spell off party size: the discount reads the caster's battlefield while
 * the card is still in hand, so a full party casts it for {W} and a lone Warrior only shaves {1}.
 */
class JourneyToOblivionScenarioTest : ScenarioTestBase() {

    private val warrior = card("Journey Test Warrior") {
        manaCost = "{1}"
        typeLine = "Creature — Human Warrior"
        power = 1
        toughness = 1
    }

    private val clericRogue = card("Journey Test Cleric Rogue") {
        manaCost = "{1}"
        typeLine = "Creature — Vampire Cleric Rogue"
        power = 1
        toughness = 1
    }

    private val rogue = card("Journey Test Rogue") {
        manaCost = "{1}"
        typeLine = "Creature — Human Rogue"
        power = 1
        toughness = 1
    }

    private val wizard = card("Journey Test Wizard") {
        manaCost = "{1}"
        typeLine = "Creature — Merfolk Wizard"
        power = 1
        toughness = 1
    }

    private fun game(plains: Int, vararg allies: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Journey to Oblivion")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", plains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        allies.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        cardRegistry.register(warrior)
        cardRegistry.register(clericRogue)
        cardRegistry.register(rogue)
        cardRegistry.register(wizard)

        test("a full party casts it for {W} and it exiles the target") {
            val game = game(
                1,
                "Journey Test Warrior", "Journey Test Cleric Rogue", "Journey Test Rogue", "Journey Test Wizard",
            )
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Journey to Oblivion").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Journey to Oblivion") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe true
        }

        test("a dual-typed Cleric Rogue fills only one slot") {
            val game = game(1, "Journey Test Warrior", "Journey Test Cleric Rogue", "Journey Test Wizard")
            game.castSpell(1, "Journey to Oblivion").error shouldNotBe null
        }

        test("a second Warrior adds nothing to the discount") {
            val game = game(3, "Journey Test Warrior", "Journey Test Warrior")
            game.castSpell(1, "Journey to Oblivion").error shouldNotBe null
        }

        test("with no party it costs the full {4}{W}") {
            val game = game(4)
            game.castSpell(1, "Journey to Oblivion").error shouldNotBe null
        }
    }
}
