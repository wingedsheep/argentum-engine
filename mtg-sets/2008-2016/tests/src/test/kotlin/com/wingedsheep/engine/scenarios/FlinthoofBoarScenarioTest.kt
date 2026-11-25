package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Flinthoof Boar (M13 #171) — {1}{G} 2/2 Creature — Boar.
 *   This creature gets +1/+1 as long as you control a Mountain.
 *   {R}: This creature gains haste until end of turn.
 */
class FlinthoofBoarScenarioTest : ScenarioTestBase() {
    init {
        val hasteId = cardRegistry.getCard("Flinthoof Boar")!!.script.activatedAbilities.single().id

        test("is 2/2 without a Mountain") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Flinthoof Boar")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val boar = game.findPermanent("Flinthoof Boar")!!
            game.state.projectedState.getPower(boar) shouldBe 2
            game.state.projectedState.getToughness(boar) shouldBe 2
        }

        test("a Mountain makes it 3/3, and {R} lets it attack the turn it arrived") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Flinthoof Boar", summoningSickness = true)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val boar = game.findPermanent("Flinthoof Boar")!!
            game.state.projectedState.getPower(boar) shouldBe 3
            game.state.projectedState.getToughness(boar) shouldBe 3

            val result = game.execute(ActivateAbility(game.player1Id, boar, hasteId))
            withClue("haste should activate: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets the summoning-sick Boar attack") {
                game.declareAttackers(mapOf("Flinthoof Boar" to 2)).error shouldBe null
            }
        }
    }
}
