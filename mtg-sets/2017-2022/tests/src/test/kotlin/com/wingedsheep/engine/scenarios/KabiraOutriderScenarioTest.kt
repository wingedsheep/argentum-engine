package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Kabira Outrider (ZNR #18) — "When this creature enters, target creature gets +1/+1 until end of
 * turn for each creature in your party."
 *
 * The Outrider is itself a Warrior, so it counts toward the party it reads; a second Warrior
 * doesn't, and a Cleric Rogue fills one slot only.
 */
class KabiraOutriderScenarioTest : ScenarioTestBase() {

    private val warrior = card("Outrider Test Warrior") {
        manaCost = "{1}"
        typeLine = "Creature — Human Warrior"
        power = 1
        toughness = 1
    }

    private val clericRogue = card("Outrider Test Cleric Rogue") {
        manaCost = "{1}"
        typeLine = "Creature — Vampire Cleric Rogue"
        power = 1
        toughness = 1
    }

    private fun enterTargeting(vararg allies: String): Pair<Int, Int> {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Kabira Outrider")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        allies.forEach { builder.withCardOnBattlefield(1, it) }
        val game = builder.build()
        val bears = game.findPermanent("Grizzly Bears")!!

        game.castSpell(1, "Kabira Outrider").error shouldBe null
        game.resolveStack()
        game.selectTargets(listOf(bears)).error shouldBe null
        game.resolveStack()

        val projected = game.state.projectedState
        return projected.getPower(bears)!! to projected.getToughness(bears)!!
    }

    init {
        cardRegistry.register(warrior)
        cardRegistry.register(clericRogue)

        test("alone, the Outrider is a party of one") {
            enterTargeting() shouldBe (3 to 3)
        }

        test("a second Warrior adds nothing; a Cleric Rogue adds one") {
            enterTargeting("Outrider Test Warrior") shouldBe (3 to 3)
            enterTargeting("Outrider Test Warrior", "Outrider Test Cleric Rogue") shouldBe (4 to 4)
        }
    }
}
