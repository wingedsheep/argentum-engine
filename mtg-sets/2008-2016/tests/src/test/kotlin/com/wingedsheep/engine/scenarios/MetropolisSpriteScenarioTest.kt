package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Metropolis Sprite (GTC #42) — {1}{U} 1/2 Creature — Faerie Rogue.
 *
 *   Flying
 *   {U}: This creature gets +1/-1 until end of turn.
 */
class MetropolisSpriteScenarioTest : ScenarioTestBase() {
    private fun TestGame.activate(name: String) =
        execute(ActivateAbility(playerId = player1Id, sourceId = findPermanent(name)!!, abilityId = cardRegistry.getCard(name)!!.script.activatedAbilities[0].id))

    init {
        context("Metropolis Sprite") {
            test("flies, and {U} makes it +1/-1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Metropolis Sprite")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sprite = game.findPermanent("Metropolis Sprite")!!
                game.state.projectedState.hasKeyword(sprite, Keyword.FLYING) shouldBe true
                game.activate("Metropolis Sprite").error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(sprite) shouldBe 2
                game.state.projectedState.getToughness(sprite) shouldBe 1
            }
        }
    }
}
