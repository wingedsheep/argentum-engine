package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Pyromancer's Gauntlet (M14 #214) — "If a red instant or sorcery spell you control or a red
 * planeswalker you control would deal damage to a permanent or player, it deals that much damage
 * plus 2 to that permanent or player instead."
 *
 * A damage-amount replacement whose source filter matches spells on the stack: these pin that a red
 * instant (to a player and to a creature), a red sorcery and a red planeswalker all get +2, and that a
 * non-red spell, a red spell an opponent controls and a red creature's combat damage do not.
 */
class PyromancersGauntletScenarioTest : ScenarioTestBase() {

    private fun setup(spell: String, land: String, lands: Int, p2Bolt: Boolean = false): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Pyromancer's Gauntlet")
            .withCardInHand(1, spell)
            .withLandsOnBattlefield(1, land, lands)
            .withCardOnBattlefield(2, "Craw Wurm")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (p2Bolt) {
            builder = builder.withCardInHand(2, "Lightning Bolt").withLandsOnBattlefield(2, "Mountain", 1)
        }
        repeat(5) { builder = builder.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain") }
        return builder.build()
    }

    private fun TestGame.damageOn(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("a red instant you control deals 2 more damage to a player") {
            val game = setup("Lightning Bolt", "Mountain", 1)
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 15
        }

        test("a red instant you control deals 2 more damage to a creature") {
            val game = setup("Lightning Bolt", "Mountain", 1)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Lightning Bolt", wurm).error shouldBe null
            game.resolveStack()

            // 3 + 2 = 5 damage kills the 6/4.
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
        }

        test("a red sorcery you control deals 2 more damage") {
            val game = setup("Lava Axe", "Mountain", 5)
            game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 13
        }

        test("a non-red spell you control is unaffected") {
            val game = setup("Vicious Hunger", "Swamp", 2)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Vicious Hunger", wurm).error shouldBe null
            game.resolveStack()

            game.damageOn("Craw Wurm") shouldBe 2
        }

        test("a red spell an opponent controls is unaffected") {
            val game = setup("Lightning Bolt", "Mountain", 1, p2Bolt = true)
            game.passPriority()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }

        test("a red planeswalker you control deals 2 more damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pyromancer's Gauntlet")
                .withCardOnBattlefield(1, "Chandra Nalaar")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra Nalaar")!!
            val plusOne = cardRegistry.getCard("Chandra Nalaar")!!.script.activatedAbilities[0]
            game.execute(
                ActivateAbility(game.player1Id, chandra, plusOne.id, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
        }

        test("a red creature's combat damage is unaffected") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pyromancer's Gauntlet")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 17
        }
    }
}
