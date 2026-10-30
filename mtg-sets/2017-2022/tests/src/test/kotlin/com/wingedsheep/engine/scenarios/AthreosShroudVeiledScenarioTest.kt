package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Athreos, Shroud-Veiled (THB #269): the end-step trigger puts a coin counter on another creature,
 * and a creature with a coin counter that dies or is exiled comes back under Athreos's controller —
 * read from the counters it had as it left, and returned without the counter.
 */
class AthreosShroudVeiledScenarioTest : ScenarioTestBase() {

    private fun TestGame.coins(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.COIN) ?: 0

    private fun TestGame.giveCoin(id: EntityId) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.COIN, 1))
        }
    }

    init {
        test("end step: a coin counter goes on another target creature; when it dies it returns under your control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Athreos, Shroud-Veiled")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.ENDING, Step.END)
            if (game.state.pendingDecision != null) game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("the end-step trigger put a coin counter on the Bears") { game.coins(bears) shouldBe 1 }

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears died and came back under Athreos's controller, without the coin counter") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.state.projectedState.getController(returned) shouldBe game.player1Id
                game.coins(returned) shouldBe 0
            }
        }

        test("a creature with a coin counter that is exiled returns under your control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Athreos, Shroud-Veiled")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Swords to Plowshares")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.giveCoin(bears)

            game.castSpell(1, "Swords to Plowshares", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears left exile for the battlefield under Athreos's controller") {
                game.isInExile(2, "Grizzly Bears") shouldBe false
                val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.state.projectedState.getController(returned) shouldBe game.player1Id
            }
        }

        test("a creature without a coin counter stays dead") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Athreos, Shroud-Veiled")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe null
        }
    }
}
