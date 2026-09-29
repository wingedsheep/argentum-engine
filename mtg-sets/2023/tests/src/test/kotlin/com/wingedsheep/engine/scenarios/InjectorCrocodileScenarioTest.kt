package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Injector Crocodile — "When this creature dies, incubate 3. Swampcycling {2}"
 */
class InjectorCrocodileScenarioTest : ScenarioTestBase() {

    private fun TestGame.incubatorCounts(): List<Int> =
        state.getBattlefield(player1Id)
            .filter { state.getEntity(it)?.get<CardComponent>()?.name == "Incubator" }
            .map { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 }

    init {
        context("Injector Crocodile") {
            test("dying incubates 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Injector Crocodile")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val croc = game.findPermanent("Injector Crocodile")!!
                game.castSpell(2, "Murder", croc).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Injector Crocodile") shouldBe true
                game.incubatorCounts() shouldBe listOf(3)
            }

            test("swampcycling discards it and fetches a Swamp") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Injector Crocodile")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val croc = game.findCardsInHand(1, "Injector Crocodile").single()
                game.execute(TypecycleCard(playerId = game.player1Id, cardId = croc)).error shouldBe null
                game.isInGraveyard(1, "Injector Crocodile") shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options.size shouldBe 1
                game.selectCards(decision.options)
                game.isInHand(1, "Swamp") shouldBe true
            }
        }
    }
}
