package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Rust Monster (AFR #161) — {2}{R} Creature — Beast 2/1, first strike.
 *
 *   Sacrifice an artifact: This creature gets +2/+0 until end of turn.
 */
class RustMonsterScenarioTest : ScenarioTestBase() {

    init {
        test("sacrificing an artifact gives +2/+0") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rust Monster")
                .withCardOnBattlefield(1, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val monster = game.findPermanent("Rust Monster")!!
            val ornithopter = game.findPermanent("Ornithopter")!!
            val pump = cardRegistry.getCard("Rust Monster")!!.activatedAbilities.first().id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = monster,
                    abilityId = pump,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(ornithopter)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.state.projectedState.getPower(monster) shouldBe 4
            game.state.projectedState.getToughness(monster) shouldBe 1
        }
    }
}
