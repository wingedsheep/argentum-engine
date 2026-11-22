package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Foundry Champion (GTC #165) — ETB damage to any target equal to the number of creatures you
 * control; {R}: +1/+0 and {W}: +0/+1 until end of turn.
 */
class FoundryChampionScenarioTest : ScenarioTestBase() {
    init {
        test("entering deals damage equal to the creatures you control, counting itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Foundry Champion")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Foundry Champion").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            withClue("three creatures I control (two Bears + the Champion); the opponent's Bears don't count") {
                game.getLifeTotal(2) shouldBe 17
            }
        }

        test("{R} pumps power and {W} pumps toughness") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Foundry Champion")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val champion = game.findPermanent("Foundry Champion")!!
            val abilities = cardRegistry.getCard("Foundry Champion")!!.script.activatedAbilities

            game.execute(ActivateAbility(game.player1Id, champion, abilities[0].id)).error shouldBe null
            game.resolveStack()
            game.execute(ActivateAbility(game.player1Id, champion, abilities[1].id)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(champion) shouldBe 5
            projected.getToughness(champion) shouldBe 5
        }
    }
}
