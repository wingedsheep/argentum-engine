package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Frilled Oculus (GTC) — {1}{G}: +2/+2 until end of turn, only once each turn. */
class FrilledOculusScenarioTest : ScenarioTestBase() {
    init {
        test("pumps +2/+2 once, and can't be activated a second time this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Frilled Oculus")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val oculus = game.findPermanent("Frilled Oculus")!!
            val abilityId = cardRegistry.getCard("Frilled Oculus")!!.script.activatedAbilities.single().id

            val result = game.execute(ActivateAbility(game.player1Id, oculus, abilityId))
            withClue("first activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(oculus) shouldBe 3
            game.state.projectedState.getToughness(oculus) shouldBe 5

            withClue("activate only once each turn") {
                game.execute(ActivateAbility(game.player1Id, oculus, abilityId)).error shouldNotBe null
            }
        }
    }
}
