package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Alpha Deathclaw (PIP #91): "When this creature enters or becomes monstrous, destroy target
 * permanent." The first card to put a "becomes monstrous" trigger in front of players — the
 * monstrous half must fire once, when monstrosity actually makes it monstrous, and never again.
 */
class AlphaDeathclawScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateMonstrosity() = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Alpha Deathclaw") ?: error("Alpha Deathclaw is not on the battlefield"),
            abilityId = cardRegistry.getCard("Alpha Deathclaw")!!.activatedAbilities[0].id,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    private fun TestGame.plusOneCounters(): Int =
        state.getEntity(findPermanent("Alpha Deathclaw")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering destroys target permanent") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Alpha Deathclaw")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Alpha Deathclaw").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Alpha Deathclaw") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("becoming monstrous destroys target permanent, and a second activation does nothing") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Alpha Deathclaw")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withLandsOnBattlefield(1, "Forest", 7)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateMonstrosity().error shouldBe null
            game.resolveStack()
            game.plusOneCounters() shouldBe 4
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears").shouldNotBeNull())).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false

            // Already monstrous: no counters, no trigger.
            game.activateMonstrosity().error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.plusOneCounters() shouldBe 4
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }
    }
}
