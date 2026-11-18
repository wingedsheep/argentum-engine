package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Hydra Broodmaster (JOU #128): "{X}{X}{G}: Monstrosity X. When this creature becomes monstrous,
 * create X X/X green Hydra creature tokens." Both X's in the trigger are the X chosen for the
 * monstrosity ability (CR 701.37c), and the trigger fires only once.
 */
class HydraBroodmasterScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateMonstrosity(x: Int) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Hydra Broodmaster") ?: error("Hydra Broodmaster is not on the battlefield"),
            abilityId = cardRegistry.getCard("Hydra Broodmaster")!!.activatedAbilities[0].id,
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    private fun TestGame.hydraTokens() = state.getBattlefield().filter {
        state.getEntity(it)?.has<TokenComponent>() == true
    }

    private fun TestGame.plusOneCounters(): Int =
        state.getEntity(findPermanent("Hydra Broodmaster")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("monstrosity 2 puts two counters on it and creates two 2/2 green Hydra tokens") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hydra Broodmaster")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateMonstrosity(2).error shouldBe null
            game.resolveStack()

            game.plusOneCounters() shouldBe 2
            val tokens = game.hydraTokens()
            tokens shouldHaveSize 2
            val projected = game.state.projectedState
            tokens.forEach { token ->
                projected.getPower(token) shouldBe 2
                projected.getToughness(token) shouldBe 2
                projected.getColors(token) shouldBe setOf(Color.GREEN.name)
                projected.getSubtypes(token).contains("Hydra") shouldBe true
            }
        }

        test("a second activation once monstrous makes no more tokens") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hydra Broodmaster")
                .withLandsOnBattlefield(1, "Forest", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateMonstrosity(1).error shouldBe null
            game.resolveStack()
            game.hydraTokens() shouldHaveSize 1

            game.activateMonstrosity(3).error shouldBe null
            game.resolveStack()
            game.hydraTokens() shouldHaveSize 1
            game.plusOneCounters() shouldBe 1
        }
    }
}
