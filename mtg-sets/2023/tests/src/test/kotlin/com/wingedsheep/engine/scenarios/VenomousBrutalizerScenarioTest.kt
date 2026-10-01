package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Venomous Brutalizer (ONE #193) — {2}{G}{G} 4/4 Phyrexian Knight.
 *
 *   Toxic 3
 *   When this creature enters, you may pay {1}{G}. If you do, proliferate.
 *
 * Pins the optional {1}{G} payment on the enters trigger: paying proliferates a chosen
 * permanent's counters; declining proliferates nothing.
 */
class VenomousBrutalizerScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Venomous Brutalizer")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast Brutalizer, resolve it and its trigger, answering the payment offer with [pay]; returns how often it was offered. */
    private fun castAndResolve(game: TestGame, pay: Boolean, proliferateTo: List<EntityId>): Int {
        game.castSpell(1, "Venomous Brutalizer").error shouldBe null
        var offers = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 40) {
            when (game.getPendingDecision()) {
                is YesNoDecision -> { game.answerYesNo(pay).error shouldBe null; offers++ }
                is SelectManaSourcesDecision -> {
                    game.submitManaSourcesDecision(autoPay = pay).error shouldBe null
                }
                is SelectCardsDecision -> game.selectCards(proliferateTo).error shouldBe null
                null -> game.resolveStack()
                else -> error("unexpected decision ${game.getPendingDecision()}")
            }
        }
        return offers
    }

    init {
        test("paying {1}{G} proliferates") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            castAndResolve(game, pay = true, proliferateTo = listOf(giant)) shouldBe 1

            game.isOnBattlefield("Venomous Brutalizer") shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(giant) shouldBe 5
        }

        test("declining the payment does not proliferate") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            castAndResolve(game, pay = false, proliferateTo = listOf(giant)) shouldBe 1

            game.isOnBattlefield("Venomous Brutalizer") shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
