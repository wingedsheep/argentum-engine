package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Frilled Sandwalla (HOU #116) — {G} 1/1 Creature — Lizard.
 *
 *   {1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
class FrilledSandwallaScenarioTest : ScenarioTestBase() {
    private fun TestGame.activate(name: String) =
        execute(ActivateAbility(playerId = player1Id, sourceId = findPermanent(name)!!, abilityId = cardRegistry.getCard(name)!!.script.activatedAbilities[0].id))

    init {
        context("Frilled Sandwalla") {
            test("pumps +2/+2 once, and can't be activated a second time that turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frilled Sandwalla")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activate("Frilled Sandwalla").error shouldBe null
                game.resolveStack()
                val walla = game.findPermanent("Frilled Sandwalla")!!
                game.state.projectedState.getPower(walla) shouldBe 3
                game.state.projectedState.getToughness(walla) shouldBe 3

                game.activate("Frilled Sandwalla").error shouldNotBe null
            }
        }
    }
}
