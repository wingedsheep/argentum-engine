package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tainted Observer (ONE #217) — {1}{G}{U} 2/3 Creature — Phyrexian Bird.
 *
 * "Flying. Toxic 1. Whenever another creature you control enters, you may pay {2}.
 *  If you do, proliferate."
 */
class TaintedObserverScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Tainted Observer")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Llanowar Elves")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("paying {2} when another creature enters proliferates") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)

            game.castSpell(1, "Llanowar Elves").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            var guard = 0
            var proliferated = false
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                when (game.getPendingDecision()) {
                    is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                    null -> game.resolveStack()
                    else -> { game.selectCards(listOf(bears)); proliferated = true }
                }
            }

            proliferated shouldBe true
            counters(game, bears) shouldBe 2
            game.state.getBattlefield().count { game.state.getEntity(it)?.get<TappedComponent>() != null } shouldBe 3
        }

        test("declining the payment does not proliferate") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)

            game.castSpell(1, "Llanowar Elves").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe 1
        }

        test("an opposing creature entering does not trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tainted Observer")
                .withCardInHand(2, "Llanowar Elves")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Llanowar Elves").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }

        test("has flying and toxic 1") {
            val game = setup()
            val observer = game.findPermanent("Tainted Observer")!!
            game.state.projectedState.hasKeyword(observer, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(observer, "TOXIC_1") shouldBe true
        }
    }
}
