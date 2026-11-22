package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Razortip Whip (GTC) — {1}, {T}: 1 damage to target opponent or planeswalker. */
class RazortipWhipScenarioTest : ScenarioTestBase() {
    init {
        test("{1}, {T}: deals 1 damage to target opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Razortip Whip")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val whip = game.findPermanent("Razortip Whip")!!
            val abilityId = cardRegistry.getCard("Razortip Whip")!!.script.activatedAbilities.single().id

            val result = game.execute(
                ActivateAbility(game.player1Id, whip, abilityId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
        }

        test("can't target its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Razortip Whip")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val whip = game.findPermanent("Razortip Whip")!!
            val abilityId = cardRegistry.getCard("Razortip Whip")!!.script.activatedAbilities.single().id

            game.execute(
                ActivateAbility(game.player1Id, whip, abilityId, targets = listOf(ChosenTarget.Player(game.player1Id)))
            ).error shouldNotBe null
        }
    }
}
