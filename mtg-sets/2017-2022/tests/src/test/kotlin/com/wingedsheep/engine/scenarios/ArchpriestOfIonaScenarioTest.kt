package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Archpriest of Iona (ZNR #5) — "Archpriest of Iona's power is equal to the number of creatures in
 * your party. At the beginning of combat on your turn, if you have a full party, target creature
 * gets +1/+1 and gains flying until end of turn."
 *
 * The Archpriest is a Cleric, so it is always in its own party: power 1 alone, rising with each
 * Rogue / Warrior / Wizard, and the combat trigger only fires with all four filled.
 */
class ArchpriestOfIonaScenarioTest : ScenarioTestBase() {

    private fun member(name: String, type: String) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human $type"
        power = 1
        toughness = 1
    }

    private val rogue = member("Iona Test Rogue", "Rogue")
    private val warrior = member("Iona Test Warrior", "Warrior")
    private val wizard = member("Iona Test Wizard", "Wizard")

    private fun board(vararg allies: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Archpriest of Iona")
        .also { b -> allies.forEach { b.withCardOnBattlefield(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(rogue)
        cardRegistry.register(warrior)
        cardRegistry.register(wizard)

        test("power is the number of creatures in your party") {
            val game = board()
            val priest = game.findPermanent("Archpriest of Iona")!!
            game.state.projectedState.getPower(priest) shouldBe 1
            game.state.projectedState.getToughness(priest) shouldBe 2

            val withTwo = board("Iona Test Rogue", "Iona Test Warrior")
            withTwo.state.projectedState.getPower(withTwo.findPermanent("Archpriest of Iona")!!) shouldBe 3
        }

        test("with a full party, beginning of combat pumps a target and gives it flying") {
            val game = board("Iona Test Rogue", "Iona Test Warrior", "Iona Test Wizard")
            val priest = game.findPermanent("Archpriest of Iona")!!
            val rogueId = game.findPermanent("Iona Test Rogue")!!
            game.state.projectedState.getPower(priest) shouldBe 4

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(rogueId)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(rogueId) shouldBe 2
            projected.getToughness(rogueId) shouldBe 2
            projected.hasKeyword(rogueId, Keyword.FLYING) shouldBe true
        }

        test("without a full party the combat trigger doesn't fire") {
            val game = board("Iona Test Rogue", "Iona Test Warrior")
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }
    }
}
